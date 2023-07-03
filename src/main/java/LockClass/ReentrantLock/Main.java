package LockClass.ReentrantLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        // test1();
        // test2();
        // test3();
        // test4();
        test5();

    }

    static void test1() throws InterruptedException {

        ReentrantLock lock = new ReentrantLock();
        lock.lock(); lock.lock();

        new Thread(() -> {
            System.out.println("线程二想要获取锁");
            lock.lock();
            System.out.println("线程二成功获取到锁");
        }).start();

        lock.unlock();
        System.out.println("线程一释放了一次锁");
        TimeUnit.SECONDS.sleep(1);
        lock.unlock();
        System.out.println("线程一再次释放了一次锁");

    }

    static void test2() throws InterruptedException {

        ReentrantLock lock = new ReentrantLock();
        lock.lock(); lock.lock();

        System.out.println("当前加锁次数: " + lock.getHoldCount() + " 是否被锁: " + lock.isLocked());
        TimeUnit.SECONDS.sleep(1);
        lock.unlock();
        System.out.println("当前加锁次数: " + lock.getHoldCount() + " 是否被锁: " + lock.isLocked());
        TimeUnit.SECONDS.sleep(1);
        lock.unlock();
        System.out.println("当前加锁次数: " + lock.getHoldCount() + " 是否被锁: " + lock.isLocked());

    }

    static void test3() throws InterruptedException {

        ReentrantLock lock = new ReentrantLock();
        lock.lock();

        Thread t1 = new Thread(lock::lock), t2 = new Thread(lock::lock);
        t1.start(); t2.start();
        TimeUnit.SECONDS.sleep(1);
        System.out.println("当前等待锁释放的线程数: " + lock.getQueueLength());
        System.out.println("线程一等待线程数: " + lock.hasQueuedThread(t1));
        System.out.println("线程二等待线程数: " + lock.hasQueuedThread(t2));
        System.out.println("当前线程是否在等待队列中: " + lock.hasQueuedThread(Thread.currentThread()));

    }

    static void test4() throws InterruptedException {

        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();

        new Thread(() -> {
            lock.lock();
            try {
                condition.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock.unlock();
        }).start();

        TimeUnit.SECONDS.sleep(1);
        lock.lock();
        System.out.println("当前Condition的等待线程数: " + lock.getWaitQueueLength(condition));
        condition.signal();
        System.out.println("当前Condition的等待线程数: " + lock.getWaitQueueLength(condition));
        lock.unlock();

    }

    static void test5() {

        // ReentrantLock lockFalse= new ReentrantLock(false);
        ReentrantLock lockTrue = new ReentrantLock(true);

        Runnable action = () -> {
            System.out.println("线程 " + Thread.currentThread().getName() + " 开始获取锁..."); lockTrue.lock();
            System.out.println("线程 " + Thread.currentThread().getName() + " 成功获取锁"); lockTrue.unlock();
        };

        for (int i = 0; i < 10; i++) {
            new Thread(action, "T" + i).start();
        }

    }

}
