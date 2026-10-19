package tr.kitaptakip.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tr.kitaptakip.dtos.BookImportResponse;
import tr.kitaptakip.dtos.BookImportRowError;
import tr.kitaptakip.dtos.BookRequest;
import tr.kitaptakip.entities.Book;
import tr.kitaptakip.enums.BookCopyStatus;
import tr.kitaptakip.exceptions.BusinessRuleException;
import tr.kitaptakip.repositories.BookRepository;
import tr.kitaptakip.specifications.BookSpecifications;

@Service
public class BookExcelService {
    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    public static final int MAX_DATA_ROWS = 1000;
    private static final int EXPORT_PAGE_SIZE = 500;
    private static final List<String> HEADERS = List.of(
            "Başlık", "Yazar", "ISBN", "Yayınevi", "Yayın Yılı", "Kategori", "Açıklama");

    private final BookService bookService;
    private final BookRepository bookRepository;
    private final Validator validator;

    public BookExcelService(BookService bookService, BookRepository bookRepository, Validator validator) {
        this.bookService = bookService;
        this.bookRepository = bookRepository;
        this.validator = validator;
    }

    public BookImportResponse importBooks(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessRuleException("Excel dosyası boş olamaz.");
        if (file.getSize() > MAX_FILE_BYTES) throw new BusinessRuleException("Excel dosyası en fazla 5 MB olabilir.");
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessRuleException("Yalnızca .xlsx biçimindeki Excel dosyaları kabul edilir.");
        }

        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            if (!(workbook instanceof XSSFWorkbook)) {
                throw new BusinessRuleException("Dosya geçerli bir .xlsx çalışma kitabı değil.");
            }
            if (workbook.getNumberOfSheets() == 0) throw new BusinessRuleException("Excel dosyasında sayfa bulunamadı.");
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > MAX_DATA_ROWS) {
                throw new BusinessRuleException("Excel dosyası başlık hariç en fazla 1000 satır içerebilir.");
            }
            validateHeaders(sheet.getRow(sheet.getFirstRowNum()));
            return importRows(sheet);
        } catch (BusinessRuleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessRuleException("Excel dosyası okunamadı. .xlsx biçimini ve dosya içeriğini kontrol edin.");
        }
    }

    @Transactional(readOnly = true)
    public void exportBooks(OutputStream output, String query, String category,
                            BookCopyStatus copyStatus, boolean includeArchived) throws IOException {
        Specification<Book> specification = BookSpecifications.search(query, category, copyStatus, includeArchived);
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            workbook.setCompressTempFiles(true);
            var sheet = workbook.createSheet("Kitaplar");
            writeHeaders(sheet.createRow(0));
            int rowIndex = 1;
            int pageIndex = 0;
            Page<Book> page;
            do {
                page = bookRepository.findAll(specification, PageRequest.of(pageIndex++, EXPORT_PAGE_SIZE,
                        Sort.by(Sort.Direction.ASC, "id")));
                for (Book book : page.getContent()) writeBookRow(sheet.createRow(rowIndex++), book);
            } while (page.hasNext());
            workbook.write(output);
            workbook.dispose();
        }
    }

    private BookImportResponse importRows(Sheet sheet) {
        DataFormatter formatter = new DataFormatter(java.util.Locale.forLanguageTag("tr-TR"));
        List<BookImportRowError> errors = new ArrayList<>();
        int total = 0;
        int imported = 0;
        for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
            Row row = sheet.getRow(index);
            if (isBlank(row, formatter)) continue;
            total++;
            try {
                BookRequest request = readRequest(row, formatter);
                Set<ConstraintViolation<BookRequest>> violations = validator.validate(request);
                if (!violations.isEmpty()) {
                    String message = violations.stream()
                            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                            .sorted().collect(Collectors.joining("; "));
                    throw new BusinessRuleException(message);
                }
                // BookService transaction sınırı her satırı diğerlerinden bağımsız kaydeder.
                bookService.create(request);
                imported++;
            } catch (RuntimeException exception) {
                errors.add(new BookImportRowError(index + 1, safeRowError(exception)));
            }
        }
        return new BookImportResponse(total, imported, total - imported, List.copyOf(errors));
    }

    private BookRequest readRequest(Row row, DataFormatter formatter) {
        String title = text(row.getCell(0), formatter, true);
        String author = text(row.getCell(1), formatter, true);
        String isbn = text(row.getCell(2), formatter, true);
        String publisher = text(row.getCell(3), formatter, true);
        Integer year = year(row.getCell(4), formatter);
        String category = text(row.getCell(5), formatter, true);
        String description = text(row.getCell(6), formatter, true);
        return new BookRequest(title, author, isbn, publisher, year, category, description);
    }

    private String text(Cell cell, DataFormatter formatter, boolean requiredTextType) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.FORMULA) throw new BusinessRuleException("Formül içeren hücre kullanılamaz.");
        if (requiredTextType && cell.getCellType() != CellType.STRING) {
            throw new BusinessRuleException("Metin alanları Excel'de metin olarak girilmelidir.");
        }
        if (cell.getCellType() != CellType.STRING && cell.getCellType() != CellType.NUMERIC) {
            throw new BusinessRuleException("Hücre türü geçersiz.");
        }
        String value = formatter.formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }

    private Integer year(Cell cell, DataFormatter formatter) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.FORMULA || cell.getCellType() == CellType.BOOLEAN) {
            throw new BusinessRuleException("Yayın yılı tam sayı olmalıdır.");
        }
        String value = formatter.formatCellValue(cell).trim();
        if (value.isEmpty()) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                double numeric = cell.getNumericCellValue();
                if (numeric != Math.rint(numeric) || numeric < Integer.MIN_VALUE || numeric > Integer.MAX_VALUE) {
                    throw new NumberFormatException();
                }
                return (int) numeric;
            }
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessRuleException("Yayın yılı tam sayı olmalıdır.");
        }
    }

    private boolean isBlank(Row row, DataFormatter formatter) {
        if (row == null) return true;
        for (int i = 0; i < HEADERS.size(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK && !formatter.formatCellValue(cell).isBlank()) return false;
        }
        return true;
    }

    private void validateHeaders(Row header) {
        if (header == null) throw new BusinessRuleException("Excel dosyasının ilk satırında kolon başlıkları bulunmalıdır.");
        DataFormatter formatter = new DataFormatter(java.util.Locale.forLanguageTag("tr-TR"));
        for (int i = 0; i < HEADERS.size(); i++) {
            Cell cell = header.getCell(i);
            String value = cell == null ? "" : formatter.formatCellValue(cell).trim();
            if (!HEADERS.get(i).equals(value)) {
                throw new BusinessRuleException("Excel kolonları beklenen başlık ve sırada olmalıdır: "
                        + String.join(", ", HEADERS));
            }
        }
        for (int i = HEADERS.size(); i < header.getLastCellNum(); i++) {
            Cell cell = header.getCell(i);
            if (cell != null && !formatter.formatCellValue(cell).isBlank()) {
                throw new BusinessRuleException("Excel dosyasında beklenmeyen ek kolon bulunuyor.");
            }
        }
    }

    private String safeRowError(RuntimeException exception) {
        if (exception instanceof BusinessRuleException || exception instanceof tr.kitaptakip.exceptions.ConflictException) {
            return exception.getMessage();
        }
        return "Satır kaydedilemedi. Alan değerlerini ve benzersizlik kurallarını kontrol edin.";
    }

    private void writeHeaders(Row row) {
        for (int i = 0; i < HEADERS.size(); i++) row.createCell(i).setCellValue(HEADERS.get(i));
    }

    private void writeBookRow(Row row, Book book) {
        row.createCell(0).setCellValue(book.getTitle());
        row.createCell(1).setCellValue(book.getAuthor());
        row.createCell(2).setCellValue(book.getIsbn() == null ? "" : book.getIsbn());
        row.createCell(3).setCellValue(book.getPublisher() == null ? "" : book.getPublisher());
        if (book.getPublicationYear() != null) row.createCell(4).setCellValue(book.getPublicationYear());
        else row.createCell(4).setCellValue("");
        row.createCell(5).setCellValue(book.getCategory() == null ? "" : book.getCategory());
        row.createCell(6).setCellValue(book.getDescription() == null ? "" : book.getDescription());
    }
}
