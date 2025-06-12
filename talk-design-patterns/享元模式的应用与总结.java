享元模式，Flyweight Pattern，看名称就知道应该与数据共享有关联。
享元模式其实是通过复用对象以减少对象创建和销毁所带来的资源开销，这里的对象可以是Java对象也可以基本类型的数据，
总的来说就是对数据的共享复用，常见的如缓存集合、线程池、连接池、对象池、常量池等等，都是享元模式的经典实现。

享元模式先尝试复用现成的可用对象，如果没有可用的则新建对象使用，新建的对象是否加入对象池取决于实际需求。
这个复用的对象称为享元对象，存储和管理享元对象的被称为享元工厂。
享元对象包含外部和内部两种状态（也就是两种属性），内部状态就是要复用的核心，外部状态一般用以标识对象是否处于使用中。

一般为了避免对象池中对象过多，会要求对象池有一个固定上限，因为当池中对象过多时不仅是占用内存过多，匹配查找时所用时间也会越多。

光看设计模式的概念好像没什么头绪，但实际这些经典模式早已应用于Java的日常开发中了，尤其是Java的核心组件和核心框架。

享元模式能减少内存消耗、提高系统效率，但与之而来的是设计和实现的复杂性，包括对象的预创建、分派、回收等，还有
并发安全和资源释等方面也需要充分考虑。

下面通过享元模式实现一个简单的数据库连接池：
DBConnection是享元对象，ConnectionPool是统一管理享元对象创建和复用的享元工厂。
示例伪代码：
public class DBConnection {
    private Connection connection; // 内部状态
    private boolean inUse; // 外部状态
    public DBConnection(String url, String user, String pwd) throws SQLException {
        this.connection = DriverManager.getConnection(url, user, pwd);
        this.inUse = false;
    }
    public Connection getConnection() {
        return connection;
    }
    public boolean isInUse() {
        return inUse;
    }
    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
    public void close() throws SQLException {
        connection.close();
    }
}

@Slf4j
public class ConnectionPool {
    private static final int DEFAULT_POOL_SIZE = 10;
    private volatile int size;
    private final Queue<DBConnection> availableConnections = new ConcurrentLinkedQueue<>();
    private final Set<DBConnection> usedConnections = Collections.synchronizedSet(new HashSet<>());
    private final String url;
    private final String user;
    private final String pwd;
    public ConnectionPool(String url, String user, String pwd) {
        // 如果没有传递线程池大小，就使用默认大小
        this(url, user, pwd, DEFAULT_POOL_SIZE);
    }
    public ConnectionPool(String url, String user, String pwd, int poolSize) {
        this.url = url;
        this.user = user;
        this.pwd = pwd;
        this.size = poolSize;
        initializePool(poolSize);
    }
    private void initializePool(int poolSize) {
        if (poolSize > 0) {
            for (int i = 0; i < poolSize; i++) {
                try {
                    availableConnections.offer(new DBConnection(url, user, pwd));
                } catch (SQLException e) {
                    log.error("数据库连接创建失败，", e);
                }
            }
        }
    }
    public synchronized Connection getConnection() throws SQLException {
        DBConnection dbConnection = availableConnections.poll();

        // 如果池中没有可用的连接，则新建一个
        if (dbConnection == null) {
            log.info("新建一个数据库连接");
            dbConnection = new DBConnection(url, user, pwd);
        }

        // 设置为使用中
        dbConnection.setInUse(true);
        usedConnections.add(dbConnection);

        return dbConnection.getConnection();
    }
    public synchronized void releaseConnection(Connection connection) throws SQLException{
        // 在已使用连接的集合中查找
        for (DBConnection dbConnection : usedConnections) {
            if (dbConnection.getConnection() == connection) {
                dbConnection.setInUse(false);
                // 从已使用集合中移除
                usedConnections.remove(dbConnection);

                if (availableConnections.size() < size) {
                    // 如果连接池里的可用连接数小于原本的数量，回收连接到连接池中
                    availableConnections.offer(dbConnection);
                } else {
                    // 如果连接池已满，直接关闭该连接
                    dbConnection.close();
                }
                return;
            }
        }

        // 如果不是连接池的，直接关闭
        log.info("关闭动态创建的数据库连接");
        connection.close();
    }
}


数据库连接池是享元模式的经典应用，用于存储和管理享元对象的容器需要是并发安全的，
而对于享元对象的分派、回收、释放等操作也需要保证并发安全。
如果缓存池中所有的享元对象都在工作状态中，对于新的请求可以采用某种拒绝策略，也可以为新请求新建一个享元对象，
更详细安全的设计可以参考线程池的核心参数：
核心线程数、最大线程数、非核心线程空闲存活时间、空闲存活时间单位、阻塞队列、线程工厂、拒绝策略。
实际生产中一般使用功能更丰富的成熟框架，例如数据库连接池可以使用Druid、HikariCP等。

但理解享元模式的基本实现与应用，能够加深对象与资源复用的认知，以及对程序性能、架构设计方面的指导。

从享元模式看模块设计、架构设计：
享元模式最关键的特性就是复用，复用在很多方面是降低开销和提升效率的有效方式。
例如一些通用处理、工具类、枚举类、常量等便可以抽取出来复用，包括Java的封装、继承、多态也都体现了复用性。
在微服务工程中，会将不同模块共用的东西放在common模块里，还有对jar包的依赖与继承，都在common模块的依赖配置文件中定义。
而在系统架构层面，中间件便是对微服务的共同需求的抽取复用，比如Nginx转发、API网关权限校验与路由、Redis缓存数据、数据库持久化数据等，
是各个微服务都可以用到的。
以前在程序开发圈子流传过一句话：“解决不了的问题加一层就好了，如果还解决不了就再加一层”。
这句话虽然对程序问题的解决方案说得太笼统虚幻，但其实也是一种复用的思想，利用第三方或中间层作为解构的中转站，
从旁观者的角度来思考服务的痛点，每个服务的痛点（或说瓶颈）基本都大同小异，而这个大同部分就可以通过中间层来提供支持并复用能力。
更具体地说，多个微服务有多种需求，这些需求差别不大，就可以将多种需求综合成一个通用需求，这样通过一次建构就能支持多处应用。
但需要注意的是，复用性的提高往往伴随着抽象复杂度的提高，而针对小异部分是复用模块支持扩展还是在各自小异部分个性化实现，
就看实际怎么抉择，总体说来个性化实现更好调控，因为复用范围越广或应用得越多，就越难以预估修改复用部分所带来的影响。