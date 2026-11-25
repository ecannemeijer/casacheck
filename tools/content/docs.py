# Italian documents (section 22). Each: code, italian name, nl/en/it dicts with name, why, check, who; question in it + nl + en.
DOCS = []
def D(code, it_name, nl, en, it, q_it, q_nl, q_en):
    assert len(nl) == 4 and len(en) == 4 and len(it) == 4
    DOCS.append(dict(code=code, it_name=it_name, nl=nl, en=en, it=it, q_it=q_it, q_nl=q_nl, q_en=q_en))

D("VISURA", "Visura catastale",
  ("Kadastraal uittreksel", "Toont eigenaren, kadastrale gegevens, categorie en kadastrale waarde van de woning.", "Kloppen eigenaren, adres, categorie (bijv. A/2, A/3) en het aantal ruimtes (vani) met de werkelijkheid?", "Notaris, geometra"),
  ("Cadastral extract", "Shows owners, cadastral data, category and cadastral value of the property.", "Do owners, address, category (e.g. A/2, A/3) and number of rooms (vani) match reality?", "Notary, geometra"),
  ("Visura catastale", "Indica intestatari, dati catastali, categoria e rendita dell'immobile.", "Intestatari, indirizzo, categoria (es. A/2, A/3) e vani corrispondono alla realtà?", "Notaio, geometra"),
  "Può inviarmi la visura catastale aggiornata?", "Kunt u mij het actuele kadastrale uittreksel sturen?", "Can you send me the up-to-date cadastral extract?")
D("PLANIMETRIA", "Planimetria catastale",
  ("Kadastrale plattegrond", "De officiële plattegrond die bij het kadaster is geregistreerd.", "Komt de indeling (muren, ramen, deuren, aanbouwen) overeen met wat je ziet?", "Geometra"),
  ("Cadastral floor plan", "The official floor plan registered with the cadastre.", "Does the layout (walls, windows, doors, extensions) match what you see?", "Geometra"),
  ("Planimetria catastale", "La planimetria ufficiale depositata al catasto.", "La distribuzione (muri, finestre, porte, ampliamenti) corrisponde allo stato di fatto?", "Geometra"),
  "La planimetria catastale corrisponde allo stato attuale dell'immobile?", "Komt de kadastrale plattegrond overeen met de huidige situatie?", "Does the cadastral floor plan match the current state of the property?")
D("CONF_CATASTALE", "Conformità catastale",
  ("Kadastrale conformiteit", "Verklaring dat de kadastrale gegevens en plattegrond overeenkomen met de werkelijkheid. Verplicht bij de akte.", "Is er een verklaring van een technicus? Let op: dit zegt NIETS over de stedenbouwkundige legaliteit.", "Geometra, notaris"),
  ("Cadastral conformity", "Statement that cadastral data and plan match reality. Required for the deed.", "Is there a technician's statement? Note: this says NOTHING about planning legality.", "Geometra, notary"),
  ("Conformità catastale", "Dichiarazione che dati e planimetria catastali corrispondono allo stato di fatto. Obbligatoria per il rogito.", "C'è una dichiarazione di un tecnico? Attenzione: non dice NULLA sulla legittimità urbanistica.", "Geometra, notaio"),
  "Esiste una dichiarazione di conformità catastale firmata da un tecnico?", "Is er een kadastrale conformiteitsverklaring getekend door een technicus?", "Is there a cadastral conformity statement signed by a technician?")
D("CONF_URBANISTICA", "Conformità urbanistica",
  ("Stedenbouwkundige conformiteit", "Bevestigt dat de woning volgens de vergunningen is gebouwd. Kadastrale conformiteit betekent niet automatisch stedenbouwkundige legaliteit.", "Laat een technicus de vergunningsdossiers bij de gemeente vergelijken met de werkelijke situatie.", "Geometra, architect"),
  ("Planning conformity", "Confirms the property was built according to its permits. Cadastral conformity does not automatically mean planning legality.", "Have a technician compare the municipal permit files with the actual situation.", "Geometra, architect"),
  ("Conformità urbanistica", "Conferma che l'immobile è conforme ai titoli edilizi. La conformità catastale non implica automaticamente quella urbanistica.", "Fai confrontare a un tecnico le pratiche comunali con lo stato di fatto.", "Geometra, architetto"),
  "È stata verificata la conformità urbanistica con l'accesso agli atti in Comune?", "Is de stedenbouwkundige conformiteit gecontroleerd via de dossiers bij de gemeente?", "Has planning conformity been checked through the municipal records?")
