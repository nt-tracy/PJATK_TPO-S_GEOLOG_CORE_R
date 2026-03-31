/**
 *
 *  @author Tracewicz Natalia s33507
 *
 */

package zad1;


public interface GeoLookup {
  GeoInfo lookup(String ip) throws GeoLookupException;
}
