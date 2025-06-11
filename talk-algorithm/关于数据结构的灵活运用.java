1、当数字范围较小时，可以用boolean数组标识数字是否符合某个条件，以数字作为数组下标。
比如数字有没有被使用，月份是不是31天的。

2、使用TreeSet实现去重的同时按需排序，如果是按自然顺序倒序排，创建时传递一个比较器。
例如：TreeSet<Integer> = new TreeSet<>((k1, k2) -> k2 - k1);
如果是对自定义对象的某个属性排序，也类似。如果有更复杂的排序要求，可实现Comparable接口。

3、使用TreeMap可以在统计数量的同时支持排序，且可以支持key、value同时排序。
例如：先按value降序排序，再按key升序排序
示例伪代码：
// 先做好统计
HashMap<Character, Integer> hashMap = new HashMap<>();
// 利用TreeMap进行自定义排序
TreeMap<Character, Integer> treeMap = new TreeMap<>(new Comparator<Character>() {
    @Override
    public int compare(Character o1, Character o2) {
        Integer num1 = hashMap.get(o1);
        Integer num2 = hashMap.get(o2);
        // 先按value降序
        int compare = num2.compareTo(num1);
        if (compare == 0) {
            // 如果相等，就继续按key升序排序
            return o1.compareTo(o2);
        } else {
            return compare;
        }
    }
});
treeMap.putAll(hashMap);