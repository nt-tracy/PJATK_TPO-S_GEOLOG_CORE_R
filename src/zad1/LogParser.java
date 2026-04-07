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
      if (p[0].isBlank() || p[3].isBlank() || p[4].isBlank() || !p[2].contains("."))
        return Optional.empty();

      LocalDateTime time = LocalDateTime.parse(p[1]);
      int status = Integer.parseInt(p[5]);
      int latency = Integer.parseInt(p[6]);
      int bytes = Integer.parseInt(p[7]);

      if (latency < 0 || bytes < 0) return Optional.empty();

      return Optional.of(new LogEntry(
              p[0], time, p[2], p[3], p[4], status, latency, bytes
      ));
    } catch (DateTimeParseException | NumberFormatException e) {
      return Optional.empty();
    }
  }
}