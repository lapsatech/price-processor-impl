package com.price.processor.throttler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.price.processor.PriceProcessor;

public class PriceThrottler implements PriceProcessor, AutoCloseable {

  private final ExecutorService threadPool;

  private final ConcurrentHashMap<PriceProcessor, Holder> procs = new ConcurrentHashMap<>();

  private static class Holder {

    private final QueuedPriceProcesorJob proc;
    private final Future<?> future;

    private Holder(QueuedPriceProcesorJob proc, Future<?> future) {
      this.proc = proc;
      this.future = future;
    }
  }

  public PriceThrottler(ExecutorService threadPool) {
    this.threadPool = threadPool;
  }

  @Override
  public void onPrice(String ccyPair, double rate) {
    procs.forEach((pp, holder) -> holder.proc.queue(ccyPair, rate));
  }

  @Override
  public void subscribe(PriceProcessor priceProcessor) {
    if (priceProcessor == this) {
      throw new IllegalArgumentException("Infinity loop. Can't subscribe to itself");
    }
    procs.computeIfAbsent(priceProcessor, pp -> {
      QueuedPriceProcesorJob proc = new QueuedPriceProcesorJob(priceProcessor);
      Future<?> processFuture = threadPool.submit(proc);
      return new Holder(proc, processFuture);
    });
  }

  public int subscribers() {
    return procs.size();
  }

  @Override
  public void unsubscribe(PriceProcessor priceProcessor) {
    unsubscribe(priceProcessor, false); // Stops queueing new updates but allows remaining ones to be handled.
                                        // Obviously this option must be aligned with the task requirements
  }

  public void unsubscribe(PriceProcessor priceProcessor, boolean stopImmediate) {
    procs.computeIfPresent(priceProcessor, (pp, holder) -> {
      if (stopImmediate) {
        stop(holder.future);
      }
      return null; // removes given processor from the registry
    });
  }

  private void stop(Future<?> future) {
    if (!future.isDone()) {
      future.cancel(true);
    }
  }

  @Override
  public void close() {
    procs.forEach((pp, holder) -> stop(holder.future));
  }

}
