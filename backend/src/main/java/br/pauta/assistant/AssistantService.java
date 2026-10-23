package br.pauta.assistant;

import br.pauta.assistant.Bm25Index.Hit;
import br.pauta.assistant.KnowledgeBase.MapRecord;
import br.pauta.assistant.KnowledgeBase.TeamKnowledge;
import br.pauta.assistant.QueryParser.Parsed;
import br.pauta.domain.TeamNames;
import br.pauta.insight.Benchmark;
import br.pauta.insight.LossBreakdownService.Slice;
import br.pauta.insight.OpportunityService;
import br.pauta.insight.PautaItem;
import br.pauta.insight.RecommendationService;
import br.pauta.insight.Verdict;
import br.pauta.metrics.RoundRef;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Assistente do treinador, sem modelo generativo. Para cada pergunta:
 * entende a intenção e as entidades ({@link QueryParser}), recupera os trechos da análise do time
 * (filtros estruturados ou BM25 na {@link KnowledgeBase}) e monta a resposta com frases-modelo.
 * Todo número da resposta vem da análise; cada resposta lista as fontes usadas.
 */
@Service
public class AssistantService {

    static final int MAX_FOCUS = 3;
    static final int VOD_PREVIEW = 3;
    /** Abaixo disso, a busca livre não achou nada parecido o bastante para responder. */
    static final double MIN_SEARCH_SCORE = 1.0;
    /** Diferença menor que meio round não é tratada como problema nem como ponto forte. */
    private static final double NEUTRAL = 0.5;
    /** Diferença mínima (em pontos percentuais) para destacar uma categoria de derrota acima da liga. */
    private static final double MIN_LOSS_GAP = 0.02;

    static final String Q_PLAN = "Monte um plano de treino para a semana";
    static final String Q_PROBLEM = "Qual é o nosso maior problema?";
    static final String Q_VOD = "Quais rounds devemos rever primeiro?";
    static final String Q_LOSSES = "Onde perdemos mais rounds?";
    static final String Q_STRENGTHS = "Quais são nossos pontos fortes?";
    static final String Q_MAPS = "Como estamos em cada mapa?";
    static final String Q_TOP25 = "O que é o quartil superior?";

    public record Suggestion(String label, String question) {
    }

    /** Botão que abre a fila de VOD de uma situação (opcionalmente num mapa). */
    public record VodLink(String label, String situationId, String map, int rounds) {
    }

    public record Section(String title, List<String> lines, VodLink action) {
    }

    /** Trecho da base usado na resposta; {@code score} só existe quando veio da busca BM25. */
    public record Source(String id, String kind, String title, Double score) {
    }

    public record Answer(String question, String intent, String title, List<String> paragraphs,
                         List<Section> sections, List<VodLink> actions, List<Source> sources,
                         List<String> followUps, double tookMs) {
    }

    private final KnowledgeBase knowledge;
    private final OpportunityService opportunities;

    public AssistantService(KnowledgeBase knowledge, OpportunityService opportunities) {
        this.knowledge = knowledge;
        this.opportunities = opportunities;
    }

    // ------------------------------------------------------------------ sugestões

    public List<Suggestion> suggestions(String team, Benchmark benchmark) {
        TeamKnowledge k = knowledge.forTeam(team, benchmark);
        List<Suggestion> out = new ArrayList<>();
        out.add(new Suggestion("Plano de treino", Q_PLAN));
        out.add(new Suggestion("Maior problema", Q_PROBLEM));
        focuses(k).stream().findFirst().ifPresent(f -> out.add(new Suggestion(f.label(),
                "Como melhorar em \"" + f.label() + "\"?")));
        out.add(new Suggestion("Rounds para rever", Q_VOD));
        out.add(new Suggestion("Onde perdemos", Q_LOSSES));
        out.add(new Suggestion("Pontos fortes", Q_STRENGTHS));
        out.add(new Suggestion("Mapas", Q_MAPS));
        out.add(new Suggestion("Quartil superior", Q_TOP25));
        return out;
    }

