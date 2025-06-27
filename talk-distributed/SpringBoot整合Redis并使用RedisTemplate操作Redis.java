SpringBoot整合Redis有个整合包，是spring-boot-starter-data-redis。
如果要使用RedisTemplate就需要引入这个依赖（以下以Maven依赖为例），例如：
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
data-redis的版本一般从spring-boot-starter-parent继承版本号。

RedisTemplate只是操作Redis的客户端工具，而连接Redis需要引入Redis连接池，
常用的有lettuce、jedisPool（Jedis内置的连接池）、Redisson。
如果不想用Jedis和Redisson，就需要引入独立的连接池，比如lettuce，
使用lettuce连接池需要引入commons-pool2依赖，例如：
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>

application.properties文件中配置如下：
# Redis连接配置开始 ##
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=
spring.redis.database=0
spring.redis.lettuce.pool.enabled=true
# 连接池最大连接数
spring.redis.lettuce.pool.max-active=8
# 连接池最大空闲连接数
spring.redis.lettuce.pool.max-idle=8
# 连接池最小空闲连接数
spring.redis.lettuce.pool.min-idle=0
# 连接池最大阻塞等待时间(负值表示不限制)
spring.redis.lettuce.pool.max-wait=-1ms
# Redis连接配置结束 ##

RedisTemplate的序列化方式默认使用JDK序列化，实际工程中一般使用JSON序列化方式。例如：
使用StringRedisSerializer来序列化和反序列化redis的key值，
使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值。

配置Redis连接池，配置好Redis键和值的序列化方式后，就可以通过@Autowired注解注入使用RedisTemplate，
例如：
@Autowired
private RedisTemplate<String, Object> redisTemplate;
如果还有其他自定义配置，在RedisConfig中，于通过new创建的RedisTemplate实例中添加对应配置。


