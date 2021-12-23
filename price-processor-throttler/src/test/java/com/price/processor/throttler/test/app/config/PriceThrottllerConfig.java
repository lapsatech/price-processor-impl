package com.price.processor.throttler.test.app.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.price.processor.throttler.PriceThrottler;
import com.price.processor.throttler.test.app.services.Consumers;
import com.price.processor.throttler.test.app.services.NamePrefixThreadFactory;

@Configuration
@Import(ConsumersConfig.class)
public class PriceThrottllerConfig {

  @Bean
  public ExecutorService consumersPool() {
    return Executors.newCachedThreadPool(new NamePrefixThreadFactory("price-throttler-thread"));
  }

  @Autowired
  private Consumers consumers;

  @Value("${price-throttler.collect-stats:true}")
  private boolean collectStats;

  @Bean
  public PriceThrottler priceThrottler() {
    PriceThrottler pt = new PriceThrottler(consumersPool(), collectStats);
    consumers.stream().forEach(pt::subscribe);
    return pt;
  }

}
