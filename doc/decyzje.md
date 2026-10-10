# Decyzje implementacyjne

Rejestr decyzji podjętych podczas implementacji tam, gdzie dokument projektowy lub CLAUDE.md
zostawiały swobodę albo były niejednoznaczne. Każda pozycja: decyzja, powód, jak zmienić.

## Projekt i build

**D1. Pakiet `pl.dev404.czytanie`.** `applicationId` i przestrzeń nazw aplikacji. Moduły biblioteczne
dostają przestrzeń nazw z ścieżki modułu, np. `:feature:reading` → `pl.dev404.czytanie.feature.reading`
(convention plugin `czytanie.android.library`).

**D2. Zero artefaktów w GitHub Actions; buildy tylko w GitHub Releases.** Konto nie ma miejsca na
artefakty Actions. `ci.yml` uruchamia `./gradlew check` i `assembleDebug` bez `upload-artifact`;
raporty lint, detekt i testów trafiają do logu zadania. `release.yml` po wypchnięciu taga `v*` buduje
APK i dołącza go do GitHub Release (`gh release create`). Cache Gradle (`setup-gradle`) to osobny
mechanizm, który nie zajmuje miejsca na artefakty; zapisuje go tylko `main`.

**D3. Podpis wydań.** Gdy w sekretach repozytorium są `CZYTANIE_KEYSTORE_BASE64`,
`CZYTANIE_KEYSTORE_PASSWORD`, `CZYTANIE_KEY_ALIAS`, `CZYTANIE_KEY_PASSWORD`, APK jest podpisany kluczem
wydania. Bez nich używany jest jednorazowy klucz debug z runnera, więc kolejnej wersji nie da się
zainstalować na poprzedniej bez odinstalowania. Przed ścieżką testów Google Play (E6) klucz jest
konieczny.

**D4. Wersje narzędzi.** Gradle 8.14.3, AGP 8.13.0, Kotlin 2.2.21, KSP 2.2.21-2.0.5, compileSdk i
targetSdk 36, minSdk 26, JVM 17. Wybrane jako zestaw o znanej wzajemnej zgodności; Dependabot
(`.github/dependabot.yml`) proponuje aktualizacje co tydzień. detekt 1.23.8 działa na własnym,
przypiętym Kotlinie 2.0.21 (konfiguracja w `Quality.kt`), bo jest z nim skompilowany.

**D5. Ostrzeżenia kompilatora Kotlin są błędami** (`allWarningsAsErrors`). Lint przerywa build na
błędach, ale nie na „dostępna nowsza wersja zależności” (tym zajmuje się Dependabot).

**D6. JUnit 5 w każdym module** (także w testach jednostkowych modułów Androida), Kotest assertions,
`kotlinx-coroutines-test` i Turbine dodawane przez convention plugin. Testy Robolectric/Roborazzi,
które wymagają JUnit 4, dojdą razem z E3 (silnik vintage).

**D7. Roborazzi wchodzi z E3.** W E0 nie ma jeszcze ekranu, którego zrzut miałby sens, więc
`check` na razie nie zawiera `verifyRoborazzi`.

## Domena (E1)

**D8. Indeksy jednostek są półotwarte `[startIndex, endIndex)`**, jak w `String.substring`, więc
`text.substring(start, end) == grapheme`. `ReadingUnit.lastIndex` podaje ostatni znak (inclusive) do
`getBoundingBox` w UI.

**D9. `grapheme` i `phonemeId` są rozdzielone.** Zwykle `phonemeId` to grafem małymi literami, ale nie
zawsze: w „nic” jednostka „n” ma głoskę „ni” (ń), bo „i” jest tu samogłoską sylaby. Dzięki temu
strzałka wskazuje literę, a dziecko słyszy właściwą głoskę. To samo dla „si”, „ci”, „zi”, „dzi”
przed spółgłoską lub na końcu wyrazu („zima” → z(zi) i m a, „dzisiaj” → dz(dzi) i si a j).

