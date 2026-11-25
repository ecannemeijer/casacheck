package nl.ecannemeijer.casacheck.domain.checklist

// Access, surroundings, amenities, internet, mobile, building documents, legal documents.
internal fun questionsPart4(): Map<String, List<CheckpointQuestion>> = mapOf(
    "acc_road" to qs(
        "La strada di accesso è asfaltata, sterrata, privata o comunale?", "Is de toegangsweg geasfalteerd, onverhard, privé of gemeentelijk?",
        "Chi paga la manutenzione della strada?", "Wie betaalt het onderhoud van de weg?",
        "Esiste un consorzio stradale con quote annuali?", "Is er een wegenconsortium met jaarlijkse bijdragen?",
    ),
    "acc_winter" to qs(
        "D'inverno la strada viene pulita dalla neve? Da chi?", "Wordt de weg 's winters sneeuwvrij gemaakt? Door wie?",
        "Ci sono giorni in cui non si riesce a raggiungere la casa?", "Zijn er dagen dat het huis onbereikbaar is?",
    ),
    "acc_emergency" to qs(
        "Un'ambulanza o i vigili del fuoco possono arrivare fino alla casa?", "Kunnen een ambulance of de brandweer tot aan het huis komen?",
        "Quanto tempo ci mette di solito un'ambulanza ad arrivare qui?", "Hoe lang doet een ambulance er meestal over om hier te komen?",
    ),
    "acc_parking" to qs(
        "Dove si può parcheggiare? Il posto auto è di proprietà?", "Waar kan geparkeerd worden? Is de parkeerplaats eigendom?",
        "Serve un permesso per parcheggiare in paese (ZTL)?", "Is een vergunning nodig om in het dorp te parkeren (ZTL)?",
    ),
    "acc_steps_mobility" to qs(
        "Quanti gradini ci sono dalla strada alla porta?", "Hoeveel treden zijn er van de straat tot de deur?",
        "Si potrebbe installare un montascale o un ascensore?", "Zou er een traplift of lift geplaatst kunnen worden?",
    ),
    "acc_public_transport" to qs(
        "Dov'è la fermata dell'autobus più vicina e quante corse ci sono?", "Waar is de dichtstbijzijnde bushalte en hoe vaak rijdt de bus?",
        "Quanto dista la stazione ferroviaria più vicina?", "Hoe ver is het dichtstbijzijnde treinstation?",
    ),
    "acc_deliveries" to qs(
        "I corrieri consegnano fino alla casa?", "Bezorgen koeriers tot aan het huis?",
        "Un camion può arrivare per un trasloco o lavori?", "Kan een vrachtwagen hier komen voor een verhuizing of werkzaamheden?",
    ),
    "sur_noise" to qs(
        "Ci sono rumori di strade, ferrovie, fabbriche o feste estive?", "Is er geluid van wegen, spoor, fabrieken of zomerfeesten?",
        "Ci sono cani dei vicini che abbaiano spesso?", "Blaffen de honden van de buren vaak?",
    ),
    "sur_smell" to qs(
        "Ci sono allevamenti, depuratori o discariche nelle vicinanze?", "Zijn er veehouderijen, waterzuiveringen of stortplaatsen in de buurt?",
        "In certi periodi dell'anno si sentono cattivi odori o si spargono letame o pesticidi?", "Zijn er in bepaalde periodes stankoverlast of wordt mest of bestrijdingsmiddel gespoten?",
    ),
    "sur_neighbours" to qs(
        "Chi sono i vicini? Abitano qui tutto l'anno?", "Wie zijn de buren? Wonen ze hier het hele jaar?",
        "Ci sono mai stati problemi o liti con i vicini?", "Zijn er ooit problemen of ruzies met de buren geweest?",
    ),
    "sur_sun_view" to qs(
        "La vista potrebbe essere coperta da nuove costruzioni?", "Kan het uitzicht door nieuwbouw worden geblokkeerd?",
        "Quante ore di sole ci sono d'inverno?", "Hoeveel uur zon is er in de winter?",
    ),
    "sur_future_dev" to qs(
        "Sono previste nuove costruzioni, strade o impianti nella zona (piano regolatore)?", "Zijn er nieuwe gebouwen, wegen of installaties gepland in de buurt (bestemmingsplan)?",
        "Sono previsti parchi eolici, fotovoltaici o antenne qui vicino?", "Zijn er windparken, zonneparken of zendmasten gepland in de buurt?",
    ),
    "sur_hunting_wildlife" to qs(
        "Si caccia vicino alla casa? In quale periodo?", "Wordt er bij het huis gejaagd? In welke periode?",
        "Ci sono cinghiali, lupi o altri animali che entrano nel terreno?", "Komen er wilde zwijnen, wolven of andere dieren op het terrein?",
    ),
    "sur_fire_risk" to qs(
        "Ci sono mai stati incendi boschivi qui vicino?", "Zijn er hier in de buurt ooit bosbranden geweest?",
        "Ci sono obblighi di pulizia della vegetazione intorno alla casa?", "Zijn er verplichtingen om de begroeiing rond het huis te onderhouden?",
    ),
    "sur_seismic" to qs(
        "In che zona sismica si trova il Comune?", "In welke seismische zone ligt de gemeente?",
        "La casa ha avuto danni in terremoti passati? Sono stati riparati?", "Heeft het huis schade gehad bij eerdere aardbevingen? Is die hersteld?",
        "Sono stati fatti interventi di miglioramento sismico?", "Zijn er aardbevingsversterkende maatregelen uitgevoerd?",
    ),
    "ame_shops" to qs(
        "Dov'è il negozio di alimentari o il supermercato più vicino?", "Waar is de dichtstbijzijnde levensmiddelenwinkel of supermarkt?",
        "I negozi del paese restano aperti anche d'inverno?", "Blijven de winkels in het dorp ook 's winters open?",
    ),
    "ame_health" to qs(
        "Dove sono il medico di base, la farmacia e il pronto soccorso più vicini?", "Waar zijn de dichtstbijzijnde huisarts, apotheek en spoedeisende hulp?",
        "Quanto tempo ci vuole per arrivare all'ospedale?", "Hoe lang duurt het om bij het ziekenhuis te komen?",
    ),
    "ame_schools" to qs(
        "Dove sono le scuole più vicine? C'è uno scuolabus?", "Waar zijn de dichtstbijzijnde scholen? Is er een schoolbus?",
        "C'è un asilo nido o una scuola materna in paese?", "Is er een kinderdagverblijf of kleuterschool in het dorp?",
    ),
    "ame_utilities" to qs(
        "Luce, acqua e gas sono intestati e attivi? Si possono volturare?", "Staan elektriciteit, water en gas op naam en zijn ze actief? Kunnen ze worden overgezet?",
        "Ci sono bollette non pagate?", "Zijn er onbetaalde rekeningen?",
    ),
    "ame_waste" to qs(
        "Come funziona la raccolta dei rifiuti: porta a porta o cassonetti?", "Hoe werkt de afvalinzameling: aan huis of containers?",
        "Quanto si paga di TARI all'anno?", "Hoeveel afvalstoffenheffing (TARI) betaalt u per jaar?",
    ),
    "net_fixed" to qs(
        "Arriva la fibra ottica o solo ADSL/FWA?", "Is er glasvezel of alleen ADSL/vaste draadloze verbinding (FWA)?",
        "Con quale operatore avete internet e che velocità avete?", "Bij welke provider heeft u internet en welke snelheid?",
    ),
    "net_speedtest" to qs(
        "Posso fare un test della velocità sulla vostra connessione?", "Mag ik een snelheidstest doen op uw verbinding?",
        "La connessione cade spesso, ad esempio con il maltempo?", "Valt de verbinding vaak weg, bijvoorbeeld bij slecht weer?",
    ),
    "net_satellite" to qs(
        "Qualcuno in zona usa internet satellitare o radio?", "Gebruikt iemand in de buurt satelliet- of straalverbindingsinternet?",
        "Si può installare un'antenna o una parabola sul tetto?", "Mag er een antenne of schotel op het dak worden geplaatst?",
    ),
    "mob_signal" to qs(
        "Quale operatore mobile prende meglio qui?", "Welke mobiele provider heeft hier het beste bereik?",
        "Il cellulare prende anche dentro casa?", "Heeft de mobiele telefoon ook binnenshuis bereik?",
    ),
    "mob_data" to qs(
        "Il 4G o il 5G funziona bene per videochiamate?", "Werkt 4G of 5G goed genoeg voor videobellen?",
        "Avete usato un router 4G per internet di casa?", "Heeft u een 4G-router gebruikt voor thuisinternet?",
    ),
    "bdoc_planimetria" to qs(
        "Posso vedere la visura e la planimetria catastale?", "Mag ik het kadasteruittreksel (visura) en de kadastrale plattegrond zien?",
        "La planimetria corrisponde allo stato attuale della casa?", "Komt de plattegrond overeen met de huidige situatie van het huis?",
    ),
    "bdoc_conformity" to qs(
        "La casa è conforme dal punto di vista urbanistico e catastale?", "Is het huis in orde qua bouwvergunningen (urbanistica) en kadaster?",
        "Ci sono abusi edilizi, condoni o sanatorie in corso?", "Zijn er illegale bouwwerken, legalisaties (condono) of lopende sanatorie?",
        "Il tecnico ha fatto una relazione di conformità prima della vendita?", "Heeft een technicus vóór de verkoop een conformiteitsrapport opgesteld?",
    ),
    "bdoc_agibilita" to qs(
        "C'è il certificato di agibilità (o la SCA)?", "Is er een bewoonbaarheidsverklaring (agibilità of SCA)?",
        "Se manca, perché? Si può ottenere?", "Als die ontbreekt, waarom? Kan die verkregen worden?",
    ),
    "bdoc_ape" to qs(
        "L'APE è stato fatto da un tecnico abilitato ed è valido?", "Is het energielabel (APE) door een bevoegde technicus opgesteld en geldig?",
        "L'APE sarà allegato all'atto di vendita?", "Wordt het APE bij de koopakte gevoegd?",
    ),
    "bdoc_systems_certs" to qs(
        "Ci sono le dichiarazioni di conformità di impianto elettrico, gas e idraulico?", "Zijn er conformiteitsverklaringen voor elektra, gas en water/sanitair?",
        "C'è il libretto di impianto della caldaia?", "Is er het installatieboekje van de cv-ketel?",
    ),
    "bdoc_works_history" to qs(
        "Quali lavori sono stati fatti negli ultimi anni? Con quali permessi (CILA, SCIA, permesso di costruire)?", "Welke werkzaamheden zijn de laatste jaren gedaan? Met welke vergunningen (CILA, SCIA, bouwvergunning)?",
        "Ci sono bonus edilizi in corso con detrazioni non ancora godute?", "Lopen er bouwsubsidies met nog niet gebruikte belastingaftrek?",
        "Posso avere le fatture e le pratiche dei lavori?", "Mag ik de facturen en dossiers van de werkzaamheden krijgen?",
    ),
    "ldoc_title" to qs(
        "Chi sono i proprietari? Tutti sono d'accordo a vendere?", "Wie zijn de eigenaren? Gaan ze allemaal akkoord met de verkoop?",
        "La casa viene da un'eredità o da una donazione? La successione è stata fatta?", "Komt het huis uit een erfenis of schenking? Is de nalatenschap afgehandeld?",
        "Posso vedere l'atto di provenienza?", "Mag ik de akte van herkomst (eigendomsakte) zien?",
    ),
    "ldoc_mortgages" to qs(
        "Ci sono ipoteche, pignoramenti o altri gravami sulla casa?", "Rusten er hypotheken, beslagen of andere lasten op het huis?",
        "Il notaio farà la visura ipotecaria prima del rogito?", "Doet de notaris vóór de overdracht een hypotheekonderzoek (visura ipotecaria)?",
    ),
    "ldoc_constraints" to qs(
        "Ci sono vincoli paesaggistici, storici o idrogeologici?", "Zijn er landschappelijke, historische of hydrogeologische beperkingen?",
        "Per lavori esterni serve l'autorizzazione paesaggistica?", "Is voor buitenwerkzaamheden een landschapsvergunning nodig?",
    ),
    "ldoc_condominium" to qs(
        "Quanto sono le spese condominiali annuali?", "Hoeveel zijn de jaarlijkse servicekosten (condominio)?",
        "Ci sono lavori straordinari già deliberati o rate non pagate?", "Zijn er al besloten grote werkzaamheden of onbetaalde termijnen?",
        "Posso vedere il regolamento e gli ultimi verbali di assemblea?", "Mag ik het reglement en de laatste notulen van de vergadering zien?",
    ),
    "ldoc_tenancy" to qs(
        "La casa è affittata? Fino a quando dura il contratto?", "Is het huis verhuurd? Tot wanneer loopt het contract?",
        "La casa sarà libera al momento del rogito?", "Is het huis leeg bij de overdracht?",
    ),
    "ldoc_compromesso" to qs(
        "Quanto è la caparra confirmatoria richiesta?", "Hoe hoog is de gevraagde aanbetaling (caparra confirmatoria)?",
        "Il compromesso sarà registrato? Si può trascrivere?", "Wordt het voorlopig koopcontract geregistreerd? Kan het worden ingeschreven?",
        "Quali condizioni sospensive possiamo inserire (mutuo, conformità)?", "Welke opschortende voorwaarden kunnen we opnemen (hypotheek, conformiteit)?",
    ),
    "ldoc_agency_fee" to qs(
        "Quanto è la provvigione dell'agenzia e quando si paga?", "Hoeveel is de makelaarscourtage en wanneer moet die betaald worden?",
        "L'agenzia è iscritta al registro e rilascia fattura?", "Is het makelaarskantoor ingeschreven in het register en geeft het een factuur?",
    ),
    "ldoc_land_use" to qs(
        "Che destinazione urbanistica ha il terreno? Posso avere il certificato di destinazione urbanistica (CDU)?", "Welke bestemming heeft de grond? Kan ik het bestemmingscertificaat (CDU) krijgen?",
        "Il terreno agricolo è soggetto a prelazione agraria dei confinanti?", "Hebben de buren een agrarisch voorkooprecht (prelazione agraria) op de landbouwgrond?",
        "Si può costruire o ampliare sul terreno?", "Mag er op het terrein gebouwd of uitgebreid worden?",
    ),
)