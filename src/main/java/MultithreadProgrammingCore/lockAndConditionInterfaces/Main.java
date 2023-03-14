package MultithreadProgrammingCore.lockAndConditionInterfaces;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Lock和Condition接口
 * 使用并发包中的锁和我们传统的synchronized锁不太一样 这里的锁我们可以认为是一把真正意义上的锁
 * 每个锁都是一个对应的锁对象 我们只需要向锁对象获取锁或是释放锁即可 我们首先来看看 此接口中定义了什么:
 *
 *                  public interface Lock {
 *
 *                      // 获取锁 拿不到锁会阻塞 等待其他线程释放锁 获取到锁后返回
 *                      void lock();
 *                      // 同上 但是等待过程中会响应中断
 *                      void lockInterruptibly() throws InterruptedException;
 *                      // 尝试获取锁 但是不会阻塞 如果能获取到会返回true 不能返回false
 *                      boolean tryLock();
 *                      // 尝试获取锁 但是可以限定超时时间 如果超出时间还没拿到锁返回false 否则返回true 可以响应中断
 *                      boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
 *                      // 释放锁
 *                      void unlock();
 *                      // 暂时可以理解为替代传统的Object的wait() notify()等操作的工具
 *                      Condition newCondition();
 *
 *                  }
 *
 * 这里我们可以演示一下 如何使用Lock类来进行加锁和释放锁操作:
 *
 *                  private static int i = 0;
 *                  public static void main(String[] args) {
 *
 *                      Lock testLock = new ReentrantLock(); // 可重入锁ReentrantLock类是Lock类的一个实现 我们后面会进行介绍
 *                      Runnable action = () -> {
 *                          for (int j = 0; j < 100000; j++) { // 还是以自增操作为例
 *                              testLock.lock(); // 加锁 加锁成功后其他线程如果也要获取锁 会阻塞 等待当前线程释放
 *                              i++;
 *                              testLock.unlock(); // 解锁 释放锁之后其他线程就可以获取这把锁了(注意在这之前一定得加锁 不然报错)
 *                          }
 *                      };
 *                      new Thread(action).start();
 *                      new Thread(action).start();
 *                      Thread.sleep(1000); // 等待上面两个线程跑完
 *                      System.out.println(i);
 *
 *                  }
 *
 * 可以看到 和我们之前使用synchronized相比 我们这里是真正在操作一个"锁"对象 当我们需要加锁时 只需要调用lock()方法
 * 当需要释放锁时 只需要调用unlock()方法 程序运行的最终结果和使用synchronized锁是一样的
 *
 * 那么 我们如何像传统的加锁那样 调用对象的wait()和notify()方法呢 并发包提供了Condition接口:
 *
 *                  public interface Condition {
 *
 *                      // 与调用锁对象的wait方法一样 会进入到等待状态 但是这里需要调用Condition的signal或signalAll方法进行唤醒(感觉就是和普通对象的wait和notify是对应的) 同时 等待状态下是可以响应中断的
 *                      void await() throws InterruptedException;
 *                      // 同上 但不响应中断(看名字就能猜到)
 *                      void awaitUninterruptibly();
 *                      // 等待指定时间 如果在指定时间(纳秒)内被唤醒 会返回剩余时间 如果超时 会返回0或负数 可以响应中断
 *                      long awaitNanos(long nanosTimeout) throwsInterruptedException;
 *                      // 等待指定时间(可以指定时间单位) 如果等待时间内被唤醒 返回true 否则返回false 可以响应中断
 *                      boolean await(long time, TimeUnit unit) throws InterruptedException;
 *                      // 可以指定一个明确的时点 如果在时间点之前被唤醒 返回true 否则返回false 可以响应中断
 *                      boolean awaitUntil(Date deadline) throws InterruptedException;
 *                      // 唤醒一个处于等待状态的线程 注意还得获取锁才能接着运行
 *                      void signal();
 *                      // 同上 但是是唤醒所有等待线程
 *                      void signalAll();
 *
 *                  }
 *
 * 这里我们通过一个简单的例子来演示一下:
 *
 *                  static void test2() {
 *
 *                      Lock testLock = new ReentrantLock();
 *                      Condition condition = testLock.newCondition();
 *                      new Thread(() -> {
 *                          testLock.lock(); // 和synchronized一样 必须持有锁的情况下才能使用await
 *                          System.out.println("线程一进入等待状态");
 *                          try {
 *                              condition.await(); // 进入等待状态
 *                          } catch (InterruptedException e) {
 *                              e.printStackTrace();
 *                          }
 *                          System.out.println("线程一等待结束");
 *                          testLock.unlock();
 *                      }).start();
 *                      try {
 *                          Thread.sleep(100); // 防止线程二先跑
 *                          new Thread(() -> {
 *                              testLock.lock();
 *                              System.out.println("线程二开始唤醒其他等待线程");
 *                              condition.signal(); // 唤醒线程一 但是此时线程一还必须要拿到锁才能继续运行
 *                              System.out.println("线程二结束");
 *                              testLock.unlock(); // 这里释放锁之后 线程二就可以拿到锁继续运行了
 *                          }).start();
 *                      } catch (InterruptedException e) {
 *                          e.printStackTrace();
 *                      }
 *
 *                  }
 *
 * 可以发现 Condition对象使用方法和传统的对象使用差别不是很大
 *
 * 思考: 下面这种情况跟上面有什么不同?
 *
 *                  static void test3() {
 *
 *                      Lock testLock = new ReentrantLock();
 *                      new Thread(() -> {
 *                          testLock.lock();
 *                          System.out.println("线程一进入等待状态");
 *                          try {
 *                              testLock.newCondition().await();
 *                          } catch (InterruptedException e) {
 *                              e.printStackTrace();
 *                          }
 *                          System.out.println("线程一等待结束");
 *                          testLock.unlock();
 *                      }).start();
 *                      try {
 *                          Thread.sleep(100);
 *                          new Thread(() -> {
 *                              testLock.lock();
 *                              System.out.println("线程二开始唤醒其他等待线程");
 *                              testLock.newCondition().signal();
 *                              System.out.println("线程二结束");
 *                              testLock.unlock();
 *                          }).start();
 *                      } catch (InterruptedException e) {
 *                          e.printStackTrace();
 *                      }
 *
 *                  }
 *
 * 通过分析可以得到 在调用newCondition()后 会生成一个新的Condition对象 并且同一把锁内是可以存在多个Condition对象的
 * (实际上原始的锁机制等待队列只能有一个 而这里可以创建多个Condition来实现多等待队列)
 * 而上面的例子中 实际上使用的是不同的Condition对象 只有对同一个Condition对象进行等待和唤醒操作才会有效 而不同的Condition对象是分开计算的
 *
 * 最后我们再来讲解一下时间单位 这是一个枚举类 也是位于java.util.concurrent包下:
 *
 *                  public enum TimeUnit {
 *
 *                      // Time unit representing one thousandth of a microsecond
 *                      NANOSECONDS {
 *                          public long toNanos(long d) { return d; }
 *                          public long toMicros(long d) { return d/(C1/C0); }
 *                          public long toMillis(long d) { return d/C2//C0; }
 *                          public long toSeconds(long d) { return d/C3//C0; }
 *                          public long toMinutes(long d) { return d/C4//C0; }
 *                          public long toHours(long d) { return d/C5//C0; }
 *                          public long toDays(long d) { return d/C6//C0; }
 *                          public long convert(long d, TimeUnit u) {return u.toNanos(d); }
 *                          int excessNanos(long d, long m) { return (int)(d - (m*C2)); }
 *                      },
 *                      // ....
 *
 *                  }
 *
 * 可以看到时间单位有很多的 比如DAY, SECONDS, MINUTES等 我们可以直接将其作为时间单位 比如我们要让一个线程等待3秒钟 可以像下面这样编写:
 *
 *                  Lock testLock = new ReentrantLock();
 *                  new Thread(() -> {
 *                      testLock.Lock;
 *                      System.out.println("线程是否未超时: " + testLock.newCondition().await(1, TimeUnit.SECONDS));
 *                  }).start;
 *
 * 当然 Lock类的tryLock方法也是支持使用时间单位的 各位可以自行进行测试 TimeUnit除了可以作为时单位表示以为 还可以再不同单位之间相互转换:
 *
 *                  public  static void main(String[] args) throws InterruptedException {
 *
 *                      System.out.println("60秒 = " + TimeUnit.SECONDS.);
 *                      System.out.println("365天 = " + TimeUnit.DAYS.toSeconds(365) + " 秒");
 *
 *                  }
 *
 * 也可以更加便捷地使用对象的wait()方法:
 *
 *                  public static void main(String[] args) throws InterruptedException {
 *
 *                      synchronized (Main.class) {
 *                          System.out.println("开始等待");
 *                          TimeUnit.SECONDS.timedWait(Main.class, 3); // 直接等待3秒
 *                          System.out.println("等待结束");
 *                      }
 *
 *                  }
 *
 * 我们也可以直接使用它来进行休眠操作:
 *
 *                  public static void main(String[] args) throws InterruptedException {
 *
 *                      TimeUnit.SECONDS.sleep(1); // 休眠1秒钟
 *
 *                  }
 */
public class Main {

    private static int i = 0;

    static void test1() {

        Lock testLock = new ReentrantLock();
        Runnable action = () -> {
            for (int j = 0; j < 100000; j++) {
                testLock.lock();
                i++;
                testLock.unlock();
            }
        };
        try {
            new Thread(action).start();
            new Thread(action).start();
            Thread.sleep(1000);
            System.out.println(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    static void test2() {

        Lock testLock = new ReentrantLock();
        Condition condition = testLock.newCondition();
        new Thread(() -> {
            testLock.lock();
            System.out.println("线程一进入等待状态");
            try {
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("线程一等待结束");
            testLock.unlock();
        }).start();
        try {
            Thread.sleep(100);
            new Thread(() -> {
                testLock.lock();
                System.out.println("线程二开始唤醒其他等待线程");
                condition.signal();
                System.out.println("线程二结束");
                testLock.unlock();
            }).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    static void test3() {

        Lock testLock = new ReentrantLock();
        new Thread(() -> {
            testLock.lock();
            System.out.println("线程一进入等待状态");
            try {
                testLock.newCondition().await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("线程一等待结束");
            testLock.unlock();
        }).start();
        try {
            Thread.sleep(100);
            new Thread(() -> {
                testLock.lock();
                System.out.println("线程二开始唤醒其他等待线程");
                testLock.newCondition().signal();
                System.out.println("线程二结束");
                testLock.unlock();
            }).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    static void test4() {

        Lock testLock = new ReentrantLock();
        new Thread(() -> {
            try {
                testLock.lock();
                System.out.println("等待线程是否未超时: " + testLock.newCondition().await(3, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            testLock.unlock();
        }).start();

    }

    static void test5() {

        System.out.println("60秒 = " + TimeUnit.SECONDS.toMinutes(60) + "分钟");
        System.out.println("365天 = " + TimeUnit.DAYS.toSeconds(365) + " 秒");

    }

    static void test6() {

        try {
            System.out.println("开睡");
            TimeUnit.SECONDS.sleep(1);
            System.out.println("起床");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {

        //test1();
        //test2();
        test3();
        //test4();
        //test5();
        //test6();

    }

}
