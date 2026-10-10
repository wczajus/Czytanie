# Czytanie

Aplikacja na Androida do nauki czytania po polsku metodą syntezy głosek (dzieci 5–7 lat).
Działa w pełni offline, bez kont, reklam i zbierania danych.

- Opis produktu i backlog: [`doc/`](doc/)
- Decyzje implementacyjne: [`doc/decyzje.md`](doc/decyzje.md)
- Zasady dla agentów i kontrybutorów: [`CLAUDE.md`](CLAUDE.md)

## Budowanie

Wymagany JDK 17+ i Android SDK (compileSdk 36).

```bash
./gradlew assembleDebug   # APK debug
./gradlew test            # testy jednostkowe
./gradlew check           # lint + detekt + ktlint + testy
```

## Wydania

Gotowe APK są publikowane wyłącznie w [GitHub Releases](../../releases). Nowe wydanie powstaje po
wypchnięciu taga, np. `git tag v0.1.0 && git push origin v0.1.0`, albo po ręcznym uruchomieniu
workflow **Release** (Actions → Release → Run workflow) z numerem wersji.
