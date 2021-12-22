package com.price.processor.throttler.test.app.services;

import static com.price.processor.throttler.DurationUtils.threadSleep;
import static java.util.Objects.requireNonNull;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.price.processor.PriceProcessor;
import com.price.processor.throttler.DurationMetrics;
import com.price.processor.throttler.DurationMetrics.Measure;

public class SamplePriceConsumer implements PriceProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(SamplePriceConsumer.class);

  public static class Builder {

    private Duration processTime;
    private String name = "UNNAMED";

    private Builder() {
    }

    public Builder withProcessTime(Duration processTime) {
      this.processTime = processTime;
      return this;
    }

    public Builder withProcessTimeMilis(long workTimeMilis) {
      this.processTime = Duration.ofMillis(workTimeMilis);
      return this;
    }

    public Builder withProcessTimeNanos(long workTimeNanos) {
      this.processTime = Duration.ofNanos(workTimeNanos);
      return this;
    }

    public Builder withProcessTime(long workTimeMilis, long workTimeNanos) {
      this.processTime = Duration.ofMillis(workTimeMilis).plusNanos(workTimeNanos);
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public SamplePriceConsumer build() {
      return new SamplePriceConsumer(processTime, name);
    }
  }

  public static Builder buillder() {
    return new Builder();
  }

  private final Duration processTime;

  private final String name;

  private SamplePriceConsumer(Duration processTime, String name) {
    this.processTime = requireNonNull(processTime, "processTime");
    this.name = requireNonNull(name, "name");
  }

  private final DurationMetrics metrics = new DurationMetrics();

  @Override
  public void onPrice(String ccyPair, double rate) {
    try {
      Measure measure = metrics.newGroupedMeasure(ccyPair);
      threadSleep(processTime);
      measure.complete();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void logStats() {
    LOGGER.debug("===> '{}' overall stats are {}", name, metrics.getStats());
    metrics.getGroupsStats()
        .forEach((ccyPair, stats) -> LOGGER.debug("===> '{}' >>> '{}' stats are {}", name, ccyPair, stats));
  }

  @Override
  public void subscribe(PriceProcessor priceProcessor) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void unsubscribe(PriceProcessor priceProcessor) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public String toString() {
    return name;
  }
}