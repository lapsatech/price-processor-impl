package com.price.processor.throttler;

import static java.util.Objects.requireNonNull;

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
  private final DurationMetrics processorPerfomance;

  private transient boolean finshOnEmptyQueue = false;

  public QueuedPriceProcesorJob(PriceProcessor priceProcessor, boolean collectStats) {
    this(priceProcessor, new AmendingRateUpdatesBlockingQueue(), collectStats);
  }

  public QueuedPriceProcesorJob(PriceProcessor priceProcessor) {
    this(priceProcessor, new AmendingRateUpdatesBlockingQueue(), false);
  }

  public QueuedPriceProcesorJob(PriceProcessor priceProcessor, RateUpdatesQueue queue, boolean collectStats) {
    this.priceProcessor = requireNonNull(priceProcessor, "priceProcessor");
    this.queue = requireNonNull(queue, "queue");
    this.processorPerfomance = collectStats
        ? new DurationMetrics()
        : null;
  }

  public void queue(String ccyPair, double rate) {
    queue.offer(ccyPair, rate);
  }

  @Override
  public void run() {
    try {
      while (!Thread.currentThread().isInterrupted()) {
        final Entry<String, Double> e;
        if (finshOnEmptyQueue) {
          if ((e = queue.peek()) == null) {
            return; // finish if queue is empty
          }
        } else {
          e = queue.take();
        }

        final Measure m = processorPerfomance == null
            ? null
            : processorPerfomance.newMeasure();

        try {
          try {
            priceProcessor.onPrice(e.getKey(), e.getValue().doubleValue());
          } finally {
            if (m != null) {
              m.complete();
            }
          }
        } catch (RuntimeException re) {
          LOG.error("Excecption occured while running price processor job", re);
        }

      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public Stats getProcessorPerfomanceStats() {
    return processorPerfomance == null
        ? null
        : processorPerfomance.getStats();
  }

  public void stopGracceful() {
    finshOnEmptyQueue = true;
  }
}
