

# **幸运大抽奖系统开发说明书 (第一版)**

**文档版本:** 1.0
**编写日期:** 2025年8月2日
**编写者:** 高级软件工程师 (AI 助手) & 项目开发者 (你)

---

## **1. 引言**

### **1.1 系统概述**

本系统是一个基于 Spring Boot、MyBatis-Plus 和 MySQL 构建的后端抽奖系统，并提供了一个简单的静态前端页面进行交互。系统旨在提供一个公平、高效的抽奖功能，支持奖品管理、库存控制和基于概率的抽奖算法。

### **1.2 目标读者**

*   Java 后端开发人员
*   前端开发人员
*   系统运维人员
*   任何希望复现或学习本抽奖系统的人员

### **1.3 系统功能**

*   **奖品管理**: 配置奖品名称、总数量、剩余数量和中奖概率。
*   **抽奖接口**: 提供 RESTful API 供前端或其他系统调用，执行抽奖逻辑。
*   **库存控制**: 确保奖品库存的正确性，防止超卖，使用乐观锁机制。
*   **概率抽奖**: 实现基于预设概率的轮盘赌抽奖算法。
*   **简单前端**: 提供一个基础的网页界面，方便用户点击抽奖并查看结果。

---

## **2. 系统环境与技术栈**

### **2.1 运行环境要求**

*   **操作系统**: Windows 11 (或其他兼容的操作系统)
*   **JDK**: Java Development Kit 17 (或更高版本，推荐 LTS 版本)
*   **Maven**: Apache Maven 3.8.x 或更高版本
*   **MySQL**: MySQL 8.x (或其他兼容的数据库)
*   **IDE**: IntelliJ IDEA (推荐，或其他 Java IDE)
*   **命令行工具**: PowerShell (Windows) 或 Bash/Zsh (Linux/macOS)

### **2.2 技术栈概览**

| 类型   | 技术名称             | 版本     | 描述                                     |
| :----- | :------------------- | :------- | :--------------------------------------- |
| **后端** | Spring Boot          | 3.2.5    | 快速开发框架                             |
|        | MyBatis-Plus         | 3.5.5    | 增强型 MyBatis，简化数据库操作           |
|        | MySQL Connector/J    | 最新版   | MySQL 数据库 JDBC 驱动                   |
|        | Lombok               | 最新版   | 简化 Java Bean 代码                      |
|        | Spring Web           | 随 Spring Boot | 提供 Web 应用能力                        |
|        | Spring Transaction   | 随 Spring Boot | 事务管理                                 |
| **前端** | HTML5                | -        | 页面结构                                 |
|        | CSS3                 | -        | 页面样式                                 |
|        | JavaScript (ES6+)    | -        | 页面交互，调用后端 API                   |
| **工具** | Maven                | 3.8.x+   | 项目构建与依赖管理                       |
|        | Git                  | -        | 版本控制 (未在文档中体现，但开发中常用)  |
|        | Postman/curl         | -        | API 测试工具                             |

---

## **3. 系统架构设计**

本系统采用经典的三层架构模式：表现层 (Controller)、业务逻辑层 (Service) 和数据持久层 (Mapper/DAO)。

### **3.1 架构图**

```mermaid
graph TD
    A[用户浏览器/前端页面] -->|HTTP POST /lottery/draw| B(Controller 层: LotteryController)
    B -->|调用业务方法| C(Service 层: LotteryService)
    C -->|1. 查询可用奖品| D(Mapper 层: PrizeMapper)
    C -->|2. 执行抽奖算法 (内存计算)| C
    C -->|3. 乐观锁扣减库存| D
    D -->|读写数据| E[数据库: MySQL Prize表]
    C -->|返回抽奖结果| B
    B -->|JSON 响应| A
```

### **3.2 各层职责**

*   **Controller 层 (`LotteryController`)**:
    *   接收前端的 HTTP 请求。
    *   调用 Service 层的方法处理业务逻辑。
    *   将 Service 层返回的结果封装成 JSON 响应返回给前端。
    *   不包含复杂的业务逻辑和数据库操作。
*   **Service 层 (`LotteryService`)**:
    *   实现核心业务逻辑，如：获取可用奖品、执行抽奖算法、处理库存扣减。
    *   管理事务。
    *   协调 Controller 层和 Mapper 层的数据交互。
*   **Mapper 层 (`PrizeMapper`)**:
    *   负责与数据库的直接交互，执行 SQL 语句。
    *   继承 MyBatis-Plus 的 `BaseMapper`，提供基础的 CRUD 功能。
    *   包含自定义的乐观锁库存扣减方法。
*   **Entity 层 (`Prize`)**:
    *   定义数据模型，映射数据库表结构。
    *   通过 Lombok 简化 Getter/Setter 等方法的编写。

---

## **4. 数据库设计**

### **4.1 数据库结构**

我们使用 `prize` 表来存储奖品信息。

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

### **4.2 关键字段说明**

*   `id`: 主键，自增。
*   `prize_name`: 奖品名称。
*   `total_quantity`: 奖品总数量。
*   `remaining_quantity`: **奖品剩余数量，用于库存控制。**
*   `probability`: **中奖概率，小数形式 (例如 0.01 表示 1%)。** 所有奖品概率之和可以小于1，剩余部分即为未中奖概率。
*   `version`: **乐观锁版本号，用于并发控制，防止库存超卖。**
*   `create_time`, `update_time`: 记录创建和更新时间。

### **4.3 初始数据 (测试用例)**

请确保在运行应用前，MySQL 数据库 `lottery` 中已创建 `prize` 表，并插入以下测试数据（或根据需要调整）：

```sql
INSERT INTO `prize` (`prize_name`, `total_quantity`, `remaining_quantity`, `probability`, `version`) VALUES
('一等奖：iPhone 15 Pro', 1, 1, 0.0010, 0),
('二等奖：华为 MatePad', 5, 5, 0.0100, 0),
('三等奖：10元优惠券', 100, 100, 0.2000, 0),
('四等奖：1元红包', 1000, 1000, 0.5000, 0),
('谢谢参与', 999999, 999999, 0.2890, 0);
```

---

## **5. 开发过程与代码实现**

### **5.1 项目初始化**

