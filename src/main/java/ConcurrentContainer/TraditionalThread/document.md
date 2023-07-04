### 并发容器
简单的讲完了 又该讲难一点的了

`注意`: 本版块的重点在于探究并发容器是如何利用锁机制和算法实现各种丰富功能的 我们会忽略一些常规功能的实现细节(比如链表如何插入元素删除元素)
        而更关注并发容器应对并发场景算法上的实现(比如在多线程环境下的插入操作是按照什么规则进行的)

在单线程模式下 集合类提供的容器可以说是非常方便了 几乎我们每个项目中都能或多或少的用到它们 我们在JavaSE阶段
为各位讲解了各个集合类的实现原理 我们了解了链表、顺序表、哈希表等数据结构 那么 在多线程环境下 这些数据结构还能正常工作吗?

### 传统容器线程安全吗?
我们来测试一下 100个线程同时向ArrayList中添加元素会怎么样:

                    static void test() {

                        List<Object> list = new ArrayList<>();
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

不出意外的话 肯定是会报错的:

                    Exception in thread "Thread-0" java.lang.ArrayIndexOutOfBoundsException: 73
                        at java.util.ArrayList.add(ArrayList.java:465)
                        at com.test.Main.lambda$main$0(Main.java:13)
                        at java.lang.Thread.run(Thread.java:750)
                    Exception in thread "Thread-19" java.lang.ArrayIndexOutOfBoundsException: 1851
                    at java.util.ArrayList.add(ArrayList.java:465)
                    at com.test.Main.lambda$main$0(Main.java:13)
                    at java.lang.Thread.run(Thread.java:750)
                    9773

那么我们来看看报的什么错 从栈追踪信息可以看出 是add方法出现了问题:

                    public boolean add(E e) {
                        ensureCapacityInternal(size + 1); // Increments modCount!!
                        elementData[size++] = e; // 这一句出现了数组越界
                        return true;
                    }

也就是说 同一时间其他线程也在疯狂向数组中添加元素 那么这个时候有可能在ensureCapacityInternal(确认容量足够)执行之后
elementData[size++] = e; 执行之前其他线程插入了元素 导致size的值超出了数组容量 这些在单线程的情况下不可能发生的问题 在多线程下就慢慢出现了

我们再来看看比较常用的HashMap呢?

                    static void test() {

                        HashMap<Integer, String> map = new HashMap<>();
                        for (int i = 0; i < 100; i++) {
                            int finalI = i;
                            new Thread(() -> {
                                for (int j = 0; j < 100; j++) map.put(finalI * 1000 + j, "yxsnb");
                            }).start();
                        }
                        try {
                            TimeUnit.SECONDS.sleep(2);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println(map.size());
                    
                    }

经过测试发现 虽然没有报错 但是最后的结果并不是我们期望的那样 实际上它还有可能导致Entry对象出现环状数据结构 引起死循环

所以 在多线程环境下 要安全地使用集合类 我们得找找解决方案了