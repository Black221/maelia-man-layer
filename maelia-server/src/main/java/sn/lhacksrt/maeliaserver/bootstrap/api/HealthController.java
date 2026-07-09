package sn.lhacksrt.maeliaserver.bootstrap.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint de santé applicatif — GET /api/v1/health.
 * Utilisé par le frontend pour afficher « backend OK » (GATE M0).
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "maelia-platform",
                "version", "0.1.0",
                "timestamp", Instant.now().toString()
        ));
    }
}
