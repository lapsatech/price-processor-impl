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
public class AmendingRateUpdatesBlockingQueue implements RateUpdatesBlockingQueue {

  private final ReentrantLock lock = new ReentrantLock();
  private final Condition notEmpty = lock.newCondition();

  private final Map<String, RateUpdate> queue = new LinkedHashMap<>();

  @Override
  public void offer(RateUpdate update) {
    lock.lock();
    try {
      queue.put(update.getCcyPair(), update);
      notEmpty.signal();
    } finally {
      lock.unlock();
    }

  }

  @Override
  public RateUpdate poll() {
    lock.lock();
    try {
      Iterator<Entry<String, RateUpdate>> i = queue.entrySet().iterator();
      Entry<String, RateUpdate> e = i.next();
      i.remove();
      return e.getValue();
    } catch (NoSuchElementException e1) {
      return null;
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