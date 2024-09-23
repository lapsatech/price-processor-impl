package com.price.processor.throttler;

import java.util.NoSuchElementException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;

public class AmendingRateUpdatesBlockingQueue implements RateUpdatesBlockingQueue {

  private final ReentrantLock queueLock = new ReentrantLock();
  private final Condition addedToQueue = queueLock.newCondition();

  private final Object2DoubleMap<String> latestPrices = new Object2DoubleOpenHashMap<>();
  private final PriorityQueue<String> ccyPairs = new ObjectArrayFIFOQueue<>();

  @Override
  public void offer(String ccyPair, double rate) throws InterruptedException {
    queueLock.lockInterruptibly();
    try {
      if (!latestPrices.containsKey(ccyPair)) {
        ccyPairs.enqueue(ccyPair);
      }
      latestPrices.put(ccyPair, rate);
      addedToQueue.signal();
    } finally {
      queueLock.unlock();
    }
  }

  @Override
  public void take(UpdateConsumer updatesListener) throws InterruptedException {
    queueLock.lockInterruptibly();
    String ccyPair;
    double price;
    try {
      for (;;) {
        try {
          ccyPair = ccyPairs.dequeue();
          price = latestPrices.removeDouble(ccyPair);
          break;
        } catch (NoSuchElementException e) {
          addedToQueue.await();
        }
      }

    } finally {
      queueLock.unlock();
    }
    updatesListener.onPrice(ccyPair, price);
  }
}