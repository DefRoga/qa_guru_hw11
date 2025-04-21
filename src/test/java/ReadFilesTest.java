import com.codeborne.pdftest.PDF;
import com.codeborne.xlstest.XLS;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReadFilesTest {

    // Класс загрузки ресурсов
    private final ClassLoader cl = ReadFilesTest.class.getClassLoader();

    // Тестовый метод для парсинга файлов в архиве .zip
    @Test
    void zipParsingTest() throws Exception {
        // Открываем zip-архив из ресурсов
        try (InputStream is = cl.getResourceAsStream("files/random_test_data.zip");
             ZipInputStream zis = new ZipInputStream(is)) {

            ZipEntry entry;

            // Проходим по каждому файлу в архиве
            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName(); // Получаем имя файла в архиве
                System.out.println("Файл в архиве: " + fileName);

                byte[] fileBytes = zis.readAllBytes(); // Читаем содержимое файла
                try (InputStream fileStream = new ByteArrayInputStream(fileBytes)) { // Создаем InputStream для файла
                    // В зависимости от расширения файла, выполняем соответствующую валидацию
                    if (fileName.endsWith(".pdf")) {
                        validatePdf(fileStream); // Валидация PDF
                    } else if (fileName.endsWith(".csv")) {
                        validateCsv(fileStream); // Валидация CSV
                    } else if (fileName.endsWith(".xlsx")) {
                        validateXlsx(fileStream); // Валидация XLSX
                    } else {
                        System.out.println("Неизвестный тип файла: " + fileName); // Если тип файла неизвестен
                    }
                }
            }
        }
    }

    // Валидация содержимого PDF
    private void validatePdf(InputStream stream) throws IOException {
        PDF pdf = new PDF(stream); // Загружаем PDF из InputStream
        System.out.println("Проверка PDF...");

        // Проверяем, содержит ли текст "Column_1"
        if (pdf.text.contains("Column_1")) {
            System.out.println("Проверка пройдена успешно"); // Если содержится, выводим успех
        } else {
            System.out.println("PDF не содержит ожидаемого текста 'Column_1'"); // Если не содержится, выводим ошибку
        }
    }

    // Валидация содержимого CSV
    private void validateCsv(InputStream stream) throws Exception {
        try (CSVReader reader = new CSVReader(new InputStreamReader(stream))) {
            List<String[]> rows = reader.readAll(); // Читаем все строки CSV в список
            System.out.println("Проверка CSV...");
            // Проверяем первый заголовок
            assertEquals("id", rows.get(0)[0], "Первый заголовок в CSV не 'id'");
            // Проверяем количество строк данных
            assertEquals(100, rows.size() - 1, "CSV не содержит 100 строк данных");
        }
    }

    // Валидация содержимого XLSX
    private void validateXlsx(InputStream stream) throws Exception {
        XLS xls = new XLS(stream); // Загружаем XLSX из InputStream
        String firstCell = xls.excel.getSheetAt(0)
                .getRow(0).getCell(0).getStringCellValue(); // Получаем значение первого столбца первой строки
        System.out.println("Проверка XLSX...");
        // Проверяем, что первый столбец содержит "id"
        assertEquals("id", firstCell, "Первый заголовок в XLSX не 'id'");
        int rowCount = xls.excel.getSheetAt(0).getPhysicalNumberOfRows() - 1; // Получаем количество строк данных
        // Проверяем, что в файле 100 строк данных
        assertEquals(100, rowCount, "XLSX не содержит 100 строк данных");
    }

    // Валидация содержимого JSON
    private void validateJson(InputStream stream) throws Exception {
        ObjectMapper mapper = new ObjectMapper(); // Создаем парсер Jackson
        JsonNode root = mapper.readTree(stream); // Парсим корневой объект JSON
        System.out.println("Проверка JSON...");

        // Проверка названия библиотеки
        assertEquals("Центральная городская библиотека", root.get("libraryName").asText(),
                "Название библиотеки не совпадает");

        // Проверка массива книг
        JsonNode books = root.get("books");
        assertTrue(books.isArray(), "Поле 'books' должно быть массивом");
        assertEquals(3, books.size(), "Ожидалось 3 книги в библиотеке");

        // Проверка, что есть книга "Мастер и Маргарита" и правильный автор
        boolean hasMaster = false;
        for (JsonNode book : books) {
            if ("Мастер и Маргарита".equals(book.get("title").asText())) {
                hasMaster = true;
                assertEquals("Михаил Булгаков", book.get("author").asText(), "Автор не совпадает");
            }
        }

        // Проверяем, что нужная книга действительно присутствует
        assertTrue(hasMaster, "Книга 'Мастер и Маргарита' не найдена");
    }
}
