package com.price.processor.throttler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.price.processor.PriceProcessor;

public class PriceThrottler implements PriceProcessor {

  private static class Runner implements Runnable {

    private final RateUpdatesBlockingQueue queue;
    private final PriceProcessor priceProcessor;
    private volatile boolean stopped = false;

    private Runner(PriceProcessor priceProcessor, RateUpdatesBlockingQueue queue) {
      this.priceProcessor = priceProcessor;
      this.queue = queue;
    }

    @Override
    public void run() {
      while (!stopped) {
        try {
          queue.take(priceProcessor::onPrice);
        } catch (RuntimeException e) {
          // log error
        } catch (InterruptedException e) {
          stopped = true;
        }
      }
    }
  }

  private final Lock subscribersReader, subscribersWriter;
  {
    ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
    subscribersReader = rw.readLock();
    subscribersWriter = rw.writeLock();
  }

  private final ExecutorService threadPool;

  private final Map<PriceProcessor, RateUpdatesBlockingQueue> queues = new HashMap<>();
  private final NavigableMap<PriceProcessor, CompletableFuture<Void>> processes = new TreeMap<>();

  public PriceThrottler(ExecutorService threadPool) {
    this.threadPool = threadPool;
  }

  @Override
  public void onPrice(String ccyPair, double rate) {
    subscribersReader.lock();
    try {
      for (RateUpdatesBlockingQueue queue : queues.values()) {
        try {
          queue.offer(ccyPair, rate);
        } catch (InterruptedException e) {
          throw new ProcessShutdownException(e);
        }
      }

    } finally {
      subscribersReader.unlock();
    }
  }

  @Override
  public void subscribe(PriceProcessor priceProcessor) {
    subscribersWriter.lock();
    try {
      if (priceProcessor == this) {
        throw new IllegalArgumentException("Can't subscribe to itself");
      }

      RateUpdatesBlockingQueue queue = new AmendingRateUpdatesBlockingQueue();
      Runnable runner = new Runner(priceProcessor, queue);
      CompletableFuture<Void> process = CompletableFuture.runAsync(runner, threadPool);

      queues.put(priceProcessor, queue);
      processes.put(priceProcessor, process);
    } finally {
      subscribersWriter.unlock();
    }
  }

  @Override
  public void unsubscribe(PriceProcessor priceProcessor) {
    subscribersWriter.lock();
    try {
      CompletableFuture<Void> process = processes.remove(priceProcessor);
      if (process != null) {
        process.cancel(true);
        process.join();
        queues.remove(priceProcessor);
      }
    } finally {
      subscribersWriter.unlock();
    }
  }

  public void unsubscribeAll() {
    subscribersWriter.lock();
    try {
      while (!processes.isEmpty()) {
        Entry<PriceProcessor, CompletableFuture<Void>> e = processes.firstEntry();
        unsubscribe(e.getKey());
      }
    } finally {
      subscribersWriter.unlock();
    }
  }

  public int getSubscribersCount() {
    subscribersReader.lock();
    try {
      return processes.size();
    } finally {
      subscribersReader.unlock();
    }
  }
}
