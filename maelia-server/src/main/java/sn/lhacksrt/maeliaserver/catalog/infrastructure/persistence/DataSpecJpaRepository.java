package sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DataSpecJpaRepository extends JpaRepository<DataSpecJpaEntity, String> {

    @Query("SELECT d FROM DataSpecJpaEntity d ORDER BY d.module, d.id")
    List<DataSpecJpaEntity> findAllOrdered();
}
