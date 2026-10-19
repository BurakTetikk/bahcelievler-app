package tr.kitaptakip.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.UUID;

import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DashboardAndExcelIntegrationTest {
    private static final String[] HEADERS = {"Başlık", "Yazar", "ISBN", "Yayınevi", "Yayın Yılı", "Kategori", "Açıklama"};

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6-alpine");

    @Autowired MockMvc mockMvc;

    @Test
    void importsRowsIndependentlyReportsErrorsAndExportsMetadataAsXlsx() throws Exception {
        String unique = UUID.randomUUID().toString();
        String title = "Excel import " + unique;
        MockMultipartFile file = new MockMultipartFile("file", "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", importWorkbook(title));
        MvcResult imported = mockMvc.perform(multipart("/api/v1/books/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(3))
                .andExpect(jsonPath("$.importedRows").value(1))
                .andExpect(jsonPath("$.failedRows").value(2))
                .andExpect(jsonPath("$.errors.length()").value(2)).andReturn();
        assertThat(imported.getResponse().getContentAsString()).contains("2").contains("4");

        MvcResult listed = mockMvc.perform(get("/api/v1/books").param("q", title))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andReturn();
        String bookId = json(listed, "$.content[0].id");
        mockMvc.perform(get("/api/v1/books/{id}/copies", bookId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));

        MvcResult async = mockMvc.perform(get("/api/v1/books/export").param("q", title))
                .andExpect(request().asyncStarted()).andReturn();
        MvcResult exported = mockMvc.perform(asyncDispatch(async))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();
        assertThat(exported.getResponse().getContentAsByteArray()).isNotEmpty();
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(exported.getResponse().getContentAsByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            for (int i = 0; i < HEADERS.length; i++) assertThat(sheet.getRow(0).getCell(i).getStringCellValue()).isEqualTo(HEADERS[i]);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo(title);
        }
    }

    @Test
    void rejectsWrongExtensionMalformedWorkbookAndUnexpectedHeaders() throws Exception {
        MockMultipartFile wrongExtension = new MockMultipartFile("file", "books.xls", "application/octet-stream", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/v1/books/import").file(wrongExtension)).andExpect(status().isBadRequest());

        MockMultipartFile malformed = new MockMultipartFile("file", "books.xlsx", "application/octet-stream", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/v1/books/import").file(malformed)).andExpect(status().isBadRequest());

        MockMultipartFile badHeaders = new MockMultipartFile("file", "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbookWithBadHeaders());
        mockMvc.perform(multipart("/api/v1/books/import").file(badHeaders)).andExpect(status().isBadRequest());
    }

    @Test
    void dashboardCountsPersistedBooksCopiesAndOverdueLoans() throws Exception {
        MvcResult before = mockMvc.perform(get("/api/v1/dashboard")).andExpect(status().isOk()).andReturn();
        String unique = UUID.randomUUID().toString();
        MvcResult book = mockMvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dashboard " + unique + "\",\"author\":\"Author\"}"))
                .andExpect(status().isCreated()).andReturn();
        String bookId = json(book, "$.id");
        MvcResult copy = mockMvc.perform(post("/api/v1/books/{id}/copies", bookId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inventoryCode\":\"DASH-" + unique + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        String copyId = json(copy, "$.id");
        MvcResult borrower = mockMvc.perform(post("/api/v1/borrowers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Dash\",\"lastName\":\"Reader" + unique + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        String borrowerId = json(borrower, "$.id");
        LocalDate today = LocalDate.now(java.time.Clock.system(java.time.ZoneId.of("Europe/Istanbul")));
        mockMvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"copyId\":" + copyId + ",\"borrowerId\":" + borrowerId
                                + ",\"loanDate\":\"" + today.minusDays(3) + "\",\"dueDate\":\"" + today.minusDays(1) + "\"}"))
                .andExpect(status().isCreated());

        MvcResult after = mockMvc.perform(get("/api/v1/dashboard")).andExpect(status().isOk())
                .andExpect(jsonPath("$.recentLoans.length()").isNumber())
                .andExpect(jsonPath("$.overdueLoans.length()").isNumber()).andReturn();
        assertThat(number(after, "$.activeBookCount")).isEqualTo(number(before, "$.activeBookCount") + 1);
        assertThat(number(after, "$.totalBookCount")).isEqualTo(number(before, "$.totalBookCount") + 1);
        assertThat(number(after, "$.totalCopyCount")).isEqualTo(number(before, "$.totalCopyCount") + 1);
        assertThat(number(after, "$.availableCopyCount")).isEqualTo(number(before, "$.availableCopyCount"));
        assertThat(number(after, "$.onLoanCopyCount")).isEqualTo(number(before, "$.onLoanCopyCount") + 1);
        assertThat(number(after, "$.activeLoanCount")).isEqualTo(number(before, "$.activeLoanCount") + 1);
        assertThat(number(after, "$.overdueLoanCount")).isEqualTo(number(before, "$.overdueLoanCount") + 1);
    }

    private byte[] importWorkbook(String title) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Kitaplar");
            addRow(sheet, 0, HEADERS);
            addRow(sheet, 1, new String[]{title, "Excel Author", "978-1234567890", "Yayınevi", "2020", "Roman", "İçe aktarıldı"});
            addRow(sheet, 2, new String[]{"Duplicate", "Other Author", "978 1234567890", "", "2021", "Roman", ""});
            addRow(sheet, 3, new String[]{" ", "Missing title", "", "", "", "", ""});
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private byte[] workbookWithBadHeaders() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Kitaplar");
            addRow(sheet, 0, new String[]{"Title", "Author", "ISBN", "Publisher", "Year", "Category", "Description"});
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void addRow(org.apache.poi.ss.usermodel.Sheet sheet, int rowIndex, String[] values) {
        var row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);
    }

    private String json(MvcResult result, String expression) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), expression).toString();
    }

    private long number(MvcResult result, String expression) throws Exception {
        return ((Number) com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), expression)).longValue();
    }
}
