package br.pauta.api;

import br.pauta.assistant.AssistantService;
import br.pauta.insight.Benchmark;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Assistente do treinador: respostas montadas só com os dados da análise do time (sem texto gerado). */
@RestController
@RequestMapping("/api/teams/{team}/assistant")
public class AssistantController {

    private final AssistantService assistant;

    public AssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    public record Question(String question, String benchmark) {
    }

    /** Perguntas sugeridas para o time, montadas a partir do relatório. */
    @GetMapping("/suggestions")
    public List<AssistantService.Suggestion> suggestions(@PathVariable String team,
                                                         @RequestParam(defaultValue = "league") String benchmark) {
        return assistant.suggestions(team, Benchmark.parse(benchmark));
    }

    @PostMapping
    public AssistantService.Answer ask(@PathVariable String team, @RequestBody Question body) {
        String benchmark = body.benchmark() == null || body.benchmark().isBlank() ? "league" : body.benchmark();
        return assistant.answer(team, Benchmark.parse(benchmark), body.question());
    }
}
