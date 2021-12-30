package com.price.processor.throttler;

import java.util.NoSuchElementException;
import java.util.Objects;
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
      if (updates.put(update.getCcyPair(), update) == null // if new ccyPair
          && ccyPairQueue.add(update.getCcyPair())) { // and put to the queue successful
        notEmpty.signal();
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public RateUpdate pollNoWait() {
    final RateUpdate upd;
    if (!lock.tryLock()) {
      return null;
    }
    try {
      final String ccyPair;
      try {
        ccyPair = ccyPairQueue.remove();
      } catch (NoSuchElementException e) {
        return null;
      }
      upd = updates.remove(ccyPair);
    } finally {
      lock.unlock();
    }
    return Objects.requireNonNull(upd, "Integrity violation exception");
  }

  @Override
  public RateUpdate poll(long time, TimeUnit unit) throws InterruptedException {
    RateUpdate upd;
    if ((upd = pollNoWait()) != null) {
      return upd;
    }
    lock.lockInterruptibly();
    try {
      if (notEmpty.await(time, unit) &&
          (upd = pollNoWait()) != null) {
        return upd;
      }
    } finally {
      lock.unlock();
    }
    return null;
  }

  @Override
  public RateUpdate take() throws InterruptedException {
    RateUpdate upd;
    while ((upd = pollNoWait()) == null) {
      lock.lockInterruptibly();
      try {
        notEmpty.await();
      } finally {
        lock.unlock();
      }
    }
    return upd;
  }

  @Override
  public RateUpdate take(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
    RateUpdate upd = poll(time, unit);
    if (upd != null) {
      return upd;
    }
    throw new TimeoutException();
  }

}