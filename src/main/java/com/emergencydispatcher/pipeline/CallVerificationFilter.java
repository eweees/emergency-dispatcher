package com.emergencydispatcher.pipeline;

import com.emergencydispatcher.model.IncidentReport;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Фильтр 2 — Верификация звонящего через внешний HTTP-сервер.
 *
 * <p>Отправляет GET-запрос на {@link com.emergencydispatcher.server.CallVerificationServer}
 * (порт 8282) с номером телефона и адресом.
 * Сервер проверяет корректность данных по заданному правилу
 * и возвращает JSON-ответ с полем {@code "verified": true/false}.
 *
 * <p>Если сервер недоступен, фильтр выбрасывает {@link FilterException}
 * с инструкцией запустить сервер.
 *
 * <p>Пример запроса:
 * <pre>
 *   GET http://localhost:8282/verify?phone=89001234567&amp;address=ул.Ленина,1
 *   → {"phone":"89001234567","address":"...","verified":true,"message":"Данные подтверждены"}
 * </pre>
 */
public class CallVerificationFilter implements Filter<IncidentReport> {

    /** URL сервера верификации звонков */
    private static final String SERVER_URL = "http://localhost:8282/verify";

    /** Таймаут HTTP-запроса */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    /** HTTP-клиент (переиспользуется) */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    @Override
    public void process(IncidentReport data) throws FilterException {
        // В SOS-режиме телефон необязателен — пропускаем верификацию
        if (data.getPhoneNumber() == null || data.getPhoneNumber().isBlank()) {
            System.out.println("[CallVerificationFilter] Телефон не указан — верификация пропущена (SOS-режим).");
            return;
        }
        System.out.println("[CallVerificationFilter] Верификация телефона: " + data.getPhoneNumber());

        String phone = URLEncoder.encode(
                data.getPhoneNumber().replaceAll("[\\s\\-()]", ""),
                StandardCharsets.UTF_8
        );
        String address = URLEncoder.encode(data.getAddress(), StandardCharsets.UTF_8);

        String url = SERVER_URL + "?phone=" + phone + "&address=" + address;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            String body = response.body();
            System.out.println("[CallVerificationFilter] Ответ сервера: " + body);

            if (response.statusCode() != 200) {
                throw new FilterException(
                        "Сервер верификации вернул ошибку: HTTP " + response.statusCode()
                );
            }

            // Разбираем JSON без библиотеки (простой grep по полю "verified")
            boolean verified = body.contains("\"verified\":true");

            if (!verified) {
                // Извлекаем сообщение об ошибке из JSON
                String message = extractJsonField(body, "message");
                throw new FilterException(
                        "Верификация не пройдена: " + message +
                        "\n\nПроверьте номер телефона и адрес."
                );
            }

            data.setPhoneVerified(true);
            System.out.println("[CallVerificationFilter] Верификация пройдена успешно.");

        } catch (FilterException fe) {
            throw fe; // перебрасываем FilterException как есть
        } catch (java.net.ConnectException ce) {
            throw new FilterException(
                    "Не удалось подключиться к серверу верификации.\n\n" +
                    "⚠ Запустите CallVerificationServer (порт 8282) перед отправкой формы.",
                    ce
            );
        } catch (Exception e) {
            throw new FilterException(
                    "Ошибка при обращении к серверу верификации: " + e.getMessage(),
                    e
            );
        }
    }

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    /**
     * Извлекает значение поля из простого JSON-ответа.
     *
     * <p>Пример: {@code extractJsonField("{\"message\":\"ok\"}", "message")} → {@code "ok"}
     *
     * @param json      строка JSON
     * @param fieldName имя поля
     * @return значение поля или «неизвестно», если поле не найдено
     */
    private String extractJsonField(String json, String fieldName) {
        String key = "\"" + fieldName + "\":\"";
        int start = json.indexOf(key);
        if (start == -1) return "неизвестно";
        start += key.length();
        int end = json.indexOf('"', start);
        if (end == -1) return "неизвестно";
        return json.substring(start, end);
    }
}
