package MultithreadProgrammingCore.AQS;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 加锁过程已经OK 我们接着来看 它的解锁过程 unlock()方法是在AQS中实现的:
 *
 *                  public void unlock() {
 *                      sync.release(1); // 直接调用了AQS中的release方法 参数为1表示解锁一次state值-1
 *                  }
 *
 *                  @ReservedStackAccess
 *                  public final boolean release(int arg) {
 *                      if (tryRelease(arg)) { // 和tryAcquire一样 也得子类去重写 释放锁操作
 *                          Node h = head; // 释放锁成功后 获取新的头结点
 *                          // 如果新的头结点不为空并且不是刚刚建立的结点(初始状态下status为默认值0 而上面在进行了shouldParkAfterFailedAcquire之后 会被设定为SIGNAL状态 值为-1)
 *                          if (h != null && h.waitStatus != 0)
 *                              unparkSuccessor(h); // 唤醒头结点下一个结点中的线程
 *                          return true;
 *                      }
 *                      return false;
 *                  }
 *
 *                  private void unparkSuccessor(Node node) {
 *                      // 将等待状态waitStatus设置为初始值0
 *                      int ws = node.waitStatus;
 *                      if (ws < 0)
 *                          compareAndSetWaitStatus(node, ws, 0);
 *                      // 获取下一个结点
 *                      Node s = node.next;
 *                      // 如果下一个结点为空或是等待状态已取消 那肯定是不能通知unpark的 这时就要遍历所有节点再另外找一个符合unpark要求的节点了
 *                      if (s == null || s.waitStatus > 0) {
 *                          s = null;
 *                          // 这里是从队尾向前 因为enq()方法中的t.next = node是在CAS之后进行的 而node.prev = t是CAS之前进行的 所以从后往前一定能够保证遍历所有节点
 *                          for (Node t = tail; t != null && t != node; t = t.prev)
 *                              if (t.waitStatus <= 0)
 *                                  s = t;
 *                      }
 *                      if (s != null) // 要是找到了 就直接unpark 要是还是没找到 那就算了
 *                          LockSupport.unpark(s.thread);
 *                  }
 *
 * 那么我们来看看tryRelease()方法是怎么实现的 具体实现在Sync中:
 *
 *                  @ReserveStackAccess
 *                  protected final boolean tryRelease(int releases) {
 *                      int c = getState() - releases; // 先计算本次解锁之后的状态值
 *                      if (Thread.currentThread() != getExclusiveOwnerThread()) // 因为是独占锁 那肯定这把锁得是当前线程持有才行
 *                          throw new IllegalMonitorStateException(); // 否则直接抛异常
 *                      boolean free = false;
 *                      if (c == 0) { // 如果解锁之后的值为0 表已经完全释放此锁
 *                          free = true;
 *                          setExclusiveOwnerThread(null); // 将独占锁持有线程设置为null
 *                      }
 *                      setState(c); // 状态值设定为c
 *                      return free; // 如果不是0表示此锁还没完全释放 返回false 是0就返回true
 *
 *                  }
 *
 * 综上 我们来画一个完整的流程:
 *
 *      https://img-blog.csdnimg.cn/img_convert/b8daaaf9f391b5f565c328c40c4ec212.png
 *
 * 这里我们只讲解了公平锁 有关非公平锁和读写锁 还请各位观众根据我们之前的思路 自行解读
 */
public class Main2 {

    static void test1() {

        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        lock.unlock();

    }

    public static void main(String[] args) {



    }

}
