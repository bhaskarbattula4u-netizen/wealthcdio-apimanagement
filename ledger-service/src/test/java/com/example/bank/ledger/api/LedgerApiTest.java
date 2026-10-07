package com.example.bank.ledger.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the real wiring (controller, service, repository) through HTTP. */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Ledger REST API")
class LedgerApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("POST /ledger/entries records an entry and returns 201 with the stamped entry")
    void appendsEntry() throws Exception {
        mvc.perform(post("/ledger/entries").contentType(MediaType.APPLICATION_JSON).content("""
                        {"accountId":"api-1","type":"DEPOSIT","amount":10.00,"balanceAfter":10.00,"correlationId":"c1"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.accountId").value("api-1"))
                .andExpect(jsonPath("$.type").value("DEPOSIT"));
    }

    @Test
    @DisplayName("GET /ledger/accounts/{id}/entries lists the account's entries oldest first")
    void listsHistory() throws Exception {
        append("api-2", "10.00", "10.00");
        append("api-2", "5.00", "15.00");

        mvc.perform(get("/ledger/accounts/api-2/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].balanceAfter").value(10.00))
                .andExpect(jsonPath("$[1].balanceAfter").value(15.00));
    }

    @Test
    @DisplayName("GET history for an account with no entries is 200 with an empty list")
    void emptyHistory() throws Exception {
        mvc.perform(get("/ledger/accounts/never-used/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("rejects a non-positive amount with 400")
    void rejectsNonPositiveAmount() throws Exception {
        mvc.perform(post("/ledger/entries").contentType(MediaType.APPLICATION_JSON).content("""
                        {"accountId":"api-3","type":"DEPOSIT","amount":0,"balanceAfter":0}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("rejects a missing account id or unknown entry type with 400")
    void rejectsIncompleteEntry() throws Exception {
        mvc.perform(post("/ledger/entries").contentType(MediaType.APPLICATION_JSON).content("""
                        {"type":"DEPOSIT","amount":1,"balanceAfter":1}
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/ledger/entries").contentType(MediaType.APPLICATION_JSON).content("""
                        {"accountId":"x","type":"BONUS","amount":1,"balanceAfter":1}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("rejects a malformed body with 400")
    void rejectsMalformedBody() throws Exception {
        mvc.perform(post("/ledger/entries").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    private void append(String accountId, String amount, String balanceAfter) throws Exception {
        mvc.perform(post("/ledger/entries").contentType(MediaType.APPLICATION_JSON).content("""
                {"accountId":"%s","type":"DEPOSIT","amount":%s,"balanceAfter":%s}
                """.formatted(accountId, amount, balanceAfter))).andExpect(status().isCreated());
    }
}
