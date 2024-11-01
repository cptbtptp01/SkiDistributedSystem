import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import config.RabbitMQConfig;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.LiftRide;
import model.LiftRideMessage;
import model.ResponseMsg;
import service.RabbitMQService;
import util.RequestParser;

@WebServlet(value = "/skiers/*")
public class SkierServlet extends HttpServlet {
  private RabbitMQService rabbitMQService;
  private Gson gson = new Gson();

  @Override
  public void init() throws ServletException {
    super.init();
    try {
      rabbitMQService = new RabbitMQService(RabbitMQConfig.createFactory());
    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void destroy() {
    rabbitMQService.close(); // Close the RabbitMQ connection
    super.destroy();
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    setupResponse(res);

    String urlPath = req.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      sendErrorResponse(res, HttpServletResponse.SC_NOT_FOUND, "Missing Parameters");
      return;
    }

    String[] urlParts = urlPath.split("/");

    if (!isUrlValid(urlParts)) {
      sendErrorResponse(res, HttpServletResponse.SC_NOT_FOUND, "Invalid URL");
      return;
    }

    // Process the valid request
    try {
      // TODO: process url params in `urlParts`
      sendSuccessResponse(res, HttpServletResponse.SC_OK, "Valid URL GET");
    } catch (Exception e) {
      sendErrorResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server Error: " + e.getMessage());
    }
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    Connection connection = null;
    Channel channel = null;
    try {
      connection = rabbitMQService.getConnection();
      channel = rabbitMQService.getChannel(connection);
      setupResponse(res);
      String urlPath = req.getPathInfo();

      if (urlPath == null || urlPath.isEmpty()) {
        sendErrorResponse(res, HttpServletResponse.SC_NOT_FOUND, "Invalid URL");
        return;
      }

      String[] urlParts = urlPath.split("/");

      if (!isUrlValid(urlParts)) {
        sendErrorResponse(res, HttpServletResponse.SC_NOT_FOUND, "URL not found");
        return;
      }

      try {
        // Read the request body
        String requestBody = RequestParser.readRequestBody(req);

        // Parse the request body into a LiftRide object
        LiftRide liftRide = RequestParser.parseLiftRide(requestBody);

        // Create a LiftRideMessage object with parsed data
        LiftRideMessage message = new LiftRideMessage(
            liftRide,
            Integer.parseInt(urlParts[1]),
            urlParts[3],
            urlParts[5],
            Integer.parseInt(urlParts[7])
        );

        // Convert message to JSON
        String messageJson = RequestParser.createMessageJson(message);

        // Publish the message to the queue
        boolean success = rabbitMQService.publishMessage(channel, messageJson);

        if (success) {
          sendSuccessResponse(res, HttpServletResponse.SC_CREATED, "Write successful");
        } else {
          sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, "Write failed");
        }

      } catch (Exception e) {
        sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, "Error processing request: " + e.getMessage());
      }

    } catch (Exception e) {
      e.printStackTrace();
      sendErrorResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server Error: " + e.getMessage());
    } finally {
      if (channel != null) rabbitMQService.returnChannel(connection, channel);
      if (connection != null) rabbitMQService.returnConnection(connection);
    }
  }

  private void setupResponse(HttpServletResponse res) {
    res.setContentType("application/json");
    res.setCharacterEncoding("UTF-8");
  }

  private void sendErrorResponse(HttpServletResponse res, int status, String message)
      throws IOException {
    res.setStatus(status);
    res.getWriter().write(gson.toJson(new ResponseMsg(message)));
  }

  private void sendSuccessResponse(HttpServletResponse res, int status, String message)
      throws IOException {
    res.setStatus(status);
    res.getWriter().write(gson.toJson(new ResponseMsg(message)));
  }

  private boolean isUrlValid(String[] urlPath) {
    if (urlPath.length == 3) {
      return urlPath[1].chars().allMatch(Character::isDigit) &&
          urlPath[2].equals("vertical");
    } else if (urlPath.length == 8) {
      return urlPath[1].chars().allMatch(Character::isDigit) &&
          urlPath[2].equals("seasons") &&
          urlPath[3].chars().allMatch(Character::isDigit) &&
          urlPath[4].equals("days") &&
          urlPath[5].chars().allMatch(Character::isDigit) &&
          urlPath[6].equals("skiers") &&
          urlPath[7].chars().allMatch(Character::isDigit) &&
          Integer.parseInt(urlPath[5]) >= 1 &&
          Integer.parseInt(urlPath[5]) <= 365;
    }
    return false;
  }
}