# ![RealWorld Example App using Kotlin and Spring](example-logo.png)

[![Actions](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)

> ### Spring boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld-example-apps) spec and API.

This codebase was created to demonstrate a fully fledged full-stack application built with Spring boot + Mybatis including CRUD operations, authentication, routing, pagination, and more.

For more information on how to this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# *NEW* GraphQL Support  

Following some DDD principles. REST or GraphQL is just a kind of adapter. And the domain layer will be consistent all the time. So this repository implement GraphQL and REST at the same time.

The GraphQL schema is https://github.com/gothinkster/spring-boot-realworld-example-app/blob/master/src/main/resources/schema/schema.graphqls and the visualization looks like below.

![](graphql-schema.png)

And this implementation is using [dgs-framework](https://github.com/Netflix/dgs-framework) which is a quite new java graphql server framework.
# How it works

The application uses Spring Boot (Web, Mybatis).

* Use the idea of Domain Driven Design to separate the business term and infrastructure term.
* Use MyBatis to implement the [Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern for persistence.
* Use [CQRS](https://martinfowler.com/bliki/CQRS.html) pattern to separate the read model and write model.

And the code is organized as this:

1. `api` is the web layer implemented by Spring MVC
2. `core` is the business model including entities and services
3. `application` is the high-level services for querying the data transfer objects
4. `infrastructure`  contains all the implementation classes as the technique details

# Security

Integration with Spring Security and add other filter for jwt token process.

The secret key is stored in `application.properties`.

# Database

It uses a ~~H2 in-memory database~~ sqlite database (for easy local test without losing test data after every restart), can be changed easily in the `application.properties` for any other database.

# Getting started

You'll need Java 11 installed.

    ./gradlew bootRun

To test that it works, open a browser tab at http://localhost:8080/tags .  
Alternatively, you can run

    curl http://localhost:8080/tags

# Try it out with [Docker](https://www.docker.com/)

You'll need Docker installed.
	
    ./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
    docker run -p 8081:8080 spring-boot-realworld-example-app

# Try it out with a RealWorld frontend

The entry point address of the backend API is at http://localhost:8080, **not** http://localhost:8080/api as some of the frontend documentation suggests.

# Run test

The repository contains a lot of test cases to cover both api test and repository test.

    ./gradlew test

# Code format

Use spotless for code format.

    ./gradlew spotlessJavaApply

# Architecture

This application follows Domain-Driven Design (DDD) principles with a 4-layer architecture:

```
┌─────────────────────────────────────────────────────────────┐
│  API Layer (io.spring.api)                                  │
│  REST Controllers & GraphQL Resolvers                       │
│  Security filters, request validation, response formatting  │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│  Application Layer (io.spring.application)                  │
│  Query Services (reads) & Command Services (writes)         │
│  DTOs, business orchestration, authorization                │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│  Core Layer (io.spring.core)                                │
│  Domain entities: User, Article, Comment, Tag               │
│  Repository interfaces, domain services                     │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│  Infrastructure Layer (io.spring.infrastructure)            │
│  MyBatis repository implementations                         │
│  JWT service, database mappers                              │
└─────────────────────────────────────────────────────────────┘
```

The key design decisions are:

**CQRS Pattern**: Read operations (queries) are separated from write operations (commands). Query services return enriched DTOs with computed fields like `favorited` status and `favoritesCount`, while command services work directly with domain entities.

**Data Mapper Pattern**: MyBatis keeps SQL separate from domain objects. The `mapper/*.xml` files contain all SQL queries, maintaining clean separation between business logic and persistence concerns.

**API-Agnostic Domain**: Both REST and GraphQL APIs share the same application and domain layers, demonstrating that the business logic remains consistent regardless of the API type.

# Project Structure

```
src/main/java/io/spring/
├── api/                          # REST Controllers
│   ├── ArticleApi.java           # GET/PUT/DELETE /articles/{slug}
│   ├── ArticlesApi.java          # GET/POST /articles, GET /articles/feed
│   ├── ArticleFavoriteApi.java   # POST/DELETE /articles/{slug}/favorite
│   ├── CommentsApi.java          # GET/POST/DELETE /articles/{slug}/comments
│   ├── CurrentUserApi.java       # GET/PUT /user
│   ├── ProfileApi.java           # GET /profiles/{username}, follow/unfollow
│   ├── TagsApi.java              # GET /tags
│   ├── UsersApi.java             # POST /users, POST /users/login
│   ├── exception/                # Custom exceptions
│   └── security/                 # JWT filter, security config
│
├── application/                  # Application Services
│   ├── ArticleQueryService.java  # Article read operations
│   ├── article/
│   │   └── ArticleCommandService.java  # Article write operations
│   ├── CommentQueryService.java
│   ├── ProfileQueryService.java
│   ├── TagsQueryService.java
│   ├── UserQueryService.java
│   ├── user/
│   │   └── UserService.java      # User registration/updates
│   └── data/                     # DTOs (ArticleData, UserData, etc.)
│
├── core/                         # Domain Layer
│   ├── article/
│   │   ├── Article.java          # Article entity
│   │   ├── ArticleRepository.java
│   │   └── Tag.java
│   ├── comment/
│   │   ├── Comment.java
│   │   └── CommentRepository.java
│   ├── favorite/
│   │   ├── ArticleFavorite.java
│   │   └── ArticleFavoriteRepository.java
│   ├── user/
│   │   ├── User.java             # User entity
│   │   ├── UserRepository.java
│   │   └── FollowRelation.java
│   └── service/
│       ├── AuthorizationService.java
│       └── JwtService.java       # Interface
│
├── graphql/                      # GraphQL Resolvers (DGS)
│   ├── ArticleDatafetcher.java
│   ├── ArticleMutation.java
│   ├── CommentDatafetcher.java
│   ├── CommentMutation.java
│   ├── ProfileDatafetcher.java
│   ├── RelationMutation.java
│   └── UserMutation.java
│
└── infrastructure/               # Technical Implementations
    ├── mybatis/
    │   ├── mapper/               # MyBatis mapper interfaces
    │   └── readservice/          # Read-optimized query interfaces
    ├── repository/               # Repository implementations
    └── service/
        └── DefaultJwtService.java

src/main/resources/
├── application.properties        # Configuration
├── db/migration/
│   └── V1__create_tables.sql     # Flyway migration
├── mapper/                       # MyBatis SQL mappings
│   ├── ArticleMapper.xml
│   ├── ArticleReadService.xml
│   ├── UserMapper.xml
│   └── ...
└── schema/
    └── schema.graphqls           # GraphQL schema
```

# REST API Endpoints

All endpoints return JSON. Authentication is via JWT token in the `Authorization` header with format `Token {jwt}`.

## Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/users` | Register a new user | No |
| POST | `/users/login` | Login and get JWT token | No |

**Register User**
```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"user": {"username": "jacob", "email": "jake@jake.jake", "password": "jakejake"}}'
```

**Login**
```bash
curl -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{"user": {"email": "jake@jake.jake", "password": "jakejake"}}'
```

## User

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/user` | Get current user | Yes |
| PUT | `/user` | Update current user | Yes |

## Profiles

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/profiles/{username}` | Get user profile | Optional |
| POST | `/profiles/{username}/follow` | Follow a user | Yes |
| DELETE | `/profiles/{username}/follow` | Unfollow a user | Yes |

## Articles

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/articles` | List articles (with filters) | Optional |
| GET | `/articles/feed` | Get feed from followed users | Yes |
| GET | `/articles/{slug}` | Get single article | Optional |
| POST | `/articles` | Create article | Yes |
| PUT | `/articles/{slug}` | Update article | Yes (author only) |
| DELETE | `/articles/{slug}` | Delete article | Yes (author only) |

**Query Parameters for GET /articles:**
- `tag` - Filter by tag
- `author` - Filter by author username
- `favorited` - Filter by user who favorited
- `limit` - Number of articles (default: 20)
- `offset` - Offset for pagination (default: 0)

**Create Article**
```bash
curl -X POST http://localhost:8080/articles \
  -H "Content-Type: application/json" \
  -H "Authorization: Token {jwt}" \
  -d '{"article": {"title": "How to train your dragon", "description": "Ever wonder how?", "body": "You have to believe", "tagList": ["dragons", "training"]}}'
```

## Comments

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/articles/{slug}/comments` | Get comments for article | Optional |
| POST | `/articles/{slug}/comments` | Add comment to article | Yes |
| DELETE | `/articles/{slug}/comments/{id}` | Delete comment | Yes (author/article owner) |

## Favorites

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/articles/{slug}/favorite` | Favorite an article | Yes |
| DELETE | `/articles/{slug}/favorite` | Unfavorite an article | Yes |

## Tags

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/tags` | Get all tags | No |

# GraphQL API

Access the GraphQL Playground at http://localhost:8080/graphiql

The GraphQL endpoint is at http://localhost:8080/graphql

## Queries

```graphql
# Get single article
query {
  article(slug: "how-to-train-your-dragon") {
    title
    body
    author {
      username
      following
    }
    favoritesCount
    tagList
  }
}

# List articles with pagination
query {
  articles(first: 10, withTag: "dragons") {
    edges {
      node {
        title
        slug
        author { username }
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}

# Get current user
query {
  me {
    username
    email
    token
  }
}

# Get user feed
query {
  feed(first: 10) {
    edges {
      node {
        title
        author { username }
      }
    }
  }
}

# Get profile
query {
  profile(username: "jacob") {
    profile {
      username
      bio
      following
    }
  }
}
```

## Mutations

```graphql
# Create user
mutation {
  createUser(input: {
    username: "jacob"
    email: "jake@jake.jake"
    password: "jakejake"
  }) {
    ... on UserPayload {
      user {
        username
        token
      }
    }
    ... on Error {
      message
      errors { key value }
    }
  }
}

# Login
mutation {
  login(email: "jake@jake.jake", password: "jakejake") {
    user {
      username
      token
    }
  }
}

# Create article
mutation {
  createArticle(input: {
    title: "How to train your dragon"
    description: "Ever wonder how?"
    body: "You have to believe"
    tagList: ["dragons", "training"]
  }) {
    article {
      slug
      title
    }
  }
}

# Follow user
mutation {
  followUser(username: "celeb_jane") {
    profile {
      username
      following
    }
  }
}

# Favorite article
mutation {
  favoriteArticle(slug: "how-to-train-your-dragon") {
    article {
      favoritesCount
      favorited
    }
  }
}
```

# Database Schema

The application uses SQLite by default with Flyway for migrations. The schema consists of the following tables:

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│    users     │       │   articles   │       │     tags     │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)      │◄──────│ user_id (FK) │       │ id (PK)      │
│ username     │       │ id (PK)      │       │ name         │
│ email        │       │ slug         │       └──────┬───────┘
│ password     │       │ title        │              │
│ bio          │       │ description  │              │
│ image        │       │ body         │              │
└──────┬───────┘       │ created_at   │              │
       │               │ updated_at   │              │
       │               └──────┬───────┘              │
       │                      │                      │
       │               ┌──────┴───────┐       ┌──────┴───────┐
       │               │              │       │              │
┌──────┴───────┐  ┌────┴────────┐  ┌──┴──────────┐  ┌────────┴───┐
│   follows    │  │  comments   │  │article_fav- │  │article_tags│
├──────────────┤  ├─────────────┤  │  orites     │  ├────────────┤
│ user_id (FK) │  │ id (PK)     │  ├─────────────┤  │ article_id │
│ follow_id(FK)│  │ body        │  │ article_id  │  │ tag_id     │
└──────────────┘  │ article_id  │  │ user_id     │  └────────────┘
                  │ user_id     │  └─────────────┘
                  │ created_at  │
                  │ updated_at  │
                  └─────────────┘
```

**Table Descriptions:**

- **users**: User accounts with BCrypt-hashed passwords
- **articles**: Blog posts with auto-generated slugs from titles
- **tags**: Unique tag names for categorizing articles
- **article_tags**: Many-to-many relationship between articles and tags
- **article_favorites**: Tracks which users have favorited which articles
- **follows**: Self-referential relationship for user following
- **comments**: User comments on articles

# Configuration

Key configuration options in `application.properties`:

```properties
# Database (SQLite by default)
spring.datasource.url=jdbc:sqlite:dev.db
spring.datasource.driver-class-name=org.sqlite.JDBC

# JWT Settings
jwt.secret=your-secret-key-here
jwt.sessionTime=86400  # Token expiration in seconds (24 hours)

# Default user avatar
image.default=https://static.productionready.io/images/smiley-cyrus.jpg

# MyBatis
mybatis.mapper-locations=mapper/*.xml
mybatis.configuration.map-underscore-to-camel-case=true
```

To use a different database (e.g., PostgreSQL, MySQL), update the datasource properties and add the appropriate JDBC driver dependency.

# Security

The application uses JWT (JSON Web Tokens) for stateless authentication.

**Authentication Flow:**
1. User registers or logs in via `/users` or `/users/login`
2. Server returns a JWT token in the response
3. Client includes token in subsequent requests: `Authorization: Token {jwt}`
4. `JwtTokenFilter` extracts and validates the token
5. If valid, the user is loaded into Spring Security context

**Endpoint Authorization:**

| Endpoint Pattern | Access |
|-----------------|--------|
| `POST /users`, `POST /users/login` | Public |
| `GET /articles/**`, `GET /profiles/**`, `GET /tags` | Public |
| `GET /articles/feed` | Authenticated |
| `/graphql`, `/graphiql` | Public (auth checked per operation) |
| All other endpoints | Authenticated |

**CORS Configuration:**
- All origins allowed (`*`)
- Supported methods: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
- Required headers: Authorization, Cache-Control, Content-Type

# Testing

The project includes comprehensive tests across all layers:

```
src/test/java/io/spring/
├── api/                    # API integration tests
│   ├── ArticleApiTest.java
│   ├── ArticlesApiTest.java
│   ├── CommentsApiTest.java
│   ├── CurrentUserApiTest.java
│   ├── ProfileApiTest.java
│   └── UsersApiTest.java
├── application/            # Service unit tests
├── core/                   # Domain entity tests
└── infrastructure/         # Repository integration tests
```

**Run all tests:**
```bash
./gradlew test
```

**Test Patterns:**
- API tests use `@WebMvcTest` with `RestAssuredMockMvc` for HTTP testing
- Service tests mock repositories to isolate business logic
- Repository tests use a real SQLite database via `DbTestBase`

# Development

## Prerequisites

- Java 11 or higher
- Gradle (wrapper included)

## Local Development

```bash
# Run the application
./gradlew bootRun

# The API will be available at http://localhost:8080
# GraphQL Playground at http://localhost:8080/graphiql
```

## Docker

```bash
# Build the Docker image
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app

# Run the container
docker run -p 8081:8080 spring-boot-realworld-example-app

# Access at http://localhost:8081
```

## Code Quality

```bash
# Format code with Spotless (Google Java Format)
./gradlew spotlessJavaApply

# Check formatting
./gradlew spotlessCheck
```

## CI/CD

The project uses GitHub Actions for continuous integration. On every push and pull request:

1. Checkout code
2. Set up JDK 11 (Zulu distribution)
3. Cache Gradle dependencies
4. Run `./gradlew clean test`

See `.github/workflows/gradle.yml` for the full configuration.

# Dependencies

Key dependencies used in this project:

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 2.6.3 | Web framework, security, validation |
| MyBatis Spring Boot | 2.2.2 | SQL mapping framework |
| Netflix DGS | 4.9.21 | GraphQL server framework |
| Flyway | (managed) | Database migrations |
| JJWT | 0.11.2 | JWT token handling |
| SQLite JDBC | 3.36.0.3 | Database driver |
| Lombok | (managed) | Boilerplate reduction |
| Joda-Time | 2.10.13 | Date/time handling |

**Test Dependencies:**
- REST Assured 4.5.1 for API testing
- Spring Security Test
- Spring Boot Test
- MyBatis Spring Boot Test

# Help

Please fork and PR to improve the project.
