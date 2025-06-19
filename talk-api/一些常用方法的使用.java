1、String对象的indexOf()方法
这个方法支持可传递最多两个参数，第一个参数是要检查的目标（字符串或整数），第二个参数是“int fromIndex”。
fromIndex参数，可以指定从此位置往后到原字符串末尾这个范围查找。可用于检查字符串是否存在重复子串。
用法示例：int index = "2fdehuiy2738fde23".indexOf("fde", 9);

2、Integer的toString()方法
Integer的静态方法toString()可以将一个整数转为指定进制的字符串。
比如将十进制整数100转为二进制的字符串：
String result = Integer.toString(100, 2);
但注意前导0不会展示。

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

6、Character的isLetter()方法
Character的isLetter()方法可以快速判断一个字符是否是字母，这个方法在字符串相关的算法中比较常用。
例如：
Character.isLetter("@");

7、Long的parseLong()方法
Long的parseLong()方法除了支持传递一个字符串参数之外，还支持传递一个进制数。
例如可以利用此方法将二进制字符串转化为十进制的长整型，
long l = Long.parseLong("00100101", 2);
类似的还有Integer的parseInt()方法、Short的parseShort()方法，也支持传递进制数，
可以将对应进制的字符串转为十进制的整数。

8、Collections的sort()方法
Collections的sort()方法可用于给List集合排序，默认是升序排序，如果想要倒序排或自定义排序，
可以传递一个Comparator对象。
例如：
ArrayList<Map.Entry<Character, Integer>> list = new ArrayList<>(map.entrySet());
Collections.sort(list, Map.Entry.comparingByValue(Collections.reverseOrder()));
如果是想要倒序排，可以用Collections.reverseOrder()传递一个反序的Comparator实现。

类似的如Arrays.sort()也可以传递反序的Comparator实现，但数组的元素得是引用类型的，比如包装类，
例如：
Integer[] arr = new Integer[] {1, 2, 3, 4};
Arrays.sort(arr, Collections.reverseOrder());
因为Collections.reverseOrder()返回的Comparator是带泛型的，只能用于引用类型数据的比较。


9、String对象的replaceAll()方法和trim()方法
String对象的replaceAll()方法可以用来替换目标字符串为指定字符串，trim()方法可以剪除字符两端的空格，
但trim()方法只能去除ASCII值为32的空格，即半角空格，
无法去除全角空格（ASCII值为160或Unicode编码为\u00A0）或其他类型的空白字符，如制表符(\t)或换行符(\n)，
这是比较坑的一点，因此实际使用trim()方法时可能无法剪除字符串前后的某些空格。

replaceAll()方法也有类似的问题，如果想要通过replaceAll(" ", "")删除掉空格，会发现有部分删不掉，
虽然都是用键盘上的space键敲的空格，半角和全角的空格也看起来没啥差别，所以最好别用这样方式，
而是用正则，例如：replaceAll("\\s+", "")，这样才能正常删除所有类型的空格。

10、StringBuilder对象的reserved()方法
StringBuilder对象的reserved()方法可以快速把StringBuilder所拼接的字符串反转，这个方法很实用。

11、java.math包下的BigInteger类
如果需要计算两个超大整数的相加，可以采用字符对齐，前导补0，然后从低位到高位逐位相加计算，可以用java.math.BigInteger类计算。
例如：
String a = "12323333333333354";
String b = "99999999454821";
BigInteger add = new BigInteger(a).add(new BigInteger(b));
System.out.println(add.toString());

12、利用Comparator实现二维数组自定义排序
例如先按第一个元素升序排，第一个元素相等时按照第二个元素降序排。
int[][] env = new int[][] {
    {1, 8}, {6, 8}, {1, 6}, {5, 4}, {4, 8}
};
System.out.println(env.length);
System.out.println("排序前：");
printArr(env);
Arrays.sort(env, (arr1, arr2) -> {
    // 先按第一个元素升序排
    int compare = arr1[0] - arr2[0];
    if (compare == 0) {
        // 第一个元素先相等时再按第二个元素降序排
        return arr2[1] - arr1[1];
    } else {
        return compare;
    }
});
System.out.println("排序后：");
printArr(env);

上面的这个比较器的实现也可以简化为：
Arrays.sort(env, (arr1, arr2) -> arr1[0] == arr2[0] ? arr2[1] - arr1[1] : arr1[0] - arr2[0]);

13、Arrays的fill()方法
这个方法可以快速将数组的所有元素初始化为指定值。当然for循环手动赋值也可以。用这个方法会更简洁些。
例如：Arrays.fill(arr, -1); // 全部初始化为-1

14、Math的PI常量与pow()方法
用于计算π相关的问题很方便。例如计算半径为r的圆的面积：
double d = Math.PI * Math.pow(r, 2);

15、BigDecimal的setScale()方法
BigDecimal的setScale()方法支持保留几位小数，并支持指定舍入规则，在BigDecimal类中有很多规则常量可选用。
例如将浮点类型数1.223按四舍五入保留5位小数，不足位的补0。
// 注意在创建BigDecimal时最好传入的是字符串，避免出现精度丢失。
new BigDecimal(1.223 + "").setScale(5, BigDecimal.ROUND_HALF_UP).toPlainString();