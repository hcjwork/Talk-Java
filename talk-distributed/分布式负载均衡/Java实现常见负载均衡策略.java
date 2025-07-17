package com.hcj.example.basedemo;

import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author hcj
 * @description
 * @date 2025-07-17
 */
public class LoadBalanceDemo {
    public static void main(String[] args) {
        // 初始化服务器列表
        List<Server> servers = Arrays.asList(
                new Server("10.0.0.1", 80, 3),
                new Server("10.0.0.2", 80, 2)
        );

        // 使用不同策略
        LoadBalancer roundRobin = new RoundRobinBalancer();
        LoadBalancer ipHash = new IpHashBalancer();
        LoadBalancer leastConn = new LeastConnectionsBalancer();

        // 模拟请求
        String[] clientIps = {"192.168.1.1", "192.168.1.2", "192.168.1.3"};
        for (String ip : clientIps) {
            System.out.println("IP Hash 选择: " + ipHash.selectServer(servers, ip).getIp());
            System.out.println("轮询选择: " + roundRobin.selectServer(servers, ip).getIp());
            System.out.println("最少连接选择: " + leastConn.selectServer(servers, ip).getIp());
        }
    }
}

/**
 * 轮询
 */
class RoundRobinBalancer implements LoadBalancer {
    // 通过原子计数器实现线程安全的顺序选择。
    private AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Server selectServer(List<Server> servers, String clientIp) {
        if (servers.isEmpty()) {
            return null;
        }
        // 计数取模，针对服务实例总数取模
        int index = counter.getAndIncrement() % servers.size();
        return servers.get(index);
    }
}

/**
 * 加权轮询。
 * 最大权重用于初始化轮询权重。
 * 最大公约数用于平滑降低权重，避免低权重服务器饥饿。
 */
class WeightedRoundRobinBalancer implements LoadBalancer {
    private AtomicInteger currentIndex = new AtomicInteger(-1);
    private AtomicInteger currentWeight = new AtomicInteger(0);
    private int maxWeight;
    private int gcdWeight;

    // 初始化计算最大权重和最大公约数
    public WeightedRoundRobinBalancer(List<Server> servers) {
        // 最大权重
        this.maxWeight = calculateMaxWeight(servers);
        // 最大公约数
        this.gcdWeight = calculateGCD(servers);
    }

    private int calculateMaxWeight(List<Server> servers) {
        int maxWeight = servers.stream()
                .mapToInt(Server::getWeight)
                .max()
                .orElse(1); // 默认权重为1
        return maxWeight;
    }

    private int calculateGCD(List<Server> servers) {
        int gcd = servers.get(0).getWeight();
        for (Server server : servers) {
            gcd = gcd(gcd, server.getWeight());
            if (gcd == 1) {
                break; // 提前终止
            }
        }
        return gcd;
    }

    // 欧几里得算法
    int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    @Override
    public Server selectServer(List<Server> servers, String clientIp) {
        while (true) {
            int index = currentIndex.get();
            int weight = currentWeight.get();

            // 权重轮询核心算法
            if (index == -1 || weight == 0) {
                currentWeight.set(maxWeight);
                index = (index + 1) % servers.size();
                currentIndex.set(index);
            }

            Server server = servers.get(index);
            if (server.getWeight() >= weight) {
                currentWeight.addAndGet(-gcdWeight);
                return server;
            } else {
                index = (index + 1) % servers.size();
                currentIndex.set(index);
            }
        }
    }
}

/**
 * IP哈希：相同 IP 的请求始终分配到同一台服务器。
 */
class IpHashBalancer implements LoadBalancer {
    @Override
    public Server selectServer(List<Server> servers, String clientIp) {
        if (servers.isEmpty() || clientIp == null) {
            return null;
        }
        int hash = clientIp.hashCode();
        return servers.get(Math.abs(hash) % servers.size());
    }
}

/**
 * 最少连接数：实际场景需用 `AtomicInteger` 统计连接数，并考虑线程安全。
 */
class LeastConnectionsBalancer implements LoadBalancer {
    @Override
    public Server selectServer(List<Server> servers, String clientIp) {
        return servers.stream()
                .min(Comparator.comparingInt(Server::getActiveConnections))
                .orElse(null);
    }
}

/**
 * 一致性哈希：使用TreeMap实现哈希环。
 */
class ConsistentHashBalancer implements LoadBalancer {
    private TreeMap<Long, Server> hashRing = new TreeMap<>();
    private int virtualNodes = 160; // 虚拟节点数

    public ConsistentHashBalancer(List<Server> servers) {
        for (Server server : servers) {
            for (int i = 0; i < virtualNodes; i++) {
                long hash = hash(server.getIp() + "#" + i);
                hashRing.put(hash, server);
            }
        }
    }

    @Override
    public Server selectServer(List<Server> servers, String clientIp) {
        long hash = hash(clientIp);
        SortedMap<Long, Server> tail = hashRing.tailMap(hash);
        Long nodeHash = tail.isEmpty() ? hashRing.firstKey() : tail.firstKey();
        return hashRing.get(nodeHash);
    }

    private long hash(String key) {
        // 使用 MurmurHash 或 MD5 等算法
        return key.hashCode();
    }
}

/**
 * 监控检查
 */
class HealthCheckWrapper implements LoadBalancer {
    private LoadBalancer delegate;
    private Map<Server, Integer> failCounts = new ConcurrentHashMap<>();

    @Override
    public Server selectServer(List<Server> servers, String clientIp) {
        List<Server> healthyServers = servers.stream()
                .filter(s -> failCounts.getOrDefault(s, 0) < 3) // 最大失败次数
                .collect(Collectors.toList());

        Server selected = delegate.selectServer(healthyServers, clientIp);
        if (selected == null) {
            // 所有服务器不可用时的降级策略
        }
        return selected;
    }

    // 模拟记录失败
    public void markFailure(Server server) {
        failCounts.merge(server, 1, Integer::sum);
    }
}


@Data
class Server {
    private String ip;
    private int port;
    private int weight; // 权重（仅加权策略需要）
    private int activeConnections; // 当前连接数（最少连接策略需要）

    public Server(String ip, int port, int weight) {
        this.ip = ip;
        this.port = port;
        this.weight = weight;
    }
}

interface LoadBalancer {
    Server selectServer(List<Server> servers, String clientIp);
}


