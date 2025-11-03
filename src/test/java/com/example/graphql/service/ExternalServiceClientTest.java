package com.example.graphql.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.graphql.entity.ExternalServiceResponse;
import com.example.graphql.entity.User;
import com.example.graphql.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalServiceClient Tests")
class ExternalServiceClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    private ExternalServiceClient externalServiceClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        externalServiceClient = new ExternalServiceClient(jwtUtil, webClient);
        // Inject userService via reflection since it's @Autowired
        org.springframework.test.util.ReflectionTestUtils.setField(externalServiceClient, "userService", userService);

        userDetails = new User("testuser", "test@example.com", "password", User.Role.USER);
    }

    @Test
    @DisplayName("Should call Service A successfully")
    void testCallServiceASuccess() {
        ExternalServiceResponse mockResponse = new ExternalServiceResponse("Data from Service A");

        // Mock JWT validation
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken("valid-token", userDetails)).thenReturn(true);

        // Mock WebClient chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExternalServiceResponse.class)).thenReturn(Mono.just(mockResponse));

        Mono<ExternalServiceResponse> result = externalServiceClient.callServiceA("user123", "valid-token");

        StepVerifier.create(result)
                .expectNextMatches(response -> "Data from Service A".equals(response.getData()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fail Service A call with invalid token")
    void testCallServiceAInvalidToken() {
        // Mock JWT validation to fail
        when(jwtUtil.extractUsername("invalid-token")).thenReturn("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken("invalid-token", userDetails)).thenReturn(false);

        Mono<ExternalServiceResponse> result = externalServiceClient.callServiceA("user123", "invalid-token");

        StepVerifier.create(result).expectError().verify();
    }

    @Test
    @DisplayName("Should call Service B successfully")
    void testCallServiceBSuccess() {
        ExternalServiceResponse mockResponse = new ExternalServiceResponse("Data from Service B");

        // Mock JWT validation
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken("valid-token", userDetails)).thenReturn(true);

        // Mock WebClient chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExternalServiceResponse.class)).thenReturn(Mono.just(mockResponse));

        Mono<ExternalServiceResponse> result = externalServiceClient.callServiceB("user123", "valid-token");

        StepVerifier.create(result)
                .expectNextMatches(response -> "Data from Service B".equals(response.getData()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle error from Service B")
    void testCallServiceBError() {
        // Mock JWT validation
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken("valid-token", userDetails)).thenReturn(true);

        // Mock WebClient chain to return error
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExternalServiceResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Service B Error")));

        Mono<ExternalServiceResponse> result = externalServiceClient.callServiceB("user123", "valid-token");

        StepVerifier.create(result)
                .expectNextMatches(response -> "Service B unavailable. Try again later.".equals(response.getData()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle missing token")
    void testMissingToken() {
        Mono<ExternalServiceResponse> result = externalServiceClient.callServiceA("user123", null);

        StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();
    }

    @Test
    @DisplayName("Should handle empty token")
    void testEmptyToken() {
        Mono<ExternalServiceResponse> result = externalServiceClient.callServiceA("user123", "");

        StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();
    }

    @Test
    @DisplayName("Should handle JWT extraction error")
    void testJwtExtractionError() {
        when(jwtUtil.extractUsername("malformed-token")).thenThrow(new RuntimeException("Invalid JWT"));

        Mono<ExternalServiceResponse> result = externalServiceClient.callServiceA("user123", "malformed-token");

        StepVerifier.create(result).expectError().verify();
    }
}
