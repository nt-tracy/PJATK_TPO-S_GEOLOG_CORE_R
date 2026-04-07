/**
 *
 *  @author Tracewicz Natalia s33507
 *
 */

package zad1;


import java.time.ZonedDateTime;
import java.util.*;

public record AnalyticsService(
    LogParser logParser,
    TimestampRepairService timestampRepairService
) {

  public AnalysisReport analyze(GeoTimeOptions options, GeoLookup lookup) {
    List<LogEntry> validParsedEntries = new ArrayList<>();

    for (String line : options.logLines()) {
      logParser.parseLine(line).ifPresent(validParsedEntries::add);
    }

    int invalidLines = options.logLines().size() - validParsedEntries.size();

    List<ResolvedLogEntry> resolved = timestampRepairService.resolveTimestamps(validParsedEntries, options.serverZoneId());

    int repairedGap = 0;
    int resolvedOverlap = 0;
    int droppedAmbiguous = 0;
    int geoFailures = 0;

    List<String> ambiguousIds = new ArrayList<>();
    Map<String, Long> countries = new HashMap<>();
    Map<String, Long> timezones = new HashMap<>();
    long[] globalHours = new long[24];
    Map<String, long[]> tzHistograms = new HashMap<>();

    for (ResolvedLogEntry res : resolved) {
      if (res.resolutionKind() == ResolutionKind.GAP_REPAIRED) repairedGap++;
      else if (res.resolutionKind() == ResolutionKind.OVERLAP_RESOLVED) resolvedOverlap++;
      else if (res.resolutionKind() == ResolutionKind.AMBIGUOUS_DROPPED) {
        droppedAmbiguous++;
        ambiguousIds.add(res.source().requestId());
        continue;
      }

      try {
        GeoInfo geo = lookup.lookup(res.source().clientIp());

        ZonedDateTime senderTime = res.serverTime().withZoneSameInstant(geo.zoneId());
        int hour = senderTime.getHour();

        countries.merge(geo.countryCode(), 1L, Long::sum);
        timezones.merge(geo.zoneId().getId(), 1L, Long::sum);

        globalHours[hour]++;
        tzHistograms.computeIfAbsent(geo.zoneId().getId(), k -> new long[24])[hour]++;

      } catch (GeoLookupException e) {
        geoFailures++;
      }
    }

    return new AnalysisReport(
            invalidLines,
            repairedGap,
            resolvedOverlap,
            droppedAmbiguous,
            geoFailures,
            ambiguousIds,
            countries,
            timezones,
            globalHours,
            tzHistograms
    );
  }
}
