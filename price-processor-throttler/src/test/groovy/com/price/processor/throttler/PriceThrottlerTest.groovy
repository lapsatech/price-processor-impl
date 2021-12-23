package com.price.processor.throttler

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

import com.price.processor.PriceProcessor

import spock.lang.Specification

class PriceThrottlerTest extends Specification {

  def 'test subscribe/unsubscribe'() {
    given:
    def pt = new PriceThrottler(Stub(ExecutorService) {
      submit(_) >> Stub(Future)
    })

    def s1 = Stub(PriceProcessor)
    def s2 = Stub(PriceProcessor)
    def s3 = Stub(PriceProcessor)
    def s4 = Stub(PriceProcessor)
    
    when:
    pt.subscribe(s1)
    pt.subscribe(s2)
    pt.subscribe(s3)
    pt.subscribe(s4)
    
    then:
    pt.subscribersCount == 4

    when:
    pt.unsubscribe(s1)

    then:
    pt.subscribersCount == 3

    then:
    pt.unsubscribe(s2)

    then:
    pt.subscribersCount == 2
    
    when:
    pt.unsubscribeAll()
    
    then:
    pt.subscribersCount == 0
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

  def 'test closed state'() {

    given:
    def pt = new PriceThrottler(Stub(ExecutorService) {
      submit(_) >> Stub(Future)
    })

    when:
    pt.subscribe(Stub(PriceProcessor))
    pt.subscribe(Stub(PriceProcessor))
    pt.subscribe(Stub(PriceProcessor))

    then:
    pt.subscribersCount == 3

    when:
    pt.onPrice('ccy', 123d)

    then:
    noExceptionThrown()

    when:
    pt.close()

    then:
    pt.subscribersCount == 0

    when:
    pt.onPrice('ccy', 123d)

    then:
    def e1 = thrown(IllegalStateException)
    e1.message == 'Resource is closed'

    when:
    pt.subscribe(Stub(PriceProcessor))

    then:
    def e2 = thrown(IllegalStateException)
    e2.message == 'Resource is closed'

    when:
    pt.close()

    then:
    def e3 = thrown(IllegalStateException)
    e3.message == 'Resource is closed already'
  }
}
