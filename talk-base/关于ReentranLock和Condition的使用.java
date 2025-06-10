ReentrantLock是juc包提供的实现了Lock接口的锁，基于AQS（AbstractQueuedSynchronizer）实现。
支持公平性的设置，支持可重入。
公平模式由AQS中的阻塞队列保证，如果设置公平性为true，锁被释放后，会唤醒处于阻塞队列队首的线程，
这样能竞争到锁的线程与最先申请锁的线程保持一致。

ReentrantLock对象通过lock()方法获取锁，没有获取到锁的线程会阻塞等待。
tryLock()方法则是一种非阻塞的方式获取锁，未获取到锁的线程不会进入阻塞队列。尝试获取，成功了返回true，失败返回false。

unlock()方法用于释放线程已持有的锁，释放锁后，ReentrantLock根据公平性从阻塞队列里唤醒一个线程去获取锁。
ReentrantLock对象并没有唤醒所有阻塞线程的方法，所以ReentrantLock实现了线程的互斥访问，即
同一时刻最多只有一个线程在执行ReentrantLock控制的代码块。

ReentrantLock本身无法让线程主动阻塞，因此提供了Condition来进行辅助。
Condition是一个接口，ReentrantLock使用AQS中的ConditionObject来实例化。
Condition实例通过ReentrantLock对象的newCondition()方法获取。

Condition对象提供了await()方法，可以让调用该方法的线程主动进入阻塞状态，
而通过Condition对象的signal()方法可以唤醒对应的线程。

Condition的具体实现由AQS支持，AQS通过LockSupport来完成线程的阻塞和唤醒。

由ReentrantLock对象创建的Condition对象，必须在ReentrantLock对控制的同步代码块中使用，否则
会抛出异常IllegalMonitorStateException。

下面从一个场景实现来熟悉下ReentrantLock和Condition（条件变量）的使用：使用ReentrantLock和Condition实现三个线程交替打印1~100
示例伪代码：
private static final ReentrantLock lock = new ReentrantLock();
private static final Condition condition1 = lock.newCondition();
private static final Condition condition2 = lock.newCondition();
private static final Condition condition3 = lock.newCondition();
private static volatile int counter = 1; // 从1开始
// 创建三个线程，为每个线程指定一个条件变量，并传递下一个要进行打印的线程所对应的条件变量
private static void doPrintWork() {
    new Thread(() -> print(condition1, condition2, 1), "线程1").start();
    new Thread(() -> print(condition2, condition3, 2), "线程2").start();
    new Thread(() -> print(condition3, condition1, 3), "线程3").start();

    lock.lock();
    try {
        condition1.signal();
    } finally {
        lock.unlock();
    }
}
// 打印1~100
private static void print(Condition cur, Condition next, int threadId) {
    lock.lock();
    try {
        // 1~100范围才打印
        while (counter <= 100) {
            if (counter > 100) {
                break;
            }
            // 如果是当前线程该打印的，打印对应数据
            if (counter % threadId == 0) {
                // 打印并累加counter
                System.out.println("线程" + threadId + "：" + counter++);
                // 打印完成功或者不是当前线程该打印时，唤醒下一个线程，阻塞当前线程
                next.signal();
            } else {
                // 不是当前线程该打印的，就阻塞等待
                cur.await();
            }
        }
        // 结束前再唤醒下其他线程，避免有些线程还在阻塞中
        next.signal();
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    } finally {
        lock.unlock();
    }
}