1、通过`-XX:+PrintFlagsFinal`属性查看
此属性能查看JVM运行时的动态参数及当前值。
命令如下：
java -XX:+PrintFlagsFinal -version
示例：
java -XX:+PrintFlagsFinal -version | grep HeapSize

2、通过`-XX:+PrintFlagsInitial`属性查看
此属性能查看JVM的静态参数及其默认值。
命令如下：
java -XX:+PrintFlagsInitial -version
示例：
java -XX:+PrintFlagsInitial -version | grep InitialHeapSize

3、通过`jinfo`命令查看
以`jinfo -flags <pid>`命令查看运行中的JVM的显示设置的值。
以`jinfo -flag <name> <pid>`命令查看运行中的JVM的指定参数的值。name是参数名称。示例如下：
jinfo -flag 36680 MaxHeapSize
