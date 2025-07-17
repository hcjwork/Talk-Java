一、Nginx服务的安装和部署
官网下载地址：https://nginx.org/en/download.html

下载最新的稳定版zip就行，加压后即可启动运行。

二、Nginx服务常用命令
Nginx支持配置修改后无感刷新，不需要重启Nginx即可生效。
1、nginx -s signal
nginx是指的nginx.exe程序，-s指的是-security安全执行，signal指的要做的动作。
实际执行"nginx -s"命令时，要把signal替换为具体的动作词语。
"nginx -s"命令支持的动作有：
stop — 快速关闭
quit — 优雅关闭
reload — 重新加载配置文件
reopen — 重新打开日志文件

当Nginx的配置做了修改后，可以通过执行“nginx -s reload”命令重新加载配置，而不用重启Nginx服务。


三、Nginx配置文件
Nginx的配置文件主要就是conf/nginx.conf文件，Linux版的位置或命名可能有区别，但相差不大。
Nginx的配置文件以#作为注释符号。
Nginx配置文件由不同的模块组成，每个模块又由指令所控制。指令分为简单指令和块指令。
1、简单指令（simple directives）和块指令（block directives）
简单指令就是名称和参数组成，用空格分隔，以;结尾。
例如：
worker_processes  1;
这是一行简单指令，位于顶层结构，不在嵌套结构中。

