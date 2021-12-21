package com.price.processor.throttler;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.price.processor.PriceProcessor;

public class PriceThrottler implements PriceProcessor, AutoCloseable {

  private transient boolean stateClosed = false;

  private static class RegistryEntry {

    private final QueuedPriceProcesorJob proc;
    private final Future<?> future;

    private RegistryEntry(QueuedPriceProcesorJob proc, Future<?> processFuture) {
      this.proc = proc;
      this.future = processFuture;
    }
  }

  private final ExecutorService threadPool;
  private final ConcurrentHashMap<PriceProcessor, RegistryEntry> processorsRegistry = new ConcurrentHashMap<>();

  public PriceThrottler(ExecutorService threadPool) {
    this.threadPool = threadPool;
  }

  @Override
  public void onPrice(String ccyPair, double rate) {
    processorsRegistry.forEach((priceProcessor, registryEntry) -> registryEntry.proc.queue(ccyPair, rate));
  }

  @Override
  public void subscribe(PriceProcessor priceProcessor) {
    if (stateClosed) {
      throw new IllegalStateException("Resource is closed");
    }
    if (priceProcessor == this) {
      throw new IllegalArgumentException("Infinity loop. Can't subscribe to itself");
    }
    processorsRegistry.computeIfAbsent(priceProcessor, pp -> {
      QueuedPriceProcesorJob proc = new QueuedPriceProcesorJob(priceProcessor);
      Future<?> processFuture = threadPool.submit(proc);
      return new RegistryEntry(proc, processFuture);
    });
  }

  @Override
  public void unsubscribe(PriceProcessor priceProcessor) {
    processorsRegistry.computeIfPresent(priceProcessor, (pp, registryEntry) -> {
      if (!registryEntry.future.isDone()) {
        registryEntry.future.cancel(true);
      }
      return null; // removes given processor from the registry
    });
  }

  public int getSubscribersCount() {
    return processorsRegistry.size();
  }

  @Override
  public void close() {
    stateClosed = true;
    while (!processorsRegistry.isEmpty()) {
      Enumeration<PriceProcessor> procs = processorsRegistry.keys();
      while (procs.hasMoreElements()) {
        unsubscribe(procs.nextElement());
      }
    }
  }
}
