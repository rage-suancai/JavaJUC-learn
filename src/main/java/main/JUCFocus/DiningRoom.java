package main.JUCFocus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class DiningRoom {

    public static void main(String[] args) {

        implement();

    }

    public static void implement() {

        BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);

        Runnable supplier = () -> {
            while (true) {
                try {
                    String name = Thread.currentThread().getName();
                    System.out.println(time() + "生产者 " + name + "正在准备餐品...");
                    TimeUnit.SECONDS.sleep(3);
                    System.out.println(time() + "生产者 " + name + "已出餐!");
                    queue.put(new Object());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Runnable consumer = () -> {
            while (true) {
                try {
                    String name = Thread.currentThread().getName();
                    System.out.println(time() + "消费者 " + name + "正在等待出现餐...");
                    queue.take();
                    System.out.println(time() + "消费者 " + name + "取到了餐品");
                    TimeUnit.SECONDS.sleep(3);
                    System.out.println(time() + "消费者 " + name + "已经将饭菜吃完了");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        for (int i = 0; i < 2; ++i) new Thread(supplier, "Supplier-" + i).start();
        for (int i = 0; i < 3; ++i) new Thread(consumer, "Supplier-" + i).start();

    }


    private static String time() {

        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        return "[" + format.format(new Date()) + "] ";

    }

}
