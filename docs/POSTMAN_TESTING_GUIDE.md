# 📬 Postman Testing Guide - GraphQL API

**Project**: SampleGraphQL  
**Document Date**: November 1, 2025  
**API Endpoint**: `http://localhost:8080/graphql`  
**GraphiQL**: `http://localhost:8080/graphiql`

---

## Table of Contents
1. [Setup Instructions](#setup-instructions)
2. [Authentication](#authentication)
3. [Test Data](#test-data)
4. [Query Examples](#query-examples)
5. [Mutation Examples](#mutation-examples)
6. [Subscription Examples](#subscription-examples)
7. [Error Scenarios](#error-scenarios)
8. [Performance Testing](#performance-testing)

---

## Setup Instructions

### 1. Import Postman Collection

Create a new collection named "GraphQL API Tests" with the following setup:

**Collection Variables:**
```
baseUrl: http://localhost:8080
graphqlEndpoint: {{baseUrl}}/graphql
token: (will be set after login)
```

**Pre-request Script** (Collection Level):
```javascript
// Auto-refresh token if expired
const token = pm.collectionVariables.get("token");
if (!token) {
    console.log("No token found. Please run the Login request first.");
}
```

### 2. Environment Setup

Create environments for different deployment stages:

**Local Environment:**
```json
{
  "name": "Local",
  "values": [
    {"key": "baseUrl", "value": "http://localhost:8080", "enabled": true},
    {"key": "username", "value": "admin", "enabled": true},
    {"key": "password", "value": "admin123", "enabled": true}
  ]
}
```

**Dev Environment:**
```json
{
  "name": "Development",
  "values": [
    {"key": "baseUrl", "value": "https://dev-api.yourapp.com", "enabled": true},
    {"key": "username", "value": "devuser", "enabled": true},
    {"key": "password", "value": "devpassword", "enabled": true}
  ]
}
```

---

## Authentication

### 1. User Registration

**Request Name:** `Auth - Register User`

**Method:** `POST`  
**URL:** `{{graphqlEndpoint}}`  
**Body Type:** `GraphQL`

**GraphQL Query:**
```graphql
mutation RegisterUser($input: UserInput!) {
  register(input: $input) {
    token
    refreshToken
    user {
      id
      username
      email
      role
      createdAt
    }
  }
}
```

**GraphQL Variables:**
```json
{
  "input": {
    "username": "newuser",
    "email": "newuser@example.com",
    "password": "Password123!",
    "role": "USER"
  }
}
```

**Tests Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Registration successful", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.register).to.have.property('token');
    pm.expect(jsonData.data.register).to.have.property('user');
    
    // Save token for subsequent requests
    pm.collectionVariables.set("token", jsonData.data.register.token);
    pm.collectionVariables.set("refreshToken", jsonData.data.register.refreshToken);
    pm.collectionVariables.set("userId", jsonData.data.register.user.id);
});

pm.test("User has correct role", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.register.user.role).to.eql("USER");
});
```

---

### 2. User Login

**Request Name:** `Auth - Login`

**Method:** `POST`  
**URL:** `{{graphqlEndpoint}}`

**GraphQL Query:**
```graphql
mutation Login($input: LoginInput!) {
  login(input: $input) {
    token
    refreshToken
    user {
      id
      username
      email
      role
    }
  }
}
```

**GraphQL Variables:**
```json
{
  "input": {
    "username": "{{username}}",
    "password": "{{password}}"
  }
}
```

**Tests Script:**
```javascript
pm.test("Login successful", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.login).to.have.property('token');
    
    // Save authentication token
    pm.collectionVariables.set("token", jsonData.data.login.token);
    console.log("Token saved:", jsonData.data.login.token);
});

pm.test("Token is valid JWT", function () {
    var token = pm.response.json().data.login.token;
    var parts = token.split('.');
    pm.expect(parts).to.have.lengthOf(3); // JWT has 3 parts
});
```

---

### 3. Refresh Token

**Request Name:** `Auth - Refresh Token`

**GraphQL Query:**
```graphql
mutation RefreshToken($refreshToken: String!) {
  refreshToken(refreshToken: $refreshToken) {
    token
    refreshToken
    user {
      username
    }
  }
}
```

**GraphQL Variables:**
```json
{
  "refreshToken": "{{refreshToken}}"
}
```

---

## Test Data

### Sample Users (Pre-seeded in Development)

```json
[
  {
    "id": "1",
    "username": "admin",
    "email": "admin@example.com",
    "password": "admin123",
    "role": "ADMIN"
  },
  {
    "id": "2",
    "username": "johndoe",
    "email": "john@example.com",
    "password": "password123",
    "role": "USER"
  },
  {
    "id": "3",
    "username": "janedoe",
    "email": "jane@example.com",
    "password": "password123",
    "role": "USER"
  }
]
```

### Sample Posts

```json
[
  {
    "id": "1",
    "title": "Welcome to GraphQL",
    "content": "This is an introduction to GraphQL with Spring Boot",
    "authorEmail": "john@example.com"
  },
  {
    "id": "2",
    "title": "Spring Security Best Practices",
    "content": "Learn how to secure your Spring Boot application",
    "authorEmail": "john@example.com"
  },
  {
    "id": "3",
    "title": "Admin Announcement",
    "content": "Important system update scheduled",
    "authorEmail": "admin@example.com"
  }
]
```

---

## Query Examples

### 1. Get All Users

**Request Name:** `Query - Get All Users`

**Headers:**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**GraphQL Query:**
```graphql
query GetAllUsers {
  users {
    id
    username
    email
    role
    createdAt
    updatedAt
  }
}
```

**Tests:**
```javascript
pm.test("Returns list of users", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.users).to.be.an('array');
    pm.expect(jsonData.data.users.length).to.be.greaterThan(0);
});

