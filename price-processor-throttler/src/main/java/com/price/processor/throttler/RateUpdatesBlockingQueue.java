package com.price.processor.throttler;

public interface RateUpdatesBlockingQueue {

  interface UpdateConsumer {
    void onPrice(String ccyPair, double rate);
  }

  void offer(String ccyPair, double rate) throws InterruptedException;

  void take(UpdateConsumer updatesListener) throws InterruptedException;

}
