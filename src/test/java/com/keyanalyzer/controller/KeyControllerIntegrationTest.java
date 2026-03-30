package com.keyanalyzer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KeyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsStructuredValidationErrors() throws Exception {
        mockMvc.perform(post("/api/compute-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attributes\":[\"A\"],\"fds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Functional dependencies list cannot be empty"));
    }

    @Test
    void exposesHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void allowsConfiguredCorsOrigins() throws Exception {
        mockMvc.perform(options("/api/compute-keys")
                        .header("Origin", "https://preview.vercel.app")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://preview.vercel.app"));
    }
}
