# 🔒 Security Checklist - GraphQL API

**Document Date**: November 1, 2025  
**Compliance Standards**: OWASP Top 10, GraphQL Security Best Practices

---

## Quick Security Status

| Category | Status | Priority |
|----------|--------|----------|
| Authentication | ⚠️ Partial | HIGH |
| Authorization | ⚠️ Partial | HIGH |
| Input Validation | ❌ Missing | HIGH |
| Query Complexity | ❌ Missing | HIGH |
| Rate Limiting | ✅ Present | MEDIUM |
| Secrets Management | ❌ Hardcoded | HIGH |
| Audit Logging | ❌ Missing | MEDIUM |
| Data Protection | ⚠️ Partial | HIGH |

---

## 1. Authentication Security ✅/⚠️

### Current Implementation
```java
// ✅ GOOD: JWT-based stateless authentication
@Component
public class JwtUtil {
    private final String secret = "your-secret-key"; // ❌ BAD: Hardcoded
    
    public String generateToken(String username) {
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // ❌ 24h too long
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
    }
}
```

### ❌ Security Issues
1. **Hardcoded secret key** - Must use environment variable
2. **Long token expiration** - 24 hours is too long
3. **No token rotation** - Refresh tokens needed
4. **Weak secret** - Should be 256-bit minimum

### ✅ Recommended Implementation
```java
@Component
public class JwtUtil {
    
    @Value("${jwt.secret}") // ✅ Externalized
    private String secret;
    
    @Value("${jwt.access-token.expiration:900000}") // ✅ 15 minutes
    private Long accessTokenExpiration;
    
    @Value("${jwt.refresh-token.expiration:604800000}") // ✅ 7 days
    private Long refreshTokenExpiration;
    
    public String generateAccessToken(UserDetails userDetails) {
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .claim("roles", userDetails.getAuthorities())
            .claim("type", "ACCESS")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
            .compact();
    }
    
    public String generateRefreshToken(String username) {
        return Jwts.builder()
            .setSubject(username)
            .claim("type", "REFRESH")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
            .setId(UUID.randomUUID().toString()) // ✅ Token ID for revocation
            .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
            .compact();
    }
    
    // ✅ Token validation with type checking
    public boolean validateToken(String token, String expectedType) {
        try {
            Claims claims = extractClaims(token);
            String type = claims.get("type", String.class);
            return !isTokenExpired(token) && expectedType.equals(type);
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException e) {
            return false;
        }
    }
}
```

### Configuration (application.yml)
```yaml
jwt:
  secret: ${JWT_SECRET:changeme-generate-random-256bit-key} # Must override in production
  access-token:
    expiration: 900000  # 15 minutes
  refresh-token:
    expiration: 604800000  # 7 days
```

---

## 2. Authorization Security ❌/⚠️

### Current Issues
```java
@Controller
public class GraphQLController {
    
    @QueryMapping
    public User user(@Argument Long id) {
        // ❌ No authorization check - any authenticated user can query any user
        return userService.findById(id);
    }
    
    @MutationMapping
    public User updateUser(@Argument UserUpdateDTO input) {
        // ❌ Missing authorization - users can update other users
        return userService.updateUser(input);
    }
}
```

### ✅ Recommended Implementation
```java
@Controller
public class GraphQLController {
    
    @Autowired
    private SecurityContextHolder securityContext;
    
    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public User user(@Argument Long id, Authentication authentication) {
        // ✅ Users can only query themselves unless they're admin
        if (!hasRole(authentication, "ADMIN") && !isCurrentUser(authentication, id)) {
            throw new AccessDeniedException("You can only view your own profile");
        }
        return userService.findById(id);
    }
    
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public User updateUser(@Argument UserUpdateDTO input, Authentication authentication) {
        // ✅ Verify user can only update their own data
        User currentUser = (User) authentication.getPrincipal();
        if (!currentUser.getId().equals(input.getId()) && !hasRole(authentication, "ADMIN")) {
            throw new AccessDeniedException("You can only update your own profile");
        }
        return userService.updateUser(input);
    }
    
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean deleteUser(@Argument Long id) {
        // ✅ Only admins can delete users
        userService.deleteUser(id);
        return true;
    }
    
    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
    
    private boolean isCurrentUser(Authentication auth, Long userId) {
        User currentUser = (User) auth.getPrincipal();
        return currentUser.getId().equals(userId);
    }
}
```

### Enable Method Security
```java
@Configuration
@EnableReactiveMethodSecurity // ✅ Enable @PreAuthorize
@EnableWebFluxSecurity
public class SecurityConfig {
    // ... existing configuration
}
```

