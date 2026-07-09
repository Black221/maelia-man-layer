package sn.lhacksrt.maeliaserver.result.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "result_value")
public class ResultValueJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "run_id", nullable = false, columnDefinition = "uuid")
    private UUID runId;

    @Column(nullable = false, length = 255)
    private String indicator;

    @Column(length = 255)
    private String zone;

    @Column(name = "obs_date")
    private LocalDate obsDate;

    private Integer cycle;

    @Column(nullable = false)
    private double value;

    @Column(length = 50)
    private String unit;

    protected ResultValueJpaEntity() {}

    public ResultValueJpaEntity(UUID id, UUID runId, String indicator, String zone,
                                LocalDate obsDate, Integer cycle, double value, String unit) {
        this.id = id;
        this.runId = runId;
        this.indicator = indicator;
        this.zone = zone;
        this.obsDate = obsDate;
        this.cycle = cycle;
        this.value = value;
        this.unit = unit;
    }

    public UUID getId()         { return id; }
    public UUID getRunId()      { return runId; }
    public String getIndicator(){ return indicator; }
    public String getZone()     { return zone; }
    public LocalDate getObsDate() { return obsDate; }
    public Integer getCycle()   { return cycle; }
    public double getValue()    { return value; }
    public String getUnit()     { return unit; }
}
