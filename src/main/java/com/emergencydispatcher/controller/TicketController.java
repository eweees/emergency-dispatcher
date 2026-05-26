package com.emergencydispatcher.controller;

import com.emergencydispatcher.model.Appeal;
import com.emergencydispatcher.model.DispatchTicket;
import com.emergencydispatcher.model.IncidentReport;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Контроллер экрана наряда-талона.
 *
 * <p>Отображает сформированный наряд с цветовым выделением по приоритету:
 * <ul>
 *   <li>🔴 RED — красный фон карточки</li>
 *   <li>🟡 YELLOW — жёлтый фон карточки</li>
 *   <li>🟢 GREEN — зелёный фон карточки</li>
 * </ul>
 *
 * <p>Позволяет сохранить наряд в TXT-файл.
 */
public class TicketController {

    // ─── FXML-элементы ───────────────────────────────────────────────────────

    @FXML private VBox     ticketCard;
    @FXML private Label    priorityLabel;
    @FXML private Label    orderNumberLabel;
    @FXML private TextArea detailsArea;
    @FXML private Button   saveButton;
    @FXML private Button   closeButton;

    // ─── Поля контроллера ────────────────────────────────────────────────────

    private DispatchTicket currentTicket;
    private IncidentReport currentReport;

    // ─── Инициализация данными ────────────────────────────────────────────────

    /**
     * Инициализирует экран данными наряда.
     * Вызывается из {@link MainController} после загрузки FXML.
     *
     * @param ticket сформированный наряд
     * @param report исходное сообщение о происшествии
     */
    public void initTicket(DispatchTicket ticket, IncidentReport report) {
        this.currentTicket = ticket;
        this.currentReport = report;

        // Устанавливаем данные
        priorityLabel.setText(ticket.getTitle());
        orderNumberLabel.setText("Наряд № " + ticket.getOrderNumber());
        detailsArea.setText(ticket.getDetails());

        // Применяем CSS-стиль карточки по приоритету
        ticketCard.getStyleClass().add(ticket.getCssClass());

        // Стиль метки приоритета
        priorityLabel.getStyleClass().add("priority-label-" +
                ticket.getType().name().toLowerCase());
    }

    // ─── Обработчики событий ─────────────────────────────────────────────────

    /**
     * Сохраняет наряд-талон в TXT-файл.
     * Открывает диалог выбора пути сохранения.
     */
    @FXML
    private void onSave() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить наряд-талон");
        fileChooser.setInitialFileName(
                currentTicket.getOrderNumber() + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) +
                ".txt"
        );
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt")
        );

        Stage stage = (Stage) saveButton.getScene().getWindow();
        java.io.File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(buildTicketText());
                showInfo("Наряд сохранён", "Файл сохранён:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showError("Ошибка сохранения", e.getMessage());
            }
        }
    }

    /**
     * Закрывает окно наряда.
     */
    @FXML
    private void onClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    /**
     * Формирует полный текст наряда для сохранения в TXT.
     *
     * @return текст наряда
     */
    private String buildTicketText() {
        return "ЕДДС 112 — ЦИФРОВОЙ ДВОЙНИК ДИСПЕТЧЕРА\n" +
               "Дата формирования: " + LocalDateTime.now()
                       .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")) + "\n\n" +
               currentTicket.getDetails();
    }

    /**
     * Показывает информационный диалог.
     *
     * @param title   заголовок
     * @param message текст
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Показывает диалог с ошибкой.
     *
     * @param title   заголовок
     * @param message текст
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
