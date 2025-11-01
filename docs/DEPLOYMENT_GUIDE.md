# 🚀 Deployment Guide

**Production Deployment for Spring Boot GraphQL API**

---

## Deployment Options

| Option | Complexity | Cost | Scalability | Best For |
|--------|------------|------|-------------|----------|
| **Docker + VPS** | Low | $ | Manual | Small apps |
| **AWS ECS** | Medium | $$ | Auto | Production apps |
| **Kubernetes** | High | $$$ | Full Auto | Enterprise apps |
| **Cloud Run** | Low | $ | Auto | Serverless |

---

## 1. Docker Deployment

### Dockerfile
```dockerfile
# Multi-stage build for optimized image
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

# Build application
RUN ./gradlew build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run with production profile
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
```

### Build and Run
```bash
# Build image
docker build -t graphql-api:latest .

# Run container
docker run -d \
  --name graphql-api \
  -p 8080:8080 \
  -e DB_PASSWORD=secret \
  -e REDIS_PASSWORD=secret \
  -e JWT_SECRET=secret \
  graphql-api:latest
```

---

## 2. Docker Compose (Full Stack)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: graphql_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
  
  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5
  
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: postgres
      DB_PASSWORD: ${DB_PASSWORD}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

volumes:
  postgres-data:
  redis-data:
```

---

## 3. Kubernetes Deployment

### Deployment Manifest
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: graphql-api
  labels:
    app: graphql-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: graphql-api
  template:
    metadata:
      labels:
        app: graphql-api
    spec:
      containers:
      - name: graphql-api
        image: your-registry/graphql-api:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_HOST
          value: "postgres-service"
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
        - name: REDIS_HOST
          value: "redis-service"
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: redis-secret
              key: password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: graphql-api-service
spec:
  selector:
    app: graphql-api
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: graphql-api-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: graphql-api
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### Secrets
```bash
kubectl create secret generic db-secret --from-literal=password=your-db-password
kubectl create secret generic redis-secret --from-literal=password=your-redis-password
kubectl create secret generic jwt-secret --from-literal=secret=your-jwt-secret
```

---

## 4. CI/CD Pipeline (GitHub Actions)

### .github/workflows/deploy.yml
```yaml
name: Build and Deploy

on:
  push:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run tests
        run: ./gradlew test
      
      - name: Upload coverage
        uses: codecov/codecov-action@v3

  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v3
      
      - name: Log in to Container Registry
        uses: docker/login-action@v2
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      
      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v4
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=sha,prefix={{branch}}-
            type=ref,event=branch
            type=semver,pattern={{version}}
      
      - name: Build and push Docker image
        uses: docker/build-push-action@v4
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to Kubernetes
        run: |
          kubectl set image deployment/graphql-api \
            graphql-api=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
```

---

## 5. AWS ECS Deployment

### Task Definition
```json
{
  "family": "graphql-api",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "containerDefinitions": [
    {
      "name": "graphql-api",
      "image": "your-ecr-repo/graphql-api:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        }
      ],
      "secrets": [
        {
          "name": "DB_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:db-password"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/graphql-api",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

---

## 6. Production Checklist

### Before Deployment
- [ ] All tests passing
- [ ] Environment variables configured
- [ ] Secrets stored securely (not in code)
- [ ] Database migrations ready
- [ ] Health checks configured
- [ ] Monitoring and alerting setup
- [ ] Load testing completed
- [ ] Security scan passed
- [ ] Documentation updated

### After Deployment
- [ ] Smoke tests passed
- [ ] Health checks responding
- [ ] Metrics being collected
- [ ] Logs being aggregated
- [ ] Alerts configured
- [ ] Backup strategy in place
- [ ] Rollback plan documented

---

## 7. Environment Variables

```bash
# Application
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

# Database
DB_HOST=postgres.example.com
DB_PORT=5432
DB_NAME=graphql_db
DB_USERNAME=postgres
DB_PASSWORD=<SECRET>

# Redis
REDIS_HOST=redis.example.com
REDIS_PORT=6379
REDIS_PASSWORD=<SECRET>

# JWT
JWT_SECRET=<256-BIT-RANDOM-KEY>

# Monitoring
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,prometheus
```

---

## 8. Scaling Strategy

### Horizontal Scaling
```bash
# Kubernetes
kubectl scale deployment graphql-api --replicas=5

# Docker Swarm
docker service scale graphql-api=5
```

### Database Connection Pooling
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Per instance
      minimum-idle: 5
```

**Formula**: `Total Connections = Replicas × Pool Size`  
**Example**: 5 replicas × 20 connections = 100 total DB connections

---

## 9. Monitoring in Production

```bash
# Check application health
curl https://api.example.com/actuator/health

# Check metrics
curl https://api.example.com/actuator/metrics

# View logs
kubectl logs -f deployment/graphql-api

# Check resource usage
kubectl top pods -l app=graphql-api
```

---

## 10. Rollback Strategy

```bash
# Kubernetes rollback
kubectl rollout undo deployment/graphql-api

# Docker rollback
docker service update --rollback graphql-api

# Manual rollback
kubectl set image deployment/graphql-api \
  graphql-api=your-registry/graphql-api:previous-tag
```

---

*Deploy with confidence, monitor continuously, scale intelligently.*
