package ru.practicum.stats.collector.service;

public interface CollectorHandler<T> {

    void handle(T proto);
}
