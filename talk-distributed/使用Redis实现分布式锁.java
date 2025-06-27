Redis可以用来实现分布式锁，是因为有一种setnx命令可以保证set操作的互斥性。
setnx是set if not exists的简写，意思是只有在键不存在的时候才设置对应的值。
但setnx本身并不能直接指定键的过期时间，只能设置值，所以需要结合expire命令。
另一种同样具备set if not exists功能的是set key value nx命令，
这个set key value nx命令可以同时指定key的过期时间，支持ex和px两种时间单位，ex后接秒数，px后接毫秒数。
是Redis2.6.12版本开始引入的。
例如：
set key value nx ex 10，当前仅当key不存在时才进行值设置，设值时指定key的过期时间为10s。
set key value nx px 5000，当前仅当key不存在时才进行值设置，设值时指定key的过期时间为5000ms，即5s。
可以用ttl命令查看key的剩余有效时间，例如：ttl key。
实际应用时更推荐set nx ex/px命令。

为什么要在设置键值的同时设置键的过期时间？
一方面是为了减少其他线程等待锁的开销，一方面是增加分布式锁管理的灵活性。
如果不同时设置键的过期时间，那就需要持有锁的线程在完成任务后主动删除这个键才行。
但这样会有点问题，如果持有锁的线程执行的是非常耗时的任务，其他线程可能长时间处于饥饿状态，
白白浪费了等待的性能开销，系统的并发能力也会下降。
另外如果程序开发者忘记删除键，就可能导致其他线程永远都获取不到锁而进入死锁状态。
所以从锁持有和锁等待上综合考虑，不仅锁持有要设置一个有限时间，锁等待也应该设置一个有限时间，
避免长时间获取不到锁而耽误其他任务的执行。

那么新的问题又产生了，如果锁持有有时间限制，持有锁的线程没有完成自己的任务那该怎么办？
这就需要为锁设置一个看门狗机制的守护线程，
在锁将要过期时（比如剩余5s有效时间时）检查持有锁的线程是否被中断、是否已经退出同步代码范围，
如果持有锁的线程状态正常并且还没有退出同步代码块说明任务没有执行完，就需要为锁延续有效时间。
延迟多久取决于具体需求和实现。

如果持有锁的线程在锁持续时间内提前完成了任务，我们可以主动删除锁对应的键以释放锁，这样能提高系统的并发能力。
但是如果持有锁的线程正要删除这个锁键，而锁键恰好已经过期并且被其他线程设置了新的值，这个时候执行键删除操作，
会把其他线程线程持有的锁删掉，最终导致持有锁的线程无法在并发安全的换下执行同步代码，可能出现不合预期的结果，
所以就需要线程获取锁时为锁键设置一个唯一标识的值以增加区分度，同时持有锁的线程在删除锁键时要先确认时键的值是自己设置的哪一个，
确认无误后再删除键。

如何为锁键设置唯一值？
我们通常考虑使用UUID再拼接业务ID，整体作为锁键的值，如果还要增加并发支撑度，可以再拼一个随机数字串或时间戳。
例如：String lockValue = UUID.randomUUID().toString();
一般UUID串就基本满足要求了，为了增加业务可读性，可以拼接上业务ID，比如：
String lockValue = UUID.randomUUID().toString() + ":" + orderId;

如何保证锁键删除无误？
确认键的值是之前设置的，没有问题后再删除，这部分操作我们通常考虑使用lua脚本将确认和删除两个步骤合并为一个原子性操作。
例如：
String script =
    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
    "   return redis.call('del', KEYS[1]) " +
    "else " +
    "   return 0 " +
    "end";
RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
redisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockValue);

如何为锁延续时间？
开启一个守护线程或者说监察线程吧，定时检查目标键的剩余时间，如果低于某个阈值就进行续期。

守护线程如何知道持有锁的线程还在执行？
设置一个全局变量locked，用volatile保证修改可见性，表示锁定状态，默认为false。
当线程获取锁成功，就将locked修改为true。等持有锁的线程已经完成同步代码的执行，就会将locked修改为false。
而守护线程监听locked变量的状态，如果是true说明还在执行中，就可以检查锁的剩余时间进行续期，或者干脆直接续期。
但每次续期的时间不能过长，一般设置为原始有效时间的三分之一。
例如：
private void startWatchdog() {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(() -> {
        if (locked) {
            redisTemplate.expire(lockKey, expireTime, TimeUnit.MILLISECONDS);
        }
    }, expireTime / 3, expireTime / 3, TimeUnit.MILLISECONDS);
}

