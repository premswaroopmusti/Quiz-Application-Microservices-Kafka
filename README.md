# Quiz Application – Microservices Architecture with Apache Kafka

A **Java Spring Boot Quiz Application** that evolved from a **monolithic application** into a **distributed microservices architecture** and was further extended with **Apache Kafka for asynchronous event-driven communication.**

The application separates **quiz management** and **question management** into independent microservices. It uses **Spring Cloud OpenFeign** for synchronous inter-service communication, **Netflix Eureka** for service discovery, and **Spring Cloud API Gateway** as a common entry point.

**Apache Kafka** is integrated to introduce **asynchronous communication** between Quiz Service and a newly developed Quiz History Service. Whenever a quiz is successfully completed, Quiz Service publishes a QuizCompletedEvent to Kafka. Quiz History Service consumes the event and independently stores the completed quiz details in PostgreSQL.

This project demonstrates both **synchronous** and **asynchronous communication** patterns in a Spring Boot microservices architecture.


## Table of Contents
- Project Evolution
- Architecture
- Kafka Integration
- Services
- Technology Stack
- Database Design
- API Gateway Routes
- API Documentation
  - Create a Quiz
  - Get Quiz Questions
  - Submit Quiz Answers
  - Question Service APIs
- Application Flow
- Load Balancing
- Run Locally
- Configuration
- Concepts Demonstrated
- Current Scope and Notes
- Future Enhancements
- Author



## Project Evolution
### 1. Monolithic Version -- QuizApp
The application was initially developed as a single Spring Boot application containing:
- Question Controller and Question Service
- Quiz Controller and Quiz Service
- Question and Quiz repositories
- Question, Quiz, QuestionWrapper, and Response models

Quiz creation, question retrieval, and score calculation were handled within the same application.

### 2. Microservices Version
The monolithic application was used as the reference for a separate microservices implementation.

Responsibilities were divided into independent services:

**Question Service** : Manages questions and performs score calculation.

**Quiz Service** : Creates quizzes, stores question IDs, retrieves quiz questions, and submits answers.

**Eureka Service Registry** : Provides service registration and discovery.

**API Gateway** : Provides a common entry point for client requests.

The services initially communicated synchronously using Spring Cloud OpenFeign and REST APIs.

### 3. Kafka Integration – Event-Driven Extension

The existing microservices application was extended with Apache Kafka.

A new Quiz History Service was introduced to demonstrate asynchronous communication.

The new workflow:

1. A user submits answers for a quiz.

2. Quiz Service requests score calculation from Question Service.

3. After receiving a successful score response, Quiz Service publishes a QuizCompletedEvent to Kafka.

4. Kafka makes the event available to subscribed consumers.

5. Quiz History Service consumes the event.

6. The consumer converts the event into a database entity and saves it in its own PostgreSQL database.

7. Quiz History Service exposes REST APIs to retrieve stored quiz history.

The existing score calculation and response flow remain synchronous. Kafka is used for the additional history-processing workflow.


