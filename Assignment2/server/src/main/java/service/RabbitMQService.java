package service;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

public class RabbitMQService {

  private static final String QUEUE_NAME = "liftRideQueue"; // Specify the queue name
  private final ConnectionFactory factory;

  private final BlockingQueue<Connection> connectionPool;
  private final ConcurrentHashMap<Connection, BlockingQueue<Channel>> channelPools;
  private static final int MAX_POOL_SIZE = 5; // adj base on load
  private static final int CHANNELS_PER_CONNECTION = 1;

  public RabbitMQService(ConnectionFactory factory) throws IOException, TimeoutException {
    this.factory = factory;
    this.connectionPool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
    this.channelPools = new ConcurrentHashMap<>();

    // Prepopulate the connection and channel pools
    for (int i = 0; i < MAX_POOL_SIZE; i++) {
      Connection connection = factory.newConnection();
      connectionPool.offer(connection);

      BlockingQueue<Channel> channelPool = new LinkedBlockingQueue<>(CHANNELS_PER_CONNECTION);
      for (int j = 0; j < CHANNELS_PER_CONNECTION; j++) {
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        channelPool.offer(channel);
      }
      channelPools.put(connection, channelPool);
    }
  }

  public Connection getConnection() throws InterruptedException {
    return connectionPool.take();
  }

  public void returnConnection(Connection connection) {
    connectionPool.offer(connection);
  }

  public Channel getChannel(Connection connection) throws InterruptedException {
    return channelPools.get(connection).take();
  }

  public void returnChannel(Connection connection, Channel channel) {
    channelPools.get(connection).offer(channel);
  }

  public boolean publishMessage(Channel channel, String message) {
    try {
      // Publish message to the specified queue
      channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }


  public void close() {
    channelPools.forEach((connection, channels) -> {
      channels.forEach(channel -> {
        try {
          channel.close();
        } catch (IOException | TimeoutException e) {
          e.printStackTrace();
        }
      });
      try {
        connection.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }
}
