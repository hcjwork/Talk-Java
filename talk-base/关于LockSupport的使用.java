LockSupport是juc包提供的一个线程阻塞与唤醒的工具类，基于Unsafe类的操作实现，这个类有许多native方法。
其他的AQS或CAS实现最终也是通过Unsafe类实现的，但其他的工具类是为了提供更丰富更灵活的API，或者是为了
在一些特定场景下有更高的性能和效率。

LockSupport的实现更靠近底层，提供的方法也不是很多，常用的都是静态方法，主要是park和unpark方法。
还有parkNacos()、parkUntil()等方法支持设置线程阻塞的超时时间，类似于对象的wait(long time)方法。

通过LockSupport.park()可以阻塞线程，通过LockSupport.unpark()可以唤醒线程。
但LockSupport.unpark()需要传递一个线程对象，native代码实现中应该也涉及到了操作系统的方法。

LockSupport.unpark()可以指定唤醒哪个线程，这相比于notify()提供了更精确的线程唤醒控制，
但不支持唤醒所有线程，相比于notifyAll()又缺少了灵活性。
可见不同的Java类提供的功能虽有所重合但侧重点不同，而实际业务也往往具有个性化差异，所以要根据具体业务来针对性选用。

下面通过一个场景实现来熟悉LockSupport的基本使用：使用LockSupport实现三个线程交替打印1~100
示例伪代码：
private static volatile int counter = 1; // 全局计数器，从1开始，使用volatile保证内存修改可见性。
private static Thread t1, t2, t3; // 线程对象需要定义在公共区域，因为LockSupport唤醒线程时需要指定具体的线程对象
// 创建三个线程并调用打印方法
public static void doPrintWork() {
    // 初始化三个线程，调用打印方法，传递下一个打印线程对象
    t1 = new Thread(() -> print(t2), "线程1");
    t2 = new Thread(() -> print(t3), "线程2");
    t3 = new Thread(() -> print(t1), "线程3");

    // 启动三个线程
    t1.start();
    t2.start();
    t3.start();

    // 唤醒线程1。unpark方法需要指定线程对象，这决定了三个线程必须声明在方法外即公共区域
    LockSupport.unpark(t1);
}

// 把下一个要执行打印操作的线程对象传进起来
private static void print(Thread nextThread) {
    // 循环直到计数器超过100
    while (counter <= 100) {
        // 一进来先阻塞住
        LockSupport.park();
        // 被唤醒了，判断计数器是否超过了100，因为在阻塞期间可能计数器被累加到超过100了
        if (counter > 100) {
            // 唤醒下一个打印线程
            LockSupport.park(nextThread);
            // 退出
            return;
        }
        // 执行打印操作，打印计数后递增计数器
        System.out.println(Thread.currentThread().getName() + "：" + counter++);
        // 唤醒下一个打印线程
        LockSupport.park(nextThread);
    }
}

精准控制的同时往往意味着通用灵活性的降低。
因此在实际应用中需要调研清楚业务的不同需求切面，这也是为什么需要有需求分析和技术选型的原因，
精准性、灵活性、扩展性，成了功能落地设计、模块交互设计、组件建构设计等的可行性基石。
