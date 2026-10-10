# Aplikacja do nauki czytania – dokument projektowy

Oct 10, 2026 · @Czajus

## Wizja i cele

Aplikacja na Androida uczy dzieci w wieku 5–7 lat czytania po polsku metodą syntezy głosek i sylab. Rdzeniem jest strzałka, która płynnie prowadzi wzrok przez wyraz w tempie ustawionym przez rodzica, z opcjonalnym odtwarzaniem głosek i całych wyrazów.

**Dla kogo:** dziecko w wieku przedszkolnym i wczesnoszkolnym (czyta) oraz rodzic lub nauczyciel (konfiguruje, ocenia postępy).

**Problem:** dziecko zna litery, ale nie potrafi ich złożyć w wyraz. Literuje („be-a-be-a”) zamiast czytać.

**Zasady produktu:**

- Działa w pełni offline, bez kont, reklam i zbierania danych.
- Krótkie sesje (5–10 minut), bez presji czasu i liczników błędów.
- Treść (słownik) jest danymi, nie kodem — łatwo ją rozszerzać.
- Najpierw próba dziecka, potem podpowiedź audio.

## Założenia dydaktyczne

Aplikacja uczy syntezy: strzałka sunie przez **głoski** (nie litery), a dźwięki przechodzą jedna w drugą („mmmaaa”), aż złożą się w sylabę i wyraz.

**Zasady:**

- **Głoski, nie nazwy liter.** Odtwarzamy „b”, nie „be”. Głoski i sylaby to nagrania, nie TTS (TTS czyta nazwy liter).
- **Jednostka = grafem.** Dwuznaki (sz, cz, rz, ch, dz, dź, dż) i zmiękczenia przed samogłoską (si, ci, zi, ni, dzi) wskazujemy jako jedną jednostkę.
- **Płynny ruch.** Strzałka porusza się w sposób ciągły, jak w karaoke; tempo regulowane, domyślnie wolne.
- **Najpierw próba.** Całego wyrazu nie odtwarzamy automatycznie — dziecko najpierw czyta samo, potem może dotknąć „posłuchaj”.
- **Obrazek po przeczytaniu.** Ilustracja pojawia się po próbie, żeby dziecko nie zgadywało wyrazu z obrazka.
- **Czcionka szkolna.** Andika (SIL, OFL): jednopiętrowe „a”, wyraźnie różne b/d/p/q, duży rozmiar, zwiększone odstępy.

**Progresja poziomów:**

| Poziom | Zakres | Przykłady |
| --- | --- | --- |
| 1 | Samogłoski | a, o, u, e, i, y |
| 2 | Sylaby otwarte | ma, to, la, pa |
| 3 | Wyrazy z sylab otwartych | mama, tata, lody, koza |
| 4 | Sylaby zamknięte, zbitki | kot, dom, sklep, trawa |
| 5 | Dwuznaki | szafa, czapka, chata, rzeka |
| 6 | Zmiękczenia i znaki diakrytyczne | siano, ciocia, koń, źrebak |
| 7 | Krótkie zdania | Ala ma kota. |

Otwarte pytanie: czy kolejność wprowadzania spółgłosek w poziomach 2–3 oprzeć na konkretnej metodzie (np. „Kocham czytać”), czy na częstości w słowniku dziecięcym.

## Funkcje i tryby

MVP ma jeden tryb czytania (strzałka automatyczna) i panel rodzica; tryb „palcem” i sylaby jako jednostka wchodzą po MVP.

| Funkcja | Opis | Zakres |
| --- | --- | --- |
| Strzałka automatyczna | Sunie pod kolejnymi głoskami; bieżąca jednostka podświetlona, przeczytane wyszarzone | MVP |
| Tempo | Suwak 300–1500 ms na jednostkę, domyślnie 900 ms; zapisywane per profil dziecka | MVP |
| Głoski na głos | Nagranie każdej głoski w chwili, gdy wskazuje ją strzałka; można wyłączyć | MVP |
| Wyraz na głos | Przycisk „posłuchaj” po przejściu strzałki; TTS lub nagranie | MVP |
| Powtórz / pauza | Dotknięcie wyrazu zatrzymuje strzałkę; przycisk powtórzenia | MVP |
| Poziomy i lekcje | 7 poziomów, lekcja = 8–10 wyrazów | MVP: poziomy 1–3 |
| Panel rodzica | Ocena „przeczytał / z pomocą / jeszcze nie”, ustawienia, postęp; wejście za bramką rodzicielską | MVP |
| Profile dzieci | Kilka dzieci na jednym urządzeniu, osobne postępy | MVP: 1 profil |
| Tryb „palcem” | Dziecko prowadzi palcem pod wyrazem, głoski odtwarzane w tempie palca | Po MVP |
| Sylaby jako jednostka | Strzałka wskazuje całe sylaby (sza-fa) | Po MVP |
| Obrazki | Ilustracja po przeczytaniu wyrazu | Po MVP |

