package com.price.processor.throttler;

import static com.price.processor.throttler.DurationUtils.toHumanReadable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public final class DurationMetrics {

  private final ReadWriteLock startedLock = new ReentrantReadWriteLock();
  private final ReadWriteLock completedLock = new ReentrantReadWriteLock();

  private volatile long startedCount = 0;
  private volatile long completedCount = 0;
  private volatile Duration completedDurationTotal = Duration.ZERO;
  private volatile Duration completedDurationAverage = null;

  private final Map<Object, DurationMetrics> childs = new ConcurrentHashMap<>();
  private final DurationMetrics parent;

  public DurationMetrics() {
    this.parent = null;
  }

  private DurationMetrics(DurationMetrics parent) {
    this.parent = parent;
  }

  private long addStarted() {
    if (parent != null) {
      parent.addStarted();
    }

    startedLock.writeLock().lock();
    try {
      return ++startedCount;
    } finally {
      startedLock.writeLock().unlock();
    }
  }

  private long addCompleted(Duration duration) {
    if (parent != null) {
      parent.addCompleted(duration);
    }

    completedLock.writeLock().lock();
    try {
      completedDurationTotal = completedDurationTotal.plus(duration);
      completedDurationAverage = completedDurationTotal.dividedBy(++completedCount);
      return completedCount;
    } finally {
      completedLock.writeLock().unlock();
    }
  }

  public interface Measure extends AutoCloseable {

    @Override
    default void close() {
      complete();
    }

    void complete();
  }

  public Measure newMeasure() {
    addStarted();
    final Instant started = Instant.now();
    return () -> {
      addCompleted(Duration.between(started, Instant.now()));
    };
  }

  public DurationMetrics groupMetrics(Object groupBy) {
    return childs.computeIfAbsent(groupBy, g -> new DurationMetrics(this));
  }

  public static class Stats {

    private final long startedCount;
    private final long completedCount;
    private final Duration completedDurationTotal;
    private final Duration completedDurationAverage;

    private Stats(long startedCount, long completedCount, Duration completedDurationTotal,
        Duration complletedDurationAverage) {
      this.startedCount = startedCount;
      this.completedCount = completedCount;
      this.completedDurationTotal = completedDurationTotal;
      this.completedDurationAverage = complletedDurationAverage;
    }

    @Override
    public String toString() {
      return "["
          + "startedCount=" + startedCount
          + ", completedCount=" + completedCount
          + ", completedDurationTotal=" + toHumanReadable(completedDurationTotal)
          + ", completedDurationAverage=" + toHumanReadable(completedDurationAverage)
          + "]";
    }

  }

  public Stats getStats() {
    startedLock.readLock().lock();
    try {
      completedLock.readLock().lock();
      try {
        return new Stats(startedCount, completedCount, completedDurationTotal, completedDurationAverage);
      } finally {
        completedLock.readLock().unlock();
      }
    } finally {
      startedLock.readLock().unlock();
    }
  }

  public Map<Object, Stats> getGroupsStats() {
    return childs.entrySet()
        .stream()
        .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getStats()));
  }
}
