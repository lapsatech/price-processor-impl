package com.price.processor.throttler;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public final class DurationMetrics {

  private final ReentrantLock lock = new ReentrantLock();

  private long numberOfIterations = 0;
  private Duration totalDuration = Duration.ZERO;
  private Duration averageDuration = null;

  private final Map<Object, DurationMetrics> childs = new ConcurrentHashMap<>();
  private final DurationMetrics parent;

  public DurationMetrics() {
    this.parent = null;
  }

  private DurationMetrics(DurationMetrics parent) {
    this.parent = parent;
  }

  public long add(Duration duration) {
    lock.lock();
    try {
      numberOfIterations++;
      totalDuration = totalDuration.plus(duration);
      averageDuration = totalDuration.dividedBy(numberOfIterations);
      if (parent != null) {
        parent.add(duration);
      }
    } finally {
      lock.unlock();
    }
    return numberOfIterations;
  }

  public interface Measure extends AutoCloseable {
    @Override
    void close();
  }

  public Measure newMeasure() {
    final Instant started = Instant.now();
    return () -> {
      add(Duration.between(started, Instant.now()));
    };
  }

  public Measure newGroupedMeasure(Object group) {
    return childs.computeIfAbsent(group, g -> new DurationMetrics(this))
        .newMeasure();
  }

  public static class Stats {

    private final long numberOfIterations;
    private final Duration totalDuration;
    private final Duration averageDuration;

    private Stats(long numberOfIterations, Duration totalDuration, Duration averageDuration) {
      this.numberOfIterations = numberOfIterations;
      this.totalDuration = totalDuration;
      this.averageDuration = averageDuration;
    }

    public long getNumber() {
      return numberOfIterations;
    }

    public Duration getTotalDuration() {
      return totalDuration;
    }

    public Duration getAverageDuration() {
      return averageDuration;
    }

    @Override
    public int hashCode() {
      return Objects.hash(averageDuration, numberOfIterations, totalDuration);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Stats other = (Stats) obj;
      return Objects.equals(averageDuration, other.averageDuration)
          && numberOfIterations == other.numberOfIterations
          && Objects.equals(totalDuration, other.totalDuration);
    }

    @Override
    public String toString() {
      return "["
          + "numberOfIterations=" + numberOfIterations
          + ", averageDuration=" + DurationUtils.toHumanReadable(averageDuration)
          + ", totalDuration=" + DurationUtils.toHumanReadable(totalDuration)
          + "]";
    }
  }

  public Stats getStats() {
    lock.lock();
    try {
      return new Stats(numberOfIterations, totalDuration, averageDuration);
    } finally {
      lock.unlock();
    }
  }

  public Map<Object, Stats> getGroupsStats() {
    return childs.entrySet()
        .stream()
        .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getStats()));
  }
}
