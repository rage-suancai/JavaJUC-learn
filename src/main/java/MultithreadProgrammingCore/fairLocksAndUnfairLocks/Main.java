package MultithreadProgrammingCore.fairLocksAndUnfairLocks;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 公平锁与非公平锁
 * 前面我们了解了如果线程之间争抢一把锁 会暂时进入到等待队列中 那么多个线程获得锁的顺序
 * 是不是一定是根据线程调用lock()方法时间来定的呢 我们可以看到 ReentrantLock的构造方法中 是这样写的:
 *
 *                  public ReentrantLock() {
 *                      sync = new NonfairSync(); // 看名字貌似是非公平的
 *                  }
 *
 * 其实锁分为公平锁和非公平锁 默认我们创建出来的ReentrantLock是采用的非公平锁作为底层锁机制 那么什么是公平锁什么又是非公平锁呢?
 *
 *      > 公平锁: 多个线程按照申请锁的顺序取获得锁 线程会直接进入队列去排队 永远都是队列的第一位才能得到锁
 *      > 非公平锁: 多个线程去获取锁的时候 会直接尝试获取 获取不到 再去进入等待队列 如果能获取到 就直接获取到锁
 *
 * 简单来说 公平锁不让插队 都老老实实排着 非公平锁让插队 但是排队的人让不让你插队就是另一回事了
 *
 * 我们可以来测试一下公平锁和非公平锁的表现情况:
 *
 *                  public ReentrantLock(boolean fair) {
 *                      sync = fait ? new FaitSync() : new NonfairSync();
 *                  }
 *
 * 这里我们选择使用第二个构造方法 可以选择是否为公平锁实现:
 *
 *                  ReentrantLock lock = new ReentrantLock(false);
 *                  Runnable action = () -> {
 *                      System.out.println("线程 " + Thread.currentThread().getName() + " 开始获取锁...");
 *                      lock.lock();
 *                      System.out.println("线程 " + Thread.currentThread().getName() + " 成功获取到锁");
 *                      lock.unlock();
 *                  };
 *                  for (int i = 0; i < 10; i++) { // 建立10个线程
 *                      new Thread(action, "T" + i).start();
 *                  }
 *
 * 这里我们只需要对比"开始获取锁..."和"成功获取锁"的顺序是否一致即可 如果是一致 那说明所有的线程都是按顺序排队获取的锁 如果不是 那说明肯定是有线程插队了
 *
 * 运行结果可发现 在公平模式下 确实是按照顺序进行的 而在非公平模式下 一般会出现这种情况: 线程刚开始获取锁马上就能抢到 并且此时之前早就开始的线程还在等待状态 很明显的插队行为
 *
 * 那么 接着下一个问题 公平锁在任何情况下都是一定公平的吗? 有关这个问题 我们会留到队列同步器中再进行讨论
 */
public class Main {

    static void test1() {

        ReentrantLock lock = new ReentrantLock(false);
        Runnable action = () -> {
            System.out.println("线程 " + Thread.currentThread().getName() + " 开始获取锁...");
            lock.lock();
            System.out.println("线程 " + Thread.currentThread().getName() + " 成功获取到锁");
            lock.unlock();
        };
        for (int i = 0; i < 10; i++) {
            new Thread(action, "T" + i).start();
        }

    }

    static void test2() {

        ReentrantLock lock = new ReentrantLock(true);
        Runnable action = () -> {
            System.out.println("线程 " + Thread.currentThread().getName() + " 开始获取锁...");
            lock.lock();
            System.out.println("线程 " + Thread.currentThread().getName() + " 成功获取到锁");
            lock.unlock();
        };
        for (int i = 0; i < 10; i++) {
            new Thread(action, "T" + i).start();
        }

    }

    public static void main(String[] args) {

        //test1();
        test2();

    }

}
