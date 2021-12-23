package com.price.processor.throttler;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private RegistryEntry(QueuedPriceProcesorJob proc, Future<?> future) {
      this.proc = proc;
      this.future = future;
    }
  }

  private final AtomicBoolean stateClosed = new AtomicBoolean(false);
  private final ConcurrentHashMap<PriceProcessor, RegistryEntry> processorsRegistry = new ConcurrentHashMap<>();
  private final ExecutorService threadPool;
  private final DurationMetrics onPricePerfomance;

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
    checkState();
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
    checkState();
    if (priceProcessor == this) {
      throw new IllegalArgumentException("Infinity loop. Can't subscribe to itself");
    }
    processorsRegistry.computeIfAbsent(priceProcessor, pp -> {
      QueuedPriceProcesorJob proc = new QueuedPriceProcesorJob(pp, onPricePerfomance != null);
      Future<?> future = threadPool.submit(proc);
      return new RegistryEntry(proc, future);
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

  public void unsubscribeAll() {
    processorsRegistry.forEachKey(Long.MAX_VALUE, this::unsubscribe);
  }

  public int getSubscribersCount() {
    return processorsRegistry.size();
  }

  public Stats getOnPricePerfomanceStats() {
    return onPricePerfomance == null
        ? null
        : onPricePerfomance.getStats();
  }

  private void checkState() {
    if (stateClosed.get()) {
      throw new IllegalStateException("Resource is closed");
    }
  }

  @Override
  public void close() {
    if (!stateClosed.compareAndSet(false, true)) {
      throw new IllegalStateException("Resource is closed already");
    }
    if (onPricePerfomance != null) {
      LOG.info("PriceThrottler this.onPrice() perfomance stats are {}", onPricePerfomance.getStats());
    }
    unsubscribeAll();
  }
}
