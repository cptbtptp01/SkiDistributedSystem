import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.LiftRide;
import model.ResponseMsg;

@WebServlet(value = "/skiers/*")
public class SkierServlet extends HttpServlet {

  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    res.setContentType("application/json");
    res.setCharacterEncoding("UTF-8");
    String urlPath = req.getPathInfo();
    Gson gson = new Gson();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write(gson.toJson(new ResponseMsg("Missing Parameters")));
      return;
    }

    String[] urlParts = urlPath.split("/");

    if (!isUrlValid(urlParts)) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write(gson.toJson(new ResponseMsg("Invalid URL")));
    } else {
      res.setStatus(HttpServletResponse.SC_OK);
      // TODO: process url params in `urlParts`
      res.getWriter().write(gson.toJson(new ResponseMsg("Valid URL GET")));
    }
  }

  // Stores new lift ride details in the data store
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    res.setContentType("application/json");
    res.setCharacterEncoding("UTF-8");
    Gson gson = new Gson();

    String urlPath = req.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write(gson.toJson(new ResponseMsg("Invalid URL")));
      return;
    }

    String[] urlParts = urlPath.split("/");

    if (!isUrlValid(urlParts)) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write(gson.toJson(new ResponseMsg("URL not found")));

    } else {

      try {
        // Stores new lift ride details
        StringBuilder body = new StringBuilder();
        String line;
        BufferedReader reader = req.getReader();
        while ((line = reader.readLine()) != null) {
          body.append(line);
        }

        // Log the request body (You can print it to the console)
        System.out.println("Request Body: " + body.toString());

        boolean success = true;

        if (success) {
          res.setStatus(HttpServletResponse.SC_CREATED);
          res.getWriter().write(gson.toJson(new ResponseMsg("Write successful")));
        } else {
          res.setStatus(HttpServletResponse.SC_NOT_FOUND);
          res.getWriter().write(gson.toJson(new ResponseMsg("Write failed")));
        }
      } catch (Exception e) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(gson.toJson(new ResponseMsg("Error")));
      }
    }
  }

  private boolean storeLiftRide(int resortID, String seasonID, String dayID, int skierID,
      LiftRide liftRide) {
    // TODO impl this
    return true;
  }

  // /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
  // urlPath  = "/1/seasons/2019/days/1/skiers/123"
  // urlParts = [, 1, seasons, 2019, days, 1, skiers, 123]
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