    // ------------------------------------------------------------------ resposta

    public Answer answer(String team, Benchmark benchmark, String question) {
        long start = System.nanoTime();
        String q = question == null ? "" : question.strip();
        if (q.isEmpty()) {
            throw new IllegalArgumentException("Escreva uma pergunta.");
        }
        if (q.length() > 300) {
            throw new IllegalArgumentException("Pergunta muito longa (máximo de 300 caracteres).");
        }
        TeamKnowledge k = knowledge.forTeam(team, benchmark);
        Parsed p = QueryParser.parse(q, knowledge.knownMaps());
        Draft d = switch (p.intent()) {
            case PLAN -> plan(k);
            case TOP_PROBLEM -> topProblem(k);
            case STRENGTHS -> strengths(k);
            case SITUATION -> situation(k, p.situationId(), p.map());
            case MAP -> map(k, p.map());
            case MAPS -> maps(k);
            case VOD -> vod(k, p.situationId(), p.map());
            case LOSSES -> losses(k);
            case METHOD -> method(k, p);
            case SEARCH -> search(k, p);
        };
        return d.toAnswer(q, d.intent != null ? d.intent : p.intent().name(), (System.nanoTime() - start) / 1e6);
    }

    private Draft plan(TeamKnowledge k) {
        Draft d = new Draft(k, "Plano de treino: " + k.displayName());
        List<PautaItem> focus = focuses(k);
        List<PautaItem> shown = focus;
        // quando os focos vêm do quartil superior, as demais seções (contra a liga) dizem a referência
        String leagueNote = "";
        if (focus.isEmpty()) {
            d.para("Nenhuma situação ficou abaixo " + ofReference(k) + " com evidência suficiente, então os dados "
                    + "não sustentam um foco de treino contra essa referência.");
            if (k.benchmark() == Benchmark.LEAGUE) {
                TeamKnowledge top = knowledge.forTeam(k.team(), Benchmark.TOP25);
                List<PautaItem> topFocus = focuses(top);
                if (topFocus.isEmpty()) {
                    d.para("Nem contra o quartil superior há ponto abaixo da referência com evidência. Use a fila de "
                            + "VOD dos maiores saldos negativos como pista.");
                } else {
                    d.para("Contra " + reference(top) + ", a régua mais alta, os dados sustentam estes focos:");
                    focusSections(top, d, topFocus);
                    shown = topFocus;
                    leagueNote = " (contra a média da liga)";
                }
            }
        } else {
            d.para("Plano montado com a análise de " + k.displayName() + " contra " + reference(k) + ". Os focos "
                    + "são as situações abaixo da referência com evidência, da evidência mais forte para a mais fraca e, "
                    + "depois, pelos rounds em jogo. O tempo de treino é dividido pelos rounds em jogo de cada foco.");
            focusSections(k, d, focus);
        }
        if (!k.report().strengths().isEmpty()) {
            PautaItem s = k.report().strengths().getFirst();
            d.section(new Section("Manter: " + s.label() + leagueNote, List.of(Fmt.comparison(s) + "; " + Fmt.stake(s) + "."),
                    null));
            d.source("item:" + s.id());
        }
        lossGap(k).ifPresent(g -> {
            d.section(new Section("Onde mais perdemos acima da liga", List.of(g.label() + ": " + Fmt.pct(g.share())
                    + " das derrotas do time, contra " + Fmt.pct(g.leagueShare()) + " na liga."), null));
            d.source("loss:" + g.id().name());
        });
        Set<String> chosen = shown.stream().map(PautaItem::situationId).collect(Collectors.toSet());
        List<String> left = overall(k).stream()
                .filter(i -> i.atStake() > NEUTRAL && !i.verdict().hasEvidence() && !chosen.contains(i.situationId()))
                .sorted(Comparator.comparingDouble(PautaItem::atStake).reversed())
                .limit(3)
                .map(i -> {
                    d.source("item:" + i.id());
                    return i.label() + ": " + Fmt.comparison(i) + ".";
                })
                .toList();
        if (!left.isEmpty()) {
            d.section(new Section("Fora do plano por falta de evidência" + leagueNote, left, null));
        }
        d.followUps(Q_VOD, Q_LOSSES, Q_MAPS);
        return d;
    }

