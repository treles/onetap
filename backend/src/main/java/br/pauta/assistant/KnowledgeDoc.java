package br.pauta.assistant;

/**
 * Um trecho da base de conhecimento do assistente. Todo texto é montado a partir da análise (números do
 * relatório, das perdas e da metodologia); nada é escrito à mão por time.
 *
 * @param keywords termos extras só para a busca (sinônimos da situação, nome do mapa); não aparecem na resposta
 */
public record KnowledgeDoc(String id, Kind kind, String title, String text, String situationId, String map,
                           String keywords) {

    public enum Kind {
        /** Situação no geral (relatório do time). */
        SITUATION,
        /** Situação num mapa específico. */
        SITUATION_MAP,
        /** Categoria de "onde perdemos". */
        LOSS,
        /** Definição de uma situação. */
        DEFINITION,
        /** Sugestão de foco de treino por situação. */
        DRILL,
        /** Como o produto calcula (referências, vereditos, saldo...). */
        METHOD
    }
}
