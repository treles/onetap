package br.pauta.api;

import br.pauta.domain.TeamNames;
import br.pauta.insight.Benchmark;
import br.pauta.insight.BenchmarkGroups;
import br.pauta.insight.Verdict;
import br.pauta.metrics.SituationEngine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dados da página "Como calculamos": definições, referências e parâmetros estatísticos. */
@RestController
@RequestMapping("/api/methodology")
public class MethodologyController {

    private final SituationEngine engine;
    private final BenchmarkGroups groups;

    public MethodologyController(SituationEngine engine, BenchmarkGroups groups) {
        this.engine = engine;
        this.groups = groups;
    }

    public record SituationInfo(String id, String label, String definition, String unit, int leagueSuccesses,
                                int leagueTotal, double leagueRate) {
    }

    public record BenchmarkInfo(Benchmark id, String label, int teams, List<String> members) {
    }

    public record Methodology(List<SituationInfo> situations, List<BenchmarkInfo> benchmarks, int minSample,
                              double alpha, double fdr) {
    }

    @GetMapping
    public Methodology methodology() {
        List<SituationInfo> situations = engine.situations().stream().map(s -> {
            var league = engine.league(s.id());
            return new SituationInfo(s.id(), s.label(), s.definition(), s.unit(), league.successes(), league.total(),
                    league.rate());
        }).toList();
        List<BenchmarkInfo> benchmarks = List.of(
                new BenchmarkInfo(Benchmark.LEAGUE, Benchmark.LEAGUE.label(), groups.teams(Benchmark.LEAGUE).size(), List.of()),
                new BenchmarkInfo(Benchmark.TOP25, Benchmark.TOP25.label(), groups.topQuartile().size(),
                        groups.topQuartile().stream().map(TeamNames::display).toList()));
        return new Methodology(situations, benchmarks, Verdict.MIN_SAMPLE, Verdict.ALPHA, Verdict.FDR);
    }
}
