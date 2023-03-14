package MultithreadProgrammingCore.atomicClass;

import java.sql.SQLOutput;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

/**
 * 原子类介绍
 * 常用基本数据类 有对应的原子类封装:
 *
 *      > AtomicInteger: 原子更新int
 *      > AtomicLong: 原子更新long
 *      > AtomicBoolean: 原子更新boolean
 *
 * 那么 原子类和普通的基本类在使用上有没有什么区别呢? 我们先来看正常情况下使用一个基本类型:
 *
 *                  public class Main {
 *
 *                      public static void main(String[] args) {
 *                          int i = 1;
 *                          System.out.println(i++);
 *                      }
 *
 *                  }
 *
 * 现在我们使用int类型对应的原子类 要实现同样的代码该如何编写:
 *
 *                  AtomicInteger i = new AtomicInteger(1);
 *                  System.out.println(i.getAndIncrement()); // 如果想实现i += 2这种操作 可以使用addAndGet()自由设置
 *
 * 我们可以将int数值封装到此类中(注意必须调用构造方法 它不像Integer那样有装箱机制) 并且通过调用此类提供的方法来获取或是对封装的int值进行自增
 * 乍一看这不就是基本类包装类嘛 有啥高级的 确实 还真包装类那味 但是它可不仅仅是简单的包装 它的自增操作是具有原子性的:
 *
 *                  private static AtomicInteger i = new AtomicInteger(0);
 *
 *                  public static void main(String[] args) throws InterruptedException {
 *
 *                      Runnable r = () -> {
 *                          for (int j = 0; j < 100000; j++)
 *                              System.out.println("自增完成");
 *                      };
 *                      new Thread(r).start();
 *                      new Thread(r).start();
 *                      TimeUnit.SECONDS.sleep(1);
 *                      System.out.println(i.get());
 *
 *                  }
 *
 * 同样是直接进行自增操作 我们发现 使用原子类是可以保证自增操作原子性的 就跟我们前面加锁一样 怎么会这么神奇? 我们来看看它的底层是如何实现的 直接从构造方法点进去:
 *
 *                  private volatile int value;
 *
 *                  public AtomicInteger(int initialValue) {
 *                      value = initialValue;
 *                  }
 *
 *                  public AtomicInteger() {
 *                  }
 *
 * 可以看到 它的底层是比较简单的 其实本质上就是封装了有关volatile类型的int值 这样能够保证可见性 在CAS操作的时候不会出现问题:
 *
 *                  private static final Unsafe unsafe = Unsafe.getUnsafe();
 *                  private static final long valueOffset;
 *
 *                  static {
 *                      try {
 *                          valueOffset = unsafe.abjectFiledOffset
 *                              (AtomicInteger.class.getDeclaredFiled("value"));
 *                      }catch (Exception ex) { throw new Error(ex); }
 *                  }
 *
 * 可以看到最上面是和AQS采用了类似的机制 因为要使用CAS算法更新value的值 所以得先计算出value字段在对象中的偏移地址
 * CAS直接修改对应位置的内存即可(可见Unsafe类的作用巨大 很多的底层操作都要靠它来完成)
 *
 * 接着我们来看自增操作是怎么在运行的:
 *
 *                  public final int getAndIncrement() {
 *                      return unsafe.getAndAddInt(this, valueOffset, 1);
 *                  }
 *
 * 可以看到这里调用了unsafe.getAndAddInt() 套娃时间到 我们接着看看Unsafe里面写了什么:
 *
 *                  public final int getAndAddInt(Object o, long offset, int delta) { // delta就是变化的值 ++操作就是自增1
 *                      int v;
 *                      do {
 *                          // volatile版本的getInt()
 *                          // 能够保证可见性
 *                          v = getIntVolatile(o, offset);
 *                      // 这里是开始cas替换int的值 每次都去拿最新的值去进行替换 如果成功则离开循环 不成功说明这个时候其他线程先修改了值 就进下一次循环再获取最新的值然后再cas一次 直到成功为止
 *                      } while (!weakCompareAndSetInt(o, offset, v, v + delta));
 *                      return v;
 *                  }
 *
 * 可以看到这是一个do-while循环 那么这个循环在做一个说明事情呢? 感觉就和我们之前讲解的AQS队列中的机制差不多 也是采用自旋形式 来不断进行CAS操作 直到成功
 *
 *      https://img-blog.csdnimg.cn/img_convert/61721ba5fbef5d0b1110c7d609993cae.png
 *
 * 可见 原子类底层也是采用了CAS算法来保证的原子性 包括getAndSet getAndAdd等方法都是这样 原子类也直接提供了CAS操作方法 我们可以直接使用:
 *
 *                  public static void main(String[] args) throws InterruptedException {
 *
 *                      AtomicInteger integer = new AtomicInteger(10);
 *                      System.out.println(integer.compareAndSet(30, 20));
 *                      System.out.println(integer.compareAndSet(10, 20));
 *                      System.out.println(integer);
 *
 *                  }
 *
 * 如果想以普通变量的方式来设定值 那么可以使用lazySet()方法 这样就不采用volatile的立即可见机制了
 *
 *                  AtomicInteger integer = new AtomicInteger(1);
 *                  integer.lazySet(2);
 *
 * 除了基本类的原子类以外 基本类型的数组类型也有原子类:
 *
 *      > AtomicIntegerArray: 原子更新int数组
 *      > AtomicLongArray: 原子更新long数组
 *      > AtomicReferenceArray: 原子更新引用数组
 *
 * 其实原子数组和原子类型一样的 不过我们可以对数组内的元素进行原子操作:
 *
 *                  public static void main(String[] args) throws InterruptedException {
 *
 *                      AtomicIntegerArray array = new AtomicIntegerArray(new int[]{0, 4, 1, 3, 5});
 *                      Runnable r = () -> {
 *                          for (int i = 0; i < 100000; i++) array.getAndAdd(0, 1);
 *                      };
 *                      new Thread(r).start();
 *                      new Thread(r).start();
 *                      try {
 *                          TimeUnit.SECONDS.sleep(1);
 *                      } catch (InterruptedException e) {
 *                          throw new RuntimeException(e);
 *                      }
 *                      System.out.println(array.get(0));
 *
 *                  }
 *
 * 在JDK8之后 新增了DoubleAdder和LongAdder 在高并发情况下 LongAdder的性能比AtomicLong的性能更好 主要体现在自增上 它的大致原理如下:
 *
 *      在低并发情况下 和AtomicLong是一样的 对value值进行CAS操作 但是出现高并发的情况时 AtomicLong会进行大量的循环操作来保证同步
 *      而LongAdder会将对value值的CAS操作分散为对数组cells中多个元素的CAS操作(内部维护了一个Cell[] as数组 每个Cell里面有一个初始值为0的long型变量
 *      在高并发时会进行分散CAS 就是不同的线程可以对数组中不同的元素进行CAS自增 这样就避免了所有线程都对同一个值进行CAS) 只需要最后再将结果加起来即可
 *
 *              https://img-blog.csdnimg.cn/img_convert/8489be69eb5e0cdbc9748a70556250ad.png
 *
 * 使用如下:
 *                  public static void main(String[] args) {
 *
 *                       LongAdder adder = new LongAdder();
 *                       Runnable r = () -> {
 *                           for (int i = 0; i < 100000; i++) adder.add(1);
 *                       };
 *                       for (int i = 0; i < 100; i++) new Thread(r).start(); // 100个线程
 *                       try {
 *                           TimeUnit.SECONDS.sleep(1);
 *                       } catch (InterruptedException e) {
 *                           throw new RuntimeException(e);
 *                       }
 *                       System.out.println(adder.sum()); // 最后求和即可
 *
 *                  }
 *
 * 由于底层源码比较复杂 这里就不做讲解了 两者的性能对比(这里用到了CountDownLatch 建议学完之后再来看):
 *
 *                  private static long LongAdderTest() {
 *
 *                      CountDownLatch latch = new CountDownLatch(100);
 *                      LongAdder adder = new LongAdder();
 *                      long timeStart = System.currentTimeMillis();
 *                      Runnable r = () -> {
 *                          for (int i = 0; i < 100000; i++) adder.add(1);
 *                          latch.countDown();
 *                      };
 *                      for (int i = 0; i < 100; i++) new Thread(r).start();
 *                      try {
 *                          latch.await();
 *                      } catch (InterruptedException e) {
 *                          throw new RuntimeException(e);
 *                      }
 *                      return System.currentTimeMillis() - timeStart;
 *
 *                  }
 *
 *                  private static long AtomicIntegerTest() {
 *
 *                      CountDownLatch latch = new CountDownLatch(100);
 *                      AtomicInteger integer = new AtomicInteger();
 *                      long timeStart = System.currentTimeMillis();
 *                      Runnable r = () -> {
 *                          for (int i = 0; i < 100000; i++) integer.incrementAndGet();
 *                          latch.countDown();
 *                      };
 *                      for (int i = 0; i < 100; i++) new Thread(r).start();
 *                      try {
 *                          latch.await();
 *                      } catch (InterruptedException e) {
 *                          throw new RuntimeException(e);
 *                      }
 *                      return System.currentTimeMillis() - timeStart;
 *
 *                  }
 *
 * 除了对基本数据类型支持原子操作外 对于引用类型 也是可以实现原子操作的:
 *
 *                  public static void main(String[] args) {
 *
 *                      String a = "Hello";
 *                      String b = "World";
 *                      AtomicReference<String> reference = new AtomicReference<>(a);
 *                      reference.compareAndSet(a, b);
 *                      System.out.println(reference.get());
 *
 *                  }
 *
 * JUC还提供了字段原子更新器 可以对类中的某个字段进行原子操作(注意: 字段必须添加volatile关键字):
 *
 *                  public class Main {
 *
 *                      public static void main(String[] args) {
 *
 *                          Student student = new Student();
 *                          AtomicIntegerFileUpdater<Student> filedUpdate =
 *                                  AtomicIntegerFiledUpdater.newUpdater(Student.class, "age");
 *                          System.out.println(filedUpdater.incrementAndGet(student));
 *
 *                      }
 *
 *                      public static class Student {
 *                          volatile int age;
 *                      }
 *
 *                  }
 *
 * 了解了这么多原子类 是不是感觉要实现保证原子性的工作更加轻松了?
 */
