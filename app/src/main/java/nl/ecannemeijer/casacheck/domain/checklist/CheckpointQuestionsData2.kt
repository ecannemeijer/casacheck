package nl.ecannemeijer.casacheck.domain.checklist

// Exterior walls, facade, roof, gutters, windows, doors, electrical, water, sewer, septic, heating, hot water, gas.
internal fun questionsPart2(): Map<String, List<CheckpointQuestion>> = mapOf(
    "ext_masonry" to qs(
        "Di che materiale sono i muri: pietra, mattoni o blocchi?", "Van welk materiaal zijn de muren: natuursteen, baksteen of blokken?",
        "Le fughe o i giunti sono stati rifatti di recente?", "Zijn de voegen onlangs vernieuwd?",
    ),
    "ext_render" to qs(
        "Quando è stato rifatto l'intonaco esterno?", "Wanneer is het buitenpleisterwerk vernieuwd?",
        "Che tipo di intonaco è stato usato (calce, cemento)?", "Welk soort pleister is gebruikt (kalk, cement)?",
    ),
    "ext_wall_thickness" to qs(
        "Che spessore hanno i muri esterni?", "Hoe dik zijn de buitenmuren?",
        "In che anno è stata costruita la casa? Ci sono parti più recenti?", "In welk jaar is het huis gebouwd? Zijn er nieuwere delen?",
    ),
    "ext_vegetation" to qs(
        "Da quanto tempo c'è la pianta rampicante sul muro?", "Hoe lang groeit de klimplant al op de muur?",
        "Le radici hanno danneggiato l'intonaco o il tetto?", "Hebben de wortels het pleisterwerk of het dak beschadigd?",
    ),
    "fac_balconies_ledges" to qs(
        "I frontalini dei balconi e i cornicioni sono stati controllati?", "Zijn de balkonranden en kroonlijsten gecontroleerd?",
        "Sono mai caduti pezzi di intonaco o cemento?", "Zijn er ooit stukken pleister of beton naar beneden gevallen?",
    ),
    "fac_paint_condition" to qs(
        "Quando è stata tinteggiata la facciata?", "Wanneer is de gevel geschilderd?",
        "Ci sono vincoli sul colore della facciata (centro storico)?", "Zijn er voorschriften voor de gevelkleur (historisch centrum)?",
    ),
    "fac_shared_wall" to qs(
        "Il muro in comune con il vicino è in buono stato dall'altra parte?", "Is de gemeenschappelijke muur aan de kant van de buren in goede staat?",
        "Ci sono accordi o liti con i vicini per muri o tetti in comune?", "Zijn er afspraken of geschillen met de buren over gedeelde muren of daken?",
        "La casa confinante è abitata e mantenuta?", "Is het naastgelegen huis bewoond en onderhouden?",
    ),
    "roof_tiles" to qs(
        "Quando è stato rifatto o ripassato il manto di copertura?", "Wanneer is de dakbedekking vernieuwd of nagelopen?",
        "Ci sono coppi o tegole rotti o spostati?", "Zijn er kapotte of verschoven dakpannen?",
        "Sotto le tegole c'è una guaina impermeabile?", "Ligt er onder de pannen een waterkerende folie/laag?",
    ),
    "roof_ridge_sag" to qs(
        "Il colmo del tetto si è abbassato nel tempo?", "Is de nok van het dak in de loop van de tijd gezakt?",
        "Le travi del tetto sono state controllate o sostituite?", "Zijn de dakbalken gecontroleerd of vervangen?",
    ),
    "roof_chimney" to qs(
        "Quando è stata pulita l'ultima volta la canna fumaria?", "Wanneer is het rookkanaal voor het laatst geveegd?",
        "La canna fumaria ha una dichiarazione di conformità o una certificazione?", "Heeft het rookkanaal een conformiteitsverklaring of certificaat?",
        "Ci sono state infiltrazioni intorno al comignolo?", "Is er rond de schoorsteen lekkage geweest?",
    ),
    "roof_eaves" to qs(
        "Le travi e le tavole della gronda sono marce?", "Zijn de balken en planken van de dakrand verrot?",
        "Il cornicione è stato riparato di recente?", "Is de dakrand onlangs gerepareerd?",
    ),
    "roof_flat" to qs(
        "Quando è stata rifatta l'impermeabilizzazione del tetto piano o del terrazzo?", "Wanneer is de waterdichting van het platte dak of dakterras vernieuwd?",
        "C'è una garanzia per la guaina?", "Is er een garantie op de dakbedekking?",
        "Ci sono infiltrazioni nei locali sottostanti?", "Zijn er lekkages in de ruimtes eronder?",
    ),
    "roof_age" to qs(
        "In che anno è stato rifatto il tetto?", "In welk jaar is het dak vernieuwd?",
        "Il tetto è isolato? Con quale materiale?", "Is het dak geïsoleerd? Met welk materiaal?",
        "Ci sono fatture o documenti dei lavori sul tetto?", "Zijn er facturen of documenten van de dakwerkzaamheden?",
    ),
    "roof_skylights" to qs(
        "Il lucernario o l'abbaino sono stati autorizzati?", "Zijn het dakraam of de dakkapel met vergunning geplaatst?",
        "Entra acqua dal lucernario quando piove?", "Lekt het dakraam bij regen?",
    ),
    "gut_condition" to qs(
        "Quando sono state pulite le grondaie?", "Wanneer zijn de dakgoten schoongemaakt?",
        "Le grondaie perdono o traboccano?", "Lekken of overlopen de goten?",
    ),
    "gut_missing" to qs(
        "Perché in questo lato non ci sono grondaie?", "Waarom zitten er aan deze kant geen goten?",
        "Dove finisce l'acqua del tetto da questo lato?", "Waar gaat het dakwater aan deze kant naartoe?",
    ),
    "gut_overflow_marks" to qs(
        "Le grondaie traboccano durante i temporali?", "Lopen de goten over tijdens onweersbuien?",
        "Ci sono macchie d'acqua sui muri sotto le grondaie?", "Zijn er watervlekken op de muren onder de goten?",
    ),
    "dp_discharge" to qs(
        "I pluviali sono collegati alla fognatura o scaricano sul terreno?", "Zijn de regenpijpen op het riool aangesloten of lozen ze op het terrein?",
        "L'acqua dei pluviali arriva vicino alle fondazioni?", "Komt het water uit de regenpijpen vlak bij de fundering?",
    ),
    "dp_damage" to qs(
        "I pluviali rotti verranno sostituiti prima del rogito?", "Worden de kapotte regenpijpen vóór de overdracht vervangen?",
        "Di che materiale sono i pluviali (rame, lamiera, PVC)?", "Van welk materiaal zijn de regenpijpen (koper, zink/staal, pvc)?",
    ),
    "win_glazing" to qs(
        "Le finestre hanno il vetro doppio o singolo?", "Hebben de ramen dubbel of enkel glas?",
        "In che anno sono stati sostituiti gli infissi? Avete usato l'ecobonus?", "In welk jaar zijn de kozijnen vervangen? Is de ecobonus gebruikt?",
    ),
    "win_operation" to qs(
        "Tutte le finestre si aprono e chiudono bene?", "Gaan alle ramen goed open en dicht?",
        "Ci sono ferramenta o maniglie da sostituire?", "Moeten er beslag of grepen vervangen worden?",
    ),
    "win_shutters" to qs(
        "Le persiane e le tapparelle funzionano tutte?", "Werken alle luiken en rolluiken?",
        "Le cinghie o i motori delle tapparelle sono stati sostituiti?", "Zijn de banden of motoren van de rolluiken vervangen?",
    ),
    "win_bars_safety" to qs(
        "Le ringhiere e i parapetti sono a norma di altezza?", "Hebben de balustrades en borstweringen de voorgeschreven hoogte?",
        "Le inferriate si possono aprire dall'interno?", "Kunnen de tralies van binnenuit geopend worden?",
    ),
    "frm_rot" to qs(
        "Gli infissi in legno vengono trattati regolarmente? Quando l'ultima volta?", "Worden de houten kozijnen regelmatig behandeld? Wanneer voor het laatst?",
        "Ci sono parti marce da sostituire?", "Zijn er rotte delen die vervangen moeten worden?",
    ),
    "frm_seals" to qs(
        "Entra aria o acqua tra infisso e muro?", "Komt er lucht of water tussen kozijn en muur door?",
        "Le sigillature sono state rifatte?", "Zijn de kitnaden vernieuwd?",
    ),
    "frm_material" to qs(
        "Di che materiale sono gli infissi e quanti anni hanno?", "Van welk materiaal zijn de kozijnen en hoe oud zijn ze?",
        "C'è la documentazione degli infissi nuovi (trasmittanza, garanzia)?", "Is er documentatie van nieuwe kozijnen (isolatiewaarde, garantie)?",
    ),
    "door_entrance" to qs(
        "La porta d'ingresso è blindata? Di che classe?", "Is de voordeur een veiligheidsdeur? Welke klasse?",
        "Quante chiavi ci sono e chi ne ha una copia?", "Hoeveel sleutels zijn er en wie heeft een kopie?",
    ),
    "door_interior" to qs(
        "Le porte interne chiudono tutte bene?", "Sluiten alle binnendeuren goed?",
        "Le porte sono incluse nella vendita così come sono?", "Horen de deuren bij de verkoop zoals ze zijn?",
    ),
    "door_garage_gate" to qs(
        "Il cancello o la porta del garage sono automatici? Funzionano?", "Zijn het hek of de garagedeur automatisch? Werken ze?",
        "Ci sono telecomandi e un contratto di manutenzione?", "Zijn er afstandsbedieningen en een onderhoudscontract?",
    ),
    "el_panel" to qs(
        "C'è l'interruttore differenziale (salvavita) e funziona?", "Is er een aardlekschakelaar (salvavita) en werkt die?",
        "Quando è stato rifatto il quadro elettrico?", "Wanneer is de meterkast/het verdeelbord vernieuwd?",
    ),
    "el_wiring_age" to qs(
        "In che anno è stato rifatto l'impianto elettrico?", "In welk jaar is de elektrische installatie vernieuwd?",
        "I fili sono nei tubi sotto traccia o ci sono ancora cavi vecchi in tessuto?", "Liggen de draden in buizen in de muur of zijn er nog oude stoffen kabels?",
        "L'impianto è a norma?", "Voldoet de installatie aan de voorschriften?",
    ),
    "el_sockets" to qs(
        "Le prese hanno tutte la messa a terra?", "Zijn alle stopcontacten geaard?",
        "C'è l'impianto di terra con relativa verifica?", "Is er een aardingsinstallatie en is die gecontroleerd?",
    ),
    "el_certificate" to qs(
        "C'è la dichiarazione di conformità (DiCo) dell'impianto elettrico?", "Is er een conformiteitsverklaring (DiCo) van de elektrische installatie?",
        "Se manca, è possibile avere una dichiarazione di rispondenza (DiRi)?", "Als die ontbreekt, is er een verklaring van overeenstemming (DiRi) mogelijk?",
        "Chi è l'elettricista che ha fatto l'impianto?", "Welke elektricien heeft de installatie aangelegd?",
    ),
    "el_meter_power" to qs(
        "Quanti kW di potenza ha il contatore?", "Hoeveel kW aansluitvermogen heeft de meter?",
        "Il contratto della luce è attivo o è stato disdetto?", "Is het elektriciteitscontract actief of opgezegd?",
        "Quanto si spende in media di luce all'anno?", "Hoeveel kost de elektriciteit gemiddeld per jaar?",
    ),
    "el_outdoor" to qs(
        "Le prese e le luci esterne sono a tenuta stagna?", "Zijn de buitenstopcontacten en -lampen waterdicht?",
        "Le linee esterne sono protette da un differenziale?", "Zijn de buitengroepen beveiligd met een aardlekschakelaar?",
    ),
    "wat_supply" to qs(
        "L'acqua arriva dall'acquedotto comunale o da un pozzo/sorgente?", "Komt het water van de gemeentelijke waterleiding of van een put/bron?",
        "Se c'è un pozzo, è denunciato o autorizzato? È stata analizzata l'acqua?", "Als er een put is, is die aangemeld of vergund? Is het water geanalyseerd?",
        "D'estate l'acqua manca mai?", "Valt het water in de zomer weleens weg?",
    ),
    "wat_pipes" to qs(
        "Di che materiale sono le tubature (ferro zincato, rame, multistrato)?", "Van welk materiaal zijn de leidingen (gegalvaniseerd ijzer, koper, meerlaags)?",
        "Ci sono ancora tubi in piombo?", "Zijn er nog loden leidingen?",
        "In che anno è stato rifatto l'impianto idraulico?", "In welk jaar is het sanitair/de waterleiding vernieuwd?",
    ),
    "wat_pressure" to qs(
        "La pressione dell'acqua è sufficiente anche ai piani alti?", "Is de waterdruk ook op de bovenverdiepingen voldoende?",
        "C'è un'autoclave o un serbatoio di accumulo?", "Is er een hydrofoorpomp of een opslagtank?",
    ),
    "wat_frost" to qs(
        "D'inverno le tubature si sono mai gelate?", "Zijn de leidingen 's winters ooit bevroren?",
        "Le tubazioni esterne sono isolate o svuotabili?", "Zijn de buitenleidingen geïsoleerd of af te tappen?",
    ),
    "sew_connection" to qs(
        "La casa è allacciata alla fognatura pubblica?", "Is het huis aangesloten op de openbare riolering?",
        "C'è l'autorizzazione allo scarico?", "Is er een lozingsvergunning?",
    ),
    "sew_drains_smell" to qs(
        "Gli scarichi si intasano o fanno cattivo odore?", "Raken de afvoeren verstopt of stinken ze?",
        "Quando sono stati fatti spurgo o pulizia degli scarichi?", "Wanneer zijn de afvoeren voor het laatst gereinigd?",
    ),
    "sew_manholes" to qs(
        "Dove si trovano i pozzetti d'ispezione?", "Waar zitten de inspectieputten?",
        "I pozzetti sono stati puliti di recente?", "Zijn de putten onlangs schoongemaakt?",
    ),
    "sep_location" to qs(
        "Che tipo di impianto c'è: fossa biologica, Imhoff o depuratore?", "Wat voor systeem is er: septic tank, Imhoff-tank of zuiveringsinstallatie?",
        "Dove si trova esattamente la fossa?", "Waar ligt de tank precies?",
        "C'è l'autorizzazione allo scarico rilasciata dal Comune?", "Is er een lozingsvergunning van de gemeente?",
    ),
    "sep_maintenance" to qs(
        "Quando è stato fatto l'ultimo svuotamento (spurgo) della fossa?", "Wanneer is de tank voor het laatst geleegd?",
        "Avete le ricevute dello spurgo?", "Heeft u de bonnen van het legen?",
        "Ci sono mai stati odori o traboccamenti?", "Zijn er ooit stankproblemen of overlopen geweest?",
    ),
    "sep_dispersal" to qs(
        "Dove va l'acqua in uscita: subirrigazione, pozzo perdente o corso d'acqua?", "Waar gaat het uitgaande water heen: infiltratieveld, zinkput of waterloop?",
        "Il terreno intorno si impregna d'acqua?", "Raakt de grond eromheen verzadigd met water?",
    ),
    "heat_system" to qs(
        "Che tipo di riscaldamento c'è (metano, GPL, gasolio, pellet, pompa di calore)?", "Wat voor verwarming is er (aardgas, lpg, stookolie, pellets, warmtepomp)?",
        "Il riscaldamento è autonomo o centralizzato?", "Is de verwarming individueel of centraal (gedeeld)?",
        "Scalda bene tutte le stanze?", "Worden alle kamers goed warm?",
    ),
    "heat_boiler" to qs(
        "Quanti anni ha la caldaia?", "Hoe oud is de cv-ketel?",
        "C'è il libretto di impianto aggiornato?", "Is er een bijgewerkt installatieboekje (libretto di impianto)?",
        "Quando è stata fatta l'ultima manutenzione e il controllo fumi (bollino)?", "Wanneer was het laatste onderhoud en de rookgasmeting (bollino)?",
        "La caldaia è a condensazione?", "Is het een hoogrendementsketel (condensatieketel)?",
    ),
    "heat_radiators" to qs(
        "I termosifoni scaldano tutti in modo uniforme?", "Worden alle radiatoren gelijkmatig warm?",
        "Ci sono valvole termostatiche?", "Zijn er thermostaatkranen?",
        "Ci sono state perdite dall'impianto di riscaldamento?", "Zijn er lekkages in de verwarmingsinstallatie geweest?",
    ),
    "heat_stove_chimney" to qs(
        "La stufa o il camino sono stati installati da un tecnico con dichiarazione di conformità?", "Is de kachel of open haard door een installateur geplaatst met conformiteitsverklaring?",
        "Quando è stata pulita la canna fumaria?", "Wanneer is het rookkanaal geveegd?",
        "La stufa è registrata nel libretto di impianto?", "Staat de kachel in het installatieboekje?",
        "Quanta legna o quanti sacchi di pellet servono in un inverno?", "Hoeveel hout of zakken pellets zijn er nodig in een winter?",
    ),
    "heat_costs" to qs(
        "Quanto avete speso di riscaldamento l'ultimo inverno?", "Hoeveel heeft u afgelopen winter aan verwarming uitgegeven?",
        "Posso vedere alcune bollette del gas o del GPL?", "Mag ik een paar gas- of lpg-rekeningen zien?",
    ),
    "hw_system" to qs(
        "Come viene prodotta l'acqua calda: caldaia, boiler elettrico o pompa di calore?", "Hoe wordt warm water gemaakt: cv-ketel, elektrische boiler of warmtepomp?",
        "L'acqua calda arriva subito in bagno e in cucina?", "Komt het warme water snel in badkamer en keuken?",
    ),
    "hw_solar" to qs(
        "I pannelli solari o fotovoltaici sono di proprietà o in leasing?", "Zijn de zonnecollectoren of zonnepanelen eigendom of geleased?",
        "Quanto producono in un anno? C'è un contratto con il GSE?", "Hoeveel leveren ze per jaar op? Is er een contract met de GSE?",
        "Ci sono la dichiarazione di conformità e la garanzia?", "Zijn er een conformiteitsverklaring en garantie?",
    ),
    "hw_cylinder_location" to qs(
        "Quanti anni ha il boiler?", "Hoe oud is de boiler?",
        "È stato fatto il controllo dell'anodo o la decalcificazione?", "Is de anode gecontroleerd of de boiler ontkalkt?",
    ),
    "gas_supply" to qs(
        "La casa è allacciata al metano o usa un serbatoio GPL?", "Is het huis aangesloten op aardgas of gebruikt het een lpg-tank?",
        "Il serbatoio GPL è di proprietà o in comodato del fornitore?", "Is de lpg-tank eigendom of in bruikleen van de leverancier?",
        "Il serbatoio ha le verifiche periodiche in regola?", "Zijn de periodieke keuringen van de tank in orde?",
    ),
    "gas_hoses_vents" to qs(
        "Il tubo flessibile del gas è a norma e non scaduto?", "Is de gasslang goedgekeurd en niet verlopen?",
        "Ci sono le aperture di ventilazione dove c'è il gas?", "Zijn er ventilatieopeningen waar gas wordt gebruikt?",
    ),
    "gas_certificate" to qs(
        "C'è la dichiarazione di conformità dell'impianto gas?", "Is er een conformiteitsverklaring van de gasinstallatie?",
        "Quando è stata fatta l'ultima prova di tenuta?", "Wanneer is voor het laatst de dichtheid getest?",
    ),
    "gas_co_signs" to qs(
        "Ci sono mai stati problemi con fumi o monossido di carbonio?", "Zijn er ooit problemen geweest met rookgassen of koolmonoxide?",
        "C'è un rilevatore di gas o di monossido?", "Is er een gas- of koolmonoxidemelder?",
    ),
)