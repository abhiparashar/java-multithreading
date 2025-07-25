package basic;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockCache<K,V> {
    private final Map<K,V> cache = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // READING: Multiple threads can do this simultaneously
    public V get(K key){
        lock.readLock().lock();
        try{
          return cache.get(key);
        }finally {
            lock.readLock().unlock();
        }
    };

    // WRITING: Only one thread can do this, blocks all readers and writers
    public void put(K key, V value){
        lock.writeLock().lock();
        try{
            cache.put(key, value);
        }finally {
            lock.writeLock().unlock();
        }
    }

    // WRITING: Clearing also modifies, so needs write lock
    public void clear(){
        lock.writeLock().lock();
        try{
            cache.clear();
        }finally {
            lock.writeLock().unlock();
        }
    }

    // READING: Just checking size is safe with other readers
    public int getSize(){
        lock.readLock().lock();
        try {
           return cache.size();
        }finally {
            lock.readLock().unlock();
        }
    }
}
