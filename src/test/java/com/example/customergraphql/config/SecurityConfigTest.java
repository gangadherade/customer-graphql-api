package com.example.customergraphql.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://test-issuer.example.com",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://test-issuer.example.com/.well-known/jwks.json"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // Prevents the real NimbusJwtDecoder from contacting the JWKS endpoint during tests
    @MockBean
    private JwtDecoder jwtDecoder;

    // --- Public endpoints ---

    @Test
    void actuatorHealth_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorInfo_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void graphiql_isPublic() throws Exception {
        mockMvc.perform(get("/graphiql"))
                .andExpect(status().isOk());
    }

    @Test
    void favicon_isPublic() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNotEqualTo(401));
    }

    // --- Protected endpoints ---

    @Test
    void graphql_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ customerById(id: \\\"1\\\") { id name dob } }\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void graphql_withValidJwt_returnsOk() throws Exception {
        mockMvc.perform(post("/graphql")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ customerById(id: \\\"1\\\") { id name dob } }\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void graphql_withJwtAndUnknownId_returnsOkWithNullData() throws Exception {
        mockMvc.perform(post("/graphql")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ customerById(id: \\\"999\\\") { id name dob } }\"}"))
                .andExpect(status().isOk());
    }
}