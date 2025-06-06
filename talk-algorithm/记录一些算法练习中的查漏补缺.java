1、如何将一个十进制的数转为八位的二进制字符串？
可以使用Integer的toString()方法指定进制，但前导0不会显示。
示例： String transfer = Integer.toString(255, 2);

还有Integer的toBinaryString()方法，可直接转为二进制字符串，但前导0也不会展示。
示例：String transfer = Integer.toString(255);

需要前位补0，可用String.format("%ns", str)实现。其中%和s是固定写法，n是最终要达到字符串长度。
但%ns前导补位为空格，需要替换空格为0。如String.format("%ns", str).replace(" ", "0");

拓展：
%nd是对数字进行前导补空格，其中%和d是固定写法，n是最终要达到字符串长度。只能用于数字的补位，用于字符串会报错。
%0nd则可以直接指定前导补位为0，只能用于数字的补位。

2、怎么判断一个字符串是否由若干个连续的1后跟若干个连续的0组成？<br>
1）使用正则表达式（推荐）。规则：匹配一个或多个连续的'1'后跟一个或多个连续的'0'
示例：
boolean isValid = str.matches("1+0+");
boolean isValid = str.matches("^1+0*$");

2）字符串截取判断
public static boolean isOneFollowedByZero(String str) {
    if (str == null || str.isEmpty()) {
        return false;
    }

    // 找到第一个0的位置
    int firstZeroIndex = str.indexOf('0');

    if (firstZeroIndex == -1) {
        // 全部是1
        return str.chars().allMatch(c -> c == '1');
    }

    // 检查0之后是否还有1
    if (str.substring(firstZeroIndex).contains("1")) {
        return false;
    }

    // 检查0之前是否全是1
    return str.substring(0, firstZeroIndex).chars().allMatch(c -> c == '1');
}
