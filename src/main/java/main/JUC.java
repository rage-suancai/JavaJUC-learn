package main;

public class JUC {

    public static void main(String[] args) {

        test();

    }

    public static void test() {

        /*int[] arr = new int[]{ 3, 1, 5, 2, 4 };
        Arrays.sort(arr);
        for (int i : arr) System.out.println(i);*/

        int[] arr = new int[]{ 3, 1, 5, 2, 4 };
        for (int i : arr) {
            new Thread(() -> {
                try {
                    Thread.sleep(i*1000);
                    System.out.println(i);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

    }

}
