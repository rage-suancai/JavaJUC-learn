### 读写锁
除了可重入锁之外 还有一种类型的锁叫做读写锁 当然它并不是专门用作读写操作的锁 它和可重入锁不同的地方在于 可重入锁是一种排他锁 当一个线程得到锁之后
另一个线程必须等待其释放锁 否则一律不允许获取到锁 而读写锁在同一时间 是可以让多个线程获取到锁的 它其实就是针对于读写场景而出现的

读写锁维护了一个读锁和一个写锁 这两个锁的机制是不同的
- 读锁: 在没有任何线程占用写锁的情况下 同一时间可以有多个线程加读锁
- 写锁: 在没有任何线程占用读锁的情况下 同一时间只能有一个线程加写锁

读写锁也有一个专门的接口:

                    public interface ReadWriteLock {
                        // 获取读锁
                        Lock readLock();
                    
                        // 获取写锁
                        Lock writeLock();
                    }

此接口有一个实现类ReentrantReadWriteLock(实现的是ReadWriteLock接口 不是Lock接口 它本身并不是锁)
注意我们操作ReentrantReadWriteLock时 不能直接上锁 而是需要获取读锁或是写锁 再进行锁操作:

                    static void test() {

                        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
                
                        lock.readLock().lock();
                        new Thread(lock.readLock()::lock).start();
                
                    }

这里我们对读锁加锁 可以看到可以多个线程同时对读锁加锁:

                    static void test() {

                        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
                
                        lock.readLock().lock();
                        new Thread(lock.writeLock()::lock).start();
                
                    }

有读锁状态下无法加写锁 反之亦然:

                    static void test() {

                        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
                
                        lock.writeLock().lock();
                        new Thread(lock.readLock()::lock).start();
                
                    }

并且 ReentrantReadWriteLock不仅具有读写锁的功能 还保留了可重入锁和公平/非公平机制 比如同一个线程可以重复为写锁加锁 并且必须全部解锁才真正释放锁:

                    static void test() {

                        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
                        lock.writeLock().lock(); lock.writeLock().lock();
                
                        new Thread(() -> {
                            lock.writeLock().lock();
                            System.out.println("成功获取到写锁");
                        }).start();
                        System.out.println("释放第一层锁");
                        lock.writeLock().unlock();
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("释放第二层锁");
                        lock.writeLock().unlock();
                
                    }

通过之前的例子来验证公平和非公平:

                    static void test() {

                        ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
                
                        Runnable action = () -> {
                            System.out.println("线程 " + Thread.currentThread().getName() + " 将在1秒后开始获取锁...");
                            lock.writeLock().lock();
                            System.out.println("线程 " + Thread.currentThread().getName() + " 获取到了锁");
                            lock.writeLock().unlock();
                        };
                        for (int i = 0; i < 10; i++) { // 建立10个线程
                            new Thread(action, "T" + i).start();
                        }
                
                    }

可以看到 结果是一致的

### 锁降级和锁升级
锁降级指的是写锁降级为读锁 当一个线程持有写锁的情况下 虽然其他线程不能加读锁 但是线程自己是可以加读锁的:

                    static void test() {

                        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
                
                        lock.writeLock().lock(); lock.readLock().lock();
                        System.out.println("成功加读锁");
                
                    }

那么 如果我们在同时加了写锁和读锁的情况下 释放写锁 是否其他的线程就可以一起加读锁了呢?

                    static void test() {

                        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
                        lock.writeLock().lock(); lock.readLock().lock();
                
                        new Thread(() -> {
                            System.out.println("开始加读锁");
                            lock.readLock().lock();
                            System.out.println("读锁添加成功");
                        }).start();
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        lock.writeLock().unlock(); // 如果释放写锁 会怎么样?
                
                    }

可以看到 一旦写锁被释放 那么主线程就只剩读锁了 因为读锁可以被多个线程共享 所以这时第二个线程也添加了读锁
而这种操作 就被称之为"锁降级" (注意不是先释放写锁再加读锁 而是持有写锁的情况下申请读锁再释放写锁)

注意在仅持有读锁的情况下去申请写锁 属于"锁升级" ReentrantReadWriteLock是不支持的:

                    static void test() {

                        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
                        lock.readLock().lock(); lock.writeLock().lock();
                
                        System.out.println("锁升级成功");
                
                    }

可以看到线程直接卡在加写锁的那一句了