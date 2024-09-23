package com.price.processor.throttler

import static com.price.processor.throttler.RateUpdatesBlockingQueue.RateUpdate.of

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import spock.lang.Specification

class AmendingRateUpdatesBlockingQueueTest extends Specification {


  def 'test take amending rate and order'() {
    given:
    def q = new RateUpdatesBlockingQueue()

    q.offer(of('eur_usd', 1000))

    q.offer(of('gpb_usd', 2000))
    q.offer(of('gpb_usd', 2100))
    q.offer(of('gpb_usd', 2101))
    q.offer(of('gpb_usd', 2102))
    q.offer(of('gpb_usd', 2001))

    q.offer(of('rub_usd', 3000)) // third ccyPair in the queue

    q.offer(of('gpb_usd', 2101)) // second ccyPair in the queue

    q.offer(of('eur_usd', 1001)) // first ccyPair in the queue

    when:
    def upd1 = q.take()
    def upd2 = q.take()
    def upd3 = q.take()

    then:
    upd1.ccyPair == 'eur_usd'
    upd1.rate == 1001

    upd2.ccyPair == 'gpb_usd'
    upd2.rate == 2101

    upd3.ccyPair == 'rub_usd'
    upd3.rate == 3000
  }

  def 'test take timout'() {
    given:
    def q = new RateUpdatesBlockingQueue()

    q.offer(of('eur_usd', 1000))
    q.offer(of('gpb_usd', 2000))

    when:
    def upd1 = q.take()
    def upd2 = q.take()
    q.take(10, TimeUnit.MILLISECONDS)

    then:
    upd1
    upd2
    thrown(TimeoutException)
  }
}
