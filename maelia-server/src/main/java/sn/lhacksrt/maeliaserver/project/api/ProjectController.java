package sn.lhacksrt.maeliaserver.project.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import sn.lhacksrt.maeliaserver.project.api.dto.*;
import sn.lhacksrt.maeliaserver.project.application.service.ProjectService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProjectResponse> list() {
        return service.listAll().stream().map(ProjectResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@RequestBody @Valid CreateProjectRequest req) {
        var project = service.create(req.name(), req.description());
        var uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(project.getId()).toUri();
        return ResponseEntity.created(uri).body(ProjectResponse.from(project));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> get(@PathVariable UUID id) {
        return service.findById(id)
                .map(ProjectResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Édition des informations générales du projet (nom, description). */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateProjectRequest req) {
        var updated = service.updateInfo(id, req.name(), req.description());
        return ResponseEntity.ok(ProjectResponse.from(updated));
    }

    @PutMapping("/{id}/modeling-configuration")
    public ResponseEntity<ProjectResponse> updateConfig(
            @PathVariable UUID id,
            @RequestBody @Valid ModelingConfigDto dto) {
        var updated = service.updateModelingConfiguration(id, dto.toDomain());
        return ResponseEntity.ok(ProjectResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        service.archive(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/completion")
    public ResponseEntity<List<CompletionEntryDto>> completion(@PathVariable UUID id) {
        var entries = service.getCompletion(id).stream()
                .map(CompletionEntryDto::from)
                .toList();
        return ResponseEntity.ok(entries);
    }
}
