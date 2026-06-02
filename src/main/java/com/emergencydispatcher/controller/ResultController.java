package com.emergencydispatcher.controller;

import com.emergencydispatcher.EmergencyApplication;
import com.emergencydispatcher.model.Appeal;
import com.emergencydispatcher.model.DispatchTicket;
import com.emergencydispatcher.model.IncidentReport;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Контроллер экрана результата SOS-вызова.
 * Отображает визуальные карточки вызванных служб и ETA.
 */
public class ResultController {

    @FXML private VBox  priorityBanner;
    @FXML private Label bannerIcon;
    @FXML private Label bannerTitle;
    @FXML private Label bannerSub;
    @FXML private VBox  servicesBox;
    @FXML private Label etaLabel;
    @FXML private Label orderLabel;
    @FXML private Label addressLabel;
    @FXML private Label tipsLabel;
    @FXML private Button savePdfButton;

    private DispatchTicket currentTicket;
    private IncidentReport currentReport;

    // ─── Инициализация данными ───────────────────────────────────────────────

    public void initResult(DispatchTicket ticket, IncidentReport report) {
        this.currentTicket = ticket;
        this.currentReport = report;

        applyPriorityTheme(ticket.getType());
        buildServiceCards(ticket.getDispatchedServices(), ticket.getType());

        int eta = ticket.getEtaMinutes();
        if (ticket.getType() == Appeal.Priority.GREEN) {
            etaLabel.setText("~" + (eta / 60) + " ч.");
        } else {
            etaLabel.setText(eta + " мин.");
        }

        orderLabel.setText(ticket.getOrderNumber());
        addressLabel.setText(report.getAddress());

        if (ticket.getType() == Appeal.Priority.RED) {
            tipsLabel.setText(
                    "• Оставайтесь на месте, ждите помощи\n" +
                    "• Не пытайтесь тушить крупный пожар самостоятельно\n" +
                    "• Помогите пострадавшим переместиться в безопасное место\n" +
                    "• При ухудшении — повторно вызовите 112"
            );
        } else if (ticket.getType() == Appeal.Priority.YELLOW) {
            tipsLabel.setText(
                    "• Оставайтесь доступны по телефону\n" +
                    "• Не покидайте место происшествия\n" +
                    "• При ухудшении ситуации — повторно вызовите 112"
            );
        }
    }

    private void applyPriorityTheme(Appeal.Priority priority) {
        priorityBanner.getStyleClass().setAll(switch (priority) {
            case RED    -> "result-banner-red";
            case YELLOW -> "result-banner-yellow";
            case GREEN  -> "result-banner-green";
        });

        switch (priority) {
            case RED -> {
                bannerIcon.setText("🚨");
                bannerTitle.setText("ПОМОЩЬ ВЫЗВАНА!");
                bannerSub.setText("Экстренные службы уже едут к вам");
            }
            case YELLOW -> {
                bannerIcon.setText("⚠️");
                bannerTitle.setText("ВЫЗОВ ПРИНЯТ");
                bannerSub.setText("Службы назначены и выедут в ближайшее время");
            }
            case GREEN -> {
                bannerIcon.setText("✅");
                bannerTitle.setText("ОБРАЩЕНИЕ ЗАРЕГИСТРИРОВАНО");
                bannerSub.setText("Патруль прибудет в течение дня");
            }
        }
    }

    private void buildServiceCards(List<String> services, Appeal.Priority priority) {
        servicesBox.getChildren().clear();
        String status = switch (priority) {
            case RED    -> "ВЫЕХАЛИ";
            case YELLOW -> "НАЗНАЧЕНЫ";
            case GREEN  -> "ЗАПЛАНИРОВАНО";
        };

        for (String service : services) {
            HBox card = new HBox(12);
            card.getStyleClass().add("service-card-" + priority.name().toLowerCase());

            Label iconLabel = new Label(service);
            iconLabel.getStyleClass().add("service-card-name");
            iconLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(iconLabel, javafx.scene.layout.Priority.ALWAYS);

            Label statusLabel = new Label(status);
            statusLabel.getStyleClass().add("service-card-status-" + priority.name().toLowerCase());

            card.getChildren().addAll(iconLabel, statusLabel);
            servicesBox.getChildren().add(card);
        }
    }

    // ─── Кнопки ──────────────────────────────────────────────────────────────

