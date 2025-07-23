一、MAT下载安装和使用
MAT(Eclipse Memory Analyzer)，是Eclipse提供的分析Java堆栈的高效工具。
1、下载
zip包下载地址：https://eclipse.dev/mat/download/
注意MAT的版本与Java版本有对应关系，要注意版本匹配，否则可能出现兼容问题。
MAT1.12.x需要Java11+，MAT1.14.x需要Java17+，兼容java8的最高版本是1.10.0。

另外官网的下载镜像地址默认使用的是韩国的Kakao镜像，在国内不好用，可以通过下方的“Select Another Mirror”选项，
选择中国南京的镜像地址。
南京大学镜像站地址：https://mirrors.nju.edu.cn/eclipse/mat/
但南京大学镜像站最低版本只到1.13.0，没有之前版本的。

源码地址：https://github.com/eclipse-mat/mat

2、安装部署
下载好Zip包，在指定目录下解压即可。
解压后运行MemoryAnalyzer.exe程序，如果想调整MAT运行所占用的内存，可以在MemoryAnalyzer.ini配置文件中修改。

3、基本使用

二、Arthas下载安装和使用
官方文档：https://arthas.aliyun.com/doc/
官方中文指南：https://github.com/alibaba/arthas/blob/master/README_CN.md
jar包下载地址：https://arthas.aliyun.com/arthas-boot.jar
Arthas与Java版本关系：Arthas4.x版本支持Java8+，Java6/7需要使用Arthas3.x版本。

1、下载
在指定目录中打开cmd窗口，执行以下命令下载arthas包：
curl -O https://arthas.aliyun.com/arthas-boot.jar

2、启动
通过`java -jar`命令直接启动，具体命令如下：
java -jar arthas-boot.jar
执行此命令后，会打印出所有正在运行的JVM进程信息，使用中输入要分析的进程的对应编号，回车后arthas正式启动。

如果想查看帮助信息，执行以下命令：
java -jar arthas-boot.jar -h

如果想直接分析指定的进程，执行以下命令：
java -jar arthas-boot.jar <pid>

如果想要终止arthas，输入stop命令并执行。quit命令只会终止会话，Arthas在后台中还会继续运行。

3、常用命令
dashboard：查看实时监控面板，主要包含CPU、内存、线程等方面的数据。
thread：查看线程状态。
watch：监控方法调用。
trace：追踪方法调用链路。
jad：反编译类代码。
heapdump：导出堆内存快照。
cls：清屏。

heapdump命令生成的文件很大，建议在执行heapdump命令时直接指定文件的绝对路径，如果不指定默认放在操作系统的用户目录下。
以上命令的具体细节可以通过在其后加上-h或-help命令进行查看。例如：dashboard -h。

dashboard命令完整表述：dashboard [-h] [-i <value>] [-n <value>]
h：help，打印帮助信息。
i：interval，数据打印的时间间隔，以毫秒为单位，默认是每隔5000ms即5s打印一次。
n：number-of-executio，打印次数。

例如：
命令：dashboard -n 1
说明：只打印一次面板信息。

命令：dashboard -i 3000 -n 3
说明：每隔3秒打印一次面板信息，一共打印3次。

其他命令的详细功能在启动arthas后可以用help命令自行查看。
也可以查看官网文档：https://arthas.aliyun.com/doc/commands.html

三、JVisualVM的使用
JVisualVM是JDK自带的分析JVM的工具，可以直接感知当前机器的所有运行中的JVM进程，也可以通过远程方式监控JVM进程，
当然也可以通过“文件->导入”菜单导入.hprof文件进行分析。

通常在启动java程序时，通过`-XX:+HeapDumpOnOutOfMemoryError`参数开启OOM自动堆转储功能，
通过`HeapDumpPath`指定文件导出后存放路径。
当发生OOM问题时，将生成的.hprof文件导入JVisualVM可视化界面中，即可分析哪个类产生的OOM、类的实例数和占用内存情况。
而对于本地进程和远程进程，还可以实时监控CPU使用、堆和方法区内存使用等数据。