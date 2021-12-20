package com.price.processor.throttler

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import spock.lang.Specification

class AmendingRateUpdateBlockingQueueTest extends Specification {


  def 'test take amending rate and order'() {
    given:
    def q = new AmendingRateUpdatesBlockingQueue()

    q.offer('eur_usd', 1000)

    q.offer('gpb_usd', 2000)
    q.offer('gpb_usd', 2100)
    q.offer('gpb_usd', 2101)
    q.offer('gpb_usd', 2102)
    q.offer('gpb_usd', 2001)

    q.offer('rub_usd', 3000) // third ccyPair in the queue

    q.offer('gpb_usd', 2101) // second ccyPair in the queue

    q.offer('eur_usd', 1001) // first ccyPair in the queue

    when:
    def upd1 = q.take()
    def upd2 = q.take()
    def upd3 = q.take()

    then:
    upd1.key == 'eur_usd'
    upd1.value == 1001

    upd2.key == 'gpb_usd'
    upd2.value == 2101

    upd3.key == 'rub_usd'
    upd3.value == 3000
  }

  def 'test take timout'() {
    given:
    def q = new AmendingRateUpdatesBlockingQueue()

    q.offer('eur_usd', 1000)
    q.offer('gpb_usd', 2000)

    when:
    def upd1 = q.take()
    def upd2 = q.take()
    q.take(10, TimeUnit.MICROSECONDS)

    then:
    upd1
    upd2
    thrown(TimeoutException)
  }
}
