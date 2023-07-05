### 并发容器介绍
怎么才能解决并发情况下的容器问题呢? 我们首先想到的肯定是给方法前面加个synchronized关键字 这样总不会抢了吧
在之前我们可以使用Vector或是Hashtable来解决 但是它们的效率实在是太低了 完全依靠锁来解决问题 因此现在已经很少再使它们了 这里也不会再去进行讲解

JUC提供了专用于并发场景下的容器 比如我们刚刚使用的ArrayList 在多线程环境下是没办法使用的 我们可以将其替换为JUC提供的多线程专用集合类:

                    static void test() {

                        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>(); // 这里使用CopyOnWriteArrayList来保证线程安全
                        Runnable r = () -> {
                            for (int i = 0; i < 100; i++) list.add("yxsnb");
                        };
                        for (int i = 0; i < 100; i++) new Thread(r).start();
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println(list.size());
                
                    }

我们发现 使用了CopyOnWriteArrayList之后 在没出现过上面的问题

那么它是如何实现的呢 我们先来看看它是如何进行add()操作的:

                    public boolean add(E e) {
                        final ReentrantLock lock = this.lock;
                        lock.lock(); // 直接加锁 保证同一时间只有一个线程进行添加操作
                        try {
                            Object[] elements = getArray(); // 获取当前存储元素的数组
                            int len = elements.length;
                            Object[] newElements = Arrays.copyOf(elements, len + 1); // 直接复制一份数组
                            newElements[len] = e; // 修改复制出来的数组
                            setArray(newElements); // 将元素数组设定为复制出来的数组
                            return true;
                        } finally {
                            lock.unlock();
                        }
                    }

可以看到添加操作是直接上锁 并且会先拷贝一份当前存放元素的数组 然后对数组进行修改 再将此数组替换(CopyOnWrite) 接着我们来看读操作:

                    public E get(int index) {
                        return get(getArray(), index);
                    }

因此 CopyOnWriteArrayList对于读操作不加锁 而对于写操作是加锁的 类似于我们前面讲解的读写锁机制 这样就可以保证不丢失读性能的情况下 写操作不会出现问题

接着我们来看对于HashMap的并发容器ConcurrentHashMap:

                    static void test() {

                        ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>();
                        for (int i = 0; i < 100; i++) {
                            int finalI = i;
                            new Thread(() -> {
                                for (int j = 0; j < 100; j++) map.put(finalI * 100 + j, "yxsnb");
                            }).start();
                        }
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println(map.size());
                
                    }

可以看到这里的ConcurrentHashMap就没有出现之前HashMap的问题了 因为线程之间会争抢同一把锁 我们之前在讲解LongAdder的时候学习到了一种压力分散思想 既然每个线程都想抢锁
那我就干脆多搞几把锁 让你们每个人都能拿到 这样就不会存在等待的问题了 而JDK7之前 ConcurrentHashMap的原理也比较类似 它将所有数据分为一段一段地存储 先分很多段出来
每一段都给一把锁 当一个线程占锁访问时 只会占用其中一把锁 也就是仅仅锁了一小段数据 而其他段的数据依然可以被其他线程正常访问

<img src="https://fast.itbaima.net/2023/03/06/elxSQDBkcmqWtGU.png">

这里我们重点讲解JDK8之后它是怎么实现的 它采用了CAS算法配合锁机制实现 我们先来回顾一下JDK8下的HashMap是什么样的结构

<img src="https://img-blog.csdnimg.cn/img_convert/3ad05990ed9e29801b1992030c030a00.png">

HashMap就是利用了哈希表 哈希表的本质其实就是一个用于存放后续节点的头结点的数组 数组里面的每一个元素都是一个头结点(也可以说就是一个链表) 当要新插入一个数据时
会先计算该数据的哈希值 找到数组下标 然后创建一个新的节点 添加到对应的链表后面 当链表的长度达到8时 会自动将链表转换为红黑树 这样能使得原有的查询效率大幅度降低
当使用红黑树之后 我们就可以利用二分搜索的思想 快速地去寻找我们想要的结果 而不是像链表一样挨个去看

又是基础不牢地动山摇环节 由于ConcurrentHashMap的源码比较复杂 所以我们先从最简单的构造方法开始下手

<img src="https://fast.itbaima.net/2023/03/06/DEFR3d6gzOf7oNs.png">

