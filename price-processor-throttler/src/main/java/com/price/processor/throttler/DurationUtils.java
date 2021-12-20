package com.price.processor.throttler;

import java.time.Duration;

public class DurationUtils {

  public static void threadSleep(Duration d) throws InterruptedException {
    final long nanos = d.toNanos();
    Thread.sleep(nanos / 1_000_000, (int) (nanos % 1_000_000));
  }

  public static String toHumanReadable(Duration d) {
    return d == null
        ? "n/a"
        : d.toString()
            .substring(2)
            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
            .toLowerCase();

  }

}
