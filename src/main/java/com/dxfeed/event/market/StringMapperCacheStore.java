package com.dxfeed.event.market;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.graalvm.nativeimage.PinnedObject;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;
import org.graalvm.word.WordFactory;

public class StringMapperCacheStore extends Mapper<String, CCharPointer> {

  protected final Map<String, Node> cache = new HashMap<>();
  protected final Map<Long, Node> cache2 = new HashMap<>();
  protected final Queue<PriorityTaskToCleanUpCache> queue = new PriorityBlockingQueue<>();
  protected final int expireAfterLastDeleteInMs;

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  public StringMapperCacheStore(final int expireAfterLastDeleteInMs) {
    this.expireAfterLastDeleteInMs = expireAfterLastDeleteInMs;
  }

  @Override
  public CCharPointer nativeObject(final String jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    readWriteLock.readLock().lock();
    Node node = cache.get(jObject);
    if (node == null) {
      readWriteLock.readLock().unlock();
      readWriteLock.writeLock().lock();
      try {
        node = cache.get(jObject);
        if (node == null) {
          byte[] bytes = jObject.getBytes(StandardCharsets.UTF_8);
          bytes = Arrays.copyOf(bytes, bytes.length + 1);
          node = new Node(jObject, new Pin(bytes));
          cache.put(jObject, node);
          cache2.put(node.value.address(), node);
        }
        readWriteLock.readLock().lock();
      } finally {
        readWriteLock.writeLock().unlock();
      }
    }
    try {
      node.increment(queue);
      return node.value.addressOfElement();
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  @Override
  public void delete(final CCharPointer nObject) {
    if (nObject.isNull()) {
      return;
    }
    readWriteLock.readLock().lock();
    try {
      cache2.get(nObject.rawValue()).decrement(queue);
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  public void cleanUp() {
    final long currentTime = System.currentTimeMillis();
    while (queue.peek() != null && queue.peek().time < currentTime) {
      final Node node = queue.poll().node;
      if (node.counter == 0) {
        readWriteLock.writeLock().lock();
        try {
          if (node.counter == 0) {
            cache.remove(node.key);
            cache2.remove(node.value.address());
          }
        } finally {
          readWriteLock.writeLock().unlock();
        }
        node.value.close();
      }
    }
  }

  @Override
  protected int size() {
    throw new IllegalStateException(
        "The size of the string depends on the content. It is an array."
    );
  }

  @Override
  protected void fillNativeObject(final CCharPointer nObject, final String jObject) {
    // nothing
  }

  @Override
  protected void cleanNativeObject(final CCharPointer nObject) {
    // nothing
  }


  private static final class Pin {

    private final PinnedObject pinnedObject;

    private Pin(final Object object) {
      this.pinnedObject = PinnedObject.create(object);
    }

    public void close() {
      this.pinnedObject.close();
    }

    public long address() {
      return this.pinnedObject.addressOfArrayElement(0).rawValue();
    }

    public <T extends PointerBase> T addressOfElement() {
      return this.pinnedObject.addressOfArrayElement(0);
    }
  }

  private final static class PriorityTaskToCleanUpCache implements
      Comparable<PriorityTaskToCleanUpCache> {

    private final Node node;
    private final long time;

    private PriorityTaskToCleanUpCache(final Node node, final long time) {
      this.node = node;
      this.time = time;
    }

    @Override
    public int compareTo(final PriorityTaskToCleanUpCache o) {
      return Long.compare(this.time, o.time);
    }
  }

  private final class Node {

    private final String key;
    private final Pin value;
    private volatile long counter = 0;

    Node(final String key, final Pin value) {
      this.key = key;
      this.value = value;
    }

    private synchronized void increment(final Queue<PriorityTaskToCleanUpCache> queue) {
      if (++Node.this.counter == 1) {
        queue.removeIf(t -> Node.this.key.equals(t.node.key));
      }
    }

    private synchronized void decrement(final Queue<PriorityTaskToCleanUpCache> queue) {
      if (--Node.this.counter == 0) {
        queue.add(
            new PriorityTaskToCleanUpCache(
                Node.this,
                System.currentTimeMillis() + StringMapperCacheStore.this.expireAfterLastDeleteInMs
            )
        );
      }
    }
  }
}