    private void focusSections(TeamKnowledge k, Draft d, List<PautaItem> focus) {
        Set<String> maps = focus.stream().map(i -> String.valueOf(i.map())).collect(Collectors.toSet());
        if (focus.size() > 1 && maps.size() == 1 && focus.getFirst().map() != null) {
            d.para("Todos os focos aparecem em " + TeamNames.map(focus.getFirst().map()) + ": é o mapa que concentra "
                    + "as diferenças com evidência.");
        }
        int[] shares = shares(focus);
        for (int i = 0; i < focus.size(); i++) {
            d.section(focusSection(k, d, focus.get(i), "Foco " + (i + 1) + ": " + focus.get(i).label() + " · "
                    + shares[i] + "% do treino"));
        }
    }

    private Section focusSection(TeamKnowledge k, Draft d, PautaItem f, String title) {
        List<String> lines = new ArrayList<>();
        lines.add("O dado: " + Fmt.comparison(f) + "; " + Fmt.stake(f) + ".");
        d.source("item:" + f.id());
        if (f.map() == null) {
            worstMap(k, f.situationId()).ifPresent(m -> {
                lines.add("Onde pesa mais: " + TeamNames.map(m.map()) + ", com " + Fmt.comparison(m) + ".");
                d.source("item:" + m.id());
            });
        }
        String drill = RecommendationService.focusFor(f.situationId());
        if (drill != null) {
            lines.add("O que treinar: " + drill + ". (Sugestão de foco por situação, não uma conclusão dos dados.)");
            d.source("drill:" + f.situationId());
        }
        lines.add("Meta: chegar a " + Fmt.pct(f.benchmarkRate()) + ", cerca de " + Fmt.count(f.atStake(), Fmt.unit(f))
                + " a mais nas mesmas " + f.n() + " ocorrências.");
        List<RoundRef> refs = vodRounds(k, f.situationId(), f.map());
        // o mesmo round pode falhar em várias situações: cada foco sugere rounds ainda não citados
        List<RoundRef> fresh = refs.stream().filter(d::firstMention).limit(VOD_PREVIEW).toList();
        if (!fresh.isEmpty()) {
            lines.add("Rever primeiro: " + fresh.stream().map(Fmt::round).collect(Collectors.joining("; ")) + ".");
        }
        return new Section(title, lines, vodLink(f.situationId(), f.map(), refs.size()));
    }

    private Draft topProblem(TeamKnowledge k) {
        List<PautaItem> focus = focuses(k);
        if (focus.isEmpty()) {
            Draft d = new Draft(k, "Sem ponto de atenção com evidência");
            d.para(k.report().recommendation().headline());
            lossGap(k).ifPresent(g -> {
                d.para("Onde o time mais perde acima da liga: " + Fmt.lowerFirst(g.label()) + ", com "
                        + Fmt.pct(g.share()) + " das derrotas contra " + Fmt.pct(g.leagueShare()) + " na liga.");
                d.source("loss:" + g.id().name());
            });
            d.followUps(Q_STRENGTHS, Q_LOSSES, Q_TOP25);
            return d;
        }
        PautaItem f = focus.getFirst();
        Draft d = new Draft(k, "Maior problema: " + f.label());
        d.para(k.report().recommendation().headline());
        d.section(focusSection(k, d, f, "Como atacar o problema"));
        if (focus.size() > 1) {
            d.section(new Section("Depois dele", focus.subList(1, focus.size()).stream().map(i -> {
                d.source("item:" + i.id());
                return i.label() + ": " + Fmt.comparison(i) + "; " + Fmt.stake(i) + ".";
            }).toList(), null));
        }
        d.followUps(Q_PLAN, Q_VOD, Q_MAPS);
        return d;
    }

