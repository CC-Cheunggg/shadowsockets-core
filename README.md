# shadowsocks-core shadowsocks（java实现）

> 维护了java版的 shadowsocks 实现

[![]()]()

## 使用指南

### 项目使用条件

```
在支持BBR加速的vps中的Linux系统中安装JDK 8.0+ 、安装BBR。
```

### 安装

```
1.在发行页下载最新的 shadowsockets-server-runtime 和 shadowsocks-core-X.X.X.Final.jar 到代理服务器中。
2.解压 shadowsockets-server-runtime
```

### 使用示例

```
1.在 shadowsockets-server-runtime/conf 编辑 config.xml 其中 ipAddr 为服务器本身的ip，大多数情况下默认即可，localIpAddr 
  和 track 参数为保留参数，serverPort为 shadowsocks 客户端访问端口，method 则为客户端加密的方式(推荐使用 chacha20-ietf-
  poly1305)，password则为客户端输入的密码。
2.编辑完 config.xml 后，执行 shadowsockets-server-runtime/bin 下的 startup.sh 脚本，观察启动日志。
3.正常启动后 Ctrl + C 终止进程，执行 ss-server-background.sh 脚本让应用后台运行。
```

## 部署方法

```
1.把下载好的 shadowsocks-core-X.X.X.Final.jar 移动到解压好的 shadowsockets-server-runtime/lib 中
2.赋予shadowsockets-server-runtime目录可读可写权限
3.赋予bin目录下所有文件可执行权限
4.注意检查防火墙是否开放指定端口
```

## 贡献指南

请阅读 [shadowsocks.org](https://shadowsocks.org/) 了解如何向这个项目贡献代码

## 版本历史

* 1.7.0.Final
    * 修复小火箭不能连接的缺陷
    * 新增了Chacha20Poly1305加密方式
* 1.6.0.Final
    * 增加AEAD加密 ：aes-128-gcm
* 1.5.0.Final
    * 新增了线程池可在配置文件中配置的功能

## 关于作者

* **RPCheung** - *Initial work* - [RPCheung](https://github.com/RPCheung)

## 授权协议

这个项目遵循 Apache 协议， 请点击 [LICENSE](https://github.com/RPCheung/shadowsockets-core/blob/master/LICENSE) 了解更多细节。