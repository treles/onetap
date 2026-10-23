package br.pauta.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP. Funciona com ou sem a base em ../data (sem base, a lista de times só fica vazia). */
@SpringBootTest
@AutoConfigureMockMvc
class ApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void healthDaBaseResponde() throws Exception {
        mvc.perform(get("/api/health/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filesRejected").value(0));
    }

    @Test
    void timeInexistenteDa404ComContratoDeErro() throws Exception {
        mvc.perform(get("/api/teams/time_que_nao_existe/report"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void listaDeTimesResponde() throws Exception {
        mvc.perform(get("/api/teams")).andExpect(status().isOk());
    }

    @Test
    void metodologiaListaAsDozeSituacoes() throws Exception {
        mvc.perform(get("/api/methodology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situations.length()").value(12))
                .andExpect(jsonPath("$.minSample").value(20));
    }
}
