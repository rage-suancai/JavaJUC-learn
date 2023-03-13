package MultitHreading.lockingMechanism;

/**
 * 在谈锁机制
 * 谈到锁机制 相信各位应该并不陌生了 我们在JavaSE阶段 通过使用synchronized关键字来实现锁
 * 这样能够很好地解决线程之间争抢资源 那么 synchronized底层到底是如何实现的呢?
 *
 * 我们知道 使用synchronized 一定是和某个对象相关联的 比如我们要对某一段代码加锁 那么我们就需要提供一个对象来作为锁本身:
 *
 *                  public static void main(String[] args) {
 *
 *                      synchronized (Main.class) {
 *                          // 这里使用的是Main类的Class对象作为锁
 *                      }
 *
 *                  }
 *
 * 我们来看看 它变成字节码之后会用到哪些指令:
 *
 *                   0 ldc #7 <javajuc2/Main>
 *                   2 dup
 *                   3 astore_1
 *                   4 monitorenter
 *                   5 aload_1
 *                   6 monitorexit
 *                   7 goto 15 (+8)
 *                  10 astore_2
 *                  11 aload_1
 *                  12 monitorexit
 *                  13 aload_2
 *                  14 athrow
 *                  15 return
 *
 * 其中最后关键的就是monitorenter指令了 可以看到之后也有monitorexit与之进行匹配(注意这里有2个) monitorenter和monitorexit分别对应加锁和释放锁
 * 在执行monitorenter之前需要尝试获取锁 每个对象都有一个monitor监视器与之对应 而这里正是去获取对象监视器的所有权 一旦monitor所有权被某个线程持有
 * 那么其他线程将无法获得(管程模式的一种实现)
 *
 * 在代码执行完成之后 我们可以看到 一共有两个monitorexit在等着我们 那么为什么这里会有两个呢
 * 按理说monitorenter和monitorexit不应该一一对应吗 这里为什么要释放锁两次呢?
 *
 * 首先我们来看第一个 这里在释放锁之后 会马上进入到一个goto指令 跳转到15行 而我们的15行对应的指令就是方法的返回指令
 * 其实正常情况下只会执行第一个monitorexit释放锁 在释放锁之后就接着同步代码块后面的内容继续向下执行了 而第二个 其实是用来处理异常的
 * 可以看到 它的位置是在12行 如果程序运行发生异常 那么就会执行第二个monitorexit 并且会继续向下通过athrow指令抛出异常 而不是直接跳转到15行正常运行下去:
 *
 *      https://img-blog.csdnimg.cn/img_convert/0e38acd553d82f6dc50113e6d0a425d6.png
 *
 * 实际上synchronized使用的锁存储在Java对象头中的 我们知道 对象是存放在堆内存中的 而每个对象内部 都有一部分空间用于存储对象头信息
 * 而对象头信息中 则包含了MarkWord用于存放hashCode和对象的锁信息 在不同状态下 它存储的数据结构有一些不同:
 *
 *      https://img-blog.csdnimg.cn/img_convert/dddacf08bcab7aa62ff8303eb363d797.png
 */
public class Main {

    public static void main(String[] args) {

        synchronized (Main.class) {

        }

    }

}
