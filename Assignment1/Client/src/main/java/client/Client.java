package client;

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
import thread.SkierEventThread;
import thread.SkierThread;

public class Client {

  private static final int TOTAL_REQUESTS = 200000; // Total lift ride events
  private static final int PHASE1_NUM_OF_THREADS = 32; // PHASE 1
  private static final int PHASE2_NUM_OF_THREADS = 56;
  private static final int PHASE1_REQUESTS_PER_THREAD = 1000; // Requests per initial thread
  private static final int PHASE2_REQUESTS_PER_THREAD = 3000;

  private static int successfulRequestsPhase1 = 0;

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  private static int failedRequestsPhase1 = 0;
  private static int successfulRequestsPhase2 = 0;
  private static int failedRequestsPhase2 = 0;

  private static final List<RequestRecord> allRequestRecords = Collections.synchronizedList(new ArrayList<RequestRecord>());

  public static void main(String[] args) {
    long totalStartTime = System.currentTimeMillis();

    BlockingDeque<SkierEvent> skierEventQueue = new LinkedBlockingDeque<>(TOTAL_REQUESTS);
    CountDownLatch phase1Latch = new CountDownLatch(PHASE1_NUM_OF_THREADS);
    CountDownLatch completionLatch = new CountDownLatch(PHASE2_NUM_OF_THREADS);

    Thread generatorThread = new Thread(new SkierEventThread(skierEventQueue, TOTAL_REQUESTS));
    generatorThread.start();

    ExecutorService executor = Executors.newFixedThreadPool(PHASE1_NUM_OF_THREADS + PHASE2_NUM_OF_THREADS);

    // PHASE 1
    long phase1StartTime = System.currentTimeMillis();
    for (int i = 0; i < PHASE1_NUM_OF_THREADS; i++) {
      executor.execute(new SkierThread(PHASE1_REQUESTS_PER_THREAD, skierEventQueue, phase1Latch, null, "phase1"));
    }

    try {
      phase1Latch.await(); // Wait for phase one threads to complete
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    long phase1EndTime = System.currentTimeMillis();

    // PHASE 2
    int remainingRequests = TOTAL_REQUESTS - (PHASE1_REQUESTS_PER_THREAD * PHASE1_NUM_OF_THREADS);
    while (remainingRequests > 0) {
      int requestsToSend = Math.min(remainingRequests, PHASE2_REQUESTS_PER_THREAD);
      executor.execute(new SkierThread(requestsToSend, skierEventQueue, null, completionLatch, "phase2"));
      remainingRequests -= requestsToSend;
    }

    try {
      completionLatch.await(); // Wait for all phase two threads to complete
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    long phase2EndTime = System.currentTimeMillis();
    executor.shutdown();
    while (!executor.isTerminated()) {
      Thread.yield();
    }

    long totalEndTime = System.currentTimeMillis();

    // Stats for Phase 1
    printPhaseResults("Phase 1", successfulRequestsPhase1, failedRequestsPhase1, phase1StartTime, phase1EndTime);
    // Stats for Phase 2
    printPhaseResults("Phase 2", successfulRequestsPhase2, failedRequestsPhase2, phase2EndTime, totalEndTime);

    // Total stats
    long totalTime = totalEndTime - totalStartTime;
    System.out.println("Total run time (ms): " + totalTime);
    System.out.println("Total successful requests: " + (successfulRequestsPhase1 + successfulRequestsPhase2) + "/" + TOTAL_REQUESTS);
    System.out.println("Total unsuccessful requests: " + (failedRequestsPhase1 + failedRequestsPhase2));
    System.out.println("Total throughput (requests/second): " + (TOTAL_REQUESTS * 1000.0 / totalTime));
  }

  private static void printPhaseResults(String phase, int success, int failed, long start, long end) {
    long wallTime = end - start;
    long throughput = (success + failed) > 0 ? 1000 * (success + failed) / wallTime : 0;

    System.out.println("\nClient " + phase + " Result:");
    System.out.println("-----------------------------------------------");
    System.out.println("Number of successful requests sent: " + success);
    System.out.println("Number of unsuccessful requests: " + failed);
    System.out.println("The total run time (wall time): " + wallTime + " milliseconds");
    System.out.println("The total throughput per second: " + throughput);
  }

  public static synchronized void incrementSuccessfulRequests(String phase) {
    if ("phase1".equals(phase)) {
      successfulRequestsPhase1++;
    } else if ("phase2".equals(phase)) {
      successfulRequestsPhase2++;
    }
  }

  public static synchronized void incrementFailedRequests(String phase) {
    if ("phase1".equals(phase)) {
      failedRequestsPhase1++;
    } else if ("phase2".equals(phase)) {
      failedRequestsPhase2++;
    }
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
