package tr.kitaptakip.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BookApiIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6-alpine");

    @Autowired MockMvc mockMvc;

    @Test
    void createsAndUpdatesBookAndRejectsDuplicateNormalizedIsbn() throws Exception {
        String isbn = "978-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String payload = bookJson("API Book " + UUID.randomUUID(), isbn, "Roman");
        MvcResult created = mockMvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        String id = json(created, "$.id");

        mockMvc.perform(get("/api/v1/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(isbn.replace("-", "").toUpperCase()));
        mockMvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("Duplicate " + UUID.randomUUID(), isbn.replace("-", " "), "Roman")))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/v1/books/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("Updated " + UUID.randomUUID(), isbn, "Tarih")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Tarih"));
    }

    @Test
    void filtersAndPaginatesBooksAndArchivesWithoutDeletingHistory() throws Exception {
        String category = "kategori-" + UUID.randomUUID();
        String id = createBook("Filter target " + UUID.randomUUID(), null, category);
        createBook("Other title " + UUID.randomUUID(), null, "başka-kategori");
        mockMvc.perform(get("/api/v1/books").param("q", "filter target").param("category", category)
                        .param("page", "0").param("size", "1").param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(delete("/api/v1/books/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/books").param("q", "filter target").param("category", category))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/books/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(get("/api/v1/books").param("includeArchived", "true").param("q", "filter target"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createsListsFiltersAndChangesPhysicalCopyStatus() throws Exception {
        String bookId = createBook("Copy owner " + UUID.randomUUID(), null, "Kopyalar");
        String code = "INV-" + UUID.randomUUID();
        MvcResult created = mockMvc.perform(post("/api/v1/books/{bookId}/copies", bookId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryCode\":\"" + code + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn();
        String copyId = json(created, "$.id");
        mockMvc.perform(post("/api/v1/books/{bookId}/copies", bookId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inventoryCode\":\"" + code + "\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/books/{bookId}/copies", bookId).param("status", "AVAILABLE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(patch("/api/v1/copies/{id}/status", copyId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DAMAGED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DAMAGED"));
        mockMvc.perform(get("/api/v1/books").param("copyStatus", "DAMAGED").param("q", "Copy owner"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(patch("/api/v1/copies/{id}/status", copyId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ON_LOAN\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/copies/{id}", copyId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryCode").value(code));
    }

    @Test
    void reportsInvalidInputsAndUnknownResourcesAsClientErrors() throws Exception {
        mockMvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"author\":\"Author\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.title").exists());
        mockMvc.perform(get("/api/v1/books").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/books").param("sort", "isbn,asc"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/books/999999999"))
                .andExpect(status().isNotFound());
    }

    private String createBook(String title, String isbn, String category) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson(title, isbn, category)))
                .andExpect(status().isCreated()).andReturn();
        return json(result, "$.id");
    }

    private String bookJson(String title, String isbn, String category) {
        return "{\"title\":\"" + title + "\",\"author\":\"Test Author\",\"isbn\":"
                + (isbn == null ? "null" : "\"" + isbn + "\"") + ",\"category\":\"" + category + "\"}";
    }

    private String json(MvcResult result, String expression) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), expression).toString();
    }
}
