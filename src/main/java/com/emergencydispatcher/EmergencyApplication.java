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
 * При запуске открывается SOS-экран с шестью кнопками быстрого выбора.
 * Из него доступен полноценный режим диспетчера (старый main-view.fxml).
 */
public class EmergencyApplication extends Application {

    private static final String WINDOW_TITLE = "Экстренный вызов 112 — ЕДДС";
    private static final double MIN_WIDTH  = 900;
    private static final double MIN_HEIGHT = 700;

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        DatabaseManager.getInstance().initDatabase();

        showSosScreen();

        stage.setTitle(WINDOW_TITLE);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.show();
    }

    // ─── Переключение экранов ────────────────────────────────────────────────

    public static void showSosScreen() {
        loadAndSetScene("fxml/sos-view.fxml", WINDOW_TITLE);
    }

    public static void showDispatcherScreen() {
        loadAndSetScene("fxml/main-view.fxml", "Диспетчер ЕДДС 112 — расширенный режим");
    }

    private static void loadAndSetScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    EmergencyApplication.class.getResource(fxmlPath)
            );
            Scene scene = new Scene(loader.load(), primaryStage.getScene() != null
                    ? primaryStage.getScene().getWidth() : MIN_WIDTH,
                    primaryStage.getScene() != null
                    ? primaryStage.getScene().getHeight() : MIN_HEIGHT);
            scene.getStylesheets().add(
                    EmergencyApplication.class.getResource("css/style.css").toExternalForm()
            );
            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить экран: " + fxmlPath, e);
        }
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) { launch(); }
}
