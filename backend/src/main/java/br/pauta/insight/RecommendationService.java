package br.pauta.insight;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Transforma o item mais relevante da Pauta numa frase de coaching, por template.
 * Nada é inventado: todo número da frase vem do item; a sugestão de foco é um texto fixo por situação.
 */
@Service
public class RecommendationService {

    /** Sugestão de foco de treino por situação (heurística de coaching, não conclusão dos dados). */
    static final Map<String, String> FOCUS = Map.ofEntries(
            Map.entry("attack", "execuções de ataque: entrada no site com utilitária e timing combinado"),
            Map.entry("attack-plant-rate", "chegar ao plant: execuções com utilitária e menos duelos isolados antes da entrada"),
            Map.entry("post-plant", "pós-plant: posicionamento depois do plant e utilitária guardada para negar o defuse"),
            Map.entry("defense", "setups de defesa e rotações"),
            Map.entry("retake", "retakes coordenados: esperar o grupo e usar utilitária antes de entrar"),
            Map.entry("first-blood-conversion", "converter a vantagem numérica: jogar o 5 contra 4 sem forçar duelos"),
            Map.entry("first-blood-reaction", "reagir ao primeiro abate sofrido: reagrupar e recuperar o controle do round"),
            Map.entry("first-death-traded", "jogar em dupla na entrada para garantir o trade do primeiro abatido"),
            Map.entry("pistol", "estratégias de pistol (compra e plano para os rounds 1 e 13)"),
            Map.entry("anti-eco", "disciplina contra eco: não dar duelos isolados nem curtos contra armas baratas"),
            Map.entry("low-buy-vs-full", "rounds de força e eco: empilhar um site e buscar abates de perto"),
            Map.entry("clutch", "situações de 1 contra vários: uso do tempo e isolamento de duelos"));

    /** Sugestão de foco da situação, ou {@code null} se não houver. */
    public static String focusFor(String situationId) {
        return FOCUS.get(situationId);
    }

    public record Recommendation(String headline, String focus, String strength, String itemId, int vodRounds) {
    }

    public Recommendation build(List<PautaItem> attention, List<PautaItem> strengths, ToIntFunction<PautaItem> vodCount) {
        String strength = strengths.isEmpty() ? null : String.format(Locale.ROOT,
                "Ponto forte: %s (%s contra %s da referência).", strengths.getFirst().label(),
                pct(strengths.getFirst().teamRate()), pct(strengths.getFirst().benchmarkRate()));

        if (attention.isEmpty()) {
            return new Recommendation(
                    "Nenhuma situação ficou abaixo da referência com evidência suficiente. "
                            + "Compare com o quartil superior ou veja onde os rounds foram perdidos.",
                    null, strength, null, 0);
        }
        PautaItem top = attention.getFirst();
        String evidence = top.verdict() == Verdict.STRONG ? "evidência forte" : "indício";
        String headline = String.format(Locale.ROOT,
                "Principal ponto de atenção: %s. O time teve %s contra %s da referência em %d ocorrências "
                        + "(cerca de %.0f %s em jogo; %s).",
                top.label(), pct(top.teamRate()), pct(top.benchmarkRate()), top.n(), top.atStake(), top.unit(), evidence);
        return new Recommendation(headline, FOCUS.get(top.situationId()), strength, top.id(), vodCount.applyAsInt(top));
    }

    private static String pct(double v) {
        return String.format(Locale.ROOT, "%.0f%%", v * 100);
    }
}
