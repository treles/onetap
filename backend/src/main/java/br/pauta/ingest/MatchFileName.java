package br.pauta.ingest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metadados extraidos do nome do arquivo - a UNICA fonte do nome dos times.
 * Exemplo: {@code UNIVAVA_C1_036_27-06-26_macklogic_red_13_x_5_octacore_javascript_lotus.json}
 * Padrao validado nos 336 arquivos da base sem nenhuma falha.
 */
public record MatchFileName(String fileId, String tournament, String number, String date,
                            String teamA, int scoreA, int scoreB, String teamB, String map) {

    // O prefixo e "preguicoso" (.+?) para parar no primeiro _numero_data_ (ex.: RUSH_INCLUSIVO_003_...).
    private static final Pattern PATTERN = Pattern.compile(
            "^(?<prefix>.+?)_(?<num>\\d+[a-z]?)_(?<date>\\d\\d-\\d\\d-\\d\\d)_(?<a>.+)_(?<sa>\\d+)_x_(?<sb>\\d+)_(?<b>.+)_(?<map>[a-z]+)$");

    public static MatchFileName parse(String fileName) {
        String base = fileName.replaceFirst("\\.json$", "");
        Matcher m = PATTERN.matcher(base);
        if (!m.matches()) {
            throw new InvalidMatchFileException("Nome de arquivo fora do padrao: " + fileName);
        }
        int scoreA = Integer.parseInt(m.group("sa"));
        int scoreB = Integer.parseInt(m.group("sb"));
        if (scoreA == scoreB) {
            throw new InvalidMatchFileException("Placar empatado no nome do arquivo: " + fileName);
        }
        return new MatchFileName(base, m.group("prefix"), m.group("num"), m.group("date"),
                m.group("a"), scoreA, scoreB, m.group("b"), m.group("map"));
    }
}
