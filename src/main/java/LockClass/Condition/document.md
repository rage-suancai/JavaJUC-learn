### Condition实现原理
通过前面的学习 我们知道Condition类实际上就是用于代替传统对象的wait/notify操作的 同样可以实现等待/通知模式
并且同一把锁下可以创建多个Condition对象 那么我们接着来看看 它又是如何实现的呢 我们先从单个Condition对象进行分析:

在AQS中 Condition有一个实现类ConditionObject 而这里也是使用了链表实现了条件队列:

                    public class ConditionObject implements Condition, java.io.Serializable {
                        private static final long serialVersionUID = 1173984872572414699L;
                        /** 条件队列的头结点 */
                        private transient Node firstWaiter;
                        /** 条件队列的尾结点 */
                        private transient Node lastWaiter;
                    
                        //...

这里是直接使用了AQS中的Node类 但是使用的是Node类中的nextWaiter字段连接节点 并且Node的status为CONDITION

<img src="https://fast.itbaima.net/2023/03/06/h7z96EeqVvpHOLQ.png">

我们知道 当一个线程调用await()方法时 会进入等待状态 直到其他线程调用signal()方法将其唤醒 而这里的条件队列 正是用于存储这些处于等待状态的线程

我们先来看看最关键的await()方法是如何实现的 为了防止一会绕晕 在开始之前 我们先明确此方法的目标:
- 只有已经持有锁的线程才可以使用此方法
- 当调用此方法后 会直接释放锁 无论加了多少次锁
- 只有其他线程调用signal()或是被中断时才会唤醒等待中的线程
- 被唤醒后 需要等待其他线程释放锁 拿到锁之后才可以继续执行 并且会恢复到之前的状态(await之前加了几层锁唤醒后依然是几层锁)

好了 差不多可以上源码了:

                    public final void await() throws InterruptedException {
                        if (Thread.interrupted())
                            throw new InterruptedException(); // 如果在调用await之前就被添加了中断标记 那么会直接抛出中断异常
                        Node node = addConditionWaiter(); // 为当前线程创建一个新的节点 并将其加入到条件队列中
                        int savedState = fullyRelease(node); // 完全释放当前线程持有的锁 并且保存一下state值 因为唤醒之后还得恢复
                        int interruptMode = 0; // 用于保存中断状态
                        while (!isOnSyncQueue(node)) { // 循环判断是否位于同步队列中 如果等待状态下的线程被其他线程唤醒 那么会正常进入到AQS的等待队列中(之后我们会讲)
                            LockSupport.park(this); // 如果依然处于等待状态 那么继续挂起
                            if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) // 看看等待的时候是不是被中断了
                                break;
                        }
                        // 出了循环之后 那线程肯定是已经醒了 这时就差拿到锁就可以恢复运行了
                        if (acquireQueued(node, savedState) && interruptMode != THROW_IE) // 直接开始acquireQueued尝试拿锁(之前已经讲过了)从这里开始基本就和一个线程去抢锁是一样的了
                            interruptMode = REINTERRUPT;
                        // 已经拿到锁了 基本可以开始继续运行了 这里再进行一下后期清理工作
                        if (node.nextWaiter != null) 
                            unlinkCancelledWaiters(); // 将等待队列中 不是Node.CONDITION状态的节点移除
                        if (interruptMode != 0) // 依然是响应中断
                            reportInterruptAfterWait(interruptMode);
                        // OK 接着该干嘛干嘛
                    }

实际上await()方法比较中规中矩 大部分操作也在我们的意料之中 那么我们接着来看signal()方法是如何实现的 同样的 为了防止各位绕晕 先明确signal的目标:
- 只有持有锁的线程才能唤醒锁所属的Condition等待的线程
- 优先唤醒条件队列中的第一个 如果唤醒过程中出现问题 接着找往下找 直到找到一个可以唤醒的
- 唤醒操作本质上是将条件队列中的结点直接丢进AQS等待队列中 让其参与到锁的竞争中
- 拿到锁之后 线程才能恢复运行

<img src="https://fast.itbaima.net/2023/03/06/UjG1Dd5xNJhIyWm.png">

好了 上源码:

                    public final void signal() {
                        if (!isHeldExclusively()) // 先看看当前线程是不是持有锁的状态
                            throw new IllegalMonitorStateException(); // 不是? 那你不配唤醒别人
                        Node first = firstWaiter; // 获取条件队列的第一个结点
                        if (first != null) // 如果队列不为空 获取到了 那么就可以开始唤醒操作
                            doSignal(first);
                    }

                    private void doSignal(Node first) {
                        do {
                            if ( (firstWaiter = first.nextWaiter) == null) // 如果当前节点在本轮循环没有后继节点了 条件队列就为空了
                                lastWaiter = null; // 所以这里相当于是直接清空
                            first.nextWaiter = null; // 将给定节点的下一个结点设置为null 因为当前结点马上就会离开条件队列了
                        } while (!transferForSignal(first) && // 接着往下看
                                 (first = firstWaiter) != null); // 能走到这里只能说明给定节点被设定为了取消状态 那就继续看下一个结点
                    }

                    final boolean transferForSignal(Node node) {
                        // 如果这里CAS失败 那有可能此节点被设定为了取消状态
                        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
                            return false;
                    
                        // CAS成功之后 结点的等待状态就变成了默认值0 接着通过enq方法直接将节点丢进AQS的等待队列中 相当于唤醒并且可以等待获取锁了
                        // 这里enq方法返回的是加入之后等待队列队尾的前驱节点 就是原来的tail
                        Node p = enq(node);
                        int ws = p.waitStatus; // 保存前驱结点的等待状态
                        // 如果上一个节点的状态为取消或者尝试设置上一个节点的状态为SIGNAL失败(可能是在ws>0判断完之后马上变成了取消状态 导致CAS失败)
                        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
                            LockSupport.unpark(node.thread); // 直接唤醒线程
                        return true;
                    }

其实最让人不理解的就是倒数第二行 明明上面都正常进入到AQS等待队列了 应该是可以开始走正常流程了 那么这里为什么还要提前来一次unpark呢?

这里其实是为了进行优化而编写 直接unpark会有两种情况:
- 如果插入结点前 AQS等待队列的队尾节点就已经被取消 则满足wc > 0
- 如果插入node后 AQS内部等待队列的队尾节点已经稳定 满足tail.waitStatus == 0 但在执行ws > 0之后!compareAndSetWaitStatus(p, ws, Node.SIGNAL)之前被取消 则CAS也会失败 满足compareAndSetWaitStatus(p, ws, Node.SIGNAL) == false

如果这里被提前unpark 那么在await()方法中将可以被直接唤醒 并跳出while循环 直接开始争抢锁 因为前一个等待结点是被取消的状态 没有必要再等它了

所以 大致流程下:

<img src="https://fast.itbaima.net/2023/03/06/w9hvtNyAM74pO8m.png">

只要把整个流程理清楚 还是很好理解的