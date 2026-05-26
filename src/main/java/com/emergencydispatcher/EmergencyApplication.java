package com.emergencydispatcher;

import com.emergencydispatcher.db.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Главный класс JavaFX-приложения «Цифровой двойник диспетчера экстренных служб».
 *
 * <p>Порядок инициализации:
 * <ol>
 *   <li>Инициализация базы данных SQLite (создание таблиц при первом запуске)</li>
 *   <li>Загрузка главного FXML-экрана с формой приёма сообщений</li>
 *   <li>Отображение главного окна приложения</li>
 * </ol>
 *
 * <p>⚠ Перед запуском этого приложения необходимо запустить {@link server.CallVerificationServer}
 * на порту 8282.
 */
public class EmergencyApplication extends Application {

    /** Заголовок главного окна приложения */
    private static final String WINDOW_TITLE = "Цифровой двойник диспетчера экстренных служб";

    /** Минимальная ширина главного окна */
    private static final double MIN_WIDTH = 900;

    /** Минимальная высота главного окна */
    private static final double MIN_HEIGHT = 700;

    @Override
    public void start(Stage stage) throws IOException {
        // 1. Инициализация БД (создание таблиц, если не существуют)
        DatabaseManager.getInstance().initDatabase();

        // 2. Загрузка FXML главного экрана
        FXMLLoader fxmlLoader = new FXMLLoader(
                EmergencyApplication.class.getResource("fxml/main-view.fxml")
        );

        // 3. Создание и настройка сцены
        Scene scene = new Scene(fxmlLoader.load(), MIN_WIDTH, MIN_HEIGHT);
        scene.getStylesheets().add(
                EmergencyApplication.class.getResource("css/style.css").toExternalForm()
        );

        // 4. Настройка и отображение главного окна
        stage.setTitle(WINDOW_TITLE);
        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
