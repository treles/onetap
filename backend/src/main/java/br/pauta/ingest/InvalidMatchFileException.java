package br.pauta.ingest;

/** Arquivo que nao pode ser transformado em partida (nome fora do padrao, lados inconsistentes etc.). */
public class InvalidMatchFileException extends RuntimeException {

    public InvalidMatchFileException(String message) {
        super(message);
    }
}
