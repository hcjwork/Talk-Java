ConcurrentHashMap简介：
ConcurrentHashMap是jdk1.5引入的可以保证并发操作安全的键值对集合，位于JUC(java.util.concurrent)包下，
底层基于Node数组+链表+红黑树的数据结构实现键值对的存取，
jdk1.8之前采用16个分段锁降低锁粒度，支持分段操作并行，以提高并发性能。
jdk1.8开始使用CAS代替分段锁，结合synchronized同步块实现更精细的同步控制。

ConcurrentHashMap通过Node数组的新建与复制实现动态扩容，Node数组的初始长度默认为16，每次扩容两倍，
扩容时机为数组的有效Node节点个数达到（数组长度 - 数组长度 / 4，即数组长度的3/4）时，比如有效节点数达到12时触发数组第一次扩容。
ConcurrentHashMap也有缩容机制，但仅在特定条件下触发，如迁移阶段或树退化为链表时。
一般缩容也不会缩小到初始容量，除非新建ConcurrentHashMap或者执行clear操作。

ConcurrentHashMap在key发生冲突时，先检查数组长度是否达到阈值64，
如果没有先扩容数组后重新计算数组中Node节点的key的哈希值，重新路由定位数组中存储的位置，从旧的数组转移到新的数组中；
如果已到阈值64，则以链表方式解决哈希冲突，新的key和value组成新的Node节点挂在数组中已有Node节点的next指针上。
当链表节点数达到阈值8时，转化链表为红黑树存储哈希冲突的键值对数据，以TreeNode保存key和value值。
而在移除键值对的过程中，如果红黑树的节点数降低到阈值6，则从树退化为链表。

ConcurrentHashMap的关键方法：
put(K key, V value)、get(Object key)、remove(Object key)。

put方法源码解析：
public V put(K key, V value) {
    // 调用putVal()方法存储键值对，无论是否已存在键值对都要添加，如果有就覆盖原来的键值对
    return (key, value, false);
}

putVal方法源码解析：

transient volatile Node<K,V>[] table; // 存储键值对的Node数组，即哈希表
private transient volatile int sizeCtl; // 数组初始化和大小调整的控制符号
private static final sun.misc.Unsafe U; // 用于调用CAS方法保证并发安全性
private static final long SIZECTL; // sizeCtl变量的内存地址
private static final int DEFAULT_CAPACITY = 16; // Node数组的初始容量

static final int TREEIFY_THRESHOLD = 8;

static final int MOVED     = -1; // hash for forwarding nodes
static final int TREEBIN   = -2; // hash for roots of trees
static final int RESERVED  = -3; // hash for transient reservations
static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

