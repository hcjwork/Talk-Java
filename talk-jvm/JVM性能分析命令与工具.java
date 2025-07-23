一、监控命令
1、jps
列出所有的Java进程。
完整命令：jps [-q] [-mlvV] [<hostid>]
<hostid>: <hostname>[:<port>]

`使用示例`
命令：jps
输出：
50736 Jps
38452
36680 RemoteMavenServer

首列是Java进程的进程号（PID），RemoteMavenServer是具体的Java进程名称。

2、jstat
实时监控GC、类加载、JIT编译等信息。
完整命令：jstat -<option> [-t] [-h<lines>] <vmid> [<interval> [<count>]]
option：所支持的选项。
-t：打印时间戳。
-h<lines>：每隔多少行附带打印表头。lines是大于0的整数。指定-h的同时必须指定lines，例如“-h1”，每隔1行打印表头。
vmid：Java进程ID，PID。
interval：时间间隔，每隔指定多长打印一次数据，单位默认毫秒，也支持秒，用秒单位时需显示指定s。例如：1000ms、2s。
count：打印多少次。传递一个整数，小于等于0时打印次数不限制。

所支持的选项可通过`jstat -option`命令查看，例如：jstat -option。
可选的选项如下：
-class
-compiler
-gc
-gccapacity
-gccause
-gcmetacapacity
-gcnew
-gcnewcapacity
-gcold
-gcoldcapacity
-gcutil
-printcompilation

-class打印类加载的数据，-gc打印GC的数据。

`使用示例1`
命令：jstat -gc -t -h1 36680 1s 2
说明：打印进程号为36680的Java进程的信息，每隔1s打印gc数据，一共打印2次，打印时间，每行打印都附带表头。
输出：
Timestamp        S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT
39858.0 512.0  512.0   0.0    0.0   24064.0   9973.2   87552.0     3113.7   18688.0 17930.7 2304.0 2138.3    108    0.304  12      1.639    1.943
Timestamp        S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT
39859.0 512.0  512.0   0.0    0.0   24064.0   9973.2   87552.0     3113.7   18688.0 17930.7 2304.0 2138.3    108    0.304  12      1.639    1.943

`使用示例2`
命令：jstat -class 36680 1s
说明：打印进程号为36680的Java进程的类加载信息，每隔1s打印数据，不限制打印次数，只在第一次打印时附带表头。
输出：
Loaded  Bytes  Unloaded  Bytes     Time
3488  6162.0       81   106.4      17.34
3488  6162.0       81   106.4      17.34

对于gc信息打印，原始输出结果虽然包含Eden区、S0区、S1区、Old区、GC等数据，但空间大小默认使用KB为单位的，且没有单位显示，
为了让输出结果的可读性更强，可以使用`awk`命令转化为指定单位，比如KB或MB。
示例：
jstat -gc <pid> 1000 | awk 'NR==1 {print $0} NR>1 {for(i=1;i<=NF;i++) if($i~/^[0-9]/) $i=sprintf("%.2fMB",$i/1024); print}'
每隔1000毫秒打印一次指定进程的GC信息，将KB数据转换为MB单位，保留2位小数。

`使用示例3`
命令：jstat -gcnew 36680 1000 | awk 'NR==1 {print $0} NR>1 {for(i=1;i<=NF;i++) if($i~/^[0-9]/) $i=sprintf("%.2fKB",$i/1024); print}'
说明：每隔1秒打印一次指定进程36680的GC信息，保留2位小数，以单位KB展示。
输出：
S0C    S1C    S0U    S1U   TT MTT  DSS      EC       EU     YGC     YGCT
0.50KB 0.50KB 0.00KB 0.19KB 0.00KB 0.01KB 0.50KB 23.50KB 6.37KB 0.11KB 0.00KB
0.50KB 0.50KB 0.00KB 0.19KB 0.00KB 0.01KB 0.50KB 23.50KB 6.37KB 0.11KB 0.00KB
0.50KB 0.50KB 0.00KB 0.19KB 0.00KB 0.01KB 0.50KB 23.50KB 6.62KB 0.11KB 0.00KB

3、jmap
堆内存分析（生成快照、查看对象分布）。
完整命令：jmap [option] <pid>
option：所支持的选项。
pid：要分析的进程ID。

