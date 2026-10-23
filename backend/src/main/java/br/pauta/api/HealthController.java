package br.pauta.api;

import br.pauta.ingest.DataQualityReport;
import br.pauta.ingest.MatchRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final MatchRepository repository;

    public HealthController(MatchRepository repository) {
        this.repository = repository;
    }

    /** Relatorio de integridade da base (tambem e o slide de "confiabilidade" do pitch). */
    @GetMapping("/data")
    public DataQualityReport data() {
        return repository.report();
    }
}
