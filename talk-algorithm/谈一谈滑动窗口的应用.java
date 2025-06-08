滑动窗口指的是利用两个边界指针维护一个逻辑窗口，计算窗口范围（包括边界）内的数据是否符合要求，如果不合要求则移动窗口。
滑动窗口是双指针的一种经典应用，而对于两个边界指针的运动，分为同时移动和一方静止一方移动两种类型。
同时移动的情形适用于对称位置的处理和计算，比如连续回文子串。
一方静止一方移动的情形适用于非连续的非对称的处理和计算，比如最小覆盖子串。

另外从窗口宽度变动情形又有固定窗口和变动窗口的分别。
两个边界同时移动的情形包含有固定创建和变动窗口，而一方静止一方移动的情形通常是变动窗口。

下面通过一些实例来看下滑动窗口的应用：
1、最小覆盖子串
问题描述】
给定两个字符串s和t。返回s中包含t的所有字符的最短子字符串。如果s中不存在符合条件的子字符串，则返回空字符串""。
如果s中存在多个符合条件的子字符串，返回任意一个。
注意：对于t中重复字符，我们寻找的子字符串中该字符数量必须不少t中该字符数量。

示例1】
输入：s = "ADOBECODEBANC", t = "ABC"
输出："BANC"
解释：最短子字符串 "BANC" 包含了字符串 t 的所有字符 'A'、'B'、'C'
示例2】
输入：s = "a", t = "aa"
输出：""
解释：t 中两个字符 'a' 均应包含在 s 的子串中，因此没有符合条件的子字符串，返回空字符串。

问题分析及示例代码】
public static String getMinumCoverageSubstring(String s, String t) {
    String result = "";
    if (s == null || s.length() < 1 || null == t || t.length() < 1 || s.length() < t.length()) {
        return result;
    }

    // 前置准备：既然t中的重复字符要分开算，那么要通过数量来区分重复字符
    // 准备一个int数组，以字符在ASCII表中的位置为索引，记录t中每种字符出现的次数
    // ASCII表中A-Z为67-90，a-z为97-122，数组长度>=122就行，这里优化成基于字符A做计算，长度只需122-67=55就行，能减少遍历次数
    // 为保险起见，长度取到60，以免出现什么边界问题。或者定义为new int['z' - 'A' + 1]
    int[] tCharNums = new int[60];
    // 遍历字符串t的字符，记录每种字符出现的次数
    int tLength = t.length();
    for (int i = 0; i < tLength; i++) {
        tCharNums[t.charAt(i) - 'A']++;
        // 0~tLength-1的范围上，把字符串s的字符也计算下，每个字符计数减1，抵消字符串t的字符计数
        tCharNums[s.charAt(i) - 'A']--;
    }

    // 如果0~tLength-1范围就满足要求，直接返回0~tLength-1的子串。如果不满足，从tLength位置开始就行
    if (isAllZero(tCharNums)) {
        return s.substring(0, tLength);
    }

    // 接下来需要考虑几个问题：
    // 1、这种最小/最长的连续子串可以使用滑动窗口来解决，滑动窗口的左右边界怎么确定？
    // 2、滑动窗口的左右边界是同时动还是分开动？
    // 3、如何判断窗口内的子串已经包含了t中所有字符了？
    // 4、窗口内子串符合要求了，但要的是最短的，怎么缩小边界？
    //
    // 定义：left-窗口左边界；right-窗口右边界，从第tLength位开始，第tLength位前面的子串长度小于t的长度，不满足要求
    // minLen-符合要求的最短子串的长度，最差情况下是s的整个长度
    int left = 0, right = tLength, sLength = s.length(), minLen = sLength + 1;
    // 右指针不能越出s的边界
    while (right < sLength) {
        // 遍历到字符串s在右指针位置上的字符，tCharNums数组对应索引的计数-1
        tCharNums[s.charAt(right) - 'A']--;
        // 判断tCharNums所有位置的计数是否都为0了，如果都是0说明left到right这个范围上的子串已经都包含了t中的所有字符，重复字符的数量也符合要求
        // 如果符合要求，就让左边界向右移动，收缩窗口，直到不合要求，此时left到right范围的就是符合要求的最短子串
        while (isAllZero(tCharNums)) {
            if (right - left + 1 < minLen) {
                // 更新最短子串长度
                minLen = right - left + 1;
                // // 更新最短子串
                result = s.substring(left, right + 1);
            }
            // 右移left，收缩窗口。同时把left位置的字符计数+1，因为left右移后对应字符已经不在子串中了，需要重新校验
            tCharNums[s.charAt(left++) - 'A']++;
        }
        // right右移
        right++;
    }

    return result;
}
private static boolean isAllZero(int[] tCharNums) {
    for (int tCharNum : tCharNums) {
        if (tCharNum > 0) {
            return false;
        }
    }
    return true;
}

上面这个代码还有优化空间，仅作为示例。