pm.test("Users have required fields", function () {
    var users = pm.response.json().data.users;
    users.forEach(user => {
        pm.expect(user).to.have.property('id');
        pm.expect(user).to.have.property('username');
        pm.expect(user).to.have.property('email');
        pm.expect(user).to.have.property('role');
    });
});
```

---

### 2. Get User by Email

**GraphQL Query:**
```graphql
query GetUserByEmail($email: String!) {
  userByEmail(email: $email) {
    id
    username
    email
    role
    createdAt
  }
}
```

**Variables:**
```json
{
  "email": "john@example.com"
}
```

---

### 3. Search Users

**GraphQL Query:**
```graphql
query SearchUsers($search: String!) {
  searchUsers(search: $search) {
    id
    username
    email
  }
}
```

**Variables:**
```json
{
  "search": "john"
}
```

---

### 4. Get All Posts with Users

**GraphQL Query:**
```graphql
query GetAllPosts {
  posts {
    id
    title
    content
    createdAt
    updatedAt
    user {
      id
      username
      email
    }
  }
}
```

**Tests:**
```javascript
pm.test("Posts have associated users", function () {
    var posts = pm.response.json().data.posts;
    posts.forEach(post => {
        pm.expect(post).to.have.property('user');
        pm.expect(post.user).to.have.property('username');
    });
});

