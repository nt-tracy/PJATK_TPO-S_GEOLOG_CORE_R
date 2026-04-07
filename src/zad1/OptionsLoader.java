/**
 *
 *  @author Tracewicz Natalia s33507
 *
 */

package zad1;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OptionsLoader {

  public GeoTimeOptions load(String filename) {
    Path path = Path.of(filename);

    try (InputStream inputStream = Files.newInputStream(path)) {
      Yaml yaml = new Yaml();
      Map<String, Object> data = yaml.load(inputStream);

      String serverZoneId = (String) data.get("serverZoneId");
      if (serverZoneId == null || serverZoneId.isBlank()) {
        throw new IllegalArgumentException("serverZoneId is required and cannot be empty");
      }

      @SuppressWarnings("unchecked")
      List<String> logLines = (List<String>) data.get("logLines");
      if (logLines == null) {
        logLines = Collections.emptyList();
      }

      return new GeoTimeOptions(serverZoneId, logLines);

    } catch (Exception e) {
      throw new RuntimeException("Failed to load GeoLogOptions.yaml: " + e.getMessage(), e);
    }
  }
}