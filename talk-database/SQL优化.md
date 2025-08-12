关系型数据库提供了一种结构化的数据查询或操作语言，称为SQL（Structure Query Language）。
数据库客户端与数据库服务端成功连接后，便可以通过SQL操作数据库和表。SQL可以分为DDL、DQL、DML三种类型的语句。
其中DDL语句是操作表结构和表约束的，包括什么数据库的创建删除、用户创建与赋权、表的创建和删除、索引创建和删除等具体操作。
其中DQL语句是指的表数据的查询语句，由select关键字作为语法起始，包含select、from、where、having、group by、
order by、limit、join、union等子句。
其中DML语句是指对于表数据的增删改语句，新增操作以insert关键字作为语法起始，修改操作以update关键字作为语法起始，
删除操作以delete关键字作为起始。insert语句包含insert into、values、select as等子句，
update语句包含update、set、where、case when等子句，delete语句包含delete、where等子句。

对于DDL、DQL、DML这三种类型的SQL语句，DDL中的索引创建、字符集选择、字段类型选择、字段长度设置等方面是可以作为SQL优化点的。
DQL中的覆盖索引、联合索引遵循最左前缀原则、基于索引进行关联查询和子查询等方面也可以作为SQL优化点。
DML中的批量增删改、只更新必要字段等方面也能作为SQL优化切入点。

DDL中这些操作其实也是为了DQL操作服务的，索引创建、字符集选择、字段类型选择、字段长度设置等方面的优化都是为了提高查询效率。
当然在DQL操作中字符集选择、字段类型选择、字段长度设置这些是早就确定了的，但对于索引是否应用却由程序开发者决定，
所以应尽可能使用到已有的索引，并且避免索引扫描降级或索引失效。

字符集选择、字段类型选择、字段长度设置是在设计初期就应该充分考量的，实际投入业务需求后，一般不会轻易更改，而SQL需要优化
必然是已经投入使用但SQL性能不够理想，此时这几个方面没法做出什么变动调整，如果是增加字段长度影响不大，如果是变更字段类型
则可能导致原业务处理出现兼容性问题，如果是字符集变更则可能导致编码兼容问题甚至出现乱码现象，字段类型和字符集能不动就不动。

再来看SQL本身，尤其是查询语句，有多种子句可以选用，这些子句有些可以辅助优化。
比如使用limit进行分页，减少单次数据传输体量；
比如只查询索引字段，避免使用`select *`或查询非必要的非索引字段；
比如将复杂查询拆分为多个简单查询；
比如用小表join大表，而不是用大表join小表，尤其是在使用`left join`和`right join`时；
比如使用join代替子查询，尤其是子查询的结果集较大或无法使用索引时；
比如使用`union all`代替union，如果需要去重，可以在`union all`之后通过distinct或`group by`去重；
比如使用with复用中间结果，减少查询次数；


role表基于user_id字段建立普通索引idx_user_id，user表基于id字段建立主键索引primary。
1、语句1：select * from role r join user u on r.user_id =  u.id;
user表有使用索引，role表没有使用到索引。
2、语句2：select * from role r join user u on u.id = r.user_id where r.user_id > 0;
user表和role表都有使用索引。
3、语句3：select * from role r left join user u on u.id = r.user_id;
user表有使用索引，role表没有使用到索引。
4、语句4：select * from role r right join user u on u.id = r.user_id;
user表和role表都没有使用索引。
5、语句5：select * from role r right join user u on u.id = r.user_id where r.user_id > 0;
user表和role表都有使用索引。

不带left或right关键词，只使用join是`inner join`即内联方式，内联方式只返回两表匹配成功的行。
`join`或`inner join`时，MySQL优化器会自动选择小表驱动，即便join左边的是大表，MySQL依然会以join右边的小表作为驱动表。
也就是说`join`或`inner join`时，无论那个表在join前面，都会优化为以两个表中较小的表放在join前面执行查询。

`left join`会先查左侧表的全部行，然后根据左侧表的全部行去匹配右侧表，右侧表没有则对应表字段全部补NULL。
`right join`会先查右侧表的全部行，然后根据右侧表的全部行去匹配左侧表，左侧表没有则对应表字段全部补NULL。

join操作。无论是`inner join`、`left join`还是`right join`，其实都可以拆解成两个表的简单查询。
比如：select * from ta a left join tb b on a.id = b.a_id;
这个语句实际是先查询表ta的所有行，然后根据ta所有行中的id字段值去表tb匹配。因此可以拆解为：
-- 查ta表的所有行，得到id为1和2
select * from ta;
-- 遍历ta的所有行取对应id值1和2，再以id为1和id为2去查tb表，用tb表的a_id字段进行匹配。
select * from tb where a_id = 1;
select * from tb where a_id = 2;

假设ta表查到的有效行（id不为null的）为m，tb表的行数为k。
如果ta表和tb表对于关联字段都没有索引，则查询的时间复杂度为O(m*k)；
如果tb表对于关联字段有索引，则查询的时间复杂度为O(m)，因为tb表对于`a_id = 1`只需要在索引中查1次，这部分耗时O(1)；

由此可见，如果要使用`left join`或`right join`应该把小表放在join前面，以减少外层循环次数。

`join`或`inner join`时可以不指定on子句，`left join`和`right join`则必须要指定on子句。
表ta的行数为m，表tb的行数为n，此时`join`操作如果没有合适的关联条件，则会产生笛卡尔积，返回的数据行数为m*n行。
因此使用join操作时一定要用on子句指定有效的关联条件。

如果必须要使用`left join`，且必须要把大表放join前面，可以通过where子句对关联字段进行必要性筛选，可以让两个表都使用到索引。
比如：select * from ta a left join tb b on a.id = b.a_id where a.id > 0 and b.a_id > 0;

子查询结果集小且只查索引字段时可能会比join查询更高效，而子查询结果集大或表嵌套较深时效率不如join，因为子查询会产生临时表。

`group by`去重受限于sql_model，sql_model有严格模式和非严格模式，默认是严格模式。
在严格模式下，select子句中只能出现`group by`的字段或被聚合函数包裹的字段，比如：select id,MAX(id) from ta group by id;
在非严格模式下，select子句中可以出现任意字段，但非`group by`字段或非聚合处理的字段，所返回的值不确定，会随机选取。
因此通常在实际生产里使用严格模式而不是非严格模式，MySQL可以设置sql_mode=only_full_group_by来使用严格模式。
在严格模式下去重最好是选择distinct方式，比如：select distinct id, name, age from ta。
distinct去重允许select子句出现任何字段，且distinct的效率要高于`group by`，因为`group by`会进行排序，而distinct不需要。
但distinct保留的数据行也是随机的，比如两个重复的id对应的name分别是jack和rose，distinct保留的会是jack或rose。
如果想要自行决定保留哪个name，可以通过`order by`对name字段排序，distinct默认保留第一个name。


从关键字出发研究SQL的实际用法，并对比分析其适用场景与性能高低。