// onlyIfAbsent：是否只有在键值对不存在时才添加键值对
final V putVal(K key, V value, boolean onlyIfAbsent) {
    // 检查key或value是否为null，如果为null则抛出空指针异常（拓展问题1：为什么不允许key或value为null？）
    if (key == null || value == null) throw new NullPointerException();
    // 计算key的哈希值（源码在下方）
    int hash = spread(key.hashCode());
    // 红黑树的当前节点数
    int binCount = 0;
    // 这是个无限循环，把当前的Node数组拿出来用了
    for (ConcurrentHashMap.Node<K,V>[] tab = table;;) {
        // 定义：f为新Node节点，n为数组长度，i为数组索引，fh为Node节点的哈希值
        ConcurrentHashMap.Node<K,V> f; int n, i, fh;
        // Node数组为空，初始化数组
        if (tab == null || (n = tab.length) == 0)
            // 创建一个新的Node数组，长度为16
            tab = initTable();
        // 检查Node数组中是否已经有相同key的键值对了
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            // 数组中没有这个这个键值对，就创建新的Node节点存储key、value，新Node节点的next指针为null
            // 基于CAS保证新Node存入操作的并发安全性。
            if (casTabAt(tab, i, null,
                    new ConcurrentHashMap.Node<K,V>(hash, key, value, null)))
                break;                   // no lock when adding to empty bin
        }
        // 如果Node数组有这个键值对，检查键值对是否被移动过
        else if ((fh = f.hash) == MOVED)
            // 如果被移动过，说明经历了扩容过程
            tab = helpTransfer(tab, f);
        // 如果Node数组有这个键值对，也没被移动过
        else {
            // 旧的value（准备覆盖）
            V oldVal = null;
            // value更新操作上锁
            synchronized (f) {
                // 再检查下数组中的i位置是不是预期的Node
                if (tabAt(tab, i) == f) {
                    // 符合预期，检查下键值对Node是否被移动过（扩容、链表化、树化）
                    if (fh >= 0) {
                        // 没有被移动过，树的节点数设置为初始值1
                        binCount = 1;
                        for (ConcurrentHashMap.Node<K,V> e = f;; ++binCount) {
                            // 键值对的key
                            K ek;
                            // 检查下新旧键值对的key，只有当两个键值对的key的值和哈希值都相等才做更新操作
                            // 拓展：为什么重写equals方法要同时重写hashCode方法？
                            if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                            (ek != null && key.equals(ek)))) {
                                // 记录旧值，作为方法返回结果
                                oldVal = e.val;
                                // 是否覆盖旧值，onlyIfAbsent为true时不覆盖旧值
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            // 不是同一个key，则创建新的Node节点，挂在数组中这个位置上的Node节点的next指针上
                            // 即以链表方式解决key的哈希冲突
                            ConcurrentHashMap.Node<K,V> pred = e;
                            // 如果数组中里的这个Node节点已经有了后继节点，就不创建新Node节点了
                            // 如果没有后继节点，就以输入的key和value创建新的Node节点作为后继节点
                            if ((e = e.next) == null) {
                                pred.next = new ConcurrentHashMap.Node<K,V>(hash, key,
                                        value, null);
                                break;
                            }
                        }
                    }
                    // 数组中的Node节点已经被移动过了
                    // 检查数组中的Node节点是不是树节点
                    else if (f instanceof ConcurrentHashMap.TreeBin) {
                        // 数组中的Node节点确实是树节点
                        ConcurrentHashMap.Node<K,V> p;
                        // 树的节点数记录为2（数组中已有的这个Node + 要创建的新Node）
                        binCount = 2;
                        // 尝试创建新的树节点
                        if ((p = ((ConcurrentHashMap.TreeBin<K,V>)f).putTreeVal(hash, key,
                                value)) != null) {
                            // 新的树节点添加成功，记录旧的value，用于方法返回结果
                            oldVal = p.val;
                            // 是否覆盖旧值，onlyIfAbsent为true时不覆盖旧值
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            // 如果树节点数不为0
            if (binCount != 0) {
                // 判断节点数是否达到了链表转红黑树的阈值8
                if (binCount >= TREEIFY_THRESHOLD)
                    // 达到阈值8了，树化处理
                    treeifyBin(tab, i);
                // 旧的value不为null，返回旧的value
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    // 键值对总数加1
    addCount(1L, binCount);
    return null;
}

static final int HASH_BITS = 0x7fffffff; // 哈希位
static final int spread(int h) {
    return (h ^ (h >>> 16)) & HASH_BITS;
}

// Node数组初始化
private final ConcurrentHashMap.Node<K,V>[] initTable() {
    // tab对应Node数组，sc对应sizeCtrl。
    ConcurrentHashMap.Node<K,V>[] tab; int sc;
    // 循环处理数组初始化工作，直到初始化成功
    while ((tab = table) == null || tab.length == 0) {
        // 检查表初始化和大小调整的标识sizeCtl，如果为负数，表示正在初始化中或正在调整大小
        if ((sc = sizeCtl) < 0)
            // 提示CPU让出调度权，自旋等待
            Thread.yield(); // lost initialization race; just spin
        // sizeCtl不为负数，说明没有在进行数组初始化或大小调整的工作
        // 通过CAS尝试修改sc为-1
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
            // sc修改成功
            try {
                // 再检查下Node数组是否已被初始化过了。（双重检查，提高Node数组的操作安全性。单例模式中有经典应用）
                if ((tab = table) == null || tab.length == 0) {
                    // Node数组初始容量：16
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    // 创建一个长度为16的Node数组
                    ConcurrentHashMap.Node<K,V>[] nt = (ConcurrentHashMap.Node<K,V>[])new ConcurrentHashMap.Node<?,?>[n];
                    table = tab = nt;
                    // 记录下一次Node数组扩容触发阈值：12。数值中的元素达到12时准备扩容
                    sc = n - (n >>> 2);
                }
            } finally {
                // 更新sizeCtl为下一次Node数组扩容触发阈值12
                sizeCtl = sc;
            }
            break;
        }
    }
    // 返回新创建的Node数组，数值长度为16
    return tab;
}

static final <K,V> ConcurrentHashMap.Node<K,V> tabAt(ConcurrentHashMap.Node<K,V>[] tab, int i) {
    return (ConcurrentHashMap.Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
}

static final <K,V> boolean casTabAt(ConcurrentHashMap.Node<K,V>[] tab, int i,
                                    ConcurrentHashMap.Node<K,V> c, ConcurrentHashMap.Node<K,V> v) {
    return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
}

final ConcurrentHashMap.Node<K,V>[] helpTransfer(ConcurrentHashMap.Node<K,V>[] tab, ConcurrentHashMap.Node<K,V> f) {
    ConcurrentHashMap.Node<K,V>[] nextTab; int sc;
    if (tab != null && (f instanceof ConcurrentHashMap.ForwardingNode) &&
            (nextTab = ((ConcurrentHashMap.ForwardingNode<K,V>)f).nextTable) != null) {
        int rs = resizeStamp(tab.length);
        while (nextTab == nextTable && table == tab &&
                (sc = sizeCtl) < 0) {
            if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || transferIndex <= 0)
                break;
            if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                transfer(tab, nextTab);
                break;
            }
        }
        return nextTab;
    }
    return table;
}

static final int MIN_TREEIFY_CAPACITY = 64;
// 树化处理
private final void treeifyBin(ConcurrentHashMap.Node<K,V>[] tab, int index) {
    // b: 键值对Node节点，n: Node数组长度，sc: 没用到
    ConcurrentHashMap.Node<K,V> b; int n, sc;
    // Node数组不为空才往下处理
    if (tab != null) {
        // 数组的长度是否小于64（树化阈值）
        if ((n = tab.length) < MIN_TREEIFY_CAPACITY)
            // 数组长度未达到阈值64，Node数组进行2倍扩容
            tryPresize(n << 1);
        // 如果index位置的Node节点不为null且没有被移动过
        else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
            synchronized (b) {
                // 双重检查锁定机制，保障修改操作的并发安全性（单例模式其中的一种安全实现就是双重检查锁定模式）
                if (tabAt(tab, index) == b) {
                    ConcurrentHashMap.TreeNode<K,V> hd = null, tl = null;
                    // 从数组中index位置的Node节点开始遍历，直到最后一个Node节点
                    // 每个Node节点都转化为TreeNode节点构建红黑树结构
                    for (ConcurrentHashMap.Node<K,V> e = b; e != null; e = e.next) {
                        ConcurrentHashMap.TreeNode<K,V> p =
                                new ConcurrentHashMap.TreeNode<K,V>(e.hash, e.key, e.val,
                                        null, null);
                        if ((p.prev = tl) == null)
                            hd = p;
                        else
                            tl.next = p;
                        tl = p;
                    }
                    // 将红黑树的头节点存入Node数组的index位置中。至此完成了链表到红黑树的转化
                    setTabAt(tab, index, new ConcurrentHashMap.TreeBin<K,V>(hd));
                }
            }
        }
    }
}

private static final int MAXIMUM_CAPACITY = 1 << 30; // 2的30次方
// Node数组扩容，size是当前数组长度的2倍
private final void tryPresize(int size) {
    // MAXIMUM_CAPACITY >>> 1 => 2的15次方
    // 如果本次扩容后的数组长度达到了2的15次方，就以2的30次方做扩容长度；最大扩容长度也只到2的30次方
    int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
            // Node数组扩容，首次扩容时size + (size >>> 1) + 1 = 16 * 2 + 16 * 2 / 2 + 1 = 49
            // 首次扩容时tableSizeFor()方法返回：64
            tableSizeFor(size + (size >>> 1) + 1);
    // c = 64，首次扩容时，sc = 12（达到阈值：16 - 16 >>> 2）
    int sc;
    while ((sc = sizeCtl) >= 0) {
        ConcurrentHashMap.Node<K,V>[] tab = table; int n;
        if (tab == null || (n = tab.length) == 0) {
            // sc = 12 < c = 64，n = 64
            n = (sc > c) ? sc : c;
            // 尝试修改sc为-1，更新SIZECTL为-1
            if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if (table == tab) {
                        @SuppressWarnings("unchecked")
                        // Node数组扩容到64的容量
                        ConcurrentHashMap.Node<K,V>[] nt = (ConcurrentHashMap.Node<K,V>[])new ConcurrentHashMap.Node<?,?>[n];
                        table = nt;
                        // 64 - 64/2/2 = 48，下一次扩容的触发阈值就是48
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;
                }
            }
        }
        // 如果扩容后的长度小于扩容触发阈值，或者大于最大容量2的30次方，就放弃这次处理
        else if (c <= sc || n >= MAXIMUM_CAPACITY)
            break;
        // 将旧的Node数组的节点移到扩容后的新数组中，根据Node节点的key重新哈希处理，定位到新数组的位置
        else if (tab == table) {
            int rs = resizeStamp(n);
            if (sc < 0) {
                ConcurrentHashMap.Node<K,V>[] nt;
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                        sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                        transferIndex <= 0)
                    break;
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                    transfer(tab, nt);
            }
            else if (U.compareAndSwapInt(this, SIZECTL, sc,
                    (rs << RESIZE_STAMP_SHIFT) + 2))
                transfer(tab, null);
        }
    }
}

