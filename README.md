  # 灵码工坊 —— AI 零代码应用生成平台

本项目是基于 Spring Boot 3 + LangChain4j + Vue 3 开发的AI应用生成平台：《零码工坊》

以**AI开发实战+后端架构**设计为核心。

## 一、项目演示

（1）**项目首页**

![image-20260802212420365](https://img-yuzong.oss-cn-guangzhou.aliyuncs.com/imgTypora/image-20260802212420365.webp)

（2）**智能代码生成**：用户输入需求描述，AI 自动分析并选择合适的生成策略，通过工具调用生成代码文件，采用流式输出让用户实时看到 AI 的执行过程。

![image-20260802212505462](https://img-yuzong.oss-cn-guangzhou.aliyuncs.com/imgTypora/image-20260802212505462.webp)

（3）**可视化编辑**：生成的应用将实时展示，可以进入编辑模式，自由选择网页元素并且和 AI 对话来快速修改页面，直到满意为止。

![image-20260802212804257](https://img-yuzong.oss-cn-guangzhou.aliyuncs.com/imgTypora/image-20260802212804257.webp)

（4）**一键部署分享**：可以将生成的应用一键部署到云端并自动截取封面图，获得可访问的地址进行分享，同时支持完整项目源码下载。

![image-20260802212922122](https://img-yuzong.oss-cn-guangzhou.aliyuncs.com/imgTypora/image-20260802212922122.webp)

（5）**企业级管理监控**：提供用户管理、应用管理、系统监控、业务指标监控等后台功能，管理员可以设置精选应用、监控 AI 调用情况和系统性能。

备注：部署上线的时候，服务器内存已经要大一点，2GB不够。记得1panel安装Grafana和Prometheus，可以监控，第一个是图片是阿里巴巴自带的，只需要在启动类的时候配置一下参数（虚拟机那里）（也可以直接看arms的文档跟着操作）：

-javaagent:/Users/yuzong/IdeaProjects/code/AliyunJavaAgent/aliyun-java-agent.jar
-Darms.licenseKey=
-Darms.appName=yuzong-ai-code-mother

![image-20260802214446071](https://img-yuzong.oss-cn-guangzhou.aliyuncs.com/imgTypora/image-20260802214446071.webp)

![image-20260802214544491](https://img-yuzong.oss-cn-guangzhou.aliyuncs.com/imgTypora/image-20260802214544491.webp)

![image-20260802215129023](https://img-yuzong.oss-cn-guangzhou.aliyuncs.com/imgTypora/image-20260802215129023.webp)



## 二、技术选型

### 核心：

Spring Boot 3.x 框架
Java 21 虚拟线程
⭐️ MyBatis Flex 数据访问

### AI 技术：

⭐️ LangChain4j 框架
⭐️ LangGraph4j 工作流引擎
⭐️ Tool Calling 工具调用
⭐️ Guardrails 护轨
DeepSeek Chat/Reasoner 大模型
⭐️ 文生图 AI 大模型
Open AI ChatModel 接入

### 数据存储：

MySQL 数据库
⭐️ Redis 分布式缓存
⭐️ COS 对象存储
⭐️ Caffeine 本地缓存

### 微服务：

⭐️ Spring Cloud Alibaba
⭐️ Dubbo RPC 调用
⭐️ Nacos 注册中心
⭐️ Higress 网关

### 设计模式：

⭐️ 门面模式、模板方法模式、策略模式、工厂模式、执行器模式
了解：适配器模式、代理模式、单例模式、观察者模式、建造者模式

### 监控运维：

⭐️ ARMS 应用性能监控
⭐️ Prometheus 指标收集存储
⭐️ Grafana 可视化监控面板

### 工具库：

⭐️ Redisson 流量保护 + 分布式 Session
⭐️ Selenium + WebDriver 浏览器自动化
jsoup 解析库
Hutool 工具库
Lombok 注解库
Knife4j + Swagger 接口文档

### 工具

部署工具

Nginx Web 服务器

### 开发工具

⭐️ Cursor 编辑器 AI Vibe Coding
JetBrains IDEA 后端
JetBrains WebStorm 前端

## 三、架构设计

![image-20260802215751907](https://img-yuzong.oss-cn-guangzhou.aliyuncs.com/imgTypora/image-20260802215751907.webp)



## 四、功能模块：

![image-20260802233736371](https://img-yuzong.oss-cn-guangzhou.aliyuncs.com/imgTypora/image-20260802233736371.webp)





## 五、配置说明

数据库，redis，knife4j接口文那些就不多说了。其实也没啥配置好说明的

重点：直接查看application里面，有一大段注释掉的，都需要自己去相对应的网站申请key

重点2: 在项目中全局搜索：mac，有一些对应注释可以看一下，那是一些bug，全都是路径问题的，mac系统需要注意，win系统可能需要改一下（也不用改，把注释mac的方法注释掉，把下面原来的取消注释就能用了）

## 六、快速运行

备注：微服务和正常启动不同

1. 下载仓库
2. application中修改数据库、redis（redis没设置密码的话不用管）、腾讯云对象存储cos（这里别用阿里云，不然要改很多东西）、Pexels、deepseek、（注意，每个模块有不同的application，切勿遗漏某个application的配置，如阿里云或deepseek之类的）
3. 去maven当中：清除一下，好像叫clear，然后刷新maven（有多个maven，直接全部刷新即可）
4. 运行：app模块、用户模块、截图模块

前端：

1. 在终端：npm install --force
2. pakage.json中运行dev

完毕

备注：如果遇到一些bug，可以看我的git提交。
备注：application当中，敏感信息务必创建多个application-local.yaml并不要提交到github









