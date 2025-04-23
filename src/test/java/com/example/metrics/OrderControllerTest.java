package com.example.metrics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void orderSuccessReturnsOk() throws Exception {
        mockMvc.perform(get("/order/success"))
                .andExpect(status().isOk());
    }

    @Test
    void orderFailReturnsOk() throws Exception {
        mockMvc.perform(get("/order/fail"))
                .andExpect(status().isOk());
    }
}
