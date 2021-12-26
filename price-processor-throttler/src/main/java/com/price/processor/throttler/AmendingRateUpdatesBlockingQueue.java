package com.price.processor.throttler;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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

  private final ConcurrentHashMap<String, RateUpdate> updates = new ConcurrentHashMap<>(200);
  private final ConcurrentLinkedQueue<String> ccyPairQueue = new ConcurrentLinkedQueue<>();

  @Override
  public void offer(RateUpdate update) {
    lock.lock();
    try {
      if (updates.put(update.getCcyPair(), update) == null) {// if new ccyPair
        if (ccyPairQueue.offer(update.getCcyPair())) { // if put to the queue successful
          notEmpty.signal();
        }
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public RateUpdate poll() {
    final RateUpdate update;
    lock.lock();
    try {
      final String ccyPair = ccyPairQueue.poll();
      if (ccyPair == null) {
        return null;
      }
      update = updates.remove(ccyPair);
    } finally {
      lock.unlock();
    }
    return requireNonNull(update, "Integrity violation exception");
  }

  @Override
  public RateUpdate poll(long time, TimeUnit unit) throws InterruptedException {
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
    return null;
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
    RateUpdate e = poll(time, unit);
    if (e != null) {
      return e;
    }
    throw new TimeoutException();
  }

}