




# **抽奖系统开发说明书 - V2.0**

## **1. 引言 (Introduction)**

### **1.1 项目背景与目标**

本项目旨在构建一个基于 Spring Boot 的小型全栈抽奖系统，提供一个具备并发安全、概率控制的核心抽奖功能，并搭配简洁直观的前端用户界面。在 V2.0 版本中，我们重点引入了 Redis 分布式缓存，以提升系统性能和并发能力。

主要目标包括：

*   实现一个稳定可靠的后端抽奖逻辑，能够处理奖品管理、库存扣减和基于概率的抽奖算法。
*   确保在高并发场景下，奖品库存不会出现超卖问题。
*   **通过引入 Redis 缓存，显著提升奖品列表读取性能，降低数据库压力。**
*   提供易于理解和操作的用户界面，方便用户参与抽奖。
*   作为学习和实践现代 Java 后端开发（Spring Boot, MyBatis-Plus, MySQL, Redis）以及前后端分离模式的入门项目。

### **1.2 功能范围**

**核心功能：**

*   **奖品管理**: 能够定义不同类型的奖品，包括奖品名称、总数量、当前剩余数量以及中奖概率。
*   **抽奖逻辑**: 根据预设的奖品概率和剩余库存，决定用户是否中奖及中何种奖品。
*   **库存扣减**: 用户中奖后，对应奖品的剩余库存自动减少。
*   **并发安全**: 通过乐观锁机制处理高并发抽奖场景，防止超卖。
*   **概率抽奖**: 实现基于预设概率的抽奖算法。
*   **奖品数据缓存**: 将奖品列表缓存至 Redis，减少数据库读取压力，提高性能。

**用户界面 (前端):**

*   一个简单的网页，包含“点击抽奖”按钮。
*   实时显示抽奖结果（中奖奖品名称或“谢谢参与”）。

### **1.3 核心技术栈**

本项目采用当下流行的前后端分离架构，后端基于 Spring Boot，前端为纯静态页面。

| 类别       | 技术名称             | 版本        | 备注                                     |
| :--------- | :------------------- | :---------- | :--------------------------------------- |
| **后端**   | Spring Boot          | `3.2.5`     | Java Web 应用快速开发框架                |
|            | Spring Framework     | `6.1.6`     | Spring Boot 3.2.5 内部依赖的核心框架     |
|            | MyBatis-Plus         | `3.5.5`     | 简化 MyBatis 操作，兼容 Spring Boot 3.x |
|            | MySQL                | `8.x`       | 关系型数据库                             |
|            | MySQL Connector/J    | 最新兼容版  | 连接 MySQL                               |
|            | Lombok               | 最新兼容版  | 简化 Java Bean 代码                      |
|            | **Redis**            | `6.x / 7.x` | **分布式缓存数据库**                     |
|            | **Spring Data Redis**| `3.2.5`     | **Spring 对 Redis 的集成**               |
| **前端**   | HTML                 | HTML5       | 页面结构                                 |
|            | CSS                  | CSS3        | 页面样式                                 |
|            | JavaScript           | ES6+        | 页面交互，通过 Fetch API 调用后端        |
| **构建/管理** | Maven                | `3.x`       | 项目依赖管理和构建                       |
| **开发环境** | Java Development Kit | `17`        | Java 运行时环境                          |
|            | IntelliJ IDEA        | 最新稳定版  | 开发工具                                 |
| **测试工具** | curl                 | -           | 命令行 API 测试工具                      |

## **2. 系统架构设计 (System Architecture Design)**

### **2.1 逻辑架构**

系统采用经典的三层架构，职责清晰，便于维护和扩展。在 V2.0 中，增加了缓存层，位于业务逻辑层和数据持久层之间。

*   **表现层 (Controller Layer)**: 负责接收用户请求，调用业务逻辑层，并将处理结果返回给前端。
*   **业务逻辑层 (Service Layer)**: 封装核心业务逻辑，包括抽奖算法、库存管理、事务控制等，并**集成缓存逻辑**。
*   **数据持久层 (Mapper/DAO Layer)**: 负责与数据库交互，执行 CRUD (增删改查) 操作。
*   **缓存层 (Cache Layer)**: 使用 Redis 缓存热点数据，减少数据库压力。

### **2.2 模块交互图**

```mermaid
flowchart TD
    subgraph Frontend [前端页面]
        A[index.html] -- 调用 API --> B(script.js)
    end

    subgraph Backend [后端服务 (Spring Boot)]
        direction LR
        C[Controller: LotteryController] -- 调用 Service --> D[Service: LotteryService]
        D -- 1. 优先从缓存读取 --> F(缓存层: Redis)
        F -- 缓存命中 --> D
        F -- 缓存未命中 --> E[Mapper: PrizeMapper]
        E -- 数据查询 --> G[数据库: MySQL]
        G --> E
        E --> F -- 缓存数据 --> D
        D -- 2. 扣减库存 (乐观锁) --> E
        E -- 数据更新 --> G
    end

    B -- HTTP POST /lottery/draw --> C
    C -- JSON 响应 --> B
```

## **3. 开发环境搭建 (Development Environment Setup)**

### **3.1 必备软件安装**

