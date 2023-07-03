package LockClass.ReadWriteLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {

    public static void main(String[] args) {

        // test4();
        // test5();
        // test6();
        // test7();
        test8();

    }

    static void test1() {

        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        lock.readLock().lock();

        new Thread(lock.readLock()::lock).start();

    }
    static void test2() {

        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        lock.readLock().lock();

        new Thread(lock.writeLock()::lock).start();

    }

    static void test3() {

        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();

        new Thread(lock.readLock()::lock).start();

    }

    static void test4() {

        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock(); lock.writeLock().lock();

        new Thread(() -> {
            lock.writeLock().lock();
            System.out.println("成功获取到写锁");
        }).start();
        System.out.println("释放第一层锁");
        lock.writeLock().unlock();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("释放第二层锁");
        lock.writeLock().unlock();

    }

    static void test5() {

        ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

        Runnable action = () -> {
            System.out.println("线程 " + Thread.currentThread().getName() + " 将在1秒后开始获取锁...");
            lock.writeLock().lock();
            System.out.println("线程 " + Thread.currentThread().getName() + " 获取到了锁");
            lock.writeLock().unlock();
        };
        for (int i = 0; i < 10; i++) {
            new Thread(action, "T" + i).start();
        }

    }

    static void test6() {

        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock(); lock.readLock().lock();

        System.out.println("成功加读锁");

    }

    static void test7() {

        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock(); lock.readLock().lock();

        new Thread(() -> {
            System.out.println("开始加读锁");
            lock.readLock().lock();
            System.out.println("读锁添加成功");
        }).start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        lock.writeLock().unlock();

    }
    static void test8() {

        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        lock.readLock().lock(); lock.writeLock().lock();

        System.out.println("锁升级成功");

    }

}
