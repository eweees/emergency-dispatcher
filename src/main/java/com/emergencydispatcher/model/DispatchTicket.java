package com.emergencydispatcher.model;

/**
 * Талон-наряд — итоговый документ, выдаваемый диспетчером.
 *
 * <p>Аналог {@code Ticket} из оригинального проекта.
 * Содержит:
 * <ul>
 *   <li>Приоритет реагирования (RED / YELLOW / GREEN)</li>
 *   <li>Заголовок наряда (например, «🔴 ЭКСТРЕННОЕ РЕАГИРОВАНИЕ»)</li>
 *   <li>Детальное описание: службы, ETA, номер наряда</li>
 *   <li>Номер наряда в формате «EM-XXXXX»</li>
 * </ul>
 */
public class DispatchTicket {

    /** Тип (приоритет) наряда */
    private Appeal.Priority type;

    /** Заголовок наряда для отображения на экране */
    private String title;

    /** Подробности: назначенные службы, ETA, инструкции */
    private String details;

    /** Уникальный номер наряда (например, «EM-00123») */
    private String orderNumber;

    /** Идентификатор связанного обращения (из БД) */
    private long appealId;

    // ─── Конструкторы ─────────────────────────────────────────────────────────

    /** Конструктор по умолчанию */
    public DispatchTicket() {
    }

    /**
     * Основной конструктор для создания наряда.
     *
     * @param type        приоритет реагирования
     * @param title       заголовок наряда
     * @param details     подробности
     * @param orderNumber номер наряда
     */
    public DispatchTicket(Appeal.Priority type,
                          String title,
                          String details,
                          String orderNumber) {
        this.type = type;
        this.title = title;
        this.details = details;
        this.orderNumber = orderNumber;
    }

    // ─── Геттеры / Сеттеры ────────────────────────────────────────────────────

    public Appeal.Priority getType() { return type; }
    public void setType(Appeal.Priority type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public long getAppealId() { return appealId; }
    public void setAppealId(long appealId) { this.appealId = appealId; }

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    /**
     * Возвращает CSS-класс для стилизации карточки наряда.
     *
     * @return CSS-класс: «ticket-red», «ticket-yellow» или «ticket-green»
     */
    public String getCssClass() {
        return switch (type) {
            case RED    -> "ticket-red";
            case YELLOW -> "ticket-yellow";
            case GREEN  -> "ticket-green";
        };
    }

    @Override
    public String toString() {
        return "DispatchTicket{" +
                "orderNumber='" + orderNumber + '\'' +
                ", type=" + type +
                ", title='" + title + '\'' +
                '}';
    }
}
