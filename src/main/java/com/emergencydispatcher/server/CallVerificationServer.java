package com.emergencydispatcher.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP-сервер верификации звонков — аналог {@code InsuranceServer} из оригинала.
 *
 * <p>Имитирует работу базы данных звонящих:
 * проверяет номер телефона и корректность адреса.
 *
 * <h3>Правило верификации:</h3>
 * <p>Телефон считается верифицированным, если:
 * <ul>
 *   <li>Номер содержит 11 цифр (после нормализации)</li>
 *   <li>Номер начинается с «8» или «+7»</li>
 *   <li>Адрес не пустой и содержит минимум одну цифру (номер дома)</li>
 * </ul>
 *
 * <h3>Эндпоинт:</h3>
 * <pre>
 *   GET http://localhost:8282/verify?phone=89001234567&amp;address=ул.Ленина,5
 *   → {"phone":"89001234567","address":"ул.Ленина,5","verified":true,"message":"Данные подтверждены"}
 *
 *   GET http://localhost:8282/verify?phone=12345&amp;address=ул.Ленина,5
 *   → {"phone":"12345","address":"ул.Ленина,5","verified":false,"message":"Некорректный номер телефона"}
 * </pre>
 *
 * <p>⚠ Запускается <b>отдельно</b>, до старта основного JavaFX-приложения.
 */
public class CallVerificationServer {

    /** Порт сервера верификации */
    private static final int PORT = 8282;

    /** Путь эндпоинта верификации */
    private static final String ENDPOINT = "/verify";

    /**
     * Точка входа для отдельного запуска сервера верификации.
     *
     * @param args аргументы командной строки (не используются)
     * @throws IOException если не удалось запустить сервер
     */
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Обработчик верификации
        server.createContext(ENDPOINT, exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, buildJson("", "", false, "Метод не поддерживается"));
                return;
            }

            // Разбираем параметры запроса
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());
            String phone   = params.getOrDefault("phone", "").trim();
            String address = params.getOrDefault("address", "").trim();

            System.out.printf("[CallVerificationServer] Запрос: phone=%s, address=%s%n",
                    phone, address);

            // Верификация
            VerificationResult result = verify(phone, address);
            String json = buildJson(phone, address, result.verified(), result.message());

            System.out.printf("[CallVerificationServer] Ответ: verified=%b, msg=%s%n",
                    result.verified(), result.message());

            sendResponse(exchange, 200, json);
        });

        // Обработчик состояния сервера
        server.createContext("/status", exchange -> {
            String json = "{\"status\":\"OK\",\"port\":" + PORT + "}";
            sendResponse(exchange, 200, json);
        });

        server.setExecutor(null); // стандартный пул потоков
        server.start();

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║       CallVerificationServer запущен             ║");
        System.out.println("║  Порт: " + PORT + "                                       ║");
        System.out.println("║  Эндпоинт: http://localhost:" + PORT + "/verify          ║");
        System.out.println("║  Пример (верный):                                ║");
        System.out.println("║    http://localhost:" + PORT + "/verify?phone=89001234567 ║");
        System.out.println("║    &address=ул.Ленина,5                          ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
    }

    // ─── Логика верификации ───────────────────────────────────────────────────

    /**
     * Верифицирует телефон и адрес по заданному правилу.
     *
     * <p>Правило:
     * <ul>
     *   <li>Телефон: 11 цифр, начинается с «8» или «7» (после удаления «+»)</li>
     *   <li>Адрес: не пустой, содержит хотя бы одну цифру (номер дома)</li>
     * </ul>
     *
     * @param phone   номер телефона
     * @param address адрес
     * @return результат верификации
     */
    private static VerificationResult verify(String phone, String address) {
        if (phone.isBlank()) {
            return new VerificationResult(false, "Номер телефона не указан");
        }

        // Нормализуем: убираем +, пробелы, дефисы, скобки
        String normalizedPhone = phone.replaceAll("[+\\s\\-()]", "");

        // Проверяем длину (11 цифр)
        if (!normalizedPhone.matches("\\d{11}")) {
            return new VerificationResult(false,
                    "Некорректный номер телефона (ожидается 11 цифр)");
        }

        // Проверяем начало: 8 или 7
        if (!normalizedPhone.startsWith("8") && !normalizedPhone.startsWith("7")) {
            return new VerificationResult(false,
                    "Номер должен начинаться с 8 или 7");
        }

        // Проверяем адрес
        if (address.isBlank()) {
            return new VerificationResult(false, "Адрес не указан");
        }

        if (!address.matches(".*\\d.*")) {
            return new VerificationResult(false,
                    "Адрес должен содержать номер дома");
        }

        return new VerificationResult(true, "Данные подтверждены");
    }

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    /**
     * Разбирает строку параметров URL-запроса в карту «ключ → значение».
     *
     * @param rawQuery строка параметров запроса (без знака «?»)
     * @return карта параметров
     */
    private static Map<String, String> parseQueryParams(String rawQuery) {
        Map<String, String> params = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return params;

        for (String pair : rawQuery.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key   = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    /**
     * Формирует JSON-ответ сервера.
     *
     * @param phone    номер телефона
     * @param address  адрес
     * @param verified результат верификации
     * @param message  сообщение
     * @return строка JSON
     */
    private static String buildJson(String phone, String address,
                                    boolean verified, String message) {
        return String.format(
                "{\"phone\":\"%s\",\"address\":\"%s\",\"verified\":%b,\"message\":\"%s\"}",
                phone, address, verified, message
        );
    }

    /**
     * Отправляет HTTP-ответ с JSON-телом.
     *
     * @param exchange   объект обмена HTTP
     * @param statusCode HTTP-статус
     * @param body       тело ответа (JSON)
     * @throws IOException при ошибке отправки
     */
    private static void sendResponse(HttpExchange exchange, int statusCode, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ─── Вспомогательная запись ───────────────────────────────────────────────

    /**
     * Результат верификации данных звонящего.
     *
     * @param verified {@code true} — данные подтверждены
     * @param message  сообщение для клиента
     */
    private record VerificationResult(boolean verified, String message) {}
}