---

## 3. Input Validation ❌

### Current Issues
```java
@MutationMapping
public User createUser(@Argument UserInput input) {
    // ❌ No validation on input
    // ❌ SQL injection possible if using native queries
    // ❌ XSS possible if rendering user content
    return userService.createUser(input);
}
```

### ✅ Recommended Implementation
```java
public record UserInput(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    String username,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "Password must contain uppercase, lowercase, number, and special character"
    )
    String password
) {}

@MutationMapping
public User createUser(@Argument @Valid UserInput input) {
    // ✅ Validation automatically applied
    return userService.createUser(input);
}
```

### Validation Configuration
```java
@Configuration
public class ValidationConfig {
    
    @Bean
    public GraphQLInstrumentation validationInstrumentation() {
        return new ValidationInstrumentation(
            ValidatorFactory.createDefaultValidator()
        );
    }
}
```

### Sanitize User Input
```java
@Service
public class UserService {
    
    public User createUser(UserInput input) {
        // ✅ Sanitize to prevent XSS
        String sanitizedUsername = StringEscapeUtils.escapeHtml4(input.username());
        
        // ✅ Use parameterized queries (JPA does this automatically)
        User user = new User();
        user.setUsername(sanitizedUsername);
        user.setEmail(input.email());
        user.setPassword(passwordEncoder.encode(input.password()));
        
        return userRepository.save(user);
    }
}
```

---

## 4. Query Complexity Limits ❌ CRITICAL

### Attack Scenario
```graphql
# ❌ Malicious query - can bring down the server
query MaliciousQuery {
  users {
    posts {
      user {
        posts {
          user {
            posts {
              user {
                posts {
                  # ... infinite depth
                }
              }
            }
          }
        }
      }
    }
  }
}
```

### ✅ Solution 1: Query Depth Limiting
```java
@Configuration
public class GraphQLSecurityConfig {
    
    @Bean
    public GraphQLInstrumentation queryDepthInstrumentation() {
        return new MaxQueryDepthInstrumentation(10); // ✅ Limit to 10 levels
    }
}
```

### ✅ Solution 2: Query Complexity Analysis
```java
@Configuration
public class GraphQLSecurityConfig {
    
    @Bean
    public GraphQLInstrumentation queryComplexityInstrumentation() {
        return new ComplexityAnalysisInstrumentation(
            maxComplexity: 200,
            complexityFunction: field -> {
                // Simple fields = 1 point
                if (field.getSelectionSet() == null) {
                    return 1;
                }
                
                // List fields = 10 points (potential N+1)
                if (field.getType() instanceof GraphQLList) {
                    return 10;
                }
                
                // Object fields = 2 points
                return 2;
            }
        );
    }
}
```

### ✅ Solution 3: Query Timeout
```yaml
spring:
  graphql:
    query-timeout: 30s # ✅ Timeout long-running queries
```

### ✅ Solution 4: Disable Introspection in Production
```java
@Configuration
public class GraphQLSecurityConfig {
    
    @Bean
    public GraphQLInstrumentation disableIntrospectionInProduction(
        @Value("${spring.profiles.active}") String profile
    ) {
        if ("prod".equals(profile)) {
            return new IntrospectionDisablingInstrumentation(); // ✅ Disable in prod
        }
        return null;
    }
}
```

---

## 5. Rate Limiting ✅ (Already Implemented)

### Current Implementation - Good!
```java
@Component
public class GraphQLRateLimitInterceptor implements WebGraphQlInterceptor {
    
    private final RateLimiter rateLimiter = RateLimiter.create(100); // ✅ 100 req/sec
    
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        if (!rateLimiter.tryAcquire()) {
            return Mono.error(new TooManyRequestsException());
        }
        return chain.next(request);
    }
}
```

### ⚠️ Enhancement: Per-User Rate Limiting
```java
@Component
public class GraphQLRateLimitInterceptor implements WebGraphQlInterceptor {
    
    private final Map<String, RateLimiter> userLimiters = new ConcurrentHashMap<>();
    
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String username = extractUsername(request);
        
        RateLimiter limiter = userLimiters.computeIfAbsent(
            username,
            k -> RateLimiter.create(10) // ✅ 10 req/sec per user
        );
        
        if (!limiter.tryAcquire()) {
            return Mono.error(new TooManyRequestsException(
                "Rate limit exceeded. Please try again later."
            ));
        }
        
        return chain.next(request);
    }
}
```

---

## 6. Secrets Management ❌ CRITICAL

