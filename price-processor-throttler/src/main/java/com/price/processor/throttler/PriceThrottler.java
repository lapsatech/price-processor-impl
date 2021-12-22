package com.price.processor.throttler;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.price.processor.PriceProcessor;
import com.price.processor.throttler.DurationMetrics.Measure;
import com.price.processor.throttler.DurationMetrics.Stats;
import com.price.processor.throttler.RateUpdatesBlockingQueue.RateUpdate;

public class PriceThrottler implements PriceProcessor, AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(PriceThrottler.class);

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
  private final DurationMetrics onPricePerfomance;

  private transient boolean stateClosed = false;

  public PriceThrottler(ExecutorService threadPool) {
    this(threadPool, false);
  }

  public PriceThrottler(ExecutorService threadPool, boolean collectStats) {
    this.threadPool = threadPool;
    this.onPricePerfomance = collectStats
        ? new DurationMetrics()
        : null;
  }

  @Override
  public void onPrice(String ccyPair, double rate) {
    if (stateClosed) {
      throw new IllegalStateException("Resource is closed");
    }
    final Measure m = onPricePerfomance == null
        ? null
        : onPricePerfomance.newMeasure();

    final RateUpdate update = RateUpdate.of(ccyPair, rate);

    try {
      processorsRegistry.forEachValue(Long.MAX_VALUE, registryEntry -> registryEntry.proc.queue(update));
    } finally {
      if (m != null) {
        m.complete();
      }
    }
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
      QueuedPriceProcesorJob proc = new QueuedPriceProcesorJob(pp, onPricePerfomance != null);
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
      final Stats stats;
      if ((stats = registryEntry.proc.getProcessorPerfomanceStats()) != null) {
        LOG.info("PriceProcessor '{}' perfomance stats are {}", pp, stats);
      }
      return null; // removes given processor from the registry
    });
  }

  public int getSubscribersCount() {
    return processorsRegistry.size();
  }

  public Stats getOnPricePerfomanceStats() {
    return onPricePerfomance == null
        ? null
        : onPricePerfomance.getStats();
  }

  @Override
  public void close() {
    stateClosed = true;
    if (onPricePerfomance != null) {
      LOG.info("PriceThrottler this.onPrice() perfomance stats are {}", onPricePerfomance.getStats());
    }
    while (!processorsRegistry.isEmpty()) {
      Enumeration<PriceProcessor> procs = processorsRegistry.keys();
      while (procs.hasMoreElements()) {
        unsubscribe(procs.nextElement());
      }
    }
  }
}
