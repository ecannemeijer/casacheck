package nl.ecannemeijer.casacheck.domain.checklist

// General, moisture, foundation, structure, cracks.
internal fun questionsPart1(): Map<String, List<CheckpointQuestion>> = mapOf(
    "gen_first_impression" to qs(
        "Da quanto tempo è in vendita la casa?", "Hoe lang staat het huis al te koop?",
        "Perché i proprietari vendono?", "Waarom verkopen de eigenaren?",
        "Ci sono già state offerte o trattative?", "Zijn er al biedingen of onderhandelingen geweest?",
    ),
    "gen_smell" to qs(
        "Da quanto tempo la casa non viene arieggiata?", "Hoe lang is het huis niet gelucht?",
        "Ci sono stati problemi di umidità o muffa in passato?", "Zijn er in het verleden problemen met vocht of schimmel geweest?",
        "Ci sono stati animali in casa o problemi con topi?", "Zijn er huisdieren geweest of problemen met muizen/ratten?",
    ),
    "gen_vacancy" to qs(
        "Da quanto tempo la casa è disabitata?", "Hoe lang staat het huis leeg?",
        "Il riscaldamento è stato acceso negli ultimi inverni?", "Is de verwarming de afgelopen winters aangezet?",
        "L'acqua è stata chiusa e gli impianti svuotati durante l'inverno?", "Is het water afgesloten en zijn de leidingen 's winters geleegd?",
    ),
    "gen_fresh_paint" to qs(
        "Quando sono state ridipinte le pareti e perché?", "Wanneer zijn de muren opnieuw geschilderd en waarom?",
        "Sotto la pittura nuova c'erano macchie di umidità o crepe?", "Zaten er onder de nieuwe verf vochtplekken of scheuren?",
        "Chi ha eseguito i lavori? C'è una fattura?", "Wie heeft het werk gedaan? Is er een factuur?",
    ),
    "gen_floor_level" to qs(
        "I pavimenti sono sempre stati così in pendenza?", "Hebben de vloeren altijd al zo scheef gelegen?",
        "Sono mai stati fatti interventi sui solai?", "Zijn er ooit werkzaamheden aan de vloeren/balklagen gedaan?",
    ),
    "gen_asbestos" to qs(
        "Ci sono materiali in amianto (eternit) nella casa o negli annessi?", "Zijn er asbesthoudende materialen (eternit) in het huis of de bijgebouwen?",
        "È mai stata fatta una verifica o una bonifica dell'amianto?", "Is asbest ooit onderzocht of gesaneerd?",
        "Le vecchie canne fumarie o i serbatoi d'acqua sono in eternit?", "Zijn oude rookkanalen of watertanks van eternit?",
    ),
    "moist_rising" to qs(
        "Ci sono mai stati problemi di umidità di risalita nei muri?", "Zijn er ooit problemen geweest met optrekkend vocht in de muren?",
        "È stata fatta una barriera chimica o un intonaco deumidificante? Quando?", "Is er een vochtkering geïnjecteerd of saneerpleister aangebracht? Wanneer?",
        "Quando è stato fatto l'ultimo intervento sulle murature?", "Wanneer is voor het laatst aan het metselwerk gewerkt?",
        "C'è una garanzia o una fattura per i lavori contro l'umidità?", "Is er een garantie of factuur voor de vochtwerkzaamheden?",
    ),
    "moist_penetrating" to qs(
        "Ci sono mai state infiltrazioni d'acqua dai muri esterni?", "Is er ooit water door de buitenmuren naar binnen gekomen?",
        "Le macchie aumentano quando piove forte?", "Worden de vlekken groter bij hevige regen?",
        "Quando è stato rifatto l'intonaco esterno?", "Wanneer is het buitenpleisterwerk vernieuwd?",
    ),
    "moist_condensation" to qs(
        "D'inverno si forma condensa sui vetri o sulle pareti?", "Ontstaat er 's winters condens op ramen of muren?",
        "Come veniva riscaldata e arieggiata la casa?", "Hoe werd het huis verwarmd en geventileerd?",
    ),
    "moist_black_mould" to qs(
        "Da quanto tempo c'è questa muffa?", "Hoe lang zit deze schimmel er al?",
        "Si è già provato a eliminarla? Con quale risultato?", "Is al geprobeerd de schimmel te verwijderen? Met welk resultaat?",
        "Qual è secondo voi la causa: infiltrazione, condensa o risalita?", "Wat is volgens u de oorzaak: lekkage, condens of optrekkend vocht?",
    ),
    "moist_salts" to qs(
        "Da quanto tempo compaiono queste efflorescenze bianche?", "Hoe lang verschijnen deze witte zoutafzettingen al?",
        "Il muro è stato trattato contro l'umidità di risalita?", "Is de muur behandeld tegen optrekkend vocht?",
    ),
    "moist_peeling_paint" to qs(
        "Perché la pittura si stacca in questo punto?", "Waarom laat de verf hier los?",
        "Ci sono state perdite d'acqua o infiltrazioni qui?", "Zijn hier lekkages of insijpeling geweest?",
    ),
    "moist_loose_plaster" to qs(
        "Da quanto tempo l'intonaco si stacca?", "Hoe lang laat het stucwerk al los?",
        "Il muro è stato controllato da un tecnico?", "Is de muur door een deskundige bekeken?",
        "Ci sono preventivi per il rifacimento dell'intonaco?", "Zijn er offertes voor het vernieuwen van het pleisterwerk?",
    ),
    "moist_musty" to qs(
        "Questa stanza viene usata e riscaldata regolarmente?", "Wordt deze kamer regelmatig gebruikt en verwarmd?",
        "Ci sono stati allagamenti o perdite in questa stanza?", "Zijn er in deze kamer overstromingen of lekkages geweest?",
    ),
    "moist_roof_leak" to qs(
        "Ci sono mai state infiltrazioni d'acqua dal tetto?", "Heeft het dak ooit gelekt?",
        "Quando e come è stata riparata l'ultima perdita?", "Wanneer en hoe is de laatste lekkage gerepareerd?",
        "C'è la fattura o la garanzia della riparazione?", "Is er een factuur of garantie van de reparatie?",
    ),
    "moist_pipe_leak" to qs(
        "Ci sono state perdite dalle tubature dell'acqua?", "Zijn er lekkages van waterleidingen geweest?",
        "Le bollette dell'acqua mostrano consumi anomali?", "Tonen de waterrekeningen een afwijkend verbruik?",
        "Le tubature sono state rifatte? In che anno?", "Zijn de leidingen vernieuwd? In welk jaar?",
    ),
    "moist_downpipe_leak" to qs(
        "I pluviali perdono o si intasano spesso?", "Lekken de regenpijpen of raken ze vaak verstopt?",
        "Dove scaricano i pluviali: in fognatura, in un pozzetto o sul terreno?", "Waar lozen de regenpijpen: op het riool, in een put of op het terrein?",
    ),
    "moist_cellar" to qs(
        "La cantina si allaga quando piove molto?", "Loopt de kelder onder water bij veel regen?",
        "C'è una pompa di drenaggio o un pozzetto di raccolta?", "Is er een drainagepomp of een opvangput?",
        "Cosa veniva conservato in cantina senza problemi?", "Wat werd er zonder problemen in de kelder bewaard?",
    ),
    "moist_garage" to qs(
        "Il garage si allaga o entra acqua dalla porta?", "Loopt de garage onder of komt er water onder de deur door?",
        "C'è una griglia di scolo davanti al garage?", "Is er een afvoergoot voor de garage?",
    ),
    "moist_soil_against_wall" to qs(
        "I muri contro terra sono impermeabilizzati o drenati?", "Zijn de muren die tegen de grond staan waterdicht gemaakt of gedraineerd?",
        "C'è un drenaggio o un'intercapedine dietro questi muri?", "Is er drainage of een spouw/luchtkoker achter deze muren?",
        "Ci sono mai state infiltrazioni da questo lato?", "Is er aan deze kant ooit water binnengekomen?",
    ),
    "moist_hillside_house" to qs(
        "Come viene allontanata l'acqua che scende dalla collina?", "Hoe wordt water dat van de helling komt afgevoerd?",
        "Ci sono drenaggi a monte della casa? Chi li ha fatti?", "Is er drainage aan de bergkant van het huis? Wie heeft die aangelegd?",
        "Dopo forti piogge entra acqua o fango?", "Komt er na hevige regen water of modder binnen?",
    ),
    "moist_poor_drainage" to qs(
        "Dopo un temporale si formano pozzanghere vicino alla casa?", "Ontstaan er na onweer plassen naast het huis?",
        "C'è un marciapiede o una canaletta intorno alla casa?", "Is er een stoep of afvoergoot rond het huis?",
    ),
    "moist_overpainted" to qs(
        "Perché questo muro è stato ridipinto di recente?", "Waarom is deze muur onlangs overgeschilderd?",
        "C'era umidità prima della pittura? Posso misurarla?", "Was er vocht vóór het schilderen? Mag ik het meten?",
    ),
    "moist_behind_furniture" to qs(
        "Posso spostare questo mobile per vedere la parete?", "Mag ik dit meubel verplaatsen om de muur te zien?",
        "Dietro gli armadi c'era muffa?", "Zat er schimmel achter de kasten?",
    ),
    "moist_windows" to qs(
        "Entra acqua dagli infissi quando piove con vento?", "Komt er water langs de kozijnen bij regen met wind?",
        "Gli infissi sono originali o sostituiti? In che anno?", "Zijn de kozijnen origineel of vervangen? In welk jaar?",
    ),
    "moist_ceilings" to qs(
        "Da dove viene la macchia sul soffitto?", "Waar komt de vlek op het plafond vandaan?",
        "Sopra c'è un bagno, un terrazzo o il tetto?", "Zit er een badkamer, terras of het dak boven?",
        "La perdita è stata riparata? Quando?", "Is het lek gerepareerd? Wanneer?",
    ),
    "moist_wood_rot" to qs(
        "Le travi o gli infissi in legno sono stati trattati o sostituiti?", "Zijn houten balken of kozijnen behandeld of vervangen?",
        "Ci sono parti in legno marcite che conoscete?", "Zijn er rotte houten delen die u kent?",
    ),
    "fnd_visible_base" to qs(
        "Su che tipo di fondazioni è costruita la casa?", "Op wat voor fundering is het huis gebouwd?",
        "Sono stati fatti interventi alle fondazioni?", "Zijn er werkzaamheden aan de fundering gedaan?",
    ),
    "fnd_doors_sticking" to qs(
        "Da quando le porte o le finestre si bloccano?", "Sinds wanneer klemmen de deuren of ramen?",
        "Sono state piallate o regolate di recente?", "Zijn ze onlangs bijgeschaafd of afgesteld?",
    ),
    "fnd_trees" to qs(
        "Questi alberi sono vicini alle fondazioni da molto tempo?", "Staan deze bomen al lang zo dicht bij de fundering?",
        "Le radici hanno mai causato danni a muri o tubature?", "Hebben de wortels ooit schade aan muren of leidingen veroorzaakt?",
    ),
    "fnd_earthquake_works" to qs(
        "Sono stati fatti interventi di miglioramento o adeguamento sismico?", "Zijn er aardbevingsversterkingen uitgevoerd?",
        "Ci sono catene, cordoli o altri rinforzi? Con progetto e collaudo?", "Zijn er trekstangen, ringbalken of andere versterkingen? Met ontwerp en keuring?",
        "Sono stati usati bonus fiscali (es. sismabonus)? Ci sono i documenti?", "Is er gebruik gemaakt van fiscale bonussen (bijv. sismabonus)? Zijn de documenten er?",
    ),
    "str_load_walls" to qs(
        "Sono stati tolti o modificati muri portanti?", "Zijn er dragende muren verwijderd of aangepast?",
        "Per quei lavori c'era un progetto strutturale depositato?", "Was er voor dat werk een ingediend constructief ontwerp?",
    ),
    "str_floors_beams" to qs(
        "Le travi del solaio sono originali?", "Zijn de balken van de vloer origineel?",
        "Sono mai state rinforzate o sostituite? Quando?", "Zijn ze ooit versterkt of vervangen? Wanneer?",
        "Il pavimento vibra o cede in qualche punto?", "Trilt of verzakt de vloer ergens?",
    ),
    "str_woodworm" to qs(
        "Ci sono mai stati tarli o termiti nel legno?", "Zijn er ooit houtworm of termieten in het hout geweest?",
        "È stato fatto un trattamento antitarlo? Quando?", "Is het hout behandeld tegen houtworm? Wanneer?",
    ),
    "str_vaults_arches" to qs(
        "Le volte hanno crepe da molto tempo?", "Zitten de scheuren in de gewelven er al lang?",
        "Sono state consolidate le volte? Con quali lavori?", "Zijn de gewelven verstevigd? Met welke werkzaamheden?",
    ),
    "str_stairs" to qs(
        "La scala è originale o è stata rifatta?", "Is de trap origineel of vernieuwd?",
        "La scala è stata realizzata con permesso?", "Is de trap met vergunning gebouwd?",
    ),
    "str_alterations" to qs(
        "Quali lavori di ristrutturazione o ampliamento sono stati fatti?", "Welke verbouwingen of uitbreidingen zijn er gedaan?",
        "Sono stati fatti con titolo edilizio (permesso, SCIA, CILA)?", "Zijn ze uitgevoerd met een bouwtitel (vergunning, SCIA, CILA)?",
        "C'è stato un condono o una sanatoria? Si possono vedere i documenti?", "Is er een legalisatie (condono/sanatoria) geweest? Mag ik de documenten zien?",
    ),
    "crk_diagonal" to qs(
        "Da quanto tempo c'è questa crepa? Si sta allargando?", "Hoe lang zit deze scheur er al? Wordt hij groter?",
        "La crepa è stata vista da un ingegnere o geometra?", "Is de scheur bekeken door een ingenieur of geometra?",
        "Ci sono stati cedimenti o lavori vicino alla casa?", "Zijn er verzakkingen of werkzaamheden vlak bij het huis geweest?",
    ),
    "crk_horizontal" to qs(
        "Da quando c'è questa crepa orizzontale?", "Sinds wanneer zit deze horizontale scheur er?",
        "Il muro spinge verso l'esterno o è sempre stato così?", "Duwt de muur naar buiten of is hij altijd zo geweest?",
    ),
    "crk_vertical" to qs(
        "La crepa è stata riparata in passato ed è riapparsa?", "Is de scheur eerder gerepareerd en weer teruggekomen?",
        "Ci sono stati terremoti o lavori che l'hanno causata?", "Zijn er aardbevingen of werkzaamheden geweest die hem veroorzaakten?",
    ),
    "crk_openings" to qs(
        "Le crepe sopra porte e finestre sono recenti?", "Zijn de scheuren boven deuren en ramen recent?",
        "Gli architravi sono stati controllati o sostituiti?", "Zijn de lateien gecontroleerd of vervangen?",
    ),
    "crk_ceiling" to qs(
        "Le crepe nel soffitto sono stabili o aumentano?", "Zijn de scheuren in het plafond stabiel of worden ze groter?",
        "Sopra c'è un solaio in legno, in laterocemento o una volta?", "Zit erboven een houten vloer, een betonvloer of een gewelf?",
    ),
    "crk_foundation" to qs(
        "La casa ha mai avuto cedimenti delle fondazioni?", "Heeft het huis ooit funderingsverzakking gehad?",
        "Sono state fatte indagini o un monitoraggio delle crepe?", "Is er onderzoek of monitoring van de scheuren gedaan?",
        "C'è una relazione tecnica che posso vedere?", "Is er een technisch rapport dat ik mag inzien?",
    ),
    "crk_facade" to qs(
        "Da quanto tempo ci sono queste crepe in facciata?", "Hoe lang zitten deze scheuren in de gevel al?",
        "L'intonaco esterno verrà rifatto? Ci sono preventivi?", "Wordt het buitenpleisterwerk vernieuwd? Zijn er offertes?",
    ),
    "crk_floor_tiles" to qs(
        "Le piastrelle si sono rotte da sole o per un urto?", "Zijn de tegels vanzelf gebarsten of door een klap?",
        "Il pavimento si è abbassato in qualche punto?", "Is de vloer ergens verzakt?",
    ),
)