package br.pauta.assistant;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Entendimento da pergunta: intenção, situação e mapa, sem modelo generativo. */
class QueryParserTest {

    private static final Set<String> MAPS = Set.of("ascent", "lotus", "split", "haven", "abyss");

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "-", textBlock = """
            Monte um plano de treino para a semana     | PLAN        | -                      | -
            Qual é o nosso maior problema?             | TOP_PROBLEM | -                      | -
            Como melhorar o pós-plant na Ascent?       | SITUATION   | post-plant             | ascent
            como a gente joga contra eco               | SITUATION   | anti-eco               | -
            rounds de eco e force                      | SITUATION   | low-buy-vs-full        | -
            estamos tradando bem?                      | SITUATION   | first-death-traded     | -
            o que fazer no retake da split             | SITUATION   | retake                 | split
            taxa de plant no ataque                    | SITUATION   | attack-plant-rate      | -
            quando sofremos o first blood              | SITUATION   | first-blood-reaction   | -
            converter o first blood                    | SITUATION   | first-blood-conversion | -
            sofrer o first blood                       | SITUATION   | first-blood-reaction   | -
            Quais rounds devemos rever primeiro?       | VOD         | -                      | -
            mostrar o vod do clutch na lotus           | VOD         | clutch                 | lotus
            Onde perdemos mais rounds?                 | LOSSES      | -                      | -
            Quais são nossos pontos fortes?            | STRENGTHS   | -                      | -
            Como estamos em cada mapa?                 | MAPS        | -                      | -
            como estamos na Haven                      | MAP         | -                      | haven
            O que é o quartil superior?                | METHOD      | -                      | -
            o que é first blood?                       | METHOD      | first-blood-conversion | -
            receita de bolo de cenoura                 | SEARCH      | -                      | -
            como fomos na semifinal                    | SEARCH      | -                      | -
            quem é o assistente?                       | SEARCH      | -                      | -
            vamos assistir os rounds de pistol         | VOD         | pistol                 | -
            qual o plano tradicional de vocês          | PLAN        | -                      | -
            """)
    void entendeAPergunta(String question, QueryParser.Intent intent, String situation, String map) {
        var parsed = QueryParser.parse(question, MAPS);
        assertEquals(intent, parsed.intent(), "intenção de: " + question);
        assertEquals(situation, parsed.situationId(), "situação de: " + question);
        assertEquals(map, parsed.map(), "mapa de: " + question);
    }
}
