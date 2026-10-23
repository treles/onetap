package br.pauta;

import br.pauta.ingest.DataQualityReport;
import br.pauta.ingest.MatchRepository;
import br.pauta.insight.Benchmark;
import br.pauta.insight.BenchmarkGroups;
import br.pauta.insight.LossBreakdownService;
import br.pauta.insight.OpportunityService;
import br.pauta.insight.PautaItem;
import br.pauta.insight.RecommendationService;
import br.pauta.insight.Verdict;
import br.pauta.metrics.SituationEngine;
import br.pauta.metrics.Tally;
import br.pauta.metrics.situations.AntiEco;
import br.pauta.metrics.situations.AttackPlantRate;
import br.pauta.metrics.situations.AttackRounds;
import br.pauta.metrics.situations.Clutch;
import br.pauta.metrics.situations.DefenseRounds;
import br.pauta.metrics.situations.FirstBloodConversion;
import br.pauta.metrics.situations.FirstBloodReaction;
import br.pauta.metrics.situations.FirstDeathTraded;
import br.pauta.metrics.situations.LowBuyVsFull;
import br.pauta.metrics.situations.PistolRound;
import br.pauta.metrics.situations.PostPlant;
import br.pauta.metrics.situations.Retake;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * "Números de ouro": valores calculados de forma independente pelos scripts Python (tools/analise)
 * sobre a base oficial. Se o Java divergir, há bug em um dos dois: investigue o round antes de ajustar o teste.
 * Este teste é PULADO quando a pasta data/ não existe (ex.: no CI, já que a base não vai para o Git).
 */
class GoldenNumbersTest {

    private static final Path DATA = Path.of("../data");
    private static MatchRepository repository;
    private static SituationEngine engine;
    private static OpportunityService opportunities;
    private static LossBreakdownService losses;

    @BeforeAll
    static void load() {
        assumeTrue(Files.isDirectory(DATA), "Base não encontrada em ../data - teste pulado");
        repository = new MatchRepository(new PautaProperties(DATA.toString()), JsonMapper.builder().build());
        engine = new SituationEngine(List.of(new AttackRounds(), new DefenseRounds(), new FirstBloodConversion(),
                new FirstBloodReaction(), new FirstDeathTraded(), new PistolRound(), new PostPlant(), new Retake(),
                new AntiEco(), new LowBuyVsFull(), new AttackPlantRate(), new Clutch()), repository);
        opportunities = new OpportunityService(engine, repository, new BenchmarkGroups(repository),
                new RecommendationService());
        losses = new LossBreakdownService(repository);
    }

    @Test
    void integridadeDaBase() {
        DataQualityReport r = repository.report();
        assertEquals(336, r.matches());
        assertEquals(0, r.filesRejected());
        assertEquals(6618, r.rounds());
        assertEquals(46753, r.rawKillEvents());
        assertEquals(22, r.observersFiltered());
        assertEquals(3360, r.playerMatchLines());
        assertEquals(0, r.killReconciliationMismatches(), "abates limpos devem bater 100% com stats.kills");
    }

    @ParameterizedTest(name = "liga {0} = {1}")
    @CsvSource({
            "attack, 0.518", "defense, 0.482", "first-blood-conversion, 0.713", "first-blood-reaction, 0.287",
            "first-death-traded, 0.202", "pistol, 0.500", "post-plant, 0.708", "retake, 0.292",
            "anti-eco, 0.897", "low-buy-vs-full, 0.202", "attack-plant-rate, 0.703", "clutch, 0.064"})
    void referenciaisDaLiga(String situation, double expected) {
        assertEquals(expected, engine.league(situation).rate(), 0.0015);
    }

    @ParameterizedTest(name = "{0} em {1} = {2}/{3}")
    @CsvSource({
            "octacore_python, attack, 41, 99",
            "octacore_python, first-blood-reaction, 23, 103",
            "octacore_python, attack-plant-rate, 59, 99",
            "ufu_saints, post-plant, 124, 148",
            "ufu_saints, first-blood-reaction, 68, 179",
            "ufu_saints, first-death-traded, 40, 179",
            "ufu_saints, retake, 41, 112",
            "ufu_saints, anti-eco, 15, 16",
            "ufu_saints, low-buy-vs-full, 16, 57",
            "ufu_saints, clutch, 15, 141"})
    void numerosDosTimes(String team, String situation, int successes, int total) {
        Tally t = engine.team(situation, team);
        assertEquals(successes, t.successes());
        assertEquals(total, t.total());
        assertEquals(total - successes, t.failures().size(), "fila de VOD = ocorrências sem sucesso");
    }

    @Test
    void identidadesDeSoma() {
        assertEquals(1.0, engine.league("first-blood-conversion").rate() + engine.league("first-blood-reaction").rate(), 1e-9);
        assertEquals(1.0, engine.league("post-plant").rate() + engine.league("retake").rate(), 1e-9);
        assertEquals(1.0, engine.league("attack").rate() + engine.league("defense").rate(), 1e-9);
    }

    @Test
    void ataqueDaOctacoreEmAscent() {
        PautaItem ascent = opportunities.situation("octacore_python", "attack", Benchmark.LEAGUE).byMap().stream()
                .filter(i -> "ascent".equals(i.map())).findFirst().orElseThrow();
        assertEquals(13, ascent.successes());
        assertEquals(45, ascent.n());
        // o ataque da liga (sem a própria octacore) vence ~47% em Ascent
        assertEquals(0.473, ascent.benchmarkRate(), 0.01);
        assertTrue(ascent.verdict().hasEvidence(), "Ascent no ataque deve ter ao menos indício");
    }

    @Test
    void referenciaGeralEPonderadaPelosMapasDoTime() {
        PautaItem overall = opportunities.situation("octacore_python", "attack", Benchmark.LEAGUE).overall();
        // 45 rounds em Ascent (~47%), 33 em Lotus (~53%), 21 em Split (~54%) => ~50,7%, não os 51,8% gerais
        assertEquals(0.507, overall.benchmarkRate(), 0.005);
        assertEquals(Verdict.NOT_PROVEN, overall.verdict(), "no geral a diferença some quando o mapa é considerado");
    }

    @Test
    void timeForteGanhaPautaContraOQuartilSuperior() {
        var league = opportunities.report("ufu_saints", Benchmark.LEAGUE);
        assertTrue(league.attention().isEmpty(), "ufu_saints não fica abaixo da liga em nada com evidência");
        var top = opportunities.report("ufu_saints", Benchmark.TOP25);
        assertTrue(top.benchmarkTeams() > 0);
        assertTrue(top.situations().size() >= 10);
    }

    @Test
    void perdasSomamOTotalDeRoundsPerdidos() {
        var b = losses.breakdown("octacore_python");
        assertEquals(b.roundsLost(), b.categories().stream().mapToInt(LossBreakdownService.Slice::rounds).sum());
        assertEquals(1.0, b.categories().stream().mapToDouble(LossBreakdownService.Slice::share).sum(), 1e-9);
        assertEquals(1.0, b.categories().stream().mapToDouble(LossBreakdownService.Slice::leagueShare).sum(), 1e-9);
    }
}
