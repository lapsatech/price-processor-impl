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

  /**
   * Put the rate update item to the queue
   * 
   * @param update
   */
  void offer(RateUpdate update);

  /**
   * Polls an item from the queue if present or <code>null</code> value if queue
   * is empty
   * 
   * @return an item taken from the queue or <code>null</code> if queue is empty
   */
  RateUpdate pollNoWait();

  /**
   * Polls an item from the queue if present or wait a given amount of time until
   * the queue has received an item and polls that item. If queue hasn't became
   * non-empty in a given amount of time the method returns <code>null</code>
   * 
   * @return an item from the queue or <code>null</code> if queue haven't receive
   *         any items in a given amount of time
   */
  RateUpdate poll(long time, TimeUnit unit) throws InterruptedException;

  /**
   * @return always return non-null item
   * @throws InterruptedException
   */
  RateUpdate take() throws InterruptedException;

  /**
   * @param time
   * @param unit
   * @return always return non-null item
   * @throws InterruptedException
   * @throws TimeoutException
   */
  RateUpdate take(long time, TimeUnit unit) throws InterruptedException, TimeoutException;
}