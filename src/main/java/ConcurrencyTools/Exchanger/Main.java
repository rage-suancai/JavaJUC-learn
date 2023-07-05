package ConcurrencyTools.Exchanger;

import java.util.concurrent.Exchanger;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        test1();

    }

    static void test1() throws InterruptedException {

        Exchanger<String> exchanger = new Exchanger<>();

        new Thread(() -> {
            try {
                System.out.println("收到主线程传递的交换数据: " + exchanger.exchange("AAAA"));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        System.out.println("收到子线程传递的交换数据: " + exchanger.exchange("BBBB"));

    }

}
