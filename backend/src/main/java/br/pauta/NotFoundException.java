package br.pauta;

/** Recurso inexistente (time, situacao...). Vira HTTP 404 no ApiErrorHandler. */
public class NotFoundException extends RuntimeException {

    private final String code;

    public NotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