D("AGIBILITA", "Agibilità",
  ("Bewoonbaarheidsverklaring", "Bevestigt dat het gebouw veilig, hygiënisch en bewoonbaar is.", "Is er een certificaat of segnalazione certificata di agibilità, ook na verbouwingen?", "Geometra, gemeente"),
  ("Certificate of habitability", "Confirms the building is safe, hygienic and habitable.", "Is there a certificate or segnalazione certificata di agibilità, also after alterations?", "Geometra, municipality"),
  ("Agibilità", "Attesta sicurezza, igiene e abitabilità dell'edificio.", "Esiste il certificato o la SCA di agibilità, anche dopo eventuali lavori?", "Geometra, Comune"),
  "L'immobile ha il certificato di agibilità?", "Heeft de woning een bewoonbaarheidsverklaring?", "Does the property have a certificate of habitability?")
D("APE", "Attestato di Prestazione Energetica (APE)",
  ("Energielabel", "Verplicht bij verkoop; geeft energieklasse en geschat verbruik.", "Is het APE geldig (10 jaar) en klopt de beschrijving van installaties?", "Certificeerder"),
  ("Energy performance certificate", "Required for sale; gives energy class and estimated consumption.", "Is the APE valid (10 years) and does it describe the systems correctly?", "Certifier"),
  ("Attestato di Prestazione Energetica", "Obbligatorio per la vendita; indica classe e consumi stimati.", "L'APE è valido (10 anni) e descrive correttamente gli impianti?", "Certificatore"),
  "Può mostrarmi l'APE in corso di validità?", "Kunt u mij het geldige energielabel laten zien?", "Can you show me the valid energy certificate?")
D("DICO_ELETTRICO", "Dichiarazione di conformità impianto elettrico",
  ("Conformiteitsverklaring elektra", "Verklaring van de installateur dat de elektrische installatie aan de normen voldoet (DM 37/08).", "Is er een verklaring of een dichiarazione di rispondenza? Van wanneer?", "Erkende elektricien"),
  ("Electrical conformity declaration", "Installer's declaration that the electrical system meets standards (DM 37/08).", "Is there a declaration or a dichiarazione di rispondenza? From when?", "Licensed electrician"),
  ("Dichiarazione di conformità impianto elettrico", "Dichiarazione dell'installatore che l'impianto è a norma (DM 37/08).", "C'è la DiCo o una dichiarazione di rispondenza? Di quando?", "Elettricista abilitato"),
  "L'impianto elettrico ha la dichiarazione di conformità?", "Heeft de elektrische installatie een conformiteitsverklaring?", "Does the electrical system have a conformity declaration?")
D("DICO_GAS", "Dichiarazione di conformità impianto gas",
  ("Conformiteitsverklaring gas", "Verklaring dat de gasinstallatie veilig en volgens de normen is aangelegd.", "Is er een verklaring, en is de ketel recent onderhouden (libretto)?", "Erkende installateur"),
  ("Gas conformity declaration", "Declaration that the gas system was installed safely and to standard.", "Is there a declaration, and was the boiler recently serviced (libretto)?", "Licensed installer"),
  ("Dichiarazione di conformità impianto gas", "Dichiarazione che l'impianto gas è sicuro e a norma.", "C'è la dichiarazione e la caldaia è stata revisionata di recente (libretto)?", "Installatore abilitato"),
  "L'impianto del gas è certificato e quando è stata fatta l'ultima manutenzione della caldaia?", "Is de gasinstallatie gecertificeerd en wanneer is de ketel voor het laatst onderhouden?", "Is the gas system certified and when was the boiler last serviced?")
D("TITOLI_EDILIZI", "Titoli edilizi (licenze, concessioni, permessi)",
  ("Bouwvergunningen", "Alle vergunningen voor bouw en verbouwingen van de woning.", "Zijn er vergunningen voor alle verbouwingen, en komen ze overeen met de situatie?", "Geometra, architect"),
  ("Building permits", "All permits for construction and alterations of the property.", "Are there permits for all alterations and do they match the situation?", "Geometra, architect"),
  ("Titoli edilizi", "Tutti i titoli per costruzione e modifiche dell'immobile.", "Ci sono titoli per tutti i lavori e corrispondono allo stato di fatto?", "Geometra, architetto"),
  "Quali titoli edilizi esistono per l'immobile e per i lavori eseguiti?", "Welke bouwvergunningen zijn er voor de woning en de uitgevoerde werkzaamheden?", "Which building permits exist for the property and the works carried out?")
