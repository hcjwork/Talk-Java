Serial回收器：单线程回收，简单、低延迟，适用于小堆（<100MB）。
Parallel Scavenge回收器：多线程并行处理，高吞吐，适用后台计算任务，适合中等到大堆规模，比如2GB-10GB。
CMS回收期：多线程并发处理，低延迟，可以减少STW时间，但内存碎片多，适合中等堆（<4GB）。
G1：分Region回收，可预测停顿，延迟较低、吞吐量也较大，平衡了吞吐与延迟，是JDK9+的默认垃圾回收器。适合大堆（>4GB）。
ZGC/Shenandoah：并发标记、压缩，适合TB级的超大堆，超低延迟，STW可以降低到亚毫秒级。

JDK8默认垃圾回收器为ParallelGC，JDK9默认垃圾回收器为G1。
CMS垃圾回收器可通过`-XX:+UseConcMarkSweepGC`参数启用。