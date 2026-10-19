package tr.kitaptakip.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tr.kitaptakip.dtos.CreateLoanRequest;
import tr.kitaptakip.dtos.LoanResponse;
import tr.kitaptakip.exceptions.ConflictException;
import tr.kitaptakip.services.LoanService;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LoanApiIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired LoanService loanService;

    @Test
    void managesBorrowersAndPreventsNewLoansForInactivePeople() throws Exception {
        String borrowerId = createBorrower("Ada", "Yılmaz");
        mockMvc.perform(get("/api/v1/borrowers").param("q", "ada").param("active", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(put("/api/v1/borrowers/{id}", borrowerId).contentType(MediaType.APPLICATION_JSON)
                        .content(borrowerJson("Ada", "Lovelace")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lastName").value("Lovelace"));
        mockMvc.perform(patch("/api/v1/borrowers/{id}/status", borrowerId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));

        String copyId = createCopy();
        mockMvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(loanJson(copyId, borrowerId, LocalDate.now(), LocalDate.now().plusDays(7))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/borrowers").param("active", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createsLoansReturnsCopiesAndListsHistoryAndOverdueRecords() throws Exception {
        String borrowerId = createBorrower("Grace", "Hopper");
        String copyId = createCopy();
        LocalDate loanDate = LocalDate.now().minusDays(10);
        LocalDate dueDate = LocalDate.now().minusDays(2);
        MvcResult created = mockMvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(loanJson(copyId, borrowerId, loanDate, dueDate)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.overdue").value(true)).andReturn();
        String loanId = json(created, "$.id");

        mockMvc.perform(get("/api/v1/loans/overdue").param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(patch("/api/v1/loans/{id}/return", loanId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"returnDate\":\"" + LocalDate.now() + "\",\"copyStatus\":\"DAMAGED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.overdue").value(false)).andExpect(jsonPath("$.returnDate").value(LocalDate.now().toString()));
        mockMvc.perform(get("/api/v1/copies/{id}", copyId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DAMAGED"));
        mockMvc.perform(patch("/api/v1/loans/{id}/return", loanId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"returnDate\":\"" + LocalDate.now() + "\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/borrowers/{id}/loans", borrowerId).param("active", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].active").value(false));
    }

    @Test
    void rejectsInvalidLoanDatesAndNonAvailableCopies() throws Exception {
        String borrowerId = createBorrower("Katherine", "Johnson");
        String copyId = createCopy();
        LocalDate today = LocalDate.now();
        mockMvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(loanJson(copyId, borrowerId, today, today.minusDays(1))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(loanJson(copyId, borrowerId, today, today.plusDays(5))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(loanJson(copyId, borrowerId, today, today.plusDays(5))))
                .andExpect(status().isConflict());
    }

    @Test
    void serializesConcurrentLoanAttemptsForTheSamePhysicalCopy() throws Exception {
        String copyId = createCopy();
        String firstBorrower = createBorrower("First", "Reader");
        String secondBorrower = createBorrower("Second", "Reader");
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> loanService.create(request(copyId, firstBorrower)));
            var second = pool.submit(() -> loanService.create(request(copyId, secondBorrower)));
            int successful = 0;
            int conflicts = 0;
            for (var future : java.util.List.of(first, second)) {
                try {
                    LoanResponse response = future.get();
                    assertThat(response.active()).isTrue();
                    successful++;
                } catch (ExecutionException exception) {
                    assertThat(exception.getCause()).isInstanceOf(ConflictException.class);
                    conflicts++;
                }
            }
            assertThat(successful).isEqualTo(1);
            assertThat(conflicts).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private String createBorrower(String firstName, String lastName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/borrowers").contentType(MediaType.APPLICATION_JSON)
                        .content(borrowerJson(firstName, lastName)))
                .andExpect(status().isCreated()).andReturn();
        return json(result, "$.id");
    }

    private String createCopy() throws Exception {
        String bookTitle = "Loan book " + UUID.randomUUID();
        MvcResult book = mockMvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + bookTitle + "\",\"author\":\"Author\"}"))
                .andExpect(status().isCreated()).andReturn();
        String bookId = json(book, "$.id");
        MvcResult copy = mockMvc.perform(post("/api/v1/books/{id}/copies", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inventoryCode\":\"LOAN-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return json(copy, "$.id");
    }

    private String borrowerJson(String firstName, String lastName) {
        return "{\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\"}";
    }

    private String loanJson(String copyId, String borrowerId, LocalDate loanDate, LocalDate dueDate) {
        return "{\"copyId\":" + copyId + ",\"borrowerId\":" + borrowerId + ",\"loanDate\":\""
                + loanDate + "\",\"dueDate\":\"" + dueDate + "\"}";
    }

    private CreateLoanRequest request(String copyId, String borrowerId) {
        LocalDate today = LocalDate.now();
        return new CreateLoanRequest(Long.valueOf(copyId), Long.valueOf(borrowerId), today, today.plusDays(7), null);
    }

    private String json(MvcResult result, String expression) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), expression).toString();
    }
}
