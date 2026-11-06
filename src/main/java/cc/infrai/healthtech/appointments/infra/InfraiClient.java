package cc.infrai.healthtech.appointments.infra;

import cc.infrai.healthtech.appointments.config.HealthtechAlertConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class InfraiClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HealthtechAlertConfig config;
    private final HttpClient httpClient;

    public final Errors errors;

    public InfraiClient(HealthtechAlertConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.timeoutMillis()))
                .build();
        this.errors = new Errors(this);
    }

    public Map<String, Object> post(String path, Map<String, Object> payload) {
        int attempt = 0;
        long backoffMillis = 300L;

        while (true) {
            attempt += 1;
            HttpResponse<String> response = send("POST", path, payload);
            Map<String, Object> envelope = parseEnvelope(response.body());

            Object okValue = envelope.get("ok");
            boolean ok = okValue instanceof Boolean && (Boolean) okValue;
            if (!ok) {
                Map<String, Object> error = asMap(envelope.get("error"));
                String code = String.valueOf(error.getOrDefault("code", "UNKNOWN_ERROR"));
                String message = String.valueOf(error.getOrDefault("message", "Request failed"));
                if (response.statusCode() == 429 && attempt < config.maxRetries()) {
                    sleep(retryAfterMillis(response, backoffMillis));
                    backoffMillis *= 2;
                    continue;
                }
                throw new InfraiError(code, message, response.statusCode());
            }

            if (response.statusCode() >= 500) {
                throw new IllegalStateException("Unexpected server response");
            }

            return asMap(envelope.get("data"));
        }
    }

    private HttpResponse<String> send(String method, String path, Map<String, Object> payload) {
        try {
            String body = MAPPER.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + path))
                    .timeout(Duration.ofMillis(config.timeoutMillis()))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException("HTTP request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP request interrupted", e);
        }
    }

    private static Map<String, Object> parseEnvelope(String body) {
        try {
            return MAPPER.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Invalid JSON envelope", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> raw) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return copy;
        }
        return new LinkedHashMap<>();
    }

    private static long retryAfterMillis(HttpResponse<String> response, long defaultMillis) {
        return response.headers()
                .firstValue("Retry-After")
                .map(value -> {
                    try {
                        return Long.parseLong(value) * 1000L;
                    } catch (NumberFormatException ex) {
                        return defaultMillis;
                    }
                })
                .orElse(defaultMillis);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Backoff interrupted", e);
        }
    }

    public static final class Errors {
        private final InfraiClient client;

        public Errors(InfraiClient client) {
            this.client = client;
        }

        public Map<String, Object> capture(Map<String, Object> payload) {
            return client.post("/v1/errors/capture", payload);
        }
    }
}
