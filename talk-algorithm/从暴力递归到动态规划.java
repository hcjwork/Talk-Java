动态规划，Dynamic Planning，简称DP。

什么叫动态规划？
如果一些值已经算过了，就存到表里面，下次需要对应的值直接取出来用就行，减少了很多重复工作。
说白了就是把已经计算过的结果缓存起来，下次要用直接取就不用重复计算了。
这种方式能提高效率，但是时间复杂度的级别并没有降低（降低的只是常数项？）

递归缓存法

先把常式函数也就是暴力递归的方式写出来。
假设我作为一个纯小白，怎么把这个常式解法写出来？

递归函数怎么写？
递归函数主要包含三部分：返回值、参数、递归处理
1）返回值
有些有返回值有些不用，返回值具体类型也取决于具体要求。比如要方法数，那就是int类型。这个其实比较好理解。
2）参数
参数一般从需求入手，给了哪些原始值，还包括一些隐含的如数组长度、数组起始位置等，然后还可以结合非边界时一般情况下一些要点，
比如当前位置、剩余长度或步数、已出现的次数等。
3）终止条件
这部分是最难把握的。每一个递归函数必须有return，不然就会栈溢出。那终止条件该怎么考虑呢？
一般从边界和特殊条件入手，比如最左侧、最右侧、长度为0、长度为1、长度为原始长度、当前对象为null等。

如果暴力递归每个分支的情形都是没有交叉的，那动态规划就无法优化也不能优化这样的递归。
只有出现不同分支有重复交叉情况时才能使用动态规划进行优化。

如何对暴力递归也就是常数写法进行优化？
1、先思考递归函数的返回值由谁来决定。
参数列表中，值不发生变化的参数对返回值没有决定性影响，而是由有变化的参数来决定。
而对于有变化的参数来说，不同分支的有些阶段计算出的结果是一样的，这部分就是重复工作了。
2、继续思考如何减少重复工作。
首先可以考虑缓存法，把计算过的缓存起来。
准备一个容器，先初始化每个位置标识为未计算，然后把容器带入递归方法，递归中先检查容器中有没有计算过对应的值，
有就直接返回，没有就计算后把结果存入容器，然后再返回计算结果。这样一来每个不同的子项就只算了一次。

这种一个个计算然后把算过的缓存起来是自顶向下的动态规划，也称作“记忆化搜索”。也是以空间换时间。

3、继续思考再往下怎么优化。
将决定递归函数返回值的参数列出来，如果超过3个，优化成2个。假设是2个参数，一个对应行，一个对应列，画一个表格。
假设这两个分别是n和m。n对应行，一共分成0~n即n+1行。m对应列，一共分成0~m即m+1列。
为什么要这么分，因为如果用容器存储计算过的值，索引从0开始，对应起来更好理解。0行或0列，根据情况决定是否选用。
划分出一个n+1行m+1列的这么一个表格，每个格子就代表参数为n和m时递归函数的返回值。

怎么填这个表格的格子？
先分析边界情况，就根据暴力递归或记忆化搜索的分支来，比如某个参数为0时预期是什么值，最左侧最右侧是什么值。
接着分析一般的普遍性情况，根据递归函数来，这种情况下递归函数的值依赖什么，关注其对上下左右时如何依赖的。

这个表格就是动态规划的二维数组。动态规划是结果，不是原因。不推荐去背状态转移方程，很难理解，又容易忘记。
分析具体问题时，就把这个n和m参数设小点，通过画图打表的方式，推出边界情况和普遍情况的值是如何依赖的。


