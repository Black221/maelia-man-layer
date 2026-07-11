# gama-headless/Dockerfile — Serveur GAMA headless (WebSocket)
# Part de l'image officielle publique et n'ajoute que deux réglages :
#   - WORKDIR /workspace : GAMA y crée ses workspaces Eclipse (.workspace*) et y lit/écrit le
#     modèle ; c'est le même chemin monté dans api et worker (GAMA_WORKSPACE=/workspace).
#   - CMD par défaut = mode socket sur 6868 (le docker-compose le redéfinit explicitement).
FROM gamaplatform/gama:latest

WORKDIR /workspace

CMD ["-socket", "6868"]
