# Bookstore A2
Java Spring Boot implementation of **17-647 Assignment A2: BFF, JWT & Multiple Services per SRP**.
This project evolves the A1 bookstore system into a **four-service microservice architecture**:
- `book-service`
- `customer-service`
- `web-bff`
- `mobile-bff`

The backend services expose book and customer APIs, while the two BFF services sit in front of them and enforce:
- client-type based behavior using `X-Client-Type`
- JWT-based authorization using the `Authorization: Bearer <token>` header
- client-specific response shaping for web vs mobile

---

## Table of Contents
1. Overview
2. Architecture
3. Services
4. Tech Stack
5. Implemented Requirements
6. Project Structure
7. API Overview
8. JWT Rules
9. BFF Behavior
10. Environment Variables
11. Local Development Setup
12. Local Run Without Docker
13. Local Run With Docker
14. Useful Test Commands
15. Database
16. AWS Deployment Notes
17. Error Handling
18. Important Notes

---

## 1. Overview

This repository implements Assignment A2 for the bookstore system.

Compared to A1, the system is now split into multiple services to follow **Single Responsibility Principle (SRP)** and the **Backend For Frontends (BFF)** pattern:

- **Book service** handles `/books`
- **Customer service** handles `/customers`
- **Web BFF** accepts only `X-Client-Type: Web`
- **Mobile BFF** accepts only `X-Client-Type: iOS` or `X-Client-Type: Android`

The BFF services validate JWT tokens and forward requests to backend services.

---

## 2. Architecture

The system is split into four Spring Boot applications:

- **book-service**
  - runs on port `3000`
  - exposes book APIs
  - stores/retrieves data from MySQL
  - includes LLM summary support in the codebase

- **customer-service**
  - runs on port `3000`
  - exposes customer APIs
  - stores/retrieves data from MySQL

- **web-bff**
  - runs on port `80`
  - accepts only web clients
  - validates JWT
  - forwards requests to backend services

- **mobile-bff**
  - runs on port `80`
  - accepts only iOS/Android clients
  - validates JWT
  - forwards requests to backend services
  - transforms selected responses for mobile clients

---

## 3. Services

### 3.1 book-service

Responsible only for book-related functionality.

Endpoints:
- `POST /books`
- `PUT /books/{isbn}`
- `GET /books/{isbn}`
- `GET /books/isbn/{isbn}`
- `GET /status`

### 3.2 customer-service

Responsible only for customer-related functionality.

Endpoints:
- `POST /customers`
- `GET /customers/{id}`
- `GET /customers?userId=...`
- `GET /status`

### 3.3 web-bff

Frontend-facing service for web clients.

Behavior:
- requires `X-Client-Type: Web`
- requires valid JWT in `Authorization`
- forwards `/books` and `/customers` requests to backend services
- does not modify successful backend responses

### 3.4 mobile-bff

Frontend-facing service for mobile clients.

Behavior:
- requires `X-Client-Type: iOS` or `X-Client-Type: Android`
- requires valid JWT in `Authorization`
- forwards `/books` and `/customers` requests to backend services
- modifies some successful responses:
  - for `GET /books/{isbn}` and `GET /books/isbn/{isbn}`, replaces `"genre": "non-fiction"` with `"genre": 3`
  - for `GET /customers/{id}` and `GET /customers?userId=...`, removes:
    - `address`
    - `address2`
    - `city`
    - `state`
    - `zipcode`

---

## 4. Tech Stack

- Java 21
- Spring Boot
- Maven
- MySQL
- Docker
- AWS EC2
- AWS Application Load Balancer
- AWS RDS / Aurora MySQL
- CloudFormation

---

## 5. Implemented Requirements

This repository implements the A2 requirements:
- split system into **4 microservices**
- separate **book** and **customer** responsibilities
- support **two BFFs**
- require `X-Client-Type` header
- require `Authorization` header with a valid JWT
- decode JWT payload and validate claims
- support web and mobile client-specific handling
- expose `/status` on all services
- use separate Dockerfiles for each microservice

