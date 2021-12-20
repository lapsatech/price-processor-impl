package load.test.app.services;

import static com.price.processor.throttler.DurationUtils.threadSleep;
import static java.util.Objects.requireNonNull;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.price.processor.PriceProcessor;
import com.price.processor.throttler.DurationMetrics;
import com.price.processor.throttler.DurationMetrics.Measure;

public class SamplePriceConsumer implements PriceProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(SamplePriceConsumer.class);

  public static class Builder {

    private Duration consumerDuration;
    private String name = "UNNAMED";

    private Builder() {
    }

    public Builder withConsumerDuration(Duration workTime) {
      this.consumerDuration = workTime;
      return this;
    }

    public Builder withConsumerDurationMilis(long workTimeMilis) {
      this.consumerDuration = Duration.ofMillis(workTimeMilis);
      return this;
    }

    public Builder withConsumerDurationNanos(long workTimeNanos) {
      this.consumerDuration = Duration.ofNanos(workTimeNanos);
      return this;
    }

    public Builder withConsumerDuration(long workTimeMilis, long workTimeNanos) {
      this.consumerDuration = Duration.ofMillis(workTimeMilis).plusNanos(workTimeNanos);
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public SamplePriceConsumer build() {
      return new SamplePriceConsumer(consumerDuration, name);
    }
  }

  public static Builder buillder() {
    return new Builder();
  }

  private final Duration processTime;

  private final String name;

  private SamplePriceConsumer(Duration workTime, String name) {
    this.processTime = requireNonNull(workTime, "workTime");
    this.name = requireNonNull(name, "name");
  }

  private final DurationMetrics metrics = new DurationMetrics();

  @Override
  public void onPrice(String ccyPair, double rate) {
    try (Measure measure = metrics.newGroupedMeasure(ccyPair)) {
      threadSleep(processTime);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void logStats() {
    LOGGER.info("===> '{}' overall stats are {}", name, metrics.getStats());
    metrics.getGroupsStats()
        .forEach((ccyPair, stats) -> LOGGER.info("===> '{}' >>> '{}' stats are {}", name, ccyPair, stats));
  }

  @Override
  public void subscribe(PriceProcessor priceProcessor) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void unsubscribe(PriceProcessor priceProcessor) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public String toString() {
    return "SamplePriceConsumer [name=" + name + "]";
  }
}