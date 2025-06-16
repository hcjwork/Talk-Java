1、String对象的indexOf()方法
这个方法支持可传递最多两个参数，第一个参数是要检查的目标（字符串或整数），第二个参数是“int fromIndex”。
fromIndex参数，可以指定从此位置往后到原字符串末尾这个范围查找。可用于检查字符串是否存在重复子串。
用法示例：int index = "2fdehuiy2738fde23".indexOf("fde", 9);

2、Integer的toString()方法
Integer的静态方法toString()可以将一个整数转为指定进制的字符串。
比如将十进制整数100转为二进制的字符串：
String result = Integer.toString(100, 2);

3、Character的isLetter()方法
Character的isLetter()静态方法，可以快速判断一个字符是否是英文字母。
例如：
char cur = str.charAt(index);
if (Character.isLetter(cur)) {
    System.out.println(cur);
}

4、List集合的sort()方法
List集合的sort()方法，可以传递参数也可以不传递参数，不传参时默认以自然顺序升序排序。
如果想要自定义排序规则，则可以传递一个Comparator对象。Comparator是一个接口，需要实现compare方法。
例如：
arrayList.sort(new Comparator<Character>() {
    public int compare(Character c1, Character c2) {
        return c2 - c1;
    }
});

5、Character的toLowerCase()方法
如果想要将英文字母不区分大小写升序排序，同时又要保证同一字母的大小写按照出现的原顺序排列，就可以利用Character的toLowerCase()方法，
将比较的两个字母转为小写后再作差。
例如：
arrayList.sort(new Comparator<Character>() {
    public int compare(Character c1, Character c2) {
        return Character.toLowerCase(c1) - Character.toLowerCase(c2);
    }
});


