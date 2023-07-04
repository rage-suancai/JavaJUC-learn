package AtomicClass.Question;

import java.util.concurrent.atomic.AtomicStampedReference;

public class Main {

    public static void main(String[] args) {

        test1();

    }

    static void test1() {

        String a = "Hello"; String b = "World";
        AtomicStampedReference<String> reference = new AtomicStampedReference<>(a, 1);
        reference.attemptStamp(a, 2);
        System.out.println(reference.compareAndSet(a, b, 2, 3));

    }

}
