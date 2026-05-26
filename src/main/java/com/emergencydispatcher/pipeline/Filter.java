package com.emergencydispatcher.pipeline;

import com.emergencydispatcher.model.IncidentReport;

/**
 * Интерфейс фильтра в архитектурном паттерне «Каналы и фильтры».
 *
 * <p>Каждый фильтр получает на вход {@link IncidentReport} (или его расширение),
 * выполняет свою часть обработки и передаёт результат следующему фильтру.
 *
 * <p>Если данные не соответствуют требованиям фильтра, выбрасывается
 * {@link FilterException} с описанием причины отказа.
 *
 * <p>Цепочка фильтров:
 * <pre>
 *   ValidationFilter
 *       → CallVerificationFilter
 *           → UrgencyClassificationFilter
 *               → DispatchRoutingFilter
 * </pre>
 *
 * @param <T> тип входных данных фильтра (наследник {@link IncidentReport})
 */
@FunctionalInterface
public interface Filter<T extends IncidentReport> {

    /**
     * Выполняет обработку данных.
     *
     * @param data входные данные
     * @throws FilterException если данные не прошли фильтр
     */
    void process(T data) throws FilterException;
}