### Current Issues
```yaml
# ❌ BAD: Hardcoded secrets in application.yml
spring:
  datasource:
    password: postgres  # ❌ Committed to Git
  data:
    redis:
      password: redis123  # ❌ Plaintext password
jwt:
  secret: your-secret-key  # ❌ Weak secret
```

### ✅ Solution 1: Environment Variables
```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}  # ✅ From environment
  data:
    redis:
      password: ${REDIS_PASSWORD}
jwt:
  secret: ${JWT_SECRET}  # ✅ Must be 256-bit random string
```

### ✅ Solution 2: Spring Cloud Config
```yaml
spring:
  cloud:
    config:
      uri: https://config-server.example.com
      fail-fast: true
  config:
    import: "optional:configserver:https://config-server.example.com"
```

### ✅ Solution 3: HashiCorp Vault
```java
@Configuration
public class VaultConfig {
    
    @Bean
    public VaultTemplate vaultTemplate() {
        VaultEndpoint endpoint = VaultEndpoint.create("vault.example.com", 8200);
        endpoint.setScheme("https");
        
        VaultTokenSupplier tokenSupplier = () -> 
            VaultToken.of(System.getenv("VAULT_TOKEN"));
        
        return new VaultTemplate(endpoint, 
            new TokenAuthentication(tokenSupplier.get()));
    }
}
```

### .env.example (for local development)
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=graphql_db
DB_USERNAME=postgres
DB_PASSWORD=changeme

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=changeme

# JWT
JWT_SECRET=changeme-generate-256bit-random-key-using-openssl

# Generate secret with: openssl rand -base64 32
```

---

## 7. Audit Logging ❌

### ✅ Recommended Implementation
```java
@Aspect
@Component
public class AuditLoggingAspect {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    
    @Around("@annotation(org.springframework.graphql.data.method.annotation.MutationMapping)")
    public Object logMutation(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "anonymous";
        String operation = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // ✅ Log before execution
        auditLogger.info("User {} attempting {} with args {}", 
            username, operation, sanitize(args));
        
        try {
            Object result = joinPoint.proceed();
            
            // ✅ Log success
            auditLogger.info("User {} successfully executed {}", username, operation);
            return result;
            
        } catch (Exception e) {
            // ✅ Log failure
            auditLogger.error("User {} failed to execute {}: {}", 
                username, operation, e.getMessage());
            throw e;
        }
    }
    
    private Object sanitize(Object[] args) {
        // ✅ Remove sensitive data (passwords, etc.)
        return Arrays.stream(args)
            .map(arg -> {
                if (arg instanceof UserInput input) {
                    return new UserInput(input.username(), input.email(), "***REDACTED***");
                }
                return arg;
            })
            .toArray();
    }
}
```

### Audit Log Configuration
```yaml
logging:
  level:
    AUDIT: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/audit.log
    max-size: 10MB
    max-history: 30
```

---

## 8. Data Protection

### ✅ Password Encryption (Already Implemented)
```java
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // ✅ Strong BCrypt with cost 12
    }
}
```

### ✅ Sensitive Data Masking
```java
@Entity
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    
    @Column(unique = true)
    private String email;
    
    @JsonIgnore // ✅ Never expose password in GraphQL responses
    private String password;
    
    @Column(name = "ssn")
    @Convert(converter = SensitiveDataConverter.class) // ✅ Encrypt SSN at rest
    private String socialSecurityNumber;
}

@Converter
public class SensitiveDataConverter implements AttributeConverter<String, String> {
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData);
    }
}
```

### ✅ HTTPS Only in Production
```yaml
server:
  ssl:
    enabled: ${SSL_ENABLED:false}
    key-store: ${SSL_KEYSTORE_PATH}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
  http2:
    enabled: true
```

---

## 9. CORS Configuration

### Current Implementation
```java
@Configuration
public class SecurityConfig {
    @Bean
    public CorsConfigurationSource corsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("*")); // ❌ TOO PERMISSIVE
        config.setAllowedMethods(Arrays.asList("*"));
        config.setAllowedHeaders(Arrays.asList("*"));
        return request -> config;
    }
}
```

### ✅ Secure CORS Configuration
```java
@Configuration
public class SecurityConfig {
    
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;
    
    @Bean
    public CorsConfigurationSource corsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();
        
        // ✅ Specific origins only
        config.setAllowedOrigins(allowedOrigins); // From application.yml
        
        // ✅ Specific methods only
        config.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS"));
        
