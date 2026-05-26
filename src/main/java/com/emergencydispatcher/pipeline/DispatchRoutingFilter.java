package com.emergencydispatcher.pipeline;

import com.emergencydispatcher.config.UrgencyScaleConfig;
import com.emergencydispatcher.db.DatabaseManager;
import com.emergencydispatcher.handler.GreenHandler;
import com.emergencydispatcher.handler.RedHandler;
import com.emergencydispatcher.handler.YellowHandler;
import com.emergencydispatcher.model.Appeal;
import com.emergencydispatcher.model.DispatchTicket;
import com.emergencydispatcher.model.IncidentReport;

/**
 * Фильтр 4 — Маршрутизация и сохранение результатов в БД.
 *
 * <p>Последний фильтр в цепочке. Выполняет:
 * <ol>
 *   <li>Сохранение {@link IncidentReport} в таблицу {@code incident_reports}</li>
 *   <li>Определение приоритета по суммарному баллу</li>
 *   <li>Создание и сохранение {@link Appeal} в таблицу {@code appeals}</li>
 *   <li>Вызов соответствующего обработчика (RED / YELLOW / GREEN)</li>
 *   <li>Сохранение {@link DispatchTicket} в таблицу {@code dispatch_tickets}</li>
 * </ol>
 *
 * <p>Результирующий наряд-талон сохраняется в поле {@link #lastTicket},
 * откуда его забирает контроллер для отображения.
 */
public class DispatchRoutingFilter implements Filter<IncidentReport> {

    /** Наряд-талон, сформированный в результате обработки */
    private DispatchTicket lastTicket;

    // ─── Обработчики приоритетов ──────────────────────────────────────────────

    private final RedHandler    redHandler    = new RedHandler();
    private final YellowHandler yellowHandler = new YellowHandler();
    private final GreenHandler  greenHandler  = new GreenHandler();

    @Override
    public void process(IncidentReport data) throws FilterException {
        System.out.println("[DispatchRoutingFilter] Маршрутизация. Балл: " + data.getTotalScore());

        DatabaseManager db = DatabaseManager.getInstance();

        // 1. Сохраняем сообщение о происшествии
        long reportId = db.saveIncidentReport(data);
        if (reportId < 0) {
            throw new FilterException("Ошибка записи в базу данных (incident_report).");
        }

        // 2. Определяем приоритет
        Appeal.Priority priority = determinePriority(data);
        System.out.println("[DispatchRoutingFilter] Приоритет: " + priority);

        // 3. Создаём и сохраняем обращение
        Appeal appeal = new Appeal(reportId, priority);
        long appealId = db.saveAppeal(appeal);
        if (appealId < 0) {
            throw new FilterException("Ошибка записи в базу данных (appeal).");
        }
        appeal.setId(appealId);

        // 4. Вызываем нужный обработчик → получаем наряд-талон
        DispatchTicket ticket = switch (priority) {
            case RED    -> redHandler.handle(data, appeal);
            case YELLOW -> yellowHandler.handle(data, appeal);
            case GREEN  -> greenHandler.handle(data, appeal);
        };

        ticket.setAppealId(appealId);

        // 5. Сохраняем наряд в БД
        long ticketId = db.saveDispatchTicket(ticket);
        if (ticketId < 0) {
            throw new FilterException("Ошибка записи в базу данных (dispatch_ticket).");
        }

        this.lastTicket = ticket;
        System.out.println("[DispatchRoutingFilter] Наряд сформирован: " + ticket.getOrderNumber());
    }

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    /**
     * Определяет приоритет реагирования по суммарному баллу.
     *
     * @param score суммарный балл
     * @return {@link Appeal.Priority}
     */
    private Appeal.Priority determinePriority(IncidentReport data) {
        // Приоритет уже определён в UrgencyClassificationFilter по типу инцидентов
        Appeal.Priority p = data.getPreliminaryPriority();
        return p != null ? p : Appeal.Priority.GREEN;
    }

    /**
     * Возвращает последний сформированный наряд-талон.
     *
     * @return {@link DispatchTicket} или {@code null}, если обработки ещё не было
     */
    public DispatchTicket getLastTicket() {
        return lastTicket;
    }
}
