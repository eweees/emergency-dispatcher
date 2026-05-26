package com.emergencydispatcher.model;

/**
 * Тип происшествия с баллом срочности.
 *
 * <p>Используется в шкале оценки ({@link com.emergencydispatcher.config.UrgencyScaleConfig})
 * и в форме приёма сообщений (CheckBox-список в GUI).
 *
 * <p>Баллы:
 * <ul>
 *   <li>5 — критическое происшествие (угроза жизни)</li>
 *   <li>3 — срочное происшествие</li>
 *   <li>1 — неэкстренное происшествие</li>
 * </ul>
 */
public class UrgencyData {

    /** Название типа происшествия */
    private final String name;

    /** Балл срочности (1, 3 или 5) */
    private final int score;

    /**
     * Создаёт новый тип происшествия.
     *
     * @param name  название типа происшествия
     * @param score балл срочности (1, 3 или 5)
     */
    public UrgencyData(String name, int score) {
        this.name = name;
        this.score = score;
    }

    /**
     * Возвращает название типа происшествия.
     *
     * @return название
     */
    public String getName() {
        return name;
    }

    /**
     * Возвращает балл срочности.
     *
     * @return балл (1, 3 или 5)
     */
    public int getScore() {
        return score;
    }

    /**
     * Возвращает строковое представление для отображения в GUI.
     *
     * <p>Формат: «Название (N б.)»
     *
     * @return строка для CheckBox
     */
    @Override
    public String toString() {
        return name + " (" + score + " б.)";
    }
}