    private Draft strengths(TeamKnowledge k) {
        Draft d = new Draft(k, "Pontos fortes: " + k.displayName());
        List<PautaItem> strong = k.report().strengths();
        if (strong.isEmpty()) {
            d.para("Nenhuma situação ficou acima " + ofReference(k) + " com evidência suficiente.");
            List<String> best = overall(k).stream().filter(i -> i.atStake() < -NEUTRAL)
                    .sorted(Comparator.comparingDouble(PautaItem::atStake)).limit(3)
                    .map(i -> {
                        d.source("item:" + i.id());
                        return i.label() + ": " + Fmt.comparison(i) + ".";
                    }).toList();
            if (!best.isEmpty()) {
                d.section(new Section("Acima da referência, mas sem evidência", best, null));
            }
        } else {
            d.para("Situações em que " + k.displayName() + " fica acima " + ofReference(k) + " com evidência. "
                    + "Vale manter no treino o que já funciona.");
            d.section(new Section("Acima da referência", strong.stream().map(i -> {
                d.source("item:" + i.id());
                return i.label() + ": " + Fmt.comparison(i) + "; " + Fmt.stake(i) + ".";
            }).toList(), null));
        }
        d.followUps(Q_PROBLEM, Q_PLAN);
        return d;
    }

    private Draft situation(TeamKnowledge k, String situationId, String map) {
        Optional<PautaItem> found = item(k, situationId, map);
        String label = situationLabel(k, situationId);
        if (found.isEmpty()) {
            Draft d = new Draft(k, label);
            d.para(map == null
                    ? "A situação " + Fmt.lowerFirst(label) + " não ocorreu para " + k.displayName() + " na base."
                    : k.displayName() + " não teve ocorrências de " + Fmt.lowerFirst(label) + " em "
                    + TeamNames.map(map) + " na base.");
            d.followUps(Q_MAPS, Q_PLAN);
            return d;
        }
        PautaItem i = found.get();
        Draft d = new Draft(k, i.label());
        d.para(i.label() + ": " + Fmt.comparison(i) + "; " + Fmt.stake(i) + ".");
        d.source("item:" + i.id());
        d.para("Como medimos: " + i.definition());
        d.source("def:" + situationId);
        String drill = RecommendationService.focusFor(situationId);
        if (i.atStake() > NEUTRAL && drill != null) {
            d.para("O que treinar: " + drill + ". (Sugestão de foco por situação, não uma conclusão dos dados.)");
            d.source("drill:" + situationId);
        } else if (i.atStake() < -NEUTRAL) {
            d.para("Aqui o time está acima da referência: vale manter o que já funciona.");
        }
        if (map == null) {
            List<String> byMap = mapItems(k, situationId).stream()
                    .sorted(Comparator.comparingDouble(PautaItem::atStake).reversed())
                    .map(m -> {
                        d.source("item:" + m.id());
                        return TeamNames.map(m.map()) + ": " + Fmt.comparison(m) + ".";
                    }).toList();
            if (!byMap.isEmpty()) {
                d.section(new Section("Por mapa, do maior saldo negativo para o menor", byMap, null));
            }
        }
        d.action(vodLink(situationId, map, vodRounds(k, situationId, map).size()));
        d.followUps(Q_PLAN, Q_PROBLEM);
        return d;
    }

