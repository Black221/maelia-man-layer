package sn.lhacksrt.maeliaserver.dataset.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetJpaRepository extends JpaRepository<DatasetJpaEntity, UUID> {
    /** Dataset unique du type (hors instances multi-instance, qui portent un instanceKey). */
    Optional<DatasetJpaEntity> findByProjectIdAndDataSpecIdAndInstanceKeyIsNull(UUID projectId, String dataSpecId);
    /** Insensible à la casse : ré-importer "2018.CSV" met à jour l'instance "2018.csv". */
    Optional<DatasetJpaEntity> findByProjectIdAndDataSpecIdAndInstanceKeyIgnoreCase(UUID projectId, String dataSpecId, String instanceKey);
    List<DatasetJpaEntity> findByProjectId(UUID projectId);
    long countByDataSpecId(String dataSpecId);
}
