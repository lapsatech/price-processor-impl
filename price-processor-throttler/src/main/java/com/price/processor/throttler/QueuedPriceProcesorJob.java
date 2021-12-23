package com.price.processor.throttler;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.price.processor.PriceProcessor;
import com.price.processor.throttler.DurationMetrics.Measure;
import com.price.processor.throttler.RateUpdatesBlockingQueue.RateUpdate;

public class QueuedPriceProcesorJob implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(QueuedPriceProcesorJob.class);

  private final RateUpdatesBlockingQueue queue;
  private final PriceProcessor priceProcessor;

  private final DurationMetrics processorOnPricePerfomance;

  public QueuedPriceProcesorJob(PriceProcessor priceProcessor, DurationMetrics processorMetrics) {
    this(priceProcessor, new AmendingRateUpdatesBlockingQueue(), processorMetrics);
  }

  public QueuedPriceProcesorJob(PriceProcessor priceProcessor) {
    this(priceProcessor, new AmendingRateUpdatesBlockingQueue(), null);
  }

  public QueuedPriceProcesorJob(
      PriceProcessor priceProcessor,
      RateUpdatesBlockingQueue queue,
      DurationMetrics processorOnPricePerfomance) {
    this.priceProcessor = requireNonNull(priceProcessor, "priceProcessor");
    this.queue = requireNonNull(queue, "queue");
    this.processorOnPricePerfomance = processorOnPricePerfomance;
  }

  public void queue(RateUpdate update) {
    queue.offer(update);
  }

  public static class OnPriceIncompleteException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }

  @Override
  public void run() {
    try {
      while (!Thread.currentThread().isInterrupted()) {
        final RateUpdate e = queue.take();
        try {
          if (processorOnPricePerfomance == null) {
            priceProcessor.onPrice(e.getCcyPair(), e.getRate());
          } else {
            final Measure m = processorOnPricePerfomance.newMeasure();
            priceProcessor.onPrice(e.getCcyPair(), e.getRate());
            m.complete();
          }
        } catch (OnPriceIncompleteException incomplete) {
          // We want to collect stats for completed invocations only
          LOG.debug("OnPrice incompleted", e);
        } catch (RuntimeException re) {
          LOG.error("Excecption occured while running price processor job", re);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