private static final int tableSizeFor(int c) {
    int n = c - 1;
    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    // 首次扩容，c = 49，n = 63
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1; // 64
}

// get方法没什么值得分析的，就是根据key的哈希值去找键值对，找到有就返回value，没有找到就返回null
public V get(Object key) {
    ConcurrentHashMap.Node<K,V>[] tab; ConcurrentHashMap.Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode());
    if ((tab = table) != null && (n = tab.length) > 0 &&
            (e = tabAt(tab, (n - 1) & h)) != null) {
        if ((eh = e.hash) == h) {
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;
        }
        else if (eh < 0)
            return (p = e.find(h, key)) != null ? p.val : null;
        while ((e = e.next) != null) {
            if (e.hash == h &&
                    ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}

// remove方法，移除键值对，数组中的Node节点、链表中的节点、红黑树的节点的数量都会收到影响
// 从红黑树退化为链表的关键方法在remove的处理中出现
public V remove(Object key) {
    return replaceNode(key, null, null);
}

final V replaceNode(Object key, V value, Object cv) {
    int hash = spread(key.hashCode());
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0 ||
            (f = tabAt(tab, i = (n - 1) & hash)) == null)
            break;
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        else {
            V oldVal = null;
            boolean validated = false;
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {
                        validated = true;
                        for (Node<K,V> e = f, pred = null;;) {
                            K ek;
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                V ev = e.val;
                                if (cv == null || cv == ev ||
                                    (ev != null && cv.equals(ev))) {
                                    oldVal = ev;
                                    if (value != null)
                                        e.val = value;
                                    else if (pred != null)
                                        pred.next = e.next;
                                    else
                                        setTabAt(tab, i, e.next);
                                }
                                break;
                            }
                            pred = e;
                            if ((e = e.next) == null)
                                break;
                        }
                    }
                    else if (f instanceof TreeBin) {
                        validated = true;
                        TreeBin<K,V> t = (TreeBin<K,V>)f;
                        TreeNode<K,V> r, p;
                        if ((r = t.root) != null &&
                            (p = r.findTreeNode(hash, key, null)) != null) {
                            V pv = p.val;
                            if (cv == null || cv == pv ||
                                (pv != null && cv.equals(pv))) {
                                oldVal = pv;
                                if (value != null)
                                    p.val = value;
                                else if (t.removeTreeNode(p))
                                    // 红黑树退化为链表
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        }
                    }
                }
            }
            if (validated) {
                if (oldVal != null) {
                    if (value == null)
                        addCount(-1L, -1);
                    return oldVal;
                }
                break;
            }
        }
    }
    return null;
}

