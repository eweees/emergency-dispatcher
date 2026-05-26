package com.emergencydispatcher;

/**
 * Лаунчер приложения.
 * Отдельный класс-обёртка нужен для корректного запуска JavaFX-приложения
 * из JAR-файла без явного добавления модулей JavaFX в classpath.
 */
public class Launcher {

    public static void main(String[] args) {
        EmergencyApplication.main(args);
    }
}
