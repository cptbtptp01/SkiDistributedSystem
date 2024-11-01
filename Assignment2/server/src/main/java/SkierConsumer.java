import com.google.gson.Gson;
import com.rabbitmq.client.*;
import config.RabbitMQConfig;
import java.util.ArrayList;
import java.util.List;
import model.LiftRideMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SkierConsumer {
  private static final String QUEUE_NAME = "liftRideQueue";
  private static final ConcurrentHashMap<Integer, LiftRideMessage> skierRides = new ConcurrentHashMap<>();
  private final Gson gson = new Gson();

  private final ConnectionFactory factory;
  private final List<Channel> consumerChannels;
  private final ExecutorService threadPool;
  private static final int NUM_CONSUMERS = 10;

  private final AtomicBoolean running = new AtomicBoolean(true);

  public SkierConsumer(ConnectionFactory factory) throws IOException, TimeoutException {
    this.factory = factory;
    this.consumerChannels = new ArrayList<>();
    this.threadPool = Executors.newFixedThreadPool(NUM_CONSUMERS);

    // Create multiple consumers on different channels
    Connection connection = factory.newConnection();
    for (int i = 0; i < NUM_CONSUMERS; i++) {
      Channel channel = connection.createChannel();
      channel.queueDeclare(QUEUE_NAME, true, false, false, null);
      channel.basicQos(250); // Set prefetch count
      consumerChannels.add(channel);
    }
  }

  public void startListening() throws IOException {
    System.out.println(" [*] Waiting for messages from queue: " + QUEUE_NAME);

    for (Channel channel : consumerChannels) {
      channel.basicConsume(QUEUE_NAME, true, new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope,
            AMQP.BasicProperties properties, byte[] body) {
          String message = new String(body, StandardCharsets.UTF_8);
          threadPool.submit(() -> processMessage(message));
        }
      });
    }
  }

  private void processMessage(String message) {
    try {
      System.out.println(" [x] Received '" + message + "'");
      LiftRideMessage liftRideMessage = gson.fromJson(message, LiftRideMessage.class);
      skierRides.put(liftRideMessage.getSkierId(), liftRideMessage);
      System.out.println(" [x] Skier " + liftRideMessage.getSkierId() + " lift ride updated in record.");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void close() {
    running.set(false);

    // Shutdown thread pool gracefully
    threadPool.shutdown();
    try {
      // Wait for tasks to complete with a timeout
      if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
        threadPool.shutdownNow();
      }
    } catch (InterruptedException e) {
      threadPool.shutdownNow();
      Thread.currentThread().interrupt();
    }

    // Close all channels
    for (Channel channel : consumerChannels) {
      try {
        if (channel != null && channel.isOpen()) {
          channel.close();
        }
      } catch (IOException | TimeoutException e) {
        e.printStackTrace();
      }
    }

    // Close the connection (it's shared by all channels)
    try {
      Connection connection = null;
      if (!consumerChannels.isEmpty()) {
        connection = consumerChannels.get(0).getConnection();
      }
      if (connection != null && connection.isOpen()) {
        connection.close();
      }
      System.out.println(" [x] All channels and connection closed");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws IOException, TimeoutException {
    ConnectionFactory factory = RabbitMQConfig.createFactory();

    SkierConsumer consumer = new SkierConsumer(factory);

    Runtime.getRuntime().addShutdownHook(new Thread(consumer::close));

    System.out.println("consumer started");
    consumer.startListening();
  }
}
