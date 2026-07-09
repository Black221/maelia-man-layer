package sn.lhacksrt.maeliaserver.result.domain.model;

/** Catégorie d'artefact de sortie produit par GAMA. */
public enum ArtifactType {
    IMAGE,
    CSV,
    XML,
    OTHER;

    /** Déduit la catégorie depuis l'extension d'un nom de fichier. */
    public static ArtifactType fromFileName(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".svg") || lower.endsWith(".bmp")) {
            return IMAGE;
        }
        if (lower.endsWith(".csv")) return CSV;
        if (lower.endsWith(".xml")) return XML;
        return OTHER;
    }
}
