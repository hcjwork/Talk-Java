wait、notify、notifyAll都是顶级父类Object中的native方法，底层需要依赖操作系统来对线程实现阻塞和唤醒，
这个过程从Java源代码到Java原生代码到操作系统代码，涉及到用户态和内核态的相互切换、线程上下文切换、线程一些配置与状态
的变更，整个链路执行下来性能开销很大。

三种方法都是final修饰的，只可被继承不可被覆盖重写。其他可实例化的类的对象，其wait、notify、notifyAll等来自Object类。
其中notify()和notifyAll()是无参方法，wait还有个支持超时时间参数的重载方法。
notify()是选择唤醒等待对象锁的多个线程之一，具体选择哪一个线程由操作系统的具体实现决定。
notifyAll()则是唤醒所有等待对象锁的线程。
注意三种方法须由同一个锁对象调用才能达到预期效果，且必须在synchronized同步块中使用，因为要对锁对象进行监视。
如果不在synchronized同步块中使用，会抛出异常IllegalMonitorStateException。
例如：
private static final Object lockObj = new Object();
public static void printABC() {
    synchronized (lockObj) {
        lockObj.wait();
        lockObj.notify();
        lockObj.notifyAll();
    }
}

业务场景实现示例：使用synchronized+wait/notify/notifyAll，实现三个线程交替打印1~100
示例伪代码：
private static final Object lock = new Object(); // 定义一个共用的锁对象。
private static volatile int counter = 1; // 计数器从1开始。使用volatile保证内存可见性。
private static volatile int printId = 1; // 当前计数时应该要执行打印操作的线程编号。使用volatile保证内存可见性。

public static void doPrintWork() {
    // 创建三个线程并启动，每个线程调用打印方法，并传递自己的编号和下一个执行打印操作的线程编号
    new Thread(() -> print(1, 2), "线程1").start();
    new Thread(() -> print(2, 3), "线程2").start();
    new Thread(() -> print(3, 1), "线程3").start();
}

private static void print(int curId, int nextId) {
    // 使用synchronized声明同步块，否则无法使用锁对象的wait、notifyAll等方法。
    synchronized (lock) {
        // 循环到超过100
        while (counter <= 100) {
            // 如果不是轮到当前线程执行打印工作，进入阻塞状态，直至被唤醒
            // 注意这里要用while而不是if，因为if只判断一次，而一回轮转下来每个线程至少会有三次需要判断
            // 如果用if可能导致三个线程并不是交替打印的
            while (curId != printId) {
                try {
                    // wait需要处理下InterruptedException。InterruptedException是受检异常必须在编译器就处理。
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            // 如果被唤醒，先检查下是不是已经超过100了，因为有可能在当前线程阻塞期间已经被运行到了101
            // 主要如果这个判断放在阻塞判断的前面，会导致有线程打印超过100
            if (counter > 100) {
                // 超过100，唤醒所有阻塞线程，退出循环
                lock.notifyAll();
                break;
            }
            // 如果计数没有超过100，又处于执行状态，说明该打印了。打印当前计数并递增计数
            System.out.println(Thread.currentThread().getName() + "：" + counter++);
            // 打印完成后，该轮到其他线程工作了。把打印线程编号更新为下一个线程的编号
            printId = nextId;
            // 唤醒其他线程
            lock.notifyAll();
        }
    }
}

遗留的小问题：虽然三个线程确实交替执行打印1~100了，但运行程序并没有正常结束而是出于等待状态。

对象的wait、notify、notifyAll方法必须要在synchronized的同步块中使用，这一点就导致灵活性降低了，所以实际应用中
通常用juc包下的锁或并发工具类如ReentrantLock、LockSupport等代替wait/notify来实现多线程的并发同步控制。