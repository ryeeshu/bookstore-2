# Bookstore A1

Java Spring Boot implementation of the Bookstore Assignment.

This project provides a REST API for managing books and customers, integrates with an LLM API to generate book summaries, runs locally with MySQL, and is deployed on AWS using Docker, EC2, Application Load Balancer, and Aurora MySQL.

---

## Table of Contents

1. [Tech Stack](#1-tech-stack)
2. [Features](#2-features)
3. [Project Structure](#3-project-structure)
4. [API Overview](#4-api-overview)
5. [Validation Rules](#5-validation-rules)
6. [Database Schema](#6-database-schema)
7. [Local Development Setup](#7-local-development-setup)
8. [Local Run Without Docker](#8-local-run-without-docker)
9. [Local Run With Docker](#9-local-run-with-docker)
10. [Useful Local Test Commands](#10-useful-local-test-commands)
11. [Docker Hub Workflow](#11-docker-hub-workflow)
12. [AWS Deployment Overview](#12-aws-deployment-overview)
13. [AWS Deployment Steps](#13-aws-deployment-steps)
14. [Testing on AWS](#14-testing-on-aws)
15. [Important Notes for Grading](#15-important-notes-for-grading)
16. [LLM Summary Behavior](#16-llm-summary-behavior)
17. [Error Handling](#17-error-handling)
18. [Files Used for AWS](#18-files-used-for-aws)

---

## 1. Tech Stack

- Java 21
- Spring Boot
- Maven
- MySQL
- Docker
- Docker Hub
- AWS EC2
- AWS Application Load Balancer
- AWS Aurora MySQL
- CloudFormation

---

## 2. Features

### Book APIs
- `POST /books`
- `PUT /books/{isbn}`
- `GET /books/{isbn}`
- `GET /books/isbn/{isbn}`

### Customer APIs
- `POST /customers`
- `GET /customers/{id}`
- `GET /customers?userId=...`

### Health Check
- `GET /status`

### LLM Summary
- A summary is generated for each book using an LLM API.
- `POST /books` creates the book without returning the summary.
- `GET /books/...` returns the book including the summary.

---

## 3. Project Structure

```text
bookstore-a1/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── edu/cmu/bookstore/
│   │   │       ├── BookstoreApplication.java
│   │   │       ├── controller/
│   │   │       │   ├── BookController.java
│   │   │       │   ├── CustomerController.java
│   │   │       │   └── StatusController.java
│   │   │       ├── service/
│   │   │       │   ├── BookService.java
│   │   │       │   ├── CustomerService.java
│   │   │       │   └── LlmService.java
│   │   │       ├── repository/
│   │   │       │   ├── BookRepository.java
│   │   │       │   └── CustomerRepository.java
│   │   │       ├── model/
│   │   │       │   ├── ApiMessage.java
│   │   │       │   ├── Book.java
│   │   │       │   ├── BookResponse.java
│   │   │       │   ├── BookDetailsResponse.java
│   │   │       │   ├── Customer.java
│   │   │       │   └── request/
│   │   │       │       ├── CreateBookRequest.java
│   │   │       │       ├── UpdateBookRequest.java
│   │   │       │       └── CreateCustomerRequest.java
│   │   │       ├── validation/
│   │   │       │   ├── BookValidator.java
│   │   │       │   ├── CustomerValidator.java
│   │   │       │   └── UsStates.java
│   │   │       ├── exception/
│   │   │       │   ├── BadRequestException.java
│   │   │       │   ├── ConflictException.java
│   │   │       │   ├── NotFoundException.java
│   │   │       │   └── GlobalExceptionHandler.java
│   │   │       ├── config/
│   │   │       │   ├── AppConfig.java
│   │   │       │   └── AsyncConfig.java
│   │   │       └── util/
│   │   │           ├── JsonUtil.java
│   │   │           └── PriceUtil.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── schema.sql
│   │       └── seed.sql
│   └── test/
├── Dockerfile
├── .dockerignore
├── .gitignore
├── pom.xml
├── run-local.sh
├── run-docker-local.sh
├── aws/
│   ├── CF-A1-cmu.yml
│   ├── create_tables.sql
│   ├── seed_data.sql
│   ├── cleanup.sql
│   ├── ec2-runbook.md
│   └── bookstore.env.example
└── url.txt
```

---

## 4. API Overview

### 4.1 GET /status

Health-check endpoint.

**Response:** `200 OK`

```
OK
```

> This endpoint is also used by the AWS ALB health check.

---

### 4.2 POST /books

Creates a new book.

**Request body:**
```json
{
  "ISBN": "978-0134685991",
  "title": "Effective Java",
  "Author": "Joshua Bloch",
  "description": "A practical guide to best practices in Java programming.",
  "genre": "non-fiction",
  "price": 45.99,
  "quantity": 10
}
```

**Success response:** `201 Created`  
`Location: /books/{ISBN}`

> The POST response does **not** include `summary`.

```json
{
  "ISBN": "978-0134685991",
  "title": "Effective Java",
  "Author": "Joshua Bloch",
  "description": "A practical guide to best practices in Java programming.",
  "genre": "non-fiction",
  "price": 45.99,
  "quantity": 10
}
```

**Error cases:**
- `400 Bad Request` — invalid input
- `422 Unprocessable Entity` — ISBN already exists

---

### 4.3 PUT /books/{isbn}

Updates an existing book.

**Request body:**
```json
{
  "ISBN": "978-0134685991",
  "title": "Effective Java 3rd Edition",
  "Author": "Joshua Bloch",
  "description": "An updated practical guide to best practices in Java programming.",
  "genre": "non-fiction",
  "price": 49.99,
  "quantity": 12
}
```

**Success response:** `200 OK` (body does not include `summary`)

**Error cases:**
- `400 Bad Request` — invalid input or path ISBN and body ISBN do not match
- `404 Not Found` — book does not exist

---

### 4.4 GET /books/{isbn}

Retrieves a book by ISBN. Response includes `summary`.

**Success response:** `200 OK`

```json
{
  "ISBN": "978-0134685991",
  "title": "Effective Java",
  "Author": "Joshua Bloch",
  "description": "A practical guide to best practices in Java programming.",
  "genre": "non-fiction",
  "price": 45.99,
  "quantity": 10,
  "summary": "..."
}
```

**Error cases:** `400 Bad Request`, `404 Not Found`

---

### 4.5 GET /books/isbn/{isbn}

Alternative path for retrieving a book by ISBN. Returns the same response as `GET /books/{isbn}`.

---

### 4.6 POST /customers

Creates a new customer.

**Request body:**
```json
{
  "userId": "sampleuser@gmail.com",
  "name": "Sample User",
  "phone": "+14125550111",
  "address": "5000 Forbes Ave",
  "address2": "Apt 1",
  "city": "Pittsburgh",
  "state": "PA",
  "zipcode": "15213"
}
```

**Success response:** `201 Created`  
`Location: /customers/{id}`

```json
{
  "id": 1,
  "userId": "sampleuser@gmail.com",
  "name": "Sample User",
  "phone": "+14125550111",
  "address": "5000 Forbes Ave",
  "address2": "Apt 1",
  "city": "Pittsburgh",
  "state": "PA",
  "zipcode": "15213"
}
```

**Error cases:**
- `400 Bad Request`
- `422 Unprocessable Entity` — userId already exists

---

### 4.7 GET /customers/{id}

Retrieves a customer by numeric ID.

**Error cases:**
- `400 Bad Request` — invalid ID format
- `404 Not Found` — customer does not exist

---

### 4.8 GET /customers?userId=...

Retrieves a customer by userId.

```
/customers?userId=sampleuser@gmail.com
```

**Error cases:** `400 Bad Request`, `404 Not Found`

---

## 5. Validation Rules

### Books
- All fields are required
- `price` must be non-negative
- `price` must have at most 2 decimal places

### Customers
- All fields are required except `address2`
- `userId` must be a valid email
- `state` must be a valid 2-letter US state abbreviation

---

## 6. Database Schema

Two tables are used:

**books** — `isbn` as primary key; stores title, author, description, genre, price, quantity, summary

**customers** — `id` as auto-increment primary key; `user_id` as unique field

Schema files:
- Local: `src/main/resources/schema.sql`
- AWS: `aws/create_tables.sql`

---

## 7. Local Development Setup

### 7.1 Prerequisites

- Java 21
- Maven
- Git
- Docker Desktop
- MySQL
- MySQL Workbench or MySQL CLI
- VS Code or IntelliJ

### 7.2 Create local database

In MySQL:

```sql
CREATE DATABASE IF NOT EXISTS bookstore;
USE bookstore;
```

Then run: `src/main/resources/schema.sql`

Optional for manual testing: `src/main/resources/seed.sql`

---

## 8. Local Run Without Docker

Set environment variables (PowerShell):

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="bookstore"
$env:DB_USER="root"
$env:DB_PASSWORD="your_mysql_password"
$env:SERVER_PORT="8080"
```

If using a real LLM API:

```powershell
$env:LLM_API_KEY="your_api_key"
$env:LLM_MODEL="gemini-1.5-flash"
$env:LLM_TIMEOUT_MS="15000"
```

Run:

```bash
mvn clean install
mvn spring-boot:run
```

Test:

```bash
curl http://localhost:8080/status
# Expected: OK
```

---

## 9. Local Run With Docker

The application runs inside Docker, but MySQL remains outside the container.

**Build image:**

```bash
docker build -t bookstore-a1:latest .
```

**Run container:**

```bash
docker run --rm -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=bookstore \
  -e DB_USER=root \
  -e DB_PASSWORD=your_mysql_password \
  -e SERVER_PORT=8080 \
  bookstore-a1:latest
```

With LLM API:

```bash
docker run --rm -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=bookstore \
  -e DB_USER=root \
  -e DB_PASSWORD=your_mysql_password \
  -e SERVER_PORT=8080 \
  -e LLM_API_KEY=your_api_key \
  -e LLM_MODEL=gemini-1.5-flash \
  bookstore-a1:latest
```

---

## 10. Useful Local Test Commands

**Health check:**
```bash
curl http://localhost:8080/status
```

**Create book:**
```bash
curl -X POST http://localhost:8080/books \
  -H "Content-Type: application/json" \
  -d '{
    "ISBN":"9780134685991",
    "title":"Effective Java",
    "Author":"Joshua Bloch",
    "description":"A practical guide to best practices in Java programming.",
    "genre":"non-fiction",
    "price":45.99,
    "quantity":10
  }'
```

**Get book:**
```bash
curl http://localhost:8080/books/9780134685991
```

**Update book:**
```bash
curl -X PUT http://localhost:8080/books/9780134685991 \
  -H "Content-Type: application/json" \
  -d '{
    "ISBN":"9780134685991",
    "title":"Effective Java 3rd Edition",
    "Author":"Joshua Bloch",
    "description":"An updated practical guide to best practices in Java programming.",
    "genre":"non-fiction",
    "price":49.99,
    "quantity":12
  }'
```

**Create customer:**
```bash
curl -X POST http://localhost:8080/customers \
  -H "Content-Type: application/json" \
  -d '{
    "userId":"sampleuser@gmail.com",
    "name":"Sample User",
    "phone":"+14125550111",
    "address":"5000 Forbes Ave",
    "address2":"Apt 1",
    "city":"Pittsburgh",
    "state":"PA",
    "zipcode":"15213"
  }'
```

**Get customer by ID:**
```bash
curl http://localhost:8080/customers/1
```

**Get customer by userId:**
```bash
curl "http://localhost:8080/customers?userId=sampleuser@gmail.com"
```

---

## 11. Docker Hub Workflow

**Login:**
```bash
docker login
```

**Tag image:**
```bash
docker tag bookstore-a1:latest <your-dockerhub-username>/bookstore-a1:latest
```

**Push image:**
```bash
docker push <your-dockerhub-username>/bookstore-a1:latest
```

---

## 12. AWS Deployment Overview

The deployment uses:
- CloudFormation template: `aws/CF-A1-cmu.yml`
- 2 EC2 instances
- 1 Application Load Balancer
- Aurora MySQL cluster
- Dockerized application image from Docker Hub

**High-level flow:**
1. Create AWS stack using CloudFormation
2. Get EC2 instance information, ALB DNS, and Aurora writer endpoint
3. Create DB schema in Aurora
4. Create environment file on both EC2 instances
5. Pull Docker image on both EC2 instances
6. Run the Docker container on both EC2 instances
7. Test through the ALB

---

## 13. AWS Deployment Steps

### 13.1 Create the stack

Use: `aws/CF-A1-cmu.yml`

Provide: stack name, SSH location, DB username, DB password.

### 13.2 Connect to EC2

SSH into both EC2 instances created by the stack.

### 13.3 Install MySQL-compatible client on EC2

```bash
sudo dnf install mariadb105-server -y
```

### 13.4 Connect to Aurora writer endpoint

```bash
mysql -h <aurora-writer-endpoint> -u <db-username> -p
```

### 13.5 Create schema

Run the SQL from `aws/create_tables.sql`. Do **not** load seed data for final grading.

### 13.6 Create environment file on each EC2

Create `/home/ec2-user/bookstore.env`:

```env
DB_HOST=<aurora-writer-endpoint>
DB_PORT=3306
DB_NAME=bookstore
DB_USER=<db-username>
DB_PASSWORD=<db-password>
LLM_API_KEY=<llm-api-key>
LLM_MODEL=gemini-1.5-flash
LLM_TIMEOUT_MS=15000
SERVER_PORT=8080
```

Protect it:

```bash
chmod 600 /home/ec2-user/bookstore.env
```

### 13.7 Pull image

```bash
sudo docker pull <your-dockerhub-username>/bookstore-a1:latest
```

### 13.8 Run container (on both EC2 instances)

```bash
sudo docker run -d \
  --name bookstore \
  --restart unless-stopped \
  --env-file /home/ec2-user/bookstore.env \
  -p 80:8080 \
  <your-dockerhub-username>/bookstore-a1:latest
```

---

## 14. Testing on AWS

**From inside EC2:**
```bash
curl http://localhost/status
# Expected: OK
```

**Through ALB:**
```bash
curl http://<alb-dns-name>/status
```

Then test the other endpoints through the ALB DNS name.

---

## 15. Important Notes for Grading

### Keep the database clean

Before final grading, clean the Aurora DB:

```sql
USE bookstore;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE customers;
TRUNCATE TABLE books;
SET FOREIGN_KEY_CHECKS = 1;
```

Or use: `aws/cleanup.sql`

### Do not seed the AWS DB for final submission

Do **not** run `aws/seed_data.sql` for the final grading deployment.

### Ensure both EC2 instances point to the same Aurora writer endpoint

```bash
cat /home/ec2-user/bookstore.env
```

### Ensure both EC2 instances are using the latest Docker image

```bash
sudo docker stop bookstore
sudo docker rm bookstore
sudo docker pull <your-dockerhub-username>/bookstore-a1:latest
```

Then run the container again.

---

## 16. LLM Summary Behavior

- `POST /books` creates the book and attempts to generate the summary after insert (summary not returned in response)
- `GET /books/...` retrieves the summary; if missing, attempts generation again
- If no LLM API key is configured, the application returns a fallback generated summary so development and testing can still proceed

---

## 17. Error Handling

| Status Code | Meaning |
|---|---|
| `200 OK` | Success |
| `201 Created` | Resource created |
| `400 Bad Request` | Invalid input, type mismatch, missing fields |
| `404 Not Found` | Book or customer not found |
| `422 Unprocessable Entity` | Duplicate ISBN or userId |
| `500 Internal Server Error` | Unexpected server error |

---

## 18. Files Used for AWS

| File | Purpose |
|---|---|
| `aws/CF-A1-cmu.yml` | CloudFormation template |
| `aws/create_tables.sql` | DB schema |
| `aws/cleanup.sql` | DB cleanup |
| `aws/seed_data.sql` | Optional seed data |
| `aws/ec2-runbook.md` | EC2 deployment notes |
| `aws/bookstore.env.example` | Environment file example |

