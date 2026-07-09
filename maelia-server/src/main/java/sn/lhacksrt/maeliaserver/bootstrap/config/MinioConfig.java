package sn.lhacksrt.maeliaserver.bootstrap.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Client MinIO + création du bucket applicatif au démarrage.
 *
 * Le stockage objet n'est pas encore sur le chemin critique (les artefacts sont lus sur le
 * volume gama-workspace) : l'initialisation est best-effort — MinIO indisponible n'empêche
 * pas l'application de démarrer, on journalise et le bucket sera créé au prochain démarrage.
 */
@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Bean
    public MinioClient minioClient(@Value("${minio.endpoint}") String endpoint,
                                   @Value("${minio.access-key}") String accessKey,
                                   @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public ApplicationRunner minioBucketInitializer(MinioClient client,
                                                    @Value("${minio.bucket}") String bucket) {
        return args -> {
            try {
                boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Bucket MinIO '{}' créé", bucket);
                } else {
                    log.info("Bucket MinIO '{}' déjà présent", bucket);
                }
            } catch (Exception e) {
                log.warn("Initialisation du bucket MinIO '{}' impossible ({}) — sera retentée au prochain démarrage",
                        bucket, e.getMessage());
            }
        };
    }
}
