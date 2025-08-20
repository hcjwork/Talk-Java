Java集合实现类：
ArrayList、EnumMap、HashMap、HashSet、Hashtable、IdentityHashMap、LinkedHashMap、LinkedHashSet、
LinkedList、PriorityQueue、Properties、TreeMap、TreeSet、Vector、WeakHashMap、Stack、ArrayBlockingQueue、
ConcurrentHashMap、ConcurrentLinkedDeque、ConcurrentLinkedQueue、ConcurrentSkipListMap、
ConcurrentSkipListSet、CopyOnWriteArrayList、CopyOnWriteArraySet、DelayQueue、LinkedBlockingDeque、
LinkedBlockingQueue、LinkedTransferQueue、PriorityBlockQueue。

单列集合顶层接口Collection，子接口List和Set；双列结合顶层接口Map。
常用单列集合实现类：
ArrayList、LinkedList、Vector、HashSet、LinkedHashSet、TreeSet、CopyOnWriteArrayList、
CopyOnWriteArraySet、ConcurrentSkipListSet、PriorityQueue、
常用双列集合实现类：
HashMap、Hashtable、LinkedHashMap、TreeMap、ConcurrentHashMap、ConcurrentSkipListMap、Stack。

常用集合实现类中可以排序的有：
ArrayList、LinkedList、Vector、CopyOnWriteArrayList、PriorityQueue、TreeSet、TreeMap、
ConcurrentSkipListSet、ConcurrentSkipListMap

常用集合实现类中可以去重的有：
HashSet、LinkedHashSet、TreeSet、CopyOnWriteArraySet、ConcurrentSkipListSet

常用集合实现类中并发安全的有：
Vector、CopyOnWriteArrayList、CopyOnWriteArraySet、Hashtable、ConcurrentHashMap

常用集合实现类中包含数组结构的有：
ArrayList、Vector、HashMap、ConcurrentHashMap、Hashtable、CopyOnWriteArrayList、CopyOnWriteArraySet、
PriorityQueue、Stack

常用集合实现类中包含链表结构的有：
LinkedList、LinkedHashSet、HashMap、ConcurrentHashMap、ConcurrentSkipListMap、ConcurrentSkipListSet

常用集合实现类中包含树结构的有：
HashMap、ConcurrentHashMap、TreeSet、TreeMap

常用集合实现类中基于数组排序的有：
ArrayList、Vector、CopyOnWriteArrayList、PriorityQueue

常用集合实现类中基于链表排序的有：
LinkedList、ConcurrentSkipListSet、ConcurrentSkipListMap

常用集合实现类中基于树排序的有：
TreeSet、TreeMap

常用集合复杂实现类的实现原理：
HashMap -> 数组 + 链表/红黑树
ConcurrentHashMap -> 数组 + 链表/红黑树 + 分段锁 + synchronized -> 并发安全
ConcurrentSkipListMap/Set -> 多层有序链表 + 跳表技术
CopyOnWriteArrayList/Set -> 数组 + 写时复制技术 -> 并发安全
TreeMap/Set -> 自定义节点 + 自平衡搜索二叉树/红黑树

常用集合实现类底层数据结构：数组、链表、树、队列、栈。
队列和栈通常是基于数组或链表实现。因此其实只有两种最底层的数据结构：数组 和 自定义节点对象。
数组要求存储空间连续并且元素类型统一，自定义节点对象的存储空间可以分散但单个对象的存储不可分割。

ConcurrentHashMap中的分段锁继承自ReentrantLock。
非并发安全集合可以通过Collections.synchronizedXxx方法转化为并发安全集合，原理就是给原来的关键操作套上synchronized。

并发场景下需使用并发安全集合，非并发场景比如方法体内且未出现引用逃出方法作用域可以使用非并发安全集合提升效率。
读到写少的场景通常使用CopyOnWriteArrayList/Set，写多的场景通常使用ConcurrentHashMap或锁机制。