package load.test.app;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.apache.logging.log4j.spi.LoggerContextFactory;

public class MainRunner {

  static void registerShutdownHook(Runnable runnable) {

    final LoggerContextFactory factory = LogManager.getFactory();
    if (factory instanceof Log4jContextFactory) {
      Log4jContextFactory contextFactory = (Log4jContextFactory) factory;
      ((DefaultShutdownCallbackRegistry) contextFactory.getShutdownCallbackRegistry()).stop();
    }

    final Runnable shutdown = () -> {
      try {
        runnable.run();
      } finally {
        LogManager.shutdown();
      }
    };

    Runtime.getRuntime().addShutdownHook(new Thread(shutdown, "shutdown-thread"));

  }

  public static void main(String[] args) throws InterruptedException {
    LoadTestApp app = new LoadTestApp();
    registerShutdownHook(app::shutdown);
    app.run();
  }

}
