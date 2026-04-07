/**
 *
 *  @author Tracewicz Natalia s33507
 *
 */
package zad1;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;

public class IpWhoIsGeoLookup implements GeoLookup {

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Override
  public GeoInfo lookup(String ip) throws GeoLookupException {
    try {
      HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create("https://ipwho.is/" + ip))
              .GET()
              .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new GeoLookupException("HTTP Error: " + response.statusCode());
      }

      return parseGeoInfo(response.body());
    } catch (Exception e) {
      throw new GeoLookupException("Network error during lookup: " + e.getMessage());
    }
  }

  public GeoInfo parseGeoInfo(String json) throws GeoLookupException {
    // Sprawdzenie pola "success":true
    if (!json.contains("\"success\":true")) {
      throw new GeoLookupException("API returned success:false");
    }

    try {
      String countryCode = extractValue(json, "country_code");
      String timezoneId = extractValue(json, "timezone\", \"id\" : \""); // Szukamy id wewnątrz obiektu timezone

      // Jeśli standardowe szukanie zawiedzie, szukamy po prostu klucza "id"
      if (timezoneId == null) timezoneId = extractValue(json, "id");

      if (countryCode == null || timezoneId == null) {
        throw new GeoLookupException("Missing country_code or timezone.id");
      }

      return new GeoInfo(countryCode, ZoneId.of(timezoneId));
    } catch (Exception e) {
      throw new GeoLookupException("Invalid data in JSON: " + e.getMessage());
    }
  }

  private String extractValue(String json, String key) {
    // Szukamy fragmentu "klucz":"wartość"
    String pattern = "\"" + key + "\":\"";
    int start = json.indexOf(pattern);
    if (start == -1) {
      // Próba obsłużenia formatu bez cudzysłowu po dwukropku (dla bezpieczeństwa)
      pattern = "\"" + key + "\": \"";
      start = json.indexOf(pattern);
    }

    if (start == -1) return null;

    start += pattern.length();
    int end = json.indexOf("\"", start);
    return (end != -1) ? json.substring(start, end) : null;
  }
}