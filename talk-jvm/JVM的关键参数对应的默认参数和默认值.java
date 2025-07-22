JVM有很多关键参数是可以在启动java程序时指定的，比如堆的大小、栈的大小、方法区的大小等，
这些关键参数有利于程序调优，这种在启动时指定或应用的参数是动态参数。
既然可以启动时指定，也可以选择不指定，那么JVM启动时各种参数必然有其默认值，默认值对应的参数名称与动态参数可能不同，
这部分参数可以称之为静态参数。
JVM的静态参数可以帮助程序开发者了解JVM的设计要素以及将来进行调整的方向。

JVM静态参数可以通过“-XX:+PrintFlagsInitial”参数查看，其中:后面的+表示启用，完整命令如下：
java -XX:+PrintFlagsInitial -version
此命令会打印当前版本的java的所有数据的默认参数及其默认值，如果想要针对性搜索，可以结合grep管道进行筛选，比如：
java -XX:+PrintFlagsInitial -version | grep Heap
需要注意：grep命令对搜索关键词是大小写敏感的。

JVM静态参数有很多，大部分其实是不用关注的，但有些与JVM的运行时数据区、垃圾回收、底层优化等相关的，值得程序开发者关注一二。
1、堆内存相关静态参数（java -XX:+PrintFlagsInitial -version | grep Heap）
`InitialHeapSize`
初始堆内存大小。
默认为当前机器物理内存的1/64，以bytes为单位展示。但在PrintFlagsInitial列表中显示为0，表示未显示设值。
当JVM启动时由JVM动态计算设置为物理内存的1/64，并标注为“:=”表示已修改。
这个值可以在“-XX:+PrintFlagsFinal”列表中查到。比如：
java -XX:+PrintFlagsFinal -version | grep InitialHeapSize
其中PrintFlagsFinal表示JVM启动运行时的最终参数列表。

`MaxHeapSize`
最大堆内存大小。
默认为当前机器的物理内存的1/4，以bytes为单位展示。但在PrintFlagsInitial列表中显示为物理内存的1/64。
当JVM启动时由JVM动态计算设置为物理内存的1/4，并标注为“:=”表示已修改。
这个值可以在“-XX:+PrintFlagsFinal”列表中查到。比如：
java -XX:+PrintFlagsFinal -version | grep MaxHeapSize

`InitialRAMFraction`
堆内存默认初始大小占物理内存的比例。
比如InitialRAMFraction=32，表示堆内存默认初始大小占物理内存的1/32。默认值为64。

`MaxRAMFraction`
堆内存默认最大大小占物理内存的比例。默认为4，表示堆内存最大占物理内存的1/4。

`NewRatio`
堆中老年代区域与新生代区域的比例。默认为2，表示老年代与新生代大小比例为2:1，即老年代占堆的2/3，新生代占堆的1/3。

`NewSize`
堆中新生代的初始大小。NewSize = InitialHeapSize / (NewRatio + 1)。
根据堆的初始大小和新老比例计算，如果堆的初始大小没有修改，InitialHeapSize默认为物理内存的1/64。

`MaxNewSize`
堆中新生代的最大大小。MaxNewSize = MaxHeapSize / (NewRatio + 1)。
根据堆的最大大小计算，如果堆的最大大小没有修改，MaxHeapSize默认为物理内存的1/4。

`OldSize`
堆中老年代的初始大小。OldSize = InitialHeapSize - NewSize。

`SurvivorRatio`
新生代中Eden区和Survivor区的比例。
默认为8，表示Eden:Survivor1:Survivor2=8:1:1。因为Survivor区是包含两个大小相等的S0和S1区域。

`MaxTenuringThreshold`
对象从新生代晋升老年代的年龄阈值。默认为15，对象在垃圾回收中每存活一次年龄加1。

`PretenureSizeThreshold`
对象直接分配在老年代的阈值（字节数，0 表示不启用）。默认为0。

2、方法区相关参数
`MetaspaceSize`
元空间的初始大小。默认为20MB左右，具体值与平台有关。
元空间是java8开始方法区的落地实现，在java7及之前版本是永久代（PermGen）。
元空间已用内存达到默认值后触发FullGC，如果MaxMetaspaceSize大于MetaspaceSize，则尝试扩容。

`MaxMetaspaceSize`
元空间的最大大小。默认无限制（上限为物理内存），建议设定一个固定值（比如256MB），以免出现内存泄露。

`MaxDirectMemorySize`
直接内存。默认无限制（上限为物理内存），建议显示设置。元空间使用的便是直接内存，也就是物理内存，不受JVM内存限制。

3、栈相关参数
`ThreadStackSize`
线程栈的默认大小。默认为1MB。高并发应用可调小此值，比如启动时设置-Xss256k。
ThreadStackSize调小，应用程序可以支撑线程并发数会增大，但不能不限调小。
因为线程栈空间要存储一个个栈帧，每调用一个方法就要存入一个栈帧，
因此方法的调用链路不宜过深，线程栈大小设置也要兼顾方法调用链的深度。

4、TLAB（Thread Local Allocation Buffer，线程本地分配缓冲区）相关参数
`UseTLAB`
是否启用TLAB功能。默认为true，即默认开启。
TLAB是JVM在堆中新生代的Eden区内为每个线程分配的私有内存区域，用于加速对象分配（避免全局堆锁竞争）。

`TLABSize`
线程本地分配缓冲区大小。默认为0，表示由JVM动态调整。

`MinTLABSize`
TLAB的最小大小，默认为2KB，低于2KB时不分配此区域。

`TLABWasteTargetPercent`
允许的单个线程TLAB所浪费的空间与Eden区的占比。
默认为1%，表示每个线程的TLAB允许的浪费空间（即未充分利用的部分）默认不超过Eden区的1%。
总TLAB占用由线程数和分配行为动态决定。

5、GC日志相关参数
`PrintGCDetails`
是否打印详细GC日志，默认为false，表示进行GC操作时不打印操作日志。
实际工程里建议开启。

`HeapDumpOnOutOfMemoryError`
OOM时是否自动生成堆转储文件，默认为false。
建议开启，方便排查OOM问题。

`HeapDumpPath`
堆转储文件路径，默认为当前目录，即应用程序所在目录。HeapDumpOnOutOfMemoryError开启时才有效。

6、垃圾回收相关参数
`UseParallelGC`
是否启用Parallel Scavenge回收器，JDK8 Server模式默认为true。

`UseConcMarkSweepGC`
是否启用 CMS 回收器，默认为false。

7、其他关键参数
`UseCompressedOops`
是否启用压缩指针，默认为true。可节省堆内存，堆<32GB时有效。


