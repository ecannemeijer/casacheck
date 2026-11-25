# Source of truth for the CasaCheck checkpoint library (NL / EN / IT).
# Run gen.py to regenerate string resources and CheckpointLibrary.kt.
ITEMS = []
FIELDS = ["t", "e", "s", "f", "n", "a", "c", "q", "r", "w", "x"]

def P(code, cat, nl, en, it, q=0, cost=None, tools="", ill=None, cond="", rooms="", flags=""):
    for lang, tup in (("nl", nl), ("en", en), ("it", it)):
        assert len(tup) == 11, (code, lang, len(tup))
    ITEMS.append(dict(code=code, cat=cat, quick=bool(q), cost=cost, tools=tools.split(), ill=ill,
                      cond=cond.split(), rooms=rooms.split(), flags=flags, nl=nl, en=en, it=it))

# category code -> (nl, en, it)
CATEGORIES = [
    ("GENERAL", "Eerste algemene indruk", "First general impression", "Prima impressione generale"),
    ("PREMARKED", "Vooraf gemarkeerde aandachtspunten", "Pre-marked points of attention", "Punti segnalati in anticipo"),
    ("MOISTURE", "Vocht en schimmel", "Damp and mould", "Umidità e muffa"),
    ("FOUNDATION", "Fundering", "Foundations", "Fondazioni"),
    ("STRUCTURE", "Constructie", "Structure", "Struttura"),
    ("CRACKS", "Scheuren en verzakkingen", "Cracks and subsidence", "Crepe e cedimenti"),
    ("EXTERIOR_WALLS", "Buitenmuren", "Exterior walls", "Muri esterni"),
    ("FACADE", "Gevels", "Facades", "Facciate"),
    ("ROOF", "Dak", "Roof", "Tetto"),
    ("GUTTERS", "Dakgoten", "Gutters", "Grondaie"),
    ("DOWNPIPES", "Regenpijpen", "Downpipes", "Pluviali"),
    ("WINDOWS", "Ramen", "Windows", "Finestre"),
    ("FRAMES", "Kozijnen", "Window frames", "Infissi"),
    ("DOORS", "Deuren", "Doors", "Porte"),
    ("ELECTRICAL", "Elektrische installatie", "Electrical installation", "Impianto elettrico"),
    ("WATER", "Waterleidingen", "Water pipes", "Impianto idrico"),
    ("SEWER", "Riolering", "Drains and sewer", "Scarichi e fognatura"),
    ("SEPTIC", "Septic tank", "Septic tank", "Fossa settica"),
    ("HEATING", "Verwarming", "Heating", "Riscaldamento"),
    ("HOT_WATER", "Warmwatervoorziening", "Hot water", "Acqua calda sanitaria"),
    ("GAS", "Gasinstallatie", "Gas installation", "Impianto gas"),
    ("INSULATION", "Isolatie", "Insulation", "Isolamento"),
    ("ENERGY", "Energieverbruik", "Energy use", "Consumi energetici"),
    ("VENTILATION", "Ventilatie", "Ventilation", "Ventilazione"),
    ("KITCHEN", "Keuken", "Kitchen", "Cucina"),
    ("BATHROOM", "Badkamers", "Bathrooms", "Bagni"),
    ("LIVING", "Woonkamers", "Living rooms", "Soggiorni"),
    ("BEDROOM", "Slaapkamers", "Bedrooms", "Camere da letto"),
    ("ROOM", "Kamercontrole", "Room check", "Controllo stanza"),
    ("CELLAR", "Kelder", "Cellar", "Cantina"),
    ("GARAGE", "Garage", "Garage", "Garage"),
    ("ATTIC", "Zolder", "Attic", "Sottotetto"),
    ("GARDEN", "Tuin", "Garden", "Giardino"),
    ("TERRACE", "Terras", "Terrace", "Terrazzo"),
    ("BALCONY", "Balkon", "Balcony", "Balcone"),
    ("GROUNDS", "Terrein", "Grounds", "Terreno"),
    ("RETAINING", "Keermuren", "Retaining walls", "Muri di contenimento"),
    ("SLOPES", "Berghellingen", "Hillsides", "Pendii"),
    ("DRAINAGE", "Drainage", "Drainage", "Drenaggio"),
    ("ACCESS", "Bereikbaarheid", "Accessibility", "Accessibilità"),
    ("SURROUNDINGS", "Omgeving", "Surroundings", "Dintorni"),
    ("AMENITIES", "Voorzieningen", "Amenities", "Servizi"),
    ("INTERNET", "Internet", "Internet", "Internet"),
    ("MOBILE", "Mobiele ontvangst", "Mobile reception", "Copertura cellulare"),
    ("BUILDING_DOCS", "Bouwkundige documenten", "Building documents", "Documenti tecnici"),
    ("LEGAL_DOCS", "Juridische documenten", "Legal documents", "Documenti legali"),
]

TOOLS = {
    "torch": ("Zaklamp", "Torch", "Torcia"),
    "moisture_meter": ("Vochtmeter (optioneel)", "Moisture meter (optional)", "Igrometro (facoltativo)"),
    "tape": ("Rolmaat", "Tape measure", "Metro"),
    "camera": ("Telefooncamera", "Phone camera", "Fotocamera del telefono"),
    "binoculars": ("Verrekijker", "Binoculars", "Binocolo"),
    "crack_gauge": ("Scheurmeter of liniaal met mm", "Crack gauge or mm ruler", "Fessurimetro o righello in mm"),
    "level": ("Waterpas", "Spirit level", "Livella"),
    "marble": ("Knikker of balletje", "Marble or small ball", "Biglia o pallina"),
    "mirror": ("Klein spiegeltje", "Small mirror", "Specchietto"),
    "socket_tester": ("Stopcontacttester (insteekbaar)", "Plug-in socket tester", "Tester per prese (a spina)"),
    "thermometer": ("Thermometer/hygrometer", "Thermo-hygrometer", "Termoigrometro"),
    "compass": ("Kompas-app", "Compass app", "App bussola"),
    "phone": ("Telefoon (ontvangst/snelheidstest)", "Phone (signal/speed test)", "Telefono (segnale/speed test)"),
    "notebook": ("Notities in de app", "Notes in the app", "Note nell'app"),
    "gloves": ("Handschoenen", "Gloves", "Guanti"),
    "maps": ("Kaarten-app", "Maps app", "App mappe"),
    "water_tap": ("Kraan open (met toestemming)", "Running tap (with permission)", "Rubinetto aperto (con permesso)"),
}

def Q(code, cat, nl, en, it, **kw):
    """Shorthand: tuples of 9 (t,e,s,f,n,a,c,q,r) get empty warning/expert fields; 10 adds a warning."""
    def pad(t):
        t = tuple(t)
        return t + ("",) * (11 - len(t))
    P(code, cat, pad(nl), pad(en), pad(it), **kw)
