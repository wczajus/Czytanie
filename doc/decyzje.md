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

**D7. Roborazzi wchodzi z E3.** W E0 nie było jeszcze ekranu, którego zrzut miałby sens. Od E3
`check` w `feature:reading` zależy od `verifyRoborazziDebug` (zob. D37).

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

## Treść i dane (E2)

**D20. Ścieżki zasobów w słowniku są względne wobec katalogu `assets/`** (`audio/phonemes/sz.ogg`,
`images/szafa.webp`), a nie wobec `assets/audio/`, jak w przykładzie z dokumentu. Jedna konwencja dla
nagrań i obrazków; walidator sprawdza istnienie pliku dokładnie pod tą ścieżką.

**D21. Opcjonalne pole `phonemeIds` w wyrazie** na wypadek jednostek czytanych inaczej niż ich
litery (np. „n” w „nic” → „ni”, zob. D9). Bez niego głoska = jednostka małymi literami.

**D22. Reguły poziomów (`LevelRules`)**: 1 — jedna samogłoska; 2 — jedna sylaba otwarta „prosta
spółgłoska + samogłoska”; 3 — co najmniej dwie sylaby otwarte z prostych grafemów; 4 — tylko proste
grafemy; 5 — co najmniej jeden dwuznak, bez zmiękczeń i znaków diakrytycznych; 6 — co najmniej jedno
zmiękczenie lub litera diakrytyczna (ą ę ó ł ś ć ź ń ż); 7 — bez ograniczeń. „ł” i „ó” traktujemy
jak litery diakrytyczne (poziom 6), bo wyglądają inaczej niż litery z poziomów 1–4. Warto to
skonsultować z metodyką; zmiana to jedna linia w `LevelRules`.

**D23. Lekcja ma 6–10 wyrazów.** Dokument mówi o 8–10, ale poziom 1 ma tylko 6 samogłosek.

**D24. Różnica między anotacją a `GraphemeSegmenter` to ostrzeżenie, nie błąd.** Test treści wypisuje
ostrzeżenia w logu i przerywa build tylko na błędach.

**D25. Słownik poziomów 1–3**: 6 samogłosek, 30 sylab otwartych w 3 lekcjach, 40 wyrazów w 4
lekcjach. Spółgłoski tylko proste; pominięte są wyrazy z „si ci zi ni” (np. „zima”, „Nina”), bo to
zmiękczenia z poziomu 6. Kolejność spółgłosek to propozycja do weryfikacji (otwarte pytanie z
dokumentu: metoda „Kocham czytać” czy częstość).

**D26. Zastępcze nagrania**: 400 ms ciszy Ogg Vorbis na głoskę, generowane przez
`tools/generate-placeholder-audio.sh` (TODO(audio)). Prawdziwe nagranie zastępuje plik o tej samej
nazwie.

**D27. Import słownika jest leniwy**: `RoomDictionaryRepository` wywołuje `DictionaryImporter` przed
pierwszym odczytem w procesie. Import podmienia słownik w jednej transakcji; próby dziecka
(`attempt`) nie mają klucza obcego do wyrazów, więc nowa wersja słownika ich nie usuwa.

**D28. Baza w wersji 1 (v0.1.0) jest odtwarzana, a nie migrowana**: zawierała tylko pustą tabelę
profili. Od wersji 2 każda zmiana wymaga migracji z testem, a schematy w `core/data/schemas` muszą
być w repozytorium (CI to sprawdza i wypisuje brakujący plik w logu).

## Ekran czytania (E3)

**D29. Strzałka dojeżdża do jednostki w 30% kroku (najwyżej 300 ms), potem stoi pod nią.** Nagranie
głoski startuje razem z krokiem (D11), więc strzałka musi być pod jednostką prawie od razu; ruch przez
cały krok oznaczałby, że głoska brzmi, zanim strzałka do niej dojedzie. Środek jednostki to połowa
odcinka od lewej krawędzi pierwszego do prawej krawędzi ostatniego znaku (`getBoundingBox`).

**D30. Dziecko samo zaczyna każdy wyraz** przyciskiem „Czytaj” albo dotknięciem wyrazu. Strzałka nie
rusza sama po pokazaniu wyrazu: dziecko ma chwilę, żeby spojrzeć na całość i się przygotować.

**D31. Przyciski mają stałe miejsca**: „Jeszcze raz” po lewej, główny w środku („Czytaj” / „Pauza” /
„Czytaj dalej”, a po przeczytaniu „Następny”), „Posłuchaj” po prawej. „Posłuchaj” i „Następny” są
aktywne dopiero po przejściu strzałki przez cały wyraz, żeby dziecko nie przeskakiwało wyrazów bez
próby.

