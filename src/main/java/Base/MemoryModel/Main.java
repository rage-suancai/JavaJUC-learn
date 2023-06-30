package Base.MemoryModel;

public class Main {

    private static int i = 0;

    public static void main(String[] args) {

        test();

    }

    static void test() {

        new Thread(() -> {
            for (int j = 0; j < 100000; j++) i++;
            System.out.println("线程1结束");
        }).start();

        new Thread(() -> {
            for (int j = 0; j < 100000; j++) i++;
            System.out.println("线程2结束");
        }).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(i);

    }

}
