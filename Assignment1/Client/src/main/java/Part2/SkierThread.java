package Part2;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import model.RequestRecord;
import model.SkierEvent;

/**
 * Worker thread for Part2
 */
public class SkierThread implements Runnable {

  private static int RETRY_TIMES = 5;

  private final Integer numberOfRequests;
  private final BlockingDeque<SkierEvent> skierEventQueue;
  private final CountDownLatch latch;

  public SkierThread(Integer numberOfRequests, BlockingDeque<SkierEvent> skierEventQueue, CountDownLatch latch) {
    this.numberOfRequests = numberOfRequests;
    this.skierEventQueue = skierEventQueue;
    this.latch = latch;
  }

  @Override
  public void run() {
    SkiersApi apiInstance = new SkiersApi();
    ApiClient client = apiInstance.getApiClient();
//     client.setBasePath("http://localhost:8080/Server_war_exploded/");
    client.setBasePath("http://54.244.199.29:8080/Server_war/");

    // records for thread
    List<RequestRecord> requestRecords = new ArrayList<>();

    // send req
    for (int i = 0; i < numberOfRequests; i++) {

      SkierEvent skierEvent;

      try {
        skierEvent = skierEventQueue.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return; // exit if interrupt
      }

      Integer liftID = skierEvent.getLiftID();
      Integer liftTime = skierEvent.getTime();
      LiftRide liftRide = new LiftRide().liftID(liftID).time(liftTime);
      boolean requestSent = false;
      // send POST

      for (int j = 0; j < RETRY_TIMES; j++) {
        // req start time
        long startTime = System.currentTimeMillis();
        try {
          ApiResponse<Void> res = apiInstance
              .writeNewLiftRideWithHttpInfo(liftRide, skierEvent.getResortID(),
                  skierEvent.getSeasonID(), skierEvent.getDayID(), skierEvent.getSkierID());

          // req end time
          long endTime = System.currentTimeMillis();
          int responseCode = res.getStatusCode();

          requestRecords.add(new RequestRecord(startTime, endTime, "POST", responseCode));

          if (res.getStatusCode() == 201) {
            Part2Client.incrementSuccessfulRequests();
            requestSent = true;
            break;
          }
        } catch (ApiException ae) {
          System.out.println("E" + ae.getMessage());
        }

        if (!requestSent) {
          Part2Client.incrementFailedRequests();
        }
      }
      if (latch != null) {
        latch.countDown();
      }
    }

    // pass records to client
    Part2Client.addRequestRecords(requestRecords);
  }
}

