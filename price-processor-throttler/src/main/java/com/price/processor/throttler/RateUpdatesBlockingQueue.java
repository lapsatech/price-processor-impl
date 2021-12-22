package com.price.processor.throttler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface RateUpdatesBlockingQueue {

  public interface RateUpdate {

    static RateUpdate of(String ccyPair, double rate) {
      return new RateUpdate() {

        @Override
        public String getCcyPair() {
          return ccyPair;
        }

        @Override
        public double getRate() {
          return rate;
        }
      };
    }

    String getCcyPair();

    double getRate();
  }

  void offer(RateUpdate update);

  RateUpdate poll();

  RateUpdate take() throws InterruptedException;

  RateUpdate take(long time, TimeUnit unit) throws InterruptedException, TimeoutException;
}