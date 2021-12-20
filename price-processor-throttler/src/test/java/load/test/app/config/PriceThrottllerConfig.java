package load.test.app.config;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.price.processor.throttler.PriceThrottler;

import load.test.app.services.NamePrefixThreadFactory;
import load.test.app.services.SamplePriceConsumer;

@Configuration
public class PriceThrottllerConfig {

  @Bean
  public SamplePriceConsumer slowConsumer1() {
    return SamplePriceConsumer.buillder()
        .withName("TAKES_30_MIN_#1")
        .withConsumerDuration(Duration.ofMinutes(30)) // very slow (onPrice() might take 30 minutes)
        .build();
  }

  @Bean
  public SamplePriceConsumer slowConsumer2() {
    return SamplePriceConsumer.buillder()
        .withName("TAKES_30_MIN_#2")
        .withConsumerDuration(Duration.ofMinutes(30)) // very slow (onPrice() might take 30 minutes)
        .build();
  }

  @Bean
  public SamplePriceConsumer slowConsumer3() {
    return SamplePriceConsumer.buillder()
        .withName("TAKES_30_MIN_#3")
        .withConsumerDuration(Duration.ofMinutes(30)) // very slow (onPrice() might take 30 minutes)
        .build();
  }

  @Bean
  public SamplePriceConsumer avgConsumer1() {
    return SamplePriceConsumer.buillder()
        .withName("TAKES_1_MIN_#1")
        .withConsumerDuration(Duration.ofMinutes(1))
        .build();
  }

  @Bean
  public SamplePriceConsumer avgConsumer2() {
    return SamplePriceConsumer.buillder()
        .withName("TAKES_1_MIN_#2")
        .withConsumerDuration(Duration.ofMinutes(1))
        .build();
  }

  @Bean
  public SamplePriceConsumer avgConsumer3() {
    return SamplePriceConsumer.buillder()
        .withName("TAKES_1_MIN_#3")
        .withConsumerDuration(Duration.ofMinutes(1))
        .build();
  }

  @Bean
  public SamplePriceConsumer fastConsumer1() {
    return SamplePriceConsumer.buillder()
        .withName("TAKES_1_MICROSEC_#1")
        .withConsumerDuration(Duration.ofNanos(1000)) // very fast (i.e. onPrice() for them will only take a microsecond)
        .build();
  }

  @Bean
  public SamplePriceConsumer fastConsumer2() {
    return SamplePriceConsumer.buillder()
        .withName("TAKES_1_MICROSEC_#2")
        .withConsumerDuration(Duration.ofNanos(1000)) // very fast (i.e. onPrice() for them will only take a microsecond)
        .build();
  }

  @Bean
  public SamplePriceConsumer fastConsumer3() {
    return SamplePriceConsumer.buillder()
        .withName("TAKES_1_MICROSEC_#3")
        .withConsumerDuration(Duration.ofNanos(1000)) // very fast (i.e. onPrice() for them will only take a microsecond)
        .build();
  }

  @Bean
  public ExecutorService consumersPool() {
    return Executors.newCachedThreadPool(new NamePrefixThreadFactory("price-throttler-thread"));
  }

  @Bean
  public PriceThrottler priceThrottler(@Autowired @Qualifier("consumersPool") ExecutorService consumersPool,
      @Autowired BeanFactory factory) {
    PriceThrottler priceThrottller = new PriceThrottler(consumersPool);
    factory.getBeanProvider(SamplePriceConsumer.class).forEach(priceThrottller::subscribe);
    return priceThrottller;
  }

}