---

## 6. Project Structure

```text
bookstore-2/
├── aws/
│   └── CF-A2-cmu.yml
├── database/
│   ├── schema.sql
│   └── seed.sql
├── services/
│   ├── book-service/
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/
│   ├── customer-service/
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/
│   ├── mobile-bff/
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/
│   └── web-bff/
│       ├── Dockerfile
│       ├── pom.xml
│       └── src/
├── .dockerignore
├── .gitignore
├── README.md
└── url.txt
```

---

## 7. API Overview

### 7.1 Health Check

```
GET /status
```

Returns plain text health response.

**Response:**
```
OK
```

### 7.2 Book APIs

These are exposed by `book-service`, and also proxied by both BFFs.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/books` | Creates a new book |
| `PUT` | `/books/{isbn}` | Updates an existing book |
| `GET` | `/books/{isbn}` | Returns book details |
| `GET` | `/books/isbn/{isbn}` | Alternative path to fetch the same book by ISBN |

### 7.3 Customer APIs

These are exposed by `customer-service`, and also proxied by both BFFs.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/customers` | Creates a new customer |
| `GET` | `/customers/{id}` | Returns a customer by numeric ID |
| `GET` | `/customers?userId=...` | Returns a customer by userId |

---

## 8. JWT Rules

All BFF requests require:

```
Authorization: Bearer <token>
```

The JWT validation in this project checks the decoded payload and enforces:

- `sub` must be one of:
  - `starlord`
  - `gamora`
  - `drax`
  - `rocket`
  - `groot`
- `iss` must equal: `cmu.edu`
- `exp` must be present, numeric, and in the future

If the `Authorization` header is missing or invalid, the BFF returns:

```
401 Unauthorized
```

---

## 9. BFF Behavior

### 9.1 X-Client-Type Rules

**Web BFF** — Accepted header:
```
X-Client-Type: Web
```

**Mobile BFF** — Accepted headers:
```
X-Client-Type: iOS
```
or
```
X-Client-Type: Android
```

If `X-Client-Type` is missing or invalid, the BFF returns:
```
400 Bad Request
```

### 9.2 Mobile Response Transformations

**Book responses**

For `GET /books/{isbn}` and `GET /books/isbn/{isbn}`:

If the backend response contains:
```json
{
  "genre": "non-fiction"
}
```
the mobile BFF returns:
```json
{
  "genre": 3
}
```

**Customer responses**

For `GET /customers/{id}` and `GET /customers?userId=...`:

The mobile BFF removes these fields:
- `address`
- `address2`
- `city`
- `state`
- `zipcode`

---

## 10. Environment Variables

### 10.1 book-service

| Variable | Default |
|----------|---------|
| `SERVER_PORT` | `3000` |
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `bookstore` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | `rootpassword` |
| `LLM_PROVIDER` | `gemini` |
| `LLM_API_KEY` | *(empty)* |
| `LLM_MODEL` | `gemini-1.5-flash` |
| `LLM_TIMEOUT_MS` | `15000` |

### 10.2 customer-service

| Variable | Default |
|----------|---------|
| `SERVER_PORT` | `3000` |
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `bookstore` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | `rootpassword` |

### 10.3 web-bff

| Variable | Default |
|----------|---------|
| `SERVER_PORT` | `80` |
| `URL_BASE_BACKEND_SERVICES` | `http://localhost:3000` |

### 10.4 mobile-bff

| Variable | Default |
|----------|---------|
| `SERVER_PORT` | `80` |
| `URL_BASE_BACKEND_SERVICES` | `http://localhost:3000` |

---

## 11. Local Development Setup

### Prerequisites
- Java 21
- Maven
- MySQL 8+
- Docker
- Git

### Database Setup

Create a MySQL database named `bookstore`, then run:
```
database/schema.sql
database/seed.sql
```

---

## 12. Local Run Without Docker

Open four terminals.

**Terminal 1: book-service**
```bash
cd services/book-service
mvn spring-boot:run
```