    private Draft map(TeamKnowledge k, String map) {
        String name = TeamNames.map(map);
        MapRecord rec = k.maps().get(map);
        if (rec == null) {
            Draft d = new Draft(k, "Mapa: " + name);
            d.para(k.displayName() + " não tem partidas em " + name + " na base. Mapas jogados: "
                    + k.maps().keySet().stream().map(TeamNames::map).collect(Collectors.joining(", ")) + ".");
            d.followUps(Q_MAPS);
            return d;
        }
        Draft d = new Draft(k, "Mapa: " + name);
        d.para(rec.maps() + (rec.maps() == 1 ? " mapa jogado" : " mapas jogados") + " em " + name + ", "
                + rec.mapsWon() + (rec.mapsWon() == 1 ? " vitória" : " vitórias") + ", " + Fmt.pct(rec.roundWinRate())
                + " dos rounds vencidos (" + rec.roundsWon() + " de " + rec.rounds() + ").");
        List<PautaItem> here = k.items().stream().filter(i -> map.equals(i.map())).toList();
        List<PautaItem> below = here.stream().filter(i -> i.atStake() > NEUTRAL)
                .sorted(Comparator.comparing((PautaItem i) -> i.verdict().ordinal())
                        .thenComparing(PautaItem::atStake, Comparator.reverseOrder()))
                .limit(3).toList();
        List<PautaItem> above = here.stream().filter(i -> i.atStake() < -NEUTRAL)
                .sorted(Comparator.comparingDouble(PautaItem::atStake)).limit(2).toList();
        if (!below.isEmpty()) {
            d.section(new Section("Abaixo da referência", lines(d, below), null));
            PautaItem top = below.getFirst();
            d.action(vodLink(top.situationId(), map, vodRounds(k, top.situationId(), map).size()));
        }
        if (!above.isEmpty()) {
            d.section(new Section("Acima da referência", lines(d, above), null));
        }
        if (!below.isEmpty() && below.stream().noneMatch(i -> i.verdict().hasEvidence())) {
            d.para("Nenhuma diferença abaixo da referência nesse mapa tem evidência: trate como pista para o VOD.");
        }
        long small = here.stream().filter(i -> i.verdict() == Verdict.SMALL_SAMPLE).count();
        if (small * 2 > here.size()) {
            d.para("Com poucas partidas nesse mapa, a maioria das situações tem amostra pequena: use como pista "
                    + "para o VOD, não como conclusão.");
        }
        d.followUps(Q_MAPS, Q_PLAN);
        return d;
    }

    private Draft maps(TeamKnowledge k) {
        Draft d = new Draft(k, "Desempenho por mapa");
        d.para("Do pior para o melhor, pela taxa de rounds vencidos. Ao lado, a situação com o maior saldo negativo "
                + "no mapa.");
        List<MapRecord> records = k.maps().values().stream()
                .sorted(Comparator.comparingDouble(MapRecord::roundWinRate)).toList();
        List<String> lines = new ArrayList<>();
        for (MapRecord r : records) {
            String line = TeamNames.map(r.map()) + ": " + r.maps() + (r.maps() == 1 ? " mapa" : " mapas") + ", "
                    + r.mapsWon() + (r.mapsWon() == 1 ? " vitória" : " vitórias") + ", "
                    + Fmt.pct(r.roundWinRate()) + " dos rounds";
            Optional<PautaItem> worst = k.items().stream()
                    .filter(i -> r.map().equals(i.map()) && i.atStake() > NEUTRAL)
                    .max(Comparator.comparingDouble(PautaItem::atStake));
            if (worst.isPresent()) {
                PautaItem w = worst.get();
                d.source("item:" + w.id());
                line += ". Maior saldo negativo: " + Fmt.lowerFirst(situationLabel(k, w.situationId()))
                        + " (" + Fmt.stake(w) + "; " + Fmt.verdict(w.verdict()) + ")";
            }
            lines.add(line + ".");
        }
        d.section(new Section("Mapas", lines, null));
        if (!records.isEmpty()) {
            d.followUps("Como estamos na " + TeamNames.map(records.getFirst().map()) + "?", Q_PLAN);
        }
        return d;
    }

