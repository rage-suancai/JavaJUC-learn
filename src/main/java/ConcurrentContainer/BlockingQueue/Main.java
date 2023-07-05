package ConcurrentContainer.BlockingQueue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {

        // test1();
        // test2();
        // test3();
        test4();

    }

    static void test1() {

        ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);

        Runnable supplier = () -> {
            while(true) {
                try {
                    String name = Thread.currentThread().getName();
                    System.err.println(time() + "生产者 " + name + "正在准备餐品...");
                    TimeUnit.SECONDS.sleep(3);
                    System.err.println(time() + "生产者 " + name + "已出餐");
                    queue.put(new Object());
                } catch (InterruptedException e) {
                    e.printStackTrace(); break;
                }
            }
        };

        Runnable consumer = () -> {
            while (true) {
                try {
                    String name = Thread.currentThread().getName();
                    System.out.println(time() + "消费者 " + name + "正在等待出餐...");
                    queue.take();
                    System.out.println(time() + "消费者 " + name + "取到了餐品");
                    TimeUnit.SECONDS.sleep(4);
                    System.out.println(time() + "消费者 " + name + "已将饭菜吃完了");
                } catch (InterruptedException e) {
                    e.printStackTrace(); break;
                }
            }
        };

        for (int i = 0; i < 2; i++) new Thread(supplier, "supplier-" + i).start();
        for (int i = 0; i < 3; i++) new Thread(consumer, "consumer-" + i).start();

    }


    static void test2() {

        LinkedTransferQueue<Object> queue = new LinkedTransferQueue<>();
        queue.put("1");
        queue.put("2");
        queue.forEach(System.out::println);

    }

    static void test3() {

        PriorityBlockingQueue<Integer> queue =
                new PriorityBlockingQueue<>(10, Integer::compare);

        queue.add(3); queue.add(1); queue.add(2);
        System.out.println(queue);
        System.out.println(queue.poll());
        System.out.println(queue.poll());
        System.out.println(queue.poll());

    }

    static void test4() {

        DelayQueue<MyDelayed> queue = new DelayQueue<>();

        queue.add(new MyDelayed(1, 2, "2号"));
        queue.add(new MyDelayed(3, 1, "1号"));
        try {
            System.out.println(queue.take());
            System.out.println(queue.take());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private static String time() {

        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        return "[" + format.format(new Date()) + "]";

    }

}
