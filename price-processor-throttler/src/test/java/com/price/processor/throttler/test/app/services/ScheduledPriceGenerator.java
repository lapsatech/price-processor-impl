package com.price.processor.throttler.test.app.services;

import java.time.Duration;

public interface ScheduledPriceGenerator {

  void generate();

  Duration getInitialDelay();

  Duration getFrequency();
}