        // ✅ Specific headers only
        config.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type",
            "X-Request-ID"
        ));
        
        // ✅ Don't allow credentials with wildcard origin
        config.setAllowCredentials(true);
        
        // ✅ Cache preflight requests
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/graphql", config);
        return source;
    }
}
```

```yaml
cors:
  allowed-origins:
    - https://app.example.com
    - https://admin.example.com
    # Never use "*" in production
```

---

## 10. Error Handling

### Current Implementation
```java
@Component
public class GraphQlExceptionHandler implements DataFetcherExceptionResolver {
    @Override
    public Mono<List<GraphQLError>> resolveException(Throwable ex, DataFetchingEnvironment env) {
        GraphQLError error = GraphqlErrorBuilder.newError()
            .message(ex.getMessage()) // ❌ Exposes internal details
            .build();
        return Mono.just(List.of(error));
    }
}
```

### ✅ Secure Error Handling
```java
@Component
public class GraphQlExceptionHandler implements DataFetcherExceptionResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphQlExceptionHandler.class);
    
    @Value("${spring.profiles.active}")
    private String activeProfile;
    
    @Override
    public Mono<List<GraphQLError>> resolveException(Throwable ex, DataFetchingEnvironment env) {
        
        // ✅ Log full details server-side
        logger.error("GraphQL error in field {}: {}", 
            env.getField().getName(), ex.getMessage(), ex);
        
        // ✅ Return safe error to client
        GraphQLError error;
        
        if (ex instanceof AccessDeniedException) {
            error = GraphqlErrorBuilder.newError()
                .message("Access denied")
                .errorType(ErrorType.FORBIDDEN)
                .build();
                
        } else if (ex instanceof ValidationException) {
            error = GraphqlErrorBuilder.newError()
                .message("Invalid input: " + ex.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .build();
                
        } else if (ex instanceof EntityNotFoundException) {
            error = GraphqlErrorBuilder.newError()
                .message("Resource not found")
                .errorType(ErrorType.NOT_FOUND)
                .build();
                
        } else {
            // ✅ Generic error for unexpected exceptions
            String message = "prod".equals(activeProfile) 
                ? "Internal server error" // ✅ Don't leak details in prod
                : ex.getMessage(); // ✅ Show details in dev
                
            error = GraphqlErrorBuilder.newError()
                .message(message)
                .errorType(ErrorType.INTERNAL_ERROR)
                .build();
        }
        
        return Mono.just(List.of(error));
    }
}
```

---

## Security Checklist Summary

### ✅ Immediate Actions (High Priority)
- [ ] Remove hardcoded secrets, use environment variables
- [ ] Implement query depth/complexity limits
- [ ] Add input validation on all mutations
- [ ] Implement proper authorization checks
- [ ] Shorten JWT expiration (15 min access, 7 day refresh)
- [ ] Disable GraphQL introspection in production
- [ ] Add audit logging for mutations

### ⚠️ Short-term Actions (Medium Priority)
- [ ] Implement per-user rate limiting
- [ ] Add sensitive data encryption at rest
- [ ] Configure secure CORS policy
- [ ] Implement secure error handling
- [ ] Add security headers (CSP, X-Frame-Options, etc.)
- [ ] Enable HTTPS in production

### 📋 Long-term Actions (Low Priority)
- [ ] Integrate with HashiCorp Vault or AWS Secrets Manager
- [ ] Implement security monitoring and alerting
- [ ] Conduct security penetration testing
- [ ] Implement data retention policies
- [ ] Add compliance logging (GDPR, SOC2)

---

## Testing Security

```java
@Test
void shouldPreventUnauthorizedAccess() {
    String query = """
        query {
            user(id: 999) {
                username
                email
            }
        }
        """;
    
    graphQlTester.document(query)
        .execute()
        .errors()
        .expect(error -> error.getMessage().equals("Access denied"));
}

@Test
void shouldPreventSQLInjection() {
    String maliciousInput = "'; DROP TABLE users; --";
    
    UserInput input = new UserInput(maliciousInput, "test@test.com", "password");
    
    // Should fail validation
    assertThrows(ValidationException.class, () -> 
        userService.createUser(input)
    );
}

@Test
void shouldLimitQueryDepth() {
    String deepQuery = """
        query {
            users {
                posts {
                    user {
                        posts {
                            user {
                                posts {
                                    # ... 20 levels deep
                                }
                            }
                        }
                    }
                }
            }
        }
        """;
    
    graphQlTester.document(deepQuery)
        .execute()
        .errors()
        .expect(error -> error.getMessage().contains("Query depth exceeded"));
}
```

---

*Security is not a one-time task - it requires continuous monitoring and improvement.*