pm.test("No N+1 query issues", function () {
    // Response time should be reasonable even with many posts
    pm.expect(pm.response.responseTime).to.be.below(500);
});
```

---

### 5. Get Posts by Author

**GraphQL Query:**
```graphql
query GetPostsByAuthor($authorEmail: String!) {
  postsByAuthor(authorEmail: $authorEmail) {
    id
    title
    content
    user {
      username
      email
    }
  }
}
```

**Variables:**
```json
{
  "authorEmail": "john@example.com"
}
```

---

### 6. Paginated Posts Query

**GraphQL Query:**
```graphql
query GetPostsConnection($input: PageInput!) {
  postsConnection(input: $input) {
    edges {
      cursor
      node {
        id
        title
        content
        user {
          username
        }
      }
    }
    pageInfo {
      hasNextPage
      hasPreviousPage
      startCursor
      endCursor
    }
    totalCount
  }
}
```

**Variables (First Page):**
```json
{
  "input": {
    "first": 5
  }
}
```

**Variables (Next Page):**
```json
{
  "input": {
    "first": 5,
    "after": "{{endCursor}}"
  }
}
```

**Tests:**
```javascript
pm.test("Pagination works correctly", function () {
    var data = pm.response.json().data.postsConnection;
    pm.expect(data).to.have.property('edges');
    pm.expect(data).to.have.property('pageInfo');
    pm.expect(data.pageInfo).to.have.property('hasNextPage');
    
    // Save cursor for next request
    if (data.pageInfo.endCursor) {
        pm.collectionVariables.set("endCursor", data.pageInfo.endCursor);
    }
});
```

---

## Mutation Examples

### 1. Create User (Admin Only)

**GraphQL Mutation:**
```graphql
mutation CreateUser($username: String!, $email: String!, $password: String!, $role: String!) {
  createUser(username: $username, email: $email, password: $password, role: $role) {
    id
    username
    email
    role
    createdAt
  }
}
```

**Variables:**
```json
{
  "username": "newuser",
  "email": "newuser@example.com",
  "password": "SecurePassword123!",
  "role": "USER"
}
```

**Headers:**
```
Authorization: Bearer {{token}}
```

**Tests:**
```javascript
pm.test("User created successfully", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.createUser).to.have.property('id');
    pm.expect(jsonData.data.createUser.username).to.eql("newuser");
    
    // Save for cleanup
    pm.collectionVariables.set("createdUserId", jsonData.data.createUser.id);
});
```

---

### 2. Update User

**GraphQL Mutation:**
```graphql
mutation UpdateUser($id: ID!, $username: String, $email: String) {
  updateUser(id: $id, username: $username, email: $email) {
    id
    username
    email
    updatedAt
  }
}
```

**Variables:**
```json
{
  "id": "{{userId}}",
  "username": "updatedusername",
  "email": "updated@example.com"
}
```

---

### 3. Create Post

**GraphQL Mutation:**
```graphql
mutation CreatePost($title: String!, $content: String!, $authorEmail: String!) {
  createPost(title: $title, content: $content, authorEmail: $authorEmail) {
    id
    title
    content
    createdAt
    user {
      username
      email
    }
  }
}
```

**Variables:**
```json
{
  "title": "My New Post",
  "content": "This is the content of my new post. It discusses various topics related to GraphQL and Spring Boot.",
  "authorEmail": "john@example.com"
}
```

**Tests:**
```javascript
pm.test("Post created successfully", function () {
    var data = pm.response.json().data.createPost;
    pm.expect(data).to.have.property('id');
    pm.expect(data.title).to.eql("My New Post");
    
    // Save post ID for further operations
    pm.collectionVariables.set("postId", data.id);
});

pm.test("Post has author", function () {
    var data = pm.response.json().data.createPost;
    pm.expect(data.user.email).to.eql("john@example.com");
});
```

---

### 4. Update Post

**GraphQL Mutation:**
```graphql
mutation UpdatePost($id: ID!, $title: String, $content: String) {
  updatePost(id: $id, title: $title, content: $content) {
    id
    title
    content
    updatedAt
  }
}
```

**Variables:**
```json
{
  "id": "{{postId}}",
  "title": "Updated Post Title",
  "content": "Updated content for this post."
}
```

---

### 5. Delete Post

**GraphQL Mutation:**
```graphql
mutation DeletePost($id: ID!) {
  deletePost(id: $id)
}
```

**Variables:**
```json
{
  "id": "{{postId}}"
}
```

**Tests:**
```javascript
pm.test("Post deleted successfully", function () {
    var result = pm.response.json().data.deletePost;
    pm.expect(result).to.be.true;
});
```

---

## Subscription Examples

### Testing Subscriptions via WebSocket

Subscriptions require WebSocket connection. Use these tools:
- **GraphiQL** (built-in): `http://localhost:8080/graphiql`
- **Postman** (with WebSocket support)
- **Apollo Studio**

