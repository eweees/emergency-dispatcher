package com.emergencydispatcher.handler;

import com.emergencydispatcher.model.Appeal;
import com.emergencydispatcher.model.DispatchTicket;
import com.emergencydispatcher.model.IncidentReport;

import java.util.Random;

/**
 * Обработчик приоритета YELLOW — срочный вызов, реагирование до 30 минут.
 *
 * <p>Направляет экстренные службы в зависимости от типа происшествия:
 * <ul>
 *   <li>🚔 Полиция (102) — при угрозе насилия, краже</li>
 *   <li>🚒 Пожарная служба (101) — при пожаре без угрозы жизни</li>
 *   <li>🔧 Аварийно-ремонтная служба — при коммунальных авариях</li>
 * </ul>
 *
 * <p>ETA: 15–30 минут.
 * Номер наряда: формат «EM-YEL-XXXXX».
 */
public class YellowHandler {

    /** Префикс номера срочного наряда */
    private static final String ORDER_PREFIX = "EM-YEL-";

    /** Генератор случайных чисел для ETA и номера наряда */
    private final Random random = new Random();

    /**
     * Формирует наряд-талон для YELLOW-приоритета.
     *
     * @param report  сообщение о происшествии
     * @param appeal  зарегистрированное обращение
     * @return сформированный наряд-талон
     */
    public DispatchTicket handle(IncidentReport report, Appeal appeal) {
        System.out.println("[YellowHandler] Формирование YELLOW-наряда...");

        String orderNumber = generateOrderNumber();
        int eta = 15 + random.nextInt(16); // 15–30 минут
        int queueNumber = 1 + random.nextInt(5);

        String callerLine = report.getCallerName().isBlank() ? "Анонимно (SOS)" : report.getCallerName();
        String phoneLine  = report.getPhoneNumber().isBlank() ? "не указан" : report.getPhoneNumber();
        String descLine   = report.getDescription().isBlank() ? "" :
                "\nОписание:    " + report.getDescription();

        String title = "🟡 СРОЧНЫЙ ВЫЗОВ — РЕАГИРОВАНИЕ ДО 30 МИН";

        String details = String.format("""
                ═══════════════════════════════════════════
                НАРЯД № %s
                ПРИОРИТЕТ: 🟡 YELLOW — СРОЧНЫЙ
                ═══════════════════════════════════════════

                📋 ДАННЫЕ ЗВОНЯЩЕГО
                ───────────────────────────────────────────
                ФИО:         %s
                Телефон:     %s
                Адрес:       %s%s

                📊 ОЦЕНКА ПРОИСШЕСТВИЯ
                ───────────────────────────────────────────
                Тип(-ы):     %s
                Балл:        %d (срочный)
                Пострадавшие: %s

                🚨 НАПРАВЛЕННЫЕ СЛУЖБЫ
                ───────────────────────────────────────────
                🚔 Полиция (102)            — НАЗНАЧЕНА
                🔧 Аварийная служба ЖКХ    — УВЕДОМЛЕНА
                📞 Позиция в очереди: #%d

                ⏱ ETA: %d минут
                ───────────────────────────────────────────
                ⚠ СТАТУС: ОЖИДАНИЕ ВЫЕЗДА
                Зарегистрировано: %s
                ═══════════════════════════════════════════
                """,
                orderNumber,
                callerLine,
                phoneLine,
                report.getAddress(),
                descLine,
                report.incidentsToString(),
                report.getTotalScore(),
                report.victimsToString(),
                queueNumber,
                eta,
                appeal.getFormattedDate()
        );

        DispatchTicket ticket = new DispatchTicket(Appeal.Priority.YELLOW, title, details, orderNumber);
        ticket.setEtaMinutes(eta);
        ticket.setDispatchedServices(java.util.List.of(
                "🚔 Полиция 102",
                "🔧 Аварийная служба ЖКХ"
        ));
        return ticket;
    }

    /**
     * Генерирует уникальный номер наряда в формате «EM-YEL-XXXXX».
     *
     * @return номер наряда
     */
    private String generateOrderNumber() {
        int number = 10000 + random.nextInt(90000);
        return ORDER_PREFIX + number;
    }
}
