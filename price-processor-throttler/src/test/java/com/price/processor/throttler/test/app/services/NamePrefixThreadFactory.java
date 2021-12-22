package com.price.processor.throttler.test.app.services;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamePrefixThreadFactory implements ThreadFactory {

  final AtomicInteger counter = new AtomicInteger();
  final String namePrefix;

  public NamePrefixThreadFactory(String namePrefix) {
    this.namePrefix = namePrefix;
  }

  @Override
  public Thread newThread(Runnable r) {
    return new Thread(r, String.format("%s-%d", namePrefix, counter.incrementAndGet()));
  }
}