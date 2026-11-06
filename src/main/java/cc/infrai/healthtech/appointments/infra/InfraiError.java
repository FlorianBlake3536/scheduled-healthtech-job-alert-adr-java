package cc.infrai.healthtech.appointments.infra;

public final class InfraiError extends RuntimeException {
    private final int statusCode;
    private final String code;

    public InfraiError(String code, String message, int statusCode) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
    }

    public String code() {
        return code;
    }

    public int statusCode() {
        return statusCode;
    }
}
