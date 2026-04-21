package com.example.customergraphql.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SecurityConfigLocalProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorHealth_isAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void graphql_isAccessibleWithoutToken() throws Exception {
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ customerById(id: \\\"1\\\") { id name dob } }\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void graphiql_isAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/graphiql"))
                .andExpect(status().isOk());
    }
}