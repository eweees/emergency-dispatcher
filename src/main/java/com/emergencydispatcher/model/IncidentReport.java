package com.emergencydispatcher.model;

import java.util.List;

/**
 * Сообщение о происшествии — входные данные от звонящего.
 *
 * <p>Аналог {@code Questionnaire} из оригинального проекта.
 * Содержит:
 * <ul>
 *   <li>Имя звонящего (ФИО)</li>
 *   <li>Номер телефона (для верификации через {@link com.emergencydispatcher.server.CallVerificationServer})</li>
 *   <li>Адрес происшествия</li>
 *   <li>Список отмеченных типов происшествий с баллами</li>
 *   <li>Суммарный балл срочности (заполняется фильтром классификации)</li>
 * </ul>
 */
public class IncidentReport {

    /** ФИО звонящего */
    private String callerName;

    /** Номер телефона (11 цифр, начинается с 8 или +7) */
    private String phoneNumber;

    /** Адрес происшествия (улица, дом, населённый пункт) */
    private String address;

    /** Список выбранных типов происшествий */
    private List<UrgencyData> selectedIncidents;

    /** Суммарный балл — заполняется {@link com.emergencydispatcher.pipeline.UrgencyClassificationFilter} */
    private int totalScore;

    /** Флаг верификации телефона — заполняется {@link com.emergencydispatcher.pipeline.CallVerificationFilter} */
    private boolean phoneVerified;

    /** Есть ли пострадавшие на месте происшествия */
    private boolean hasVictims;

    /** Количество пострадавших (0 если hasVictims == false) */
    private int victimsCount;

    /** Предварительный приоритет — заполняется UrgencyClassificationFilter */
    private Appeal.Priority preliminaryPriority;

    // ─── Конструкторы ─────────────────────────────────────────────────────────

    /** Конструктор по умолчанию (для тестирования и десериализации) */
    public IncidentReport() {
    }

    /**
     * Основной конструктор для приёма сообщения от диспетчера.
     *
     * @param callerName        ФИО звонящего
     * @param phoneNumber       номер телефона
     * @param address           адрес происшествия
     * @param selectedIncidents выбранные типы происшествий
     * @param hasVictims        есть ли пострадавшие
     * @param victimsCount      количество пострадавших
     */
    public IncidentReport(String callerName,
                          String phoneNumber,
                          String address,
                          List<UrgencyData> selectedIncidents,
                          boolean hasVictims,
                          int victimsCount) {
        this.callerName = callerName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.selectedIncidents = selectedIncidents;
        this.hasVictims = hasVictims;
        this.victimsCount = victimsCount;
        this.totalScore = 0;
        this.phoneVerified = false;
    }

    // ─── Геттеры / Сеттеры ────────────────────────────────────────────────────

    public String getCallerName() { return callerName; }
    public void setCallerName(String callerName) { this.callerName = callerName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public List<UrgencyData> getSelectedIncidents() { return selectedIncidents; }
    public void setSelectedIncidents(List<UrgencyData> selectedIncidents) {
        this.selectedIncidents = selectedIncidents;
    }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public boolean isHasVictims() { return hasVictims; }
    public void setHasVictims(boolean hasVictims) { this.hasVictims = hasVictims; }

    public int getVictimsCount() { return victimsCount; }
    public void setVictimsCount(int victimsCount) { this.victimsCount = victimsCount; }

    public Appeal.Priority getPreliminaryPriority() { return preliminaryPriority; }
    public void setPreliminaryPriority(Appeal.Priority p) { this.preliminaryPriority = p; }

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    /**
     * Формирует строку с перечнем инцидентов для сохранения в БД.
     *
     * <p>Формат: «Тип1 (5б.); Тип2 (3б.); ...»
     *
     * @return строковое представление инцидентов
     */
    public String incidentsToString() {
        if (selectedIncidents == null || selectedIncidents.isEmpty()) {
            return "не указано";
        }
        StringBuilder sb = new StringBuilder();
        for (UrgencyData ud : selectedIncidents) {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append(ud.getName()).append(" (").append(ud.getScore()).append("б.)");
        }
        return sb.toString();
    }

    /**
     * Проверяет, содержит ли список хотя бы один критический инцидент (5 баллов).
     *
     * @return {@code true}, если есть критический инцидент
     */
    public boolean hasCriticalIncident() {
        if (selectedIncidents == null) return false;
        return selectedIncidents.stream().anyMatch(ud -> ud.getScore() == 5);
    }

    /**
     * Формирует строку о пострадавших для отображения в наряде.
     *
     * @return «Нет пострадавших» или «Есть пострадавшие: N чел.»
     */
    public String victimsToString() {
        if (!hasVictims || victimsCount == 0) return "Нет пострадавших";
        return "Есть пострадавшие: " + victimsCount + " чел.";
    }

    @Override
    public String toString() {
        return "IncidentReport{" +
                "callerName='" + callerName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", address='" + address + '\'' +
                ", totalScore=" + totalScore +
                ", phoneVerified=" + phoneVerified +
                '}';
    }
}