option所支持的选项如下：
<none>     			   打印与 Solaris pmap 相同的信息。
-heap                  打印 Java 堆的主要信息。
-histo[:live]          打印 Java 对象堆的直方图；如果指定"live"子选项，则仅统计存活对象。
-clstats               打印类加载器统计信息。
-finalizerinfo         打印等待终结的对象信息。

-dump:<dump-options>   以 hprof 二进制格式转储 Java 堆。
                       转储选项：
                         live         仅转储存活对象；若未指定，则转储堆中所有对象
                         format=b     二进制格式
                         file=<文件>  将堆转储到指定文件
                       示例：jmap -dump:live,format=b,file=heap.bin <进程 ID>

-F                     强制模式。与-dump:<dump-options> <进程 ID>或-histo 配合使用，
                       当<进程 ID>无响应时强制进行堆转储或直方图统计。
                       此模式下不支持"live"子选项。

-h | -help             打印此帮助信息。
-J<flag>               将<flag>直接传递给运行时系统。

`使用示例1`
命令：jmap -heap 36680
说明：打印36680进程的Java堆信息。
输出：
主要包含“堆的关键配置、堆内存的使用率和可用空间”等信息。

`使用示例2`
命令：jmap -histo:live 36680 | head -n 5
说明：打印36680进程存活的类实例信息，只输出前5行（包括表头）。
输出：
 num     #instances         #bytes  class name
----------------------------------------------
   1:         13005        1070592  [C
   2:          3609         397168  java.lang.Class

`使用示例3`
命令：jmap -dump:format=b,file=heap.hprof 36680
说明：以hprof二进制格式转储Java堆信息，“heap.hprof”是自定义文件名称。
输出：
Dumping heap to F:\Work\heap.hprof ...
Heap dump file created

heap.hprof文件可以MAT或JProfiler工具进行分析查看。

但jmap命令执行时会产生STW停顿，对程序的运行影响较大，所以一般是在启动时开启OMM时自动堆转储功能，
通过`-XX:+HeapDumpOnOutOfMemoryError`开启，通过`HeapDumpPath`可指定文件导出后存放路径。

4、jstack
打印线程快照信息（查死锁、CPU飚高问题）。
完整命令1：jstack [-l] <pid>
完整命令2：jstack -F [-m] [-l] <pid>
操作选项说明：
-F          强制生成线程转储。当 jstack <pid> 无响应（进程挂起）时使用
-m          打印 Java 和本地栈帧（混合模式）
-l          长列表。打印关于锁的额外信息
-h或-help    打印此帮助消息

`使用示例1`
命令：jstack -l 36680
说明：打印进程36680的线程快照信息。
输出：线程快照信息。
注意：打印的信息很多，可以通过管道筛选关键词。例如：jstack -l 36680 | grep TIMED_WAITING -A 10 -B 10

`使用示例2`
命令：jstack -l 36680 | grep DEAD_LOCK -A 10 -B 10
说明：打印进程36680的线程死锁信息。
输出：线程快照信息。

`使用示例3`
命令：jstack -l 36680 > thread_dump.log
说明：打印进程36680的线程死锁信息，输出到thread_dump.log文件中。
输出：线程快照信息。


5、jcmd
多功能诊断（替代部分 jmap/jstack 功能）。
完整命令1：jcmd -l
完整命令2：jcmd <pid | main class> <command ...|PerfCounter.print|-f file>

`jcmd -l`类似于jps命令。

二、分析工具
VisualVM：JDK自带的分析工具，支持插件扩展，可视化监控堆、线程、CPU、MBean等信息。
MAT(Eclipse Memory Analyzer)：分析堆转储文件，OOM排查方便，有强大的对象引用链分析，可以查找内存泄漏。
JProfiler：商业级全功能分析，例如内存、线程、CPU。低开销，支持生产环境。
Arthas：阿里的分析工具，在线诊断（无需重启），支持热修复、方法调用追踪。
GCViewer：可视化分析GC日志。下载地址：https://github.com/chewiebug/GCViewer。
GCEasy：支持在线分析GC日志，在线地址：https://gceasy.io/。