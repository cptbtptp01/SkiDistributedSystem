package config;

import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQConfig {
  private static final String DEFAULT_HOST = "172.31.45.147"; // Default host if not provided
  private static final int DEFAULT_PORT = 5672; // Default port if not provided
  private static final String DEFAULT_USERNAME = "guest"; // Default username if not provided
  private static final String DEFAULT_PASSWORD = "guest"; // Default password if not provided

  public static ConnectionFactory createFactory() {
    ConnectionFactory factory = new ConnectionFactory();

    // Get RabbitMQ configurations from environment variables or use defaults
    String host = System.getenv("RABBITMQ_HOST") != null ? System.getenv("RABBITMQ_HOST") : DEFAULT_HOST;
    String port = System.getenv("RABBITMQ_PORT") != null ? System.getenv("RABBITMQ_PORT") : String.valueOf(DEFAULT_PORT);
    String username = System.getenv("RABBITMQ_USERNAME") != null ? System.getenv("RABBITMQ_USERNAME") : DEFAULT_USERNAME;
    String password = System.getenv("RABBITMQ_PASSWORD") != null ? System.getenv("RABBITMQ_PASSWORD") : DEFAULT_PASSWORD;

    factory.setHost(host);
    factory.setPort(Integer.parseInt(port)); // Convert port to int
    factory.setUsername(username);
    factory.setPassword(password);

    return factory;
  }
}