public class Main {

    private static final AtomicInteger i = new AtomicInteger(0);

    static void test1() {

        /*int  i = 1;
        System.out.println(i++);*/

        /*AtomicInteger i = new AtomicInteger(1);
        System.out.println(i.getAndIncrement());*/

        /*Runnable r = () -> {
            for (int j = 0; j < 100000; j++)
                i.getAndIncrement();
            System.out.println("自增完成");
        };
        new Thread(r).start();
        new Thread(r).start();
        try {
            TimeUnit.SECONDS.sleep(1);
            System.out.println(i.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        /*AtomicInteger integer = new AtomicInteger(10);
        System.out.println(integer.compareAndSet(30, 20));
        System.out.println(integer.compareAndSet(10, 20));
        System.out.println(integer);*/

        AtomicInteger integer = new AtomicInteger(1);
        integer.lazySet(2);
        System.out.println(integer);

    }

    static void test2() {

        /*AtomicIntegerArray array = new AtomicIntegerArray(new int[]{0, 4, 1, 3, 5});
        Runnable r = () -> {
            for (int i = 0; i < 100000; i++) array.getAndAdd(1, 1);
        };
        new Thread(r).start();
        new Thread(r).start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(array);*/

        LongAdder adder = new LongAdder();
        Runnable r = () -> {
            for (int i = 0; i < 100000; i++) adder.add(1);
        };
        for (int i = 0; i < 100; i++) new Thread(r).start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(adder.sum());

    }

