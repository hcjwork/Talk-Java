Sa-Token是一个轻量级的支持用户登录、鉴权和会话管理的权限认证框架，开箱即用，使用起来方便快捷，
没有沉重的代码配置，直接用StpUtil工具类操作即可。

一、Maven依赖
1、sa-token依赖
可以使用sa-token原生依赖，例如：
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-core</artifactId>
    <version>1.44.0</version>
</dependency>

也可以使用sa-token和spring-boot的整合依赖，例如：
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot-starter</artifactId>
    <version>1.39.0</version>
</dependency>

sa-token与spring-boot的版本要兼容，对于SpringBoot3.x版本的，要使用sa-token-spring-boot3-starter依赖，
例如：
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot3-starter</artifactId>
    <version>1.39.0</version>
</dependency>

对于SpringBoot3.x版本，需要比java8更高的java版本才能兼容。因此SpringBoot和Java版本要综合权衡。
只要是sa-token-spring-boot-starter依赖，不管哪个版本都是兼容SpringBoot2.x和Java8的。
在能兼容SpringBoot2.x和Java8的前提下，尽量选择最新的稳定版本，能使用最新的特性。

如果启动是出现了类似下述的报错：
Caused by: org.springframework.beans.BeanInstantiationException:
Failed to instantiate [cn.dev33.satoken.spring.SaTokenContextRegister]:
Constructor threw exception; nested exception is java.lang.NoSuchFieldError: instance
可能sa-token和spring-boot的版本不兼容，也可能是sa-token-dao-redis依赖的问题。

2、sa-token整合redis的依赖
利用redis实现分布式的跨会话管理。例如：
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-dao-redis</artifactId>
    <version>1.34.0</version>
</dependency>

注意：如果使用的是sa-token-dao-redis，目前最高版本是1.34.0。Sa-Token核心依赖必须要与sa-token-dao-redis版本匹配，
如果版本高出sa-token-dao-redis，可能导致启动失败。
如果Sa-Token核心依赖用的是sa-token-spring-boot-starter，版本最好也采用1.34.0。
如果想用更高的sa-token-spring-boot-starter版本，那么就不要用sa-token-dao-redis依赖，考虑用更新的Redis整合依赖，
例如：
sa-token-redis-jackson、
sa-token-redis-template、
sa-token-redis-template-jdk-serializer

或使用结合了Redis序列化方式的整合依赖，例如：
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-redis-jackson</artifactId>
    <version>1.44.0</version>
</dependency>

3、其他基础依赖
commons-pool2：redis的lettuce连接池。
spring-boot-starter-parent
spring-boot-starter-web

二、SpringBoot配置
通过sa-token属性进行配置，包含token有效期、是否允许并发登录、token生成格式等关键配置。
yml示例如下：
# Sa-Token配置
sa-token:
  token-name: satoken           # Token名称
  timeout: 86400                # Token有效期（秒，默认30天）
  activity-timeout: -1          # 无操作时Token有效期（-1表示不限制）
  is-concurrent: true           # 允许并发登录
  is-share: true                # 共享Cookie
  token-style: uuid             # Token生成风格（可选：uuid、simple-uuid、random-64）
  # Redis配置（若需分布式会话）
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0

三、使用示例
通过StpUtil工具类的静态方法结合Sa-Token的关键注解执行核心操作。
1、StpUtil工具类的常用犯方法
StpUtil.login()：用户登录。
StpUtil.logout()：用户登出。
StpUtil.getTokenValue()：获取当前用户的token。
StpUtil.getSession()：获取当前登录用户的会话信息。

2、Sa-Token权限注解
@SaCheckLogin：校验是否登录。
@SaCheckRole("admin")：校验当前用户是否是admin角色。
@SaCheckPermission("user.add")：校验当前用户是否有“user.add”权限。
@SaCheckSafe()：校验是否完成二级认证。

@SaCheckBasic(account = "sa:123456")：HTTP认证校验。
@SaCheckHttpBasic(account = "sa:123456")：HTTP认证校验。
如果sa-token与redis的整合依赖用的是sa-token-redis-jackson，HTTP认证校验是用@SaCheckHttpBasic注解。
如果sa-token与redis的整合依赖用的是sa-token-dao-redis，HTTP认证校验是用@SaCheckBasic注解。

3、SaTokenDao实现类
如果用的是sa-token-redis-jackson依赖，SaTokenDao接口的实现类里整合了Redis的是SaTokenDaoForRedisTemplate类。
如果用的是sa-token-dao-redis依赖，SaTokenDao接口的实现类里整合了Redis的是SaTokenDaoRedis类。

如果想自定义序列化方式，可以手动创建Redis操作工具类，再赋值给SaTokenDao接口实现类的属性。
使用Redis自定义序列化方式时，要注意所有地方对于Redis的键和值的操作的序列化方式都要一致，读写的序列化方式也要一致。
否则可能会出现序列化和反序列化失败而导致Redis读写操作不可用。

4、接口url示例
以curl命令为例。
curl http://localhost:8091/user/doLogin -d "username=zhang&password=123456"
curl http://localhost:8091/session/set -d "key=name&value=lisi" -H "satoken: 490e4c94-6056-4239-ae43-5a8769cef0d4"
curl -X GET http://localhost:8091/auth/checkPermission -H "satoken: 490e4c94-6056-4239-ae43-5a8769cef0d4"

如果实在Windows的cmd窗口执行，包含&的字符串要用双引号包裹，否则会解析出错。


四、核心原理