static final int UNTREEIFY_THRESHOLD = 6; // 树退化为链表的阈值。节点数与阈值6的比较在transfer()方法中，太长了不好展示
// 截取部分展示如下
// ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :  (hc != 0) ? new TreeBin<K,V>(lo) : t;
// hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :  (lc != 0) ? new TreeBin<K,V>(hi) : t;

// 树退化处理
static <K,V> ConcurrentHashMap.Node<K,V> untreeify(ConcurrentHashMap.Node<K,V> b) {
    ConcurrentHashMap.Node<K,V> hd = null, tl = null;
    for (ConcurrentHashMap.Node<K,V> q = b; q != null; q = q.next) {
        ConcurrentHashMap.Node<K,V> p = new ConcurrentHashMap.Node<K,V>(q.hash, q.key, q.val, null);
        if (tl == null)
            hd = p;
        else
            tl.next = p;
        tl = p;
    }
    return hd;
}



问题1：ConcurrentHashMap为何不支持key和value为null
因为ConcurrentHashMap作为一个并发安全集合，需要保证多线程下操作的语义清晰，避免出现二义性。
如果允许key为null，在多线程下无法分辨是键值对的key原本就是null，还是key被其他线程移除了成为的null。
如果允许value为null，在多线程下无法分辨是键值对的value原本就是null，还是value被其他线程修改了成为的null。
虽然可以通过互斥锁来解决决线程竞争问题，但实现会更复杂且性能较低，关键是并不能解决null的二义性问题。
说白了只要在多线程环境下，null就至少有两种含义：原本为null或被变更为null，就不可避免会出现语义分歧。
所以禁止极端条件的输入是最高效的解决方式。

