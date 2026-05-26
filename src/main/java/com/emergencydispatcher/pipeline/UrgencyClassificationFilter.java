package com.emergencydispatcher.pipeline;

import com.emergencydispatcher.config.UrgencyScaleConfig;
import com.emergencydispatcher.model.Appeal;
import com.emergencydispatcher.model.IncidentReport;

/**
 * Фильтр 3 — Классификация происшествия по шкале срочности.
 *
 * <p>Алгоритм (приоритет правил сверху вниз):
 * <ol>
 *   <li>Если есть хотя бы один КРИТИЧЕСКИЙ инцидент (5 баллов) → RED</li>
 *   <li>Если есть хотя бы один СРОЧНЫЙ инцидент (3 балла) → YELLOW</li>
 *   <li>Иначе (только неэкстренные, 1 балл) → GREEN</li>
 * </ol>
 *
 * <p>Сумма баллов НЕ влияет на повышение приоритета:
 * множество GREEN-инцидентов остаётся GREEN.
 */
public class UrgencyClassificationFilter implements Filter<IncidentReport> {

    @Override
    public void process(IncidentReport data) throws FilterException {
        System.out.println("[UrgencyClassificationFilter] Начало классификации...");

        // Подсчёт баллов по категориям
        boolean hasCritical = data.getSelectedIncidents().stream()
                .anyMatch(ud -> ud.getScore() == UrgencyScaleConfig.SCORE_CRITICAL);

        boolean hasUrgent = data.getSelectedIncidents().stream()
                .anyMatch(ud -> ud.getScore() == UrgencyScaleConfig.SCORE_URGENT);

        int totalScore = data.getSelectedIncidents().stream()
                .mapToInt(ud -> ud.getScore())
                .sum();

        data.setTotalScore(totalScore);

        // Определяем приоритет по ТИПУ инцидентов, а не по сумме
        Appeal.Priority priority;
        if (hasCritical) {
            priority = Appeal.Priority.RED;
        } else if (hasUrgent) {
            priority = Appeal.Priority.YELLOW;
        } else {
            priority = Appeal.Priority.GREEN;
        }

        // Сохраняем предварительный приоритет в report для RoutingFilter
        data.setPreliminaryPriority(priority);

        System.out.printf("[UrgencyClassificationFilter] Суммарный балл: %d | " +
                        "Критических: %b | Срочных: %b | Приоритет: %s%n",
                totalScore, hasCritical, hasUrgent, priority);
    }
}