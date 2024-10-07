package Part1;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import model.SkierEvent;
import model.SkierEventGenerator;

public class Part1Client {

  private static final int TOTAL_REQUESTS = 200000; // Total lift ride events
  private static final int NUM_OF_THREADS = 32; // Initial number of threads
  private static final int REQUESTS_PER_THREAD = 1000; // Requests per initial thread

  private static int successfulRequests = 0;
  private static int failedRequests = 0;

  public static void main(String[] args) {
    long startTime = System.currentTimeMillis();

    BlockingDeque<SkierEvent> skierEventQueue = new LinkedBlockingDeque<>(TOTAL_REQUESTS);
    CountDownLatch latch = new CountDownLatch(NUM_OF_THREADS);

    Thread generatorThread = new Thread(new SkierEventGenerator(skierEventQueue, TOTAL_REQUESTS));
    generatorThread.start();

    ExecutorService executor = Executors.newFixedThreadPool(NUM_OF_THREADS);

    for (int i = 0; i < NUM_OF_THREADS; i++) {
      executor.execute(new SkierThread(REQUESTS_PER_THREAD, skierEventQueue, latch));
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    int remainingRequests = TOTAL_REQUESTS - (NUM_OF_THREADS * REQUESTS_PER_THREAD);
    while (remainingRequests > 0) {
      int requestsToSend = Math.min(remainingRequests, REQUESTS_PER_THREAD);
      executor.execute(new SkierThread(requestsToSend, skierEventQueue, null));
      remainingRequests -= requestsToSend;
    }

    executor.shutdown();
    while (!executor.isTerminated()) {

    }

    long endTime = System.currentTimeMillis();
    long totalTime = endTime - startTime;

    // stats
    System.out.println("\nNumber of thread: " + NUM_OF_THREADS);
    System.out.println("Request per thread: " + REQUESTS_PER_THREAD);
    System.out.println(
        "Number of successful requests sent: " + successfulRequests + "/" + TOTAL_REQUESTS);
    System.out.println("Number of unsuccessful requests: " + failedRequests);
    System.out.println("Total run time (ms): " + totalTime);
    System.out.println(
        "Total throughput (requests/second): " + (TOTAL_REQUESTS * 1000.0 / totalTime));
  }

  public static synchronized void incrementSuccessfulRequests() {
    successfulRequests++;
  }

  public static synchronized void incrementFailedRequests() {
    failedRequests++;
  }
}