我们发现 它的构造方法和HashMap的构造方法有很大的出入 但是大体的结构和HashMap是差不多的 也是维护了一个哈希表
并且哈希表中存放的是链表或是红黑树 所以我们直接来看put()操作是如何实现的 只要看明白这个 基本上就懂了

                    public V put(K key, V value) {
                        return putVal(key, value, false);
                    }
                    
                    // 有点小乱 如果看着太乱 可以在IDEA中折叠一下代码块 不然有点难受
                    final V putVal(K key, V value, boolean onlyIfAbsent) {
                        if (key == null || value == null) throw new NullPointerException(); // 键值不能为空 基操
                        int hash = spread(key.hashCode()); // 计算键的hash值 用于确定在哈希表中的位置
                        int binCount = 0; // 一会用来记录链表长度的 忽略
                        for (Node<K,V>[] tab = table;;) { // 无限循环 而且还是并发包中的类 盲猜一波CAS自旋锁
                            Node<K,V> f; int n, i, fh;
                            if (tab == null || (n = tab.length) == 0)
                                tab = initTable(); // 如果数组(哈希表) 为空肯定是要进行初始化的 然后再重新进下一轮循环
                            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) { // 如果哈希表该位置为null 直接CAS插入结点作为头结即可(注意这里会将f设置当前哈希表位置上的头结点)
                                if (casTabAt(tab, i, null,
                                            new Node<K,V>(hash, key, value, null)))  
                                    break;                   // 如果CAS成功 直接break结束put方法 失败那就继续下一轮循环
                            } else if ((fh = f.hash) == MOVED) // 头结点哈希值为-1 这里只需要知道是因为正在扩容即可
                                tab = helpTransfer(tab, f); // 帮助进行迁移 完事之后再来下一次循环
                            else { // 特殊情况都完了 这里就该是正常情况了
                                V oldVal = null;
                                synchronized (f) { // 在前面的循环中f肯定是被设定为了哈希表某个位置上的头结点 这里直接把它作为锁加锁了 防止同一时间其他线程也在操作哈希表中这个位置上的链表或是红黑树
                                    if (tabAt(tab, i) == f) {
                                        if (fh >= 0) { // 头结点的哈希值大于等于0说明是链表 下面就是针对链表的一些列操作
                                            ...实现细节略
                                        } else if (f instanceof TreeBin) { // 肯定不大于0 肯定也不是-1 还判断是不是TreeBin 所以不用猜了 肯定是红黑树 下面就是针对红黑树的情况进行操作
                                            // 在ConcurrentHashMap并不是直接存储的TreeNode 而是TreeBin
                                            ...实现细节略
                                        }
                                    }
                                }
                                    // 根据链表长度决定是否要进化为红黑树
                                if (binCount != 0) {
                                    if (binCount >= TREEIFY_THRESHOLD)
                                        treeifyBin(tab, i); // 注意这里只是可能会进化为红黑树 如果当前哈希表的长度小于64 它会优先考虑对哈希表进行扩容
                                    if (oldVal != null)
                                        return oldVal;
                                    break;
                                }
                            }
                        }
                        addCount(1L, binCount);
                        return null;
                    }

怎么样 是不是感觉看着挺复杂 其实也还好 总结一下就是:

<img src="https://fast.itbaima.net/2023/03/06/qvRH4wsIi9fczVh.png">

我们接着来看看get()操作:

                    public V get(Object key) {
                        Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
                        int h = spread(key.hashCode()); // 计算哈希值
                        if ((tab = table) != null && (n = tab.length) > 0 &&
                            (e = tabAt(tab, (n - 1) & h)) != null) {
                            // 如果头结点就是我们要找的 那直接返回值就行了
                            if ((eh = e.hash) == h) {
                                if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                                    return e.val;
                            }
                            // 要么是正在扩容 要么就是红黑树 负数只有这两种情况
                            else if (eh < 0)
                                return (p = e.find(h, key)) != null ? p.val : null;
                            // 确认无误 肯定在列表里 开找
                            while ((e = e.next) != null) {
                                if (e.hash == h &&
                                    ((ek = e.key) == key || (ek != null && key.equals(ek))))
                                    return e.val;
                            }
                        }
                        // 没找到只能null了
                        return null;
                    }

综上 ConcurrentHashMap的put操作 实际上是对哈希表上的所有头结点元素分别加锁 理论上来说哈希表的长度很大程度上决定了ConcurrentHashMap在同一时间能够处理的线程数量
这也是为什么treeifyBin()会优先考虑为哈希表进行扩容的原因 显然 这种加锁方式比JDK7的分段锁机制性能更好

其实这里也只是简单地介绍了一下它的运行机制 ConcurrentHashMap真正的难点在于扩容和迁移操作 我们主要了解的是他的并发执行机制 有关它的其他实现细节 这里暂时不进行讲解