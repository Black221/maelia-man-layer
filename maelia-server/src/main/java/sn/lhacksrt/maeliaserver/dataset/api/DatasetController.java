package sn.lhacksrt.maeliaserver.dataset.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sn.lhacksrt.maeliaserver.dataset.api.dto.DatasetFileDto;
import sn.lhacksrt.maeliaserver.dataset.api.dto.DatasetResponse;
import sn.lhacksrt.maeliaserver.dataset.api.dto.ValidationReportDto;
import sn.lhacksrt.maeliaserver.dataset.application.materializer.IncludesMaterializer;
import sn.lhacksrt.maeliaserver.dataset.application.service.BulkImportService;
import sn.lhacksrt.maeliaserver.dataset.application.service.DatasetService;
import sn.lhacksrt.maeliaserver.dataset.application.service.ShpUploadService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DatasetController {

    private final DatasetService service;
    private final ShpUploadService shpUpload;
    private final BulkImportService bulkImport;
    private final IncludesMaterializer materializer;

    public DatasetController(DatasetService service, ShpUploadService shpUpload,
                             BulkImportService bulkImport, IncludesMaterializer materializer) {
        this.service = service;
        this.shpUpload = shpUpload;
        this.bulkImport = bulkImport;
        this.materializer = materializer;
    }

    /** Liste les datasets d'un projet (sans les enregistrements). */
    @GetMapping("/projects/{projectId}/datasets")
    public List<DatasetResponse> listByProject(@PathVariable UUID projectId) {
        return service.listByProject(projectId).stream()
                .map(DatasetResponse::summary)
                .toList();
    }

    /** Crée ou retrouve le dataset pour (projectId, dataSpecId). */
    @PostMapping("/projects/{projectId}/datasets/{dataSpecId}")
    public ResponseEntity<DatasetResponse> getOrCreate(
            @PathVariable UUID projectId,
            @PathVariable String dataSpecId) {
        var ds = service.getOrCreate(projectId, dataSpecId);
        return ResponseEntity.ok(DatasetResponse.from(ds));
    }

    /** Lit un dataset avec ses enregistrements. */
    @GetMapping("/datasets/{id}")
    public ResponseEntity<DatasetResponse> get(@PathVariable UUID id) {
        return service.findById(id)
                .map(DatasetResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Remplace les enregistrements (saisie en grille). */
    @PutMapping("/datasets/{id}/records")
    public DatasetResponse upsertRecords(
            @PathVariable UUID id,
            @RequestBody List<Map<String, Object>> records) {
        return DatasetResponse.from(service.upsertRecords(id, records));
    }

    /** Import CSV multipart. */
    @PostMapping("/projects/{projectId}/datasets/{dataSpecId}/import")
    public ResponseEntity<DatasetResponse> importCsv(
            @PathVariable UUID projectId,
            @PathVariable String dataSpecId,
            @RequestParam("file") MultipartFile file) throws Exception {
        var ds = service.importCsv(projectId, dataSpecId, file);
        return ResponseEntity.ok(DatasetResponse.summary(ds));
    }

    /**
     * C8 — Upload d'un shapefile : archive .zip contenant le jeu .shp/.shx/.dbf (+ .prj…).
     * Les fichiers sont renommés sur le nom attendu par le modèle et stockés dans MinIO ;
     * ils écrasent ceux du socle à la matérialisation. Le dataset passe VALIDE.
     */
    @PostMapping("/projects/{projectId}/datasets/{dataSpecId}/shp")
    public List<DatasetFileDto> uploadShp(
            @PathVariable UUID projectId,
            @PathVariable String dataSpecId,
            @RequestParam("file") MultipartFile file) throws Exception {
        return shpUpload.upload(projectId, dataSpecId, file).stream()
                .map(DatasetFileDto::from)
                .toList();
    }

    /** Fichiers uploadés pour (projectId, dataSpecId) — vide si le socle est utilisé. */
    @GetMapping("/projects/{projectId}/datasets/{dataSpecId}/files")
    public List<DatasetFileDto> listFiles(
            @PathVariable UUID projectId,
            @PathVariable String dataSpecId) {
        return shpUpload.listFiles(projectId, dataSpecId).stream()
                .map(DatasetFileDto::from)
                .toList();
    }

    /**
     * Initialisation en masse : archive ZIP contenant un maximum de fichiers d'entrée
     * (CSV + shapefiles). Chaque entrée est appariée au catalogue par son nom de fichier ;
     * rapport détaillé par fichier (VALIDE / INVALIDE / IGNORE / ERREUR).
     */
    @PostMapping("/projects/{projectId}/datasets/import-zip")
    public BulkImportService.BulkImportReport importZip(
            @PathVariable UUID projectId,
            @RequestParam("file") MultipartFile file) throws Exception {
        return bulkImport.importZip(projectId, file);
    }

    /** Lance la validation, retourne le rapport. */
    @PostMapping("/datasets/{id}/validate")
    public ValidationReportDto validate(@PathVariable UUID id) {
        return ValidationReportDto.from(service.validate(id));
    }

    /** Matérialise l'arborescence includes/ dans le volume gama-workspace. */
    @PostMapping("/projects/{projectId}/materialize")
    public ResponseEntity<Map<String, String>> materialize(@PathVariable UUID projectId) throws Exception {
        var path = materializer.materialize(projectId);
        return ResponseEntity.ok(Map.of("path", path.toString()));
    }
}
