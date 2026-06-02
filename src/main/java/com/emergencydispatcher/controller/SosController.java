package com.emergencydispatcher.controller;

import com.emergencydispatcher.EmergencyApplication;
import com.emergencydispatcher.config.UrgencyScaleConfig;
import com.emergencydispatcher.model.UrgencyData;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * Контроллер главного SOS-экрана.
 * Отображает 6 крупных кнопок категорий проблем для быстрого выбора.
 * При нажатии открывает мастер ввода с предвыбранным инцидентом.
 */
public class SosController {

    @FXML
    private void onFire() {
        openWizard(List.of(findIncident("Пожар с угрозой жизни людей")), "🔥 Пожар");
    }

    @FXML
    private void onCrash() {
        openWizard(List.of(findIncident("ДТП с пострадавшими / жертвами")), "🚗 ДТП");
    }

    @FXML
    private void onViolence() {
        openWizard(List.of(findIncident("Вооружённое нападение / стрельба")), "🔫 Нападение");
    }

    @FXML
    private void onMedical() {
        openWizard(List.of(findIncident("Требуется медицинская помощь")), "🚑 Медицина");
    }

    @FXML
    private void onUtility() {
        openWizard(List.of(
                findIncident("Утечка газа / взрыв"),
                findIncident("Затопление / авария коммунальных сетей")
        ), "💧 Авария");
    }

    @FXML
    private void onOther() {
        openWizard(List.of(findIncident("Мелкое хулиганство / дебош")), "⚡ Другое");
    }

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
            showError("Не удалось открыть журнал: " + e.getMessage());
        }
    }

    @FXML
    private void onDispatcherMode() {
        EmergencyApplication.showDispatcherScreen();
    }

    // ─── Вспомогательные методы ─────────────────────────────────────────────

    private void openWizard(List<UrgencyData> incidents, String categoryLabel) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    EmergencyApplication.class.getResource("fxml/wizard-view.fxml")
            );
            Stage wizardStage = new Stage();
            wizardStage.setTitle("Вызов помощи — " + categoryLabel);
            Scene scene = new Scene(loader.load(), 920, 680);
            scene.getStylesheets().add(
                    EmergencyApplication.class.getResource("css/style.css").toExternalForm()
            );
            wizardStage.setScene(scene);
            wizardStage.initModality(Modality.APPLICATION_MODAL);

            WizardController ctrl = loader.getController();
            ctrl.initWithIncidents(incidents, categoryLabel);

            wizardStage.show();
        } catch (IOException e) {
            showError("Не удалось открыть мастер вызова: " + e.getMessage());
        }
    }

    private UrgencyData findIncident(String name) {
        return UrgencyScaleConfig.getAllIncidentTypes().stream()
                .filter(ud -> ud.getName().equals(name))
                .findFirst()
                .orElse(UrgencyScaleConfig.getAllIncidentTypes().get(0));
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
