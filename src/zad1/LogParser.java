/**
 *
 *  @author Tracewicz Natalia s33507
 *
 */

package zad1;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class LogParser {

  public Optional<LogEntry> parseLine(String line) {
    if (line == null || line.isBlank()) return Optional.empty();

    String[] p = line.split("\\|", -1);
    if (p.length != 8) return Optional.empty();

    try {
      // Walidacja pól tekstowych i formatu IP (uproszczona)
      if (p[0].isEmpty() || p[3].isEmpty() || p[4].isEmpty() || !p[2].contains("."))
        return Optional.empty();

      return Optional.of(new LogEntry(
              p[0],
              LocalDateTime.parse(p[1]),
              p[2], p[3], p[4],
              Integer.parseInt(p[5]),
              Integer.parseInt(p[6]),
              Integer.parseInt(p[7])
      ));
    } catch (DateTimeParseException | NumberFormatException e) {
      return Optional.empty();
    }
  }
}