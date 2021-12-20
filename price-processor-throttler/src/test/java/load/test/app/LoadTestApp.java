package load.test.app;

import static com.price.processor.throttler.DurationUtils.threadSleep;
import static com.price.processor.throttler.DurationUtils.toHumanReadable;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.price.processor.throttler.PriceThrottler;

import load.test.app.config.LoadTestAppConfig;
import load.test.app.services.Generators;
import load.test.app.services.SamplePriceConsumer;
import load.test.app.services.SamplePriceGenerator;

public class LoadTestApp {

  protected static final Logger LOG = LoggerFactory.getLogger(LoadTestApp.class);

  private AnnotationConfigApplicationContext appContext;

  private LocalDateTime started = null;

  public void run() throws InterruptedException {
    final Duration d = Duration.parse(System.getProperty("duration", "PT20S")); // Run 20 seconds by default

    appContext = new AnnotationConfigApplicationContext(LoadTestAppConfig.class);

    try (PriceThrottler pt = appContext.getBean(PriceThrottler.class);
        Generators generators = appContext.getBean(Generators.class)) {
      started = LocalDateTime.now();
      LOG.info("Started at {}", started);
      LOG.info("Will run for {}. Hit Ctrl-Break for immediate termination", toHumanReadable(d));

      generators.start(pt);
      threadSleep(d);
      generators.stop();

    } finally {
      shutdown();
    }
  }

  public void shutdown() {
    if (appContext != null) {
      LocalDateTime finished = LocalDateTime.now();
      appContext.getBeanProvider(SamplePriceGenerator.class).forEach(SamplePriceGenerator::logStats);
      appContext.getBeanProvider(SamplePriceConsumer.class).forEach(SamplePriceConsumer::logStats);
      appContext.close();
      LOG.info("Finished at {}", finished);
      LOG.info("Overall runtime is {}", toHumanReadable(Duration.between(started, finished)));
    }
  }

}
