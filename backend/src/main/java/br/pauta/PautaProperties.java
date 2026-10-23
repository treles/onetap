package br.pauta;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracoes do projeto (prefixo "pauta" no application.properties). */
@ConfigurationProperties(prefix = "pauta")
public record PautaProperties(String dataDir) {
}
