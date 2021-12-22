package com.price.processor.throttler.test.app.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.price.processor.throttler.test.app.services.Consumers;
import com.price.processor.throttler.test.app.services.SamplePriceConsumer;

@Configuration
public class ConsumersConfig {

  @Value("${consumers.takes30min.count:70}")
  private int takes30minCount;

  private AtomicInteger takes30minCounter = new AtomicInteger();

  public SamplePriceConsumer takes30min() {
    return SamplePriceConsumer.buillder()
        .withName("TAKES_30_MIN_#" + takes30minCounter.incrementAndGet())
        .withProcessTime(Duration.ofMinutes(30)) // very slow (onPrice() might take 30 minutes)
        .build();
  }

  @Value("${consumers.takes1min.count:70}")
  private int takes1minCount;

  private AtomicInteger takes1minCounter = new AtomicInteger();

  public SamplePriceConsumer takes1min() {
    return SamplePriceConsumer.buillder()
        .withName("TAKES_1_MIN_#" + takes1minCounter.incrementAndGet())
        .withProcessTime(Duration.ofMinutes(1))
        .build();
  }

  @Value("${consumers.takes1microsec.count:70}")
  private int takes1microsecCount;

  private AtomicInteger takes1microsecCounter = new AtomicInteger();

  public SamplePriceConsumer takes1microsec() {
    return SamplePriceConsumer.buillder()
        .withName("TAKES_1_MICROSEC_#" + takes1microsecCounter.incrementAndGet())
        .withProcessTime(Duration.ofNanos(1000)) // very fast (i.e. onPrice() for them will only take a microsecond)
        .build();
  }

  @Bean
  public Consumers consumers() {
    List<SamplePriceConsumer> c = new ArrayList<>();
    for (int i = 0; i < takes30minCount; i++) {
      c.add(takes30min());
    }
    for (int i = 0; i < takes1minCount; i++) {
      c.add(takes1min());
    }
    for (int i = 0; i < takes1microsecCount; i++) {
      c.add(takes1microsec());
    }
    return new Consumers(c);
  }
}