1.  访问 [Spring Initializr](https://start.spring.io/)。
2.  选择项目类型：`Maven Project`。
3.  选择语言：`Java`。
4.  选择 Spring Boot 版本：`3.2.5`。
5.  设置项目元数据 (Group, Artifact 等)。
6.  添加以下依赖：
    *   `Spring Web`
    *   `MySQL Driver`
    *   `Lombok`
    *   `MyBatis-Plus` (搜索 `mybatis-plus-spring-boot3-starter`)
    *   `Spring Boot DevTools` (可选，方便开发热重载)
    *   `Spring Boot Actuator` (可选，提供生产就绪功能)
7.  点击 `Generate` 下载项目压缩包，解压后用 IntelliJ IDEA 导入。

### **5.2 后端核心代码**

#### **5.2.1 `pom.xml` 配置**

关键依赖：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>3.2.5</version>
		<relativePath/>
	</parent>

	<groupId>com.miku</groupId>
	<artifactId>Lottery</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>Lottery</name>
	<description>Lottery</description>
	<properties>
		<java.version>17</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<!-- 关键：MyBatis-Plus 针对 Spring Boot 3 的启动器 -->
		<dependency>
			<groupId>com.baomidou</groupId>
			<artifactId>mybatis-plus-spring-boot3-starter</artifactId>
			<version>3.5.5</version>
		</dependency>
		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
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

#### **5.2.2 `application.properties` 配置**

```properties
spring.application.name=Lottery

server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/lottery?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

mybatis-plus.mapper-locations=classpath:/mapper/*.xml
#mybatis-plus.global-config.db-config.logic-delete-field=flag
```

#### **5.2.3 `LotteryApplication.java` (启动类)**

```java
package com.miku.lottery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.miku.lottery.mapper") // 扫描 Mapper 接口
public class LotteryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LotteryApplication.class, args);
    }
}
```

#### **5.2.4 `Prize.java` (实体类)**

```java
package com.miku.lottery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 奖品实体类
 */
@Data
@TableName("prize")
public class Prize {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String prizeName;
    private Integer totalQuantity;
    private Integer remainingQuantity;
    private BigDecimal probability;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### **5.2.5 `PrizeMapper.java` (Mapper 接口)**

```java
package com.miku.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miku.lottery.entity.Prize;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Prize 表的 Mapper 接口
 */
@Mapper
public interface PrizeMapper extends BaseMapper<Prize> {

    /**
     * 扣减库存（使用乐观锁）
     * @param id 奖品ID
     * @param version 版本号
     * @return 更新的行数，0表示失败，1表示成功
     */
    @Update("UPDATE prize SET remaining_quantity = remaining_quantity - 1, version = version + 1 " +
            "WHERE id = #{id} AND remaining_quantity > 0 AND version = #{version}")
    int deductStock(@Param("id") Long id, @Param("version") Integer version);
}
```

#### **5.2.6 `LotteryService.java` (Service 层)**

```java
package com.miku.lottery.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.miku.lottery.entity.Prize;
import com.miku.lottery.mapper.PrizeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Service
public class LotteryService {

    @Autowired
    private PrizeMapper prizeMapper;

    private final Random random = new Random();

    /**
     * 执行抽奖
     * @return 返回中奖的奖品信息，如果未中奖则返回 null
     */
    @Transactional(rollbackFor = Exception.class)
    public Prize draw() {
        // 1. 获取所有库存大于0的奖品列表
        List<Prize> availablePrizes = prizeMapper.selectList(
                new QueryWrapper<Prize>().gt("remaining_quantity", 0)
        );

        if (availablePrizes.isEmpty()) {
            return null;
        }

        // 2. 计算总概率（可以小于1）
        BigDecimal totalProbability = availablePrizes.stream()
                .map(Prize::getProbability)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 生成一个 0 到 totalProbability 之间的随机数
        BigDecimal randomValue = totalProbability.multiply(new BigDecimal(random.nextDouble()));

        // 4. 轮盘赌算法：确定中奖奖品
        Prize winningPrize = null;
        BigDecimal currentSum = BigDecimal.ZERO;
        for (Prize prize : availablePrizes) {
            currentSum = currentSum.add(prize.getProbability());
            if (randomValue.compareTo(currentSum) < 0) {
                winningPrize = prize;
                break;
            }
        }

        if (winningPrize == null) {
            return null;
        }

        // 6. 核心步骤：使用乐观锁扣减库存
        int affectedRows = prizeMapper.deductStock(winningPrize.getId(), winningPrize.getVersion());

        // 7. 如果扣减失败（通常是高并发导致），说明其他线程已修改，本次抽奖算作未中奖
        if (affectedRows == 0) {
            System.out.println("库存扣减失败，奖品ID: " + winningPrize.getId() + "，版本号: " + winningPrize.getVersion());
            return null;
        }

        // 8. 扣减成功，返回中奖奖品信息
        return winningPrize;
    }
}
```

#### **5.2.7 `LotteryController.java` (Controller 层)**

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

@RestController
@RequestMapping("/lottery")
public class LotteryController {

    @Autowired
    private LotteryService lotteryService;

    /**
     * 抽奖接口
     * @return JSON 格式的抽奖结果
     */
    @PostMapping("/draw")
    public Map<String, Object> drawLottery() {
        Map<String, Object> result = new HashMap<>();
        
        Prize prize = lotteryService.draw();

        if (prize != null) {
            result.put("code", 200);
            result.put("message", "恭喜你，中奖了！");
            result.put("data", prize.getPrizeName());
        } else {
            result.put("code", 404);
            result.put("message", "很遗憾，未中奖，谢谢参与！");
            result.put("data", null);
        }

        return result;
    }
}
```

### **5.3 前端静态页面**

所有前端文件放置在 `src/main/resources/static/` 目录下。

#### **5.3.1 `index.html`**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>幸运大抽奖</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="container">
        <h1>幸运大抽奖</h1>
        <button id="drawButton">点击抽奖</button>
        <p id="resultMessage"></p>
        <p id="prizeDisplay"></p>
    </div>

    <script src="script.js"></script>
</body>
</html>
```

#### **5.3.2 `style.css`**

```css
body {
    font-family: Arial, sans-serif;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    margin: 0;
    background-color: #f0f2f5;
    color: #333;
}

.container {
    background-color: #fff;
    padding: 40px;
    border-radius: 10px;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
    text-align: center;
    width: 90%;
    max-width: 500px;
}

h1 {
    color: #4CAF50;
    margin-bottom: 30px;
}

button {
    background-color: #007bff;
    color: white;
    padding: 15px 30px;
    border: none;
    border-radius: 5px;
    font-size: 1.2em;
    cursor: pointer;
    transition: background-color 0.3s ease;
    margin-bottom: 20px;
}

button:hover {
    background-color: #0056b3;
}

#resultMessage {
    font-size: 1.1em;
    font-weight: bold;
    color: #d9534f;
    margin-top: 20px;
}

