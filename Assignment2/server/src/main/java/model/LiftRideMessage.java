package model;

import lombok.Data;

@Data
public class LiftRideMessage {
  private final LiftRide liftRide;
  private final int resortId;
  private final String seasonId;
  private final String dayId;
  private final int skierId;
}
