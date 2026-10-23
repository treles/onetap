package br.pauta.assistant;

import br.pauta.NotFoundException;
import br.pauta.insight.Benchmark;
import br.pauta.insight.OpportunityService;
import br.pauta.insight.PautaItem;
import br.pauta.insight.Verdict;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** O assistente responde com os números da análise e nunca com algo fora dela. Usa a base em ../data. */
@SpringBootTest
class AssistantServiceTest {

    private static final String TEAM = "octacore_python";

    @Autowired
    AssistantService assistant;

    @Autowired
    OpportunityService opportunities;

    @Autowired
    KnowledgeBase knowledge;

    @BeforeEach
    void precisaDaBase() {
        assumeTrue(Files.isDirectory(Path.of("../data")), "base em ../data não encontrada");
    }

    @Test
    void todaSugestaoTemResposta() {
        for (Benchmark b : Benchmark.values()) {
            for (var s : assistant.suggestions(TEAM, b)) {
                var a = assistant.answer(TEAM, b, s.question());
                assertNotEquals("UNKNOWN", a.intent(), s.question());
                assertFalse(a.paragraphs().isEmpty() && a.sections().isEmpty(), s.question());
            }
        }
    }

    @Test
    void planoUsaOsPontosDeAtencaoDoRelatorio() {
        var report = opportunities.report(TEAM, Benchmark.LEAGUE);
        PautaItem top = report.attention().getFirst();
        var plan = assistant.answer(TEAM, Benchmark.LEAGUE, AssistantService.Q_PLAN);

        assertEquals("PLAN", plan.intent());
        var first = plan.sections().getFirst();
        assertTrue(first.title().startsWith("Foco 1: " + top.label()), first.title());
        assertTrue(first.lines().getFirst().contains(Fmt.pct(top.teamRate()) + " contra " + Fmt.pct(top.benchmarkRate())),
                first.lines().getFirst());
        assertEquals(top.situationId(), first.action().situationId());
        assertTrue(plan.sources().stream().anyMatch(s -> s.id().equals("item:" + top.id())));
    }

    @Test
    void divisaoDoTreinoSomaCemEmTodoTime() {
        for (String team : List.of(TEAM, "ufu_saints", "pucc_cardinals", "azure_bears_black")) {
            for (Benchmark b : Benchmark.values()) {
                List<PautaItem> focus = AssistantService.focuses(knowledge.forTeam(team, b));
                if (focus.isEmpty()) continue;
                int[] shares = AssistantService.shares(focus);
                assertEquals(100, Arrays.stream(shares).sum(), team + " " + b);
                assertTrue(Arrays.stream(shares).allMatch(s -> s >= 10 && s % 5 == 0), team + " " + b);
            }
        }
    }

    @Test
    void divisaoNaoZeraOFocoComEvidenciaMaisForteESaldoMenor() {
        // focos na ordem do relatório: o primeiro (evidência forte) tem o menor saldo
        int[] shares = AssistantService.shares(List.of(
                item("a", 2, Verdict.STRONG), item("b", 20, Verdict.INDICATION), item("c", 20, Verdict.INDICATION)));
        assertEquals(100, Arrays.stream(shares).sum());
        assertTrue(Arrays.stream(shares).allMatch(s -> s >= 10), Arrays.toString(shares));
    }

    @Test
    void focosSeguemAOrdemDoRelatorioSemOLimiteDeDestaques() {
        var k = knowledge.forTeam(TEAM, Benchmark.TOP25);
        List<PautaItem> focus = AssistantService.focuses(k);
        assertEquals(k.report().attention().getFirst().id(), focus.getFirst().id());
        long withEvidence = k.items().stream().filter(i -> i.verdict().hasEvidence() && i.atStake() > 0)
                .map(PautaItem::situationId).distinct().count();
        assertEquals(Math.min(AssistantService.MAX_FOCUS, withEvidence), focus.size());
    }

    @Test
    void timeAcimaDaLigaRecebeFocosContraOQuartilSuperior() {
        var plan = assistant.answer("ufu_saints", Benchmark.LEAGUE, AssistantService.Q_PLAN);
        assertTrue(plan.paragraphs().stream().anyMatch(p -> p.startsWith("Contra o quartil superior")),
                String.join("\n", plan.paragraphs()));
        // um foco não pode reaparecer como "fora do plano"
        var focusSituations = plan.sections().stream().filter(s -> s.title().startsWith("Foco "))
                .map(s -> s.action() == null ? null : s.action().situationId()).toList();
        plan.sections().stream().filter(s -> s.title().startsWith("Fora do plano")).findFirst().ifPresent(out -> {
            assertTrue(out.title().endsWith("(contra a média da liga)"), out.title());
            for (String line : out.lines()) {
                for (String id : focusSituations) {
                    if (id == null) continue;
                    String label = knowledge.forTeam("ufu_saints", Benchmark.LEAGUE).index().doc("def:" + id)
                            .orElseThrow().title();
                    assertFalse(line.startsWith(label + ":"), line);
                }
            }
        });
    }

    @Test
    void perguntaForaDaAnaliseNaoInventaResposta() {
        var a = assistant.answer(TEAM, Benchmark.LEAGUE, "receita de bolo de cenoura");
        assertEquals("UNKNOWN", a.intent());
        assertTrue(a.sections().isEmpty());
        assertFalse(a.followUps().isEmpty());
    }

    @Test
    void metodologiaVemDaBuscaComRelevancia() {
        var a = assistant.answer(TEAM, Benchmark.LEAGUE, "O que é o quartil superior?");
        assertEquals("Quartil superior", a.title());
        assertTrue(a.sources().getFirst().score() > 0);
    }

    @Test
    void mapaNaoJogadoEDitoComHonestidade() {
        var played = opportunities.items(TEAM, Benchmark.LEAGUE).stream().map(PautaItem::map).toList();
        var notPlayed = knowledge.knownMaps().stream().filter(m -> !played.contains(m)).findFirst();
        assumeTrue(notPlayed.isPresent(), "o time jogou todos os mapas da base");
        var a = assistant.answer(TEAM, Benchmark.LEAGUE, "como estamos na " + notPlayed.get());
        assertEquals("MAP", a.intent());
        assertTrue(a.paragraphs().getFirst().contains("não tem partidas"), a.paragraphs().getFirst());
    }

    @Test
    void mapasMostramONomeDaSituacaoSemOMapa() {
        var a = assistant.answer(TEAM, Benchmark.LEAGUE, AssistantService.Q_MAPS);
        for (String line : a.sections().getFirst().lines()) {
            String map = line.substring(0, line.indexOf(':'));
            int at = line.indexOf("Maior saldo negativo:");
            if (at >= 0) {
                assertFalse(line.substring(at).contains(" em " + map), line);
            }
        }
    }

    private static PautaItem item(String id, double atStake, Verdict verdict) {
        return new PautaItem(id, id, null, id, "", "rounds", 10, 40, 0.25, 0.25 + atStake / 40, 0.1, 0.4, atStake,
                0.01, verdict);
    }

    @Test
    void perguntaVaziaETimeInexistente() {
        assertThrows(IllegalArgumentException.class, () -> assistant.answer(TEAM, Benchmark.LEAGUE, "  "));
        assertThrows(NotFoundException.class, () -> assistant.answer("time_que_nao_existe", Benchmark.LEAGUE, "plano"));
    }
}