    private Draft vod(TeamKnowledge k, String situationId, String map) {
        PautaItem target = situationId != null
                ? item(k, situationId, map).orElse(null)
                : focuses(k).stream().findFirst()
                .or(() -> overall(k).stream().max(Comparator.comparingDouble(PautaItem::atStake)))
                .orElse(null);
        if (target == null) {
            Draft d = new Draft(k, "Rounds para rever");
            d.para("Não encontrei rounds dessa situação para " + k.displayName() + ".");
            d.followUps(Q_PLAN);
            return d;
        }
        List<RoundRef> refs = vodRounds(k, target.situationId(), target.map());
        Draft d = new Draft(k, "Rounds para rever: " + target.label());
        d.source("item:" + target.id());
        d.source("method:vod");
        if (refs.isEmpty()) {
            d.para("Não há rounds sem sucesso em " + Fmt.lowerFirst(target.label()) + ".");
        } else {
            d.para((situationId == null ? "Começando pelo principal ponto de atenção. " : "") + refs.size()
                    + " rounds em que o time não teve sucesso em " + Fmt.lowerFirst(target.label()) + " ("
                    + Fmt.comparison(target) + "). Os primeiros da fila:");
            d.section(new Section("Primeiros da fila", refs.stream().limit(5).map(r -> Fmt.round(r) + ".").toList(),
                    null));
            d.action(vodLink(target.situationId(), target.map(), refs.size()));
        }
        d.followUps(Q_PLAN, Q_PROBLEM);
        return d;
    }

    private Draft losses(TeamKnowledge k) {
        var b = k.losses();
        Draft d = new Draft(k, "Onde perdemos rounds");
        d.para(k.displayName() + " perdeu " + b.roundsLost() + " rounds na base. Cada derrota entra em uma categoria "
                + "só; ao lado, a fatia da mesma categoria na liga.");
        d.source("method:losses");
        List<Slice> slices = b.categories().stream()
                .sorted(Comparator.comparingDouble((Slice s) -> s.share() - s.leagueShare()).reversed()).toList();
        d.section(new Section("Categorias, da maior diferença para a liga à menor", slices.stream().map(s -> {
            d.source("loss:" + s.id().name());
            return s.label() + ": " + Fmt.count(s.rounds(), "rounds") + ", " + Fmt.pct(s.share()) + " das derrotas (liga: "
                    + Fmt.pct(s.leagueShare()) + ").";
        }).toList(), null));
        lossGap(k).ifPresentOrElse(g -> {
            String situationId = g.id().situationId();
            d.para("A maior diferença para a liga está em " + Fmt.lowerFirst(g.label()) + ".");
            item(k, situationId, null).ifPresent(i ->
                    d.action(vodLink(situationId, null, vodRounds(k, situationId, null).size())));
        }, () -> d.para("Nenhuma categoria passa a fatia da liga em 2 pontos percentuais ou mais: as derrotas do "
                + "time se distribuem como as da liga."));
        d.followUps(Q_PLAN, Q_PROBLEM);
        return d;
    }

    private Draft method(TeamKnowledge k, Parsed p) {
        Set<KnowledgeDoc.Kind> kinds = EnumSet.of(KnowledgeDoc.Kind.METHOD, KnowledgeDoc.Kind.DEFINITION);
        List<Hit> hits = k.index().search(p.question(), 3, doc -> kinds.contains(doc.kind()));
        Optional<KnowledgeDoc> first = p.situationId() != null
                ? k.index().doc("def:" + p.situationId())
                : hits.stream().findFirst().map(Hit::doc);
        if (first.isEmpty()) {
            return unknown(k);
        }
        Draft d = new Draft(k, first.get().title());
        d.para(first.get().text());
        d.hit(hits, first.get());
        hits.stream().filter(h -> !h.doc().id().equals(first.get().id()))
                .filter(h -> !hits.isEmpty() && h.score() >= 0.6 * hits.getFirst().score())
                .limit(1)
                .forEach(h -> {
                    d.section(new Section(h.doc().title(), List.of(h.doc().text()), null));
                    d.hit(hits, h.doc());
                });
        d.followUps(Q_PLAN, Q_PROBLEM);
        return d;
    }

