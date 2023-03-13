队列同步器AQS

注意: 难度巨大 如果对锁的使用不是很熟悉建议之后再来看‼

前面我们了解了可重入锁和读写锁 那么它们的底层实现原理到底是什么样的呢? 又是大家看到就想跳过的套娃解析环节

比如我们执行了ReentrantLock的lock()方法 那它的内部是怎么在执行的呢?

                 public void lock() {
                     sync.lock();
                 }

可以看到 它的内部实际上啥都没做 而是交给了Sync对象在进行 并且 不只是这个方法 其他的很多方法都是依靠Sync对象在进行:

                 public void unlock() {
                     sync.release(1);
                 }

那么这个Sync对象是干什么的呢? 可以看到 公平锁和非公平锁都是继承自Sync 而Sync是继承自AbstractQueuedSynchronizer 简称队列同步器:

                 abstract static class Sync extends AbstractQueuedSynchronizer {
                     //...
                 }

                 static final class NonfairSync extends Sync {
                     //...
                 }
                 static final class FairSync extends Sync {
                     //...
                 }

所以 要了解它的底层到底是如何进行操作的 还得看队列同步器 我们就先从这里下手吧