package ConcurrentContainer.ContainerIntroduction;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {

        // test1();
        test2();

    }

    static void test1() {

        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        Runnable r = () -> {
            for (int i = 0; i < 100; i++) list.add("yxsnb");
        };
        for (int i = 0; i < 100; i++) new Thread(r).start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(list.size());

    }

    static void test2() {

        ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>();
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            new Thread(() -> {
                for (int j = 0; j < 100; j++) map.put(finalI * 100 + j, "yxsnb");
            }).start();
        }
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(map.size());

    }

}
