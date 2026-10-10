# CLAUDE.md — Aplikacja do nauki czytania (Android)

Ten plik opisuje projekt dla Claude Code. Czytaj go na początku każdej sesji.
Pełny dokument projektowy (wizja, dydaktyka, backlog MVP): https://claude.ai/code/artifact/2703d2f6-a574-4177-a067-9f6f4d067a4f
(kopia w `doc/`). Decyzje podjęte w trakcie implementacji: `doc/decyzje.md` — dopisuj tam nowe.

## Cel produktu

Aplikacja na Androida uczy dzieci (5–7 lat) czytania po polsku metodą syntezy głosek i sylab.
Strzałka płynnie przesuwa się pod kolejnymi głoskami wyrazu w tempie ustawionym przez rodzica.
Opcjonalnie odtwarzane są głoski (nagrania) i całe wyrazy (TTS lub nagranie).

## Zasady dydaktyczne — NIE łam ich w kodzie

- Jednostką wskazywania jest **grafem/głoska**, nie litera. Dwuznaki `sz cz rz ch dz dź dż` i zmiękczenia przed samogłoską (`si ci zi ni dzi`, a także `pi bi mi wi ki`… — D61) to JEDNA jednostka.
- Głoski i sylaby odtwarzamy **wyłącznie z nagrań**. Nigdy nie używaj TTS do pojedynczych liter — TTS czyta nazwy liter („be”), a dziecko ma słyszeć głoskę („b”).
- Cały wyraz NIE jest odtwarzany automatycznie. Przycisk „posłuchaj” aktywuje się dopiero po przejściu strzałki przez cały wyraz.
- Obrazek do wyrazu pokazujemy dopiero po przeczytaniu.
- Bez presji czasu, bez liczników błędów, bez kar.
- Podział wyrazów pochodzi z ręcznej anotacji w słowniku (źródło prawdy). `GraphemeSegmenter` służy tylko jako podpowiedź przy dodawaniu wyrazów i w walidacji.

## Stos technologiczny

