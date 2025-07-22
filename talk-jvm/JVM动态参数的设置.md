JVM的有些关键的参数可以在启动时动态设置，具体命令通常是用`java -jar`来启动应用程序。

一、常用参数
1、`-Xms`
初始堆大小。示例：`-Xms2G`。

2、`-Xmx`
最大堆大小，建议与`-Xms`相同，避免运行时动态扩容带来的性能波动。示例：`-Xmx4G`。

3、`-XX:NewRatio`
新生代与老年代的比例，默认`2`，即老年代:新生代=2:1。示例：`-XX:NewRatio=3`。可用于调整新生代/老年代比例。

4、`-XX:SurvivorRatio`
Eden区与Survivor区的比例，默认`8`，即Eden:Survivor=8:1:1。

5、`-XX:MetaspaceSize`
元空间初始大小，Java8之前是PermGen。示例：`-XX:MetaspaceSize=256M`。适当调大以免频繁FullGC。

6、`-XX:MaxMetaspaceSize`
元空间最大大小，默认无限制，上限是物理内存。示例：`-XX:MaxMetaspaceSize=512M`。
应当显示设置一个固定值，避免元空间无限膨胀或产生内存泄漏问题。
可以考虑与MetaspaceSize设置相同的值，减少因动态扩容而导致的GC停顿。

7、`-Xss`
每个线程的栈大小，默认为1MB。示例：`-Xss256KB`。
适当调小可以减少线程数多时的内存消耗，增加程序可支撑的线程并发数。

二、常见场景的典型参数配置示例
1、通用 Web 服务
命令和参数示例：
java -Xms4G -Xmx4G \
-XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=512M \
-XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
-XX:+HeapDumpOnOutOfMemoryError \
-jar app.jar

2、高并发低延迟系统
命令和参数示例：
java -Xms8G -Xmx8G \
-XX:MetaspaceSize=512M \
-XX:+UseG1GC -XX:MaxGCPauseMillis=100 \
-XX:InitiatingHeapOccupancyPercent=45 \
-XX:G1HeapRegionSize=16M \
-jar app.jar

3、大数据处理
命令和参数示例：
java -Xms16G -Xmx16G \
-XX:NewRatio=1 -XX:SurvivorRatio=8 \
-XX:+UseParallelGC -XX:ParallelGCThreads=4 \
-jar your-app.jar

三、关键参数调整依准
堆内存：对象的内存空间在堆中分配，大对象较多时可适当调整堆内存大小。
方法区：设置固定值，避免内存泄漏或内存无限膨胀。但不能设置太小，以免FullGC频繁。
栈内存：适当降低，可以提高程序的并发能力。
垃圾回收器：根据具体应用方向选择合适的垃圾回收器。
堆转储：在OOM时自动转储堆的日志，方便OOM问题的定位排查。