Bramka rodzicielska: proste zadanie, którego dziecko nie przeczyta (np. „przytrzymaj 3 sekundy” albo działanie matematyczne), zgodnie z wymaganiami Google Play dla aplikacji dziecięcych.

## Architektura

Aplikacja to Kotlin + Jetpack Compose w architekturze warstwowej (MVVM z jednokierunkowym przepływem stanu); cała logika czytania mieszka w module `core:domain`, który jest zwykłym modułem JVM, więc import `android.*` w nim się nie skompiluje.

&#91;embedded content: zależności modułów · 4 warstwy\]

Moduły `feature:*` nie zależą od siebie nawzajem; `core:data` i `core:audio` implementują interfejsy zdefiniowane w `core:domain`, a Hilt łączy implementacje.

**Przepływ danych:** JSON w assets → import do Room → `WordRepository` → `ReadingViewModel` → `PacingEngine` emituje `StateFlow<ReadingState>` → Compose rysuje strzałkę; `SpeechPlayer` zgłasza silnikowi koniec nagrania.

**Stany silnika (`PacingEngine`):**

- `Idle` → `Pointing(0)` po starcie wyrazu.
- `Pointing(i)` → `WaitingForAudio(i)`, gdy głoski są włączone; inaczej po upływie tempa → `Pointing(i+1)`.
- `WaitingForAudio(i)` → `Pointing(i+1)` po końcu nagrania i upływie tempa (krok = `max(tempo, nagranie)`).
- Dowolny stan aktywny → `Paused(i)` po dotknięciu wyrazu; `Paused(i)` → `Pointing(i)` po wznowieniu.
- Ostatnia jednostka → `WordDone`; odblokowuje przycisk „posłuchaj” i ocenę rodzica.

**Stos:** Kotlin 2.x, Compose (BOM), Navigation Compose, Hilt, Room, DataStore, kotlinx.serialization, Coroutines/Flow; build: Gradle Kotlin DSL, version catalog, convention plugins.

## Struktura słownika

Słownik to ręcznie anotowany JSON w `assets/content/`, ładowany do Room przy pierwszym uruchomieniu lub zmianie wersji. Źródłem prawdy jest anotacja, nie algorytm — polszczyzna ma wyjątki, których reguły nie złapią.

```json
{
  "version": 1,
  "phonemes": [
    { "id": "sz", "grapheme": "sz", "audio": "phonemes/sz.ogg" },
    { "id": "si", "grapheme": "si", "audio": "phonemes/si.ogg", "note": "zmiękczenie przed samogłoską" }
  ],
  "levels": [
    { "id": 5, "name": "Dwuznaki", "lessons": ["l5-sz", "l5-cz"] }
  ],
  "words": [
    {
      "id": "szafa",
      "text": "szafa",
      "units": ["sz", "a", "f", "a"],
      "syllables": ["sza", "fa"],
      "level": 5,
      "lesson": "l5-sz",
      "image": "images/szafa.webp",
      "wordAudio": "words/szafa.ogg"
    }
  ]
}
```

**Zasady anotacji:**

- `units` złączone muszą dać dokładnie `text`; `syllables` złączone też. Walidator w teście jednostkowym sprawdza to dla całego słownika.
- Każda jednostka w `units` ma odpowiadający wpis w `phonemes` z nagraniem.
- `wordAudio` opcjonalne; brak = TTS.
- Pułapki do anotacji ręcznej: „marznąć” (r + z osobno), „odzysk” (d + z osobno), zapożyczenia typu „pianino”, „Maria” (i przed samogłoską czytane inaczej niż w „siano”).
- Narzędzie pomocnicze `GraphemeSegmenter` proponuje podział przy dopisywaniu wyrazów; człowiek zatwierdza.

**Encje Room:** `WordEntity`, `PhonemeEntity`, `LessonEntity`, `ChildProfileEntity`, `AttemptEntity` (profil, wyraz, ocena rodzica, data, tempo).

## Kluczowe decyzje techniczne i ryzyka

Największe ryzyko to synchronizacja strzałki z dźwiękiem; rozwiązuje ją silnik, który czeka na koniec nagrania, zanim przejdzie dalej.

