import pandas as pd
import requests
from bs4 import BeautifulSoup
import urllib3

# Désactiver les avertissements SSL (parfois nécessaire sur les vieux sites INRA)
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

urls = [
    "http://maelia-platform.inra.fr/donnees/donnees-agricoles/agriculteurs/",
    "http://maelia-platform.inra.fr/donnees/donnees-agricoles/biais-de-perception-agriculteurs/",
    "http://maelia-platform.inra.fr/donnees/donnees-agricoles/donnees-ilot/",
    "http://maelia-platform.inra.fr/donnees/donnees-agricoles/liste-des-cultures/",
    "http://maelia-platform.inra.fr/donnees/donnees-agricoles/regles-de-decision/",
    "http://maelia-platform.inra.fr/donnees/donnees-agricoles/materiels-dirrigation/",
    "http://maelia-platform.inra.fr/donnees/donnees-agricoles/donnees-parcelle/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/donnees-sol/",
    "http://maelia-platform.inra.fr/donnees/donnees-agricoles/systeme-de-culture/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/donnees-zone-meteo/",
    "http://maelia-platform.inra.fr/donnees/donnees-agricoles/marche-agricole-2/primes-couplees/",
    "http://maelia-platform.inra.fr/donnees/donnees-agricoles/marche-agricole-2/charges-operationnelles-hors-irrigation/",
    "http://maelia-platform.inra.fr/donnees/donnees-agricoles/marche-agricole-2/charges-operationnelles-irrigation/",
    "http://maelia-platform.inra.fr/donnees/donnees-agricoles/marche-agricole-2/charges-operationnelles-irrigation/charges-collectifs-dirrigation/",
    "http://maelia-platform.inra.fr/donnees/donnees-agricoles/marche-agricole-2/charges-de-passages/",
    "http://maelia-platform.inra.fr/donnees/donnees-agricoles/marche-agricole-2/charges-fixes-irrigation/",
    "http://maelia-platform.inra.fr/donnees/donnees-generales/donnees-equipement/",
    "http://maelia-platform.inra.fr/donnees/divers/altitude/",
    "http://maelia-platform.inra.fr/donnees/divers/contour/",
    "http://maelia-platform.inra.fr/donnees/divers/donnees-corrine-land-cover-clc/",
    "http://maelia-platform.inra.fr/donnees/divers/date/",
    "http://maelia-platform.inra.fr/donnees/donnees-normatives/donnees-secteur-administratif/",
    "http://maelia-platform.inra.fr/donnees/donnees-normatives/donnees-station-de-mesure/",
    "http://maelia-platform.inra.fr/donnees/donnees-normatives/donnees-unite-de-gestion/",
    "http://maelia-platform.inra.fr/donnees/donnees-normatives/donnees-zone-administrative/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/barrage/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/caracteristiques-des-cours-deau/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/canaux/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/donnees-hydrologic-response-unit-hru/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/noeuds-hydrographiques/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/ressource-en-eau/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/canaux-2/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/ressource-en-eau/cours-deau/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/ressource-en-eau/nappe-phreatique/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/ressource-en-eau/retenue-collinaire/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/donnees-sol/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/donnees-zone-hydrographique/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/donnees-zone-meteo/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/debit-dentree/",
    "http://maelia-platform.inra.fr/donnees/donnees-hydrologiques/serie-climatique/",
    "http://maelia-platform.inra.fr/donnees/donnees-generales/donnees-equipement/",
    "http://maelia-platform.inra.fr/donnees/donnees-generales/evolution-de-la-population/",
    "http://maelia-platform.inra.fr/donnees/donnees-generales/donnees-equipement/",
    "http://maelia-platform.inra.fr/donnees/donnees-generales/prix-de-leau-potable/",
    "http://maelia-platform.inra.fr/donnees/donnees-generales/salaire/",
    "http://maelia-platform.inra.fr/donnees/donnees-generales/taux-de-residences-secondaires/",
]

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
all_data = {}

for url in urls:
    # Créer un nom d'onglet propre à partir de l'URL
    topic = url.rstrip('/').split('/')[-1]
    if topic == "marche-agricole-2": continue # On skip la page parente qui est juste un sommaire
    
    print(f"Extraction de : {topic}...")
    try:
        response = requests.get(url, headers=headers, verify=False, timeout=10)
        soup = BeautifulSoup(response.text, 'html.parser')
        
        # MAELIA stocke généralement ses données dans des tableaux HTML (balises <table>)
        tables = pd.read_html(str(soup))
        
        if tables:
            # Prendre le premier tableau trouvé sur la page (généralement le bon)
            df = tables[0]
            all_data[topic] = df
        else:
            all_data[topic] = pd.DataFrame({"Message": ["Aucun tableau trouvé sur cette page"]})
            
    except Exception as e:
        all_data[topic] = pd.DataFrame({"Erreur": [str(e)]})

# Sauvegarder tout dans un fichier Excel avec un onglet par URL
with pd.ExcelWriter('MAELIA_Schema_Donnees.xlsx') as writer:
    for sheet_name, data in all_data.items():
        # Limiter le nom des onglets à 31 caractères (limite Excel)
        clean_name = sheet_name[:31] 
        data.to_excel(writer, sheet_name=clean_name, index=False)

print("Terminé ! Fichier 'MAELIA_Schema_Donnees.xlsx' créé.")