# CasaCheck – ontwikkeling

## Vereisten
- JDK 17 of 21 (getest met Temurin 21), Android SDK platform 35 + build-tools 35.
- Gradle via de wrapper (8.11.1). AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2024.12.01.

## Bouwen
```bash
./gradlew :app:assembleDebug          # debug-APK (applicationId nl.ecannemeijer.casacheck.debug)
./gradlew :app:testDebugUnitTest      # unittests (JVM)
./gradlew :app:connectedDebugAndroidTest   # instrumented tests op emulator/toestel
./gradlew :app:assembleRelease        # ondertekende release (zie hieronder)
```
Uitvoer: `app/build/outputs/apk/<variant>/`.

## Release-ondertekening
De keystore en wachtwoorden staan **nooit** in de repository (`*.jks`, `signing.properties` staan in `.gitignore`).
`app/build.gradle.kts` leest `~/.casacheck/signing.properties` (of het pad uit de omgevingsvariabele
`CASACHECK_SIGNING_PROPERTIES`) met de sleutels `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
Zonder dat bestand wordt de release-APK niet ondertekend.

- De release-keystore (`casacheck-release.jks`, alias `casacheck`, RSA 4096, PKCS12) is aangemaakt op de bouwmachine
  (de Grok Bot-box) in `~/.casacheck/` met daarnaast `signing.properties`.
  **Maak hiervan een veilige back-up**: updates van de app moeten met dezelfde sleutel ondertekend worden.
- Om op een andere pc te ondertekenen: kopieer beide bestanden naar `%USERPROFILE%\.casacheck\` en pas `storeFile` aan.
- Controle: `apksigner verify --print-certs CasaCheck-1.0.0.apk`.

## Architectuur
- Eén module `app`, pakket `nl.ecannemeijer.casacheck`. Jetpack Compose + Material 3, Navigation Compose.
- Handmatige DI via `AppContainer` (in `CasaApp`): Room-database, `FileStore`, DataStore-instellingen, repositories,
  `ImportManager`, OCR (`MlKitOcrEngine`, gebundeld model), `BackupManager`, `PdfReportGenerator`.
- `domain/`: `CheckpointLibrary` (202 controlepunten), `ChecklistGenerator`, `CostCalculator` (geen dubbeltelling),
  `ScoreCalculator`, `PreferenceEvaluator`, `MoistureAnalyzer`.
- `data/importer/`: URL-herkenning, parsers (Idealista, Immobiliare, generieke meta, tekst), `PhotoDownloadWorker` (WorkManager).
- `ui/`: per scherm een package (properties, add, detail, inspection, photos, compare, reports, settings).
- Bestanden staan in app-intern geheugen; delen via `FileProvider` (`${applicationId}.files`). `allowBackup=false`.

## Teksten en illustraties
- UI-teksten (NL standaard, EN, IT) worden gegenereerd: `cd tools/content && python3 gen_ui.py`
  (bronnen `ui_strings.py` + alle `uis_*.py`; tuples `(key, nl, en, it)`).
- Inhoud controlepunten: `tools/content` (checklist/content-generators).
- Voorbeeldtekeningen (vector drawables `ill_*`): `python3 tools/art/gen_ill.py` (eigen tekenwerk).

## Bekende keuzes
- Geen Play Billing, geen Play Integrity, geen in-app updates (sideload).
- Geen omzeiling van bot-bescherming; bij 403 volgt een duidelijke melding met alternatieven.
