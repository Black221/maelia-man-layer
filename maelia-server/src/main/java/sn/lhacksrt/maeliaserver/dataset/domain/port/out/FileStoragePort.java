package sn.lhacksrt.maeliaserver.dataset.domain.port.out;

import java.io.InputStream;

/** Stockage objet des fichiers uploadés (adapté sur MinIO). */
public interface FileStoragePort {

    /** Écrit (ou remplace) l'objet et retourne sa clé. */
    void put(String objectKey, InputStream content, long sizeBytes, String contentType);

    /** Flux de lecture de l'objet ; à fermer par l'appelant. */
    InputStream get(String objectKey);
}
