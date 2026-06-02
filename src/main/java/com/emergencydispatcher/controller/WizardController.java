package com.emergencydispatcher.controller;

import com.emergencydispatcher.EmergencyApplication;
import com.emergencydispatcher.bridge.MapBridge;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Контроллер мастера ввода SOS-вызова.
 *
 * Шаг 1 — Карта: пользователь кликает на карту или нажимает GPS-кнопку.
 * Шаг 2 — Детали: пострадавшие, телефон, краткое описание.
 */
public class WizardController implements Initializable {

    // ─── FXML ────────────────────────────────────────────────────────────────

    @FXML private Label    categoryLabel;
    @FXML private Label    step1Dot;
    @FXML private Label    step2Dot;

    @FXML private VBox     step1Panel;
    @FXML private VBox     step2Panel;

    @FXML private WebView  mapView;
    @FXML private TextField addressField;

    @FXML private CheckBox  hasVictimsCheck;
    @FXML private HBox      victimsCountBox;
    @FXML private Spinner<Integer> victimsSpinner;
    @FXML private TextField phoneField;
    @FXML private TextArea  descriptionField;
    @FXML private Label     statusLabel;

    @FXML private Button backButton;
    @FXML private Button nextButton;
    @FXML private Button submitButton;

    // ─── Поля ────────────────────────────────────────────────────────────────

    private List<UrgencyData> preSelectedIncidents;
    private String categoryName;
    private int currentStep = 1;
    private final MapBridge mapBridge = new MapBridge();
    private final Pipeline pipeline = new Pipeline();

