package br.pauta.assistant;

import br.pauta.domain.Match;
import br.pauta.domain.Side;
import br.pauta.domain.TeamNames;
import br.pauta.ingest.MatchRepository;
import br.pauta.insight.Benchmark;
import br.pauta.insight.BenchmarkGroups;
import br.pauta.insight.LossBreakdownService;
import br.pauta.insight.OpportunityService;
import br.pauta.insight.PautaItem;
import br.pauta.insight.RecommendationService;
import br.pauta.insight.Verdict;
import br.pauta.metrics.Situation;
import br.pauta.metrics.SituationEngine;
import br.pauta.metrics.Tally;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base de conhecimento do assistente: documentos gerados da análise de cada time (relatório, situações por mapa,
 * perdas) mais os documentos gerais (definições, sugestões de foco, metodologia), indexados com BM25.
 * Cada combinação time + referência é montada uma vez e fica em cache.
 */
@Service
public class KnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBase.class);

    /** Desempenho do time num mapa (dados brutos das partidas, não da referência). */
    public record MapRecord(String map, int maps, int mapsWon, int rounds, int roundsWon) {
        double roundWinRate() {
            return rounds == 0 ? 0 : (double) roundsWon / rounds;
        }
    }

    /** Tudo o que o assistente sabe sobre um time numa referência. */
    public record TeamKnowledge(String team, String displayName, Benchmark benchmark,
                                OpportunityService.TeamReport report, List<PautaItem> items,
                                LossBreakdownService.Breakdown losses, Map<String, MapRecord> maps,
                                Bm25Index index) {
    }

    private record Key(String team, Benchmark benchmark) {
    }

    private final MatchRepository repository;
    private final SituationEngine engine;
    private final OpportunityService opportunities;
    private final LossBreakdownService lossService;
    private final BenchmarkGroups groups;
    private final List<KnowledgeDoc> generalDocs;
    private final Set<String> knownMaps;
    private final Map<Key, TeamKnowledge> cache = new ConcurrentHashMap<>();

    public KnowledgeBase(MatchRepository repository, SituationEngine engine, OpportunityService opportunities,
                         LossBreakdownService lossService, BenchmarkGroups groups) {
        this.repository = repository;
        this.engine = engine;
        this.opportunities = opportunities;
        this.lossService = lossService;
        this.groups = groups;
        this.knownMaps = new TreeSet<>(repository.findAll().stream().map(Match::map).toList());
        this.generalDocs = buildGeneralDocs();
    }

    /** Monta os índices de todos os times em segundo plano, para a primeira pergunta já ser rápida. */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        Thread.ofVirtual().name("assistente-indices").start(() -> {
            long start = System.nanoTime();
            Set<String> teams = new TreeSet<>();
            repository.findAll().forEach(m -> teams.addAll(m.teamName().values()));
            for (String team : teams) {
                for (Benchmark b : Benchmark.values()) {
                    forTeam(team, b);
                }
            }
            log.info("Assistente: índices de {} times prontos em {} ms", teams.size(),
                    (System.nanoTime() - start) / 1_000_000);
        });
    }

    public Set<String> knownMaps() {
        return knownMaps;
    }

    public List<KnowledgeDoc> generalDocs() {
        return generalDocs;
    }

    /** Lança {@code NotFoundException} (via OpportunityService) se o time não existir. */
    public TeamKnowledge forTeam(String team, Benchmark benchmark) {
        TeamKnowledge cached = cache.get(new Key(team, benchmark));
        if (cached != null) {
            return cached;
        }
        TeamKnowledge built = build(team, benchmark);
        cache.put(new Key(team, benchmark), built);
        return built;
    }

    private TeamKnowledge build(String team, Benchmark benchmark) {
        var report = opportunities.report(team, benchmark);
        List<PautaItem> items = opportunities.items(team, benchmark);
        var losses = lossService.breakdown(team);

        List<KnowledgeDoc> docs = new ArrayList<>(generalDocs);
        for (PautaItem i : items) {
            boolean overall = i.map() == null;
            String text = i.label() + ": " + Fmt.comparison(i) + "; " + Fmt.stake(i) + ". "
                    + (overall ? "Todos os mapas." : "Mapa " + TeamNames.map(i.map()) + ".");
            docs.add(new KnowledgeDoc("item:" + i.id(), overall ? KnowledgeDoc.Kind.SITUATION
                    : KnowledgeDoc.Kind.SITUATION_MAP, i.label(), text, i.situationId(), i.map(),
                    Vocabulary.keywords(i.situationId()) + (overall ? "" : " " + i.map())));
        }
        for (var slice : losses.categories()) {
            String text = slice.label() + ": " + slice.rounds() + " rounds perdidos, " + Fmt.pct(slice.share())
                    + " das derrotas do time; na liga, " + Fmt.pct(slice.leagueShare()) + " das derrotas.";
            docs.add(new KnowledgeDoc("loss:" + slice.id().name(), KnowledgeDoc.Kind.LOSS, "Onde perdemos: "
                    + slice.label(), text, slice.id().situationId(), null,
                    "derrota perdemos perder perdido"));
        }
        return new TeamKnowledge(team, report.displayName(), benchmark, report, items, losses, mapRecords(team),
                new Bm25Index(docs));
    }

    private Map<String, MapRecord> mapRecords(String team) {
        Map<String, int[]> acc = new TreeMap<>();
        for (Match m : repository.findByTeam(team)) {
            Side side = m.sideOf(team).orElseThrow();
            int[] a = acc.computeIfAbsent(m.map(), k -> new int[4]);
            a[0]++;
            if (m.winner() == side) a[1]++;
            a[2] += m.rounds().size();
            a[3] += m.roundsWon().get(side);
        }
        Map<String, MapRecord> out = new TreeMap<>();
        acc.forEach((map, a) -> out.put(map, new MapRecord(map, a[0], a[1], a[2], a[3])));
        return out;
    }

    private List<KnowledgeDoc> buildGeneralDocs() {
        List<KnowledgeDoc> docs = new ArrayList<>();
        for (Situation s : engine.situations()) {
            Tally league = engine.league(s.id());
            docs.add(new KnowledgeDoc("def:" + s.id(), KnowledgeDoc.Kind.DEFINITION, s.label(),
                    "Como medimos " + Fmt.lowerFirst(s.label()) + ": " + s.definition() + " Na liga, a taxa é "
                            + Fmt.pct(league.rate()) + " (" + league.successes() + " de " + league.total() + ").",
                    s.id(), null, Vocabulary.keywords(s.id()) + " definicao significa medimos calcula"));
            String focus = RecommendationService.focusFor(s.id());
            if (focus != null) {
                docs.add(new KnowledgeDoc("drill:" + s.id(), KnowledgeDoc.Kind.DRILL, "Treino: " + s.label(),
                        "Sugestão de foco de treino (heurística de coaching, não conclusão dos dados): " + focus + ".",
                        s.id(), null, Vocabulary.keywords(s.id()) + " treino treinar melhorar exercicio"));
            }
        }
        int top = groups.topQuartile().size();
        method(docs, "league", "Média da liga",
                "A média da liga usa todos os times da base, sem o próprio time. No geral, a referência é ponderada "
                        + "pelos mapas que o time jogou: cada mapa entra com a taxa da referência naquele mapa.",
                "referencia media liga comparacao");
        method(docs, "top25", "Quartil superior",
                "Os 25% dos times com maior taxa de rounds vencidos, entre os que jogaram pelo menos 8 mapas ("
                        + top + " times). A média da liga mostra o padrão geral, incluindo os times mais fracos. "
                        + "O próprio time não entra na própria referência.",
                "quartil superior top 25 melhores times referencia");
        method(docs, "verdicts", "Vereditos",
                "Evidência forte: a diferença continua significativa depois de descontar as múltiplas comparações "
                        + "(Benjamini-Hochberg, q = " + Fmt.pct(Verdict.FDR) + "). Indício: a referência fica fora do "
                        + "intervalo de 95% do time (teste de score, p < " + Fmt.pct(Verdict.ALPHA) + "), sem passar "
                        + "nesse controle. Sem evidência: a diferença pode ser acaso. Amostra pequena: menos de "
                        + Verdict.MIN_SAMPLE + " ocorrências, não dá para concluir.",
                "veredito evidencia forte indicio acaso amostra pequena significativo confianca");
        method(docs, "stake", "Rounds em jogo (saldo)",
                "Saldo = (taxa da referência - taxa do time) x ocorrências. Positivo: rounds que o time deixaria de "
                        + "perder jogando como a referência. Negativo: rounds a favor, um ponto forte.",
                "saldo rounds em jogo prioridade ordem");
        method(docs, "interval", "Intervalo de 95%",
                "Intervalo de Wilson de 95% para a taxa do time: a faixa onde a taxa real provavelmente está, "
                        + "dado o tamanho da amostra. Quanto menos ocorrências, mais larga a faixa.",
                "intervalo confianca wilson margem erro");
        method(docs, "losses", "Onde perdemos",
                "Cada round perdido entra em uma única categoria, a primeira que se aplica nesta ordem: pistol, "
                        + "compra baixa contra compra alta, ataque com plant, ataque sem plant, retake e defesa "
                        + "eliminada antes do plant. A comparação é com a fatia de cada categoria na liga.",
                "onde perdemos derrota categoria perder");
        method(docs, "vod", "Fila de VOD",
                "A fila de VOD lista os rounds em que o time não teve sucesso na situação, com mapa, adversário, "
                        + "torneio, data, placar e número do round, para revisar no vídeo.",
                "vod rever revisar video replay assistir");
        method(docs, "base", "Base de dados",
                "A análise usa só os arquivos JSON da organização: " + repository.findAll().size() + " mapas de "
                        + "partidas do Univavá e da Rush Series, sem dados externos.",
                "base dados partidas fonte json");
        method(docs, "assistant", "Como o assistente responde",
                "O assistente não gera texto livre. Ele reconhece a intenção da pergunta e a situação ou o mapa "
                        + "citados, busca os trechos da análise do time num índice BM25 e monta a resposta com "
                        + "frases-modelo e os números reais, mostrando as fontes.",
                "assistente funciona responde ia inteligencia artificial rag");
        return List.copyOf(docs);
    }

    private static void method(List<KnowledgeDoc> docs, String id, String title, String text, String keywords) {
        docs.add(new KnowledgeDoc("method:" + id, KnowledgeDoc.Kind.METHOD, title, text, null, null, keywords));
    }
}
