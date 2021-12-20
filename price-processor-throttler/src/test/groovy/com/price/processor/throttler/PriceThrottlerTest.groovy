package com.price.processor.throttler

import java.util.concurrent.Executors

import com.price.processor.PriceProcessor

import spock.lang.Specification

class PriceThrottlerTest extends Specification {

  def 'test subscribe/unsubscribe'() {
    given:
    def pool = Executors.newFixedThreadPool(3)
    def pt = new PriceThrottler(pool)

    def s1 = Mock(PriceProcessor)
    def s2 = Mock(PriceProcessor)
    def s3 = Mock(PriceProcessor)


    when:
    pt.subscribe(s1)
    pt.subscribe(s2)
    pt.subscribe(s3)

    then:
    pt.subscribers() == 3

    when:
    pt.unsubscribe(s1)

    then:
    pt.subscribers() == 2

    then:
    pt.unsubscribe(s2)
    pt.unsubscribe(s3)

    then:
    pt.subscribers() == 0

    cleanup:
    pool.shutdownNow()
  }

  def 'test subscribers getting updates'() {
    given:
    def pool = Executors.newFixedThreadPool(3)
    def pt = new PriceThrottler(pool)

    def s1 = Mock(PriceProcessor)
    def s2 = Mock(PriceProcessor)
    def s3 = Mock(PriceProcessor)

    pt.subscribe(s1)
    pt.subscribe(s2)
    pt.subscribe(s3)

    when:
    pt.onPrice('ccy1', 10d)
    pt.onPrice('ccy2', 20d)
    pt.onPrice('ccy3', 30d)
    Thread.sleep(100)

    then:
    1 * s1.onPrice('ccy1', 10d)
    1 * s1.onPrice('ccy2', 20d)
    1 * s1.onPrice('ccy3', 30d)

    1 * s2.onPrice('ccy1', 10d)
    1 * s2.onPrice('ccy2', 20d)
    1 * s2.onPrice('ccy3', 30d)

    1 * s3.onPrice('ccy1', 10d)
    1 * s3.onPrice('ccy2', 20d)
    1 * s3.onPrice('ccy3', 30d)

    cleanup:
    pool.shutdownNow()
  }
}
