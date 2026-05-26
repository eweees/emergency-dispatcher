package com.emergencydispatcher.controller;

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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Контроллер экрана наряда-талона.
 *
 * <p>Отображает сформированный наряд с цветовым выделением по приоритету.
 * Поддерживает сохранение в TXT и PDF (с кириллицей через PDFBox + встроенный шрифт).
 */
public class TicketController {

    // ─── FXML-элементы ───────────────────────────────────────────────────────

    @FXML private VBox     ticketCard;
    @FXML private Label    priorityLabel;
    @FXML private Label    orderNumberLabel;
    @FXML private TextArea detailsArea;
    @FXML private Button   saveButton;
    @FXML private Button   savePdfButton;
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

        priorityLabel.setText(ticket.getTitle());
        orderNumberLabel.setText("Наряд № " + ticket.getOrderNumber());
        detailsArea.setText(ticket.getDetails());

        ticketCard.getStyleClass().add(ticket.getCssClass());
        priorityLabel.getStyleClass().add("priority-label-" +
                ticket.getType().name().toLowerCase());
    }

    // ─── Обработчики событий ─────────────────────────────────────────────────

    /**
     * Сохраняет наряд-талон в TXT-файл.
     */
    @FXML
    private void onSave() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Сохранить наряд-талон");
        fc.setInitialFileName(buildFileName("txt"));
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt"));

        File file = fc.showSaveDialog(saveButton.getScene().getWindow());
        if (file == null) return;

        try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write(buildTicketText());
            showInfo("Наряд сохранён", "Файл сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            showError("Ошибка сохранения", e.getMessage());
        }
    }

    /**
     * Сохраняет наряд-талон в PDF с поддержкой кириллицы и Unicode-символов.
     *
     * <p>Использует Apache PDFBox + шрифт Arial/DejaVu из ресурсов приложения.
     * Все символы (═══, ───, эмодзи-замены, кириллица) отображаются корректно.
     */
    @FXML
    private void onSavePdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Сохранить наряд как PDF");
        fc.setInitialFileName(buildFileName("pdf"));
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF файлы", "*.pdf"));

        File file = fc.showSaveDialog(savePdfButton.getScene().getWindow());
        if (file == null) return;

        try {
            writePdf(file);
            showInfo("PDF сохранён", "Файл сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            showError("Ошибка сохранения PDF", e.getMessage());
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

    // ─── PDF-генерация ────────────────────────────────────────────────────────

    /**
     * Генерирует PDF через Apache PDFBox с кириллическим шрифтом.
     *
     * <p>Алгоритм:
     * <ol>
     *   <li>Загружает TrueType-шрифт с поддержкой кириллицы из системы</li>
     *   <li>Разбивает текст наряда на строки</li>
     *   <li>Переносит длинные строки по ширине страницы</li>
     *   <li>Добавляет новые страницы при переполнении</li>
     * </ol>
     *
     * @param file файл для сохранения
     * @throws IOException при ошибке записи
     */
    private void writePdf(File file) throws IOException {
        String text = buildTicketText();

        try (PDDocument doc = new PDDocument()) {

            // Загружаем шрифт с поддержкой кириллицы
            PDType0Font font = loadCyrillicFont(doc);

            float fontSize   = 10f;
            float lineHeight = fontSize * 1.4f;
            float margin     = 50f;
            float pageWidth  = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float usableWidth = pageWidth - 2 * margin;
            float startY     = pageHeight - margin;

            // Разбиваем на строки с учётом ширины страницы
            String[] rawLines = text.split("\n", -1);
            java.util.List<String> lines = new java.util.ArrayList<>();
            for (String raw : rawLines) {
                lines.addAll(wrapLine(raw, font, fontSize, usableWidth));
            }

            // Рисуем страницы
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
                    // Новая страница
                    cs.endText();
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.newLineAtOffset(margin, startY);
                    cs.setLeading(lineHeight);
                    currentY = startY;
                }

                // Заменяем эмодзи на текстовые эквиваленты для шрифта
                String safeLine = replaceEmoji(line);
                cs.showText(safeLine);
                cs.newLine();
                currentY -= lineHeight;
            }

            cs.endText();
            cs.close();
            doc.save(file);
        }
    }

    /**
     * Загружает шрифт с поддержкой кириллицы.
     *
     * <p>Порядок поиска:
     * <ol>
     *   <li>Arial из Windows Fonts (C:/Windows/Fonts/arial.ttf)</li>
     *   <li>DejaVu Sans из Linux (/usr/share/fonts/...)</li>
     *   <li>LiberationSans из Linux</li>
     * </ol>
     *
     * @param doc документ PDFBox
     * @return загруженный шрифт
     * @throws IOException если ни один шрифт не найден
     */
    private PDType0Font loadCyrillicFont(PDDocument doc) throws IOException {
        String[] fontPaths = {
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/Arial.ttf",
                "C:/Windows/Fonts/arialbd.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                "/System/Library/Fonts/Supplemental/Arial.ttf"
        };

        for (String path : fontPaths) {
            File f = new File(path);
            if (f.exists()) {
                System.out.println("[PDF] Шрифт: " + path);
                return PDType0Font.load(doc, f);
            }
        }

        // Fallback — встроенный шрифт PDFBox (без кириллицы, но не упадёт)
        throw new IOException(
                "Не найден шрифт с поддержкой кириллицы.\n" +
                "Убедитесь что на компьютере установлен Arial (Windows) или DejaVu Sans (Linux)."
        );
    }

    /**
     * Переносит длинную строку по ширине страницы.
     *
     * @param line       исходная строка
     * @param font       шрифт
     * @param fontSize   размер шрифта
     * @param maxWidth   максимальная ширина в pt
     * @return список строк после переноса
     */
    private java.util.List<String> wrapLine(String line, PDType0Font font,
                                             float fontSize, float maxWidth)
            throws IOException {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (line.isEmpty()) {
            result.add("");
            return result;
        }

        String safe = replaceEmoji(line);
        float lineWidth = getStringWidth(safe, font, fontSize);

        if (lineWidth <= maxWidth) {
            result.add(safe);
            return result;
        }

        // Разбиваем по словам
        StringBuilder current = new StringBuilder();
        for (String word : safe.split("(?<=\\s)|(?=\\s)")) {
            String candidate = current + word;
            if (getStringWidth(candidate, font, fontSize) > maxWidth && current.length() > 0) {
                result.add(current.toString().stripTrailing());
                current = new StringBuilder(word.stripLeading());
            } else {
                current.append(word);
            }
        }
        if (current.length() > 0) result.add(current.toString());
        return result;
    }

    /**
     * Вычисляет ширину строки в pt для данного шрифта и размера.
     */
    private float getStringWidth(String text, PDType0Font font, float fontSize)
            throws IOException {
        try {
            return font.getStringWidth(text) / 1000 * fontSize;
        } catch (Exception e) {
            return text.length() * fontSize * 0.5f; // fallback
        }
    }

    /**
     * Заменяет эмодзи и Unicode-псевдографику на текстовые эквиваленты,
     * которые корректно отображаются в стандартных шрифтах.
     *
     * @param text исходная строка
     * @return строка без эмодзи
     */
    private String replaceEmoji(String text) {
        if (text == null) return "";
        return text
                // Цветные кружки приоритетов
                .replace("🔴", "[КРАСНЫЙ]")
                .replace("🟡", "[ЖЁЛТЫЙ]")
                .replace("🟢", "[ЗЕЛЁНЫЙ]")
                // Службы и значки
                .replace("🚒", "[101]")
                .replace("🚑", "[103]")
                .replace("🚔", "[102]")
                .replace("🛡", "[РОСГВАРДИЯ]")
                .replace("🔧", "[АВ.СЛУЖБА]")
                .replace("📋", "[ДАННЫЕ]")
                .replace("📊", "[ОЦЕНКА]")
                .replace("🚨", "[СЛУЖБЫ]")
                .replace("⏱", "[ETA]")
                .replace("⚠", "[!]")
                .replace("✅", "[OK]")
                .replace("📅", "[ПЛАН]")
                .replace("📞", "[ТЕЛ]")
                // Псевдографика — оставляем ASCII-аналоги
                .replace("═", "=")
                .replace("─", "-")
                // Прочие спецсимволы
                .replace("\u00a0", " "); // неразрывный пробел
    }

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    /**
     * Формирует полный текст наряда для сохранения в файл.
     */
    private String buildTicketText() {
        return "ЕДДС 112 - ЦИФРОВОЙ ДВОЙНИК ДИСПЕТЧЕРА\n" +
               "Дата формирования: " +
               LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")) +
               "\n\n" +
               currentTicket.getDetails();
    }

    /**
     * Формирует имя файла для сохранения.
     *
     * @param ext расширение файла (txt или pdf)
     */
    private String buildFileName(String ext) {
        return currentTicket.getOrderNumber() + "_" +
               LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) +
               "." + ext;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
