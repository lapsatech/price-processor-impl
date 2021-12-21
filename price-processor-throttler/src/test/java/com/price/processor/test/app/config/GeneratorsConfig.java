package com.price.processor.test.app.config;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.price.processor.test.app.services.Generators;
import com.price.processor.test.app.services.NamePrefixThreadFactory;
import com.price.processor.test.app.services.SamplePriceGenerator;
import com.price.processor.throttler.PriceThrottler;

@Configuration
@Import(PriceThrottllerConfig.class)
public class GeneratorsConfig {

  @Bean
  public SamplePriceGenerator continuouslyPairGenerator() {
    return SamplePriceGenerator.builder()
        .withCcyPair("CONTINUOUS_GEN_PAIR")
        .withFrequencyMillis(1 * 1_000 / 100) // 100 times per second
        .withConsumer(priceThrottler)
        .build();
  }

  @Bean
  public SamplePriceGenerator onesPerSecPairGenerator() {
    return SamplePriceGenerator.builder()
        .withCcyPair("ONES_PER_SEC_PAIR")
        .withFrequency(Duration.ofSeconds(1))
        .withConsumer(priceThrottler)
        .build();
  }

  @Bean
  public SamplePriceGenerator onesPerMinPairGenerator() {
    return SamplePriceGenerator.builder()
        .withCcyPair("ONES_PER_MIN_PAIR")
        .withFrequency(Duration.ofMinutes(1)) // every minute
        .withConsumer(priceThrottler)
        .build();
  }

  @Bean
  public SamplePriceGenerator onesPerDayPairGenerator() {
    return SamplePriceGenerator.builder()
        .withCcyPair("ONES_PER_DAY_PAIR")
        .withInitialDelay(Duration.ofMinutes(20))
        .withFrequency(Duration.ofDays(1)) // once per day
        .withConsumer(priceThrottler)
        .build();
  }

  @Bean
  public ExecutorService generatorsPool() {
    return Executors.newCachedThreadPool(new NamePrefixThreadFactory("generators-thread"));
  }

  @Autowired
  private BeanFactory factory;

  @Autowired
  private PriceThrottler priceThrottler;

  @Bean
  public Generators generators(
      @Autowired @Qualifier("generatorsPool") ExecutorService generatorsPool) {
    return new Generators(generatorsPool, factory.getBeanProvider(SamplePriceGenerator.class)
        .stream()
        .collect(Collectors.toList()));
  }
}
