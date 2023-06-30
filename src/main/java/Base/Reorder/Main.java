package Base.Reorder;

public class Main {

    private static int a = 0;
    private static int b = 0;
    private static volatile int c = 0;

    public static void main(String[] args) {

        // test1();
        // test2();
        // test3();
        // test4();
        test5();

    }

    static void test1() {

        int a = 10; int b = 20;
        System.out.println(a + b);

    }
    static void test2() {

        int b = 10; int a = 20;
        System.out.println(a + b);

    }

    static void test3() {

        new Thread(() -> {
            if (b == 1) {
                if (a == 0) System.out.println("A");
                else System.out.println("B");
            }
        }).start();

        new Thread(() -> {
            a = 1; b = 1;
        }).start();

    }

    static void test4() {

        new Thread(() -> {
            while (c == 0);
            System.out.println("线程结束");
        }).start();

        /*new Thread(() -> {
            while (a == 0) synchronized (Main.class) {}
            System.out.println("线程结束");
        }).start();*/

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("正在修改c的值...");
        c = 1;

    }

    static void test5() {

        Runnable r = () -> {
          for (int i = 0; i < 10000; i++) c++;
          System.out.println("任务完成");
        };
        new Thread(r).start();
        new Thread(r).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(c);

    }

}
