package LockClass.AQS;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class Main {

    public static void main(String[] args) {

        /*ReentrantLock lock = new ReentrantLock();
        lock.lock();*/

        test1();

    }

    static void test1() {

        Thread t = Thread.currentThread();
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
                System.out.println("主线程可以继续运行了");
                LockSupport.unpark(t);
                // t.interrupt();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        System.out.println("主线程被挂起");
        LockSupport.park();
        System.out.println("主线程继续运行");

    }

}
