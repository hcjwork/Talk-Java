1、String对象的indexOf()方法
这个方法支持可传递最多两个参数，第一个参数是要检查的目标（字符串或整数），第二个参数是“int fromIndex”。
fromIndex参数，可以指定从此位置往后到原字符串末尾这个范围查找。可用于检查字符串是否存在重复子串。
用法示例：int index = "2fdehuiy2738fde23".indexOf("fde", 9);

2、Integer的toString()方法
Interger的静态方法toString()可以将一个整数转为指定进制的字符串。
比如将十进制整数100转为二进制的字符串：
String result = Integer.toString(100, 2);