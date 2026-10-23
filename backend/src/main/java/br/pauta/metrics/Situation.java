package br.pauta.metrics;

import br.pauta.domain.Match;
import br.pauta.domain.Round;
import br.pauta.domain.Side;

/**
 * Uma situacao de jogo medida como taxa de sucesso (sucessos / ocorrencias).
 *
 * <p>Para criar uma nova situacao: implemente esta interface, anote com {@code @Component} e escreva
 * um teste com um round montado a mao. O {@link SituationEngine} descobre a classe sozinho.
 */
public interface Situation {

    /** Identificador estavel usado na URL, ex.: "first-blood-conversion". */
    String id();

    /** Nome exibido na tela, ex.: "Converter o first blood". */
    String label();

    /** Definicao exata exibida ao usuario (transparencia: o treinador precisa saber o que foi contado). */
    String definition();

    /** Unidade do que esta em jogo no ranking: normalmente "rounds"; use outra quando o sucesso nao for vencer. */
    default String unit() {
        return "rounds";
    }

    /**
     * Chamado para cada round e cada lado. Se a situacao ocorreu para {@code side}, registre em {@code acc}
     * se foi um sucesso. Se nao ocorreu, simplesmente nao chame {@code acc.add}.
     */
    void evaluate(Match match, Round round, Side side, Recorder acc);

    @FunctionalInterface
    interface Recorder {
        void add(boolean success);
    }
}
