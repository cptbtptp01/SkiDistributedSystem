package Part2;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import model.RequestRecord;
import model.SkierEvent;
import model.SkierEventGenerator;

public class Part2Client {

  private static final int TOTAL_REQUESTS = 200000; // Total lift ride events
  private static final int NUM_OF_THREADS = 128; // Initial number of threads
  private static final int REQUESTS_PER_THREAD = 1000; // Requests per initial thread

  private static int successfulRequests = 0;
  private static int failedRequests = 0;

  private static final List<RequestRecord> allRequestRecords = Collections.synchronizedList(new ArrayList<RequestRecord>());

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

    processRequestRecords();
    writeRecordsToCSV("src/main/java/Part2/request_records_128.csv");

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

  public static synchronized void addRequestRecords(List<RequestRecord> records) {
    allRequestRecords.addAll(records);
  }

  private static void processRequestRecords() {
    List<Long> latencies = new ArrayList<>();
    for (RequestRecord record : allRequestRecords) {
      latencies.add(record.getLatency());
    }

    Collections.sort(latencies);

    long min = latencies.get(0);
    long max = latencies.get(latencies.size() - 1);
    double mean = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
    double median = latencies.get(latencies.size() / 2);
    double p99 = latencies.get((int) (latencies.size() * 0.99));

    System.out.println("Min response time (ms): " + min);
    System.out.println("Max response time (ms): " + max);
    System.out.println("Mean response time (ms): " + mean);
    System.out.println("Median response time (ms): " + median);
    System.out.println("99th percentile response time (ms): " + p99);
  }

  private static void writeRecordsToCSV(String fileName) {
    try (FileWriter writer = new FileWriter(fileName)) {
      writer.append("StartTime,RequestType,Latency,ResponseCode\n");
      for (RequestRecord record : allRequestRecords) {
        writer.append(record.getStartTime() + "," + record.getRequestType() + "," + record.getLatency() + "," + record.getResponseCode() + "\n");
      }
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
