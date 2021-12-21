package com.price.processor.test.app.services;

import static com.price.processor.throttler.DurationUtils.threadSleep;
import static com.price.processor.throttler.DurationUtils.toHumanReadable;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

public class App implements DisposableBean {

  protected static final Logger LOG = LoggerFactory.getLogger(App.class);

  private LocalDateTime started = null;

  private final Generators generators;
  private final Duration duration;

  public App(Generators generators, Duration duration) {
    this.generators = generators;
    this.duration = duration;
  }

  public void run() throws InterruptedException {
    started = LocalDateTime.now();
    LOG.info("Started at {}", started);
    LOG.info("Will run for {}. Hit Ctrl-Break for immediate termination", toHumanReadable(duration));

    generators.start();
    threadSleep(duration);
    generators.stop();
  }

  public void logStats() {
    LocalDateTime finished = LocalDateTime.now();
    LOG.info("Finished at {}", finished);
    LOG.info("Overall runtime is {}", toHumanReadable(Duration.between(started, finished)));
  }

  @Override
  public void destroy() throws Exception {
    logStats();
  }
}