#prizeDisplay {
    font-size: 1.5em;
    font-weight: bold;
    color: #4CAF50;
}
```

#### **5.3.3 `script.js`**

```javascript
document.addEventListener('DOMContentLoaded', () => {
    const drawButton = document.getElementById('drawButton');
    const resultMessage = document.getElementById('resultMessage');
    const prizeDisplay = document.getElementById('prizeDisplay');

    drawButton.addEventListener('click', async () => {
        resultMessage.textContent = '抽奖中...';
        prizeDisplay.textContent = '';
        resultMessage.style.color = '#333';

        try {
            const response = await fetch('/lottery/draw', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            const data = await response.json();

            if (data.code === 200) {
                resultMessage.textContent = data.message;
                resultMessage.style.color = '#4CAF50';
                prizeDisplay.textContent = `恭喜您获得：${data.data}`;
            } else {
                resultMessage.textContent = data.message;
                resultMessage.style.color = '#d9534f';
                prizeDisplay.textContent = '';
            }
        } catch (error) {
            console.error('抽奖请求失败:', error);
            resultMessage.textContent = '抽奖失败，请稍后再试。';
            resultMessage.style.color = '#d9534f';
            prizeDisplay.textContent = '';
        }
    });
});
```

---

## **6. 遇到的困难与解决方案总结**

在开发过程中，我们遇到了一系列常见且具有挑战性的问题。以下是详细的记录和解决方案，它们对于未来复现或调试类似问题具有重要参考价值。

### **6.1 困难一：`javax.annotation.Resource` 找不到符号**

*   **问题描述**: 在 Java 17 环境下，使用 `@Resource` 注解时，编译器提示 `找不到符号`。
*   **根本原因**: 从 Java 9 开始，`javax.annotation` 包被从 JSE 标准库中移除。Spring Boot 3.x 默认使用 Java 17，不再包含此包。
*   **解决方案**: 将代码中的 `@Resource` 注解替换为 Spring 框架自有的 `@Autowired` 注解。这是 Spring 推荐的依赖注入方式，且无需额外依赖。
*   **具体操作**:
    *   在 `LotteryController.java` 和 `LotteryService.java` 中：
        *   删除 `import javax.annotation.Resource;`
        *   添加 `import org.springframework.beans.factory.annotation.Autowired;`
        *   将 `@Resource` 修改为 `@Autowired`。
*   **备选方案**: 如果必须使用 `@Resource`，可以在 `pom.xml` 中手动添加 `jakarta.annotation-api` 依赖。但考虑到 `Spring Boot 3.x` 已经全面转向 `jakarta` 命名空间，此方案略显冗余。

### **6.2 困难二：Lombok 注解不生效，找不到 Getter/Setter 方法**

*   **问题描述**: `Prize` 实体类使用了 `@Data` 注解，但编译器报错称找不到 `getPrizeName()`, `getProbability()` 等方法。
*   **根本原因**: Lombok 是一个注解处理器，它在编译阶段生成代码。如果 IDE (如 IntelliJ IDEA) 没有启用“注解处理器”功能，Lombok 就无法在编译前生成这些方法。
*   **解决方案**: 在 IntelliJ IDEA 中启用注解处理器。
*   **具体操作**:
    *   `File` -> `Settings...` (或 `IntelliJ IDEA` -> `Preferences...` on macOS)。
    *   导航到 `Build, Execution, Deployment` -> `Compiler` -> `Annotation Processors`。
    *   勾选 `Enable annotation processing`。
    *   点击 `Apply` 和 `OK`。
    *   执行 `Build` -> `Rebuild Project`。
    *   同时，优化了 `pom.xml` 中 `maven-compiler-plugin` 的配置，移除冗余的 `<configuration>`，让 `Spring Boot Parent` 自动管理 `Lombok` 的注解处理器。
    

### **6.3 困难三：`Invalid value type for attribute 'factoryBeanObjectType': java.lang.String` 错误 (MyBatis-Plus 与 Spring Boot 兼容性)**

这是本次开发中最复杂、最顽固的问题，经历多次尝试才最终解决。

*   **问题描述**: Spring Boot 应用启动时，在初始化 `prizeMapper` Bean 时失败，抛出 `IllegalArgumentException` 或 `BeanDefinitionStoreException`，错误信息指明 `factoryBeanObjectType` 类型不匹配。
*   **根本原因**: 这是 MyBatis-Plus 和 Spring Framework 特定版本之间存在的兼容性问题。
    *   **第一次尝试 (错误版本)**: 初始使用了不存在的 Spring Boot `3.5.4` 版本，导致底层依赖冲突。
    *   **第二次尝试 (版本不兼容)**: 将 Spring Boot 降级到 `3.3.0`，并将 MyBatis-Plus 升级到 `3.5.7`。虽然 `3.5.7` 是最新版，但它与 Spring Framework `6.1.x` 系列（Spring Boot `3.3.0` 和 `3.2.5` 内部使用）在处理 Mapper Bean 定义时仍存在某种微妙的兼容性问题。
    *   **第三次尝试 (彻底清理无效)**: 进行了 Maven 深度清理 (`mvn clean install -U`) 和 IDEA 缓存清理，但问题依然存在，进一步证明是版本间的深层兼容性问题。
*   **最终解决方案**:
    1.  **确定 Spring Boot 版本**: 选用一个稳定且广泛使用的 Spring Boot 3.x 版本，如 `3.2.5`。
    2.  **使用 MyBatis-Plus 针对 Spring Boot 3 的专用启动器**: 将 `mybatis-plus-boot-starter` 替换为 **`mybatis-plus-spring-boot3-starter`**，并使用其兼容版本，如 `3.5.5`。这个启动器是 MyBatis-Plus 官方为 Spring Boot 3.x 量身定制的，解决了与 Spring Framework 6.x 的兼容性问题。
    3.  **彻底清理 Maven 本地仓库缓存**: 手动删除 `.m2/repository/com/baomidou` 文件夹，确保 Maven 重新下载干净的依赖。
    4.  **强制 Maven 更新**: 使用 `mvn clean install -U` 命令强制 Maven 忽略本地缓存并重新解析下载所有依赖。
*   **具体操作**:
    *   `pom.xml` 中：
        ```xml
        <parent>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-parent</artifactId>
            <version>3.2.5</version>
        </parent>
        ```
        
        ```xml
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>3.5.5</version>
        </dependency>
        ```
    *   关闭 IDEA。
    *   删除 `C:\Users\\.m2\repository\com\baomidou` 文件夹。
    *   在 PowerShell 中，导航到项目根目录，执行 `mvn clean install -U`。
    *   重启 IDEA，并 `Invalidate Caches and Restart`。

### **6.4 困难四：`curl` 命令无法获取响应 / `502 Bad Gateway`**

*   **问题描述**: 后端应用已启动，但使用 `curl` 命令测试接口时，没有返回 JSON 响应，或者返回 `502 Bad Gateway` 错误。
*   **根本原因**: `curl` 命令受到系统环境变量 `http_proxy` 的影响，强制通过代理服务器访问 `localhost`。代理服务器无法正确处理对 `localhost` 的请求，导致连接失败。
*   **解决方案**: 告诉 `curl` 在访问 `localhost` 或 `127.0.0.1` 时跳过代理。
*   **具体操作**:
    *   在 PowerShell 中执行 `curl` 命令时，添加 `--noproxy` 参数：
        ```powershell
        curl -v -X POST --noproxy "localhost,127.0.0.1" http://localhost:8080/lottery/draw
        ```
    *   （可选）或者设置临时的环境变量：
        ```powershell
        $env:NO_PROXY = "localhost,127.0.0.1"
        curl -v -X POST http://localhost:8080/lottery/draw
        ```

---

## **7. 部署与运行**

### **7.1 数据库准备**

1.  确保 MySQL 服务正在运行。
2.  创建 `lottery` 数据库。
3.  执行 **4.3 初始数据 (测试用例)** 中提供的 SQL 语句，创建 `prize` 表并插入测试数据。

### **7.2 后端应用启动**

1.  打开 IntelliJ IDEA，导入 `Lottery` Maven 项目。
2.  确保 `pom.xml` 和 `application.properties` 配置正确。
3.  点击 `LotteryApplication.java` 文件上的绿色运行按钮，或在 Maven 面板中执行 `spring-boot:run` 命令。
4.  观察控制台日志，确认看到 `Tomcat started on port 8080` 字样，表示后端服务已成功启动。

### **7.3 前端页面访问**

1.  确保后端服务已启动。
2.  打开任意现代浏览器。
3.  在地址栏输入 `http://localhost:8080/` 或 `http://localhost:8080/index.html`。
4.  页面加载后，点击“点击抽奖”按钮，查看抽奖结果。

---

## **8. 测试指南**

### **8.1 API 接口测试 (使用 `curl`)**

1.  打开 PowerShell 命令行。
2.  执行以下命令，多次尝试，观察返回的 JSON 结果和数据库中 `remaining_quantity` 的变化。
    ```powershell
    curl -v -X POST --noproxy "localhost,127.0.0.1" http://localhost:8080/lottery/draw
    ```
3.  **预期结果**: 每次请求返回 JSON 格式的抽奖结果，可能是中奖信息 (`code: 200`) 或未中奖信息 (`code: 404`)。数据库中被抽中的奖品库存会相应减少。

### **8.2 前端页面测试**

1.  在浏览器中访问 `http://localhost:8080/`。
2.  点击页面上的“点击抽奖”按钮。
3.  **预期结果**: 页面上的“结果消息”和“奖品显示”区域会根据抽奖结果动态更新。

---

## **9. 未来增强与优化**

当前系统已具备核心抽奖功能，但作为一个生产级系统，仍有许多可优化和扩展的空间：

*   **高并发优化**: 引入 Redis 缓存奖品库存，使用消息队列进行异步奖品发放。
*   **用户系统**: 实现用户认证、抽奖次数限制（例如每用户每天一次）。
*   **中奖记录**: 记录所有中奖用户的详细信息，并提供历史查询功能。
*   **管理后台**: 开发一个 Web 界面，用于管理奖品、查看抽奖活动数据。
*   **安全性**: 添加接口认证、防刷机制（如验证码、IP 限制）。
*   **可配置化**: 将抽奖活动的时间、参与条件等参数配置化，方便运营调整。
*   **日志与监控**: 完善日志系统，集成 ELK Stack 或 Prometheus/Grafana 进行系统监控。
*   **国际化**: 支持多语言显示。

---

至此，幸运大抽奖系统的第一版开发说明书已完成。希望这份详细的文档能为你未来的学习和开发提供坚实的基础！
















# **抽奖系统后端代码及原理详解**

本项目是一个基于 Spring Boot 的抽奖系统后端，采用经典的三层架构，并结合 MyBatis-Plus 进行数据持久化操作。系统核心在于实现一个并发安全的、基于概率的抽奖逻辑。

## **1. 项目核心结构概览**

```
Lottery/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/miku/lottery/
│   │   │       ├── LotteryApplication.java  // 应用启动类
│   │   │       ├── controller/              // 表现层，处理HTTP请求
│   │   │       │   └── LotteryController.java
│   │   │       ├── entity/                  // 实体层，数据模型
│   │   │       │   └── Prize.java
│   │   │       ├── mapper/                  // 数据持久层，数据库操作接口
│   │   │       │   └── PrizeMapper.java
│   │   │       └── service/                 // 业务逻辑层，核心业务处理
│   │   │           └── LotteryService.java
│   │   └── resources/
│   │       ├── application.properties     // 应用配置文件
│   │       └── static/                    // 静态资源目录（前端页面）
│   │           ├── index.html
│   │           ├── style.css
│   │           └── script.js
│   └── test/
├── pom.xml                                // Maven 项目对象模型文件
```

## **2. 详细代码讲解与原理分析**

我们将从应用程序的入口点开始，逐步深入到各个组件。

### **2.1 `LotteryApplication.java` - 应用程序启动类**

*   **路径**: `src/main/java/com/miku/lottery/LotteryApplication.java`
*   **作用**: 这是 Spring Boot 应用程序的入口点，负责启动 Spring 容器，加载配置，并扫描组件。

```java
package com.miku.lottery;

import org.mybatis.spring.annotation.MapperScan; // 导入 MapperScan 注解
import org.springframework.boot.SpringApplication; // 导入 SpringApplication 类，用于启动 Spring Boot 应用
import org.springframework.boot.autoconfigure.SpringBootApplication; // 导入 SpringBootApplication 注解

@SpringBootApplication // 核心注解：声明这是一个 Spring Boot 应用程序
@MapperScan("com.miku.lottery.mapper") // 关键注解：扫描指定包下的 Mapper 接口
public class LotteryApplication {

    public static void main(String[] args) {
        // SpringApplication.run() 方法用于启动 Spring Boot 应用程序
        // 它会执行以下操作：
        // 1. 创建 Spring 应用上下文 (ApplicationContext)。
        // 2. 注册和扫描所有 Spring 组件 (如 @Controller, @Service, @Component 等)。
        // 3. 自动配置 (根据 classpath 中的依赖自动配置数据库连接、Web服务器等)。
        // 4. 启动嵌入式 Web 服务器 (如 Tomcat)。
        SpringApplication.run(LotteryApplication.class, args);
    }
}
```

*   **`package com.miku.lottery;`**: 定义了该类所属的包。
*   **`import org.mybatis.spring.annotation.MapperScan;`**: 导入了 `MapperScan` 注解。
*   **`import org.springframework.boot.SpringApplication;`**: 导入了 `SpringApplication` 类，这是 Spring Boot 应用程序的启动器。
*   **`import org.springframework.boot.autoconfigure.SpringBootApplication;`**: 导入了 `SpringBootApplication` 注解。
*   **`@SpringBootApplication`**:
    *   **原理**: 这是一个组合注解，它包含了 `@Configuration`（声明这是一个配置类）、`@EnableAutoConfiguration`（开启 Spring Boot 的自动配置机制，根据 classpath 中的依赖自动配置 Bean，如数据库连接池、Web 服务器等）和 `@ComponentScan`（默认扫描当前包及其子包下的所有 Spring 组件，如 `@Controller`, `@Service`, `@Repository` 等）。
    *   **作用**: 简化了 Spring Boot 应用的配置，让开发者可以专注于业务逻辑。
*   **`@MapperScan("com.miku.lottery.mapper")`**:
    *   **原理**: 这个注解是 MyBatis-Spring (MyBatis 与 Spring 集成) 提供的，用于告诉 Spring 容器去扫描指定包路径下的所有被 `@Mapper` 注解标记的接口，并为它们创建代理实现类（即 Mapper Bean），然后将这些 Mapper Bean 注册到 Spring 的 IoC 容器中。
    *   **作用**: 使得在 Service 层可以通过 `@Autowired` 直接注入 `PrizeMapper` 接口，而无需手动编写 Mapper 的实现类。这是实现数据持久层与业务逻辑层解耦的关键。
*   **`public static void main(String[] args)`**: Java 应用程序的入口方法。
*   **`SpringApplication.run(LotteryApplication.class, args);`**: 启动 Spring Boot 应用程序。它会初始化 Spring 上下文，加载所有配置，启动嵌入式 Tomcat 服务器，并使应用程序开始监听 HTTP 请求。

### **2.2 `Prize.java` - 奖品实体类**

*   **路径**: `src/main/java/com/miku/lottery/entity/Prize.java`
*   **作用**: 定义了奖品的 Java 对象模型，它与数据库中的 `prize` 表结构相对应。

```java
package com.miku.lottery.entity;

import com.baomidou.mybatisplus.annotation.IdType; // 导入 MyBatis-Plus 的 ID 类型注解
import com.baomidou.mybatisplus.annotation.TableId; // 导入 MyBatis-Plus 的表主键注解
import com.baomidou.mybatisplus.annotation.TableName; // 导入 MyBatis-Plus 的表名注解
import com.baomidou.mybatisplus.annotation.Version; // 导入 MyBatis-Plus 的乐观锁版本注解
import lombok.Data; // 导入 Lombok 的 Data 注解

import java.math.BigDecimal; // 导入 BigDecimal 类，用于精确处理小数（概率）
import java.time.LocalDateTime; // 导入 LocalDateTime 类，用于处理日期时间

/**
 * 奖品实体类，映射数据库 prize 表
 */
@Data // Lombok 注解：自动生成 getter, setter, equals, hashCode, toString 方法
@TableName("prize") // MyBatis-Plus 注解：声明该实体类映射到数据库中的 "prize" 表
public class Prize {

    @TableId(type = IdType.AUTO) // MyBatis-Plus 注解：声明 id 字段是主键，并设置为数据库自增类型
    private Long id; // 奖品唯一标识符

    private String prizeName; // 奖品名称，例如“iPhone 15 Pro”

    private Integer totalQuantity; // 奖品总库存量

    private Integer remainingQuantity; // 奖品当前剩余库存量，抽中后会减少

    private BigDecimal probability; // 奖品的中奖概率，使用 BigDecimal 确保浮点数精度

    @Version // MyBatis-Plus 注解：声明 version 字段是乐观锁版本号
    private Integer version; // 乐观锁版本号，每次更新时会自动递增

    private LocalDateTime createTime; // 记录创建时间

    private LocalDateTime updateTime; // 记录最后更新时间
}
```

*   **`package com.miku.lottery.entity;`**: 定义了该实体类所属的包。
*   **`import ...`**: 导入了 Lombok 和 MyBatis-Plus 相关的注解以及 Java 内置的日期时间和高精度数值类。
*   **`@Data` (Lombok)**:
    *   **原理**: Lombok 是一个代码生成库。`@Data` 注解在编译时会自动为类中的所有非静态字段生成 `getter` 和 `setter` 方法，以及 `equals()`, `hashCode()`, `toString()` 方法。这大大减少了样板代码的编写。
    *   **作用**: 使得 `Prize` 类代码简洁，专注于字段定义。
*   **`@TableName("prize")` (MyBatis-Plus)**:
    *   **原理**: MyBatis-Plus 的注解，用于指定实体类所映射的数据库表名。如果类名与表名一致（遵循驼峰命名转下划线规则），可以省略此注解。
    *   **作用**: 明确指定 `Prize` 实体对应 `prize` 表。
*   **`@TableId(type = IdType.AUTO)` (MyBatis-Plus)**:
    *   **原理**: 标记字段为主键，`IdType.AUTO` 表示主键由数据库自动生成（例如 MySQL 的 `AUTO_INCREMENT`）。
    *   **作用**: 告诉 MyBatis-Plus `id` 字段是主键，并且是自增的。
*   **`@Version` (MyBatis-Plus)**:
    *   **原理**: 这是实现**乐观锁**的关键。当进行更新操作时，MyBatis-Plus 会自动在 SQL 的 `WHERE` 子句中添加 `AND version = currentVersion` 条件，并在 `SET` 子句中添加 `version = version + 1`。如果更新成功（`affectedRows > 0`），说明版本号匹配且数据未被其他线程修改。如果更新失败（`affectedRows = 0`），则说明在读取数据后，有其他线程已经修改了该行数据，导致版本号不匹配，从而避免了并发更新冲突。
    *   **作用**: 确保在高并发场景下，奖品库存的扣减是线程安全的，不会出现超卖。

### **2.3 `PrizeMapper.java` - 奖品数据操作接口**

*   **路径**: `src/main/java/com/miku/lottery/mapper/PrizeMapper.java`
*   **作用**: 定义了与 `prize` 表进行数据库交互的方法。

```java
package com.miku.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper; // 导入 MyBatis-Plus 的基础 Mapper 接口
import com.miku.lottery.entity.Prize; // 导入 Prize 实体类
import org.apache.ibatis.annotations.Mapper; // 导入 MyBatis 的 Mapper 注解
import org.apache.ibatis.annotations.Param; // 导入 MyBatis 的 Param 注解
import org.apache.ibatis.annotations.Update; // 导入 MyBatis 的 Update 注解

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
     * @return 更新的行数。如果返回 0，表示扣减失败（如库存不足或版本号不匹配，即发生了并发冲突）；如果返回 1，表示成功扣减。
     */
    @Update("UPDATE prize SET remaining_quantity = remaining_quantity - 1, version = version + 1 " +
            "WHERE id = #{id} AND remaining_quantity > 0 AND version = #{version}")
    int deductStock(@Param("id") Long id, @Param("version") Integer version);
}
```

*   **`package com.miku.lottery.mapper;`**: 定义了该接口所属的包。
*   **`import ...`**: 导入了 MyBatis-Plus 的 `BaseMapper` 和 MyBatis 的注解。
*   **`@Mapper`**:
    *   **原理**: MyBatis 框架的注解，与 `LotteryApplication` 中的 `@MapperScan` 配合使用。它告诉 MyBatis 和 Spring 这个接口是一个 Mapper 接口，Spring 会为其创建代理对象，并注册到 IoC 容器中。
    *   **作用**: 使得 `PrizeMapper` 接口能够被 Spring 容器识别并注入到其他组件（如 Service 层）。
*   **`extends BaseMapper<Prize>`**:
    *   **原理**: 这是 MyBatis-Plus 的核心功能之一。`BaseMapper` 提供了大量常用的单表 CRUD 方法，如 `selectById`, `selectList`, `insert`, `updateById`, `deleteById` 等，无需编写 SQL 语句即可直接使用。
    *   **作用**: 大幅简化了数据持久层的开发工作量。
*   **`@Update(...)`**:
    *   **原理**: MyBatis 提供的注解，用于直接在接口方法上定义 SQL 更新语句。
    *   **作用**: 定义了自定义的 `deductStock` 方法的 SQL 逻辑。
*   **`int deductStock(@Param("id") Long id, @Param("version") Integer version)`**:
    *   **方法作用**: 这是实现乐观锁库存扣减的核心方法。它接收奖品ID和当前版本号作为参数。
    *   **`@Param`**: 用于为方法参数指定名称，这样在 SQL 语句中可以通过 `#{参数名}` 的形式引用。
    *   **SQL 语句分析**:
        *   `UPDATE prize SET remaining_quantity = remaining_quantity - 1, version = version + 1`: 这是更新操作，将剩余库存减 1，并将版本号加 1。
        *   `WHERE id = #{id} AND remaining_quantity > 0 AND version = #{version}`: 这是 `WHERE` 子句，包含了三个关键条件：
            1.  `id = #{id}`: 确保更新的是指定 ID 的奖品。
            2.  `remaining_quantity > 0`: 确保只有当奖品还有库存时才进行扣减，避免负库存。
            3.  `version = #{version}`: **乐观锁的核心**。只有当数据库中该奖品的版本号与传入的版本号一致时，更新才会被执行。如果其他线程在此之前更新了该奖品（导致版本号改变），那么这个条件将不满足，本次更新将失败（`affectedRows` 为 0）。
    *   **返回值**: 返回受影响的行数。在乐观锁场景下，如果返回 1，表示扣减成功；如果返回 0，表示扣减失败（通常是并发冲突）。

### **2.4 `LotteryService.java` - 抽奖业务逻辑服务类**

*   **路径**: `src/main/java/com/miku/lottery/service/LotteryService.java`
*   **作用**: 封装了抽奖的核心业务逻辑，包括奖品获取、概率计算、中奖判定和库存扣减。

```java
package com.miku.lottery.service;

import org.springframework.beans.factory.annotation.Autowired; // 导入 Spring 的 Autowired 注解
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper; // 导入 MyBatis-Plus 的查询条件构造器
import com.miku.lottery.entity.Prize; // 导入 Prize 实体类
import com.miku.lottery.mapper.PrizeMapper; // 导入 PrizeMapper 接口
import org.springframework.stereotype.Service; // 导入 Spring 的 Service 注解
import org.springframework.transaction.annotation.Transactional; // 导入 Spring 的 Transactional 注解

import java.math.BigDecimal; // 导入 BigDecimal 类
import java.util.List; // 导入 List 集合
import java.util.Random; // 导入 Random 类，用于生成随机数

/**
 * 抽奖业务逻辑服务类
 * 负责处理抽奖的核心业务流程，包括奖品获取、概率计算、中奖判定和库存扣减。
 */
@Service // 声明这是一个 Spring Service 组件，会被 Spring 容器扫描并管理
public class LotteryService {

    @Autowired // 自动注入 PrizeMapper 实例
    private PrizeMapper prizeMapper;

    private final Random random = new Random(); // 创建一个 Random 实例，用于生成随机数

    /**
     * 执行抽奖的核心方法。
     * 该方法是事务性的，确保库存扣减和中奖判定的一致性。
     *
     * @return 返回中奖的奖品实体信息，如果未中奖或抽奖失败（如并发冲突），则返回 null。
     */
    @Transactional(rollbackFor = Exception.class) // 声明该方法为事务性方法。
                                                  // rollbackFor = Exception.class 表示任何 Exception 都会导致事务回滚，
                                                  // 确保数据一致性（如抽中但扣库存失败时，整个操作回滚）。
    public Prize draw() {
        // 1. 获取所有库存大于0的奖品列表
        // 使用 QueryWrapper 构造查询条件：remaining_quantity > 0
        List<Prize> availablePrizes = prizeMapper.selectList(
                new QueryWrapper<Prize>().gt("remaining_quantity", 0)
        );

        // 如果查询结果为空，表示当前没有可用的奖品，直接返回 null
        if (availablePrizes.isEmpty()) {
            System.out.println("当前没有可用奖品。"); // 打印日志方便调试
            return null;
        }

        // 2. 计算所有可用奖品的总概率
        // 使用 Java 8 Stream API 遍历 availablePrizes 列表，
        // 映射（map）每个 Prize 对象的 probability 字段，
        // 然后使用 reduce 操作将所有 BigDecimal 概率累加起来，初始值为 BigDecimal.ZERO。
        BigDecimal totalProbability = availablePrizes.stream()
                .map(Prize::getProbability) // 获取奖品的概率
                .reduce(BigDecimal.ZERO, BigDecimal::add); // 累加所有概率

        // 如果所有奖品的总概率为0，也无法进行抽奖，返回 null
        if (totalProbability.compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("所有可用奖品的总概率为0，无法抽奖。");
            return null;
        }

        // 3. 生成一个 0 到 totalProbability 之间的随机数
        // random.nextDouble() 生成一个 [0.0, 1.0) 之间的双精度浮点数，
        // 乘以 totalProbability，得到在总概率范围内的随机数。
        // 使用 BigDecimal 包装以保持精度。
        BigDecimal randomValue = totalProbability.multiply(new BigDecimal(random.nextDouble()));

        // 4. 轮盘赌算法：遍历奖品列表，确定中奖奖品
        Prize winningPrize = null; // 用于存储最终中奖的奖品
        BigDecimal currentSum = BigDecimal.ZERO; // 累积概率和，用于判断随机数落在哪个奖品区间

        // 遍历可用奖品列表
        for (Prize prize : availablePrizes) {
            currentSum = currentSum.add(prize.getProbability()); // 将当前奖品的概率累加到 currentSum
            // 判断随机数是否落在当前奖品的概率区间内
            // 如果 randomValue 小于 currentSum，说明随机数落在了当前奖品或其之前的某个奖品的概率范围内
            // 由于是顺序遍历，一旦满足条件，当前 prize 就是中奖奖品。
            if (randomValue.compareTo(currentSum) < 0) {
                winningPrize = prize; // 确定中奖奖品
                break; // 找到中奖奖品后立即退出循环，提高效率
            }
        }

        // 5. 如果根据概率算法没有抽中任何奖品（winningPrize 仍为 null）
        // 这通常发生在所有奖品概率之和小于1，且随机数落在了“未中奖”区间，或者所有奖品都已无库存。
        if (winningPrize == null) {
            System.out.println("随机数未落在任何奖品区间，未中奖。");
            return null; // 返回 null 表示未中奖
        }

        // 6. 核心步骤：尝试使用乐观锁扣减中奖奖品的库存
        // 调用 PrizeMapper 的 deductStock 方法，传入中奖奖品的 ID 和当前版本号。
        // 这个方法会尝试原子性地更新数据库，并在更新时检查版本号是否匹配。
        int affectedRows = prizeMapper.deductStock(winningPrize.getId(), winningPrize.getVersion());

        // 7. 检查库存扣减结果
        if (affectedRows == 0) {
            // 如果 affectedRows 为 0，表示扣减失败。
            // 这通常是由于在高并发情况下，其他线程先一步修改了该奖品的库存或版本号。
            // 在这种情况下，本次抽奖应视为未中奖（因为库存实际已被占用），并打印日志以便调试。
            System.out.println("库存扣减失败，奖品ID: " + winningPrize.getId() + "，版本号: " + winningPrize.getVersion() + "。可能是并发冲突导致。");
            // 事务会自动回滚，确保不会出现数据不一致。
            return null; // 返回 null 表示本次抽奖未成功
        }

        // 8. 库存扣减成功，返回中奖奖品信息
        System.out.println("恭喜抽中奖品: " + winningPrize.getPrizeName());
        return winningPrize;
    }
}
```

*   **`@Service`**:
    *   **原理**: Spring 的注解，标记这是一个业务逻辑组件。它会被 `@ComponentScan` 扫描到，并注册为 Spring Bean。
    *   **作用**: 使得 `LotteryService` 能够被 `@Autowired` 注入到其他组件（如 `LotteryController`）。
*   **`@Autowired`**:
    *   **原理**: Spring 的依赖注入注解。它会自动查找并注入 Spring IoC 容器中类型匹配的 Bean。在这里，它会自动注入 `PrizeMapper` 的代理实现类。
    *   **作用**: 实现了 `LotteryService` 对 `PrizeMapper` 的依赖，解耦了业务逻辑与数据访问层。
*   **`private final Random random = new Random();`**:
    *   **原理**: `Random` 类用于生成伪随机数。`final` 关键字表示该对象引用不可更改。
    *   **作用**: 为抽奖算法提供随机性。
*   **`@Transactional(rollbackFor = Exception.class)`**:
    *   **原理**: Spring 的事务管理注解。当方法被调用时，Spring 会开启一个数据库事务。如果方法执行过程中抛出 `Exception.class` 定义的异常（包括运行时异常和检查型异常），事务会自动回滚；如果没有异常抛出，事务则会提交。
    *   **作用**: 保证抽奖过程中数据库操作的原子性。例如，如果成功抽中了奖品，但在扣减库存时因为并发冲突 (`deductStock` 返回 0) 导致本次抽奖失败，那么整个事务会回滚，确保奖品不会被“凭空”抽走，也不会导致数据不一致。
*   **抽奖算法 (轮盘赌算法)**:
    *   **步骤 1: 获取可用奖品**: 从数据库查询 `remaining_quantity > 0` 的奖品。
    *   **步骤 2: 计算总概率**: 累加所有可用奖品的 `probability`。
    *   **步骤 3: 生成随机数**: 生成一个 `0` 到 `totalProbability` 之间的随机数。
    *   **步骤 4: 轮盘选择**: 遍历奖品列表，累加每个奖品的概率。当随机数小于当前累积概率时，就选中了当前奖品。这模拟了一个“概率轮盘”，随机数落在哪个区间，哪个奖品就中奖。
    *   **步骤 5: 处理未中奖**: 如果随机数没有落在任何奖品的概率区间（即所有奖品概率之和小于1，且随机数落在了剩余的“空闲”区间），则视为未中奖。
    *   **步骤 6: 乐观锁扣减库存**: 调用 `PrizeMapper.deductStock()` 方法。这是最关键的一步，它将中奖判定和库存扣减（乐观锁）合并到一个原子操作中。
    *   **步骤 7: 处理并发冲突**: 如果 `deductStock` 返回 0，说明在当前线程读取奖品信息和尝试扣减库存之间，有其他线程已经成功扣减了该奖品。此时，本次抽奖视为失败，并回滚事务。
    *   **步骤 8: 返回结果**: 扣减成功则返回中奖奖品，否则返回 `null`。








#### **轮盘赌算法 (Roulette Wheel Selection) 详解**

#### **1. 算法概述与目的**

轮盘赌算法是一种基于概率的随机选择方法，常用于需要根据不同权重（或概率）进行选择的场景，例如：

*   **抽奖系统**: 根据奖品的不同中奖概率来决定用户抽中哪个奖品。
*   **遗传算法**: 选择个体进行交叉和变异，高适应度的个体有更大的几率被选中。
*   **负载均衡**: 根据服务器的权重分配请求。

它的核心思想是将每个选项（在本例中是奖品）的概率映射到轮盘（或一条线段）上的一段区域，区域越大，被选中的概率就越高。然后，通过一个随机数来模拟“转动轮盘”，随机数落在哪段区域，对应的选项就被选中。

#### **2. 算法原理**

想象一个圆形的轮盘，它的周长被分割成若干扇形区域。每个扇形区域的大小与它所代表的选项的中奖概率成正比。

例如，如果有三个奖品：
*   A 奖品：概率 20%
*   B 奖品：概率 30%
*   C 奖品：概率 50%

那么轮盘会被分成 20%、30%、50% 的区域。
算法步骤如下：
1.  **计算总权重（总概率）**：将所有选项的权重（概率）相加，得到一个总和。
2.  **生成随机数**：生成一个介于 0 到总权重（不包含总权重）之间的随机数。
3.  **遍历选择**：从第一个选项开始，依次累加它们的权重。当累加的权重首次**大于或等于**生成的随机数时，当前遍历到的选项就是被选中的选项。

#### **3. 代码中的轮盘赌算法实现**

我们来逐行分析 `LotteryService.java` 中的 `draw()` 方法，看它是如何实现轮盘赌算法的。

```java
public Prize draw() {
    // 1. 获取所有库存大于0的奖品列表
    List<Prize> availablePrizes = prizeMapper.selectList(
            new QueryWrapper<Prize>().gt("remaining_quantity", 0)
    );

    // 如果没有可用奖品，直接返回 null
    if (availablePrizes.isEmpty()) {
        System.out.println("当前没有可用奖品。");
        return null;
    }

    // 2. 计算所有可用奖品的总概率
    BigDecimal totalProbability = availablePrizes.stream()
            .map(Prize::getProbability) // 获取每个奖品的概率
            .reduce(BigDecimal.ZERO, BigDecimal::add); // 累加所有概率

    // 如果总概率为0，也无法抽奖
    if (totalProbability.compareTo(BigDecimal.ZERO) == 0) {
        System.out.println("所有可用奖品的总概率为0，无法抽奖。");
        return null;
    }

    // 3. 生成一个 0 到 totalProbability 之间的随机数
    // random.nextDouble() 生成一个 [0.0, 1.0) 之间的双精度浮点数
    // totalProbability.multiply(...) 将随机数映射到总概率的区间内
    BigDecimal randomValue = totalProbability.multiply(new BigDecimal(random.nextDouble()));

    // 4. 轮盘赌算法：遍历奖品列表，确定中奖奖品
    Prize winningPrize = null; // 存储中奖奖品
    BigDecimal currentSum = BigDecimal.ZERO; // 累积概率和

    for (Prize prize : availablePrizes) {
        currentSum = currentSum.add(prize.getProbability()); // 累加当前奖品的概率
        // 如果随机数小于当前累积概率和，则表示随机数落入当前奖品的概率区间，即中奖
        if (randomValue.compareTo(currentSum) < 0) {
            winningPrize = prize; // 确定中奖奖品
            break; // 找到中奖奖品后立即退出循环
        }
    }

    // 5. 如果根据概率没有抽中任何奖品（winningPrize 仍为 null）
    // 这发生在所有奖品概率之和小于1，且 randomValue 落在了“未中奖”区间。
    if (winningPrize == null) {
        System.out.println("随机数未落在任何奖品区间，未中奖。");
        return null;
    }

    // ... 后续的库存扣减和事务处理逻辑 ...
    // ... (这部分是乐观锁和数据库操作，不属于轮盘赌算法本身) ...
    int affectedRows = prizeMapper.deductStock(winningPrize.getId(), winningPrize.getVersion());
    if (affectedRows == 0) {
        System.out.println("库存扣减失败，奖品ID: " + winningPrize.getId() + "，版本号: " + winningPrize.getVersion() + "。可能是并发冲突导致。");
        return null;
    }
    System.out.println("恭喜抽中奖品: " + winningPrize.getPrizeName());
    return winningPrize;
}
```

**详细步骤分析：**

1.  **获取可用奖品**:
    *   `List<Prize> availablePrizes = prizeMapper.selectList(new QueryWrapper<Prize>().gt("remaining_quantity", 0));`
    *   **作用**: 这是算法的基础。只有有库存的奖品才有可能被抽中。`QueryWrapper` 用于构建 SQL 条件 `WHERE remaining_quantity > 0`。
    *   **原理**: 确保了抽奖的有效性，避免抽中已无库存的奖品。

2.  **计算总概率 ( `totalProbability` )**:
    *   `BigDecimal totalProbability = availablePrizes.stream().map(Prize::getProbability).reduce(BigDecimal.ZERO, BigDecimal::add);`
    *   **作用**: 累加所有 `availablePrizes` 的 `probability` 字段，得到一个总和。这个总和就是“轮盘”的总大小。
    *   **原理**:
        *   使用 `BigDecimal` 而不是 `double` 或 `float` 来处理概率，是为了**避免浮点数计算的精度问题**。在金融或概率计算中，精度至关重要。
        *   `totalProbability` 不一定等于 1。如果所有奖品的概率之和小于 1，那么 `1 - totalProbability` 的部分就是“未中奖”的概率。如果等于 1，则每次必中某个奖品。

3.  **生成随机数 ( `randomValue` )**:
    *   `BigDecimal randomValue = totalProbability.multiply(new BigDecimal(random.nextDouble()));`
    *   **作用**: `random.nextDouble()` 生成一个 `[0.0, 1.0)` 范围内的随机数。然后将其乘以 `totalProbability`，使得 `randomValue` 的范围是 `[0.0, totalProbability)`。
    *   **原理**: 这个 `randomValue` 就相当于“轮盘指针”停止的位置。它决定了最终会落在轮盘的哪个区域。

4.  **遍历选择 (轮盘模拟)**:
    *   `Prize winningPrize = null;`
    *   `BigDecimal currentSum = BigDecimal.ZERO;`
    *   `for (Prize prize : availablePrizes) { ... }`
    *   **作用**: 模拟轮盘的旋转和指针停止。
    *   **原理**:
        *   `currentSum` 变量是关键。它代表了从轮盘起点（0）开始，到当前遍历到的奖品结束时的累积概率。
        *   例如，如果奖品 A 概率 0.1，奖品 B 概率 0.2，奖品 C 概率 0.7（总概率为 1.0）：
            *   第一次循环 (prize = A): `currentSum` 变为 0.1。如果 `randomValue < 0.1`，则 A 中奖。
            *   第二次循环 (prize = B): `currentSum` 变为 0.1 + 0.2 = 0.3。如果 `0.1 <= randomValue < 0.3`，则 B 中奖。
            *   第三次循环 (prize = C): `currentSum` 变为 0.3 + 0.7 = 1.0。如果 `0.3 <= randomValue < 1.0`，则 C 中奖。
        *   `randomValue.compareTo(currentSum) < 0`：这是 `randomValue < currentSum` 的 `BigDecimal` 安全比较方式。一旦随机数落入当前奖品对应的概率区间，该奖品即被选中，循环立即 `break`。

5.  **处理未中奖情况**:
    *   `if (winningPrize == null) { ... return null; }`
    *   **作用**: 如果循环结束后 `winningPrize` 仍然是 `null`，这表示 `randomValue` 大于所有奖品的累积概率总和（即 `totalProbability`）。这通常发生在奖品总概率小于 1 的情况下，那么随机数就落在了“未中奖”的区域。
    *   **原理**: 确保了即使没有抽中任何具体奖品，系统也能给出“未中奖”的明确结果。在你的数据库中添加一个“谢谢参与”的奖品，并为其设置概率，是管理这种“未中奖”情况的常见且推荐做法，因为它将“未中奖”也视为一个特殊的“奖品”。

#### **4. 算法的优缺点**

*   **优点**:
    *   **公平性**: 严格按照设定的概率进行选择，概率越高，被选中的机会越大。
    *   **简单易懂**: 实现逻辑直观，容易理解。
    *   **灵活性**: 奖品的概率总和可以小于、等于或大于 1（如果大于 1，则需要归一化处理，但本实现中 `totalProbability` 可以大于 1，只要 `randomValue` 落在其中一个区间即可）。

*   **缺点**:
    *   **性能**: 对于奖品数量非常庞大的情况（例如成千上万种奖品），每次抽奖都需要遍历整个奖品列表，效率会降低。但在大多数抽奖场景中，奖品种类有限，这种性能影响可以忽略不计。
    *   **精度**: 依赖于浮点数计算。虽然这里使用了 `BigDecimal` 解决了 Java `double` 类型的精度问题，但在其他语言或不当使用浮点数时，可能会出现微小的累积误差。

#### **5. 与抽奖系统的结合**

轮盘赌算法是抽奖系统实现概率分配的核心。它确保了运营者可以根据自己的需求，灵活地设置不同奖品的中奖几率，从而控制活动的吸引力和成本。结合后续的乐观锁库存扣减，共同构成了这个抽奖系统的核心。









### **2.5 `LotteryController.java` - 抽奖接口控制器**

*   **路径**: `src/main/java/com/miku/lottery/controller/LotteryController.java`
*   **作用**: 负责接收前端的 HTTP 请求，调用 `LotteryService` 处理业务逻辑，并将结果封装成 JSON 响应返回给前端。

```java
package com.miku.lottery.controller;

import org.springframework.beans.factory.annotation.Autowired; // 导入 Spring 的 Autowired 注解
import com.miku.lottery.entity.Prize; // 导入 Prize 实体类
import com.miku.lottery.service.LotteryService; // 导入 LotteryService 服务类
import org.springframework.web.bind.annotation.PostMapping; // 导入 Spring Web 的 PostMapping 注解
import org.springframework.web.bind.annotation.RequestMapping; // 导入 Spring Web 的 RequestMapping 注解
import org.springframework.web.bind.annotation.RestController; // 导入 Spring Web 的 RestController 注解

import java.util.HashMap; // 导入 HashMap 类，用于构建 JSON 响应
import java.util.Map; // 导入 Map 接口

/**
 * 抽奖系统的 RESTful API 控制器
 * 负责处理来自前端的 HTTP 请求，并返回 JSON 格式的响应。
 */
@RestController // 声明这是一个 RESTful Controller，其所有方法的返回值都会被自动转换为 JSON 或 XML 格式
@RequestMapping("/lottery") // 映射所有方法到 "/lottery" 路径下
public class LotteryController {

    @Autowired // 自动注入 LotteryService 实例
    private LotteryService lotteryService;

    /**
     * 抽奖接口。
     * 该方法处理对 "/lottery/draw" 路径的 POST 请求。
     *
     * @return 返回包含抽奖结果的 JSON 响应。
     *         结构为 Map<String, Object>，最终会被 Spring 自动序列化为 JSON。
     *         - code: 状态码，200 表示成功中奖，404 表示未中奖或抽奖失败。
     *         - message: 提示信息（如“恭喜你，中奖了！”或“很遗憾，未中奖，谢谢参与！”）。
     *         - data: 如果中奖，则为中奖奖品的名称；否则为 null。
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

*   **`@RestController`**:
    *   **原理**: 这是 `@Controller` 和 `@ResponseBody` 的组合注解。`@Controller` 标记类为 Spring MVC 的控制器，`@ResponseBody` 则表示该类所有方法的返回值都会直接写入 HTTP 响应体，通常以 JSON 或 XML 格式（由 Spring 的消息转换器处理）。
    *   **作用**: 简化了 RESTful API 的开发，无需手动进行 JSON 序列化。
*   **`@RequestMapping("/lottery")`**:
    *   **原理**: 标记类或方法处理的请求路径。放在类上表示所有方法的前缀路径。
    *   **作用**: 将 `LotteryController` 中的所有接口都映射到 `/lottery` 路径下。
*   **`@PostMapping("/draw")`**:
    *   **原理**: 标记方法处理 HTTP `POST` 请求，并指定具体的子路径。
    *   **作用**: 将 `drawLottery()` 方法映射到 `/lottery/draw` 的 `POST` 请求。
*   **`@Autowired private LotteryService lotteryService;`**:
    *   **原理**: 自动注入 `LotteryService` 的实例。
    *   **作用**: `LotteryController` 依赖 `LotteryService` 来执行业务逻辑，通过注入实现解耦。
*   **`public Map<String, Object> drawLottery()`**:
    *   **返回值**: `Map<String, Object>`，Spring 会自动将其转换为 JSON 响应。
    *   **逻辑**: 调用 `lotteryService.draw()` 获取抽奖结果，然后根据结果构建一个包含 `code`、`message` 和 `data` 的 Map，作为 JSON 返回给前端。

### **2.6 `LotteryApplication.java` - 应用配置文件**

*   **路径**: `src/main/resources/application.properties`
*   **作用**: 存储应用程序的配置信息，如服务器端口、数据库连接、MyBatis-Plus 配置等。

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

# MyBatis-Plus Mapper XML 文件位置（如果使用 XML 配置 SQL）
# classpath:/mapper/*.xml 表示在 classpath 下的 mapper 文件夹中查找所有 .xml 文件。
# 本项目主要使用注解，此配置可能不会被严格要求，但保留是良好实践。
mybatis-plus.mapper-locations=classpath:/mapper/*.xml
# MyBatis-Plus 全局配置：逻辑删除字段（本项目 prize 表无此字段，故注释）
# mybatis-plus.global-config.db-config.logic-delete-field=flag
```

*   **`spring.application.name`**: 定义 Spring Boot 应用的名称，用于日志记录和监控。
*   **`server.port`**: 指定内嵌 Tomcat 服务器监听的端口。
*   **`spring.datasource.*`**: 数据库连接配置。Spring Boot 会自动读取这些配置来创建数据源（DataSource）Bean，供 MyBatis-Plus 使用。
    *   `useUnicode=true&characterEncoding=utf-8`: 确保数据库传输和存储中文不会乱码。
    *   `serverTimezone=Asia/Shanghai`: 指定服务器时区，避免时间存储和读取时出现时区问题。
*   **`mybatis-plus.mapper-locations`**: 配置 MyBatis-Plus 扫描 Mapper XML 文件的位置。即使主要使用注解，有时也会配合 XML 来编写复杂 SQL。
*   **`#mybatis-plus.global-config.db-config.logic-delete-field=flag`**: 这是一个被注释掉的配置，用于 MyBatis-Plus 的逻辑删除功能。由于 `prize` 表没有逻辑删除字段，因此将其注释掉。

## **3. 前端代码讲解**

前端代码是纯静态的 HTML、CSS 和 JavaScript 文件，用于提供用户界面和与后端 API 交互。

### **3.1 `index.html` - 页面结构**

*   **路径**: `src/main/resources/static/index.html`
*   **作用**: 定义了抽奖页面的基本结构，包括标题、按钮和结果显示区域。

```html
<!DOCTYPE html>
<html lang="zh-CN"> <!-- 声明文档类型为 HTML5，语言为中文 -->
<head>
    <meta charset="UTF-8"> <!-- 设置字符编码为 UTF-8，确保中文显示正常 -->
    <meta name="viewport" content="width=device-width, initial-scale=1.0"> <!-- 适配移动设备视图 -->
    <title>幸运大抽奖</title> <!-- 页面标题 -->
    <link rel="stylesheet" href="style.css"> <!-- 引入外部 CSS 样式表 -->
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

### **3.2 `style.css` - 页面样式**

*   **路径**: `src/main/resources/static/style.css`
*   **作用**: 定义了页面的布局、颜色、字体等视觉样式，使页面美观。

```css
body {
    font-family: Arial, sans-serif; /* 设置字体 */
    display: flex; /* 使用 Flex 布局 */
    justify-content: center; /* 水平居中 */
    align-items: center; /* 垂直居中 */
    min-height: 100vh; /* 最小高度为视口高度，确保内容垂直居中 */
    margin: 0; /* 移除默认外边距 */
    background-color: #f0f2f5; /* 背景颜色 */
    color: #333; /* 默认文本颜色 */
}

.container {
    background-color: #fff; /* 容器背景色 */
    padding: 40px; /* 内边距 */
    border-radius: 10px; /* 圆角边框 */
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1); /* 阴影效果 */
    text-align: center; /* 文本居中 */
    width: 90%; /* 宽度占父容器的90% */
    max-width: 500px; /* 最大宽度限制 */
}