其他的并发安全的键值对集合也同样不支持key或value为null，就是为了避免多线程下key和value相关操作出现歧义。
比如Hashtable也是不支持null值的key或value。
HashMap之所以支持null值的key或value，是因为定位不同，HashMap不保证并发安全，本身就不适用于多线程环境。
如果不是多线程环境，key或value为null也是可以接受的，因为只有一个线程操作，null的语义是清晰无歧义的。

问题2：当链表节点数达到8时，为什么要把链表转化为红黑树？
链表是为了解决key哈希冲突的，随着哈希冲突的key越来越多，链表上挂载的Node节点也越来越多，
哈希冲突解决的处理效率以及get相关操作的效率就会越来越低，因为要逐个遍历链表，
尤其是插入新的Node节时，需要遍历到链表尾部，将新的Node节点挂在链表最后一个节点的next指针上。
这些操作的时间复杂度平均为O(n)，为了提高键值对插入和查找的效率，在链表节点数达到8时，
转化链表为红黑树，能将操作的时间复杂度降低到O(log n)，特别是在高并发的插入或查找时，效率提升明显。

问题3：为什么要使用红黑树而不是别的树？
使用红黑树是功能、性能与实现复杂度的权衡。
普通二叉树在极端情况下可能会退化为链表，操作的时间复杂度上升到O(n)。
而红黑树通过左右旋转能保证最坏情况下操作的时间复杂度也只到O(log n)。
红黑树虽然比普通的二叉树复杂，但相比许多其他平衡树更容易实现，且旋转操作简单可预测。
另外每个节点只需要格外1位存储颜色信息（boolean red），空间开销很小。
所以使用红黑树是综合考量了操作效率、实现复杂度、空间开销等多方面的均衡结果。

问题4：当红黑树节点数减少到6时，为什么要退化为链表？
为了提高效率。虽然红黑树操作的平均时间复杂度为O(log n)，但其旋转操作需要一个常数时间O(1)，
包括上浮、下沉等操作也都需要时间，当数的节点降到6时，红黑树操作实际的时间开销并不比链表操作占优势，
因此将红黑树退化为链表以提高键值对操作的效率。

问题5：为什么链表转红黑树的阈值是8，红黑树转链表的阈值是6？
红黑树退化为链表的阈值不使用8，是为了避免链表和红黑树的频繁转换，
比如在高并发的写入和删除并行时，链表节点数和红黑树节点数可能来回浮动，如果不错开转换阈值可能会一直在相互转换。
另外阈值8和阈值6也是工业大量测验后的最佳结果，换成其他的阈值数就没这么好的效果了。

问题6：为什么在比较了新旧键值对的key的哈希值后，还要比较两个key是否相等？
因为不同的key的哈希值也可能相同，为了保证操作的正确性和严谨性，必须还要比较两个key是否相等，
只有哈希值和key本身相同，才能确定是同一个key。

启示1：双重检查锁定结合volatile保证并发修改安全。
启示2：边界条件处理复杂时禁止输入可能会更简单高效。（例如一些业务中的极端边界，可以考虑直接校验或过滤掉）
启示3：设计方案的选择是功能、性能、复杂度的综合权衡。
启示4：基础数据结构是数据处理和转换效率的基石。