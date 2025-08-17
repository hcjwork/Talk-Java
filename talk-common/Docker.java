1、Docker的基本原理
Docker是一种容器打包技术，是容器编排的基础，利用虚拟内存将服务隔离部署和运维，就相当于是一个个独立的容器，但这些容器是基于物理机器的。
Docker各个容器隔离，端口、配置等信息相互独立，但容器内所映射的容器外的端口、路径等信息是不可冲突的。
Docker容器外的空间是机器空间，是每个Docker容器所共享的，也是其他程序所共享的。
Docker各个容器间不能直接访问交互，而是需要通过容器外的映射才能透传到容器内部进行交互。

Docker架构主要由三部分组成：Docker Daemon、Docker Client、Registry。
Docker Daemon：Docker守护进程，管理Docker镜像和Docker容器。
Docker Client：Docker客户端，可通过命令行与Daemon交互。
Registry：Docker镜像仓库，如Docker Hub、私有Harbor。

Docker的两个核心概念：镜像、容器。
镜像：包含应用代码和运行环境的只读模板。
容器：镜像的运行时实例，可存储变化数据。

镜像就是打包好的代码和各种配置，容器是镜像运行实例，镜像运行后不可随意更新数据。

Docker的实现与操作系统内核技术的支撑脱不开关系，比如进程、网络、文件系统等资源的隔离，CPU、内存、磁盘等资源的使用限制，
文件系统的分层技术。

2、Docker常用命令
构建镜像：`docker build -t 镜像名称:版本号`。例如：`docker build -t myapp:1.0 .`。
拉取镜像：`docker pull 镜像名称:版本号`。例如：`docker pull nginx:latest`。
查看本地镜像：`docker images`。
启动容器：`docker run`。例如后台运行容器：`docker run -d -p 8080:8080 --name myapp myapp:1.0`。
查看所有容器（包含已停止的）：`docker ps -a`。
停止容器：`docker stop `。例如：`docker stop myapp`。
重启容器：`docker restart`。例如：`docker restart myapp`。
实时查看容器日志：`docker logs -f 容器ID`。例如：`docker logs -f myapp`，`myapp`是容器ID也是镜像名称。
进入容器终端：`docker exec -it`。例如：`docker exec -it myapp /bin/bash`。
强制删除容器：`docker rm -f`。例如：`docker rm -f myapp`。
删除镜像：`docker rmi`。例如：`docker rmi myapp:1.0`。
清理无用镜像和容器：`docker system prune`。

3、Docker高效命令
查看容器资源占用：`docker stats --no-stream`。
停止所有运行中的容器：`docker stop $(docker ps -q)`。
删除所有已停止的容器：`docker rm $(docker ps -aq)`。
从容器外拷贝文件到容器中：`docker cp 容器外文件 容器ID:容器内目录`，例如`docker cp local_file.txt myapp:/app/`。
查看容器启动命令：`docker inspect --format='{{.Config.Cmd}}' 容器ID`。
查看容器端口映射：`docker port 容器ID`。

注：容器ID通常为镜像名称。

4、Dockerfile编写
一个完整的Dockerfile主要基于以下部分构成（以下格式示例中[]表示可有可无，<>表示必须要有）：
指定基础镜像：使用`FROM`关键字。格式：`FROM <image>[:<tag>] [AS <name>]`。
定义元数据：使用`LABEL`、`MAINTAINER`，其中`MAINTAINER`在高版本中被逐渐弃用。格式：`LABEL <key>=<value> <key>=<value> ...`。
运行命令：使用`RUN`。例如：`RUN pwd`。
复制文件：使用`COPY`、`ADD`，其中`ADD`是高级复制，可以解压压缩文件、从URL直接下载文件。格式：`COPY [--chown=<user>:<group>] <src>... <dest>`，`ADD [--chown=<user>:<group>] <src>... <dest>`。
声明端口：使用`EXPOSE`。格式：`EXPOSE <port> [<port>/<protocol>...]`。
指定工作目录：使用`WORKDIR`。例如：`WORKDIR /app`。
设置环境变量：使用`ENV`。格式：`ENV <key>=<value> ...`。
容器启动命令：使用`CMD` 或 `ENTRYPOINT`，`CMD`可以作为`ENTRYPOINT`的默认参数。

完整Dockerfile示例：
#### 构建示例
FROM maven:3.8.4-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/myapp.jar ./app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m -Xms256m"
USER 1000
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
#### 构建结束

5、Docker容器内外的端口和文件等映射
端口映射：
要想在Docker外访问到Docker容器内部的服务，则需要进行端口映射，将容器外端口与容器内端口进行绑定，这样外部访问时访问
容器外端口即可映射到Docker容器内部对应服务上。如果不进行端口映射，想访问Docker容器内部服务，则可以通过以下方式访问。
- 容器运行时使用host网络模式，也就是共享主机网络，直接通过主机ip+容器端口进行访问。
  例如：`docker run --network=host <container-name>`。如果容器内java服务的端口为8080，主机ip为10.19.105.1，
  则curl命令可以为`curl http://10.19.105.1/8080`。
- 通过其他的容器代理，比如nginx。
- 通过`docker exec -it`命令直接进入容器内部访问，例如：`docker exec -it <container-name> curl localhost:8080`。

文件映射：文件映射并非必须，但通过映射可以灵活启用容器外部配置。如果不映射则需要在构建镜像时拷贝到容器工作目录中，或者是
通过环境变量传递，或者使用Docker Config/Secret传递。

端口映射需要通过容器启动命令指定容器内外端口，命令格式为：`docker run -p 主机端口:容器端口 -d 镜像名`。
假设通过Docker构建Java服务镜像并运行，通过命令`docker run -p 8888:8080 -d java-image`指定java服务docker容器
内外端口分别为`8888`和`8080`，docker容器所在物理机器ip为10.19.105.1，
通过curl或postman访问java服务的/user/getRoles接口，url全路径则为`http://10.19.105.1/8888/user/getRoles`。

文件映射也可以通过`docker run命令`指定，例如：`docker run -v /主机路径:/容器路径 -p 8888:8080 -d java-image`。

6、Docker的默认存储位置
1）Docker拉取和构建的镜像默认存储位置
Linux系统：`/var/lib/docker`
  - 镜像存储在：`/var/lib/docker/image/[存储驱动]`
  - 层数据在：`/var/lib/docker/[存储驱动]`
Windows系统：`C:\ProgramData\docker`

通过修改daemon.json文件或使用符号链接（适用于已有镜像迁移）可以修改镜像存储位置。

2）Docker容器日志文化默认存储位置
Linux系统：`/var/lib/docker/containers/<container-id>/<container-id>-json.log`
Windows系统：`C:\ProgramData\docker\containers\<container-id>\<container-id>-json.log`

查看容器日志所在位置：`docker inspect --format='{{.LogPath}}' <container-name>`。
通过容器运行命令可以限制容器日志大小，例如：
`docker run --log-driver=json-file --log-opt max-size=10m --log-opt max-file=3 <container-name>`

