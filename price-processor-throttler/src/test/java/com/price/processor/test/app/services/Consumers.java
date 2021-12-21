package com.price.processor.test.app.services;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.DisposableBean;

public class Consumers implements DisposableBean {

  private final List<SamplePriceConsumer> consumers;

  public Consumers(List<SamplePriceConsumer> consumers) {
    this.consumers = consumers;
  }

  public Stream<SamplePriceConsumer> stream() {
    return consumers.stream();
  }

  public void logStats() {
    consumers.forEach(SamplePriceConsumer::logStats);
  }

  @Override
  public void destroy() throws Exception {
    logStats();
  }
}