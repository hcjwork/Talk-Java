String类有一个静态方法format()，常用来做字符串的格式化操作。
我之前用这个方法做比较多是字符串替换和前导补位，但其实还可以来做其他的操作，算是一个小的工具方法了。
而且之前对于字符串的前导补位还搞混淆一些东西，正好这次厘清下，顺便探索下别的用法。

1、字符串替换
使用%s作为占位符，替换成目标字符串。其实直接用String对象的replace()方法效果一样，但看起来会比较杂乱。
所以有些时候还是用String.format()实现更优雅，尤其是在字符串拼接的时候，比如异常catch到后打印关键日志，需要拼接主键什么的。
示例：
String replaceStr = String.format("Hello %s", "World");
当需要拼接多个串时，这些串又要放在中间位置，如果用+操作就显得比较臃肿。

2、整数的前导补位
使用%nd实现：转化整数为字符串，并在整数前面补位空格直到满足指定长度n。
用%nd的时候只能对整数进行操作，如果是对字符串操作则会报错。
示例：
String format1 = String.format("%5d", 10);
System.out.println("format1: " + format1); // format1:    10

如果想要对整数前导补位补得是0，就需要使用%0nd的写法。这样如果不到指定长度则会在前面补N个0，直到达到指定长度。
示例：
String format2 = String.format("%05d", 10);
System.out.println("format2: " + format2); // format2: 00010

3、字符串的前导补位
和整数的前导补位有点类似，但使用的是%ns实现。也一样是补位空格。
示例：
String format3 = String.format("%5s", "10");
System.out.println("format3: " + format3); // format3:    10

字符串的补位不支持直接补位0，如果想要达到和整数前补0一样的效果，则需要将空格替换成0。
示例：
String format4 = String.format("%5s", "10").replace(" ", "0");
System.out.println("format4: " + format4); // format4: 00010

4、字符串的后导补位
整数不支持后导补位，但字符串的操作支持。使用%-ns写法实现，在字符串后面补加空格直到满足长度n。
示例：
String format5 = String.format("%-5s", "10").replace(" ", "0");
System.out.println("format5: " + format5); // format5: 10000

5、字符串的截取
使用%.ns可以实现截取字符串的前n个字符。效果与String对象的substring()方法一样，实际中substring()方法用得较多。
示例：
String format6 = String.format("%.3s", "ocean and moonlight");
System.out.println("format6: " + format6); // format6: oce

6、四舍五入保留n位小数
使用%.nf可以实现将浮点数在四舍五入后保留n位小数。这个功能挺实用的，尤其在一些统计相关的业务中。
示例：
String format7 = String.format("%.2f", 4.4999);
System.out.println("format7: " + format7); // format7: 4.50