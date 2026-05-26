package com.emergencydispatcher.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Обращение в диспетчерский центр.
 *
 * <p>Создаётся после успешного прохождения всех фильтров и содержит:
 * <ul>
 *   <li>Ссылку на исходное сообщение о происшествии</li>
 *   <li>Приоритет реагирования (RED / YELLOW / GREEN)</li>
 *   <li>Дату и время регистрации обращения</li>
 * </ul>
 *
 * <p>Приоритеты:
 * <ul>
 *   <li>{@code RED} — угроза жизни, немедленное реагирование</li>
 *   <li>{@code YELLOW} — срочный вызов, реагирование в ближайшее время</li>
 *   <li>{@code GREEN} — неэкстренный вызов, плановый выезд</li>
 * </ul>
 */
public class Appeal {

    /** Форматтер для отображения даты/времени */
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    /** Идентификатор из БД (заполняется после сохранения) */
    private long id;

    /** Идентификатор связанного сообщения о происшествии */
    private long reportId;

    /** Приоритет реагирования */
    private Priority priority;

    /** Дата и время регистрации обращения */
    private LocalDateTime registeredAt;

    // ─── Перечисление приоритетов ─────────────────────────────────────────────

    /**
     * Приоритет реагирования диспетчерского центра.
     */
    public enum Priority {
        /** Угроза жизни — немедленное реагирование всех экстренных служб */
        RED,
        /** Срочный вызов — выезд полиции / скорой в течение 30 мин */
        YELLOW,
        /** Неэкстренный вызов — плановый выезд или консультация */
        GREEN
    }

    // ─── Конструкторы ─────────────────────────────────────────────────────────

    /** Конструктор по умолчанию */
    public Appeal() {
        this.registeredAt = LocalDateTime.now();
    }

    /**
     * Конструктор для создания обращения после классификации.
     *
     * @param reportId идентификатор сообщения о происшествии
     * @param priority приоритет реагирования
     */
    public Appeal(long reportId, Priority priority) {
        this.reportId = reportId;
        this.priority = priority;
        this.registeredAt = LocalDateTime.now();
    }

    // ─── Геттеры / Сеттеры ────────────────────────────────────────────────────

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getReportId() { return reportId; }
    public void setReportId(long reportId) { this.reportId = reportId; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    /**
     * Возвращает дату/время в форматированном виде для отображения.
     *
     * @return строка в формате «dd.MM.yyyy HH:mm:ss»
     */
    public String getFormattedDate() {
        return registeredAt != null ? registeredAt.format(FORMATTER) : "—";
    }

    @Override
    public String toString() {
        return "Appeal{" +
                "id=" + id +
                ", reportId=" + reportId +
                ", priority=" + priority +
                ", registeredAt=" + getFormattedDate() +
                '}';
    }
}
