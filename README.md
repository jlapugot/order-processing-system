# Order Processing System

[![Build Status](https://github.com/jlapugot/order-processing-system/workflows/CI/badge.svg)](https://github.com/jlapugot/order-processing-system/actions)
[![Coverage](https://img.shields.io/badge/coverage-91%25-brightgreen.svg)](https://github.com/jlapugot/order-processing-system)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)

> **Portfolio Project** - A production-ready microservices-based order processing system demonstrating enterprise-grade software engineering practices, cloud-native architecture, and modern DevOps workflows.

## Project Overview

This is a **portfolio project** showcasing:
- **Microservices Architecture** with event-driven design
- **High test coverage** with 91% code coverage
- **Production-ready infrastructure** on AWS (ap-southeast-1)
- **Complete CI/CD pipeline** with automated testing and deployment
- **Enterprise patterns**: Circuit breaker, caching, DLQ, retry logic
- **Infrastructure as Code** with Terraform
- **Comprehensive testing**: 69 tests (unit, integration, Kafka)

**Built to demonstrate proficiency in:**
- Spring Boot ecosystem
- Apache Kafka event streaming
- AWS cloud deployment
- DevOps practices (Docker, Terraform, GitHub Actions)
- Software quality (testing, code coverage, security scanning)

---

## Architecture

This system implements an **event-driven microservices architecture**:

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  Load Balancer  │ (AWS ALB)
└────────┬────────┘
         │
    ┌────┴─────┬────────────┐
    │          │            │
    ▼          ▼            ▼
┌────────┐ ┌──────────┐ ┌────────────┐
│ Order  │ │Inventory │ │Notification│
│Service │ │ Service  │ │  Service   │
└───┬────┘ └────┬─────┘ └─────┬──────┘
    │           │             │
    └───────────┼─────────────┘
                ▼
        ┌───────────────┐
        │ Apache Kafka  │ (Event Bus)
        └───────────────┘
                │
       ┌────────┴────────┐
       ▼                 ▼
┌─────────────┐   ┌─────────────┐
│ PostgreSQL  │   │   Redis     │
│   (RDS)     │   │(ElastiCache)│
└─────────────┘   └─────────────┘
```

### Core Components

- **Order Service**: REST API for order management (CRUD operations)
- **Inventory Service**: Manages inventory reservations via Kafka events
- **Notification Service**: Sends notifications based on order events
- **Common Library**: Shared utilities, events, and exceptions

---

## Tech Stack

### Backend
- **Java 21** - Latest LTS with modern language features
- **Spring Boot 3.2.5** - Framework with DI, REST, JPA
- **Spring Kafka** - Event-driven messaging
- **Spring Data JPA** - Database abstraction
- **Spring Cache (Redis)** - Distributed caching
- **Resilience4j** - Circuit breaker pattern

### Data Layer
- **PostgreSQL 15** - Relational database
- **Redis 7** - In-memory cache
- **Apache Kafka 3.x** - Event streaming platform

### Infrastructure & DevOps
- **AWS Services**: ECS (EC2 t3.micro), RDS (db.t3.micro), ElastiCache (cache.t3.micro), S3, CloudWatch
- **Terraform** - Infrastructure as Code
- **Docker & Docker Compose** - Containerization
- **GitHub Actions** - CI/CD pipeline

### Testing & Quality
- **JUnit 5** - Unit testing framework
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions
- **Testcontainers** - Integration testing
- **JaCoCo** - Code coverage (91%)
- **EmbeddedKafka** - Kafka integration tests
- **Trivy, Snyk, CodeQL** - Security scanning

### Region
- **AWS ap-southeast-1** (Singapore) - Closest to Philippines for optimal latency

---

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- Git

### Local Development Setup

1. **Clone the repository**
```bash
git clone https://github.com/jlapugot/order-processing-system.git
cd order-processing-system
```

2. **Start infrastructure services**
```bash
docker-compose up -d postgres kafka zookeeper redis
```

3. **Build the project**
```bash
# Order Service
cd services/order-service
mvn clean install

# Inventory Service
cd ../inventory-service
mvn clean install
```

4. **Run the services**
```bash
# Terminal 1 - Order Service
cd services/order-service
mvn spring-boot:run

# Terminal 2 - Inventory Service
cd services/inventory-service
mvn spring-boot:run
```

5. **Access the application**
- Order Service: http://localhost:8080
- Inventory Service: http://localhost:8081
- Health Check: http://localhost:8080/actuator/health

---

## API Examples

### Create an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "items": [
      {
        "productId": 1,
        "quantity": 2,
        "price": 29.99
      }
    ]
  }'
```

**Response:**
```json
{
  "id": 1,
  "customerId": 1,
  "status": "CONFIRMED",
  "totalAmount": 59.98,
  "items": [...],
  "createdAt": "2025-10-24T10:30:00Z"
}
```

### Get Order

```bash
curl http://localhost:8080/api/orders/1
```

### Check Inventory

```bash
curl http://localhost:8081/api/inventory/product/1
```

---

## Project Structure

```
order-processing-system/
├── services/
│   ├── common-lib/           # Shared models and utilities
│   ├── order-service/        # Order management
│   ├── inventory-service/    # Inventory management
│   └── notification-service/ # Event-driven notifications
├── terraform/
│   ├── modules/              # Reusable infrastructure modules
│   │   ├── vpc/             # Network configuration
│   │   ├── rds/             # PostgreSQL database
│   │   ├── elasticache/     # Redis cache
│   │   ├── ecs/             # Container orchestration
│   │   └── s3/              # Object storage
│   └── environments/
│       ├── dev/             # Development environment
│       └── prod/            # Production environment
├── scripts/                  # Deployment automation
├── .github/workflows/        # CI/CD pipelines
│   ├── ci.yml               # Continuous Integration
│   ├── cd-dev.yml           # Deploy to development
│   └── cd-prod.yml          # Deploy to production
└── docker-compose.yml        # Local development setup
```

---

## Testing

**Total Coverage: 91% (69 tests passing)**

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Test Breakdown

- **Unit Tests (70%)**: Service logic, controllers, repositories
- **Integration Tests (30%)**: Kafka, Redis cache, database
- **Test Types**:
  - Repository tests (H2 in-memory)
  - Kafka integration (EmbeddedKafka)
  - Cache tests (Redis)
  - Circuit breaker tests
  - DLQ and retry tests

---

## AWS Deployment

### Infrastructure Overview

- **VPC**: Custom networking with public/private subnets
- **ECS**: Container orchestration (EC2 t3.micro)
- **RDS**: PostgreSQL db.t3.micro (Multi-AZ in prod)
- **ElastiCache**: Redis cache.t3.micro
- **S3**: Artifacts, logs, backups
- **ALB**: Application load balancer

### Deployment Process

1. **Setup Terraform backend**
```bash
./scripts/setup-terraform.sh
```

2. **Deploy infrastructure**
```bash
./scripts/deploy-infrastructure.sh dev
```

3. **Create ECR repositories**
```bash
./scripts/create-ecr-repositories.sh
```

4. **Setup ECS services**
```bash
./scripts/setup-ecs-services.sh dev
```

5. **Deploy via CI/CD**
- Push to `main` → Auto-deploy to dev
- Create release → Manual approval → Deploy to prod

---

## CI/CD Pipeline

### Continuous Integration (ci.yml)
- Build Java applications
- Run 69 tests
- Check code coverage (>80%)
- Security scanning (Trivy, Snyk, CodeQL)
- Build Docker images

### Continuous Deployment (cd-dev.yml)
- Auto-deploy on push to `main`
- Build & push Docker images to ECR
- Deploy to ECS
- Run smoke tests

### Production Deployment (cd-prod.yml)
- Manual approval required
- Backup before deployment
- Rolling deployment
- Integration tests
- Automatic rollback on failure

---

## Key Features Demonstrated

### Microservices Patterns
- Event-driven architecture
- Asynchronous communication (Kafka)
- Service isolation
- Domain-driven design

### Resilience Patterns
- Circuit breaker (Resilience4j)
- Retry logic with exponential backoff
- Dead Letter Queue (DLQ)
- Distributed caching (Redis)

### DevOps Excellence
- Infrastructure as Code (Terraform)
- Containerization (Docker)
- Automated CI/CD (GitHub Actions)
- Security scanning
- Health checks & monitoring

### Software Quality
- 91% code coverage
- Comprehensive unit and integration testing
- Clean code principles
- SOLID principles

---

## Why This Project?

This portfolio project demonstrates:

1. **Production-Ready Code**
   - Not a tutorial copy-paste
   - Real-world patterns and best practices
   - Comprehensive error handling

2. **Enterprise Architecture**
   - Scalable microservices
   - Event-driven design
   - Cloud-native deployment

3. **DevOps Expertise**
   - Full CI/CD automation
   - Infrastructure as Code
   - Multi-environment setup

4. **Quality Focus**
   - High test coverage (91%)
   - Comprehensive testing strategy
   - Security scanning

5. **Cloud Deployment**
   - Real AWS infrastructure
   - Cost-optimized
   - Production-ready

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Contact

**Julius Lapugot**

- GitHub: [@jlapugot](https://github.com/jlapugot)
- LinkedIn: [linkedin.com/in/julius-cessar-lapugot-a31985155](https://linkedin.com/in/julius-cessar-lapugot-a31985155)
- Email: juliusclapugot@gmail.com

**Project Link**: [https://github.com/jlapugot/order-processing-system](https://github.com/jlapugot/order-processing-system)

---

## Acknowledgments

This project showcases modern software engineering practices for portfolio purposes. Built with passion to demonstrate technical expertise in Java, Spring Boot, Kafka, AWS, and DevOps.

**Tech Stack Selection Rationale:**
- Java 21: Latest LTS, modern features
- Spring Boot 3.x: Industry standard
- Kafka: Event streaming excellence
- AWS: Enterprise cloud platform
- Terraform: Infrastructure automation

**Open for opportunities!** If you're looking for a developer with experience in microservices, cloud deployment, and DevOps practices, let's connect!
