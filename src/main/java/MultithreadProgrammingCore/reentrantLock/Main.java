package MultithreadProgrammingCore.reentrantLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 可重入锁
 * 前面 我们讲解了锁框架的两个核心接口 那么我们接着来看看锁接口的具体实现类 我们前面用到了ReentrantLock
 * 它其实是锁的一种 叫做可重入锁 那么这个可重入代表的是什么意思呢? 简单来说 就是同一个线程 可反复进行加锁操作:
 *
 *                  static void test1() {
 *
 *                      ReentrantLock lock = new ReentrantLock();
 *                      lock.lock();
 *                      lock.lock(); // 连续加锁2次
 *                      new Thread(() -> {
 *                          System.out.println("线程二想要获取锁");
 *                          lock.lock();
 *                          System.out.println("线程二成功获取到锁");
 *                      }).start();
 *                      lock.unlock();
 *                      System.out.println("线程一释放了一次锁");
 *                      try {
 *                          TimeUnit.SECONDS.sleep(1);
 *                          lock.unlock();
 *                          System.out.println("线程一再次释放了一次锁"); // 释放两次后其他线程才能加锁
 *                      } catch (InterruptedException e) {
 *                          e.printStackTrace();
 *                      }
 *
 *                  }
 *
 * 可以看到 主线程连续进行了两次加锁操作(此操作是不会被阻塞的) 在当前线程持有锁的情况下继续加锁不会被阻塞
 * 并且 加锁几次 就必须要解锁几次 否则此线程依旧持有锁 我们可以使用getHoldCount()方法查看当前线程的加锁次数:
 *
 *                  ReentrantLock lock = new ReentrantLock();
 *                  lock.lock();
 *                  lock.lock();
 *                  System.out.println("当前加锁次数: " p+ lock.getHoldCount() + " 是否被锁: " + lock.isLocked());
 *                  try {
 *                      TimeUnit.SECONDS.sleep(1);
 *                      lock.unlock();
 *                      System.out.println("当前加锁次数: " + lock.getHoldCount() + " 是否被锁: " + lock.isLocked());
 *                      TimeUnit.SECONDS.sleep(1);
 *                      lock.unlock();
 *                      System.out.println("当前加锁次数: " + lock.getHoldCount() + " 是否被锁: " + lock.isLocked());
 *                  } catch (InterruptedException e) {
 *                      e.printStackTrace();
 *                  }
 *
 * 可以看到 当锁不再被任何线程持有时 值为0 并且通过isLocked()方法查询结果为false
 *
 * 实际上 如果存在线程持有当前的锁 那么其他线程在获取锁时 是会暂时进入到等待队列的 我们可以通过getQueueLength()方法获取等待中线程数量的预估值:
 *
 *                   ReentrantLock lock = new ReentrantLock();
 *                   lock.lock();
 *                   Thread t1 = new Thread(lock::lock), t2 = new Thread(lock::lock);
 *                   t1.start();
 *                   t2.start();
 *                   try {
 *                       TimeUnit.SECONDS.sleep(1);
 *                       System.out.println("当前等待锁释放的线程数: " + lock.getQueueLength());
 *                       System.out.println("线程一是否在等待队列中: " + lock.hasQueuedThread(t1));
 *                       System.out.println("线程二是否在等待队列中: " + lock.hasQueuedThread(t2));
 *                       System.out.println("当前线程是否在等待队列中: " + lock.hasQueuedThread(Thread.currentThread()));
 *                   } catch (InterruptedException e) {
 *                       e.printStackTrace();
 *                   }
 *
 * 我们可以通过hasQueuedThread()方法来判断某个线程是否正在等待获取锁状态
 *
 * 同样的 Condition也可以进行判断:
 *
 *                  ReentrantLock lock = new ReentrantLock();
 *                  Condition condition = lock.newCondition();
 *                  new Thread(() -> {
 *                      lock.lock();
 *                      try {
 *                          condition.await();
 *                      } catch (InterruptedException e) {
 *                          e.printStackTrace();
 *                      }
 *                      lock.unlock();
 *                  }).start();
 *                  try {
 *                      TimeUnit.SECONDS.sleep(1);
 *                      lock.lock();
 *                      System.out.println("当前Condition的等待线程数: " + lock.getWaitQueueLength(condition));
 *                      condition.signal();
 *                      System.out.println("当前Condition的等待线程数: " + lock.getWaitQueueLength(condition));
 *                      lock.unlock();
 *                  } catch (InterruptedException e) {
 *                      e.printStackTrace();
 *                  }
 *
 * 通过使用getWaitQueueLength()方法能够查看同一个Condition目前有多少线程处于等待状态
 */
public class Main {

    static void test1() {

        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        lock.lock();
        new Thread(() -> {
            System.out.println("线程二想要获取锁");
            lock.lock();
            System.out.println("线程二成功获取到锁");
        }).start();
        lock.unlock();
        System.out.println("线程一释放了一次锁");
        try {
            TimeUnit.SECONDS.sleep(1);
            lock.unlock();
            System.out.println("线程一再次释放了一次锁");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    static void test2() {

        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        lock.lock();
        System.out.println("当前加锁次数: " + lock.getHoldCount() + " 是否被锁: " + lock.isLocked());
        try {
            TimeUnit.SECONDS.sleep(1);
            lock.unlock();
            System.out.println("当前加锁次数: " + lock.getHoldCount() + " 是否被锁: " + lock.isLocked());
            TimeUnit.SECONDS.sleep(1);
            lock.unlock();
            System.out.println("当前加锁次数: " + lock.getHoldCount() + " 是否被锁: " + lock.isLocked());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    static void test3() {

        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        Thread t1 = new Thread(lock::lock), t2 = new Thread(lock::lock);
        t1.start();
        t2.start();
        try {
            TimeUnit.SECONDS.sleep(1);
            System.out.println("当前等待锁释放的线程数: " + lock.getQueueLength());
            System.out.println("线程一是否在等待队列中: " + lock.hasQueuedThread(t1));
            System.out.println("线程二是否在等待队列中: " + lock.hasQueuedThread(t2));
            System.out.println("当前线程是否在等待队列中: " + lock.hasQueuedThread(Thread.currentThread()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    static void test4() {

        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        new Thread(() -> {
            lock.lock();
            try {
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock.unlock();
        }).start();
        try {
            TimeUnit.SECONDS.sleep(1);
            lock.lock();
            System.out.println("当前Condition的等待线程数: " + lock.getWaitQueueLength(condition));
            condition.signal();
            System.out.println("当前Condition的等待线程数: " + lock.getWaitQueueLength(condition));
            lock.unlock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {

        //test1();
        //test2();
        //test3();
        test4();

    }

}
