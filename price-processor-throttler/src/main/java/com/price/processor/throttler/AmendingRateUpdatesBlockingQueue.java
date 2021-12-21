package com.price.processor.throttler;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is implementing FIFO thread-safe queue containing updates of
 * currency pair rates. The currency pair rate value can be amended. The
 * amendment won't take effect on the queue position of the entry.
 * 
 */
public class AmendingRateUpdatesBlockingQueue implements RateUpdatesQueue {

  private final ReentrantLock lock = new ReentrantLock();
  private final Condition notEmpty = lock.newCondition();

  private final Map<String, Double> queue = new LinkedHashMap<>();

  @Override
  public void offer(String ccyPair, double rate) {
    try {
      lock.lock();
      queue.put(ccyPair, rate);
      notEmpty.signal();
    } finally {
      lock.unlock();
    }

  }

  @Override
  public Entry<String, Double> peek() {
    lock.lock();
    try {
      Iterator<Entry<String, Double>> i = queue.entrySet().iterator();
      Entry<String, Double> e = i.next();
      i.remove();
      return e;
    } catch (NoSuchElementException e1) {
      return null;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Entry<String, Double> take() throws InterruptedException {
    while (true) {
      Entry<String, Double> e = peek();
      if (e != null) {
        return e;
      }
      try {
        lock.lock();
        notEmpty.await();
      } finally {
        lock.unlock();
      }
    }
  }

  @Override
  public Entry<String, Double> take(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
    Entry<String, Double> e = peek();
    if (e != null) {
      return e;
    }
    try {
      lock.lock();
      if (notEmpty.await(time, unit)) {
        e = peek();
        if (e != null) {
          return e;
        }
      }
    } finally {
      lock.unlock();
    }
    throw new TimeoutException();
  }
}