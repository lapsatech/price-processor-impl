package com.price.processor.throttler;

@SuppressWarnings("serial")
public class ProcessShutdownException extends RuntimeException {

  public ProcessShutdownException() {
    super();
  }

  public ProcessShutdownException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProcessShutdownException(String message) {
    super(message);
  }

  public ProcessShutdownException(Throwable cause) {
    super(cause);
  }

}
