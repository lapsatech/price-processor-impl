package com.price.processor.throttler;

import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.price.processor.PriceProcessor;
import com.price.processor.throttler.DurationMetrics.Measure;
import com.price.processor.throttler.DurationMetrics.Stats;

public class QueuedPriceProcesorJob implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(QueuedPriceProcesorJob.class);

  private final RateUpdatesQueue queue;
  private final PriceProcessor priceProcessor;
  private final DurationMetrics processorMetrics = new DurationMetrics();

  private transient boolean finshOnEmptyQueue = false;

  public QueuedPriceProcesorJob(PriceProcessor priceProcessor) {
    this.priceProcessor = priceProcessor;
    this.queue = new AmendingRateUpdatesBlockingQueue();
  }

  public QueuedPriceProcesorJob(PriceProcessor priceProcessor, RateUpdatesQueue queue) {
    this.priceProcessor = priceProcessor;
    this.queue = queue;
  }

  public void queue(String ccyPair, double rate) {
    queue.offer(ccyPair, rate);
  }

  @Override
  public void run() {
    try {
      while (true) {
        final Entry<String, Double> e;
        if (finshOnEmptyQueue) {
          e = queue.peek();
          if (e == null) {
            return; // finish if queue is empty
          }
        } else {
          e = queue.take();
        }
        try (Measure m = processorMetrics.newMeasure()) {
          priceProcessor.onPrice(e.getKey(), e.getValue().doubleValue());
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (RuntimeException re) {
      LOG.error("Excecption occured while running price processor job", re);
    }
  }

  public Stats getStats() {
    return processorMetrics.getStats();
  }

  public void requestStop() {
    finshOnEmptyQueue = true;
  }
}
