package com.emergencydispatcher.handler;

import com.emergencydispatcher.model.Appeal;
import com.emergencydispatcher.model.DispatchTicket;
import com.emergencydispatcher.model.IncidentReport;

import java.util.Random;

/**
 * Обработчик приоритета RED — угроза жизни, немедленное реагирование.
 *
 * <p>Направляет экстренные службы:
 * <ul>
 *   <li>🚒 Пожарная служба (101)</li>
 *   <li>🚑 Скорая медицинская помощь (103)</li>
 *   <li>🚔 Полиция (102)</li>
 *   <li>🛡 Росгвардия (при необходимости)</li>
 * </ul>
 *
 * <p>ETA: 5–10 минут.
 * Номер наряда: формат «EM-RED-XXXXX».
 */
public class RedHandler {

    /** Префикс номера экстренного наряда */
    private static final String ORDER_PREFIX = "EM-RED-";

    /** Генератор случайных чисел для ETA и номера наряда */
    private final Random random = new Random();

    /**
     * Формирует наряд-талон для RED-приоритета.
     *
     * @param report  сообщение о происшествии
     * @param appeal  зарегистрированное обращение
     * @return сформированный наряд-талон
     */
    public DispatchTicket handle(IncidentReport report, Appeal appeal) {
        System.out.println("[RedHandler] Формирование RED-наряда...");

        String orderNumber = generateOrderNumber();
        int eta = 5 + random.nextInt(6); // 5–10 минут

        String callerLine = report.getCallerName().isBlank() ? "Анонимно (SOS)" : report.getCallerName();
        String phoneLine  = report.getPhoneNumber().isBlank() ? "не указан" : report.getPhoneNumber();
        String descLine   = report.getDescription().isBlank() ? "" :
                "\nОписание:    " + report.getDescription();

        String title = "🔴 ЭКСТРЕННОЕ РЕАГИРОВАНИЕ — УГРОЗА ЖИЗНИ";

        String details = String.format("""
                ═══════════════════════════════════════════
                НАРЯД № %s
                ПРИОРИТЕТ: 🔴 RED — УГРОЗА ЖИЗНИ
                ═══════════════════════════════════════════

                📋 ДАННЫЕ ЗВОНЯЩЕГО
                ───────────────────────────────────────────
                ФИО:         %s
                Телефон:     %s
                Адрес:       %s%s

                📊 ОЦЕНКА ПРОИСШЕСТВИЯ
                ───────────────────────────────────────────
                Тип(-ы):     %s
                Балл:        %d (критический)
                Пострадавшие: %s

                🚨 НАПРАВЛЕННЫЕ СЛУЖБЫ
                ───────────────────────────────────────────
                🚒 Пожарная служба (101) — ВЫЕХАЛА
                🚑 Скорая помощь (103)   — ВЫЕХАЛА
                🚔 Полиция (102)         — ВЫЕХАЛА
                🛡 Росгвардия            — УВЕДОМЛЕНА

                ⏱ ETA: %d минут
                ───────────────────────────────────────────
                ⚠ СТАТУС: АКТИВНЫЙ НАРЯД
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
                eta,
                appeal.getFormattedDate()
        );

        DispatchTicket ticket = new DispatchTicket(Appeal.Priority.RED, title, details, orderNumber);
        ticket.setEtaMinutes(eta);
        ticket.setDispatchedServices(java.util.List.of(
                "🚒 Пожарная служба 101",
                "🚑 Скорая помощь 103",
                "🚔 Полиция 102",
                "🛡 Росгвардия"
        ));
        return ticket;
    }

    /**
     * Генерирует уникальный номер наряда в формате «EM-RED-XXXXX».
     *
     * @return номер наряда
     */
    private String generateOrderNumber() {
        int number = 10000 + random.nextInt(90000);
        return ORDER_PREFIX + number;
    }
}
