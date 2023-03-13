package MultithreadProgrammingCore.AQS;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 底层实现
 * AbstractQueuedSynchronizer(下面称为AQS) 是实现锁机制的基础 它的内部封装了包括锁的获取 释放 以及等待队列
 *
 * 一个锁(排他锁为例)的基本功能加锁获取锁 释放锁 当锁被占用时 其他线程来争抢会进入等待队列 AQS已经将这些基本的功能封装完成了 其中等待队列是核心内容
 * 等待队列是由双向链表数据结构实现的 每个等待状态下的线程都可以被封装进结点中并放入双向链表中 而对于双向链表是以队列的形式进行操作的 它像这样:
 *
 *                              队列同步器
 *                     AbstractQueuedSynchronizer               头节点           结点1            结点2
 *                                                              Prev <--------- Prev <--------- Prev
 *                             Node head; --------------------> Next ---------> Next ---------> Next
 *                             Node tail; --------->           status          status          status
 *                             int status          |           thread          thread          thread
 *                                                 |                                             |
 *                                                 |--------------------------------------------->
 *
 * AQS中有一个head字段和一个tail字段分别记录双向链表的头结点和尾结点 而之后的一系列操作都是围绕此队列来进行的 我们先来了解一下每个结点都包含了哪些内容:
 *
 *                  static final class Node {
 *                    	// 每个节点都可以被分为独占模式节点或是共享模式节点 分别适用于独占锁和共享锁
 *                      static final Node SHARED = new Node();
 *                      static final Node EXCLUSIVE = null;
 *
 *                    	// 等待状态 这里都定义好了
 *                     	// 唯一一个大于0的状态 表示已失效 可能是由于超时或中断 此节点被取消
 *                      static final int CANCELLED =  1;
 *                    	// 此节点后面的节点被挂起(进入等待状态)
 *                      static final int SIGNAL    = -1;
 *                    	// 在条件队列中的节点才是这个状态
 *                      static final int CONDITION = -2;
 *                    	// 传播 一般用于共享锁
 *                      static final int PROPAGATE = -3;
 *
 *                      volatile int waitStatus; // 等待状态值
 *                      volatile Node prev; // 双向链表基操
 *                      volatile Node next;
 *                      volatile Thread thread; // 每一个线程都可以被封装进一个节点进入到等待队列
 *
 *                      Node nextWaiter; // 在等待队列中表示模式 条件队列中作为下一个结点的指针
 *
 *                      final boolean isShared() {
 *                          return nextWaiter == SHARED;
 *                      }
 *
 *                      final Node predecessor() throws NullPointerException {
 *                          Node p = prev;
 *                          if (p == null)
 *                              throw new NullPointerException();
 *                          else
 *                              return p;
 *                      }
 *
 *                      Node() {
 *                      }
 *
 *                      Node(Thread thread, Node mode) {
 *                          this.nextWaiter = mode;
 *                          this.thread = thread;
 *                      }
 *
 *                      Node(Thread thread, int waitStatus) {
 *                          this.waitStatus = waitStatus;
 *                          this.thread = thread;
 *                      }
 *                  }
 *
 * 在一开始的时候 head和tail都是null stare为默认值0:
 *
 *                  private transient volatile Node head;
 *
 *                  private transient volatile Node tail;
 *
 *                  private volatile int state();
 *
 * 不用担心双向链表不会进行初始化 初始化是在实际使用时才开始的 先不管 我们接着来看其他的初始化内容:
 *
 *                  private static final Unsafe unsafe = Unsafe.getUnsafe(); // 直接使用Unsafe类进行操作
 *
 *                  private static final long stateOffset; // 记录类中属性在内存中的偏移地址 方便Unsafe类直接操作内存进行赋值等(直接修改对应地址的内存)
 *                  private static final long headOffset; // 这里对应的就是AQS类中的state成员字段
 *                  private static final long tailOffset; // 这里对应的就是AQS类中的head头结点成员字段
 *                  private static final long waitStatusOffset;
 *                  private static final long nextOffset;
 *
 *                  static { // 静态代码块 在类加载的时候就会自动获取偏移地址
 *                      try {
 *                          stateOffset = unsafe.objectFieldOffset
 *                              (AbstractQueuedSynchronizer.class.getDeclaredFiled("state"));
 *                          headOffset = unsafe.objectFieldOffset
 *                              (AbstractQueuedSynchronizer.class.getDeclaredFiled("head"));
 *                          tailOffset = unsafe.objectFieldOffset
 *                              (AbstractQueuedSynchronizer.class.getDeclaredFiled("tail"));
 *                          waitStatusOffset = unsafe.objectFieldOffset
 *                              (Node.class.getDeclaredField("waitStatus"));
 *                          nextOffset = unsafe.objectFieldOffset
 *                              (Node.class.getDeclaredField("next"));
 *                      } catch (Exception ex) { throw new Error(ex); }
 *                  }
 *
 *                  // 通过CAS操作来修改头结点
 *                  private final boolean compareAndSetHead(Node update) {
 *                      // 调用的是Unsafe类的compareAndSwapObject方法 通过CAS算法比较对象并替换
 *                      return unsafe.compareAndSwapObject(this, headOffset, null, update);
 *                  }
 *                  // 同上 省略部分代码
 *                  private final boolean compareAndSetTail(Node expect, update) {
 *
 *                  private static final boolean compareAndSetWaitStatus(Node node, int expect, int update)
 *
 *                  private static final boolean compareAndSetNext(Node node, Node expect, Node update) {
 *
 * 可以发现 队伍列同步器由于使用到CAS算法 所以 直接使用了Unsafe工具类 Unsafe类中提供了CAS操作的方法(Java无法实现 底层由C++实现) 所有对AQS类中成员字段的修改 都有对应的CAS操作封装
 *
 * 现在我们大致了解了一下它的底层运作机制 我们接着来看这个类是如何进行使用的 它提供了一些可重写的方法(根据不同的锁类型和机制 可以自由定制规则 并且为独占式和非独占式锁都提供了对应的方法)
 * 以及一些已经写好的模板方法(模板方法会调用这些可重写的方法) 使用此类只需要将可重写的方法进行重写 并调用提供的模板方法 从而实现锁功能(学习过设计模式会比较好理解一些)
 *
 * 我们首先来看可重写方法:
 *
 *                  // 独占式获取同步状态 查看同步状态是否和参数一致 如果返回没有问题 那么会使用CAS操作设置同步状态并返回true
 *                  protected boolean tryAcquire(int arg) {
 *                      throw new UnsupportedOperationException();
 *                  }
 *                  // 独占式释放同步状态
 *                  protected boolean tryRelease(int arg) {
 *                      throw new UnsupportedOperationException();
 *                  }
 *                  // 共享式获取同步状态 返回值大于0表示成功 否则失败
 *                  protected int tryAcquireShared(int arg) {
 *                      throw new UnsupportedOperationException();
 *                  }
 *                  // 共享式释放同步状态
 *                  protected boolean tryReleaseShared(int arg) {
 *                      throw new UnsupportedOperationException();
 *                  }
 *                  // 是否在独占模式下被当前线程占用(锁是否被当前线程持有)
 *                  protected boolean isHeldExclusively() {
 *                      throw new UnsupportedOperationException();
 *                  }
 *
 * 可以看到 这些需要重写的方法默认是直接抛出UnsupportedOperationException 也就是说根据不同的锁类型
 * 我们需要去实现对应的方法 我们可以来看一下ReentrantLock(此类是全局独占式的)中的公平锁是如何借助AQS实现的:
 *
 *                  static final class FairSync extends Sync {
 *                      private static final long serialVersionUID = -3000897897090466540L;
 *
 *                      // 加锁操作调用了模板方法acquire
 *                      // 为了防止各位绕晕 请时刻记住 lock方法一定是在某个线程下为了加锁而调用的 并且同一时间可能会有其他线程也在调用此方法
 *                      final void lock() {
 *                          acquire(1);
 *                      }
 *
 *                      ...
 *                  }
 *
 * 我们先看看加锁操作干了什么事情 这里直接调用了AQS提供的模板方法acquire() 我们来看看它在AQS类中的实现细节:
 *
 *                  @ReservedStackAccess // 这个是JEP 270添加的新注解 它会保护被注解的方法 通过添加一些额外的空间 防止在多线程运行的时候出现栈溢出 下同
 *                  public final void acquire(int arg) {
 *                      if(!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) // 节点为独占模式Node.EXCLUSIVE
 *                      selfInterrupt();
 *                  }
 *
 * 首先会调用tryAcquire()方法(这里是由FairSync类实现的) 如果尝试加独占锁失败(返回false了)
 * 说明可能这个时候有其他线程持有了此独占锁 所以当前线程先等着 那么会调用addWaiter()方法将线程加入等待队列中:
 *
 *                  private Node addWriter(Node mode) {
 *
 *                      Node node = new Node(Thread.currentThread(), mode);
 *                      // 先尝试使用CAS直接入队 如果这个时候其他线程也在入队(就是不止一个线程在同一时间争抢这把锁)就进入enq()
 *                      Node pred = tail;
 *                      if (pred != null) {
 *                          node.prev = pred;
 *                          if (compareAndSetTail(pred, node)) {
 *                              pred.next = node;
 *                              return node;
 *                          }
 *                      }
 *                      // 此方法是CAS快速入队失败时调用
 *                      enq(node);
 *                      return node;
 *
 *                  }
 *
 *                  private Node enq(final Node node) {
 *                      // 自旋形式入队 可以看到这里是一个无限循环
 *                      for (;;) {
 *                          Node t = tail;
 *                          if (t == null) { // 这种情况只能说明头结点和尾结点都还没初始化
 *                              if (compareAndSetHead(new Node())) // 初始化头结点和尾结点
 *                                  tail = head;
 *                          } else {
 *                              node.prev = t;
 *                              if (compareAndSetTail(t, node)) {
 *                                  t.next = node;
 *                                  // 只有CAS成功的情况下 才算入队成功 如果CAS失败 那说明其他线程同一时间也在入队 并且手速还比当前线程快 刚好走到CAS操作的时候
 *                                     其他线程就先入队了那么这个时候node.prev就不是我们预期的节点了 而是另一个线程新入队的节点 所以说得进下一个循环再来一次CAS 这种形式就是自旋
 *                                  return t;
 *                              }
 *                          }
 *                      }
 *                  }
 *
 * 在了解了addWaiter()方法会将节点加入等待队列之后 我们接着来看 addWaiter()会返回已经进加入的节点 acquireQueued()在得到返回的节点时 也会进入自动旋状态 等待唤醒(也就是开始进入到拿锁的环节了):
 *
 *                  @ReservedStackAccess
 *                  final boolean acquireQueued(final Node node, int arg) {
 *                      boolean failed = true;
 *                      try {
 *                          boolean interrupted = false;
 *                          for (;;) {
 *                              final Node p = node.predecessor();
 *                              if (p == head && tryAcquire(arg)) { // 可以看到当此结点位于队首(node.prev == head)时 会再次调用tryAcquire方法获取锁 如果获取成功 会返回此过程中是否被中断的值
 *                                  setHead(node); // 新的头结点设置为当前结点
 *                                  p.next = null; // 原来的头结点没有存在的意义了
 *                                  failed = false; // 没有失败
 *                                  return interrupted; // 直接返回等待过程中是否被中断
 *                              }
 *                              // 依然没获取成功
 *                              if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) // 将当前节点的前驱结点等待状态设置为SIGNAL 如果失败将直接开启下一轮循环 直到成功为止 如果成功接着往下
 *                                  interrupted = true; // 挂起线程进入等待状态 等待被唤醒 如果在等待状态下被中断 那么会返回true 直接将中断标志设为true 否则就是正常唤醒 继续自旋
 *                          }
 *                      } finally {
 *                          if (failed)
 *                              cancelAcquire(node);
 *                      }
 *                  }
 *
 *                  private final boolean parkAndCheckInterrupt() {
 *                      LockSupport.park(this); // 通过nsafe类操作底层挂起线程(会直接进入阻塞状态)
 *                      return Thread.interrupted();
 *                  }
 *
 *                  private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
 *                      int ws = pred.waitStatus;
 *                      if (ws == Node.SIGNAL)
 *                          return ture; // 已经是SIGNAL 直接true
 *                      if (ws > 0) { // 不能是已经取消的结点 必须找到一个没被取消的
 *                          do {
 *                              node.prev = pred = pred.prev;
 *                          } while (pred.waitStatus > 0);
 *                          pred.next = node; // 直接抛弃被取消的结点
 *                      } else {
 *                          // 不是SIGNAL 先CAS设置为SIGNAL(这里没有返回true因为CAS不一定成功 需要下一轮再判断一次)
 *                          compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
 *                      }
 *                      return false; // 返回false 马上开启下一轮循环
 *                  }
 *
 * 所以 acquire()中的if条件如果为true 那么只有一种情况 就是等待过程中被中断了 其他任何情况下都是成功获取到独占锁 所以当等待过程被中断时 会调用selfInterrupt()方法:
 *
 *                  static void selfInterrupt() {
 *                      Thread.currentThread().interrupt();
 *                  }
 *
 * 这里就是直接向当前线程发送中断信号了
 *
 * 上面提到了LockSupport类 它是一个工具类 我们也可以来玩一下这个park和unpark:
 *
 *                  Thread t = Thread.currentThread(); // 先拿到主线程的Thread对象
 *                  new Thread(() -> {
 *                      try {
 *                          TimeUnit.SECONDS.sleep(1);
 *                          System.out.println("主线程可以继续运行了");
 *                          LockSupport.unpark(t);
 *                          // t.interrupt(); 发送中断信号也可以恢复运行
 *                      } catch (InterruptedException e) {
 *                           e.printStackTrace();
 *                      }
 *                  }).start;
 *                  System.out.println("主线程被挂起");
 *                  lockSupport.park();
 *                  System.out.println("主线程继续运行");
 *
 * 这里我们就把公平锁的lock()方法实现讲解完毕了(让我猜猜 已经晕了对吧 就是到源码越考验个人的基础知识掌握 基础不牢地动山摇) 接着我们来看公平锁的tryAcquire()方法:
 *
 *                  static final class FairSync extends Sync {
 *                      // 可重入独占锁的公平实现
 *                      @ReservedStackAccess
 *                      protected final boolean tryAcquire(int acquires) {
 *                         final Thread current = Thread.currenThread(); // 先获取当前线程的Thread对象
 *                         int c = getState(); // 获取当前AQS对象状态(独占模式下0为未占用 大于0表示已占用)
 *                         if (c == 0) { // 如果是0 那就表示没有占用 现在我们的线程就要来尝试占用它
 *                             // 等待队列是否不为空且当前线程没有拿到锁 其实就是看看当前线程有没有必要进行排队 如果没有必要排队 就说明可以直接获取锁
 *                             // CAS设置状态 如果成功则说明成功拿到了这把锁 失败则说明可能这个时候其他线程在争抢 并且还比你先抢到
 *                             if (!hasQueuedPredecessors() && compareAndSetState(0, acquires)) {
 *                                 // 成功拿到锁 会将独占模式所有者线程设定为当前线程(这个方法是父类AbstractOwnableSynchronizer中的 就表示当前这把锁已经是这个线程的了)
 *                                 setExclusiveOwnerThread(current);
 *                                 return true; // 占用锁成功 返回true
 *                             }
 *                         // 如果不是0 那就表示被线程占用了 这个时候看看是不是自己占用的 如果是 由于是可重入锁 可以继续加锁
 *                         } else if (current == getExclusiveOwnerThread()) {
 *                             int nextc = c + acquires; // 多次加锁会将状态值进行增加 状态值就是加锁次数
 *                             if (nextc < 0) // 加到int值溢出了?
 *                                  throw new Error("Maximum lock count exceeded");
 *                             setState(nextc); // 设置为新的加锁次数
 *                                  return true;
 *                         }
 *                         return false; // 其他任何情况都是加锁失败
 *                      }
 *                  }
 *
 * 在了解了公平锁的实现之后 是不是感觉有点恍然大悟的感觉 虽然整个过程非常复杂 但是只要理清思路 还是比较简单的
 */
public class Main1 {

    static void test1() {

        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        lock.unlock();

    }

    public static void main(String[] args) {

        Thread t = Thread.currentThread();
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
                System.out.println("主线程可以继续运行了...");
                LockSupport.unpark(t);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        System.out.println("主线程被挂起");
        LockSupport.park();
        System.out.println("主线程继续运行...");

    }

}
