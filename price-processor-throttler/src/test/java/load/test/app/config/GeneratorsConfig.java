package load.test.app.config;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import load.test.app.services.Generators;
import load.test.app.services.NamePrefixThreadFactory;
import load.test.app.services.SamplePriceGenerator;

@Configuration
public class GeneratorsConfig {

  @Bean
  public SamplePriceGenerator continuouslyPairGenerator() {
    return SamplePriceGenerator.builder()
        .withCcyPair("CONTINUOUS_GEN_PAIR")
        .withFrequencyMillis(1 * 1_000 / 100) // 100 times per second
        .build();
  }

  @Bean
  public SamplePriceGenerator onesPerSecPairGenerator() {
    return SamplePriceGenerator.builder()
        .withCcyPair("ONES_PER_SEC_PAIR")
        .withFrequency(Duration.ofSeconds(1))
        .build();
  }

  @Bean
  public SamplePriceGenerator onesPerMinPairGenerator() {
    return SamplePriceGenerator.builder()
        .withCcyPair("ONES_PER_MIN_PAIR")
        .withFrequency(Duration.ofMinutes(1)) // every minute
        .build();
  }

  @Bean
  public SamplePriceGenerator onesPerDayPairGenerator() {
    return SamplePriceGenerator.builder()
        .withCcyPair("ONES_PER_DAY_PAIR")
        .withInitialDelay(Duration.ofMinutes(20))
        .withFrequency(Duration.ofDays(1)) // once per day
        .build();
  }

  @Bean
  public ExecutorService generatorsPool() {
    return Executors.newCachedThreadPool(new NamePrefixThreadFactory("generators-thread"));
  }

  @Autowired
  private BeanFactory factory;

  @Bean
  public Generators generators(
      @Autowired @Qualifier("generatorsPool") ExecutorService generatorsPool) {

    List<SamplePriceGenerator> samplePriceGenerators = factory.getBeanProvider(SamplePriceGenerator.class)
        .stream()
        .collect(Collectors.toList());

    return new Generators(generatorsPool, samplePriceGenerators);
  }
}
