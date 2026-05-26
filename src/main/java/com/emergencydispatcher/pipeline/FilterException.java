package com.emergencydispatcher.pipeline;

/**
 * Исключение, выбрасываемое фильтром при невозможности обработать данные.
 *
 * <p>Используется в цепочке «Каналы и фильтры» для остановки обработки
 * с информативным сообщением об ошибке.
 */
public class FilterException extends Exception {

    /**
     * Создаёт новое исключение фильтра с описанием причины.
     *
     * @param message описание ошибки (отображается пользователю)
     */
    public FilterException(String message) {
        super(message);
    }

    /**
     * Создаёт новое исключение фильтра с описанием причины и первопричиной.
     *
     * @param message описание ошибки
     * @param cause   исходное исключение
     */
    public FilterException(String message, Throwable cause) {
        super(message, cause);
    }
}
