package com.emergencydispatcher.config;

import com.emergencydispatcher.model.UrgencyData;

import java.util.List;

/**
 * Конфигурация шкалы срочности для оценки происшествий.
 *
 * <p>Шкала содержит 15 типов происшествий, разбитых на 3 категории:
 * <ul>
 *   <li><b>КРИТИЧЕСКИЕ (5 баллов)</b> — угроза жизни, немедленное реагирование (RED)</li>
 *   <li><b>СРОЧНЫЕ (3 балла)</b> — серьёзный инцидент, реагирование в ближайшее время (YELLOW)</li>
 *   <li><b>НЕЭКСТРЕННЫЕ (1 балл)</b> — плановый выезд, постановка на учёт (GREEN)</li>
 * </ul>
 *
 * <p>Порог автоматического RED-приоритета: любой критический тип (5 баллов)
 * или суммарный балл ≥ 5.
 *
 * <p>Порог YELLOW-приоритета: суммарный балл 3–4.
 *
 * <p>GREEN-приоритет: суммарный балл 1–2.
 */
public class UrgencyScaleConfig {

    // ─── Пороговые значения приоритетов ──────────────────────────────────────

    /** Балл критического типа происшествия (немедленно RED) */
    public static final int CRITICAL_SCORE = 5;

    /** Минимальный суммарный балл для RED (если нет критического) */
    public static final int RED_THRESHOLD = 5;

    /** Минимальный суммарный балл для YELLOW */
    public static final int YELLOW_THRESHOLD = 3;

    // ─── Значения баллов по категориям ────────────────────────────────────────

    /** Балл критических происшествий */
    public static final int SCORE_CRITICAL = 5;

    /** Балл срочных происшествий */
    public static final int SCORE_URGENT = 3;

    /** Балл неэкстренных происшествий */
    public static final int SCORE_ROUTINE = 1;

    // ─── Каталог типов происшествий ───────────────────────────────────────────

    /**
     * Возвращает полный список типов происшествий со шкалой баллов.
     *
     * @return неизменяемый список {@link UrgencyData}
     */
    public static List<UrgencyData> getAllIncidentTypes() {
        return List.of(
                // ── Критические (5 баллов) ───────────────────────────────────
                new UrgencyData("Пожар с угрозой жизни людей",         SCORE_CRITICAL),
                new UrgencyData("ДТП с пострадавшими / жертвами",       SCORE_CRITICAL),
                new UrgencyData("Вооружённое нападение / стрельба",     SCORE_CRITICAL),
                new UrgencyData("Обрушение здания / завал людей",       SCORE_CRITICAL),
                new UrgencyData("Утечка газа / взрыв",                  SCORE_CRITICAL),

                // ── Срочные (3 балла) ─────────────────────────────────────────
                new UrgencyData("ДТП без пострадавших (материальный ущерб)", SCORE_URGENT),
                new UrgencyData("Пожар без угрозы жизни (бытовой)",    SCORE_URGENT),
                new UrgencyData("Угроза насилия / семейный конфликт",   SCORE_URGENT),
                new UrgencyData("Кража / грабёж (по горячим следам)",   SCORE_URGENT),
                new UrgencyData("Затопление / авария коммунальных сетей", SCORE_URGENT),

                // ── Неэкстренные (1 балл) ─────────────────────────────────────
                new UrgencyData("Мелкое хулиганство / дебош",          SCORE_ROUTINE),
                new UrgencyData("Потеря документов / имущества",        SCORE_ROUTINE),
                new UrgencyData("Плановая проверка / профилактика",     SCORE_ROUTINE),
                new UrgencyData("Жалоба на соседей (шум, бытовой)",     SCORE_ROUTINE),
                new UrgencyData("Консультация / информационный запрос", SCORE_ROUTINE)
        );
    }

    /**
     * Возвращает только критические типы происшествий (5 баллов).
     *
     * @return неизменяемый список критических {@link UrgencyData}
     */
    public static List<UrgencyData> getCriticalIncidentTypes() {
        return getAllIncidentTypes().stream()
                .filter(d -> d.getScore() == SCORE_CRITICAL)
                .toList();
    }

    /**
     * Возвращает только срочные типы происшествий (3 балла).
     *
     * @return неизменяемый список срочных {@link UrgencyData}
     */
    public static List<UrgencyData> getUrgentIncidentTypes() {
        return getAllIncidentTypes().stream()
                .filter(d -> d.getScore() == SCORE_URGENT)
                .toList();
    }

    /**
     * Возвращает только неэкстренные типы происшествий (1 балл).
     *
     * @return неизменяемый список неэкстренных {@link UrgencyData}
     */
    public static List<UrgencyData> getRoutineIncidentTypes() {
        return getAllIncidentTypes().stream()
                .filter(d -> d.getScore() == SCORE_ROUTINE)
                .toList();
    }
}
