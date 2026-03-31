/**
 *
 *  @author Tracewicz Natalia s33507
 *
 */

package zad1;


import java.time.ZonedDateTime;

public record ResolvedLogEntry(
    LogEntry source,
    ZonedDateTime serverTime,
    ResolutionKind resolutionKind
) {}