**Subscription Query:**
```graphql
subscription OnPostAdded {
  postAdded {
    id
    title
    content
    user {
      username
    }
    createdAt
  }
}
```

**Testing Flow:**
1. Open GraphiQL in browser
2. Start subscription in one tab
3. Create a post in another tab
4. Verify subscription receives the new post

---

## Error Scenarios

### 1. Unauthorized Access

**Request:** Query without token

**Expected Response:**
```json
{
  "errors": [
    {
      "message": "Unauthorized",
      "extensions": {
        "classification": "UNAUTHORIZED"
      }
    }
  ]
}
```

**Test:**
```javascript
pm.test("Unauthorized request blocked", function () {
    var errors = pm.response.json().errors;
    pm.expect(errors).to.be.an('array');
    pm.expect(errors[0].message).to.include("Unauthorized");
});
```

---

### 2. Invalid Input Validation

**Mutation:** Create user with invalid email

**Variables:**
```json
{
  "username": "test",
  "email": "invalid-email",
  "password": "pass",
  "role": "USER"
}
```

**Expected Response:**
```json
{
  "errors": [
    {
      "message": "Input validation failed: email: Email must be valid, password: Password must be at least 8 characters",
      "extensions": {
        "classification": "BAD_REQUEST"
      }
    }
  ]
}
```

---

### 3. Resource Not Found

**Query:** Get non-existent user

**Variables:**
```json
{
  "id": "999999"
}
```

**Expected Response:**
```json
{
  "errors": [
    {
      "message": "User not found with ID: 999999",
      "extensions": {
        "classification": "NOT_FOUND"
      }
    }
  ]
}
```

---

## Performance Testing

### Load Test Scenario

**Collection Runner Settings:**
- Iterations: 100
- Delay: 100ms
- Data file: users.csv

**CSV Data (users.csv):**
```csv
username,email,password
user1,user1@test.com,Password123!
user2,user2@test.com,Password123!
user3,user3@test.com,Password123!
```

**Performance Tests:**
```javascript
pm.test("Response time is acceptable", function () {
    pm.expect(pm.response.responseTime).to.be.below(200);
});

pm.test("No server errors", function () {
    pm.response.to.not.be.error;
    pm.response.to.have.status(200);
});
```

---

## Complete Test Workflow

### Test Execution Order

1. **Authentication**
   - Register User
   - Login
   - Verify Token

2. **User Operations**
   - Get All Users
   - Search Users
   - Get User by Email
   - Update User
   - Delete User

3. **Post Operations**
   - Get All Posts
   - Create Post
   - Get Posts by Author
   - Update Post
   - Delete Post
   - Paginated Posts

4. **Security Tests**
   - Unauthorized Access
   - Invalid Token
   - Forbidden Operations

5. **Cleanup**
   - Delete Test Data

---

## Postman Collection JSON

Download the complete collection:
[GraphQL_API_Tests.postman_collection.json](./postman/GraphQL_API_Tests.postman_collection.json)

---

## Tips & Best Practices

1. **Use Environment Variables**: Don't hardcode values
2. **Chain Requests**: Use test scripts to save IDs for subsequent requests
3. **Validate Responses**: Always check response structure and values
4. **Performance Monitoring**: Track response times
5. **Data Cleanup**: Delete test data after tests
6. **Error Testing**: Test both success and failure scenarios

---

## Troubleshooting

### Common Issues

**Issue:** "Unauthorized" error  
**Solution:** Ensure token is set: `pm.collectionVariables.get("token")`

**Issue:** GraphQL syntax errors  
**Solution:** Validate query in GraphiQL first

**Issue:** Network timeout  
**Solution:** Check server is running on correct port

---

**Next Steps:**
1. Import Postman collection
2. Configure environments
3. Run authentication requests first
4. Execute test scenarios
5. Review test results

*Happy Testing! 🚀*
