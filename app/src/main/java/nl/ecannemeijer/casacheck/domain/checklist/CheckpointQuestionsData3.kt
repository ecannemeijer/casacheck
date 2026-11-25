package nl.ecannemeijer.casacheck.domain.checklist

// Insulation, energy, ventilation, rooms, cellar, garage, attic, garden, terrace, balcony, grounds, retaining walls, slopes, drainage.
internal fun questionsPart3(): Map<String, List<CheckpointQuestion>> = mapOf(
    "ins_roof" to qs(
        "Il tetto o il sottotetto sono isolati? Con che spessore?", "Is het dak of de zolder geïsoleerd? Hoe dik?",
        "D'estate le stanze sotto il tetto diventano molto calde?", "Worden de kamers onder het dak 's zomers erg warm?",
    ),
    "ins_walls" to qs(
        "I muri sono isolati (cappotto o isolamento interno)?", "Zijn de muren geïsoleerd (buitengevelisolatie of binnenisolatie)?",
        "Sono stati usati superbonus o ecobonus? Ci sono le pratiche complete?", "Is de superbonus of ecobonus gebruikt? Zijn de dossiers compleet?",
    ),
    "ins_floor" to qs(
        "Il pavimento sopra la cantina o il vespaio è isolato?", "Is de vloer boven de kelder of kruipruimte geïsoleerd?",
        "C'è un vespaio aerato sotto il piano terra?", "Is er een geventileerde kruipruimte onder de begane grond?",
    ),
    "en_ape" to qs(
        "Posso vedere l'APE (attestato di prestazione energetica)? È ancora valido?", "Mag ik het energielabel (APE) zien? Is het nog geldig?",
        "In che classe energetica è la casa?", "In welke energieklasse valt het huis?",
    ),
    "en_bills" to qs(
        "Quanto si spende in media all'anno di luce, gas e acqua?", "Hoeveel kosten elektriciteit, gas en water gemiddeld per jaar?",
        "Posso vedere le bollette dell'ultimo anno?", "Mag ik de rekeningen van het afgelopen jaar zien?",
    ),
    "en_orientation" to qs(
        "In inverno quante ore di sole prende la casa?", "Hoeveel uur zon krijgt het huis in de winter?",
        "Ci sono alberi o edifici che fanno ombra?", "Zijn er bomen of gebouwen die schaduw geven?",
    ),
    "ven_general" to qs(
        "Come si arieggia la casa? Ci sono prese d'aria?", "Hoe wordt het huis geventileerd? Zijn er ventilatieroosters?",
        "Ci sono problemi di condensa o muffa d'inverno?", "Zijn er 's winters problemen met condens of schimmel?",
    ),
    "ven_wet_rooms" to qs(
        "Il bagno cieco ha un aspiratore funzionante?", "Heeft de badkamer zonder raam een werkende afzuiging?",
        "Dove scarica la cappa della cucina?", "Waar voert de afzuigkap van de keuken af?",
    ),
    "kit_condition" to qs(
        "La cucina è inclusa nella vendita? Anche gli elettrodomestici?", "Hoort de keuken bij de verkoop? Ook de apparatuur?",
        "Quanti anni ha la cucina?", "Hoe oud is de keuken?",
    ),
    "kit_extraction_gas" to qs(
        "Il piano cottura è a gas, a induzione o elettrico?", "Is de kookplaat op gas, inductie of elektrisch?",
        "In cucina c'è l'apertura di ventilazione obbligatoria per il gas?", "Is er in de keuken de verplichte ventilatieopening voor gas?",
        "La cappa scarica all'esterno?", "Voert de afzuigkap naar buiten af?",
    ),
    "kit_tiles_worktop" to qs(
        "Ci sono state perdite sotto il lavello o la lavastoviglie?", "Zijn er lekkages onder de gootsteen of vaatwasser geweest?",
        "Di che materiale è il piano di lavoro?", "Van welk materiaal is het aanrechtblad?",
    ),
    "bath_leaks" to qs(
        "Ci sono state perdite dal bagno verso il piano di sotto?", "Zijn er lekkages van de badkamer naar de verdieping eronder geweest?",
        "Quando è stato rifatto il bagno?", "Wanneer is de badkamer vernieuwd?",
        "Le fughe della doccia sono state sigillate di recente?", "Zijn de voegen van de douche onlangs opnieuw afgekit?",
    ),
    "bath_count_layout" to qs(
        "Tutti i bagni sono regolari e presenti nella planimetria?", "Zijn alle badkamers legaal en staan ze op de plattegrond?",
        "È possibile aggiungere un bagno? Dove passano gli scarichi?", "Kan er een badkamer bij? Waar lopen de afvoeren?",
    ),
    "bath_fixtures" to qs(
        "I sanitari e la rubinetteria funzionano tutti bene?", "Werken alle sanitair en kranen goed?",
        "Lo sciacquone perde o la cassetta è incassata nel muro?", "Lekt de toiletspoeling of zit het reservoir in de muur?",
    ),
    "bath_ventilation_mould" to qs(
        "C'è muffa nel bagno d'inverno?", "Is er 's winters schimmel in de badkamer?",
        "L'aspiratore funziona e dove scarica?", "Werkt de afzuiging en waar voert die af?",
    ),
    "liv_general" to qs(
        "Il camino in soggiorno funziona e tira bene?", "Werkt de open haard in de woonkamer en trekt die goed?",
        "Quali mobili rimangono nella casa?", "Welke meubels blijven in het huis?",
    ),
    "liv_ceiling_height" to qs(
        "Qual è l'altezza dei soffitti? I locali sono abitabili secondo il Comune?", "Hoe hoog zijn de plafonds? Zijn de ruimtes bewoonbaar volgens de gemeente?",
        "Questa stanza risulta come camera o come locale accessorio nella planimetria?", "Staat deze ruimte als kamer of als bijruimte op de plattegrond?",
    ),
    "bed_size_use" to qs(
        "Questa camera è accatastata come camera da letto?", "Is deze kamer kadastraal geregistreerd als slaapkamer?",
        "La finestra è sufficiente per l'abitabilità della stanza?", "Is het raam groot genoeg voor de bewoonbaarheid van de kamer?",
    ),
    "bed_general" to qs(
        "In questa camera c'è mai stata umidità o muffa?", "Is er in deze slaapkamer ooit vocht of schimmel geweest?",
        "La camera è calda d'inverno?", "Is de kamer 's winters warm?",
    ),
    "room_walls_moisture" to qs(
        "In questa stanza ci sono mai state macchie di umidità?", "Zijn er in deze kamer ooit vochtplekken geweest?",
        "Le pareti sono state ridipinte di recente? Perché?", "Zijn de muren onlangs geschilderd? Waarom?",
    ),
    "room_cracks" to qs(
        "Queste crepe ci sono da molto tempo?", "Zitten deze scheuren er al lang?",
        "Sono state stuccate e poi sono riapparse?", "Zijn ze dichtgesmeerd en daarna teruggekomen?",
    ),
    "room_floor" to qs(
        "Il pavimento è originale? Sotto c'è un altro pavimento?", "Is de vloer origineel? Ligt er een andere vloer onder?",
        "Ci sono piastrelle che suonano vuote o sono staccate?", "Zijn er tegels die hol klinken of loszitten?",
    ),
    "room_ceiling" to qs(
        "Il soffitto è in legno, laterocemento o una volta?", "Is het plafond van hout, betonnen balken of een gewelf?",
        "Ci sono mai state macchie o crepe sul soffitto?", "Zijn er ooit vlekken of scheuren in het plafond geweest?",
    ),
    "room_windows" to qs(
        "Le finestre di questa stanza chiudono bene?", "Sluiten de ramen in deze kamer goed?",
        "Entra spiffero o acqua quando piove?", "Tocht het of komt er water binnen bij regen?",
    ),
    "room_electric" to qs(
        "Le prese di questa stanza hanno la messa a terra?", "Zijn de stopcontacten in deze kamer geaard?",
        "Ci sono abbastanza prese o servono prolunghe?", "Zijn er genoeg stopcontacten of zijn verlengsnoeren nodig?",
    ),
    "room_heating" to qs(
        "Questa stanza si scalda bene d'inverno?", "Wordt deze kamer 's winters goed warm?",
        "Il termosifone ha la valvola termostatica e funziona?", "Heeft de radiator een thermostaatkraan en werkt die?",
    ),
    "cel_structure" to qs(
        "La cantina ha mai avuto infiltrazioni o allagamenti?", "Heeft de kelder ooit lekkages of overstromingen gehad?",
        "Le volte o le travi della cantina sono state consolidate?", "Zijn de gewelven of balken van de kelder verstevigd?",
    ),
    "cel_use_legal" to qs(
        "La cantina è accatastata come cantina o come locale abitabile?", "Is de kelder geregistreerd als kelder of als bewoonbare ruimte?",
        "Si può trasformare in taverna o abitazione? Serve un permesso?", "Mag hij omgebouwd worden tot woonruimte? Is een vergunning nodig?",
    ),
    "cel_ventilation" to qs(
        "La cantina ha finestre o bocche di aerazione?", "Heeft de kelder ramen of ventilatieopeningen?",
        "Cosa si può conservare in cantina senza che faccia muffa?", "Wat kan in de kelder bewaard worden zonder dat het schimmelt?",
    ),
    "gar_structure" to qs(
        "Il garage è regolare e accatastato?", "Is de garage legaal en kadastraal geregistreerd?",
        "Il tetto del garage è in eternit?", "Is het dak van de garage van eternit (asbest)?",
    ),
    "gar_size_access" to qs(
        "Il garage è adatto per un'auto grande?", "Past er een grote auto in de garage?",
        "L'accesso al garage è privato o in comune con altri?", "Is de toegang tot de garage privé of gedeeld met anderen?",
    ),
    "att_roof_timber" to qs(
        "Il legno del tetto è stato controllato o trattato?", "Is het dakhout gecontroleerd of behandeld?",
        "Ci sono macchie d'acqua sul sottotetto?", "Zijn er watervlekken aan de onderkant van het dak?",
    ),
    "att_insulation" to qs(
        "Il sottotetto è isolato e ventilato?", "Is de zolder geïsoleerd en geventileerd?",
        "D'estate fa molto caldo sotto il tetto?", "Wordt het 's zomers erg warm onder het dak?",
    ),
    "att_pests" to qs(
        "Ci sono mai stati topi, ghiri, vespe o piccioni nel sottotetto?", "Zijn er ooit muizen, relmuizen, wespen of duiven op zolder geweest?",
        "È stata fatta una disinfestazione? Quando?", "Is er ongedierte bestreden? Wanneer?",
    ),
    "att_conversion" to qs(
        "Il sottotetto è abitabile secondo il Comune o solo un locale di sgombero?", "Is de zolder volgens de gemeente bewoonbaar of alleen bergruimte?",
        "La trasformazione del sottotetto è stata autorizzata?", "Is de verbouwing van de zolder met vergunning gedaan?",
    ),
    "gdn_general" to qs(
        "Il giardino è compreso tutto nella proprietà?", "Hoort de hele tuin bij het eigendom?",
        "Chi si è occupato della manutenzione finora?", "Wie deed tot nu toe het onderhoud?",
    ),
    "gdn_fencing" to qs(
        "La recinzione è completa e di proprietà?", "Is de omheining compleet en eigendom?",
        "Il cancello si chiude? Un cane potrebbe uscire?", "Sluit het hek? Kan een hond ontsnappen?",
    ),
    "gdn_well_irrigation" to qs(
        "Il pozzo è denunciato o autorizzato? L'acqua si può usare per irrigare?", "Is de put aangemeld of vergund? Mag het water gebruikt worden voor irrigatie?",
        "C'è un impianto di irrigazione funzionante?", "Is er een werkende irrigatie-installatie?",
    ),
    "ter_waterproofing" to qs(
        "Quando è stata rifatta l'impermeabilizzazione del terrazzo?", "Wanneer is de waterdichting van het terras vernieuwd?",
        "Ci sono infiltrazioni nei locali sotto il terrazzo?", "Zijn er lekkages in de ruimtes onder het terras?",
    ),
    "ter_railing" to qs(
        "Il parapetto è stabile e alto almeno un metro?", "Is de borstwering stevig en minstens een meter hoog?",
        "La ringhiera è stata trattata contro la ruggine?", "Is de reling behandeld tegen roest?",
    ),
    "ter_legal" to qs(
        "La pergola o la copertura del terrazzo è autorizzata?", "Is de pergola of overkapping van het terras vergund?",
        "Il terrazzo chiuso (veranda) risulta nei documenti comunali?", "Staat de dichte veranda in de gemeentelijke documenten?",
    ),
    "bal_slab" to qs(
        "Il cemento del balcone si sgretola o si vedono i ferri?", "Brokkelt het beton van het balkon af of zijn de wapeningsstaven zichtbaar?",
        "Il balcone è stato ristrutturato? Quando?", "Is het balkon gerenoveerd? Wanneer?",
    ),
    "bal_railing_drain" to qs(
        "L'acqua sul balcone scarica bene?", "Loopt het water op het balkon goed weg?",
        "La ringhiera è ben fissata?", "Zit de reling stevig vast?",
    ),
    "grd_boundaries" to qs(
        "I confini del terreno sono segnati? Coincidono con la mappa catastale?", "Zijn de perceelgrenzen gemarkeerd? Komen ze overeen met de kadasterkaart?",
        "È stato fatto un frazionamento o un rilievo dei confini?", "Is er een kadastrale splitsing of grensmeting gedaan?",
        "Ci sono contestazioni con i vicini sui confini?", "Zijn er geschillen met de buren over de grenzen?",
    ),
    "grd_servitude" to qs(
        "Ci sono servitù di passaggio, acqua o luce sul terreno?", "Zijn er erfdienstbaarheden voor doorgang, water of elektriciteit op het terrein?",
        "Qualcuno ha il diritto di passare sulla proprietà?", "Heeft iemand het recht om over het terrein te lopen/rijden?",
        "La strada di accesso è privata, comunale o vicinale?", "Is de toegangsweg privé, gemeentelijk of een buurtweg?",
    ),
    "grd_trees" to qs(
        "Gli alberi vicini alla casa sono della proprietà?", "Zijn de bomen vlak bij het huis eigendom?",
        "Servono permessi per abbattere questi alberi?", "Is er een vergunning nodig om deze bomen te kappen?",
    ),
    "grd_outbuildings" to qs(
        "Gli annessi e i ripostigli sono regolari e accatastati?", "Zijn de bijgebouwen en schuren legaal en kadastraal geregistreerd?",
        "Si possono trasformare in abitazione?", "Mogen ze tot woonruimte worden omgebouwd?",
    ),
    "grd_asbestos" to qs(
        "Le coperture degli annessi sono in eternit?", "Zijn de daken van de bijgebouwen van eternit (asbest)?",
        "Chi paga la rimozione dell'amianto prima del rogito?", "Wie betaalt het verwijderen van asbest vóór de overdracht?",
    ),
    "ret_condition" to qs(
        "I muri di contenimento si sono mai mossi o crepati?", "Zijn de keermuren ooit verschoven of gescheurd?",
        "Sono stati costruiti o rifatti con un progetto?", "Zijn ze gebouwd of vernieuwd met een ontwerp?",
    ),
    "ret_weepholes" to qs(
        "I muri di contenimento hanno fori di drenaggio?", "Hebben de keermuren drainagegaten?",
        "Dopo la pioggia esce acqua dai fori?", "Komt er na regen water uit de gaten?",
    ),
    "ret_ownership" to qs(
        "Di chi sono i muri di sostegno: vostri, del vicino o del Comune?", "Van wie zijn de keermuren: van u, de buren of de gemeente?",
        "Chi paga la manutenzione se crollano?", "Wie betaalt het onderhoud als ze instorten?",
    ),
    "ret_terraces" to qs(
        "I muretti a secco dei terrazzamenti sono stabili?", "Zijn de droge stapelmuren van de terrassen stabiel?",
        "Ci sono parti crollate da ricostruire?", "Zijn er ingestorte delen die herbouwd moeten worden?",
    ),
    "slo_landslide" to qs(
        "La zona è a rischio frana nel PAI o nella cartografia del Comune?", "Valt de zone onder aardverschuivingsrisico in het PAI of op de gemeentelijke kaarten?",
        "Ci sono mai state frane o smottamenti qui vicino?", "Zijn er hier in de buurt ooit aardverschuivingen geweest?",
        "C'è un vincolo idrogeologico sul terreno?", "Rust er een hydrogeologische beperking (vincolo idrogeologico) op het terrein?",
    ),
    "slo_rockfall" to qs(
        "Cadono mai sassi dalla parete sopra la casa?", "Vallen er weleens stenen van de helling boven het huis?",
        "Ci sono reti o barriere paramassi? Chi le mantiene?", "Zijn er steenslagnetten of -barrières? Wie onderhoudt ze?",
    ),
    "slo_house_position" to qs(
        "La casa ha mai avuto problemi per la posizione sul pendio?", "Heeft het huis ooit problemen gehad door de ligging op de helling?",
        "Come arriva l'acqua piovana dalla parte a monte?", "Hoe komt regenwater van de bergkant naar beneden?",
    ),
    "slo_snow_avalanche" to qs(
        "Quanta neve cade di solito? Il tetto regge il carico?", "Hoeveel sneeuw valt er meestal? Kan het dak de last dragen?",
        "La zona è soggetta a valanghe?", "Is de zone gevoelig voor lawines?",
    ),
    "slo_erosion" to qs(
        "Con le piogge forti l'acqua scava il terreno?", "Spoelt het water bij hevige regen de grond weg?",
        "Sono stati fatti lavori per contenere l'erosione?", "Zijn er werkzaamheden tegen erosie uitgevoerd?",
    ),
    "drn_around_house" to qs(
        "Intorno alla casa ci sono drenaggi o canalette?", "Zijn er rond het huis drainage of afvoergoten?",
        "Dopo forti piogge l'acqua ristagna vicino ai muri?", "Blijft er na hevige regen water staan tegen de muren?",
    ),
    "drn_flood_zone" to qs(
        "La zona è a rischio alluvione secondo il PAI o il piano di protezione civile?", "Valt de zone onder overstromingsrisico volgens het PAI of het rampenplan?",
        "La casa o la strada si sono mai allagate?", "Zijn het huis of de weg ooit overstroomd?",
        "È possibile assicurare la casa contro le alluvioni?", "Kan het huis verzekerd worden tegen overstroming?",
    ),
    "drn_culverts" to qs(
        "Chi pulisce i fossi e i tombini sul terreno?", "Wie maakt de greppels en duikers op het terrein schoon?",
        "I fossi si intasano durante i temporali?", "Raken de greppels verstopt bij onweer?",
    ),
    "drn_spring_water" to qs(
        "Ci sono sorgenti o acqua nel terreno vicino alla casa?", "Zijn er bronnen of is er water in de grond vlak bij het huis?",
        "Il terreno resta bagnato anche in estate?", "Blijft de grond ook in de zomer nat?",
    ),
)