    private Draft search(TeamKnowledge k, Parsed p) {
        List<Hit> hits = k.index().search(p.question(), 4, doc -> true);
        if (hits.isEmpty() || hits.getFirst().score() < MIN_SEARCH_SCORE) {
            return unknown(k);
        }
        Draft d = new Draft(k, "O que encontrei na análise");
        d.para("Busquei a pergunta nos dados de " + k.displayName() + ". Estes são os trechos mais próximos:");
        double top = hits.getFirst().score();
        for (Hit h : hits.stream().filter(h -> h.score() >= 0.5 * top).limit(3).toList()) {
            KnowledgeDoc doc = h.doc();
            VodLink link = null;
            if (doc.situationId() != null && (doc.kind() == KnowledgeDoc.Kind.SITUATION
                    || doc.kind() == KnowledgeDoc.Kind.SITUATION_MAP)) {
                link = vodLink(doc.situationId(), doc.map(),
                        vodRounds(k, doc.situationId(), doc.map()).size());
            }
            d.section(new Section(doc.title(), List.of(doc.text()), link));
            d.hit(hits, doc);
        }
        d.followUps(Q_PLAN, Q_PROBLEM);
        return d;
    }

    private Draft unknown(TeamKnowledge k) {
        Draft d = new Draft(k, "Não encontrei isso nos dados");
        d.intent = "UNKNOWN";
        d.para("Eu respondo só com o que está na análise de " + k.displayName() + ": situações de round (pós-plant, "
                + "retake, first blood, pistol, eco, clutch...), mapas, onde os rounds foram perdidos, fila de VOD e "
                + "como calculamos. Tente uma destas perguntas:");
        d.followUps(Q_PLAN, Q_PROBLEM, Q_MAPS, Q_TOP25);
        return d;
    }

    // ------------------------------------------------------------------ apoio

    /** Mesma ordem dos pontos de atenção do relatório: evidência mais forte primeiro, depois rounds em jogo. */
    private static final Comparator<PautaItem> BY_EVIDENCE_THEN_STAKE = Comparator
            .comparing((PautaItem i) -> i.verdict().ordinal())
            .thenComparing(PautaItem::atStake, Comparator.reverseOrder());

    /**
     * Focos do treino: itens abaixo da referência com evidência, um por situação (o primeiro na ordem do
     * relatório). Usa todos os itens, não só os 5 destaques do relatório, para não perder situações.
     */
    static List<PautaItem> focuses(TeamKnowledge k) {
        Map<String, PautaItem> bySituation = new LinkedHashMap<>();
        k.items().stream()
                .filter(i -> i.verdict().hasEvidence() && i.atStake() > 0)
                .sorted(BY_EVIDENCE_THEN_STAKE)
                .forEach(i -> bySituation.putIfAbsent(i.situationId(), i));
        return bySituation.values().stream().limit(MAX_FOCUS).toList();
    }

    /** Divide 100% do treino em múltiplos de 5, proporcional aos rounds em jogo (mínimo de 10% por foco). */
    static int[] shares(List<PautaItem> focus) {
        double total = focus.stream().mapToDouble(PautaItem::atStake).sum();
        int[] shares = new int[focus.size()];
        int sum = 0;
        for (int i = 0; i < shares.length; i++) {
            shares[i] = Math.max(10, (int) Math.round(focus.get(i).atStake() / total * 20) * 5);
            sum += shares[i];
        }
        // o ajuste do arredondamento vai para a maior fatia: os focos seguem a ordem da evidência, não do
        // saldo, então o primeiro pode ser o menor e ficaria com 0% ou menos
        int largest = 0;
        for (int i = 1; i < shares.length; i++) {
            if (shares[i] > shares[largest]) largest = i;
        }
        shares[largest] += 100 - sum;
        return shares;
    }

    /** Nome da situação sem o mapa ("Pós-plant (ataque)"), do documento de definição. */
    private static String situationLabel(TeamKnowledge k, String situationId) {
        return k.index().doc("def:" + situationId).map(KnowledgeDoc::title).orElse(situationId);
    }

    private static List<PautaItem> overall(TeamKnowledge k) {
        return k.items().stream().filter(i -> i.map() == null).toList();
    }

    private static List<PautaItem> mapItems(TeamKnowledge k, String situationId) {
        return k.items().stream().filter(i -> i.map() != null && i.situationId().equals(situationId)).toList();
    }

