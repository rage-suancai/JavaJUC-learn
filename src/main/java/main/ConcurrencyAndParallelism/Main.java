package main.ConcurrencyAndParallelism;

public class Main {

    private static int a = 0;
    private static int b = 0;

    public static void main(String[] args) {

        //test1();
        //test2();
        //test3();
        test4();

    }

    private static int i = 0;
    public static void test1() throws InterruptedException {

        new Thread(() -> {
            for (int j = 0; j < 100000; ++j) ++i;
            System.out.println("线程一结束");
        }).start();
        new Thread(() -> {
            for (int j = 0; i < 100000; ++j) ++i;
            System.out.println("线程二结束");
        }).start();
        Thread.sleep(1000);
        System.out.println(i);

    }

    public static void test2() {

        new Thread(() -> {
            if (b == 1) {
                if (a == 0) System.out.println("A");
                System.out.println("B");
            }
        }).start();
        new Thread(() -> {
            a = 1;
            b = 1;
        }).start();

    }

    public static void test3() throws InterruptedException {

        /*new Thread(() -> {
            while (a == 0);
            System.out.println("线程结束!");
        }).start();
        Thread.sleep(1000);
        System.out.println("正在修改a的值...");
        a = 1;*/

        /*new Thread(() -> {
            while (a == 0) {
                synchronized (Main.class) { }
            }
            System.out.println("线程结束!");
        }).start();
        Thread.sleep(1000);
        System.out.println("正在修改a的值...");
        synchronized (Main.class) {
            a = 1;
        }*/

        Runnable r = () -> {
            for (int i = 0; i < 10000; ++i) ++a;
            System.out.println("任务完成!");
        };
        new Thread(r).start();
        new Thread(r).start();
        Thread.sleep(1000);
        System.out.println(a);

    }

    public static void test4() {

        a = 10;
        b = a+1;
        new Thread(() -> {
            if (b > 10) System.out.println(a);
        }).start();

    }

}
