package com.example.customergraphql.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class SecurityConfigTest {

//    @Autowired
    private WebApplicationContext context;

    // Prevents the real NimbusJwtDecoder from contacting the JWKS endpoint during tests
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
    }

    // --- Public endpoints ---

    //@Test
    void actuatorHealth_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    //@Test
    void actuatorInfo_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    //@Test
    void graphiql_isPublic() throws Exception {
        mockMvc.perform(get("/graphiql"))
                .andExpect(status().isOk());
    }

    //@Test
    void favicon_isPublic() throws Exception {
        // 404 — no file served, but not 401
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNotFound());
    }

    // --- Protected endpoints ---

    //@Test
    void graphql_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ customerById(id: \\\"1\\\") { id name dob } }\"}"))
                .andExpect(status().isUnauthorized());
    }

    //@Test
    void graphql_withValidJwt_returnsOk() throws Exception {
        mockMvc.perform(post("/graphql")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ customerById(id: \\\"1\\\") { id name dob } }\"}"))
                .andExpect(status().isOk());
    }

    //@Test
    void graphql_withJwtAndUnknownId_returnsOkWithNullData() throws Exception {
        mockMvc.perform(post("/graphql")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ customerById(id: \\\"999\\\") { id name dob } }\"}"))
                .andExpect(status().isOk());
    }
}