D("AMPLIAMENTI", "Legittimità di ampliamenti e annessi",
  ("Legale status van aanbouwen", "Aanbouwen, veranda's en bijgebouwen zijn vaak zonder vergunning gebouwd (abuso edilizio).", "Is elke aanbouw vergund of gelegaliseerd (condono, sanatoria)?", "Geometra"),
  ("Legal status of extensions", "Extensions, verandas and outbuildings are often built without permits (abuso edilizio).", "Is every extension permitted or legalised (condono, sanatoria)?", "Geometra"),
  ("Legittimità degli ampliamenti", "Ampliamenti, verande e annessi sono spesso abusivi.", "Ogni ampliamento è autorizzato o sanato (condono, sanatoria)?", "Geometra"),
  "Tutti gli ampliamenti e gli annessi sono regolari o condonati?", "Zijn alle aanbouwen en bijgebouwen legaal of gelegaliseerd?", "Are all extensions and outbuildings legal or legalised?")
D("PROPRIETA", "Atto di provenienza / situazione proprietaria",
  ("Eigendomssituatie", "Wie de eigenaren zijn en hoe zij eigenaar zijn geworden (koop, erfenis).", "Tekenen alle eigenaren? Is een erfenis (successione) volledig afgehandeld?", "Notaris"),
  ("Ownership", "Who the owners are and how they became owners (purchase, inheritance).", "Will all owners sign? Has any inheritance (successione) been fully settled?", "Notary"),
  ("Situazione proprietaria", "Chi sono i proprietari e come lo sono diventati (acquisto, eredità).", "Firmano tutti i proprietari? La successione è completa?", "Notaio"),
  "Chi sono i proprietari e la successione è stata completata?", "Wie zijn de eigenaren en is de erfenis afgehandeld?", "Who are the owners and has the inheritance been settled?")
D("SERVITU", "Servitù",
  ("Erfdienstbaarheden", "Rechten van anderen op het perceel, zoals overpad, leidingen of afvoer.", "Staan er servitù in de akte of op de kaart? Zijn er paden of leidingen van derden?", "Notaris, geometra"),
  ("Easements", "Rights of others over the plot, such as passage, pipes or drainage.", "Are servitù in the deed or on the map? Are there third-party paths or pipes?", "Notary, geometra"),
  ("Servitù", "Diritti di terzi sul fondo, come passaggio, condotte o scarichi.", "Ci sono servitù nell'atto o in mappa? Passaggi o condotte di terzi?", "Notaio, geometra"),
  "Esistono servitù di passaggio o di altro tipo sul terreno?", "Zijn er erfdienstbaarheden zoals recht van overpad op het terrein?", "Are there easements such as rights of way on the land?")
D("IPOTECHE", "Ipoteche e gravami",
  ("Hypotheken en lasten", "Hypotheken, beslagen of andere lasten die op de woning rusten.", "Laat de notaris een visura ipotecaria doen en controleer hoe lasten worden doorgehaald.", "Notaris"),
  ("Mortgages and charges", "Mortgages, liens or other charges on the property.", "Have the notary run a visura ipotecaria and check how charges will be cancelled.", "Notary"),
  ("Ipoteche e gravami", "Ipoteche, pignoramenti o altri gravami sull'immobile.", "Fai fare al notaio una visura ipotecaria e verifica come saranno cancellati.", "Notaio"),
  "Ci sono ipoteche o altri gravami sull'immobile?", "Rusten er hypotheken of andere lasten op de woning?", "Are there mortgages or other charges on the property?")
D("CONFINI", "Estratto di mappa e confini",
  ("Perceelgrenzen", "Kadasterkaart met de perceelgrenzen (estratto di mappa).", "Komen hekken en muren overeen met de kaart? Welke percelen horen bij de koop?", "Geometra"),
  ("Plot boundaries", "Cadastral map with the plot boundaries (estratto di mappa).", "Do fences and walls match the map? Which parcels are included in the sale?", "Geometra"),
  ("Estratto di mappa e confini", "Mappa catastale con i confini (estratto di mappa).", "Recinzioni e muri corrispondono alla mappa? Quali particelle sono incluse?", "Geometra"),
  "Può fornirmi l'estratto di mappa con tutte le particelle incluse nella vendita?", "Kunt u de kadasterkaart geven met alle percelen die bij de verkoop horen?", "Can you provide the cadastral map with all parcels included in the sale?")
D("RISCHIO_ALLUVIONE", "Rischio idraulico (PAI / PGRA)",
  ("Overstromingsrisico", "Officiële kaarten van de regio/het stroomgebied tonen overstromingsgevaar.", "Ligt het perceel in een gevarenzone? Visuele inspectie bewijst niet dat er geen risico is.", "Gemeente, geoloog"),
  ("Flood risk", "Official regional/basin maps show flood hazard.", "Is the plot in a hazard zone? A visual inspection does not prove there is no risk.", "Municipality, geologist"),
  ("Rischio idraulico", "Le carte ufficiali (PAI/PGRA) indicano la pericolosità idraulica.", "Il terreno è in area a rischio? L'ispezione visiva non prova l'assenza di rischio.", "Comune, geologo"),
  "L'immobile ricade in un'area a rischio idraulico secondo il PAI?", "Ligt de woning volgens het PAI in een overstromingsgebied?", "Is the property in a flood-risk area according to the PAI?")
