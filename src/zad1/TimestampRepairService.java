/**
 *
 *  @author Tracewicz Natalia s33507
 *
 */
package zad1;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.List;

public class TimestampRepairService {

  public List<ResolvedLogEntry> resolveTimestamps(List<LogEntry> entries, String zoneIdStr) {
    ZoneId zoneId = ZoneId.of(zoneIdStr);
    ZoneRules rules = zoneId.getRules();
    List<ResolvedLogEntry> results = new ArrayList<>();

    for (int i = 0; i < entries.size(); i++) {
      LogEntry current = entries.get(i);
      LocalDateTime ldt = current.serverLocalTime();

      ZoneOffsetTransition transition = rules.getTransition(ldt);
      if (transition != null && transition.isGap()) {
        LocalDateTime repaired = ldt.plus(transition.getDuration());
        results.add(new ResolvedLogEntry(current, repaired.atZone(zoneId), ResolutionKind.GAP_REPAIRED));
        continue;
      }

      if (rules.getValidOffsets(ldt).size() > 1) {
        List<LogEntry> block = new ArrayList<>();
        block.add(current);

        int j = i + 1;
        while (j < entries.size() && rules.getValidOffsets(entries.get(j).serverLocalTime()).size() > 1) {
          block.add(entries.get(j));
          j++;
        }

        resolveAmbiguousBlock(block, zoneId, results);
        i = j - 1;
        continue;
      }

      results.add(new ResolvedLogEntry(current, ldt.atZone(zoneId), ResolutionKind.OK));
    }
    return results;
  }

  private void resolveAmbiguousBlock(List<LogEntry> block, ZoneId zone, List<ResolvedLogEntry> results) {
    int jumpIndex = -1;
    for (int k = 1; k < block.size(); k++) {
      if (block.get(k).serverLocalTime().isBefore(block.get(k - 1).serverLocalTime())) {
        if (jumpIndex != -1) {
          markAsDropped(block, results);
          return;
        }
        jumpIndex = k;
      }
    }

    if (jumpIndex == -1) {
      markAsDropped(block, results);
      return;
    }

    for (int k = 0; k < block.size(); k++) {
      LogEntry e = block.get(k);
      var offsets = zone.getRules().getValidOffsets(e.serverLocalTime());
      ZonedDateTime zdt = ZonedDateTime.ofLocal(e.serverLocalTime(), zone, offsets.get(k < jumpIndex ? 0 : 1));
      results.add(new ResolvedLogEntry(e, zdt, ResolutionKind.OVERLAP_RESOLVED));
    }
  }

  private void markAsDropped(List<LogEntry> block, List<ResolvedLogEntry> results) {
    for (LogEntry e : block) {
      results.add(new ResolvedLogEntry(e, null, ResolutionKind.AMBIGUOUS_DROPPED));
    }
  }
}