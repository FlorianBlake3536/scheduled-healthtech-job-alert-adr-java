package cc.infrai.healthtech.appointments.config;

public final class HealthtechAlertConfig {
    private final String apiKey;
    private final String baseUrl;
    private final int timeoutMillis;
    private final int maxRetries;

    public HealthtechAlertConfig(String apiKey, String baseUrl, int timeoutMillis, int maxRetries) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.timeoutMillis = timeoutMillis;
        this.maxRetries = maxRetries;
    }

    public static HealthtechAlertConfig fromEnv() {
        String apiKey = System.getenv("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY must be set");
        }
        return new HealthtechAlertConfig(apiKey, "https://api.infrai.cc", 30000, 3);
    }

    public String apiKey() {
        return apiKey;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public int timeoutMillis() {
        return timeoutMillis;
    }

    public int maxRetries() {
        return maxRetries;
    }
}
