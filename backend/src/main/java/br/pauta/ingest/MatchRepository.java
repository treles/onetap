package br.pauta.ingest;

import br.pauta.PautaProperties;
import br.pauta.domain.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Carrega todos os JSON uma unica vez na inicializacao e mantem as partidas em memoria (somente leitura).
 * Um arquivo ruim gera WARN e entra no relatorio, mas nao derruba a aplicacao.
 */
@Component
public class MatchRepository {

    private static final Logger log = LoggerFactory.getLogger(MatchRepository.class);

    private final List<Match> matches;
    private final DataQualityReport report;

    public MatchRepository(PautaProperties properties, JsonMapper jsonMapper) {
        Path dir = Path.of(properties.dataDir()).toAbsolutePath().normalize();
        IngestStats stats = new IngestStats();
        this.matches = load(dir, jsonMapper, stats);
        this.report = stats.toReport(dir.toString(), matches.size());
        log.info("Base carregada de {}: {} partidas, {} rounds, {} arquivos rejeitados, {} observadores filtrados, "
                        + "{} autoabates e {} abates de fogo amigo removidos, reconciliacao de kills {}/{}",
                dir, matches.size(), report.rounds(), report.filesRejected(), report.observersFiltered(),
                report.selfKillsRemoved(), report.teamKillsRemoved(),
                report.playerMatchLines() - report.killReconciliationMismatches(), report.playerMatchLines());
    }

    static List<Match> load(Path dir, JsonMapper jsonMapper, IngestStats stats) {
        if (!Files.isDirectory(dir)) {
            log.error("Pasta de dados nao encontrada: {}. Veja o README (secao 'Preparar os dados').", dir);
            return List.of();
        }
        List<Path> files;
        try (Stream<Path> walk = Files.walk(dir)) {
            files = walk.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao listar " + dir, e);
        }

        List<Match> result = new ArrayList<>();
        for (Path file : files) {
            stats.filesRead++;
            try (InputStream in = Files.newInputStream(file)) {
                MatchFileName name = MatchFileName.parse(file.getFileName().toString());
                RawMatch raw = jsonMapper.readValue(in, RawMatch.class);
                result.add(MatchAssembler.assemble(name, raw, stats));
            } catch (IOException | RuntimeException e) {   // inclui InvalidMatchFileException e erros do Jackson
                stats.filesRejected++;
                stats.rejected.add(file.getFileName() + ": " + e.getMessage());
                log.warn("Arquivo rejeitado {}: {}", file.getFileName(), e.getMessage());
            }
        }
        return List.copyOf(result);
    }

    public List<Match> findAll() {
        return matches;
    }

    public List<Match> findByTeam(String team) {
        return matches.stream().filter(m -> m.involves(team)).toList();
    }

    public DataQualityReport report() {
        return report;
    }
}
