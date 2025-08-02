

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





