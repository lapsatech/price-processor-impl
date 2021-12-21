package com.price.processor.test.app;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.price.processor.test.app.config.AppConfig;
import com.price.processor.test.app.services.App;

public class MainRunner {

  private static void registerShutdownHook(Runnable appShutdown) {

    final LoggerContextFactory factory = LogManager.getFactory();
    if (factory instanceof Log4jContextFactory) {
      Log4jContextFactory contextFactory = (Log4jContextFactory) factory;
      ((DefaultShutdownCallbackRegistry) contextFactory.getShutdownCallbackRegistry()).stop();
    }

    final Runnable shutdown = () -> {
      try {
        appShutdown.run();
      } finally {
        LogManager.shutdown();
      }
    };

    Runtime.getRuntime().addShutdownHook(new Thread(shutdown, "shutdown-thread"));
  }

  public static void main(String[] args) throws InterruptedException {
    try (AnnotationConfigApplicationContext appContext = new AnnotationConfigApplicationContext(AppConfig.class)) {
      App app = appContext.getBean(App.class);
      registerShutdownHook(appContext::close);
      app.run();
    }
  }

}
