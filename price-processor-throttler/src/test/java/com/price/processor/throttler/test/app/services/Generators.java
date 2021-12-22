package com.price.processor.throttler.test.app.services;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.DisposableBean;

public class Generators implements DisposableBean {

  private final ExecutorService threadPool;
  private final List<SamplePriceGenerator> generators;

  private List<Future<?>> futures;

  public Generators(ExecutorService threadPool, List<SamplePriceGenerator> generators) {
    this.threadPool = threadPool;
    this.generators = generators;
  }

  public void start() {
    this.futures = generators.stream()
        .map(threadPool::submit)
        .collect(Collectors.toList());
  }

  public void stop() {
    futures.forEach(future -> {
      if (!future.isDone()) {
        future.cancel(true);
      }
    });
  }

  public void logStats() {
    generators.forEach(SamplePriceGenerator::logStats);
  }

  public Stream<SamplePriceGenerator> stream() {
    return generators.stream();
  }

  @Override
  public void destroy() throws Exception {
    stop();
    logStats();
  }

}