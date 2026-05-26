package com.emergencydispatcher.pipeline;

import com.emergencydispatcher.model.DispatchTicket;
import com.emergencydispatcher.model.IncidentReport;

/**
 * Оркестратор цепочки фильтров — реализация паттерна «Каналы и фильтры».
 *
 * <p>Запускает последовательно 4 фильтра:
 * <pre>
 *   IncidentReport
 *        │
 *        ▼
 *   ┌─────────────────────────────┐
 *   │ Фильтр 1: ValidationFilter  │  Проверка ФИО, телефона, адреса, инцидентов
 *   └──────────────┬──────────────┘
 *                  │
 *                  ▼
 *   ┌──────────────────────────────────┐
 *   │ Фильтр 2: CallVerificationFilter │  HTTP-верификация → CallVerificationServer :8282
 *   └──────────────┬───────────────────┘
 *                  │
 *                  ▼
 *   ┌────────────────────────────────────────┐
 *   │ Фильтр 3: UrgencyClassificationFilter  │  Подсчёт баллов → предварительный приоритет
 *   └──────────────┬─────────────────────────┘
 *                  │
 *                  ▼
 *   ┌────────────────────────────────────┐
 *   │ Фильтр 4: DispatchRoutingFilter    │  Маршрутизация + сохранение в БД
 *   └──────────────┬─────────────────────┘
 *                  │
 *                  ▼
 *            DispatchTicket
 * </pre>
 *
 * <p>При ошибке в любом фильтре обработка прерывается, исключение
 * передаётся в контроллер для отображения пользователю.
 */
public class Pipeline {

    // ─── Фильтры цепочки ─────────────────────────────────────────────────────

    private final ValidationFilter           validationFilter    = new ValidationFilter();
    private final CallVerificationFilter     verificationFilter  = new CallVerificationFilter();
    private final UrgencyClassificationFilter classificationFilter = new UrgencyClassificationFilter();
    private final DispatchRoutingFilter      routingFilter       = new DispatchRoutingFilter();

    /**
     * Запускает полную цепочку обработки сообщения о происшествии.
     *
     * @param report сообщение от звонящего
     * @return сформированный наряд-талон
     * @throws FilterException если данные не прошли один из фильтров
     */
    public DispatchTicket process(IncidentReport report) throws FilterException {
        System.out.println("═".repeat(60));
        System.out.println("[Pipeline] Запуск обработки: " + report.getCallerName());
        System.out.println("═".repeat(60));

        // Последовательно применяем фильтры
        validationFilter.process(report);
        verificationFilter.process(report);
        classificationFilter.process(report);
        routingFilter.process(report);

        DispatchTicket ticket = routingFilter.getLastTicket();

        System.out.println("═".repeat(60));
        System.out.println("[Pipeline] Обработка завершена. Наряд: " + ticket.getOrderNumber());
        System.out.println("═".repeat(60));

        return ticket;
    }
}
