package com.debanjan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class RetailWorkflowTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    long batch(boolean rx, LocalDate expiry) throws Exception {
        String payload = """
            {"name":"Test batch","batchNumber":"B-1","unitPrice":0.10,"stockQuantity":4,
             "requiresPrescription":%s,"expiryDate":"%s"}
            """.formatted(rx, expiry);
        String result = mvc.perform(post("/api/medicines").with(csrf()).contentType(APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(result).get("id").asLong();
    }

    @Test
    void staffSignInRequired() throws Exception {
        mvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser
    void receiptRetryAndCustomerHistory() throws Exception {
        long id = batch(true, LocalDate.now().plusDays(10));
        String payload = """
            {"customerName":"Alex","customerPhone":"5550100","paymentMethod":"Cash","checkoutKey":"test-retry",
             "prescriptionNumber":"RX-1","items":[{"medicineId":%d,"quantity":3}]}
            """.formatted(id);
        JsonNode receipt = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            String result = mvc.perform(post("/api/sales").with(csrf()).contentType(APPLICATION_JSON).content(payload))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(0.30))
                    .andReturn().getResponse().getContentAsString();
            JsonNode next = mapper.readTree(result);
            if (receipt != null) assertEquals(receipt.get("id"), next.get("id"));
            receipt = next;
        }
        mvc.perform(get("/api/medicines/"+id)).andExpect(jsonPath("$.stockQuantity").value(1));
        mvc.perform(get("/api/sales/customer/"+receipt.at("/customer/id").asLong()))
                .andExpect(jsonPath("$[0].items[0].medicineName").value("Test batch"));
        mvc.perform(delete("/api/medicines/"+id).with(csrf())).andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void invalidCartRollsBackAllStock() throws Exception {
        long valid = batch(false, LocalDate.now().plusDays(1));
        long expired = batch(false, LocalDate.now().minusDays(1));
        long rx = batch(true, LocalDate.now().plusDays(1));
        for (long blocked : new long[]{expired, rx}) {
            String payload = """
                {"items":[{"medicineId":%d,"quantity":2},{"medicineId":%d,"quantity":1}]}
                """.formatted(valid, blocked);
            mvc.perform(post("/api/sales").with(csrf()).contentType(APPLICATION_JSON).content(payload))
                    .andExpect(status().is4xxClientError());
        }
        for (int quantity : new int[]{-1, 0, 5}) {
            mvc.perform(post("/api/sales").with(csrf()).contentType(APPLICATION_JSON)
                    .content("{\"items\":[{\"medicineId\":"+valid+",\"quantity\":"+quantity+"}]}"))
                    .andExpect(status().is4xxClientError());
        }
        mvc.perform(get("/api/medicines/"+valid)).andExpect(jsonPath("$.stockQuantity").value(4));
    }

    @Test @WithMockUser
    void simultaneousSalesCannotOversell() throws Exception {
        long id = batch(false, LocalDate.now().plusDays(2));
        var start = new java.util.concurrent.CountDownLatch(1);
        var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.Callable<Integer> checkout = () -> {
            start.await();
            return mvc.perform(post("/api/sales")
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("staff"))
                    .with(csrf()).contentType(APPLICATION_JSON)
                    .content("{\"items\":[{\"medicineId\":"+id+",\"quantity\":3}]}"))
                    .andReturn().getResponse().getStatus();
        };
        try {
            var first = workers.submit(checkout);
            var second = workers.submit(checkout);
            start.countDown();
            assertEquals(java.util.Set.of(200, 409), java.util.Set.of(
                    first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(10, java.util.concurrent.TimeUnit.SECONDS)));
        } finally {
            workers.shutdownNow();
        }
        mvc.perform(get("/api/medicines/"+id)).andExpect(jsonPath("$.stockQuantity").value(1));
    }
}
