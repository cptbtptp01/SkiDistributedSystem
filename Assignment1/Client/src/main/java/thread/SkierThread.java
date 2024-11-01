package thread;

import client.Client;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import model.RequestRecord;
import model.SkierEvent;

public class SkierThread implements Runnable {

  private static int RETRY_TIMES = 5;

  private final Integer numberOfRequests;
  private final BlockingDeque<SkierEvent> skierEventQueue;
  private final CountDownLatch phase1Latch;
  private final CountDownLatch completionLatch;

  private final String phase;

  public SkierThread(Integer numberOfRequests, BlockingDeque<SkierEvent> skierEventQueue, CountDownLatch phase1Latch, CountDownLatch completionLatch, String phase) {
    this.numberOfRequests = numberOfRequests;
    this.skierEventQueue = skierEventQueue;
    this.phase1Latch = phase1Latch;
    this.completionLatch = completionLatch;
    this.phase = phase;
  }

  @Override
  public void run() {
    SkiersApi apiInstance = new SkiersApi();
    ApiClient client = apiInstance.getApiClient();
//     client.setBasePath("http://localhost:8080/server_war_exploded/");
    client.setBasePath("http://35.90.22.97:8080/server_war/");

//    client.setConnectTimeout(5000);
//    client.setReadTimeout(5000);

    // records for thread
    List<RequestRecord> requestRecords = new ArrayList<>();

    // send req
    for (int i = 0; i < numberOfRequests; i++) {

      SkierEvent skierEvent;

      try {
        skierEvent = skierEventQueue.take();
        if (skierEvent == null) {
          System.out.println("Warning: Received null event from queue after waiting 1 second");
          continue;
        }
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
            Client.incrementSuccessfulRequests(phase);
            requestSent = true;
            break;
          }
        } catch (ApiException ae) {
          System.out.println("ApiException" + ae.getMessage());
        }

        if (!requestSent) {
          Client.incrementFailedRequests(phase);
        }
      }
      if (phase1Latch != null) {
        phase1Latch.countDown();
      }
      if (completionLatch != null) {
        completionLatch.countDown();
      }
    }

    // pass records to client
    Client.addRequestRecords(requestRecords);
  }
}

