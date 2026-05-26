package com.emergencydispatcher.pipeline;

import com.emergencydispatcher.model.IncidentReport;

import java.util.regex.Pattern;

/**
 * Фильтр 1 — Валидация входных данных сообщения о происшествии.
 *
 * <p>Проверяет обязательность и корректность полей:
 * <ul>
 *   <li>ФИО звонящего — не пустое, минимум 2 слова</li>
 *   <li>Номер телефона — 11 цифр, начинается с «8» или «+7»</li>
 *   <li>Адрес происшествия — не пустой, минимум 5 символов</li>
 *   <li>Список инцидентов — минимум 1 выбранный тип</li>
 * </ul>
 *
 * <p>При несоответствии хотя бы одного поля выбрасывается {@link FilterException}
 * с описанием нарушения.
 */
public class ValidationFilter implements Filter<IncidentReport> {

    /** Паттерн валидного российского телефонного номера */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(\\+7|8)\\d{10}$");

    /** Минимальное количество слов в ФИО */
    private static final int MIN_NAME_WORDS = 2;

    /** Минимальная длина адреса */
    private static final int MIN_ADDRESS_LENGTH = 5;

    @Override
    public void process(IncidentReport data) throws FilterException {
        System.out.println("[ValidationFilter] Начало валидации: " + data);

        validateCallerName(data.getCallerName());
        validatePhoneNumber(data.getPhoneNumber());
        validateAddress(data.getAddress());
        validateIncidents(data);

        System.out.println("[ValidationFilter] Валидация пройдена успешно.");
    }

    // ─── Частные методы валидации ─────────────────────────────────────────────

    /**
     * Проверяет ФИО звонящего.
     *
     * @param name ФИО
     * @throws FilterException если ФИО пустое или содержит менее 2 слов
     */
    private void validateCallerName(String name) throws FilterException {
        if (name == null || name.isBlank()) {
            throw new FilterException("Введите ФИО звонящего.");
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length < MIN_NAME_WORDS) {
            throw new FilterException(
                    "ФИО должно содержать минимум " + MIN_NAME_WORDS + " слова (Фамилия Имя)."
            );
        }
    }

    /**
     * Проверяет формат номера телефона.
     *
     * <p>Допустимые форматы: «8XXXXXXXXXX» или «+7XXXXXXXXXX».
     *
     * @param phone номер телефона
     * @throws FilterException если формат не соответствует требованиям
     */
    private void validatePhoneNumber(String phone) throws FilterException {
        if (phone == null || phone.isBlank()) {
            throw new FilterException("Введите номер телефона звонящего.");
        }
        // Убираем пробелы и дефисы для проверки
        String normalized = phone.replaceAll("[\\s\\-()]", "");
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new FilterException(
                    "Некорректный номер телефона.\n" +
                    "Допустимые форматы: 8XXXXXXXXXX или +7XXXXXXXXXX (11 цифр)."
            );
        }
    }

    /**
     * Проверяет адрес происшествия.
     *
     * @param address адрес
     * @throws FilterException если адрес пустой или слишком короткий
     */
    private void validateAddress(String address) throws FilterException {
        if (address == null || address.isBlank()) {
            throw new FilterException("Введите адрес происшествия.");
        }
        if (address.trim().length() < MIN_ADDRESS_LENGTH) {
            throw new FilterException(
                    "Адрес должен содержать минимум " + MIN_ADDRESS_LENGTH + " символов."
            );
        }
    }

    /**
     * Проверяет список выбранных типов происшествий.
     *
     * @param data сообщение о происшествии
     * @throws FilterException если ни один тип происшествия не выбран
     */
    private void validateIncidents(IncidentReport data) throws FilterException {
        if (data.getSelectedIncidents() == null || data.getSelectedIncidents().isEmpty()) {
            throw new FilterException(
                    "Выберите хотя бы один тип происшествия из списка."
            );
        }
    }
}
