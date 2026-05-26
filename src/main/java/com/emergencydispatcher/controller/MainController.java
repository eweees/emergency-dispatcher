package com.emergencydispatcher.controller;

import com.emergencydispatcher.EmergencyApplication;
import com.emergencydispatcher.config.UrgencyScaleConfig;
import com.emergencydispatcher.model.DispatchTicket;
import com.emergencydispatcher.model.IncidentReport;
import com.emergencydispatcher.model.UrgencyData;
import com.emergencydispatcher.pipeline.FilterException;
import com.emergencydispatcher.pipeline.Pipeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Контроллер главного экрана — форма приёма сообщения о происшествии.
 *
 * <p>Управляет:
 * <ul>
 *   <li>Полями ввода данных звонящего (ФИО, телефон, адрес)</li>
 *   <li>Списком типов происшествий (CheckBox-группы по категориям)</li>
 *   <li>Запуском цепочки фильтров ({@link Pipeline})</li>
 *   <li>Переходом на экран наряда-талона</li>
 *   <li>Открытием журнала обращений</li>
 * </ul>
 */
public class MainController implements Initializable {

    // ─── FXML-элементы ───────────────────────────────────────────────────────

    @FXML private TextField     callerNameField;
    @FXML private TextField     phoneField;
    @FXML private TextField     addressField;

    /** Контейнер для критических CheckBox (5 баллов) */
    @FXML private VBox          criticalIncidentsBox;

    /** Контейнер для срочных CheckBox (3 балла) */
    @FXML private VBox          urgentIncidentsBox;

    /** Контейнер для неэкстренных CheckBox (1 балл) */
    @FXML private VBox          routineIncidentsBox;

    @FXML private Label         statusLabel;
    @FXML private Button        submitButton;
    @FXML private Button        clearButton;
    @FXML private Button        journalButton;

    // ─── Поля контроллера ────────────────────────────────────────────────────

    /** Список всех CheckBox с привязанными типами происшествий */
    private final List<CheckBoxEntry> checkBoxEntries = new ArrayList<>();

    /** Конвейер обработки */
    private final Pipeline pipeline = new Pipeline();

