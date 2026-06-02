package com.emergencydispatcher.handler;

import com.emergencydispatcher.model.Appeal;
import com.emergencydispatcher.model.DispatchTicket;
import com.emergencydispatcher.model.IncidentReport;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Обработчик приоритета GREEN — неэкстренный вызов, плановый выезд.
 *
 * <p>Регистрирует обращение и назначает время планового выезда патруля:
 * <ul>
 *   <li>🚔 Ближайший патруль — в течение рабочего дня</li>
 *   <li>📋 Участковый инспектор — по расписанию</li>
 * </ul>
 *
 * <p>Время ожидания: 2–6 часов.
 * Номер наряда: формат «EM-GRN-XXXXX».
 */
public class GreenHandler {

    /** Префикс номера неэкстренного наряда */
    private static final String ORDER_PREFIX = "EM-GRN-";

    /** Форматтер для времени плановых мероприятий */
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");

    /** Генератор случайных чисел */
    private final Random random = new Random();

    /**
     * Формирует наряд-талон для GREEN-приоритета.
     *
     * @param report  сообщение о происшествии
     * @param appeal  зарегистрированное обращение
     * @return сформированный наряд-талон
     */
    public DispatchTicket handle(IncidentReport report, Appeal appeal) {
        System.out.println("[GreenHandler] Формирование GREEN-наряда...");

        String orderNumber = generateOrderNumber();
        int hoursToVisit = 2 + random.nextInt(5); // 2–6 часов
        LocalDateTime visitTime = LocalDateTime.now().plusHours(hoursToVisit);

        // Назначаем участкового/инспектора (случайный из списка)
        String[] inspectors = {
                "Инспектор Сидоров А.В., уч. №12",
                "Инспектор Петров К.М., уч. №7",
                "Инспектор Козлова Т.Н., уч. №15",
                "Инспектор Новиков Д.С., уч. №3"
        };
        String inspector = inspectors[random.nextInt(inspectors.length)];

        String callerLine = report.getCallerName().isBlank() ? "Анонимно (SOS)" : report.getCallerName();
        String phoneLine  = report.getPhoneNumber().isBlank() ? "не указан" : report.getPhoneNumber();
        String descLine   = report.getDescription().isBlank() ? "" :
                "\nОписание:    " + report.getDescription();

        String title = "🟢 НЕЭКСТРЕННЫЙ ВЫЗОВ — ПЛАНОВЫЙ ВЫЕЗД";

        String details = String.format("""
                ═══════════════════════════════════════════
                НАРЯД № %s
                ПРИОРИТЕТ: 🟢 GREEN — НЕЭКСТРЕННЫЙ
                ═══════════════════════════════════════════

                📋 ДАННЫЕ ЗВОНЯЩЕГО
                ───────────────────────────────────────────
                ФИО:         %s
                Телефон:     %s
                Адрес:       %s%s

                📊 ОЦЕНКА ПРОИСШЕСТВИЯ
                ───────────────────────────────────────────
                Тип(-ы):     %s
                Балл:        %d (неэкстренный)
                Пострадавшие: %s

                📅 ПЛАНОВЫЕ МЕРОПРИЯТИЯ
                ───────────────────────────────────────────
                Назначен:    %s
                Прибытие:    ~%s

                📞 РЕКОМЕНДАЦИИ
                ───────────────────────────────────────────
                • Оставайтесь на месте или будьте доступны
                  по указанному номеру телефона
                • При ухудшении ситуации перезвоните 112

                ⏱ Ожидаемое время прибытия: ~%d ч.
                ───────────────────────────────────────────
                ✅ СТАТУС: ЗАРЕГИСТРИРОВАНО
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
                inspector,
                visitTime.format(TIME_FORMATTER),
                hoursToVisit,
                appeal.getFormattedDate()
        );

        DispatchTicket ticket = new DispatchTicket(Appeal.Priority.GREEN, title, details, orderNumber);
        ticket.setEtaMinutes(hoursToVisit * 60);
        ticket.setDispatchedServices(java.util.List.of(
                "🚔 Ближайший патруль",
                "👮 Участковый инспектор"
        ));
        return ticket;
    }

    /**
     * Генерирует уникальный номер наряда в формате «EM-GRN-XXXXX».
     *
     * @return номер наряда
     */
    private String generateOrderNumber() {
        int number = 10000 + random.nextInt(90000);
        return ORDER_PREFIX + number;
    }
}
