# CasaCheck – Woninginspectie

Android-app (sideload-APK) om woningen in Italië te verzamelen, te bezichtigen en systematisch te inspecteren.
Alles werkt offline en lokaal op het toestel: geen account, geen cloud, geen Google Play-diensten.

## Functies
- **Woningen toevoegen** via link (Idealista / Immobiliare.it, ook via *Delen* vanuit de browser), geplakte tekst,
  screenshots (OCR op het toestel, ML Kit gebundeld) of handmatig. Voorbeeldscherm met "automatisch gevonden / niet gevonden".
- **Woninglijst** met zoeken, sorteren (9 opties), filters (favorieten, ernstig, niet gestart, archief), dupliceren, archiveren, vergelijken.
- **Woningdetail** met 11 tabbladen: overzicht (score, waarschuwingen, kosten, wensen, bezichtigingen), advertentie
  (bijwerken met wijzigingsvoorstel, prijsverloop), foto's, inspectie, ruimtes, gebreken, metingen (vocht en scheuren),
  kosten, documenten, omgeving en rapport.
- **Inspectiewizard** met 202 controlepunten (snelle eerste bezichtiging, volledig, per ruimte, per categorie, twijfelpunten),
  grote statusknoppen, uitleg, stappen, voorbeeldtekeningen, foto's koppelen.
- **Camera** (CameraX) met reeksopnames, **annotatie** (pijl, cirkel, rechthoek, vrije hand, tekst, ongedaan maken/opnieuw;
  origineel blijft bewaard) en **foto's vergelijken** met gesynchroniseerde zoom.
- **Gebreken** worden automatisch aangemaakt uit controlepunten; **kosten** zonder dubbeltelling (projecten, offertes,
  aankoopkosten, onvoorzien); **vochtmetingen** met patroonanalyse; **scheuren** met vervolgmetingen.
- **Makelaarsvragen** in het Italiaans, groot weergeven of laten voorlezen (TTS).
- **Vergelijken** van 2–4 woningen met instelbare weging; **wensen** (harde eis / wens).
- **PDF-rapporten** (8 soorten) in NL / EN / IT; delen en afdrukken.
- **Back-up / herstel** als één ZIP-bestand met integriteitscontrole en bescherming tegen onveilige paden.
- App-taal NL / EN / IT, licht/donker thema.

## Belangrijk
De score en rapporten zijn een hulpmiddel voor je eigen afweging, **geen bouwkundig rapport** en geen vervanging van een
geometra, ingegnere of perito. Bedragen zijn indicatief.

## Importeren van advertenties
Idealista (Cloudflare) en Immobiliare.it (DataDome) blokkeren automatische toegang vaak met HTTP 403. De app herkent dat,
probeert niets te omzeilen en biedt dan direct alternatieven: tekst plakken, screenshots (OCR) of handmatig invoeren.

## Installeren
Zet op het toestel *Installeren van onbekende apps* aan voor de bestandsbeheerder en open `CasaCheck-1.0.0.apk`.
Minimaal Android 8.0 (API 26).

Zie [DEVELOPMENT.md](DEVELOPMENT.md) voor bouwen en ontwikkelen.
