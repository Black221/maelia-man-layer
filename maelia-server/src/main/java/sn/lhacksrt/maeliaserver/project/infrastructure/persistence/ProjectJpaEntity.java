package sn.lhacksrt.maeliaserver.project.infrastructure.persistence;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "project")
@Getter @Setter @NoArgsConstructor
public class ProjectJpaEntity {

    // UUID assigné par le domaine (Project.create) — PAS @GeneratedValue, sinon Hibernate
    // réassigne un id au merge et le GET /{id} renvoyé par le POST tombe en 404.
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "study_area", nullable = false)
    private String studyArea = "ferlo-sine";

    @Type(JsonType.class)
    @Column(name = "modeling_configuration", columnDefinition = "jsonb")
    private Map<String, Object> modelingConfiguration;

    @Column(nullable = false)
    private String status = "ACTIF";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
