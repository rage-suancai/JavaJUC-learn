package Multithreading.heavyweightLock;

/**
 * 重量级锁
 * 在JDK6之前 synchronized一直被称为重量级锁 monitor依赖于底层操作系统的Lock实现
 * Java的线程是映射到操作系统的原生线程上 切换成本比高 而在JDK6之后 锁的实现得到了改进 我们先从最原始的重量级锁开始:
 *
 * 我们说了 每个对象都有一个monitor于之关联 在Java虚拟机(HotSpot)中 monitor是由ObjectMonitor实现的:
 *
 *                  ObjectMonitor() {
 *                      _header         = NULL;
 *                      _count          = 0;
 *                      _waiters        = 0;
 *                      _recursions     = 0;
 *                      _object         = NULL;
 *                      _owner          = NULL;
 *                      _WaitSet        = NULL; // 处于wait状态的线程 会被加入到_WaitSet
 *                      _WaitSetLock    = 0;
 *                      _Responsible    = NULL;
 *                      _succ           = NULL;
 *                      _cxq            = NULL;
 *                      FreeNext        = NULL;
 *                      _EntryList      = NULL; // 处于等待锁block状态的线程 会被加入到该列表
 *                      _SpinFreq       = 0;
 *                      _SpinClock      = 0;
 *                      OwnerIsThread   = 0;
 *                  }
 *
 * 每个等待锁的线程都会被封装成ObjectWaiter对象 进入到如下机制:
 *
 *      https://img-blog.csdnimg.cn/img_convert/09248f3cfd15d82c216c3b7233173e1e.png
 *
 * ObjectWaiter首先会进入Entry Set等着 当线程获取到对象的monitor后进入TheOwner区域并把monitor中的owner变量设置为当前线程
 * 同时monitor中的计数器count加1 若线程调用wait()方法 将释放当前持有的monitor owner变量恢复为null count自减1
 * 同时该线程进入WaitSet集合中等待被唤醒 若当前线程执行完毕也将释放monitor并复位变量的值 以便其他线程进入获取对象的monitor
 *
 * 虽然这样的设计思路非常合理 但是在大多数应用上 每一个线程占用同步代码块的时间并不是很长
 * 我们完全没用必要将竞争中的线程挂起然后又唤醒 并且现代CPU基本都是多核心运行的 我们可以采用一种新的思路来实现锁
 *
 * 在JDK1.4.2时 引入了自旋锁(JDK6之后默认开启) 它不会将处于等待状态的线程挂起 而是通过无限循环的方式 不断检查是否能够获取锁 由于单个线程占用锁的时间非常短
 * 所以说循环次数不会太多 可能很快就能够拿到锁并运行 这就是自旋锁 当然 仅仅是在等待时间非常短的情况下 自旋锁的表现会很好 但是如果等待时间太长
 * 由于循环是需要处理器继续运算的 所以这样只会浪费处理器资源 因此自旋锁的等待时间是有限制的 默认情况下为10次 如果失败 那么会进而采用重量级锁机制
 *
 *      https://img-blog.csdnimg.cn/img_convert/6a4248f16de3ae5de8ad00a9333d7643.png
 *
 * 在JDK6之后 自旋锁得到了一次优化 自旋的次数限制不再固定的 而是自适应变化的 比如在同一个锁对象上 自旋等待刚刚成功获得过锁 并且持有锁的线程正在运行
 * 那么这次自旋也是有可能成功的 所以会允许自旋锁更多次 当然 如果某个锁经常都自旋失败 那么有可能会不再采用自旋锁策略 而是直接使用重量级锁
 */
public class Main {

    public static void main(String[] args) {



    }

}
