package ConcurrencyTools.CounterLock;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class Main {

    public static void main(String[] args) {

        test();

    }

    static void test() {

        CountDownLatch latch = new CountDownLatch(20);

        for (int i = 0; i < 20; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    Thread.sleep((long) (2000 * new Random().nextDouble()));
                    System.out.println("子任务" + finalI + "执行完成");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                latch.countDown();
            }).start();
        }
        try {
            latch.await();
            System.out.println("所有子任务都完成 任务完成");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