**D32. Bieżącą jednostkę wyróżnia tylko kolor**, nie pogrubienie: inna grubość zmieniłaby szerokość
wyrazu i strzałka by skakała.

**D33. Po przeczytaniu cały wyraz wraca do zwykłego koloru, a strzałka znika.** Dziecko widzi wyraz
jako całość, a nie jako ciąg wyszarzonych liter. Obrazek (jeśli jest) pojawia się dopiero wtedy.

**D34. Długi wyraz jest zmniejszany, żeby zmieścił się w jednej linii.** Wybrany rozmiar (D18)
pozostaje górną granicą; wyrazu nigdy nie dzielimy na linie.

**D35. Do czasu E4 aplikacja używała `SilentSpeechPlayer`**, który nic nie odtwarzał. Od E4 zastępuje
go `AndroidSpeechPlayer` (D39).

**D36. Lista poziomów to na razie prosta lista lekcji** („Poziom 1: Samogłoski”, „Lekcja 1”…), żeby
ekran czytania był dostępny w wydaniu. Postępy, bramka i panel rodzica dochodzą w E5.

**D37. Zrzuty Roborazzi są w `feature/reading/src/test/snapshots`.** Testy JVM tego modułu działają
tylko w wariancie debug (aktywność testowa Compose jest rejestrowana tylko tam). Lokalnego SDK
Androida w środowisku Claude nie ma, więc pierwsze zrzuty nagrywa CI i wypisuje je w logu jako base64
(bez artefaktów), a człowiek lub Claude przegląda je przed dodaniem do repozytorium.

**D38. detekt dopuszcza stałe `SCREAMING_CASE` obok `PascalCase`** (wzorzec `[A-Z][A-Za-z0-9_]*`),
bo stałe `const val` w Kotlinie tradycyjnie mają podkreślenia, a wartości `Dp` w Compose nie.

## Dźwięk (E4)

**D39. Trzy odtwarzacze za jednym `SpeechPlayer`.** Głoski: `SoundPool` (`SoundPoolClipPlayer`,
małe opóźnienie). Cały wyraz: nagranie z `MediaPlayer`, jeśli słownik je ma, w przeciwnym razie TTS.
`AndroidSpeechPlayer` tylko wybiera źródło, więc reguły („głoska nigdy z TTS”, „uszkodzone nagranie
wyrazu → TTS”) są testowane na JVM z fałszywymi odtwarzaczami.

**D40. Długość nagrania głoski czytamy przy ładowaniu** (`MediaMetadataRetriever`), bo `SoundPool` nie
zgłasza końca odtwarzania. `playPhoneme` wraca po tym czasie, więc silnik może czekać na nagranie (D11).
Nieznana długość = 0 ms: strzałka idzie wtedy w samym tempie.

**D41. Lekcja wczytuje swoje głoski z góry** (`SpeechPlayer.prepare` przy otwarciu lekcji). Głoska
spoza listy ładuje się przy pierwszym użyciu. Nagrania zostają w pamięci do końca procesu (kilkadziesiąt
krótkich plików).

**D42. TTS tylko z polskim głosem offline.** Bez polskiego głosu wyraz nie jest czytany wcale (głos
innego języka uczyłby złej wymowy), a przycisk „Posłuchaj” znika, zostawiając puste miejsce, żeby
pozostałe przyciski się nie przesuwały. Głosy sieciowe pomijamy, żeby nic z lekcji nie opuszczało
urządzenia. Silnik TTS startuje przy pierwszym sprawdzeniu, a nie razem z aplikacją. Locale to
`Locale.forLanguageTag("pl-PL")`, bo konstruktor `Locale("pl", "PL")` jest przestarzały, a ostrzeżenia
są błędami (D5).

**D43. Nagrania `.ogg` nie są kompresowane w APK** (`noCompress`), bo `openFd` wymaga
nieskompresowanych zasobów.

## Lekcje i panel rodzica (E5)

**D44. Rodzic ocenia wyrazy po lekcji, a nie po każdym wyrazie.** Na ekranie pochwały jest dyskretny
przycisk „Dla rodzica: oceń czytanie”, który prowadzi przez bramkę do listy wyrazów lekcji z ocenami
„przeczytane / z pomocą / jeszcze nie”. Ocena w trakcie lekcji przerywałaby dziecku czytanie, a
przyciski oceny obok wyrazu dziecko naciskałoby samo. Nieocenione wyrazy nie są zapisywane.

