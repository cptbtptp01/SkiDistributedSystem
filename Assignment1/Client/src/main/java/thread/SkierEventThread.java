package thread;

import java.util.Random;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;
import model.SkierEvent;


public class SkierEventThread implements Runnable {
  private final BlockingDeque<SkierEvent> skierEventQueue;
  private final int totalEvents;
  private Random random;

  public SkierEventThread(BlockingDeque<SkierEvent> skierEventQueue, int totalEvents) {
    this.skierEventQueue = skierEventQueue;
    this.totalEvents = totalEvents;
    this.random = new Random();
  }

  @Override
  public void run() {
    for (int i = 0; i < totalEvents; i++) {

      int skierID = random.nextInt(100000) + 1;  // Between 1 and 100000
      int resortID = random.nextInt(10) + 1;     // Between 1 and 10
      int liftID = random.nextInt(40) + 1;       // Between 1 and 40
      String seasonID = "2024";                  // Fixed value
      String dayID = "1";                        // Fixed value
      int time = random.nextInt(360) + 1;        // Between 1 and 360

      SkierEvent event = new SkierEvent(skierID, resortID, liftID, seasonID, dayID, time);

      try {
        skierEventQueue.put(event);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
