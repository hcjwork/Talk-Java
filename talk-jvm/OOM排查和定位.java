可以从离线和在线两种情况来探讨。
1、离线时
在离线时可以从服务运行日志、堆转储文件、GC日志三个方面来分析。 
1）查看服务运行日志
在运行日志中查找OutOfMemory关键词，查看堆栈上下文信息，确认OOM类型，OOM发生的内存区域是堆、方法区还是直接内存。
如果是堆中的OOM，日志中会包含“OutOfMemoryError: Heap”相关的报错信息；
如果是方法区中的OOM，日志中会包含“OutOfMemoryError: Metaspace”相关的报错信息；
如果是直接内存中的OOM，日志中会包含“OutOfMemoryError: Direct Memory”相关的报错信息。

2）分析HeapDump文件
使用`jmap -dump`命令导出堆转储文件，但这个命令会导致主线程停顿，生产环境不建议使用。
可以在服务启动时通过`-XX:+HeapDumpOnOutOfMemoryError`命令指定在OOM时自动导出堆转储文件，这个是推荐做法。
导出的堆转储文件可以用JDK自带的JVisualVM、MAT(Eclipse Memory Analyzer)、JProfile等工具查看，
查找大对象或内存泄露对象，比如重复创建的集合、未关闭的资源。

3）检查GC日志
通过`-XX:+PrintGCDetails`参数可以打印GC日志。在GC日志中重点分析GC频率、耗时，确认是否因内存不足导致Full GC无法回收。

2、在线时
如果还在线肯定是没有发生OOM异常，一旦发生了OOM异常JVM进程会自动终止。所以在线时要做的工作是关注内存的使用和回收情况，提前
预防OOM异常发生。
通过`jstat -gcutil`命令实时观察各内存分区的使用率即GC情况，包括Eden区、Survivor区、Old区、Metaspace等。
通过`jmap -histo`命令实时统计对象分布，定位异常类。
使用Arthas工具通过`heapdump`、`dashboard`、`ognl`的命令（Arthas的命令）查看对象引用链。 
重点观察GC前后的内存可用空间占比变化，如果GC前后变化不大，排查是否存在内存泄漏、大对象过多。