    private static Optional<PautaItem> item(TeamKnowledge k, String situationId, String map) {
        return k.items().stream()
                .filter(i -> i.situationId().equals(situationId) && Objects.equals(i.map(), map))
                .findFirst();
    }

    private static Optional<PautaItem> worstMap(TeamKnowledge k, String situationId) {
        return mapItems(k, situationId).stream().filter(i -> i.atStake() > NEUTRAL)
                .max(Comparator.comparingDouble(PautaItem::atStake));
    }

    /** Categoria de derrota em que o time mais passa da fatia da liga (só se passar). */
    private static Optional<Slice> lossGap(TeamKnowledge k) {
        return k.losses().categories().stream().filter(s -> s.share() - s.leagueShare() >= MIN_LOSS_GAP)
                .max(Comparator.comparingDouble(s -> s.share() - s.leagueShare()));
    }

    /** "a média da liga (67 times)" ou "o quartil superior (9 times)". */
    private static String reference(TeamKnowledge k) {
        String name = k.benchmark() == Benchmark.TOP25 ? "o quartil superior" : "a média da liga";
        return name + " (" + k.report().benchmarkTeams() + " times)";
    }

    /** "da média da liga (67 times)" ou "do quartil superior (9 times)". */
    private static String ofReference(TeamKnowledge k) {
        return "d" + reference(k);
    }

    private List<RoundRef> vodRounds(TeamKnowledge k, String situationId, String map) {
        return opportunities.vodQueue(k.team(), situationId, map);
    }

    private static VodLink vodLink(String situationId, String map, int rounds) {
        return rounds == 0 ? null
                : new VodLink("Abrir fila de VOD (" + Fmt.count(rounds, "rounds") + ")", situationId, map, rounds);
    }

    private static List<String> lines(Draft d, List<PautaItem> items) {
        return items.stream().map(i -> {
            d.source("item:" + i.id());
            return i.label() + ": " + Fmt.comparison(i) + "; " + Fmt.stake(i) + ".";
        }).toList();
    }

    /** Resposta em construção: frases, seções, ações e as fontes usadas. */
    private static final class Draft {
        private final TeamKnowledge k;
        private final String title;
        private final List<String> paragraphs = new ArrayList<>();
        private final List<Section> sections = new ArrayList<>();
        private final List<VodLink> actions = new ArrayList<>();
        private final Map<String, Source> sources = new LinkedHashMap<>();
        private final List<String> followUps = new ArrayList<>();
        private final Set<String> mentionedRounds = new HashSet<>();
        private String intent;

        Draft(TeamKnowledge k, String title) {
            this.k = k;
            this.title = title;
        }

        void para(String text) {
            paragraphs.add(text);
        }

        void section(Section s) {
            sections.add(s);
        }

        void action(VodLink link) {
            if (link != null) {
                actions.add(link);
            }
        }

        void source(String id) {
            k.index().doc(id).ifPresent(doc ->
                    sources.putIfAbsent(id, new Source(id, doc.kind().name(), doc.title(), null)));
        }

        void hit(List<Hit> hits, KnowledgeDoc doc) {
            Double score = hits.stream().filter(h -> h.doc().id().equals(doc.id())).map(Hit::score).findFirst()
                    .map(s -> Math.round(s * 100) / 100.0).orElse(null);
            sources.putIfAbsent(doc.id(), new Source(doc.id(), doc.kind().name(), doc.title(), score));
        }

        boolean firstMention(RoundRef r) {
            return mentionedRounds.add(r.fileId() + "#" + r.round());
        }

        void followUps(String... questions) {
            followUps.addAll(List.of(questions));
        }

        Answer toAnswer(String question, String intent, double tookMs) {
            return new Answer(question, intent, title, List.copyOf(paragraphs), List.copyOf(sections),
                    List.copyOf(actions), List.copyOf(sources.values()), List.copyOf(followUps),
                    Math.round(tookMs * 100) / 100.0);
        }
    }
}