    // ─── Инициализация ───────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        victimsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1)
        );
        hasVictimsCheck.selectedProperty().addListener((obs, old, selected) -> {
            victimsCountBox.setVisible(selected);
            victimsCountBox.setManaged(selected);
        });

        mapBridge.setOnAddressSelected(addr -> Platform.runLater(() -> {
            addressField.setText(addr);
        }));

        mapBridge.setOnGpsRequested(() -> {
            // GPS через WebView не сработал — показываем подсказку
            addressField.setPromptText("Введите адрес вручную или нажмите на карту");
        });

        initMap();
    }

    public void initWithIncidents(List<UrgencyData> incidents, String catLabel) {
        this.preSelectedIncidents = incidents;
        this.categoryName = catLabel;
        categoryLabel.setText(catLabel);
    }

    // ─── Карта ───────────────────────────────────────────────────────────────

    private void initMap() {
        WebEngine engine = mapView.getEngine();

        // Регистрируем мост ПОСЛЕ загрузки страницы
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("mapBridge", mapBridge);
            }
        });

        engine.loadContent(buildMapHtml());
    }

    @FXML
    private void onLocateMe() {
        mapView.getEngine().executeScript("locateMe()");
    }

    // ─── Навигация шагов ─────────────────────────────────────────────────────

    @FXML
    private void onNext() {
        String addr = addressField.getText().trim();
        if (addr.isBlank()) {
            shake(addressField);
            addressField.setPromptText("Укажите адрес — нажмите на карту или кнопку GPS");
            return;
        }
        showStep(2);
    }

    @FXML
    private void onBack() {
        showStep(1);
    }

    private void showStep(int step) {
        currentStep = step;
        boolean onStep1 = (step == 1);

        step1Panel.setVisible(onStep1);
        step1Panel.setManaged(onStep1);
        step2Panel.setVisible(!onStep1);
        step2Panel.setManaged(!onStep1);

        backButton.setVisible(!onStep1);
        backButton.setManaged(!onStep1);
        nextButton.setVisible(onStep1);
        nextButton.setManaged(onStep1);
        submitButton.setVisible(!onStep1);
        submitButton.setManaged(!onStep1);

        step1Dot.getStyleClass().setAll(onStep1 ? "wizard-step-active" : "wizard-step-done");
        step2Dot.getStyleClass().setAll(onStep1 ? "wizard-step-inactive" : "wizard-step-active");
        step1Dot.setText(onStep1 ? "● Шаг 1: Место" : "✓ Шаг 1: Место");
        step2Dot.setText(onStep1 ? "○ Шаг 2: Детали" : "● Шаг 2: Детали");
    }

    // ─── Отправка ────────────────────────────────────────────────────────────

    @FXML
    private void onSubmit() {
        submitButton.setDisable(true);
        showStatus("⏳ Обрабатываем вызов...", false);

        IncidentReport report = buildReport();

        Thread.ofVirtual().start(() -> {
            try {
                DispatchTicket ticket = pipeline.process(report);
                Platform.runLater(() -> openResultScreen(ticket, report));
            } catch (FilterException fe) {
                Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    showStatus("❌ " + fe.getMessage(), true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    showStatus("❌ Системная ошибка: " + e.getMessage(), true);
                });
            }
        });
    }

    private IncidentReport buildReport() {
        boolean hasVictims = hasVictimsCheck.isSelected();
        int count = hasVictims ? victimsSpinner.getValue() : 0;
        String phone = phoneField.getText().trim();
        String desc  = descriptionField.getText().trim();

        IncidentReport report = new IncidentReport(
                "",
                phone,
                addressField.getText().trim(),
                preSelectedIncidents,
                hasVictims,
                count
        );
        report.setDescription(desc);
        return report;
    }

    private void openResultScreen(DispatchTicket ticket, IncidentReport report) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    EmergencyApplication.class.getResource("fxml/result-view.fxml")
            );
            Stage resultStage = (Stage) submitButton.getScene().getWindow();
            Scene scene = new Scene(loader.load(), 800, 620);
            scene.getStylesheets().add(
                    EmergencyApplication.class.getResource("css/style.css").toExternalForm()
            );

            ResultController ctrl = loader.getController();
            ctrl.initResult(ticket, report);

            resultStage.setScene(scene);
            resultStage.setTitle("Помощь вызвана! — " + ticket.getOrderNumber());
        } catch (IOException e) {
            showStatus("❌ Не удалось открыть экран результата: " + e.getMessage(), true);
            submitButton.setDisable(false);
        }
    }

    // ─── Вспомогательные ─────────────────────────────────────────────────────

    private void showStatus(String text, boolean isError) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().setAll(
                "status-label", isError ? "status-error" : "status-ok"
        );
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void shake(TextField field) {
        field.setStyle("-fx-border-color: #e53935; -fx-border-width: 2;");
        field.textProperty().addListener((o, oldV, newV) ->
                field.setStyle("")
        );
    }

    // ─── HTML карты ──────────────────────────────────────────────────────────

    private String buildMapHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <link rel="stylesheet"
                        href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                  <style>
                    body { margin:0; padding:0; }
                    #map { width:100%; height:100vh; }
                    #locateBtn {
                      position:absolute; top:10px; right:10px; z-index:1000;
                      background:#e53935; color:white; border:none;
                      padding:10px 16px; border-radius:4px; font-size:14px;
                      cursor:pointer; font-family:sans-serif; font-weight:bold;
                      box-shadow:0 2px 6px rgba(0,0,0,0.3);
                    }
                    #locateBtn:hover { background:#b71c1c; }
                    #hint {
                      position:absolute; bottom:4px; left:50%; transform:translateX(-50%);
                      background:rgba(0,0,0,0.6); color:white; padding:4px 12px;
                      border-radius:12px; font-size:12px; z-index:1000;
                      font-family:sans-serif; pointer-events:none;
                    }
                  </style>
                </head>
                <body>
                  <div id="map"></div>
                  <button id="locateBtn" onclick="locateMe()">📍 Моё место</button>
                  <div id="hint">Нажмите на карту чтобы отметить место</div>
                  <script>
                    var map = L.map('map').setView([55.7558, 37.6173], 12);
                    var marker = null;

                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                      attribution: '© OpenStreetMap', maxZoom: 19
                    }).addTo(map);

                    var redIcon = L.divIcon({
                      className:'',
                      html:'<div style="width:20px;height:20px;background:#e53935;border-radius:50%;border:3px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.5)"></div>',
                      iconSize:[20,20], iconAnchor:[10,10]
                    });

                    map.on('click', function(e) { setLocation(e.latlng.lat, e.latlng.lng); });

                    function locateMe() {
                      document.getElementById('hint').textContent = 'Определяем местоположение...';
                      if (navigator.geolocation) {
                        navigator.geolocation.getCurrentPosition(
                          function(p) {
                            setLocation(p.coords.latitude, p.coords.longitude);
                            map.setView([p.coords.latitude, p.coords.longitude], 17);
                          },
                          function() {
                            document.getElementById('hint').textContent = 'GPS недоступен — нажмите на карту';
                            if (window.mapBridge) window.mapBridge.requestGpsLocation();
                          },
                          { timeout:8000, maximumAge:30000, enableHighAccuracy:true }
                        );
                      } else {
                        if (window.mapBridge) window.mapBridge.requestGpsLocation();
                      }
                    }

                    function setLocation(lat, lng) {
                      if (marker) { marker.setLatLng([lat, lng]); }
                      else {
                        marker = L.marker([lat, lng], {icon:redIcon, draggable:true}).addTo(map);
                        marker.on('dragend', function(e) {
                          var p = e.target.getLatLng();
                          reverseGeocode(p.lat, p.lng);
                        });
                      }
                      reverseGeocode(lat, lng);
                    }

                    function reverseGeocode(lat, lng) {
                      document.getElementById('hint').textContent = 'Определяем адрес...';
                      fetch('https://nominatim.openstreetmap.org/reverse?lat=' + lat + '&lon=' + lng
                            + '&format=json&accept-language=ru',
                            { headers: { 'User-Agent': 'EmergencyDispatcher/1.0' } })
                        .then(r => r.json())
                        .then(d => {
                          var addr = d.display_name || (lat.toFixed(5) + ', ' + lng.toFixed(5));
                          document.getElementById('hint').textContent = '✓ Адрес определён';
                          if (window.mapBridge) window.mapBridge.onLocationSelected(lat, lng, addr);
                        })
                        .catch(() => {
                          var coords = lat.toFixed(5) + ', ' + lng.toFixed(5);
                          document.getElementById('hint').textContent = '✓ Координаты: ' + coords;
                          if (window.mapBridge) window.mapBridge.onLocationSelected(lat, lng, coords);
                        });
                    }

                    function setLocationFromJava(lat, lng) {
                      setLocation(lat, lng);
                      map.setView([lat, lng], 17);
                    }
                  </script>
                </body>
                </html>
                """;
    }
}
