package ConcurrencyTools.CyclicBarrier;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Main {

    public static void main(String[] args) {

        // test1();
        // test2();
        // test3();
        test4();

    }

    static void test1() {

        CyclicBarrier barrier = new CyclicBarrier(10,
                () -> System.out.println("飞机马上就要起飞了 各位特种兵请准备"));

        for (int i = 0; i < 10; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    Thread.sleep((long) (2000 * new Random().nextDouble()));
                    System.out.println("玩家 " + finalI + " 进入房间进行等待... - " + barrier.getNumberWaiting() + "/10");
                    barrier.await();
                    System.out.println("玩家 " + finalI + " 进入游戏");
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

    }

    static void test2() {

        CyclicBarrier barrier = new CyclicBarrier(5);

        for (int i = 0; i < 10; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    Thread.sleep((long) (2000 * new Random().nextDouble()));
                    System.out.println("玩家 " + finalI + " 进入房间进行等待... - " + barrier.getNumberWaiting() + "/5");
                    barrier.await();
                    System.out.println("玩家 " + finalI + " 进入游戏");
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

    }

    static void test3() {

        CyclicBarrier barrier = new CyclicBarrier(5);

        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

        try {
            Thread.sleep(500);
            System.out.println("当前屏障前的等待线程数: " + barrier.getNumberWaiting());
            barrier.reset();
            System.out.println("重置后屏障前的等待线程数: " + barrier.getNumberWaiting());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    static void test4() {

        CyclicBarrier barrier = new CyclicBarrier(10);

        Runnable r = () -> {
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        };
        Thread t = new Thread(r);
        t.start(); t.interrupt();
        new Thread(r).start();

    }

}