## Architecture



                         Client / Postman
                                |
                                | HTTP
                                v
                    +------------------------+
                    |      API Gateway       |
                    |       Port: 8765       |
                    +------------------------+
                         |             |
              /quiz-service/**   /quiz-history/**
                         |             |
                         v             v
                +---------------+  +--------------------+
                | Quiz Service  |  | Quiz History       |
                | Port: 8090    |  | Service            |
                +---------------+  | Port: 8091         |
                         |         +--------------------+
                         |                 ^
                         |                 |
              OpenFeign  |                 | Kafka Consumer
                         |                 |
                         v                 |
                +----------------+    +------------------+
                | Question       |    | Apache Kafka     |
                | Service        |    | Port: 9092       |
                | 8080 / 8081    |    |                  |
                +----------------+    | quiz-completed   |
                         |            | topic            |
                         v            +------------------+
                  +-------------+           ^
                  | questiondb  |           |
                  | PostgreSQL  |           |
                  +-------------+           |
                                            |
                  +-------------+           |
                  | quizdb      |           |
                  | PostgreSQL  |           |
                  +-------------+           |
                                            |
                                    Kafka Producer
                                            |
                                      Quiz Service

                +-----------------------------+
                |      quizhistorydb          |
                |        PostgreSQL           |
                +-----------------------------+

                +-----------------------------+
                |    Eureka Service Registry  |
                |         Port: 8761          |
                +-----------------------------+

#### Service discovery:
The following services register with Eureka:
- API Gateway
- Quiz Service
- Question Service
- Quiz History Service

Quiz Service uses the logical service name QUESTION-SERVICE through OpenFeign instead of hardcoding the Question Service host and port.

API Gateway uses service discovery to route requests to the appropriate service.

### Communication Patterns

<img width="910" height="392" alt="image" src="https://github.com/user-attachments/assets/64839b5d-0ddd-439c-81c5-66af04b1e5a0" />

## 3. Kafka Integration

This is the primary extension added to the existing microservices application.

**3.1 Why Apache Kafka?**

Previously, Quiz Service handled quiz operations and returned the calculated score to the client.

Now, whenever a quiz is successfully completed, Quiz Service additionally publishes an event to Kafka.

Quiz History Service independently processes that event and stores the quiz completion details.

This introduces asynchronous event-driven communication without making the existing score calculation dependent on the History Service.

**3.2 Kafka Topic**

The application uses the following Kafka topic:
```

quiz-completed
```

Quiz Service publishes messages to this topic, and Quiz History Service subscribes to it.

**3.3 Kafka Producer – Quiz Service**

A Kafka producer was implemented inside Quiz Service using Spring Kafka and KafkaTemplate.

Main responsibilities:

- Create a quiz completion event after successful score calculation.

- Generate a unique event ID.

- Include quiz completion details in the event.

- Publish the event to the quiz-completed topic.

The producer uses:
```
KafkaTemplate<String, QuizCompletedEvent>
```

The event is published using its event ID as the Kafka message key.

**3.4 QuizCompletedEvent**

<img width="675" height="445" alt="image" src="https://github.com/user-attachments/assets/9a272ec4-c83a-4db6-9d20-c0cbccb5bfe2" />

Example Kafka message:
```
{
  "eventId": "0667a5b2-28ad-4690-8598-774f0b077e72",
  "quizId": 1,
  "score": 5,
  "totalQuestions": 5,
  "completedAt": [2026, 11, 9, 18, 48, 34, 661931400]
}
```
The event is serialized into JSON before being published to Kafka.

**3.5 Kafka Consumer – Quiz History Service**

A new Spring Boot microservice named Quiz History Service was created to consume Kafka events.

It uses Spring Kafka's @KafkaListener.
```
@KafkaListener(
    topics = "quiz-completed",
    groupId = "quiz-history-group"
)
public void consumeQuizCompletedEvent(QuizCompletedEvent event) {
    // Process and save quiz history
}
```

Consumer responsibilities:
1. Listen to the quiz-completed topic.

2. Receive and deserialize the event.

3. Check whether the event ID has already been processed.

4. Convert the event into a QuizHistory entity.

5. Persist the record in PostgreSQL.

**3.6 Quiz History Persistence**

Quiz History Service maintains its own PostgreSQL database.

Database:
```
quizhistorydb
```

Table:
```
quiz_history

```

<img width="887" height="552" alt="image" src="https://github.com/user-attachments/assets/220cac6c-4ac4-4825-bd88-b837ac0ccc63" />

**3.7 Kafka Consumer Configuration**

The consumer uses the following configuration:

```
spring.kafka.bootstrap-servers=localhost:9092

spring.kafka.consumer.group-id=quiz-history-group
spring.kafka.consumer.auto-offset-reset=earliest

spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer

spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer

spring.kafka.consumer.properties.spring.json.trusted.packages=com.example.quiz_history_service.event

spring.kafka.consumer.properties.spring.json.value.default.type=com.example.quiz_history_service.event.QuizCompletedEvent

spring.kafka.consumer.properties.spring.json.use.type.headers=false
```

The consumer group is named quiz-history-group.

The earliest offset configuration allows a new consumer group to read from the earliest available topic offset when it has no committed offset.


## Services
### 1. Question Service

Question Service is responsible for managing question data and
performing question-related operations.

**Responsibilities**
- Retrieve all questions.
- Retrieve questions by category.
- Add questions.
- Select a requested number of random question IDs for a category.
- Retrieve question details for a supplied list of IDs.
- Calculate the score by comparing submitted answers with the correct
  answers.
  

**Question model** 

The Question entity contains:
- id
- questionTitle
- option1
- option2
- option3
- option4
- rightAnswer
- difficultylevel
- category
  
**Question response model**

QuestionWrapper is used to return question details and the four
options without exposing the rightAnswer field.

**Persistence**
- Database: PostgreSQL

Two Question Service instances were started on ports 8080 and 8081 to test load balancing.


### 2. Quiz Service

Quiz Service manages quiz creation and quiz-related operations.


**Responsibilities**
- Create a quiz using a category, number of questions, and title.
- Store the generated question IDs with the quiz.
- Retrieve quiz questions using a quiz ID.
- Submit answers and request score calculation from Question Service.
- Publish quiz completion events to Kafka after successful score calculation.


**Quiz model**

The Quiz entity contains:
- id
- title
- questionIds

  The question IDs are stored using JPA @ElementCollection.
  

**Persistence**
- Database: PostgreSQL

**OpenFeign integration**

Quiz Service communicates with Question Service through:

```
@FeignClient("QUESTION-SERVICE")
public interface QuizInterface {
    // Question Service API mappings
}
```

The Feign client calls the Question Service to generate question IDs,
retrieve question details, and calculate scores.

Kafka integration

Quiz Service also contains:

- QuizCompletedEvent

- QuizProducer

After successful score calculation, the producer publishes the completion event to Kafka.

Persistence: PostgreSQL (quizdb).

### 3. Quiz History Service

Quiz History Service is the newly introduced Kafka consumer microservice.

Responsibilities:

- Subscribe to the quiz-completed Kafka topic.

- Consume quiz completion events.

- Deserialize JSON messages into Java event objects.

- Check for previously processed event IDs.

- Store completed quiz details in its own PostgreSQL database.

- Provide REST APIs for retrieving quiz history.

Main components:

- QuizHistoryServiceApplication

- QuizCompletedEvent

- QuizHistory

- QuizHistoryRepository

- QuizHistoryConsumer

- QuizHistoryController

Persistence: PostgreSQL (quizhistorydb).


### 4. Eureka Service Registry

The Service Registry provides a central place for service registration and discovery.

Implementation:

- Spring Cloud Netflix Eureka Server.

- Enabled using @EnableEurekaServer.

Dashboard:
```

http://localhost:8761
```
The dashboard displays registered application instances and their status.

### 5. API Gateway

The API Gateway provides a single entry point for client requests.

Implementation:

- Spring Cloud Gateway Server MVC.

- Eureka Client for service discovery.

It contains two configured routes:

Quiz Service route
```
/quiz-service/**
```

Quiz History Service route
```
/quiz-history/**
```
Both routes use lb:// service URIs and StripPrefix=1.

## Technology Stack

- **Language :** Java 17

- **Framework :** Spring Boot 4.1.2

- **Microservices :** Spring Cloud

- **API development :** Spring Web MVC, REST APIs

- **API Gateway :** Spring Cloud Gateway Server MVC

- **Service discovery :** Spring Cloud Netflix Eureka Server and Client
- **Synchronous Communication :** Spring Cloud OpenFeign
- **Asynchronous Communication :** Apache Kafka
- **Kafka Integration :** Spring for Apache Kafka

- **Message Serialization :** JSON / Jackson

- **Load balancing :** Spring Cloud LoadBalancer (through service discovery integration)

- **Persistence :** Spring Data JPA, Hibernate

- **Databases :** PostgreSQL

- **Boilerplate reduction :** Project Lombok

- **Build and dependency management :** Maven

- **API testing :** Postman

- **Database administration :** pgAdmin 4

- **IDE :** IntelliJ IDEA

## Database Design
<img width="822" height="347" alt="image" src="https://github.com/user-attachments/assets/827975e1-79fd-4aee-936d-ac8ef3369dad" />


## API Gateway Routes

<img width="905" height="746" alt="image" src="https://github.com/user-attachments/assets/4b9978ad-0d58-4f89-92dd-1086cdc86618" />

## API Documentation
Quiz Service and Question Service APIs

<img width="757" height="747" alt="image" src="https://github.com/user-attachments/assets/40695f60-970e-4c19-afbc-0d0712a0fac1" />
<img width="755" height="802" alt="image" src="https://github.com/user-attachments/assets/2600263d-6f87-4796-8561-bb9b253b908a" />
<img width="747" height="906" alt="image" src="https://github.com/user-attachments/assets/0d58a910-68b5-4f01-8efb-2546ac379b1a" />
<img width="531" height="232" alt="image" src="https://github.com/user-attachments/assets/d2718cfa-7ff4-44e7-8431-b92bcd0404fd" />


## Application Flow


<img width="927" height="691" alt="image" src="https://github.com/user-attachments/assets/1a4ea099-4cc5-4a6b-8e56-7db1838bd391" />


## Load Balancing

Question Service was started as two instances:

- Instance 1: localhost:8080

- Instance 2: localhost:8081

Eureka registers both instances under the same service name.

Quiz Service uses OpenFeign with service discovery, allowing requests to be distributed across available Question Service instances through Spring Cloud LoadBalancer.

<img width="727" height="717" alt="image" src="https://github.com/user-attachments/assets/c0bbe546-d7c0-47a3-9ae1-75b49ecab243" />

<img width="731" height="487" alt="image" src="https://github.com/user-attachments/assets/023eb1f4-2758-426c-a6ae-20c458f13537" />



## Concepts Demonstrated
<img width="467" height="800" alt="image" src="https://github.com/user-attachments/assets/702b4f6e-519f-4204-83c0-28352e9367be" />
<img width="330" height="256" alt="image" src="https://github.com/user-attachments/assets/a833a26c-60c6-4358-af77-f733f0fd3b0a" />



## Current Scope and Future Enhancements
The current implementation demonstrates Kafka-based quiz completion event publishing and history persistence.

Potential future improvements:

- Implement reliable Kafka publishing and failure handling.

- Add retry mechanisms and Dead Letter Topics.

- Explore Kafka acknowledgements and delivery guarantees.

- Implement the Transactional Outbox pattern.

- Add student/user identification to quiz completion events.

- Add pagination and sorting to quiz history APIs.

- Add centralized configuration management.

- Add distributed tracing and centralized logging.

- Add health checks, metrics, and monitoring.

- Add circuit breakers, timeouts, and retry policies for synchronous inter-service calls.

## Author
Prem Swaroop
