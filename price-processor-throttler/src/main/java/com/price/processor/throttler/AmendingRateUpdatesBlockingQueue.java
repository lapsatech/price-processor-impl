package com.price.processor.throttler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
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
public class AmendingRateUpdatesBlockingQueue implements RateUpdatesBlockingQueue {

  private final ReentrantLock lock = new ReentrantLock();
  private final Condition notEmpty = lock.newCondition();

  private final Map<String, RateUpdate> rates = new HashMap<>(200);
  private final Queue<String> queue = new LinkedList<>();

  @Override
  public void offer(RateUpdate update) {
    lock.lock();
    try {
      if (rates.put(update.getCcyPair(), update) == null) {// if new ccyPair
        queue.offer(update.getCcyPair()); // put to the queue
        notEmpty.signal();
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public RateUpdate poll() {
    lock.lock();
    try {
      final String ccyPair = queue.poll();
      if (ccyPair == null) {
        return null;
      }
      return rates.remove(ccyPair);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public RateUpdate take() throws InterruptedException {
    RateUpdate e;
    while ((e = poll()) == null) {
      lock.lock();
      try {
        notEmpty.await();
      } finally {
        lock.unlock();
      }
    }
    return e;
  }

  @Override
  public RateUpdate take(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
    RateUpdate e = poll();
    if (e != null) {
      return e;
    }

    lock.lock();
    try {
      if (notEmpty.await(time, unit)) {
        e = poll();
      }
    } finally {
      lock.unlock();
    }

    if (e != null) {
      return e;
    }
    throw new TimeoutException();
  }

}