    static void test3() {

        /*String a = "Hello"; String b = "World";
        AtomicReference<String> reference = new AtomicReference<>(a);
        reference.compareAndSet(a, b);
        System.out.println(reference.get());*/

        Student student = new Student();
        AtomicIntegerFieldUpdater<Student> fieldUpdater =
                AtomicIntegerFieldUpdater.newUpdater(Student.class, "age");
        System.out.println(fieldUpdater.incrementAndGet(student));

    }

    public static void main(String[] args) {

        //test1();
        //test2();
        test3();

        /*System.out.println("使用AtomicLong的时间消耗: " + AtomicIntegerTest() + "ms");
        System.out.println("使用LongAdder的时间消耗: " + LongAdderTest() + "ms");*/

    }

    private static long LongAdderTest() {

        CountDownLatch latch = new CountDownLatch(100);
        LongAdder adder = new LongAdder();
        long timeStart = System.currentTimeMillis();
        Runnable r = () -> {
            for (int i = 0; i < 100000; i++) adder.add(1);
            latch.countDown();
        };
        for (int i = 0; i < 100; i++) new Thread(r).start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return System.currentTimeMillis() - timeStart;

    }

    private static long AtomicIntegerTest() {

        CountDownLatch latch = new CountDownLatch(100);
        AtomicInteger integer = new AtomicInteger();
        long timeStart = System.currentTimeMillis();
        Runnable r = () -> {
            for (int i = 0; i < 100000; i++) integer.addAndGet(1);
            latch.countDown();
        };
        for (int i = 0; i < 100; i++) new Thread(r).start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return System.currentTimeMillis() - timeStart;

    }

}
