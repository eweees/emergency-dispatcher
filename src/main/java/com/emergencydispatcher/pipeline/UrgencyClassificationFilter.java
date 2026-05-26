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
 *   <li>Если есть пострадавшие → RED (независимо от типа инцидента)</li>
 *   <li>Если есть хотя бы один СРОЧНЫЙ инцидент (3 балла) → YELLOW</li>
 *   <li>Иначе (только неэкстренные, 1 балл) → GREEN</li>
 * </ol>
 *
 * <p>Сумма баллов НЕ влияет на повышение приоритета:
 * множество GREEN-инцидентов без пострадавших остаётся GREEN.
 */
public class UrgencyClassificationFilter implements Filter<IncidentReport> {

    @Override
    public void process(IncidentReport data) throws FilterException {
        System.out.println("[UrgencyClassificationFilter] Начало классификации...");

        boolean hasCritical = data.getSelectedIncidents().stream()
                .anyMatch(ud -> ud.getScore() == UrgencyScaleConfig.SCORE_CRITICAL);

        boolean hasUrgent = data.getSelectedIncidents().stream()
                .anyMatch(ud -> ud.getScore() == UrgencyScaleConfig.SCORE_URGENT);

        boolean hasVictims = data.isHasVictims() && data.getVictimsCount() > 0;

        int totalScore = data.getSelectedIncidents().stream()
                .mapToInt(ud -> ud.getScore())
                .sum();

        data.setTotalScore(totalScore);

        // Приоритет определяется по ТИПУ инцидентов и наличию пострадавших
        Appeal.Priority priority;
        if (hasCritical) {
            priority = Appeal.Priority.RED;
            System.out.println("[UrgencyClassificationFilter] Критический инцидент → RED");
        } else if (hasVictims) {
            priority = Appeal.Priority.RED;
            System.out.println("[UrgencyClassificationFilter] Есть пострадавшие (" +
                    data.getVictimsCount() + " чел.) → RED");
        } else if (hasUrgent) {
            priority = Appeal.Priority.YELLOW;
            System.out.println("[UrgencyClassificationFilter] Срочный инцидент → YELLOW");
        } else {
            priority = Appeal.Priority.GREEN;
            System.out.println("[UrgencyClassificationFilter] Только неэкстренные → GREEN");
        }

        data.setPreliminaryPriority(priority);

        System.out.printf("[UrgencyClassificationFilter] Балл: %d | Крит: %b | " +
                        "Срочн: %b | Постр: %b (%d) | Итог: %s%n",
                totalScore, hasCritical, hasUrgent,
                hasVictims, data.getVictimsCount(), priority);
    }
}