**Decyzje:**

| Obszar | Decyzja | Uzasadnienie |
| --- | --- | --- |
| UI | Jetpack Compose, jedna aktywność | Animacja strzałki na `Canvas`, łatwe testy UI |
| Pozycja strzałki | `TextLayoutResult.getBoundingBox(offset)` dla pierwszego i ostatniego znaku jednostki | Działa dla dowolnej czcionki i rozmiaru |
| Animacja | `Animatable` po osi X, czas = krok tempa | Płynny ruch; domena podaje tylko indeks i czas |
| Krok silnika | `max(tempo, długość nagrania)` | Nagranie nigdy nie jest ucinane |
| Głoski i sylaby | Nagrania `.ogg`, odtwarzanie przez `SoundPool` (preload) | Niskie opóźnienie, poprawne głoski |
| Całe wyrazy | Android `TextToSpeech`, `Locale("pl", "PL")`; nagranie, jeśli jest | Brak nagrań dla każdego wyrazu w MVP |
| Brak polskiego głosu | `isLanguageAvailable` przy starcie → ekran z instrukcją instalacji danych głosowych | Nie każde urządzenie ma polski głos |
| Dane | Room (słownik, postępy), DataStore (ustawienia) | Offline, testowalne |
| DI | Hilt | Standard, łatwe podmiany w testach |
| Czas | `Clock`/`TimeSource` wstrzykiwany do silnika | Deterministyczne testy z wirtualnym czasem |
| Prywatność | Brak uprawnienia `INTERNET`, brak SDK analitycznych | Polityka Google Play Families, RODO |
| minSdk | 26 | `UtteranceProgressListener.onRangeStart`, \~95% urządzeń |

**Ryzyka:**

| Ryzyko | Skutek | Ograniczenie |
| --- | --- | --- |
| Jakość nagrań głosek | Złe nagranie uczy złej wymowy | Konsultacja z logopedą, jeden lektor, normalizacja głośności |
| Opóźnienie TTS przy pierwszym użyciu | Przycisk „posłuchaj” reaguje z opóźnieniem | Inicjalizacja TTS przy starcie, rozgrzewka pustą wypowiedzią |
| Błędy anotacji słownika | Strzałka dzieli „rz” w „marznąć” | Walidator w testach + przegląd ręczny |
| Czcionki o innych metrykach | Strzałka rozjeżdża się z literą | Testy zrzutów ekranu dla kilku rozmiarów |
| Utrata uwagi dziecka | Krótkie korzystanie | Krótkie lekcje, pochwała dźwiękowa, obrazek jako nagroda |

## Strategia testów

Cała logika czytania żyje w czystym Kotlinie (`core:domain`), więc większość testów to szybkie testy jednostkowe na JVM; UI sprawdzają testy zrzutów ekranu i kilka testów przepływu.

| Warstwa | Narzędzie | Co sprawdzamy | Przykład |
| --- | --- | --- | --- |
| Domena | JUnit 5 + Kotest assertions | Segmentacja, sylabizacja | „szczoteczka” → sz-cz-o-t-e-cz-k-a; „siano” → si-a-n-o |
| Silnik tempa | kotlinx-coroutines-test (`runTest`, wirtualny czas) | Kolejność stanów, czasy kroków, pauza | Tempo 500 ms, nagranie 700 ms → krok 700 ms |
| ViewModel | Turbine | Strumień stanów UI | Pauza w połowie wyrazu → stan Paused z indeksem 2 |
| Słownik | Test walidatora na pełnym JSON | Spójność `units`/`syllables`/`text`, brakujące nagrania | Każdy wyraz poziomu 5 ma dwuznak |
| Dane | Room in-memory | DAO, migracje | Zapis próby i odczyt postępu |
| UI statyczne | Roborazzi | Pozycja strzałki pod jednostką | 3 rozmiary czcionki × jasny/ciemny |
| UI przepływ | Compose UI Test | Lekcja od startu do końca | 10 wyrazów → ekran pochwały |
| Audio | Fałszywa implementacja `SpeechPlayer` | Kolejność i czas odtwarzania | Brak odtwarzania przy wyłączonych głoskach |

CI: GitHub Actions — `./gradlew check` (lint, detekt, ktlint, testy jednostkowe, Roborazzi verify) przy każdym PR; testy instrumentalne na emulatorze nocą lub ręcznie.

## Backlog MVP

MVP to 7 epików w kolejności zależności; pierwszy działający ekran czytania powstaje po epikach E0–E3.

