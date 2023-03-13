package MultitHreading.lightweightLock;

/**
 * 轻量级锁
 *
 *      从JDK1.6开始 为了减少获得锁和释放锁带来的性能消耗 就引入了轻量级锁
 *
 * 轻量级锁的目标是 在无竞争的情况下 减少重量级锁产生的性能消耗(并不是为了代替重量级锁 实际上就是赌一手同一时间只有一个线程在占用资源)
 * 包括系统调用引用的内核态与用户态去切换 线程阻塞造成的线程切换等 它不像是重量级锁那样 需要向操作系统申请互斥量 它的运作机制如下:
 *
 * 在即将开始执行同步代码块的内容时 会首先检查对象的Mark Word 查看锁对象是否被其他线程占用 如果没有任何线程占用
 * 那么会在当前线程中所处的栈帧中建立一个名为锁记录(Lock Record)的空间 用于复制并存储对象目前的Mark Word信息(官方称为Displaced Mark Word)
 *
 * 接着 虚拟机将使用CAS操作将对象的Mark Word更新为轻量级锁状态(数据结构变为指向LockRecord的指针 指向的是当前的栈帧)
 *
 *      CAS(Compare And Swap)是一种无锁算法(我们之前在SpringBoot阶段已经讲解过了) 它并不会为对象加锁 而是在执行的时候
 *      看看当前数据的值是不是我们预期的那样 如果是 那就正常进行替换 如果不是 那么就替换失败 比如有两个线程都需要修改变量i的值 默认为10
 *      现在一个线程要将其修改为20 另一个要修改为30 如果他们都使用CAS算法 那么并不会加锁访问i 而是直接尝试修改i的值 但是在修改时 需要确认i是不是10
 *      如果是 表示其他线程还没对其他进行修改 如果不是 那么说明其他线程已经将其修改 此时不能完成修改任务 修改失败
 *
 *      在CPU中 CAS操作使用的是cmpxchg指令 能够从最底层硬件层面得到效率的提升
 *
 * 如果CAS操作失败了的话 那么说明可能这时有线程已经进入这个同步代码块了 这时虚拟机再次检查对象的Mark Word 是否指向当前线程的栈帧
 * 如果是 说明不是其他线程 而是当前线程已经有了这个对象的锁 直接放心大胆进同步代码块即可 如果不是 那确实被其他线程占用了
 *
 * 这时 轻量级锁一开始的想法就是错的(这时有对象在竞争资源 已经赌输了) 所以说只能将锁膨胀为重量级锁 按照重量级锁的操作执行(注意: 锁的膨胀是不可逆的)
 *
 *      https://img-blog.csdnimg.cn/img_convert/95ffc9204fd06b2b0b7e8363ffcd7e17.png
 *
 * 所以 轻量级锁 -> 失败 -> 自适应自旋锁 -> 失败 -> 重量级锁
 *
 * 解锁过程同样采用CAS算法 如果对象的Mark Word仍然指向线程的锁记录 那么就用CAS操作把对象的Mark Word和复制到栈帧中的Displaced Mark Word进行交换
 * 如果替换失败 说明其他线程尝试通过获取该锁 在释放锁的同时 需要唤醒被挂起的线程
 */
public class Main {

    public static void main(String[] args) {



    }

}
