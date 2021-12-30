package com.price.processor.throttler.test.app.services;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.DisposableBean;

public class Generators implements DisposableBean {

  private final ScheduledExecutorService threadPool;
  private final List<ScheduledPriceGenerator> generators;

  private List<ScheduledFuture<?>> futures;

  public Generators(ScheduledExecutorService threadPool, List<ScheduledPriceGenerator> generators) {
    this.threadPool = threadPool;
    this.generators = generators;
  }

  public void start() {
    this.futures = generators.stream()
        .map(generator -> threadPool.scheduleWithFixedDelay(generator::generate, generator.getInitialDelay().toNanos(),
            generator.getFrequency().toNanos(), TimeUnit.NANOSECONDS))
        .collect(Collectors.toList());
  }

  public void stop() {
    futures.forEach(future -> {
      if (!future.isDone()) {
        future.cancel(true);
      }
    });
  }

  @Override
  public void destroy() throws Exception {
    stop();
  }

}