h1 {
    color: #4CAF50; /* 标题颜色（绿色） */
    margin-bottom: 30px; /* 标题下边距 */
}

button {
    background-color: #007bff; /* 按钮背景色（蓝色） */
    color: white; /* 按钮文本颜色 */
    padding: 15px 30px; /* 按钮内边距 */
    border: none; /* 无边框 */
    border-radius: 5px; /* 按钮圆角 */
    font-size: 1.2em; /* 按钮字体大小 */
    cursor: pointer; /* 鼠标悬停时显示手型光标 */
    transition: background-color 0.3s ease; /* 背景色变化时的过渡效果 */
    margin-bottom: 20px; /* 按钮下边距 */
}

button:hover {
    background-color: #0056b3; /* 按钮悬停时的背景色 */
}

#resultMessage {
    font-size: 1.1em; /* 结果消息字体大小 */
    font-weight: bold; /* 字体加粗 */
    color: #d9534f; /* 默认颜色为红色（表示未中奖） */
    margin-top: 20px; /* 上边距 */
}

#prizeDisplay {
    font-size: 1.5em; /* 奖品显示字体大小 */
    font-weight: bold; /* 字体加粗 */
    color: #4CAF50; /* 奖品显示颜色为绿色（表示中奖） */
}
```

### **3.3 `script.js` - 页面交互逻辑**

*   **路径**: `src/main/resources/static/script.js`
*   **作用**: 包含了前端的 JavaScript 逻辑，用于监听按钮点击事件，发送 HTTP 请求到后端 API，并根据响应更新页面内容。

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
            resultMessage.textContent = '抽奖失败，请稍后再试。'; // 向用户显示通用的失败消息
            resultMessage.style.color = '#d9534f'; // 设为红色
            prizeDisplay.textContent = ''; // 清空奖品显示
        }
    });
});
```