**Terminal 2: customer-service**
```bash
cd services/customer-service
mvn spring-boot:run
```

**Terminal 3: web-bff**
```bash
cd services/web-bff
mvn spring-boot:run
```

**Terminal 4: mobile-bff**
```bash
cd services/mobile-bff
mvn spring-boot:run
```

Set environment variables before startup as needed, especially:
- `DB_*` variables for backend services
- `URL_BASE_BACKEND_SERVICES` for BFF services

---

## 13. Local Run With Docker

Build images from each service directory.

**book-service**
```bash
cd services/book-service
docker build -t bookstore-book-service .
```

**customer-service**
```bash
cd services/customer-service
docker build -t bookstore-customer-service .
```

**web-bff**
```bash
cd services/web-bff
docker build -t bookstore-web-bff .
```

**mobile-bff**
```bash
cd services/mobile-bff
docker build -t bookstore-mobile-bff .
```

### Example Run Commands

**book-service**
```bash
docker run --rm -p 3000:3000 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=bookstore \
  -e DB_USER=root \
  -e DB_PASSWORD=rootpassword \
  bookstore-book-service
```

**customer-service**
```bash
docker run --rm -p 3001:3000 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=bookstore \
  -e DB_USER=root \
  -e DB_PASSWORD=rootpassword \
  bookstore-customer-service
```

**web-bff**
```bash
docker run --rm -p 8080:80 \
  -e URL_BASE_BACKEND_SERVICES=http://host.docker.internal:3000 \
  bookstore-web-bff
```

**mobile-bff**
```bash
docker run --rm -p 8081:80 \
  -e URL_BASE_BACKEND_SERVICES=http://host.docker.internal:3000 \
  bookstore-mobile-bff
```

> **Note:** For full local behavior, the BFF base URL should point to the backend routing destination you want to test.

---

## 14. Useful Test Commands

**Health check**
```bash
curl http://localhost:8080/status
curl http://localhost:8081/status
```

**Example Authorization header**
```
Bearer <your-jwt-token>
```

**Create customer through web BFF**
```bash
curl -X POST http://localhost:8080/customers \
  -H "Content-Type: application/json" \
  -H "X-Client-Type: Web" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "userId":"star+lord@gmail.com",
    "name":"Star Lord",
    "phone":"+14125550111",
    "address":"5000 Forbes Ave",
    "address2":"Apt 1",
    "city":"Pittsburgh",
    "state":"PA",
    "zipcode":"15213"
  }'
```

**Get customer through mobile BFF**
```bash
curl "http://localhost:8081/customers?userId=star%2Blord%40gmail.com" \
  -H "X-Client-Type: Android" \
  -H "Authorization: Bearer <token>"
```

**Get book through mobile BFF**
```bash
curl http://localhost:8081/books/9780134685991 \
  -H "X-Client-Type: Android" \
  -H "Authorization: Bearer <token>"
```

---

## 15. Database

The repository contains:

- `database/schema.sql` — creates `books` and `customers` tables
- `database/seed.sql` — inserts one sample book and one sample customer

---

## 16. AWS Deployment Notes

The repository includes: `aws/CF-A2-cmu.yml`

This project is intended to be deployed with:
- public-facing ALB for client routing
- internal ALB for BFF-to-backend routing
- EC2 instances
- MySQL-compatible AWS database setup

Per the assignment architecture:

| Service | Port |
|---------|------|
| web BFF | `80` |
| mobile BFF | `80` |
| customer service | `3000` |
| book service | `3000` |

---

## 17. Error Handling

The services use structured exception handling and return appropriate HTTP status codes.

| Status Code | Cause |
|-------------|-------|
| `400 Bad Request` | Missing or invalid `X-Client-Type`; malformed input; missing required query parameters |
| `401 Unauthorized` | Missing `Authorization` header; invalid JWT; expired JWT |
| `404 Not Found` | Missing book or customer |
| `409 Conflict` | Duplicate customer `userId`; duplicate book ISBN |
| `500 Internal Server Error` | Unexpected server-side failure |