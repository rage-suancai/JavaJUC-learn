package LockClass.LockAndCondition;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

    private static int i = 0;

    public static void main(String[] args) {

        // test1();
        // test2();
        // test3();
        // test4();
        test5();

    }

    static void test1() {

        Lock testLock = new ReentrantLock();

        Runnable action = () -> {
            for (int j = 0; j < 100000; j++) {
                testLock.lock(); i++;
                testLock.unlock();
            }
        };
        new Thread(action).start();
        new Thread(action).start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(i);

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
                throw new RuntimeException(e);
            }
            System.out.println("线程一等待结束");
            testLock.unlock();
        }).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            testLock.lock();
            System.out.println("线程二开始唤醒其他等待线程");
            condition.signal();
            System.out.println("线程二结束");
            testLock.unlock();
        }).start();

    }

    static void test3() {

        Lock testLock = new ReentrantLock();

        new Thread(() -> {
            testLock.lock();
            System.out.println("线程一进入等待状态");
            try {
                testLock.newCondition().await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("线程一等待结束");
            testLock.unlock();
        }).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            testLock.lock();
            System.out.println("线程二开始唤醒其他等待线程");
            testLock.newCondition().signal();
            System.out.println("线程二结束");
            testLock.unlock();
        }).start();

    }

    static void test4() {

        Lock testLock = new ReentrantLock();

        new Thread(() -> {
            testLock.lock();
            try {
                System.out.println("等待是否未超时: " + testLock.newCondition().await(1, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            testLock.unlock();
        }).start();

    }

    static void test5() {

        /*System.out.println("60秒 = " + TimeUnit.SECONDS.toMinutes(60) + "分钟");
        System.out.println("365天 = " + TimeUnit.DAYS.toSeconds(365) + " 秒");*/

        synchronized (Main.class) {
            System.out.println("开始等待");
            try {
                TimeUnit.SECONDS.timedWait(Main.class, 3);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("等待结束");
        }

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