**D10. `GraphemeSegmenter` zwraca podpowiedzi do ręcznej weryfikacji**, bo jest tylko narzędziem
pomocniczym (źródłem prawdy jest anotacja):
- `FromExceptionList`: podział z listy wyjątków („marznąć”, „odzysk”, „odzew”, „nadzór”…);
- `PossiblySeparateLetters`: „rz” przed spółgłoską („drzwi” — dwuznak, „marznąć” — osobno) i „dz”
  tuż po przedrostku od-/nad-/pod-/przed- („nadzieja” — dwuznak, „odzysk” — osobno);
- `IBeforeVowel`: „i” przed samogłoską po spółgłosce innej niż s/c/z/n („miasto” vs „Maria”, „pianino”).

Segmenter obsługuje pojedyncze wyrazy z liter. Zdania (poziom 7, po MVP) wymagają decyzji, jak
traktować spacje i interpunkcję.

**D11. `WaitingForAudio(i)` pojawia się tylko wtedy, gdy tempo minęło, a nagranie jeszcze trwa.**
Dokument dopuszczał też odczytanie „zawsze przechodź przez WaitingForAudio, gdy głoski są włączone”.
Wybrany wariant niesie informację dla UI (strzałka czeka pod jednostką), a krok i tak trwa
`max(tempo, nagranie)`. Nagranie startuje razem z krokiem, więc dźwięk zaczyna się w chwili, gdy
strzałka dochodzi do jednostki.

**D12. Pauza zatrzymuje też nagranie; wznowienie odtwarza bieżącą jednostkę od początku**
(`Paused(i)` → `Pointing(i)` z pełnym krokiem i ponownym nagraniem). Dziecko słyszy całą głoskę,
a nie jej urwaną końcówkę.

**D13. Zmiana tempa w trakcie wyrazu działa od następnej jednostki.** Bieżący krok kończy się w starym
tempie, żeby animacja strzałki nie skakała.

**D14. Uszkodzone lub brakujące nagranie nie zatrzymuje czytania.** Silnik traktuje je jak nagranie
o zerowej długości. Brak pliku ma wykryć walidator słownika (E2), a nie dziecko w trakcie lekcji.

**D15. Czas w silniku to wyłącznie `delay` na wstrzykniętym `CoroutineScope`.** W aplikacji jest to
`viewModelScope`, w testach `TestScope` z wirtualnym czasem (`runTest`, `advanceTimeBy`). Polecenia
(`start`, `pause`, `resume`…) są wywoływane z wątku tego scope'u (główny wątek); dodatkowo licznik
przebiegów chroni przed nadpisaniem stanu przez anulowany krok.

## UI i UX

**D16. Andika w całej aplikacji**, nie tylko w wyrazie do czytania: dziecko widzi te same kształty
liter na przyciskach i w lekcji. Pliki z Google Fonts (OFL 1.1, licencja w `core/ui/licenses`).

**D17. Kolory stałe, bez Material You (dynamic color).** Kontrast bieżącej jednostki, przeczytanych
i nadchodzących liter musi być taki sam na każdym urządzeniu. Tło „papierowe” zamiast czystej bieli
(mniej olśnień), jasny i ciemny motyw. Przeczytane jednostki mają kontrast ok. 3:1: są wyraźnie
cofnięte, ale nadal czytelne przy dużym rozmiarze.

**D18. Trzy rozmiary wyrazu: 56, 72 (domyślny), 96 sp**, z odstępem między literami 0,08 em, żeby
strzałka jednoznacznie stała pod jedną jednostką.

**D19. Brak kopii zapasowej w chmurze** (`allowBackup=false`, reguły wykluczające wszystko). Dane
dziecka nie opuszczają urządzenia. Uprawnienie `INTERNET` jest jawnie usuwane w manifeście
(`tools:node="remove"`), żeby żadna zależność nie mogła go dołączyć.
