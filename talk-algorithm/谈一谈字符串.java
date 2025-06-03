1、使用二分查找检查是否存在重复子串

例1：检查是否存在两个独立的且长度大于3的相同的连续子串
private static boolean checkSubStrValid(String input) {
    int len = input.length();
    // 如果长度小于等于4，只能分出最多一个长度大于2的子串，天然合法
    if(len <= 4) {
        return true;
    }
    // 从0~len/2上的字符都截取个遍，截取长度从3~len/2都试一遍，然后看截取的末尾位置往后是否存在该子串
    // 从第3个字符开始遍历到len/2个字符
    for (int i = 3; i < len / 2; i++) {
        // 截取的末尾位置不能越界。假设要截取i个连续字符作为子串，从j位置开始截，则j+i要小于等于len
        for (int j = 0; j <= len - i; j++) {
            String sub = input.substring(j, j + i);
            // 从j+i位置往后是否还存在子串sub
            if (input.indexOf(sub, j + i) != -1) {
                // 存在则密码不合法
                return false;
            }
        }
    }
    return true;
}

前缀树
KMP算法