块指令就是指用{}括起来的指令，当然{前面要有指令名称。
块指令的{}中可以包含有简单指令也可以包含其他的块指令。例如：
events {
    worker_connections  1024;
}
这一段指令包含有{}，{前面的events关键词就是指令名称。


2、核心模块解析
Nginx配置文件的核心模块有：events、http、server、location、upstream。
其中events和http都是顶级模块，server和upstream是http内的子模块，location是server的子模块。
location模块只能用在server模块内，server和upstream模块只能用在http模块内。
例如：
events {
}
http {
    server {
        location {
        }
    }
}

events模块主要声明一些Nginx工作线程相关的参数，
http模块用以解析http请求；
server模块用于监听目标服务；
location模块用于url匹配和路由转发。

配置文件可以包含多个server块，一个http块中可以有多个server块，用以监听多个服务器的请求。
多个server块通过监听的端口和服务名称进行区分。server块内通过location块监听指定规则的请求url。
例如：
http {
    server {
		listen 8080;
		server_name localhost;
        location / {
            root   html;
            index  index.html index.htm;
        }
    }
    server {
        listen 8081;
        server_name 192.168.0.103;
        location / {
            root   html;
            index  index.html index.htm;
        }
    }
}

listen指令后面接端口号，server_name指令后面接服务器的ip。
location指令后跟请求url（去除协议、ip和端口），如果是“/”表示只要http请求地址中是以这个端口和ip开头的都会被
这个location拦截。
location也可以配置多个，如果有多个则先以请求uri匹配规则更短的那个优先匹配。
例如：
http {
    server {
		listen 8080;
		server_name localhost;
        location / {
            root   html;
            index  index.html index.htm;
        }
        location /custom {
            root   /data;
        }
    }
}
一个location的匹配uri路径只需要/就行，一个location的匹配uri路径要以/custom开头才行，
请求uri的匹配和路由会先走“location /”的，如果“location /”走不通才往下走“location /custom”的。
所以实际工程设计里，“location /”一般不会添加或者一般放在server块的最末尾作为兜底的location配置。

四、配置示例
1、events模块
events {
    worker_connections  8192;  # 单个 worker 进程可同时处理的最大连接数（含客户端+代理连接）
    use epoll;          # 指定事件驱动模型（epoll/kqueue/select等）
    multi_accept on;    # 是否一次性接受所有新连接（on）或逐个接受（off），高并发时建议 on
    accept_mutex off;   # 启用连接互斥锁，避免惊群问题（多个worker争抢连接）。内核≥3.9时关闭
}

2、http模块
http {
    default_type  application/octet-stream;
    sendfile      on; # 启用零拷贝文件传输（提升静态文件性能）
    tcp_nopush    on; # 合并数据包减少网络调用，仅在 `sendfile on` 时有效

    # 日志格式
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';
    # nginx访问日志文件
    access_log /var/log/nginx/access.log main gzip=4 flush=1m;
    # nginx错误日志文件
    error_log  /var/log/nginx/error.log warn;

    # 负载均衡
    upstream backend {
        ...
    }
    # 虚拟主机
    server {
        ...
    }
}

3、upstream模块
upstream backend {
    # 默认轮询策略
    server 10.0.0.1:8080 weight=3;
    server 10.0.0.2:8080;
    server 10.0.0.3:8080 backup;  # 备用服务器
}
支持多种负载均衡策略，比如：轮询、ip哈希、最少连接数、自定义哈希。

4、server模块
server {
    listen 8080; #服务器端口
    server_name example.com www.example.com; # 服务器名称（IP或域名）

    # 静态资源缓存
    location ~* \.(jpg|css|js)$ {
        expires 365d;
        access_log off;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://backend; # backend是upstream块的名称，表示应用负载均衡
        proxy_set_header Host $host; # 添加请求头“Host”
        proxy_set_header X-Real-IP $remote_addr; # 添加请求头“X-Real-IP”
    }

    # 禁止访问敏感文件
    location ~ /\.(env|git) {
        deny all;
    }

    # 错误页面
    error_page 404 /404.html;
    error_page 500 502 503 504 /50x.html;
}

5、location模块
server {
    listen 80;
    server_name example.com;

    # 静态文件服务（缓存优化）
    location ~* \.(jpg|css|js)$ {
        root /var/www/static;
        expires 365d;
        access_log off;
        add_header Cache-Control "public";
    }

    # API反向代理（负载均衡）
    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_connect_timeout 3s;
    }
}

五、调优关键指令
worker_processes：
顶级指令。工作进程数。可以设置具体的值，也可以设置为auto，设置为auto时自动匹配CPU核心数。

worker_connections：
events块中的指令。单个worker进程可同时处理的最大连接数（含客户端+代理连接），高并发场景需调高。默认512。

multi_accept：
events块中的指令。是否一次性接受所有新连接（on）或逐个接受（off）。高并发时建议设置为on。默认off。

worker_aio_requests：
events块中的指令。单个worker的异步I/O操作最大数量（需启用 AIO）。文件异步传输时建议调大。默认32。

sendfile：
http块中的指令。启用零拷贝文件传输（提升静态文件性能），建议开启。

tcp_nopush：
http块中的指令。仅在 `sendfile on` 时有效，合并数据包减少网络调用。

gzip：
为on时启用响应压缩。

client_max_body_size：
客户端请求体最大大小（上传文件限制），有文件上传下载、批量导入导出需求的建议调大。

client_body_buffer_size：
http块中的指令。请求体缓冲区大小，如果请求体参数较多，建议调大。

open_file_cache：
http块中的指令。缓存文件描述符、元数据等，减少磁盘 I/O。

limit_req_zone：
http块中的指令。请求限流（需配合 `limit_req` 使用）。

deny/allow：
IP访问控制，IP黑白名单控制。

六、基本原理
1、事件驱动模型
基于I/O多路复用，以非阻塞方式处理请求。使用事件驱动模型，例如epoll/kqueue/select等。

2、多进程模型
Nginx启动时运行Master进程和Worker进程。Master进程管理Worker进程，Worker进程处理具体请求。

3、零拷贝技术
通过sendfile指令直接在内核空间传输文件，不需要在用户态拷贝后才传输给内核空间。

4、负载均衡
通过upstream块指令能支持多种负载均衡策略，例如：轮询、加权轮询、IP哈希、最少连接数、一致性哈希、响应时间优先。
默认是采用的轮询策略。

轮询策略（Round Robin）：按请求的顺序依次分配请求给每个后端服务器。
配置示例：（其中的backend是自定义的upstream块名称，以下其他示例同理）
upstream backend {
    server 192.0.0.1:80;
    server 192.0.0.2:80;
}

加权轮询（Weighted Round Robin）：根据服务器权重分配流量，权重越高接收的请求越多。适用于后端服务器性能不均（如CPU、内存有较大差异）的场景。
配置示例：（前面3个请求分给192.0.0.1这台服务器，接着2个请求分给192.0.0.2这台服务器，接下来3个又分给192.0.0.1服务器）
upstream backend {
    server 192.0.0.1:80 weight=3;  # 3/5 的流量
    server 192.0.0.2:80 weight=2;  # 2/5 的流量
}

IP 哈希（IP Hash）：根据客户端 IP 的哈希值固定分配到同一台后端服务器。能解决会话保持（Session Persistence）问题。
配置示例：（后端服务器数量发送变化时，会导致哈希重新分布）
upstream backend {
    ip_hash;
    server 192.0.0.1:80;
    server 192.0.0.2:80;
}


一致性哈希策略：依赖第三方模块，通过哈希环（如 $uri 或 $args）固定请求到特定服务器，减少增减节点时的数据迁移。
配置示例：
upstream backend {
    consistent_hash $request_uri;
    server 192.0.0.1:8080;
    server 192.0.0.2:8080;
}

响应时间优先策略仅Nginx-Plus支持，综合响应时间和最少连接数，选择最优服务器。
配置示例：
upstream backend {
    least_time header;  # 根据响应头计算时间
    server 192.0.0.1:8080;
    server 192.0.0.2:8080;
}

七、Nginx的常见应用场景
1、反向代理和负载均衡
主要应用场景，将请求分发到多个后端应用服务器。Nginx支持多种负载均衡策略。

2、静态资源服务
托管 HTML、CSS、JS、图片等静态文件。
基于零拷贝技术（sendfile指令）直接内核态传输文件，减少用户态拷贝开销。
基于高效缓存机制（open_file_cache指令）缓存文件描述符，减少磁盘 I/O。

3、API网关
可实现路由转发、鉴权、限流、日志聚合等能力。
结合Lua脚本（OpenResty）实现动态路由和JWT验证。
使用限流模块（limit_req_zone）结合limit_req指令防止API请求过载。

4、其他应用场景
动态内容缓存：缓存后端动态生成的页面（如商品详情页）。
SSL/TLS 终端：集中管理 HTTPS 证书，卸载后端服务器的加密开销。
流媒体服务：视频点播（HLS/DASH）或实时流（RTMP）。
微服务入口：作为 Kubernetes Ingress 或服务网格入口。