**D45. Lekcja jest zaliczona, gdy co najmniej 80% jej wyrazów ma ostatnią ocenę „przeczytane”**
(8 z 10, 5 z 6). Liczy się tylko najnowsza ocena wyrazu, więc wcześniejsze „jeszcze nie” nie obciąża
dziecka. Zaliczoną lekcję oznacza gwiazdka na liście.

**D46. Bramka rodzicielska to mnożenie dwóch liczb od 3 do 9 wpisywane na klawiaturze cyfr.** Błędna
odpowiedź od razu zmienia zadanie, więc zgadywanie nie pomaga. Bramka jest w `core:ui`, bo stoi przed
ekranami dwóch modułów (panel i ocena lekcji), a moduły funkcji nie mogą zależeć od siebie.
Otwarta bramka obowiązuje do wyjścia z ekranu.

**D47. Ustawienia są dostępne tylko z panelu rodzica**, więc nie mają własnej bramki. Suwak tempa
idzie od „wolniej” do „szybciej” (krótszy czas na głoskę), a podgląd rozmiaru pokazuje wyraz „szafa”.

**D48. Jeden profil dziecka (`DEFAULT_PROFILE_ID = 1`)** do czasu, aż pojawi się potrzeba kilku dzieci.
Tabela profili już istnieje. Czas zapisu ocen pochodzi z wstrzykniętego `java.time.Clock`.

**D49. Ekran pochwały nie liczy niczego**: gwiazdka, „Brawo!”, „Lekcja przeczytana.” Bez punktów, bez
liczby błędów, taki sam dla każdego dziecka. Dźwięk pochwały dojdzie razem z nagraniami (TODO(audio)).

## Wydanie (E6)

**D50. Ikona to otwarta książka ze strzałką pod spodem** na granatowym tle z motywu (`#1D4E89`),
strzałka w ciepłym pomarańczowym kolorze z ciemnego motywu. Ikona jest tylko wektorowa (adaptacyjna,
minSdk 26), z osobną warstwą `monochrome` bez linii tekstu dla ikon motywowych Androida 13+. Pliki
do karty Google Play (ikona 512, grafika 1024×500) są w `doc/google-play/`, renderowane z tych samych
kształtów.

**D51. Polityka prywatności jest w aplikacji i w repozytorium** (`doc/polityka-prywatnosci.md`),
z tym samym tekstem. W aplikacji: Dla rodzica → Prywatność, więc dziecko jej nie otwiera. Nie podajemy
adresu e-mail w aplikacji: kontaktem jest adres dewelopera w karcie Google Play.

**D52. Release buduje też AAB** i dołącza go do wydania na GitHubie (nie jako artefakt Actions).
Z sekretem `PLAY_SERVICE_ACCOUNT_JSON` i kluczem wydania AAB trafia sam do ścieżki testów
wewnętrznych (`r0adkll/upload-google-play`). Bez nich krok jest pomijany. Formularze Play Console
(Families, Data safety, IARC) wypełnia właściciel konta według `doc/google-play/README.md`.

## Po MVP: poziomy 4–6

**D53. Słownik w wersji 2 ma poziomy 4–6** (111 nowych wyrazów w 12 lekcjach): 4 — sylaby zamknięte,
zbitki i nowe litery c, h, j; 5 — sz, cz, rz, ch, dz; 6 — si ci zi ni dzi, litery ze znakami i
dwuznaki dż, dź. Unikane są wyrazy, w których „i” przed samogłoską tylko zmiękcza spółgłoskę inną niż
s c z n dz („pies”, „kwiat”, „wiosna”), bo tego nie opisuje jeszcze żadna jednostka. Poziom 7
(zdania) wymaga najpierw obsługi spacji i interpunkcji na ekranie czytania.

**D54. „dż” i „dź” należą do poziomu 6**, bo mają literę ze znakiem (D22). `LevelRules` uznaje
jednostkę za poziom 6, gdy którakolwiek jej litera jest diakrytyczna.

**D55. Jedno nagranie na grafem**, także gdy dwa grafemy brzmią tak samo („rz”/„ż”, „ch”/„h”,
„ó”/„u”, „si”/„ś”). `phonemeId` zgadza się wtedy z propozycją `GraphemeSegmenter`, a lektor może
nagrać ten sam dźwięk dwa razy. Pole `note` w słowniku mówi, jak czytać grafem. Nazwy plików są w
ASCII (`z_dot.ogg`, `a_nasal.ogg`, `dz_acute.ogg`), żeby uniknąć problemów z kodowaniem w APK i
narzędziach. Nowe nagrania to cisza zastępcza (D26, TODO(audio)).
