package MultithreadProgrammingCore.atomicClass;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
 *
 *
 *
 */
public class Main {

    private static AtomicInteger i = new AtomicInteger(0);

    static void test1() {

        /*int  i = 1;
        System.out.println(i++);*/

        /*AtomicInteger i = new AtomicInteger(1);
        System.out.println(i.getAndIncrement());*/

        Runnable r = () -> {
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
        }

    }

    static void test2() {

        AtomicInteger integer = new AtomicInteger(10);
        System.out.println(integer.compareAndSet(30, 20));
        System.out.println(integer.compareAndSet(10, 20));
        System.out.println(integer);

    }

    public static void main(String[] args) {

        //test1();
        test2();

    }

}
