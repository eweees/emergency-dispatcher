package com.emergencydispatcher.db;

import com.emergencydispatcher.model.Appeal;
import com.emergencydispatcher.model.DispatchTicket;
import com.emergencydispatcher.model.IncidentReport;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Менеджер базы данных SQLite — синглтон.
 *
 * <p>Управляет тремя таблицами:
 * <ul>
 *   <li>{@code incident_reports} — сообщения о происшествиях</li>
 *   <li>{@code appeals} — зарегистрированные обращения с приоритетом</li>
 *   <li>{@code dispatch_tickets} — выданные наряды</li>
 * </ul>
 *
 * <p>Файл базы данных {@code emergency_dispatcher.db} создаётся
 * автоматически в корне проекта при первом запуске.
 */
public class DatabaseManager {

    /** URL подключения к SQLite-базе */
    private static final String DB_URL = "jdbc:sqlite:emergency_dispatcher.db";

    /** Единственный экземпляр (Singleton) */
    private static DatabaseManager instance;

    // ─── Singleton ────────────────────────────────────────────────────────────

    /** Закрытый конструктор — создание только через {@link #getInstance()} */
    private DatabaseManager() {
    }

    /**
     * Возвращает единственный экземпляр менеджера БД.
     *
     * @return экземпляр {@link DatabaseManager}
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // ─── Инициализация ────────────────────────────────────────────────────────

    /**
     * Инициализирует базу данных: создаёт таблицы, если они не существуют.
     * Вызывается один раз при старте приложения.
     */
    public void initDatabase() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            // Таблица сообщений о происшествиях
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS incident_reports (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        caller_name TEXT    NOT NULL,
                        phone       TEXT    NOT NULL,
                        address     TEXT    NOT NULL,
                        incidents   TEXT    NOT NULL,
                        total_score INTEGER NOT NULL,
                        created_at  TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
                    )
                    """);

            // Таблица обращений
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS appeals (
                        id        INTEGER PRIMARY KEY AUTOINCREMENT,
                        report_id INTEGER NOT NULL REFERENCES incident_reports(id),
                        priority  TEXT    NOT NULL,
                        created_at TEXT   NOT NULL DEFAULT (datetime('now', 'localtime'))
                    )
                    """);

            // Таблица нарядов
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS dispatch_tickets (
                        id           INTEGER PRIMARY KEY AUTOINCREMENT,
                        appeal_id    INTEGER NOT NULL REFERENCES appeals(id),
                        order_number TEXT    NOT NULL,
                        type         TEXT    NOT NULL,
                        title        TEXT    NOT NULL,
                        details      TEXT    NOT NULL
                    )
                    """);

            System.out.println("[DatabaseManager] База данных инициализирована.");

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Ошибка инициализации БД: " + e.getMessage());
            throw new RuntimeException("Не удалось инициализировать БД", e);
        }
    }

    // ─── Сохранение данных ────────────────────────────────────────────────────

    /**
     * Сохраняет сообщение о происшествии в БД.
     *
     * @param report сообщение о происшествии
     * @return сгенерированный идентификатор записи
     */
    public long saveIncidentReport(IncidentReport report) {
        String sql = """
                INSERT INTO incident_reports (caller_name, phone, address, incidents, total_score)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, report.getCallerName());
            ps.setString(2, report.getPhoneNumber());
            ps.setString(3, report.getAddress());
            ps.setString(4, report.incidentsToString());
            ps.setInt(5, report.getTotalScore());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Ошибка сохранения report: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Сохраняет обращение в БД.
     *
     * @param appeal обращение с приоритетом
     * @return сгенерированный идентификатор записи
     */
    public long saveAppeal(Appeal appeal) {
        String sql = "INSERT INTO appeals (report_id, priority) VALUES (?, ?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, appeal.getReportId());
            ps.setString(2, appeal.getPriority().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Ошибка сохранения appeal: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Сохраняет наряд-талон в БД.
     *
     * @param ticket наряд-талон
     * @return сгенерированный идентификатор записи
     */
    public long saveDispatchTicket(DispatchTicket ticket) {
        String sql = """
                INSERT INTO dispatch_tickets (appeal_id, order_number, type, title, details)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, ticket.getAppealId());
            ps.setString(2, ticket.getOrderNumber());
            ps.setString(3, ticket.getType().name());
            ps.setString(4, ticket.getTitle());
            ps.setString(5, ticket.getDetails());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Ошибка сохранения ticket: " + e.getMessage());
        }
        return -1;
    }

    // ─── Чтение журнала ───────────────────────────────────────────────────────

    /**
     * Возвращает последние N записей журнала обращений.
     *
     * <p>Каждая запись — строка вида:
     * {@code [dd.MM.yyyy HH:mm] PRIORITY | ФИО | Телефон | Адрес | Наряд №}
     *
     * @param limit максимальное количество записей (0 — все)
     * @return список строк для отображения в журнале
     */
    public List<String[]> getJournalEntries(int limit) {
        String sql = """
                SELECT a.created_at, a.priority,
                       r.caller_name, r.phone, r.address, r.total_score,
                       t.order_number
                FROM appeals a
                JOIN incident_reports r ON r.id = a.report_id
                LEFT JOIN dispatch_tickets t ON t.appeal_id = a.id
                ORDER BY a.id DESC
                """ + (limit > 0 ? "LIMIT " + limit : "");

        List<String[]> result = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(new String[]{
                        rs.getString("created_at"),
                        rs.getString("priority"),
                        rs.getString("caller_name"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getString("total_score"),
                        rs.getString("order_number") != null ? rs.getString("order_number") : "—"
                });
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Ошибка чтения журнала: " + e.getMessage());
        }
        return result;
    }

    // ─── Статистика ───────────────────────────────────────────────────────────

    /**
     * Возвращает статистику обращений по приоритетам за указанный период.
     *
     * @param from начало периода (включительно), или {@code null} — без ограничения
     * @param to   конец периода (включительно), или {@code null} — без ограничения
     * @return карта «приоритет → количество обращений»
     */
    public Map<String, Integer> getStatsByPeriod(LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT priority, COUNT(*) AS cnt
                FROM appeals
                WHERE 1=1
                """);
        if (from != null) sql.append(" AND date(created_at) >= '").append(from).append("'");
        if (to   != null) sql.append(" AND date(created_at) <= '").append(to).append("'");
        sql.append(" GROUP BY priority ORDER BY priority");

        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("RED",    0);
        stats.put("YELLOW", 0);
        stats.put("GREEN",  0);

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            while (rs.next()) {
                stats.put(rs.getString("priority"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Ошибка статистики: " + e.getMessage());
        }
        return stats;
    }

    /**
     * Возвращает общее количество обращений в БД.
     *
     * @return общее количество обращений
     */
    public int getTotalAppealsCount() {
        String sql = "SELECT COUNT(*) FROM appeals";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Ошибка подсчёта: " + e.getMessage());
        }
        return 0;
    }

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    /**
     * Открывает соединение с базой данных.
     *
     * @return объект {@link Connection}
     * @throws SQLException если не удалось подключиться
     */
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