1.  **JDK 17**:
    *   下载地址: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) 或 [OpenJDK](https://openjdk.org/install/)
    *   安装并配置 `JAVA_HOME` 环境变量。
2.  **Maven 3.x**:
    *   下载地址: [Apache Maven](https://maven.apache.org/download.cgi)
    *   解压到指定目录，并配置 `M2_HOME` 和添加到 `Path` 环境变量。
3.  **MySQL 8.x**:
    *   下载地址: [MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
    *   安装并启动 MySQL 服务。
    *   **创建数据库**: 登录 MySQL，执行 `CREATE DATABASE lottery;`
4.  **Redis 6.x / 7.x**:
    *   下载地址: [Redis 官方网站](https://redis.io/download/)
    *   安装并启动 Redis 服务（默认端口 6379）。
    *   **验证**: 在命令行输入 `redis-cli ping`，如果返回 `PONG` 则表示 Redis 运行正常。
5.  **IntelliJ IDEA**:
    *   下载地址: [JetBrains IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
    *   安装并启动。

### **3.2 项目初始化 (Spring Initializr)**

1.  访问 [https://start.spring.io/](https://start.spring.io/)。
2.  配置项目信息如下：
    *   **Project**: Maven Project
    *   **Language**: Java
    *   **Spring Boot**: `3.2.5` (此版本在多个测试中表现出与 MyBatis-Plus 的较好兼容性)
    *   **Group**: `com.miku`
    *   **Artifact**: `Lottery`
    *   **Name**: `Lottery`
    *   **Package name**: `com.miku.lottery`
    *   **Packaging**: Jar
    *   **Java**: `17`
    *   **Dependencies**:
        *   `Spring Web`
        *   `MySQL Driver`
        *   `Lombok`
        *   `MyBatis-Plus` (此为占位，后续 `pom.xml` 中将替换为 `mybatis-plus-spring-boot3-starter`)
        *   `Spring Data Redis` (重要！引入 Redis 集成)
        *   `Spring Boot DevTools` (可选，提供热部署功能，开发时方便)
        *   `Spring Boot Actuator` (可选，提供生产就绪功能，如健康检查)
3.  点击 `GENERATE` 下载项目压缩包。
4.  解压项目到本地目录（例如 `A:\study\javaee\lottery\Lottery`）。
5.  在 IntelliJ IDEA 中，选择 `File` -> `Open`，然后选择解压后的项目根目录。

## **4. 数据库设计与初始化 (Database Design & Initialization)**

### **4.1 `prize` 表结构**

该表用于存储抽奖系统的奖品信息。

```sql
-- prize: 奖品表
CREATE TABLE `prize` (
  `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
  `prize_name` VARCHAR(255) NOT NULL COMMENT '奖品名称',
  `total_quantity` INT NOT NULL DEFAULT 0 COMMENT '总库存',
  `remaining_quantity` INT NOT NULL DEFAULT 0 COMMENT '剩余库存',
  `probability` DECIMAL(5, 4) NOT NULL DEFAULT 0.0000 COMMENT '中奖概率（0到1之间）',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='奖品信息表';
```

**字段说明:**

*   `id`: 主键ID，自增长。
*   `prize_name`: 奖品名称，例如“iPhone 15 Pro”、“1元红包”。
*   `total_quantity`: 奖品总库存量。
*   `remaining_quantity`: 奖品当前剩余库存量，抽中后减少。
*   `probability`: 奖品的中奖概率，一个介于0到1之间的Decimal值。所有奖品的概率之和可以小于1（表示有未中奖的可能性），也可以等于1（表示每次必中）。
*   `version`: **乐观锁版本号**，用于在高并发场景下保证库存扣减的原子性和一致性。每次更新库存时，此版本号会递增，并且更新操作会检查版本号是否匹配，防止并发修改。
*   `create_time`: 记录创建时间。
*   `update_time`: 记录最后更新时间。

### **4.2 初始数据插入**

在 MySQL 客户端或数据库管理工具中执行以下 SQL 语句，为 `prize` 表插入初始数据：

```sql
INSERT INTO `prize` (`prize_name`, `total_quantity`, `remaining_quantity`, `probability`, `version`) VALUES
('一等奖：iPhone 15 Pro', 1, 1, 0.0010, 0),
('二等奖：华为 MatePad', 5, 5, 0.0100, 0),
('三等奖：10元优惠券', 100, 100, 0.2000, 0),
('四等奖：1元红包', 1000, 1000, 0.5000, 0),
('谢谢参与', 999999, 999999, 0.2890, 0); -- 建议添加此项，使得所有奖品概率总和为1，确保每次抽奖都有明确结果
```

## **5. 后端代码实现 (Backend Code Implementation)**

### **5.1 `pom.xml` 文件**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>3.2.5</version> <!-- Spring Boot 版本 -->
		<relativePath/> <!-- lookup parent from repository -->
	</parent>

	<groupId>com.miku</groupId>
	<artifactId>Lottery</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>Lottery</name>
	<description>Lottery</description>
	<url/>
	<licenses>
		<license/>
	</licenses>
	<developers>
		<developer/>
	</developers>
	<scm>
		<connection/>
		<developerConnection/>
		<tag/>
		<url/>
	</scm>
	<properties>
		<java.version>17</java.version>
	</properties>
	<dependencies>

		<!-- Spring Boot Web 依赖，提供 Web 应用功能 -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>

		<!-- MyBatis-Plus 针对 Spring Boot 3 的启动器，关键依赖 -->
		<dependency>
			<groupId>com.baomidou</groupId>
			<artifactId>mybatis-plus-spring-boot3-starter</artifactId>
			<version>3.5.5</version>
		</dependency>

		<!-- Spring Data Redis 依赖，用于集成 Redis 缓存 -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis</artifactId>
		</dependency>

		<!-- MySQL JDBC 驱动 -->
		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>

		<!-- Lombok 简化代码 -->
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>

		<!-- Spring Boot 测试依赖 -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<!-- 依赖 Spring Boot Parent 插件管理，无需额外配置 Lombok 注解处理器 -->
			</plugin>

			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<excludes>
						<exclude>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</exclude>
					</excludes>
				</configuration>
			</plugin>
		</plugins>
	</build>

</project>
```

### **5.2 `application.properties` 文件**

位于 `src/main/resources/application.properties`。

```properties
# 应用名称
spring.application.name=Lottery

# 服务器端口
server.port=8080

# 数据库连接配置
# spring.datasource.url: JDBC 连接字符串，指定数据库类型(mysql)、主机(localhost)、端口(3306)、数据库名(lottery)。
#                       参数 useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai 用于解决中文乱码和时区问题。
spring.datasource.url=jdbc:mysql://localhost:3306/lottery?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
# spring.datasource.username: 数据库用户名
spring.datasource.username=root
# spring.datasource.password: 数据库密码
spring.datasource.password=123456
# spring.datasource.driver-class-name: JDBC 驱动类名
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver


# Redis 配置
# Redis 服务器地址
spring.data.redis.host=localhost
# Redis 服务器端口
spring.data.redis.port=6379
# Redis 数据库索引（默认为 0）
spring.data.redis.database=0
# Redis 连接密码（如果你的 Redis 设置了密码，请取消注释并填写）
# spring.data.redis.password=your_redis_password
# 连接超时时间（毫秒）
spring.data.redis.timeout=5000


# MyBatis-Plus Mapper XML 文件位置（如果使用 XML 配置 SQL）
# classpath:/mapper/*.xml 表示在 classpath 下的 mapper 文件夹中查找所有 .xml 文件。
# 本项目主要使用注解，此配置可能不会被严格要求，但保留是良好实践。
mybatis-plus.mapper-locations=classpath:/mapper/*.xml
# MyBatis-Plus 全局配置：逻辑删除字段（本项目 prize 表无此字段，故注释）
#mybatis-plus.global-config.db-config.logic-delete-field=flag


# 允许 Spring 应用程序上下文中的循环引用
# 警告：通常不鼓励在生产环境中使用，因为它可能掩盖设计问题。
# 但对于 AOP 自注入场景，它是一个常见且可接受的解决方案。
spring.main.allow-circular-references=true
```

### **5.3 实体类：`Prize.java`**

位于 `src/main/java/com/miku/lottery/entity/Prize.java`。

```java
package com.miku.lottery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable; // 导入 Serializable 接口
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 奖品实体类，映射数据库 prize 表
 */
@Data // Lombok 注解，自动生成 getter, setter, equals, hashCode, toString 方法
@TableName("prize") // 映射到数据库的 prize 表
public class Prize implements Serializable { // 关键改动：实现 Serializable 接口，以便能够被序列化存储到 Redis

    @TableId(type = IdType.AUTO) // 声明主键并设置为数据库自增
    private Long id; // 奖品唯一标识符

    private String prizeName; // 奖品名称，例如“iPhone 15 Pro”

    private Integer totalQuantity; // 奖品总库存量

    private Integer remainingQuantity; // 奖品当前剩余库存量，抽中后会减少

    private BigDecimal probability; // 奖品的中奖概率，使用 BigDecimal 确保浮点数精度

    @Version // 声明为乐观锁版本号字段，MyBatis-Plus 会自动处理
    private Integer version; // 乐观锁版本号，每次更新时会自动递增

    private LocalDateTime createTime; // 记录创建时间

    private LocalDateTime updateTime; // 记录最后更新时间
}
```

### **5.4 Mapper 接口：`PrizeMapper.java`**

位于 `src/main/java/com/miku/lottery/mapper/PrizeMapper.java`。

```java
package com.miku.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miku.lottery.entity.Prize;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Prize 表的 Mapper 接口，继承 BaseMapper 提供常用 CRUD 方法
 */
@Mapper // 声明为 MyBatis 的 Mapper 接口，Spring 会扫描并创建代理对象
public interface PrizeMapper extends BaseMapper<Prize> {

    /**
     * 扣减库存方法，使用乐观锁机制保证并发安全。
     * 该方法直接通过 SQL 更新语句操作数据库。
     * 只有当奖品ID和版本号都匹配，且剩余库存大于0时，才执行扣减和版本号递增。
     *
     * @param id 奖品ID，通过 @Param("id") 映射到 SQL 中的 #{id}
     * @param version 当前奖品的版本号，通过 @Param("version") 映射到 SQL 中的 #{version}
     * @return 更新的行数，0表示扣减失败（如库存不足或版本号不匹配），1表示成功扣减。
     */
    @Update("UPDATE prize SET remaining_quantity = remaining_quantity - 1, version = version + 1 " +
            "WHERE id = #{id} AND remaining_quantity > 0 AND version = #{version}")
    int deductStock(@Param("id") Long id, @Param("version") Integer version);
}
```

### **5.5 Service 层：`LotteryService.java`**

位于 `src/main/java/com/miku/lottery/service/LotteryService.java`。

```java
package com.miku.lottery.service;

import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.miku.lottery.entity.Prize;
import com.miku.lottery.mapper.PrizeMapper;
import org.springframework.cache.annotation.Cacheable; // 导入 Cacheable 注解
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

/**
 * 抽奖业务逻辑服务类
 * 负责处理抽奖的核心业务流程，包括奖品获取、概率计算、中奖判定和库存扣减。
 */
@Service
public class LotteryService {

    @Autowired // 自动注入 PrizeMapper 实例
    private PrizeMapper prizeMapper;

    @Autowired
    private LotteryService self; // 关键改动：自注入 LotteryService 实例的代理对象，解决 AOP 自调用问题

    private final Random random = new Random(); // 用于生成随机数

    /**
     * 获取所有库存大于0的奖品列表。
     * 该方法的查询结果会被缓存到名为 "prizes" 的缓存空间中。
     * 缓存键固定为 "availablePrizesList"。
     *
     * @return 可用的奖品列表
     */
    @Cacheable(value = "prizes", key = "'availablePrizesList'")
    // value = "prizes": 指定缓存的名称，Redis 中会以这个名称作为 key 的前缀。
    // key = "'availablePrizesList'": 指定缓存的键。这里使用一个固定的字符串作为键，
    //                                因为我们总是缓存所有可用奖品的列表。
    public List<Prize> getAllAvailablePrizes() {
        System.out.println("从数据库加载可用奖品列表..."); // 打印日志，观察是否命中缓存
        return prizeMapper.selectList(
                new QueryWrapper<Prize>().gt("remaining_quantity", 0)
        );
    }

    /**
     * 执行抽奖
     *
     * @return 返回中奖的奖品信息，如果未中奖则返回 null
     */
    @Transactional(rollbackFor = Exception.class)   // 开启事务，任何异常都回滚
    // 关键改动：移除 @CacheEvict 注解。
    // 缓存清除现在依赖 Redis 的 TTL (Time To Live) 机制。
    // 每次成功的抽奖不会立即清除缓存，而是等待缓存自然过期（5分钟）后，
    // 下次查询时才会从数据库重新加载最新的奖品列表。
    public Prize draw() {
        // 1. 获取所有库存大于0的奖品列表 (通过 self.getAllAvailablePrizes() 调用，确保缓存生效)
        List<Prize> availablePrizes = self.getAllAvailablePrizes();

        // 如果没有可用奖品，则直接返回 null，表示无法抽奖
        if (availablePrizes.isEmpty()) {
            System.out.println("当前没有可用奖品。");
            return null;
        }

        // 2. 计算所有可用奖品的总概率
        BigDecimal totalProbability = availablePrizes.stream()
                .map(Prize::getProbability)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 如果总概率为0，也无法抽奖
        if (totalProbability.compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("所有可用奖品的总概率为0，无法抽奖。");
            return null;
        }

        // 3. 生成一个 0 到 totalProbability 之间的随机数
        BigDecimal randomValue = totalProbability.multiply(new BigDecimal(random.nextDouble()));

        // 4. 轮盘赌算法：遍历奖品列表，确定中奖奖品
        Prize winningPrize = null;
        BigDecimal currentSum = BigDecimal.ZERO;
        for (Prize prize : availablePrizes) {
            currentSum = currentSum.add(prize.getProbability());
            if (randomValue.compareTo(currentSum) < 0) {
                winningPrize = prize;
                break;
            }
        }

        // 5. 如果根据概率没有抽中任何奖品（winningPrize 仍为 null）
        if (winningPrize == null) {
            System.out.println("随机数未落在任何奖品区间，未中奖。");
            return null;
        }

        // 6. 核心步骤：尝试使用乐观锁扣减中奖奖品的库存
        // 此操作直接修改数据库，不会立即影响 Redis 缓存中的奖品列表。
        int affectedRows = prizeMapper.deductStock(winningPrize.getId(), winningPrize.getVersion());

        // 7. 检查库存扣减结果
        if (affectedRows == 0) {
            // 如果 affectedRows 为 0，表示库存扣减失败。
            // 这通常是由于在高并发情况下，其他线程先一步修改了该奖品的库存或版本号。
            // 在此情况下，事务会自动回滚。
            System.out.println("库存扣减失败，奖品ID: " + winningPrize.getId() + "，版本号: " + winningPrize.getVersion() + "。可能是并发冲突导致。");
            return null;
        }

        // 8. 扣减成功，返回中奖奖品信息
        System.out.println("恭喜抽中奖品: " + winningPrize.getPrizeName());
        return winningPrize;
    }
}
```

### **5.6 Controller 层：`LotteryController.java`**

位于 `src/main/java/com/miku/lottery/controller/LotteryController.java`。

```java
package com.miku.lottery.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.miku.lottery.entity.Prize;
import com.miku.lottery.service.LotteryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 抽奖系统的 RESTful API 控制器
 * 负责处理来自前端的 HTTP 请求，并返回 JSON 格式的响应。
 */
@RestController // 声明为 RESTful 风格的 Controller，返回 JSON 数据
@RequestMapping("/lottery") // 所有接口的根路径
public class LotteryController {

    @Autowired // 自动注入 LotteryService 实例
    private LotteryService lotteryService;

    /**
     * 抽奖接口。
     * 接收 POST 请求，执行抽奖逻辑并返回结果。
     *
     * @return 返回包含抽奖结果的 JSON 响应。
     *         code: 200 表示中奖，404 表示未中奖。
     *         message: 提示信息。
     *         data: 中奖奖品名称（如果中奖），否则为 null。
     */
    @PostMapping("/draw") // 映射到 POST 请求，具体路径为 /lottery/draw
    public Map<String, Object> drawLottery() {
        Map<String, Object> result = new HashMap<>(); // 创建一个 HashMap 用于存储响应数据

        // 调用业务逻辑层 (Service) 的 draw() 方法执行抽奖
        Prize prize = lotteryService.draw();

        // 根据 draw() 方法的返回结果判断抽奖状态
        if (prize != null) {
            // 如果 prize 不为 null，表示用户成功中奖
            result.put("code", 200); // 设置成功状态码
            result.put("message", "恭喜你，中奖了！"); // 设置成功消息
            result.put("data", prize.getPrizeName()); // 将中奖奖品的名称放入 data 字段
        } else {
            // 如果 prize 为 null，表示用户未中奖或抽奖过程中发生并发冲突导致失败
            result.put("code", 404); // 设置未中奖状态码
            result.put("message", "很遗憾，未中奖，谢谢参与！"); // 设置未中奖消息
            result.put("data", null); // data 字段为 null
        }

        return result; // Spring 会自动将这个 Map 序列化为 JSON 字符串并作为 HTTP 响应体返回
    }
}
```

### **5.7 主启动类：`LotteryApplication.java`**

位于 `src/main/java/com/miku/lottery/LotteryApplication.java`。

```java
package com.miku.lottery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching; // 导入 EnableCaching 注解

/**
 * Spring Boot 抽奖系统主启动类
 */
@SpringBootApplication // 声明这是一个 Spring Boot 应用程序
@MapperScan("com.miku.lottery.mapper") // 扫描指定包下的 Mapper 接口，将其注册为 Spring Bean
@EnableCaching // 关键改动：启用 Spring Boot 的缓存功能
public class LotteryApplication {

    public static void main(String[] args) {
        // 启动 Spring Boot 应用程序
        SpringApplication.run(LotteryApplication.class, args);
    }

}
```

### **5.8 Redis 配置类：`RedisConfig.java`**

位于 `src/main/java/com/miku/lottery/config/RedisConfig.java`。

```java
package com.miku.lottery.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // 导入 JavaTimeModule

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration; // 导入 RedisCacheConfiguration
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer; // 使用 Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext; // 导入 RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer; // 导入 StringRedisSerializer

import java.time.Duration; // 导入 Duration 类，用于设置缓存过期时间

/**
 * Redis 配置类，用于自定义 RedisTemplate 和 RedisCacheManager 的序列化器。
 */
@Configuration // 声明这是一个配置类，会被 Spring 容器扫描并加载
public class RedisConfig {

    /**
     * 配置 RedisTemplate，自定义键和值的序列化器。
     * 这个 Bean 主要用于当你在代码中直接注入和使用 RedisTemplate 时，
     * 它的序列化行为。@Cacheable 注解默认不直接使用这个 RedisTemplate 的序列化器，
     * 而是通过 RedisCacheConfiguration 来配置。
     *
     * @param redisConnectionFactory Redis 连接工厂，由 Spring 自动注入
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean // 声明这是一个 Spring Bean，会被 Spring 容器管理
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 创建并配置 ObjectMapper，用于 JSON 序列化和反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        // 指定序列化时可以访问所有字段（包括私有字段），以及所有 getter/setter 方法
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 注册 JavaTimeModule 模块，解决 LocalDateTime 等 Java 8 时间类型序列化问题
        objectMapper.registerModule(new JavaTimeModule());
        // 激活默认类型信息，解决反序列化时多态类型的问题（通常用于存储复杂对象）
        // 对于 Prize 这种简单对象，如果 Prize 是 final 类且不涉及继承，此行可能不是必需的，但保留可增强兼容性。
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        // 使用 Jackson2JsonRedisSerializer，并传入自定义的 ObjectMapper 实例
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // 设置键（Key）的序列化器为 StringRedisSerializer
        template.setKeySerializer(new StringRedisSerializer());
        // 设置值（Value）的序列化器为 Jackson2JsonRedisSerializer
        template.setValueSerializer(jackson2JsonRedisSerializer);

        // 设置 Hash 类型键（HashKey）的序列化器为 StringRedisSerializer
        template.setHashKeySerializer(new StringRedisSerializer());
        // 设置 Hash 类型值（HashValue）的序列化器为 Jackson2JsonRedisSerializer
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        // 调用 afterPropertiesSet() 方法，确保所有属性设置完毕并完成初始化
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 Redis 缓存管理器（RedisCacheManager）的默认缓存行为。
     * 这是 @Cacheable/@CacheEvict 注解能够使用 JSON 序列化的关键。
     *
     * @return 配置好的 RedisCacheConfiguration 实例
     */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        // 创建并配置 ObjectMapper，用于 JSON 序列化和反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.registerModule(new JavaTimeModule()); // 解决 Java 8 时间类型序列化问题
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        // 使用 Jackson2JsonRedisSerializer 作为缓存值的序列化器
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        return RedisCacheConfiguration.defaultCacheConfig() // 获取默认的缓存配置
                .entryTtl(Duration.ofMinutes(5)) // 设置缓存项的默认过期时间为 5 分钟
                // 设置键的序列化器为 StringRedisSerializer
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // 设置值的序列化器为 Jackson2JsonRedisSerializer
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer));
    }
}
```

## **6. 前端代码实现 (Frontend Code Implementation)**

前端代码作为静态资源放置在 `src/main/resources/static/` 目录下。

### **6.1 `index.html`**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>幸运大抽奖</title>
    <!-- 引入样式表 -->
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="container"> <!-- 页面主要内容的容器 -->
        <h1>幸运大抽奖</h1> <!-- 页面主标题 -->
        <button id="drawButton">点击抽奖</button> <!-- 抽奖按钮，通过 ID 方便 JS 访问 -->
        <p id="resultMessage"></p> <!-- 用于显示抽奖结果消息的段落，通过 ID 方便 JS 访问 -->
        <p id="prizeDisplay"></p> <!-- 用于显示中奖奖品名称的段落，通过 ID 方便 JS 访问 -->
    </div>

    <script src="script.js"></script> <!-- 引入外部 JavaScript 脚本，通常放在 body 底部以确保 DOM 加载完毕 -->
</body>
</html>
```

### **6.2 `style.css`**

```css
body {
    font-family: Arial, sans-serif; /* 字体设置 */
    display: flex; /* Flex布局居中 */
    justify-content: center; /* 水平居中 */
    align-items: center; /* 垂直居中 */
    min-height: 100vh; /* 最小高度占满视口 */
    margin: 0; /* 移除默认外边距 */
    background-color: #f0f2f5; /* 背景色 */
    color: #333; /* 默认文字颜色 */
}

.container {
    background-color: #fff; /* 容器背景色 */
    padding: 40px; /* 内边距 */
    border-radius: 10px; /* 圆角 */
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1); /* 阴影效果 */
    text-align: center; /* 文字居中 */
    width: 90%; /* 宽度 */
    max-width: 500px; /* 最大宽度 */
}

h1 {
    color: #4CAF50; /* 标题颜色 */
    margin-bottom: 30px; /* 标题下方间距 */
}

button {
    background-color: #007bff; /* 按钮背景色 */
    color: white; /* 按钮文字颜色 */
    padding: 15px 30px; /* 按钮内边距 */
    border: none; /* 无边框 */
    border-radius: 5px; /* 按钮圆角 */
    font-size: 1.2em; /* 按钮文字大小 */
    cursor: pointer; /* 鼠标悬停手型 */
    transition: background-color 0.3s ease; /* 背景色过渡效果 */
    margin-bottom: 20px; /* 按钮下方间距 */
}

button:hover {
    background-color: #0056b3; /* 按钮悬停背景色 */
}

#resultMessage {
    font-size: 1.1em; /* 结果消息字体大小 */
    font-weight: bold; /* 字体加粗 */
    color: #d9534f; /* 默认红色表示未中奖 */
    margin-top: 20px; /* 上方间距 */
}

#prizeDisplay {
    font-size: 1.5em; /* 奖品显示字体大小 */
    font-weight: bold; /* 字体加粗 */
    color: #4CAF50; /* 绿色表示中奖 */
}
```

### **6.3 `script.js`**

```javascript
// 当 DOM (文档对象模型) 内容完全加载并解析完毕后执行回调函数
document.addEventListener('DOMContentLoaded', () => {
    // 通过 ID 获取页面元素
    const drawButton = document.getElementById('drawButton'); // 抽奖按钮
    const resultMessage = document.getElementById('resultMessage'); // 结果消息显示区域
    const prizeDisplay = document.getElementById('prizeDisplay'); // 奖品名称显示区域

    // 为抽奖按钮添加点击事件监听器
    drawButton.addEventListener('click', async () => {
        // 点击按钮后，立即更新页面状态，提示用户正在抽奖
        resultMessage.textContent = '抽奖中...';
        prizeDisplay.textContent = ''; // 清空之前的奖品显示
        resultMessage.style.color = '#333'; // 将消息颜色重置为默认色

        try {
            // 使用 Fetch API 发送异步 POST 请求到后端抽奖接口
            // await 关键字会暂停代码执行，直到 fetch 请求完成并返回响应
            const response = await fetch('/lottery/draw', {
                method: 'POST', // 指定 HTTP 请求方法为 POST
                headers: {
                    // 设置请求头，告知服务器请求体是 JSON 格式。
                    // 尽管本请求没有实际的请求体，但这是一个良好实践。
                    'Content-Type': 'application/json'
                }
            });

            // await response.json() 会解析响应体为 JSON 对象。
            // 再次使用 await 是因为解析 JSON 也是一个异步操作。
            const data = await response.json();

            // 根据后端返回的 JSON 数据中的 'code' 字段判断抽奖结果
            if (data.code === 200) {
                // 如果 code 为 200，表示成功中奖
                resultMessage.textContent = data.message; // 显示后端返回的成功消息
                resultMessage.style.color = '#4CAF50'; // 将结果消息颜色设为绿色
                prizeDisplay.textContent = `恭喜您获得：${data.data}`; // 显示中奖奖品名称
            } else {
                // 如果 code 不为 200（例如 404），表示未中奖或抽奖失败
                resultMessage.textContent = data.message; // 显示后端返回的失败消息
                resultMessage.style.color = '#d9534f'; // 将结果消息颜色设为红色
                prizeDisplay.textContent = ''; // 清空奖品显示区域
            }
        } catch (error) {
            // 捕获在 fetch 请求或 JSON 解析过程中可能发生的网络错误或其他异常
            console.error('抽奖请求失败:', error); // 在浏览器控制台打印详细错误信息
            resultMessage.textContent = '抽奖失败，请稍后再试。'; // 提示用户抽奖失败
            resultMessage.style.color = '#d9534f'; // 设为红色
            prizeDisplay.textContent = ''; // 清空奖品显示
        }
    });
});
```

## **7. 系统运行与测试 (Running & Testing)**

### **7.1 启动后端服务**

1.  **打开 IntelliJ IDEA**，导航到 `src/main/java/com/miku/lottery/LotteryApplication.java`。
2.  右键点击文件空白处或类名，选择 `Run 'LotteryApplication.main()'`。
3.  观察 IDEA 控制台输出，确认最后一行日志显示类似 `Tomcat started on port 8080` 字样，表示后端服务已成功启动并监听 8080 端口。

### **7.2 访问前端页面进行抽奖**

1.  在后端服务成功启动后，打开您的网页浏览器（推荐 Chrome, Firefox 等）。
2.  在浏览器地址栏输入 `http://localhost:8080/` 或 `http://localhost:8080/index.html`。
3.  页面加载成功后，点击页面上的“**点击抽奖**”按钮。
4.  观察页面上显示的抽奖结果。同时，**观察 IDEA 控制台日志**：
    *   第一次点击时，会看到 `从数据库加载可用奖品列表...`。
    *   在 5 分钟内连续点击，将**不再看到**该日志，表明奖品列表是从 Redis 缓存中获取的。
    *   等待 5 分钟后再次点击，会重新看到该日志，因为缓存已过期。

### **7.3 API 接口测试 (使用 PowerShell `curl`)**

确保后端服务正在运行。

1.  打开 Windows **PowerShell** 命令行工具。
2.  执行以下 `curl` 命令来模拟 POST 请求到抽奖接口：
    ```powershell
    curl -v -X POST --noproxy "localhost,127.0.0.1" http://localhost:8080/lottery/draw
    ```
    *   `-v`: 显示详细的请求和响应过程。
    *   `-X POST`: 指定 HTTP 请求方法为 POST。
    *   `--noproxy "localhost,127.0.0.1"`: 这是一个关键参数，用于告诉 `curl` 在访问 `localhost` 或 `127.0.0.1` 时，**跳过任何系统或环境变量配置的代理服务器**。
3.  观察 PowerShell 窗口中返回的 JSON 响应，验证抽奖结果是否符合预期。例如：
    ```json
    {"code":200,"data":"四等奖：1元红包","message":"恭喜你，中奖了！"}
    ```
    或
    ```json
    {"code":404,"data":null,"message":"很遗憾，未中奖，谢谢参与！"}
    ```
4.  多次执行此命令，并结合数据库管理工具查看 `prize` 表中 `remaining_quantity` 字段的实时变化，以验证库存扣减的正确性。

## **8. 开发过程中遇到的困难与解决方案总结**

本次抽奖系统从基础功能到引入 Redis 缓存的开发过程中，我们遇到了一系列典型且棘手的挑战。以下详细记录了这些困难及其解决过程，为未来类似项目提供宝贵的经验。

### **阶段一**

### **8.1 困难一：`javax.annotation.Resource` 找不到符号**

*   **现象**: 编译时报错 `java: 找不到符号 符号: 类 Resource 位置: 程序包 javax.annotation`。
*   **原因分析**:
    *   项目使用了 Java 17。
    *   在 Java 9 及更高版本中，`javax.annotation` 包被从 Java SE 模块中移除，不再默认提供。`@Resource` 注解属于此包。
    *   早期 Spring Boot 版本（Spring Boot 2.x）通常与 Java 8 配合，该包默认存在。而 Spring Boot 3.x 默认支持 Java 17+，但它自身并未直接提供 `javax.annotation` 的兼容依赖。
*   **解决方案**:
    *   **推荐方案**: 将代码中的 `@Resource` 注解替换为 Spring 框架自身提供的 `@Autowired` 注解。`@Autowired` 是 Spring 原生且更推荐的依赖注入方式，不需要额外依赖，且在 Spring 生态中广泛使用。
    *   **备选方案**: 如果必须使用 `@Resource`，可以在 `pom.xml` 中手动添加 `jakarta.annotation-api` 依赖。但考虑到 Spring Boot 3.x 已经全面转向 `jakarta` 命名空间，此方案略显冗余。

### **8.2 困难二：Lombok `getter/setter` 方法找不到符号**

*   **现象**: 编译时报错 `java: 找不到符号 符号: 方法 getPrizeName() 位置: 类型为com.miku.lottery.entity.Prize的变量 prize` 等。
*   **原因分析**:
    *   Lombok 通过注解处理器在编译阶段自动生成 `getter/setter` 等方法。
    *   IDE (IntelliJ IDEA) 的“注解处理器”功能未启用，导致 Lombok 没有在编译前生成这些方法，编译器自然找不到。
*   **解决方案**:
    *   在 IntelliJ IDEA 中，导航到 `File` -> `Settings` (或 `Preferences` for macOS) -> `Build, Execution, Deployment` -> `Compiler` -> `Annotation Processors`。
    *   **勾选 `Enable annotation processing` 复选框。**
    *   点击 `Apply` -> `OK`，然后执行 `Build` -> `Rebuild Project`。
    *   同时，优化了 `pom.xml` 中 `maven-compiler-plugin` 的配置，移除冗余的 `<configuration>`，让 Spring Boot Parent 自动管理 Lombok 的注解处理器。

### **8.3 困难三：`Invalid bean definition...factoryBeanObjectType: java.lang.String` (MyBatis-Plus 兼容性问题)**

*   **现象**: 应用启动失败，日志中出现 `org.springframework.beans.factory.BeanDefinitionStoreException: Invalid bean definition with name 'prizeMapper' ... Invalid value type for attribute 'factoryBeanObjectType': java.lang.String`。
*   **原因分析**:
    *   `3.5.4` 是一个非稳定、可能不存在的 Spring Boot 版本，导致依赖解析混乱。
    *   即使切换到 `3.3.0` 或 `3.2.5`，`Invalid value type for attribute 'factoryBeanObjectType': java.lang.String` 错误依然存在，这强烈暗示 `MyBatis-Plus` 的旧启动器 (`mybatis-plus-boot-starter`) 与 `Spring Framework 6.1.x` (Spring Boot 3.x 内部使用) 之间存在深层次的兼容性问题，尤其是在 Bean 定义元数据处理方面。
*   **故障排除步骤 (迭代式)**:
    *   初始尝试通过在 `LotteryApplication` 中添加 `@MapperScan("com.miku.lottery.mapper")` 来解决（此步骤正确，但不足以解决版本兼容性问题）。
    *   尝试了不同的 Spring Boot 版本（如 3.5.4, 3.3.0, 3.2.5）以寻找兼容的 Spring Framework 版本。
    *   尝试升级 `mybatis-plus-boot-starter` 到更高版本（如 3.5.7），甚至回退到更早的 `3.4.3.4`。
    *   多次执行彻底的 Maven 缓存失效 (`mvn clean install -U`) 和 IntelliJ IDEA 缓存清除 (`File > Invalidate Caches...`)，以消除环境因素。
*   **最终解决方案**: 将 `pom.xml` 中 `com.baomidou:mybatis-plus-boot-starter` 依赖替换为 **`com.baomidou:mybatis-plus-spring-boot3-starter` (版本 `3.5.5`)**。这个特定的启动器是官方为完全兼容 Spring Boot 3.x 及其底层 Spring Framework 6.x 而设计的。

### **8.4 困难四：`curl` 命令无输出 / `502 Bad Gateway`**

*   **现象**: 后端服务已启动，但 `curl` 命令发送请求后无任何输出或返回 `502 Bad Gateway` 错误。
*   **原因分析**:
    *   通过 `curl -v` 详细日志发现：`* Uses proxy env variable http_proxy == 'http://192.168.86.163:7899'` 和 `< HTTP/1.1 502 Bad Gateway`。
    *   系统环境变量中配置了 HTTP 代理，导致 `curl` 尝试通过代理服务器访问 `localhost`。
    *   代理服务器无法正确转发或连接到本地的 Spring Boot 应用（`localhost:8080`），从而返回 `502 Bad Gateway` 错误，且响应体为空。
*   **解决方案**:
    *   在 `curl` 命令中明确指定不使用代理访问本地地址。
    *   **推荐方案**: `curl -v -X POST --noproxy "localhost,127.0.0.1" http://localhost:8080/lottery/draw`。
    *   **备选方案**: 临时设置 PowerShell 环境变量 `$env:NO_PROXY = "localhost,127.0.0.1"`。
    *   **根本解决方案（如果需要）**: 检查并调整系统级代理设置或代理软件配置，确保其不会劫持本地回环地址流量。


### **阶段二**

### **8.5 困难五：`java.io.NotSerializableException` (Redis 序列化问题)**

*   **现象**: 应用程序启动正常，但在第一次调用抽奖接口时，抛出 `java.io.NotSerializableException: com.miku.lottery.entity.Prize` 异常。
*   **原因分析**:
    *   Spring Data Redis 默认的 `JdkSerializationRedisSerializer` 要求被缓存的对象（包括集合中的元素）必须实现 `Serializable` 接口。
    *   即使 `Prize` 类实现了 `Serializable`，但如果涉及到复杂对象（如 `LocalDateTime`）或集合的默认 JDK 序列化，仍然可能出现问题。
    *   `RedisTemplate` 的序列化器配置默认不影响 `@Cacheable` 所使用的 `RedisCacheManager` 的序列化器。
*   **解决方案**:
    *   **确保 `Prize` 实体类实现 `Serializable` 接口。**
    *   **明确配置 `RedisCacheConfiguration` 使用 Jackson JSON 序列化器。** 在 `RedisConfig.java` 中添加 `redisCacheConfiguration()` Bean，使用 `Jackson2JsonRedisSerializer` 并传入自定义的 `ObjectMapper` 来处理 `LocalDateTime` 等 Java 8 时间类型，并将其设置为 `RedisCacheConfiguration` 的值序列化器。这确保了 `@Cacheable` 注解能够正确地将对象序列化为 JSON 字符串存入 Redis。

### **8.6 困难六：Spring Cache AOP 自调用失效导致缓存不生效**

*   **现象**: 应用程序启动正常，`RedisConfig` 也正确配置，但每次调用 `draw()` 方法时，控制台仍然打印 `从数据库加载可用奖品列表...`，表明 `getAllAvailablePrizes()` 方法的缓存没有生效。
*   **原因分析**:
    *   Spring AOP (Spring Cache 基于 AOP 实现) 通过代理为 Bean 添加横切逻辑。
    *   当 `LotteryService` 内部的方法（`draw()`）调用其自身被 `@Cacheable` 注解的方法（`getAllAvailablePrizes()`）时，调用的是 `this` 引用，即原始对象，而不是 Spring 创建的代理对象。因此，AOP 无法拦截到这个内部方法调用，缓存逻辑未能应用。
*   **解决方案**:
    *   在 `LotteryService` 类中，通过 `@Autowired` 注入自身的一个代理实例：`@Autowired private LotteryService self;`。
    *   在 `draw()` 方法内部，将 `this.getAllAvailablePrizes()` 的调用改为 `self.getAllAvailablePrizes()`。这样，通过代理对象调用，Spring AOP 就能拦截到方法，并应用缓存逻辑。
    *   **注意**: 引入自注入会产生循环依赖。为了解决这个问题，需要在 `application.properties` 中添加配置 `spring.main.allow-circular-references=true`，允许 Spring 容器处理这种特定类型的循环依赖。

### **8.7 困难七：`@CacheEvict` 策略导致缓存频繁失效**

*   **现象**: 解决了自调用问题后，`getAllAvailablePrizes()` 第一次调用会从数据库加载，但后续每次成功的 `draw()` 操作后，下一次调用又会从数据库加载，缓存并未持续生效。
*   **原因分析**:
    *   `draw()` 方法上原有的 `@CacheEvict(value = "prizes", key = "'availablePrizesList'")` 注解。
    *   `@CacheEvict` 默认在方法成功返回后执行清除操作。由于 `draw()` 方法每次都会成功返回（无论是中奖还是“谢谢参与”），因此它每次都会清除奖品列表缓存。
    *   这导致缓存形同虚设，每次请求都回源数据库，违背了缓存的优化初衷。
*   **解决方案**:
    *   **移除 `draw()` 方法上的 `@CacheEvict` 注解。**
    *   奖品列表的缓存策略改为**依赖 Redis `TTL` (Time To Live，过期时间)**。在 `RedisConfig.java` 的 `redisCacheConfiguration()` 中，我们已经设置了 `entryTtl(Duration.ofMinutes(5))`。这意味着奖品列表会在缓存中保留 5 分钟。在这 5 分钟内，所有抽奖请求都将从缓存中获取数据，大幅减少数据库压力。库存的最终一致性由 TTL 和乐观锁保证。

## **9. 未来增强与优化 (Future Enhancements & Optimizations)**

当前系统已具备核心功能和初步性能优化，但作为生产级应用，仍有诸多可优化和扩展之处：

*   **用户模块**: 引入用户注册/登录功能，实现每个用户独立的抽奖次数限制、中奖历史记录。
*   **异步化奖品发放**: 引入消息队列（如 Kafka/RabbitMQ），将中奖记录持久化和奖品发放等耗时操作异步化，提高抽奖接口响应速度。
*   **管理后台**: 开发一个简单的 Web 界面，供管理员配置奖品、查看数据、管理用户。
*   **高并发优化**: 进一步优化乐观锁的重试机制，或者考虑引入分布式锁。
*   **日志与监控**: 完善日志级别和内容，集成 ELK (Elasticsearch, Logstash, Kibana) 或 Prometheus/Grafana 进行日志分析和系统监控。
*   **安全性**: 增加接口鉴权、防止 SQL 注入、XSS 攻击等安全措施。
*   **部署**: 将应用打包为 Docker 镜像，部署到云服务器或 Kubernetes 集群。

---