    @FXML
    private void onSavePdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Сохранить наряд как PDF");
        fc.setInitialFileName(currentTicket.getOrderNumber() + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF файлы", "*.pdf"));

        File file = fc.showSaveDialog(savePdfButton.getScene().getWindow());
        if (file == null) return;

        try {
            writePdf(file);
            showInfo("PDF сохранён", "Файл сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            showError("Ошибка сохранения PDF", e.getMessage());
        }
    }

    @FXML
    private void onNewCall() {
        try {
            Stage stage = (Stage) savePdfButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    EmergencyApplication.class.getResource("fxml/sos-view.fxml")
            );
            Scene scene = new Scene(loader.load(),
                    stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(
                    EmergencyApplication.class.getResource("css/style.css").toExternalForm()
            );
            stage.setScene(scene);
            stage.setTitle("Экстренный вызов 112");
        } catch (IOException e) {
            showError("Ошибка", e.getMessage());
        }
    }

    // ─── PDF-генерация (адаптирована из TicketController) ────────────────────

    private void writePdf(File file) throws IOException {
        String text = "ЕДДС 112 - ЦИФРОВОЙ ДВОЙНИК ДИСПЕТЧЕРА\n" +
                "Дата: " + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")) + "\n\n" +
                currentTicket.getDetails();

        try (PDDocument doc = new PDDocument()) {
            PDType0Font font = loadCyrillicFont(doc);
            float fontSize = 10f, lineHeight = 14f, margin = 50f;
            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float usableWidth = pageWidth - 2 * margin;
            float startY = pageHeight - margin;

            String[] rawLines = text.split("\n", -1);
            java.util.List<String> lines = new java.util.ArrayList<>();
            for (String raw : rawLines) lines.addAll(wrapLine(raw, font, fontSize, usableWidth));

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(margin, startY);
            cs.setLeading(lineHeight);
            float currentY = startY;

            for (String line : lines) {
                if (currentY - lineHeight < margin) {
                    cs.endText(); cs.close();
                    page = new PDPage(PDRectangle.A4); doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    cs.beginText(); cs.setFont(font, fontSize);
                    cs.newLineAtOffset(margin, startY); cs.setLeading(lineHeight);
                    currentY = startY;
                }
                cs.showText(sanitize(line));
                cs.newLine();
                currentY -= lineHeight;
            }
            cs.endText(); cs.close();
            doc.save(file);
        }
    }

    private PDType0Font loadCyrillicFont(PDDocument doc) throws IOException {
        for (String path : new String[]{
                "C:/Windows/Fonts/arial.ttf", "C:/Windows/Fonts/Arial.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"
        }) {
            File f = new File(path);
            if (f.exists()) return PDType0Font.load(doc, f);
        }
        throw new IOException("Не найден шрифт с поддержкой кириллицы (Arial / DejaVuSans).");
    }

    private java.util.List<String> wrapLine(String line, PDType0Font font,
                                             float size, float maxW) throws IOException {
        java.util.List<String> r = new java.util.ArrayList<>();
        if (line.isBlank()) { r.add(""); return r; }
        String s = sanitize(line);
        if (getWidth(s, font, size) <= maxW) { r.add(s); return r; }
        StringBuilder cur = new StringBuilder();
        for (String w : s.split("(?<=\\s)|(?=\\s)")) {
            if (getWidth(cur + w, font, size) > maxW && !cur.isEmpty()) {
                r.add(cur.toString().stripTrailing());
                cur = new StringBuilder(w.stripLeading());
            } else cur.append(w);
        }
        if (!cur.isEmpty()) r.add(cur.toString());
        return r;
    }

    private float getWidth(String t, PDType0Font f, float size) {
        try { return f.getStringWidth(t) / 1000 * size; }
        catch (Exception e) { return t.length() * size * 0.5f; }
    }

    private String sanitize(String t) {
        if (t == null) return "";
        return t.replace("🔴","[RED]").replace("🟡","[YEL]").replace("🟢","[GRN]")
                .replace("🚒","[101]").replace("🚑","[103]").replace("🚔","[102]")
                .replace("🛡","[ROSGV]").replace("🔧","[AV]").replace("📋","[DATA]")
                .replace("📊","[SCORE]").replace("🚨","[SOS]").replace("⏱","[ETA]")
                .replace("⚠","[!]").replace("✅","[OK]").replace("📅","[DATE]")
                .replace("📞","[TEL]").replace("═","=").replace("─","-")
                .replace(" "," ");
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