## **4. 整体请求流程 (Overall Request Flow)**

1.  **用户操作**: 用户在浏览器中访问 `http://localhost:8080/`，加载 `index.html` 页面。
2.  **前端事件**: 用户点击“点击抽奖”按钮，`script.js` 中的事件监听器被触发。
3.  **发送请求**: `script.js` 使用 `fetch` API 向 `http://localhost:8080/lottery/draw` 发送一个 `POST` 请求。
4.  **后端路由**: Spring Boot 应用程序接收到请求，`@RequestMapping("/lottery")` 和 `@PostMapping("/draw")` 将请求路由到 `LotteryController` 的 `drawLottery()` 方法。
5.  **业务逻辑调用**: `LotteryController` 通过 `@Autowired` 注入的 `lotteryService` 对象，调用其 `draw()` 方法。
6.  **数据访问与核心逻辑**:
    *   `LotteryService` 的 `draw()` 方法首先通过 `prizeMapper` 查询数据库中所有有库存的奖品。
    *   根据查询到的奖品列表及其概率，执行轮盘赌算法，确定是否中奖以及中何种奖品。
    *   如果中奖，`LotteryService` 会调用 `prizeMapper.deductStock()` 方法，尝试使用**乐观锁**原子性地扣减中奖奖品的库存。
    *   如果扣减成功，`draw()` 方法返回中奖的 `Prize` 对象；如果扣减失败（并发冲突）或未中奖，则返回 `null`。
7.  **事务管理**: `LotteryService.draw()` 方法上的 `@Transactional` 确保了整个抽奖过程（查询、概率计算、库存扣减）是一个原子操作。如果库存扣减失败，事务会自动回滚，保证数据的一致性。
8.  **后端响应**: `LotteryController` 根据 `lotteryService.draw()` 的返回结果，构建一个 `Map` 对象，其中包含 `code`、`message` 和 `data` 等信息。由于 `@RestController` 的作用，这个 `Map` 会被 Spring 自动序列化为 JSON 格式的 HTTP 响应。
9.  **前端处理响应**: `script.js` 接收到后端返回的 JSON 响应，解析数据，并根据 `code` 字段更新页面上的 `resultMessage` 和 `prizeDisplay` 区域，向用户展示最终的抽奖结果。

---

通过这份详细的讲解，你应该对抽奖系统的每一部分代码、它们是如何协同工作以及背后的原理有了深入的理解。
















