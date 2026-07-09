package sn.lhacksrt.maeliaserver.dataset.infrastructure.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.FileStoragePort;

import java.io.InputStream;

/**
 * Adaptateur MinIO du stockage objet (C8) : les fichiers SHP uploadés par
 * l'utilisateur y sont conservés, puis relus à chaque matérialisation.
 * Le bucket est créé au démarrage par MinioConfig (bootstrap).
 */
@Component
public class MinioFileStorage implements FileStoragePort {

    private final MinioClient client;
    private final String bucket;

    public MinioFileStorage(MinioClient client, @Value("${minio.bucket}") String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void put(String objectKey, InputStream content, long sizeBytes, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(content, sizeBytes, -1)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Écriture MinIO impossible (" + objectKey + ") : " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream get(String objectKey) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Lecture MinIO impossible (" + objectKey + ") : " + e.getMessage(), e);
        }
    }
}
