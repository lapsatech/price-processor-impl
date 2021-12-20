package load.test.app.services;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.price.processor.PriceProcessor;

public class Generators implements AutoCloseable {

  private final ExecutorService generatorsPool;
  private final List<SamplePriceGenerator> priceGenerators;

  private List<Future<?>> generatorsTasks;

  public Generators(ExecutorService generatorsPool, List<SamplePriceGenerator> priceGenerators) {
    this.generatorsPool = generatorsPool;
    this.priceGenerators = priceGenerators;
  }

  public void start(PriceProcessor priceProcessor) {
    this.generatorsTasks = priceGenerators.stream()
        .map(g -> generatorsPool.submit(() -> g.run(priceProcessor)))
        .collect(Collectors.toList());
  }

  public void stop() {
    priceGenerators.forEach(SamplePriceGenerator::interrupt);
    generatorsTasks.forEach(future -> {
      if (!future.isDone()) {
        future.cancel(true);
      }
    });
  }

  @Override
  public void close() {
    stop();
  }

}