# Cookie Cloud Burp 插件
## 项目介绍
Cookie Cloud Burp 是一个 Burp Suite 插件，用于管理和同步 Cookie 数据。它允许用户在不同设备间安全地共享和同步 Cookie，简化了多设备测试和协作渗透测试的流程。

## 主要功能
- Cookie 自动注入：自动将云端或本地缓存的 Cookie 注入到 HTTP 请求中
- Header 自动注入：自动将云端或本地缓存的 Header 注入到 HTTP 请求中
- 模式匹配：支持多种获取模式
  - Cookie 获取模式：
    - !{name} : 强制从远程获取最新的 Cookie
    - ${name} : 从本地缓存获取 Cookie
  - Header 获取模式（不区分大小写）：
    - !{peername|header-name} : 强制从远程获取最新的 `Header[header-name]`
    - ${peername|header-name} : 从本地缓存获取 `Header[header-name]`
    - !{peername} : 使用当前头名称，强制从远程获取最新的 Header
    - ${peername} : 使用当前头名称，从本地缓存获取 Header
- 密钥管理：支持本地密钥和对端密钥的管理，确保数据的安全传输
- 工具选择：可以选择在哪些 Burp 工具中启用插件功能（Proxy、Repeater、Intruder 等）
- 缓存控制：可配置数据缓存时间，平衡性能和实时性

## 界面组件
插件界面分为以下几个主要部分：

1. 基本设置面板 ：控制插件的全局开关、缓存时间和工具选择
2. 密钥管理 ：
   - 本地密钥管理 ：生成和管理用于加密的本地密钥对
   - 对端密钥管理 ：添加、编辑和删除对端公钥，用于与其他用户共享 Cookie
3. 缓存管理 ：管理本地缓存的 Cookie 数据

## 使用方法
1. 在 Burp Suite 中加载插件
2. 配置基本设置，包括启用状态和 Cookie Cloud 服务端点
3. 生成本地密钥对或导入已有的私钥
4. 添加对端公钥以启用 Cookie 共享
5. 在 HTTP 请求中使用模式匹配语法获取 Cookie

## 配置说明
- 全局状态 ：控制插件是否启用
- 缓存时间 ：设置 Cookie 缓存的有效期（分钟）
- 启用的工具 ：选择在哪些 Burp 工具中启用插件
- Endpoint ：Cookie Cloud 服务的 API 端点
- 本地密钥 ：用于加密和解密 Cookie 数据的密钥对
- 对端密钥 ：其他用户的公钥，用于安全共享 Cookie

## 安装方法
1. 下载最新的 Cookie Cloud Burp 插件 JAR 文件
2. 在 Burp Suite 中，转到 "Extensions" 标签
3. 点击 "Add" 按钮，选择 "Java" 类型
4. 选择下载的 JAR 文件
5. 插件将被加载，并在 Burp Suite 中显示 "Cookie Cloud" 标签


# License

[GNU General Public License v3.0](https://github.com/kur4ge/cookie-cloud-burp/blob/main/LICENSE)