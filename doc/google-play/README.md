# Google Play: karta aplikacji, Families i wydanie

Materiały i odpowiedzi do formularzy Play Console. Odpowiedzi wynikają z kodu: brak uprawnienia
`INTERNET`, brak SDK zewnętrznych, dane tylko lokalnie (zob. `doc/polityka-prywatnosci.md`).
Jeśli kod się zmieni, zaktualizuj ten plik razem z nim.

## Pliki

| Plik | Do czego |
| --- | --- |
| `ikona-512.png` | Ikona w karcie aplikacji (512×512, PNG 32-bit) |
| `ikona.svg` | Źródło ikony (te same kształty co `ic_launcher_foreground.xml`) |
| `grafika-1024x500.png` | Grafika promocyjna |

Zrzuty ekranu (min. 2, telefon) zrób na urządzeniu z wersji z GitHub Releases.

## Karta aplikacji

- **Nazwa:** Czytanie
- **Krótki opis (do 80 znaków):** Nauka czytania głoskami: strzałka prowadzi dziecko pod każdą głoską wyrazu.
- **Pełny opis:**

> Czytanie pomaga dzieciom w wieku 5–7 lat nauczyć się czytać po polsku metodą syntezy głosek.
>
> Strzałka przesuwa się pod kolejnymi głoskami wyrazu w tempie dobranym przez rodzica. Dwuznaki, takie
> jak „sz”, „cz” czy „rz”, są wskazywane razem, tak jak się je czyta. Po przejściu strzałki przez cały
> wyraz dziecko może go posłuchać.
>
> - lekcje od samogłosek i sylab do prostych wyrazów,
> - nagrania głosek (opcjonalnie) i polski syntezator mowy dla całych wyrazów,
> - duża, czytelna czcionka Andika i ciemny motyw,
> - bez presji czasu, bez punktów i bez kar,
> - panel rodzica za bramką: oceny przeczytanych wyrazów, historia i ustawienia tempa.
>
> Aplikacja działa bez internetu, nie ma reklam ani zakupów i nie zbiera żadnych danych.

- **Kategoria:** Edukacja. **Tagi:** czytanie, nauka, dla dzieci.
- **Polityka prywatności (URL):** publiczny adres `doc/polityka-prywatnosci.md`, np. przez GitHub Pages
  albo link do pliku, jeśli repozytorium jest publiczne.

## Grupa docelowa i treści (Families)

- **Grupy wiekowe:** 5 lat i mniej, 6–8 lat. Aplikacja nie jest kierowana do starszych.
- **Czy aplikacja może przypadkowo zainteresować dzieci:** tak, jest dla dzieci.
- **Reklamy:** nie. **Zakupy w aplikacji:** nie.
- **Program „Nauczyciele polecają”:** opcjonalnie, po pierwszych wydaniach.
- **Zgodność z zasadami Families:** brak SDK, brak sieci, brak zbierania danych, panel rodzica za bramką.

## Bezpieczeństwo danych (Data safety)

- **Czy aplikacja zbiera lub udostępnia dane użytkownika typu objętego formularzem:** nie.
  Oceny i ustawienia zostają na urządzeniu i nigdy go nie opuszczają, a Google uznaje takie dane za
  niezbierane.
- **Szyfrowanie przesyłanych danych:** nie dotyczy (aplikacja niczego nie przesyła).
- **Prośba o usunięcie danych:** nie dotyczy; dane usuwa odinstalowanie aplikacji.

## Klasyfikacja treści (IARC)

Kategoria: aplikacja edukacyjna / referencyjna. Na wszystkie pytania o przemoc, treści seksualne,
wulgaryzmy, substancje, hazard, interakcje użytkowników, udostępnianie lokalizacji i zakupy
odpowiedź „nie”. Oczekiwana klasyfikacja: PEGI 3.

## Uprawnienia

Brak uprawnień niebezpiecznych. Manifest usuwa `INTERNET`. Wpis `<queries>` dla `TTS_SERVICE`
służy tylko znalezieniu syntezatora mowy w systemie.

## Pierwsze wydanie w ścieżce testów wewnętrznych

Kroki tylko dla właściciela konta (Claude nie ma dostępu do Play Console ani do kluczy):

1. Utwórz klucz przesyłania (upload key), np.
   `keytool -genkeypair -v -keystore czytanie-upload.jks -alias czytanie -keyalg RSA -keysize 4096 -validity 10000`,
   i trzymaj kopię w bezpiecznym miejscu.
2. Dodaj w repozytorium sekrety: `CZYTANIE_KEYSTORE_BASE64` (`base64 -w0 czytanie-upload.jks`),
   `CZYTANIE_KEYSTORE_PASSWORD`, `CZYTANIE_KEY_ALIAS`, `CZYTANIE_KEY_PASSWORD`.
3. Uruchom workflow Release. Do wydania na GitHubie trafią `czytanie-X.Y.Z.apk` i
   `czytanie-X.Y.Z.aab`. Bez sekretów AAB jest podpisany kluczem debug i Play go odrzuci.
4. W Play Console: utwórz aplikację `pl.dev404.czytanie`, włącz podpisywanie aplikacji przez Google
   Play, uzupełnij powyższe formularze i prześlij AAB w ścieżce **Testy wewnętrzne**.
5. Opcjonalnie: automatyczne przesyłanie z CI. Utwórz konto usługi z dostępem do aplikacji w Play
   Console i dodaj jego klucz JSON jako sekret `PLAY_SERVICE_ACCOUNT_JSON`. Release wyśle wtedy AAB do
   ścieżki testów wewnętrznych sam. Pierwszy plik AAB Play przyjmuje tylko ręcznie (krok 4).

`versionCode` to numer uruchomienia workflow Release, więc rośnie z każdym wydaniem.
