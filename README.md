task: S_GEOLOG_CORE_R | render: html | charset: cp1250 | generated_at: __TS__
>> Zadanie: GeoLog Core R
Cel zadania: napisz bibliotekę analizującą logi HTTP zapisane w lokalnym czasie serwera. Część wpisów może mieć błędny albo niejednoznaczny czas z powodu zmiany czasu. Program ma odrzucić linie błędne składniowo, naprawić czasy nieistniejące, w miarę możliwości ujednoznacznić poprawne sekwencje czasu, odrzucić sekwencje niejednoznaczne i dopiero na poprawionych danych wykonać analizę odwiedzeń.
>> Problem:
Serwer zapisuje tylko lokalny LocalDateTime, przez co przy zmianie czasu pojawiają się dwa problemy:

czas nieistniejący - lokalna godzina nie występuje, bo zegar przeskakuje do przodu,
czas niejednoznaczny - lokalna godzina występuje więcej niż raz; zegar cofany o wartość wynikającą z reguł strefy
>> Schemat rozwiązania
GeoLogOptions.yaml -> LogParser -> TimestampRepairService -> GeoLookup: ipwho.is -> AnalyticsService -> AnalysisReport
>> Wejście
Program czyta plik GeoLogOptions.yaml z katalogu domowego użytkownika (user.home). W pliku znajdują się:

serverZoneId - strefa czasowa serwera, np. Europe/Warsaw,
logLines - lista linii logów.
Przykładowy plik:

serverZoneId: Europe/Warsaw
logLines:
  - "r0001|2024-01-10T12:00:00|8.8.8.8|GET|/login|200|15|1234"
>> Format pojedynczej linii logu
requestId|serverLocalTime|clientIp|method|endpoint|status|latencyMs|bytes
Przykład poprawnej linii:

r0001|2024-01-10T12:00:00|8.8.8.8|GET|/login|200|15|1234
>> Znaczenie pól
requestId - niepusty identyfikator żądania,
serverLocalTime - lokalny czas serwera bez strefy, format ISO yyyy-MM-dd'T'HH:mm:ss,
clientIp - poprawny adres IPv4,
method - niepusta metoda HTTP, np. GET lub POST,
endpoint - niepusta ścieżka zasobu, np. /login,
status - kod odpowiedzi HTTP jako liczba całkowita,
latencyMs - liczba całkowita nieujemna,
bytes - liczba całkowita nieujemna.
>>Należy zaimplementować
>> 1. LogParser
Klasa odpowiada za walidację i parsowanie jednej linii logu. Jeżeli linia jest niepoprawna, metoda ma zwrócić Optional.empty(), a nie rzucić wyjątek.

Za niepoprawną uznaj między innymi:

null albo pusty tekst,
złą liczbę pól,
puste pola obowiązkowe,
zły adres IPv4,
złą datę albo zły format daty,
wartości liczbowe, których nie da się przetworzyć,
>> 2. TimestampRepairService
Ta klasa pracuje na LocalDateTime serwera i ma przygotować sekwencję zapisów czasu w strefie serwera.

Wymagane zachowanie:

jeżeli czas jest 'poprawny/zwykły' - zaakceptuj go bez zmian,
jeżeli czas nie istnieje - napraw go przez przesunięcie do przodu o rzeczywistą długość luki,
jeżeli mamy blok kolejnych czasów niejednoznacznych i da się wskazać jeden wyraźny punkt „powrotu do przeszłości”, rozwiąż blok na podstawie tej sekwencji,
jeżeli blok nie daje się jednoznacznie rozstrzygnąć - odrzuć cały blok .
Jeżeli sekwencja nie daje podstaw do logicznego rozstrzygnięcia, trzeba ją oznaczyć jako niejednoznaczną i wykluczyć z dalszej analityki.

>> 3. OptionsLoader
Ta klasa czyta konfigurację z YAML i buduje GeoTimeOptions.

serverZoneId jest polem wymaganym i nie może być puste,
logLines jest polem opcjonalnym; brak oznacza pustą listę.
>> 4. IpWhoIsGeoLookup
Ta klasa łączy się z API https://ipwho.is/<ip> przez HTTPS. Z odpowiedzi JSON należy odczytać tylko pola:

success,
country_code,
timezone.id.
Metoda lookup(String ip) ma wykonać żądanie sieciowe, a następnie wywołać metodę pomocniczą parseGeoInfo(String json).

Metoda parseGeoInfo(String json) ma zwrócić GeoInfo tylko wtedy, gdy odpowiedź jest poprawna i zawiera poprawne country_code oraz poprawne timezone.id. W przeciwnym razie ma rzucić GeoLookupException.

Uwaga: nie należy określać strefy czasowej po kraju (jeden kraj może mieć wiele stref).

>> 5. AnalyticsService
To główna klasa zadania. Jej rola:

odrzucić linie błędne składniowo,
naprawić i ujednoznacznić sekwencję czasu po stronie serwera,
zapisać, które wpisy musiały zostać naprawione lub odrzucone,
dla zaakceptowanych wpisów pobrać GeoInfo z GeoLookup,
przeliczyć czas serwera na lokalny czas nadawcy,
zbudować analitykę per kraj i per strefa czasowa nadawcy.
>> Jak rozumieć poprawną i niejednoznaczną sekwencję
Przykład sekwencji umożliwiającej korektę:

a 2024-10-27T02:30
b 2024-10-27T02:35
c 2024-10-27T02:20
d 2024-10-27T02:25
W tej sekwencji występuje jeden punkt powrotu do wcześniejszego czasu. Taki blok można rozdzielić na dwa fragmenty i naprawić.

Przykład sekwencji, której nie należy naprawiać:


x 2025-10-26T02:40
y 2025-10-26T02:45
		
Taki blok nie ma jednego punktu cofnięcia. Wpisy należy odrzucić jako niejednoznaczne.

>> Raporty
>> Raport zbiorczy
Metoda AnalysisReport.toText() zwraca tekst dokładnie w tej strukturze:

SUMMARY
Metric                      Value
--------------------------  -----
Invalid lines               1
Repaired gap times          1
Resolved overlap entries    2
Dropped ambiguous entries   2
GeoLookup failures          0

AMBIGUOUS REQUEST IDS
f
g

COUNTRIES
Code Count
---- -----
CN       2
US       4

TIMEZONES
Timezone                 Count
------------------------ -----
America/Los_Angeles          4
Asia/Shanghai                2

HOURS (sender)
Hour range  Count
----------- -----
10:00-10:59     1
14:00-14:59     1
17:00-17:59     1
18:00-18:59     2
19:00-19:59     1
sekcja SUMMARY zawiera podstawowe liczniki jakości danych i przebiegu analizy,
AMBIGUOUS REQUEST IDS zawiera identyfikatory wpisów odrzuconych jako niejednoznaczne,
COUNTRIES pokazuje liczbę poprawnych żądań per kraj,
TIMEZONES pokazuje liczbę poprawnych żądań per strefa czasowa nadawcy,
HOURS (sender) pokazuje globalny histogram godzin nadawcy w formie tabeli.
>> Raport szczegółowy dla strefy czasowej
Metoda timezoneHistogram(String timezoneId) zwraca histogram godzin dla jednej strefy czasowej nadawcy, zawsze w pełnych 24 wierszach.

TIMEZONE HISTOGRAM
Timezone: America/Los_Angeles

Hour range  Count
----------- -----
00:00-00:59     0
01:00-01:59     0
02:00-02:59     0
...
23:00-23:59     0
