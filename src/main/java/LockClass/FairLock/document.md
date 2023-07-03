### 公平锁一定公平吗?
前面我们讲解了公平锁的实现原理 那么 我们尝试分析一下 在并发的情况下 公平锁一定公平吗?

我们再次来回顾一下tryAcquire()方法的实现:

                    @ReservedStackAccess
                    protected final boolean tryAcquire(int acquires) {
                        final Thread current = Thread.currentThread();
                        int c = getState();
                        if (c == 0) {
                            if (!hasQueuedPredecessors() && // 注意这里 公平锁的机制是一开始会查看是否有节点处于等待
                                compareAndSetState(0, acquires)) { // 如果前面的方法执行后发现没有等待节点 就直接进入占锁环节了
                                setExclusiveOwnerThread(current);
                                return true;
                            }
                        }
                        else if (current == getExclusiveOwnerThread()) {
                            int nextc = c + acquires;
                            if (nextc < 0)
                                throw new Error("Maximum lock count exceeded");
                            setState(nextc);
                            return true;
                        }
                        return false;
                    }

所以hasQueuedPredecessors()这个环节容不得半点闪失 否则会直接破坏掉公平性 假如现在出现了这样的情况:

线程1已经持有锁了 这时线程2来争抢这把锁 走到hasQueuedPredecessors()判断出为false 线程2继续运行
然后线程2肯定获取锁失败(因为锁这时是被线程1占有的) 因此就进入到等待队列中:

                    private Node enq(final Node node) {
                        for (;;) {
                            Node t = tail;
                            if (t == null) { // 线程2进来之后 肯定是要先走这里的 因为head和tail都是null
                                if (compareAndSetHead(new Node()))
                                    tail = head; // 这里就将tail直接等于head了 注意这里完了之后还没完 这里只是初始化过程
                            } else {
                                node.prev = t;
                                if (compareAndSetTail(t, node)) {
                                    t.next = node;
                                    return t;
                                }
                            }
                        }
                    }
                    
                    private Node addWaiter(Node mode) {
                        Node node = new Node(Thread.currentThread(), mode);
                        Node pred = tail;
                        if (pred != null) { // 由于一开始head和tail都是null 所以线程2直接就进enq()了
                            node.prev = pred;
                            if (compareAndSetTail(pred, node)) {
                                pred.next = node;
                                return node;
                            }
                        }
                        enq(node); // 请看上面
                        return node;
                    }

而碰巧不巧 这个时候线程3也来抢锁了 按照正常流程走到了hasQueuedPredecessors()方法 而在此方法中:

                    public final boolean hasQueuedPredecessors() {
                        Node t = tail; // Read fields in reverse initialization order
                        Node h = head;
                        Node s;
                        // 这里直接判断h != t 而此时线程2才刚刚执行完 tail = head 所以直接就返回false了
                        return h != t &&
                            ((s = h.next) == null || s.thread != Thread.currentThread());
                    }

因此 线程3这时就紧接着准备开始CAS操作了 又碰巧 这时线程1释放锁了 现在的情况就是 线程3直接开始CAS判断
而线程2还在插入节点状态 结果可想而知 居然是线程3先拿到了锁 这显然是违背了公平锁的公平机制

一张图就是:

<img src="https://img-blog.csdnimg.cn/img_convert/6968cc52fe4b051dd7ca8cb262875b02.png">

因此公不公平全看hasQueuedPredecessors() 而此方法只有在等待队列中存在节点时才能保证不会出现问题 所以公平锁 只有在等待队列存在节点时 才是真正公平的