- Kotlin 2.x, Jetpack Compose (BOM), Material 3, Navigation Compose
- Hilt (DI), Room (słownik, postępy), DataStore (ustawienia)
- kotlinx.serialization (import słownika z JSON), Coroutines + Flow
- Audio: `SoundPool` (głoski, sylaby — preload na lekcję), `TextToSpeech` z `Locale("pl", "PL")` (całe wyrazy)
- Czcionka: Andika (SIL, OFL) w `core:ui`
- Build: Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`), convention plugins w `build-logic/`
- minSdk 26, targetSdk = najnowszy stabilny
- Pakiet / `applicationId`: `pl.dev404.czytanie`; moduły: `pl.dev404.czytanie.<ścieżka modułu>`

## Struktura modułów

```
app/                 MainActivity, nawigacja, Hilt
feature/reading/     ekran czytania, strzałka, ReadingViewModel
feature/levels/      lista poziomów i lekcji, postęp
feature/parent/      bramka rodzicielska, oceny, historia prób
feature/settings/    tempo, głoski wł./wył., rozmiar czcionki
core/ui/             motyw, czcionka Andika, wspólne komponenty
core/data/           Room, DataStore, import JSON, implementacje repozytoriów
core/audio/          implementacje SpeechPlayer (SoundPool, TTS)
core/domain/         CZYSTY KOTLIN (moduł JVM): modele, GraphemeSegmenter, PacingEngine, interfejsy
build-logic/         convention plugins
```

### Reguły zależności

- `core:domain` to moduł `kotlin("jvm")` — **zero** importów `android.*` i `androidx.*`.
- `feature:*` nie zależą od siebie nawzajem; komunikują się przez nawigację w `app`.
- `core:data` i `core:audio` implementują interfejsy z `core:domain`; Hilt je wiąże.
- UI nie woła Room ani `SoundPool` bezpośrednio — tylko przez ViewModel → use case/repozytorium.

## Kluczowe elementy domeny

- `Word(id, text, units: List<ReadingUnit>, syllables, level, lesson, image?, wordAudio?)`
- `ReadingUnit(grapheme, phonemeId, startIndex, endIndex)` — indeksy znaków w `text` (do pozycjonowania strzałki).
- `PacingEngine` — maszyna stanów emitująca `StateFlow<ReadingState>`:
  - `Idle` → `Pointing(0)` po starcie
  - `Pointing(i)` → `WaitingForAudio(i)` (gdy głoski włączone) lub po upływie tempa → `Pointing(i+1)`
  - `WaitingForAudio(i)` → `Pointing(i+1)`; krok trwa `max(tempo, długość nagrania)` — nagranie nigdy nie jest ucinane
  - każdy stan aktywny → `Paused(i)`; `Paused(i)` → `Pointing(i)`
  - ostatnia jednostka → `WordDone`
- Czas wstrzykiwany (`TimeSource`/`CoroutineScheduler`) — silnik musi być testowalny wirtualnym czasem.
- `SpeechPlayer` (interfejs): `suspend fun playPhoneme(id)`, `suspend fun playWord(word)`; funkcje wracają po zakończeniu odtwarzania.

## UI: strzałka

- Pozycja jednostki: `TextLayoutResult.getBoundingBox(offset)` dla pierwszego i ostatniego znaku jednostki → środek X pod jednostką.
- Ruch: `Animatable` po osi X, czas = bieżący krok z `ReadingState`. Domena podaje tylko indeks i czas; animacja żyje w UI.
- Bieżąca jednostka podświetlona, przeczytane wyszarzone. Dotknięcie wyrazu = pauza.
- Duże czcionki, wysoki kontrast, obsługa ciemnego motywu.

## Treść (słownik)

- Pliki: `app/src/main/assets/content/dictionary.json`, nagrania `assets/audio/phonemes/*.ogg`, `assets/audio/words/*.ogg`, obrazki `assets/images/*.webp`.
- Import do Room przy pierwszym starcie i przy zmianie pola `version`.
- Niezmienniki (sprawdzane testem walidatora na CAŁYM słowniku):
  - `units.joinToString("") == text` oraz `syllables.joinToString("") == text`; w zdaniach (poziom 7) jednostki i sylaby pokrywają tylko litery, a spacje i interpunkcja leżą między nimi (D56)
  - każda jednostka ma wpis w `phonemes` i istniejący plik audio
  - poziom wyrazu zgodny z jego zawartością (np. poziom 3 nie zawiera dwuznaków)
- Pułapki: „marznąć” (r+z osobno), „odzysk” (d+z osobno), zapożyczenia typu „pianino”. Przy dodawaniu wyrazów sprawdzaj je ręcznie.

## CI i wydania

- **Żadnych artefaktów w GitHub Actions** (nie używaj `actions/upload-artifact` ani niczego, co zapisuje artefakty). Konto nie ma na nie miejsca i build by się wywracał.
- `.github/workflows/ci.yml`: `./gradlew check` + `assembleDebug`, raporty tylko w logu.
- Buildy publikujemy wyłącznie jako **GitHub Releases**: `.github/workflows/release.yml` po wypchnięciu taga `v*` albo ręcznie (`workflow_dispatch` z numerem wersji).

## Prywatność i Google Play Families

- **Brak uprawnienia `INTERNET`** w manifeście. Nie dodawaj go bez wyraźnej zgody właściciela projektu.
- Brak SDK analitycznych, reklamowych, crash-reporterów wysyłających dane.
- Panel rodzica za bramką rodzicielską.

## Komendy

```bash
./gradlew assembleDebug            # build
./gradlew test                     # testy jednostkowe (JVM)
./gradlew check                    # lint + detekt + ktlint + testy + Roborazzi verify
./gradlew recordRoborazziDebug     # aktualizacja zrzutów referencyjnych (tylko świadomie!)
./gradlew connectedDebugAndroidTest # testy instrumentalne (emulator)
```

Przed zakończeniem zadania zawsze uruchom `./gradlew check` i upewnij się, że przechodzi.

## Testy

- Domena: JUnit 5 + Kotest assertions. Każda zmiana w `GraphemeSegmenter` lub `PacingEngine` wymaga testu.
  - przykłady obowiązkowe: „szczoteczka” → `sz cz o t e cz k a`; „siano” → `si a n o`; „marznąć” wg anotacji
- Silnik: `kotlinx-coroutines-test` (`runTest`, `advanceTimeBy`) — bez `Thread.sleep` i bez prawdziwego czasu.
- ViewModel: Turbine.
- Room: baza in-memory, testy migracji.
- UI statyczne: Roborazzi (3 rozmiary czcionki × jasny/ciemny), szczególnie pozycja strzałki pod „sz” w „szafa”.
- UI przepływ: Compose UI Test (cała lekcja).
- Audio w testach: fałszywa implementacja `SpeechPlayer` z kontrolowanym czasem trwania.
- Preferuj TDD dla `core:domain`: najpierw test, potem implementacja.

## Konwencje kodu

- Identyfikatory, komentarze w kodzie i commity po angielsku; teksty widoczne dla dziecka i rodzica po polsku w `strings.xml` (żadnych napisów na sztywno w Compose).
- Commity: Conventional Commits (`feat(reading): ...`, `fix(domain): ...`, `test(...)`).
- Małe, spójne zmiany — jedna historyjka z backlogu na raz.
- Stan UI jako niemutowalna `data class` + `StateFlow`; zdarzenia jako `sealed interface`.
- Nie dodawaj nowych bibliotek bez uzasadnienia; dodawaj je przez version catalog.

## Kolejność pracy (backlog MVP)

1. **E0 Fundament** — moduły, convention plugins, Hilt, Compose, Room, DataStore, CI (GitHub Actions: `./gradlew check`).
2. **E1 Domena** — modele, `GraphemeSegmenter`, `PacingEngine` + testy.
3. **E2 Treść i dane** — schemat JSON, walidator, słownik poziomów 1–3, import do Room, DataStore.
4. **E3 Ekran czytania** — wyraz czcionką Andika, animowana strzałka, pauza/powtórz/następny, „posłuchaj” po przejściu strzałki.
5. **E4 Audio** — `RecordedPhonemePlayer` (SoundPool), `TtsWordPlayer` (obsługa braku polskiego głosu), nagrania poziomów 1–3.
6. **E5 Lekcje i panel rodzica** — lista poziomów, bramka, oceny, ekran pochwały.
7. **E6 Wydanie** — ikona, polityka prywatności, ankieta Families, ścieżka testów wewnętrznych.

Nagrania głosek przygotowuje człowiek. Do czasu ich dostarczenia używaj plików zastępczych (cisza o zadanej długości) i oznacz to w kodzie `// TODO(audio)`.
Nagrania sylab (`assets/audio/syllables/`, nazwy wg `Syllable.audioPathOf`) są tymczasowo syntetyczne: generuje je offline `tools/generate-syllable-audio.py` (espeak-ng) przed buildem, nigdy w aplikacji (D64). Po dodaniu wyrazów uruchom skrypt ponownie; walidator wymaga nagrania każdej sylaby.

## Czego NIE robić

- Nie używaj TTS do głosek ani sylab.
- Nie dziel dwuznaków na osobne litery.
- Nie dodawaj importów Androida do `core:domain`.
- Nie odtwarzaj całego wyrazu automatycznie przed próbą dziecka.
- Nie aktualizuj zrzutów Roborazzi tylko po to, żeby test przeszedł — najpierw wyjaśnij różnicę.
- Nie dodawaj uprawnienia `INTERNET` ani SDK zbierających dane.
