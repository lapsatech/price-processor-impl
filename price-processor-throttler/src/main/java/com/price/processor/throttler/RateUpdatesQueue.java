package com.price.processor.throttler;

import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface RateUpdatesQueue {

  void offer(String ccyPair, double rate);

  Entry<String, Double> peek();

  Entry<String, Double> take() throws InterruptedException;

  Entry<String, Double> take(long time, TimeUnit unit) throws InterruptedException, TimeoutException;
}