### E0. Fundament projektu

- [ ] Projekt wielomodułowy (`app`, `core:domain`, `core:data`, `core:audio`, `core:ui`, `feature:*`) z version catalog i convention plugins. **AC:** `./gradlew check` przechodzi na pustym projekcie.
- [ ] Hilt, Compose, Room, DataStore skonfigurowane. **AC:** aplikacja startuje z pustym ekranem.
- [ ] CI na GitHub Actions z lintem, detekt, ktlint i testami. **AC:** PR bez zielonego CI nie może zostać scalony.

### E1. Domena czytania

- [ ] Modele `Word`, `ReadingUnit`, `Syllable`. **AC:** czysty Kotlin, zero zależności od Androida.
- [ ] `GraphemeSegmenter` z obsługą dwuznaków i zmiękczeń. **AC:** testy dla min. 30 wyrazów, w tym przypadków z sekcji „Struktura słownika”.
- [ ] `PacingEngine` jako maszyna stanów (Idle, Pointing, WaitingForAudio, Paused, WordDone). **AC:** testy z wirtualnym czasem; krok = `max(tempo, nagranie)`; pauza i wznowienie zachowują indeks.

### E2. Treść i dane

- [ ] Schemat JSON + walidator słownika. **AC:** test wykrywa niezgodne `units`, brakujące nagranie i zły poziom.
- [ ] Słownik poziomów 1–3: samogłoski, \~20 sylab otwartych, \~40 wyrazów. **AC:** walidator przechodzi.
- [ ] Room: import słownika przy pierwszym starcie i przy zmianie `version`. **AC:** test migracji i importu.
- [ ] DataStore: tempo, głoski wł./wył., rozmiar czcionki. **AC:** ustawienia przetrwają restart.

### E3. Ekran czytania

- [ ] Wyraz czcionką Andika, strzałka animowana pod bieżącą jednostką, przeczytane wyszarzone. **AC:** test Roborazzi dla 3 rozmiarów; strzałka wyśrodkowana pod „sz” w „szafa”.
- [ ] Sterowanie: start, pauza (dotknięcie wyrazu), powtórz, następny. **AC:** test Compose UI całej lekcji.
- [ ] Przycisk „posłuchaj” aktywny dopiero po przejściu strzałki. **AC:** przed końcem przycisk wyłączony.

### E4. Audio

- [ ] Interfejs `SpeechPlayer` + `RecordedPhonemePlayer` (SoundPool, preload lekcji). **AC:** odtworzenie głoski < 100 ms od zdarzenia.
- [ ] `TtsWordPlayer` z polskim głosem i obsługą braku głosu. **AC:** brak głosu → ekran z instrukcją, aplikacja działa dalej bez TTS.
- [ ] Nagrania głosek poziomów 1–3. **AC:** jeden lektor, znormalizowana głośność, format `.ogg`.

### E5. Lekcje i panel rodzica

- [ ] Lista poziomów i lekcji z postępem. **AC:** lekcja zaliczona po ocenie min. 8/10 wyrazów jako „przeczytał”.
- [ ] Bramka rodzicielska i panel: ocena wyrazów, ustawienia, historia prób. **AC:** dziecko nie wejdzie przypadkiem (test UI bramki).
- [ ] Ekran pochwały po lekcji. **AC:** bez punktacji błędów.

### E6. Wydanie

- [ ] Ikona, nazwa, polityka prywatności, ankieta Google Play Families. **AC:** wydanie w ścieżce testów wewnętrznych.

## Roadmapa po MVP

Po MVP rozbudowa idzie w kolejności: więcej treści, aktywniejsze tryby, adaptacja tempa.

1. **Poziomy 4–7** — zbitki, dwuznaki, zmiękczenia, krótkie zdania; nagrania pozostałych głosek.
2. **Obrazki do wyrazów** — pokazywane po przeczytaniu.
3. **Tryb „palcem”** — dziecko prowadzi palcem, głoski odtwarzane w tempie palca.
4. **Sylaby jako jednostka** — przełącznik w ustawieniach; strzałka wskazuje „sza-fa”.
5. **Wiele profili dzieci** — osobne postępy i tempo.
6. **Adaptacyjne tempo** — przyspieszenie po serii ocen „przeczytał”, zwolnienie po „jeszcze nie”.
7. **Eksport postępów** — PDF dla rodzica lub nauczyciela, bez chmury.
8. **Rozpoznawanie mowy (eksperyment)** — offline, tylko jako podpowiedź dla rodzica, nie jako ocena.
