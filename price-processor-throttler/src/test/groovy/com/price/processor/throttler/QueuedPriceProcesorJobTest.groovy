package com.price.processor.throttler

import java.util.AbstractMap.SimpleEntry
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException

import com.price.processor.PriceProcessor

import spock.lang.Specification

class QueuedPriceProcesorJobTest extends Specification {

  def 'test'() {

    given:
    def pp = Mock(PriceProcessor)
    def q = Mock(RateUpdatesQueue)

    q.take() >>> [
      new SimpleEntry('ccy1', 10d),
      new SimpleEntry('ccy2', 20d),
      new SimpleEntry('ccy3', 30d)
    ] >> { Thread.sleep(10000); throw new RuntimeException("Please make sure you interrupted the job's thread") } // no more updates in a query


    def thread = Executors.newSingleThreadExecutor()
    def job = new QueuedPriceProcesorJob(pp, q)

    when:
    def f = thread.submit(job)
    Thread.sleep(100)
    f.cancel(true)

    then:
    1 * pp.onPrice('ccy1', 10d)
    1 * pp.onPrice('ccy2', 20d)
    1 * pp.onPrice('ccy3', 30d)

    cleanup:
    thread.shutdownNow()
  }
}