    // ─── Инициализация ────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        buildIncidentCheckBoxes();
        setStatus("Готов к приёму сообщений. Заполните форму и нажмите «Отправить».", false);
    }

    /**
     * Создаёт CheckBox-элементы для каждой категории происшествий.
     */
    private void buildIncidentCheckBoxes() {
        // Критические (5 баллов)
        for (UrgencyData ud : UrgencyScaleConfig.getCriticalIncidentTypes()) {
            CheckBox cb = new CheckBox(ud.toString());
            cb.getStyleClass().add("incident-critical");
            criticalIncidentsBox.getChildren().add(cb);
            checkBoxEntries.add(new CheckBoxEntry(cb, ud));
        }

        // Срочные (3 балла)
        for (UrgencyData ud : UrgencyScaleConfig.getUrgentIncidentTypes()) {
            CheckBox cb = new CheckBox(ud.toString());
            cb.getStyleClass().add("incident-urgent");
            urgentIncidentsBox.getChildren().add(cb);
            checkBoxEntries.add(new CheckBoxEntry(cb, ud));
        }

        // Неэкстренные (1 балл)
        for (UrgencyData ud : UrgencyScaleConfig.getRoutineIncidentTypes()) {
            CheckBox cb = new CheckBox(ud.toString());
            cb.getStyleClass().add("incident-routine");
            routineIncidentsBox.getChildren().add(cb);
            checkBoxEntries.add(new CheckBoxEntry(cb, ud));
        }
    }

    // ─── Обработчики событий ─────────────────────────────────────────────────

    /**
     * Обрабатывает нажатие кнопки «Отправить».
     * Запускает конвейер обработки в отдельном потоке,
     * чтобы не блокировать UI-поток JavaFX.
     */
    @FXML
    private void onSubmit() {
        submitButton.setDisable(true);
        setStatus("⏳ Обрабатываем сообщение...", false);

        // Собираем данные из формы
        IncidentReport report = buildReport();

        // Запускаем конвейер в фоновом потоке
        Thread.ofVirtual().start(() -> {
            try {
                DispatchTicket ticket = pipeline.process(report);

                // Возвращаемся в UI-поток для отображения результата
                Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    setStatus("✅ Наряд сформирован: " + ticket.getOrderNumber(), false);
                    openTicketScreen(ticket, report);
                });

            } catch (FilterException fe) {
                Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    setStatus("❌ " + fe.getMessage(), true);
                    showError("Ошибка обработки", fe.getMessage());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    setStatus("❌ Системная ошибка: " + e.getMessage(), true);
                    showError("Системная ошибка", e.getMessage());
                });
            }
        });
    }

    /**
     * Обрабатывает нажатие кнопки «Очистить».
     */
    @FXML
    private void onClear() {
        callerNameField.clear();
        phoneField.clear();
        addressField.clear();
        checkBoxEntries.forEach(e -> e.checkBox().setSelected(false));
        setStatus("Форма очищена. Готов к приёму нового сообщения.", false);
    }

    /**
     * Открывает экран журнала обращений.
     */
    @FXML
    private void onOpenJournal() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    EmergencyApplication.class.getResource("fxml/journal-view.fxml")
            );
            Stage journalStage = new Stage();
            journalStage.setTitle("Журнал обращений — ЕДДС 112");
            journalStage.setScene(new Scene(loader.load(), 900, 600));
            journalStage.initModality(Modality.NONE);
            journalStage.show();

        } catch (IOException e) {
            showError("Ошибка", "Не удалось открыть журнал: " + e.getMessage());
        }
    }

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    /**
     * Собирает данные из полей формы в объект {@link IncidentReport}.
     *
     * @return сформированное сообщение о происшествии
     */
    private IncidentReport buildReport() {
        List<UrgencyData> selected = checkBoxEntries.stream()
                .filter(e -> e.checkBox().isSelected())
                .map(CheckBoxEntry::urgencyData)
                .toList();

        return new IncidentReport(
                callerNameField.getText().trim(),
                phoneField.getText().trim(),
                addressField.getText().trim(),
                selected
        );
    }

    /**
     * Открывает модальный экран с нарядом-талоном.
     *
     * @param ticket сформированный наряд
     * @param report исходное сообщение
     */
    private void openTicketScreen(DispatchTicket ticket, IncidentReport report) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    EmergencyApplication.class.getResource("fxml/ticket-view.fxml")
            );
            Stage ticketStage = new Stage();
            ticketStage.setTitle("Наряд " + ticket.getOrderNumber());
            ticketStage.setScene(new Scene(loader.load(), 700, 550));
            ticketStage.initModality(Modality.APPLICATION_MODAL);

            TicketController controller = loader.getController();
            controller.initTicket(ticket, report);

            ticketStage.show();

        } catch (IOException e) {
            showError("Ошибка", "Не удалось открыть экран наряда: " + e.getMessage());
        }
    }

    /**
     * Устанавливает текст и стиль строки статуса.
     *
     * @param text    текст статуса
     * @param isError {@code true} — красный текст (ошибка), {@code false} — обычный
     */
    private void setStatus(String text, boolean isError) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll("status-error", "status-ok");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-ok");
    }

    /**
     * Показывает диалоговое окно с ошибкой.
     *
     * @param title   заголовок диалога
     * @param message текст ошибки
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ─── Вспомогательная запись ───────────────────────────────────────────────

    /**
     * Связывает CheckBox с конкретным типом происшествия.
     *
     * @param checkBox   элемент CheckBox в GUI
     * @param urgencyData тип происшествия с баллом
     */
    private record CheckBoxEntry(CheckBox checkBox, UrgencyData urgencyData) {}
}