D("RISCHIO_FRANA", "Rischio frana (PAI / IFFI)",
  ("Aardverschuivingsrisico", "Officiële kaarten (PAI, IFFI) tonen bekende aardverschuivingen en gevarenzones.", "Staat het perceel of de toegangsweg in een frana-zone? Vraag een geologisch rapport bij twijfel.", "Gemeente, geoloog"),
  ("Landslide risk", "Official maps (PAI, IFFI) show known landslides and hazard zones.", "Is the plot or access road in a landslide zone? Ask for a geological report if in doubt.", "Municipality, geologist"),
  ("Rischio frana", "Le carte ufficiali (PAI, IFFI) indicano frane note e aree a rischio.", "Il terreno o la strada sono in area di frana? Nel dubbio chiedi una relazione geologica.", "Comune, geologo"),
  "Ci sono frane note nella zona o vincoli idrogeologici?", "Zijn er bekende aardverschuivingen of hydrogeologische beperkingen in de buurt?", "Are there known landslides or hydrogeological restrictions in the area?")
D("RISCHIO_SISMICO", "Classificazione sismica",
  ("Seismisch risico", "De seismische zone van de gemeente (1 hoog – 4 laag).", "In welke zone ligt de gemeente? Zijn er versterkingen uitgevoerd?", "Ingenieur"),
  ("Seismic risk", "The seismic zone of the municipality (1 high – 4 low).", "Which zone is the municipality in? Has strengthening been carried out?", "Engineer"),
  ("Classificazione sismica", "La zona sismica del Comune (1 alta – 4 bassa).", "In che zona è il Comune? Sono stati fatti interventi di miglioramento?", "Ingegnere"),
  "In che zona sismica si trova il Comune e sono stati fatti interventi antisismici?", "In welke seismische zone ligt de gemeente en is de woning versterkt?", "Which seismic zone is the municipality in and has the house been strengthened?")
D("SPESE_COMUNI", "Spese condominiali / comuni",
  ("Gezamenlijke kosten", "Kosten voor gedeelde delen (condominio, gedeelde weg, gedeelde put).", "Hoeveel per jaar? Zijn er besloten maar nog niet betaalde werken of achterstanden?", "Amministratore, notaris"),
  ("Shared costs", "Costs for shared parts (condominium, shared road, shared well).", "How much per year? Are there approved but unpaid works or arrears?", "Administrator, notary"),
  ("Spese comuni", "Spese per parti comuni (condominio, strada o pozzo condivisi).", "Quanto all'anno? Ci sono lavori deliberati non pagati o morosità?", "Amministratore, notaio"),
  "Quali sono le spese comuni annuali e ci sono lavori già deliberati?", "Wat zijn de jaarlijkse gezamenlijke kosten en zijn er al werken besloten?", "What are the annual shared costs and have works already been approved?")
D("OBBLIGHI_MANUTENZIONE", "Obblighi di manutenzione",
  ("Onderhoudsverplichtingen", "Verplichtingen voor onderhoud van wegen, keermuren, greppels of bossen.", "Moet je meebetalen aan een privéweg, keermuur of consorzio?", "Notaris, gemeente"),
  ("Maintenance obligations", "Obligations to maintain roads, retaining walls, ditches or woods.", "Do you have to contribute to a private road, retaining wall or consorzio?", "Notary, municipality"),
  ("Obblighi di manutenzione", "Obblighi di manutenzione di strade, muri, fossi o boschi.", "Si deve contribuire a una strada privata, a muri o a un consorzio?", "Notaio, Comune"),
  "Ci sono obblighi di manutenzione per strade private, muri o consorzi?", "Zijn er onderhoudsverplichtingen voor privéwegen, muren of een consortium?", "Are there maintenance obligations for private roads, walls or a consortium?")
D("ACCESSO", "Diritti di accesso",
  ("Toegangsrechten", "Het recht om de woning via de weg te bereiken, vooral als de weg over andermans grond loopt.", "Is de toegang via openbare weg of via een vastgelegd recht van overpad?", "Notaris"),
  ("Access rights", "The right to reach the house by road, especially when the road crosses someone else's land.", "Is access via a public road or via a registered right of way?", "Notary"),
  ("Diritti di accesso", "Il diritto di raggiungere la casa, soprattutto se la strada passa su terreni altrui.", "L'accesso è da strada pubblica o da servitù di passaggio trascritta?", "Notaio"),
  "L'accesso alla casa è da strada pubblica o tramite servitù di passaggio?", "Is de toegang tot het huis via een openbare weg of via een recht van overpad?", "Is access to the house via a public road or a right of way?")
