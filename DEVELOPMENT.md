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

- **Release-sleutel (vanaf 1.0.0):** `casacheck-release.jks` (alias `casacheck`, RSA 4096, PKCS12) op de laptop van Enrico in
  `%USERPROFILE%\.casacheck\`, met daarnaast `signing.properties`. Certificaat-SHA-256:
  `c9c0d3243d4a323fa2428d788490d182dabe4951718b6927cd32e7362b29db9f`.
  **Maak hiervan een veilige back-up** (bijv. versleuteld op een USB-stick of in een wachtwoordkluis): updates van de app
  moeten met dezelfde sleutel ondertekend worden, anders moet de app eerst verwijderd worden.
- Een eerdere sleutel op de Grok Bot-box (`/workspace/casacheck-keys/`, cert-SHA-256 `5b7559...cd02`) is nooit gebruikt voor een
  verspreide build en is **niet** de release-sleutel.
- Om op een andere pc te ondertekenen: kopieer beide bestanden naar `%USERPROFILE%\.casacheck\` (of `~/.casacheck/`) en pas `storeFile` aan.
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

## Updates
- De app controleert `https://api.github.com/repos/ecannemeijer/casacheck/releases/latest` (zonder token) bij het starten/hervatten, hooguit elke 6 uur, en handmatig via Instellingen.
- Een release moet een tag `vX.Y.Z` hebben (hoger dan `versionName`), een hogere `versionCode` en een `.apk`-asset (bij voorkeur `CasaCheck-X.Y.Z.apk`). Prereleases en drafts worden genegeerd.
- De gedownloade APK wordt alleen aangeboden als pakketnaam en ondertekeningscertificaat gelijk zijn aan de geïnstalleerde app; daarna installeert de systeeminstaller (gebruiker bevestigt). Rechten: `REQUEST_INSTALL_PACKAGES`.
- Debug-builds controleren niet op updates.
- Releasestappen: versionCode/versionName ophogen, `assembleRelease`, `apksigner verify`, GitHub-release `vX.Y.Z` (latest) met de APK als asset.
