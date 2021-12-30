package com.price.processor.throttler.test.app.services;

import java.time.Duration;
import java.util.Random;

import com.price.processor.PriceProcessor;

public class DefaultScheduledPriceGenerator implements ScheduledPriceGenerator {

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

    public Builder withConsumer(PriceProcessor priceProcessor) {
      this.priceProcessor = priceProcessor;
      return this;
    }

    public ScheduledPriceGenerator build() {
      return new DefaultScheduledPriceGenerator(priceProcessor, ccyPair, frequency, initialDelay);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private final Random random = new Random();
  private final PriceProcessor consumer;
  private final String ccyPair;

  private final Duration frequency;
  private final Duration initialDelay;

  private DefaultScheduledPriceGenerator(PriceProcessor consumer, String ccyPair, Duration frequency,
      Duration initialDelay) {
    this.consumer = consumer;
    this.ccyPair = ccyPair;
    this.frequency = frequency;
    this.initialDelay = initialDelay;
  }

  @Override
  public void generate() {
    consumer.onPrice(ccyPair, random.nextDouble());
  }

  @Override
  public Duration getInitialDelay() {
    return initialDelay;
  }

  @Override
  public Duration getFrequency() {
    return frequency;
  }

}
