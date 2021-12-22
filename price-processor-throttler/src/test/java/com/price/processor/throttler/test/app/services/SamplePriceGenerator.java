package com.price.processor.throttler.test.app.services;

import static com.price.processor.throttler.DurationUtils.threadSleep;
import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.price.processor.PriceProcessor;
import com.price.processor.throttler.DurationMetrics;
import com.price.processor.throttler.DurationMetrics.Measure;

public class SamplePriceGenerator implements Runnable {

  public static class Builder {

    private String ccyPair;
    private Duration initialDelay = Duration.ZERO;
    private Duration frequency = Duration.ofMillis(100);
    private PriceProcessor priceProcessor;

    private Builder() {
    }

    public Builder withCcyPair(String ccyPair) {
      this.ccyPair = ccyPair;
      return this;
    }

    public Builder withInitialDelay(Duration initialDelay) {
      this.initialDelay = initialDelay;
      return this;
    }

    public Builder withFrequency(Duration frequency) {
      this.frequency = frequency;
      return this;
    }

    public Builder withFrequency(long milis, int nanos) {
      this.frequency = Duration.ofMillis(milis).plusNanos(nanos);
      return this;
    }

    public Builder withFrequencyNanos(int nanos) {
      this.frequency = Duration.ofNanos(nanos);
      return this;
    }

    public Builder withFrequencyMillis(long milis) {
      this.frequency = Duration.ofMillis(milis);
      return this;
    }

    public SamplePriceGenerator build() {
      return new SamplePriceGenerator(priceProcessor, ccyPair, frequency, initialDelay);
    }

    public Builder withConsumer(PriceProcessor priceProcessor) {
      this.priceProcessor = priceProcessor;
      return this;
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(SamplePriceGenerator.class);

  public static Builder builder() {
    return new Builder();
  }

  private final Random random = new Random();

  private final String ccyPair;
  private final Duration frequency;
  private final Duration initialDelay;
  private final PriceProcessor priceProcessor;
  private final DurationMetrics consumerPerfomanceMetrics = new DurationMetrics();

  public SamplePriceGenerator(PriceProcessor priceProcessor, String ccyPair, Duration frequency, Duration initialDelay) {
    this.ccyPair = requireNonNull(ccyPair, "ccyPair");
    this.frequency = requireNonNull(frequency, "frequency");
    this.initialDelay = requireNonNull(initialDelay, "initialDelay");
    this.priceProcessor = requireNonNull(priceProcessor, "priceProcessor");
  }

  @Override
  public void run() {
    try {
      threadSleep(initialDelay);
      while (!Thread.currentThread().isInterrupted()) {
        Measure m = consumerPerfomanceMetrics.newMeasure();
        priceProcessor.onPrice(ccyPair, random.nextDouble());
        m.complete();
        threadSleep(frequency);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void logStats() {
    LOGGER.debug("<--- '{}' pair consumption stats are {}", ccyPair, consumerPerfomanceMetrics.getStats());
  }

}