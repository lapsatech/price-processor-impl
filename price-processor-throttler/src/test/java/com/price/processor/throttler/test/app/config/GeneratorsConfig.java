package com.price.processor.throttler.test.app.config;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.price.processor.throttler.PriceThrottler;
import com.price.processor.throttler.test.app.services.DefaultScheduledPriceGenerator;
import com.price.processor.throttler.test.app.services.Generators;
import com.price.processor.throttler.test.app.services.NamePrefixThreadFactory;
import com.price.processor.throttler.test.app.services.ScheduledPriceGenerator;

@Configuration
@Import(PriceThrottllerConfig.class)
public class GeneratorsConfig {

  @Autowired
  private PriceThrottler priceThrottler;

  @Bean
  public ScheduledPriceGenerator continuouslyPairGenerator() {
    return DefaultScheduledPriceGenerator.builder()
        .withCcyPair("CONTINUOUS_GEN_PAIR")
        .withFrequencyMillis(1 * 1_000 / 100) // 100 times per second
        .withConsumer(priceThrottler)
        .build();
  }

  @Bean
  public ScheduledPriceGenerator onesPerSecPairGenerator() {
    return DefaultScheduledPriceGenerator.builder()
        .withCcyPair("ONES_PER_SEC_PAIR")
        .withFrequency(Duration.ofSeconds(1))
        .withConsumer(priceThrottler)
        .build();
  }

  @Bean
  public ScheduledPriceGenerator onesPerMinPairGenerator() {
    return DefaultScheduledPriceGenerator.builder()
        .withCcyPair("ONES_PER_MIN_PAIR")
        .withFrequency(Duration.ofMinutes(1)) // every minute
        .withConsumer(priceThrottler)
        .build();
  }

  @Bean
  public ScheduledPriceGenerator onesPerDayPairGenerator() {
    return DefaultScheduledPriceGenerator.builder()
        .withCcyPair("ONES_PER_DAY_PAIR")
        .withInitialDelay(Duration.ofMinutes(20))
        .withFrequency(Duration.ofDays(1)) // once per day
        .withConsumer(priceThrottler)
        .build();
  }

  @Bean
  public ScheduledExecutorService generatorsPool() {
    return Executors.newScheduledThreadPool(4, new NamePrefixThreadFactory("generators-thread"));
  }

  @Autowired
  private BeanFactory factory;

  @Bean
  public Generators generators() {
    List<ScheduledPriceGenerator> generators = factory.getBeanProvider(ScheduledPriceGenerator.class).stream()
        .collect(Collectors.toList());
    return new Generators(generatorsPool(), generators);
  }
}
