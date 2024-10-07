package model;

public class RequestRecord {
  private long startTime;
  private long endTime;
  private String requestType;
  private int responseCode;

  public RequestRecord(long startTime, long endTime, String requestType, int responseCode) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.requestType = requestType;
    this.responseCode = responseCode;
  }

  public long getLatency() {
    return endTime - startTime;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public String getRequestType() {
    return requestType;
  }

  public int getResponseCode() {
    return responseCode;
  }
}
