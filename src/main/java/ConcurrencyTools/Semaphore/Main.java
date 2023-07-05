package ConcurrencyTools.Semaphore;

import java.util.concurrent.Semaphore;

public class Main {

    public static void main(String[] args) {

        // test1();
        // test2();
        // test3();
        test4();

    }

    static void test1() {

        Semaphore semaphore = new Semaphore(2);

        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    semaphore.acquire();
                    System.out.println("许可证申请成功");
                    semaphore.release();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

    }

    static void test2() {

        Semaphore semaphore = new Semaphore(3);

        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try {
                    semaphore.acquire(2);
                    System.out.println("许可证申请成功");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

    }

    static void test3() {

        Semaphore semaphore = new Semaphore(3);

        for (int i = 0; i < 5; i++) new Thread(semaphore::acquireUninterruptibly).start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("剩余许可证数量: " + semaphore.availablePermits());
        System.out.println("是否存在线程等待许可证: " + (semaphore.hasQueuedThreads() ? "是" : "否"));
        System.out.println("等待许可证线程数量: " + semaphore.getQueueLength());

    }

    static void test4() {

        Semaphore semaphore = new Semaphore(3);

        new Thread(semaphore::acquireUninterruptibly).start();
        try {
            Thread.sleep(500);
            System.out.println("收回剩余许可数量: " + semaphore.drainPermits());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
