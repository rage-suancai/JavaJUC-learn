package Base.HappensBefore;

public class Main {

    private static int a = 0;
    private static int b = 0;

    public static void main(String[] args) {

        test();

    }

    static void test() {

        a = 10; b = a + 1;
        new Thread(() -> {
            if (b > 10) System.out.println(a);
        }).start();

    }

}
