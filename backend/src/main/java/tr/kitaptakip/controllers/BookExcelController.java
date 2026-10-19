package tr.kitaptakip.controllers;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tr.kitaptakip.dtos.BookImportResponse;
import tr.kitaptakip.enums.BookCopyStatus;
import tr.kitaptakip.services.BookExcelService;

@RestController
@RequestMapping("/api/v1/books")
public class BookExcelController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final BookExcelService bookExcelService;

    public BookExcelController(BookExcelService bookExcelService) { this.bookExcelService = bookExcelService; }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BookImportResponse importBooks(@RequestParam("file") MultipartFile file) {
        return bookExcelService.importBooks(file);
    }

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportBooks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BookCopyStatus copyStatus,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        StreamingResponseBody stream = output -> bookExcelService.exportBooks(output, q, category, copyStatus, includeArchived);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("kitaplar.xlsx").build().toString())
                .body(stream);
    }
}
