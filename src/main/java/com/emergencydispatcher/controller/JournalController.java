package com.emergencydispatcher.controller;

import com.emergencydispatcher.db.DatabaseManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Контроллер экрана журнала обращений и статистики.
 *
 * <p>Функциональность:
 * <ul>
 *   <li>Таблица последних 100 обращений</li>
 *   <li>Фильтрация по периоду (DatePicker)</li>
 *   <li>Статистика по приоритетам с процентами</li>
 * </ul>
 */
public class JournalController implements Initializable {

    // ─── FXML-элементы ───────────────────────────────────────────────────────

    @FXML private TableView<String[]> journalTable;
    @FXML private TableColumn<String[], String> colDate;
    @FXML private TableColumn<String[], String> colPriority;
    @FXML private TableColumn<String[], String> colCaller;
    @FXML private TableColumn<String[], String> colPhone;
    @FXML private TableColumn<String[], String> colAddress;
    @FXML private TableColumn<String[], String> colScore;
    @FXML private TableColumn<String[], String> colOrder;

    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;

    @FXML private Label statsTotalLabel;
    @FXML private Label statsRedLabel;
    @FXML private Label statsYellowLabel;
    @FXML private Label statsGreenLabel;

    @FXML private Button refreshButton;

    // ─── Инициализация ────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        loadJournal();
        loadStats(null, null);
    }

    /**
     * Настраивает колонки таблицы журнала.
     */
    private void setupTable() {
        colDate.setCellValueFactory(    cell -> new SimpleStringProperty(cell.getValue()[0]));
        colPriority.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue()[1]));
        colCaller.setCellValueFactory(  cell -> new SimpleStringProperty(cell.getValue()[2]));
        colPhone.setCellValueFactory(   cell -> new SimpleStringProperty(cell.getValue()[3]));
        colAddress.setCellValueFactory( cell -> new SimpleStringProperty(cell.getValue()[4]));
        colScore.setCellValueFactory(   cell -> new SimpleStringProperty(cell.getValue()[5]));
        colOrder.setCellValueFactory(   cell -> new SimpleStringProperty(cell.getValue()[6]));

        // Цветовое выделение строк по приоритету
        journalTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(String[] item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-red", "row-yellow", "row-green");
                if (!empty && item != null && item[1] != null) {
                    switch (item[1]) {
                        case "RED"    -> getStyleClass().add("row-red");
                        case "YELLOW" -> getStyleClass().add("row-yellow");
                        case "GREEN"  -> getStyleClass().add("row-green");
                    }
                }
            }
        });
    }

    // ─── Загрузка данных ──────────────────────────────────────────────────────

    /**
     * Загружает последние 100 записей журнала.
     */
    private void loadJournal() {
        List<String[]> entries = DatabaseManager.getInstance().getJournalEntries(100);
        journalTable.setItems(FXCollections.observableArrayList(entries));
    }

    /**
     * Загружает статистику по приоритетам за указанный период.
     *
     * @param from начало периода или {@code null}
     * @param to   конец периода или {@code null}
     */
    private void loadStats(LocalDate from, LocalDate to) {
        Map<String, Integer> stats = DatabaseManager.getInstance().getStatsByPeriod(from, to);
        int total = DatabaseManager.getInstance().getTotalAppealsCount();

        int red    = stats.getOrDefault("RED",    0);
        int yellow = stats.getOrDefault("YELLOW", 0);
        int green  = stats.getOrDefault("GREEN",  0);
        int periodTotal = red + yellow + green;

        statsTotalLabel.setText("Всего обращений: " + total);

        statsRedLabel.setText(String.format("🔴 RED: %d (%.1f%%)",
                red, periodTotal > 0 ? 100.0 * red / periodTotal : 0));
        statsYellowLabel.setText(String.format("🟡 YELLOW: %d (%.1f%%)",
                yellow, periodTotal > 0 ? 100.0 * yellow / periodTotal : 0));
        statsGreenLabel.setText(String.format("🟢 GREEN: %d (%.1f%%)",
                green, periodTotal > 0 ? 100.0 * green / periodTotal : 0));
    }

    // ─── Обработчики событий ─────────────────────────────────────────────────

    /**
     * Обновляет журнал и статистику по выбранному периоду.
     */
    @FXML
    private void onRefresh() {
        LocalDate from = dateFrom.getValue();
        LocalDate to   = dateTo.getValue();
        loadJournal();
        loadStats(from, to);
    }

    /**
     * Сбрасывает фильтр периода и обновляет данные.
     */
    @FXML
    private void onResetFilter() {
        dateFrom.setValue(null);
        dateTo.setValue(null);
        loadJournal();
        loadStats(null, null);
    }
}
