package sn.lhacksrt.maeliaserver.project.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, UUID> {

    @Query("SELECT p FROM ProjectJpaEntity p WHERE p.status = 'ACTIF' ORDER BY p.createdAt DESC")
    List<ProjectJpaEntity> findAllActive();
}
