# Loretta Bank

![loretta_home](https://github.com/user-attachments/assets/931188dc-e208-4cd9-b490-4ee2da1db25e)

![loretta_transfer](https://github.com/user-attachments/assets/b1ca33b2-d9bb-4bde-8056-898a13f551d9)

![loretta_login](https://github.com/user-attachments/assets/419bc0ee-1603-4791-9042-bef949005428)

![loretta_load](https://github.com/user-attachments/assets/5ed0e259-9a34-401c-ae56-07bb974d81d3)

Loretta Bank is a modern online banking system built with a Spring Boot backend and a React frontend. The system consists of multiple microservices deployed on a Kubernetes cluster and provides features for user management, account management, transactions, loans, cards, and customer support.

## Project Structure

The project is divided into two main parts:

1. **Server**: A Spring Boot application consisting of multiple microservices.
2. **Client**: A React application that interacts with the backend services.

## Prerequisites

- Java 22
- Maven 3.8+
- Docker
- Kubernetes cluster
- Node.js 16+
- PostgreSQL 14+

## Getting Started

### Server Setup

#### Microservices

The backend consists of the following microservices:

- **User Service**: Manages user data and authentication.
- **Account Service**: Manages bank accounts.
- **Transaction Service**: Handles transactions between accounts.
- **Loan Service**: Manages loan applications and information.
- **Card Service**: Manages credit and debit cards.
- **Customer Support Service**: Handles customer support tickets and inquiries.

Each service is a separate Spring Boot application.

#### Building the Server

1. **Clone the repository:**
   ```bash
   git clone https://github.com/7irelo/loretta-bank.git
   cd loretta-bank/server
   ```

2. **Build the project:**
   ```bash
   mvn clean install
   ```

3. **Run the services locally:**
   Each service can be run independently. Navigate to each service directory and use:
   ```bash
   mvn spring-boot:run
   ```

#### Docker and Kubernetes

1. **Build Docker images for each service:**
   ```bash
   docker build -t user-service:latest -f docker/Dockerfile.user .
   ```

2. **Push images to a container registry:**
   ```bash
   docker tag user-service:latest your-dockerhub-repo/user-service:latest
   docker push your-dockerhub-repo/user-service:latest
   ```

3. **Deploy to Kubernetes:**
   Apply the Kubernetes configurations for each service:
   ```bash
   kubectl apply -f kubernetes/deployment.user.yaml
   ```

### Client Setup

The frontend is a React application that interacts with the backend services.

1. **Navigate to the client directory:**
   ```bash
   cd ../client
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Run the client locally:**
   ```bash
   npm start
   ```

The client application will be accessible at `http://localhost:3000`.

## Configuration

### Server Configuration

The server configuration files are located in `src/main/resources/application.properties` for each service. Key configurations include:

- **Database Connection:**
  ```properties
  spring.datasource.url=jdbc:postgresql://postgres-service:5432/lorettabank
  spring.datasource.username=postgres
  spring.datasource.password=your_password
  ```

### Client Configuration

The client configuration can be adjusted in the `src/config.js` file to set API endpoints and other global settings.

## Running Tests

### Server Tests

To run tests for the server, execute the following command in the server root:

```bash
mvn test
```

### Client Tests

To run tests for the client, use:

```bash
npm test
```

## Deployment

For production deployment, ensure the following:

1. **Docker Images**: All services should be containerized and pushed to a Docker registry.
2. **Kubernetes Cluster**: Services are deployed to a Kubernetes cluster using the provided YAML configurations.
3. **Environment Variables**: Set appropriate environment variables for database connections, API keys, etc.

## Contributing

Contributions are welcome! Please fork the repository and create a pull request with your changes.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

This README should provide a comprehensive overview of the project, helping users set up and run the system effectively. Let me know if there are any specific sections you'd like to adjust or expand further!
