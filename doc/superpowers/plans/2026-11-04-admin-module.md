# RouteGo-admin 第一批实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按设计文档把 admin 15 张表做成可用的后台业务模块，并落地统一缓存 key、缓存问题治理、脱敏、验证码与操作日志。

**Architecture:** 在 `RouteGo-common` 增加常量/缓存/脱敏/验证码/日志 SPI，admin 模块在此之上实现认证、用户、角色、菜单、权限、字典、参数、公告、文件、日志、OAuth 的 Service/Controller。读单对象与字典走 Redis，写操作事务提交后精确失效缓存；手机号明文入库、输出脱敏。

**Tech Stack:** Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.7, Sa-Token 1.44, Redisson, Redis, JUnit 5 + Mockito。

**Spec:** `doc/superpowers/specs/2026-11-04-admin-module-design.md`

## Global Constraints

- 缓存 key 严格为 `[模块]:[表/控制器]:[方法]:[参数]`，全部集中在 `com.rg.common.constant.CacheKeyConstants`，禁止散落字符串。
- 不做字段加密、不做盲索引、不引入布隆过滤器；空值缓存 60 秒。
- 所有缓存必须带 TTL，TTL 加 ±10% 抖动；写后删缓存，删除在事务提交后执行。
- 响应统一 `Result<T>` / `PageResult<T>`；分页参数继承 `PageQuery`。
- 所有后台接口路径以 `/admin/**` 开头（应用 context-path 为 `/api`）；写接口必须加 `@SaCheckPermission` 或 `@SaCheckLogin`。
- 密码永不出现在 VO/日志；`SysUser.password` 加 `@JsonIgnore`。
- 业务编码（username/role_code/permission_code/dict_type/config_key）不可修改；逻辑删除后编码仍占用，查重包含 `deleted=1`。
- 更新接口使用 `version` 乐观锁，冲突返回 409。
- 本期不新增第三方依赖。
- 本机 Maven 命令统一用下面的前缀（anaconda 的 cygpath 会破坏 mvnw）：

```bash
cd D:/PROJECT/RouteGo/RouteGo
export PATH="/usr/bin:$PATH"
export JAVA_HOME="/d/Program Files/java/java21"
MVN="./mvnw -o -q"
```

## Review Focus

- 缓存里出现密码或脱敏值：命中 `USER_BY_ID` 后改密必须仍能读到密码哈希，手机号必须在 HTTP 输出时才脱敏。
- 逻辑删除后重建同编码：唯一索引会撞，必须返回 409 而不是 500。
- 验证码重放/大小写/过期：必须一次性消费，错误大小写可通过，过期返回 400。
- 角色停用后旧会话：必须清用户级缓存并踢下线，不能继续用旧权限 30 分钟。
- 掩码回写：前端把 `138****8000` 原样提交时不能覆盖数据库明文手机号。

---

### Task 1: 缓存 key 常量与空值哨兵

**Files:**
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/constant/CacheKeyConstants.java`
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/cache/CacheNullValue.java`
- Test: `RouteGo/RouteGo-common/src/test/java/com/rg/common/constant/CacheKeyConstantsTest.java`

**Interfaces:**
- Consumes: 无
- Produces:
  - `CacheKeyConstants.Admin.CAPTCHA` 等本计划全部 key 模板（`String`）
  - `CacheKeyConstants.Lock.CACHE_REBUILD`
  - `CacheNullValue.INSTANCE`（`Serializable` 单例）

- [ ] **Step 1: 写失败测试**

```java
@Test
void keyTemplatesFollowModuleTableMethodFormat() {
    assertThat(CacheKeyConstants.Admin.USER_BY_ID).isEqualTo("admin:SysUser:getById:%s");
    assertThat(CacheKeyConstants.Admin.DICT_DATA_BY_TYPE).isEqualTo("admin:SysDictData:listByType:%s");
    assertThat(CacheKeyConstants.Lock.CACHE_REBUILD).isEqualTo("lock:cache:rebuild:%s");
    assertThat(String.format(CacheKeyConstants.Admin.USER_BY_ID, 7)).isEqualTo("admin:SysUser:getById:7");
}

@Test
void nullSentinelIsSingletonSerializable() {
    assertThat(CacheNullValue.INSTANCE).isSameAs(CacheNullValue.INSTANCE);
    assertThat(CacheNullValue.INSTANCE).isInstanceOf(Serializable.class);
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `PATH... $MVN -pl RouteGo-common -am test -Dtest=CacheKeyConstantsTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败，类不存在。

- [ ] **Step 3: 实现常量和哨兵**

`CacheKeyConstants` 为 `final` 类、私有构造，内部静态类 `Admin`、`Catalog`、`Trade`、`Customer`、`Operations`、`Lock`。Admin 常量按设计文档 §3.1 的 20 个 key 全部落地；其余模块先放空内部类。

`CacheNullValue` 实现 `Serializable`，`public static final CacheNullValue INSTANCE = new CacheNullValue();`，私有构造。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add RouteGo/RouteGo-common/src/main/java/com/rg/common/constant RouteGo/RouteGo-common/src/main/java/com/rg/common/cache RouteGo/RouteGo-common/src/test/java/com/rg/common/constant
git commit -m "feat(common): add centralized cache key constants and null sentinel"
```

---

### Task 2: 脱敏注解与 Jackson 序列化器

**Files:**
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/sensitive/Sensitive.java`
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/sensitive/SensitiveType.java`
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/sensitive/SensitiveSerializer.java`
- Test: `RouteGo/RouteGo-common/src/test/java/com/rg/common/sensitive/SensitiveSerializerTest.java`

**Interfaces:**
- Consumes: 无
- Produces:
  - `@Sensitive(SensitiveType.PHONE)` 字段注解
  - `SensitiveType` 枚举：`PHONE/EMAIL/NAME/ID_CARD/BANK_CARD`

- [ ] **Step 1: 写失败测试**

```java
record Fixture(
    @Sensitive(SensitiveType.PHONE) String phone,
    @Sensitive(SensitiveType.EMAIL) String email,
    @Sensitive(SensitiveType.NAME) String name,
    @Sensitive(SensitiveType.ID_CARD) String idCard,
    @Sensitive(SensitiveType.BANK_CARD) String bankCard,
    String plain) {}

@Test
void masksAllTypes() throws Exception {
    var json = new ObjectMapper().writeValueAsString(
        new Fixture("13812348000", "alice@example.com", "张三", "110101199001011234",
                    "6222021234567890123", "visible"));
    assertThat(json).contains("\"phone\":\"138****8000\"");
    assertThat(json).contains("\"email\":\"a***@example.com\"");
    assertThat(json).contains("\"name\":\"张*\"");
    assertThat(json).contains("\"idCard\":\"110***********1234\"");
    assertThat(json).contains("\"bankCard\":\"**** **** **** 0123\"");
    assertThat(json).contains("\"plain\":\"visible\"");
}

@Test
void nullAndShortValuesPassThrough() throws Exception {
    var json = new ObjectMapper().writeValueAsString(new Fixture(null, "", "李", "123", null, null));
    assertThat(json).contains("\"phone\":null");
    assertThat(json).contains("\"email\":\"\"");
    assertThat(json).contains("\"name\":\"李\"");
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `... test -Dtest=SensitiveSerializerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败。

- [ ] **Step 3: 实现注解和序列化器**

`@Sensitive` 使用 `@JacksonAnnotationsInside` + `@JsonSerialize(using = SensitiveSerializer.class)`，`SensitiveType value()`。

`SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer`：无参构造默认 `PHONE`；`createContextual` 从 `BeanProperty.getAnnotation(Sensitive.class)` 取类型；`serialize` 按设计文档 §4.1 规则掩码；`null`、空串、长度不足原样输出。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add RouteGo/RouteGo-common/src/main/java/com/rg/common/sensitive RouteGo/RouteGo-common/src/test/java/com/rg/common/sensitive
git commit -m "feat(common): add @Sensitive field masking with Jackson serializer"
```

---

### Task 3: 通用缓存模板（空值/击穿/雪崩）

**Files:**
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/cache/CacheHelper.java`
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/cache/RedisCacheHelper.java`
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/autoconfigure/RouteGoCacheAutoConfiguration.java`
- Modify: `RouteGo/RouteGo-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `RouteGo/RouteGo-common/src/main/java/com/rg/common/redis/RedisService.java`（增加 `getAndDelete`）
- Test: `RouteGo/RouteGo-common/src/test/java/com/rg/common/cache/RedisCacheHelperTest.java`

**Interfaces:**
- Consumes: `RedisService`、`RedissonClient`、`CacheNullValue`
- Produces:
  - `<T> T CacheHelper.getOrLoad(String key, Duration ttl, Supplier<T> loader)`
  - `void CacheHelper.evict(String... keys)`
  - `void CacheHelper.evictAfterCommit(String... keys)`
  - `RedisService.getAndDelete(String key)`
  - 自动配置 Bean `CacheHelper`（`@ConditionalOnBean({RedisService, RedissonClient})`）

- [ ] **Step 1: 写失败测试**

```java
@ExtendWith(MockitoExtension.class)
class RedisCacheHelperTest {
    @Mock RedisService redis;
    @Mock RedissonClient redisson;
    @Mock RLock lock;
    RedisCacheHelper helper;

    @BeforeEach void setUp() { helper = new RedisCacheHelper(redis, redisson); }

    @Test void returnsCachedValueWithoutLoading() {
        when(redis.get("k")).thenReturn("v");
        assertThat(helper.getOrLoad("k", Duration.ofMinutes(1), () -> "db")).isEqualTo("v");
        verifyNoInteractions(redisson);
    }

    @Test void cachesNullFor60Seconds() throws Exception {
        when(redis.get("k")).thenReturn(null);
        when(redisson.getLock("lock:cache:rebuild:k")).thenReturn(lock);
        when(lock.tryLock(3, 10, TimeUnit.SECONDS)).thenReturn(true);
        assertThat(helper.getOrLoad("k", Duration.ofMinutes(1), () -> null)).isNull();
        verify(redis).set("k", CacheNullValue.INSTANCE, Duration.ofSeconds(60));
        verify(lock).unlock();
    }

    @Test void ttlJitterStaysWithinTenPercent() {
        for (int i = 0; i < 100; i++) {
            assertThat(helper.jitter(Duration.ofSeconds(1000)))
                .isBetween(Duration.ofSeconds(900), Duration.ofSeconds(1100));
        }
    }
}
```

同时测试：`CacheNullValue` 命中时返回 `null` 且不调用 loader；`evictAfterCommit` 在无事务时立即删除。

- [ ] **Step 2: 运行测试确认失败**

Run: `... test -Dtest=RedisCacheHelperTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败。

- [ ] **Step 3: 实现**

`RedisCacheHelper` 构造注入 `RedisService`、`RedissonClient`。`getOrLoad`：读缓存 → 空值哨兵返回 null → 抢 `Lock.CACHE_REBUILD` 锁 → 二次读 → loader → null 写 60 秒哨兵，否则按抖动 TTL 写入 → finally 解锁。抢锁失败先重读缓存，最多等 500ms，仍无则直接回源（不写缓存）。

`jitter` 方法包可见，返回 `ttl * (0.9 + ThreadLocalRandom.nextDouble() * 0.2)`。

`evictAfterCommit`：`TransactionSynchronizationManager.isSynchronizationActive()` 为真时注册 `afterCommit` 删除，否则立即删除。

`RedisService.getAndDelete` 使用 `redisTemplate.opsForValue().getAndDelete(key)`。

新建 `RouteGoCacheAutoConfiguration`，在 `RedisService` 与 `RedissonClient` 都存在时注册 `CacheHelper`；把类名追加到 `AutoConfiguration.imports`。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add RouteGo/RouteGo-common/src/main/java/com/rg/common/cache RouteGo/RouteGo-common/src/main/java/com/rg/common/redis RouteGo/RouteGo-common/src/main/java/com/rg/common/autoconfigure/RouteGoCacheAutoConfiguration.java RouteGo/RouteGo-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports RouteGo/RouteGo-common/src/test/java/com/rg/common/cache
git commit -m "feat(common): add cache helper with null caching, mutex rebuild and ttl jitter"
```

---

### Task 4: 验证码生成与一次性校验

**Files:**
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/captcha/CaptchaResult.java`
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/captcha/CaptchaGenerator.java`
- Create: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/service/CaptchaService.java`
- Create: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/vo/CaptchaResponse.java`
- Test: `RouteGo/RouteGo-common/src/test/java/com/rg/common/captcha/CaptchaGeneratorTest.java`
- Test: `RouteGo/RouteGo-modules/RouteGo-admin/src/test/java/com/rg/admin/service/CaptchaServiceTest.java`

**Interfaces:**
- Consumes: `RedisService.getAndDelete`、`CacheKeyConstants.Admin.CAPTCHA`
- Produces:
  - `record CaptchaResult(String code, byte[] png)`
  - `CaptchaGenerator.generate()` → `CaptchaResult`
  - `CaptchaResponse CaptchaService.generate()`
  - `void CaptchaService.verify(String uuid, String code)`（失败抛 `AdminException(400, ...)`）

- [ ] **Step 1: 写失败测试**

```java
@Test void generatesFourCharsAndValidPng() throws Exception {
    CaptchaResult r = new CaptchaGenerator().generate();
    assertThat(r.code()).hasSize(4).matches("[A-HJ-NP-Z2-9]{4}");
    BufferedImage img = ImageIO.read(new ByteArrayInputStream(r.png()));
    assertThat(img.getWidth()).isEqualTo(120);
    assertThat(img.getHeight()).isEqualTo(40);
}
```

```java
@Test void verifyConsumesOnceAndIsCaseInsensitive() {
    when(redis.getAndDelete("admin:Auth:captcha:u1")).thenReturn("AB3D");
    service.verify("u1", "ab3d");
    verify(redis).getAndDelete("admin:Auth:captcha:u1");
}

@Test void verifyFailsWhenExpired() {
    when(redis.getAndDelete("admin:Auth:captcha:u2")).thenReturn(null);
    assertThatThrownBy(() -> service.verify("u2", "AB3D"))
        .isInstanceOf(AdminException.class);
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `... test -Dtest=CaptchaGeneratorTest,CaptchaServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败。

- [ ] **Step 3: 实现**

`CaptchaGenerator`：字符集 `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`，`SecureRandom` 取 4 位；`BufferedImage(120,40,TYPE_INT_RGB)`，渐变背景、每字符随机旋转 ±25°、5 条干扰线、30 个噪点；`ImageIO.write(..., "png", baos)`。

`CaptchaService.generate()`：生成后写 `String.format(CAPTCHA, uuid)`，TTL 2 分钟，返回 `uuid` + Base64；`verify` 用 `getAndDelete` 原子消费，比较时 `equalsIgnoreCase`，uuid/code 为空返回 400。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add RouteGo/RouteGo-common/src/main/java/com/rg/common/captcha RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/service/CaptchaService.java RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/vo/CaptchaResponse.java RouteGo/RouteGo-common/src/test/java/com/rg/common/captcha RouteGo/RouteGo-modules/RouteGo-admin/src/test/java/com/rg/admin/service/CaptchaServiceTest.java
git commit -m "feat(common): add 4-char captcha generator and one-time verification"
```

---

### Task 5: 日志脱敏、@OperLog 与客户端 IP

**Files:**
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/annotation/BusinessType.java`
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/annotation/OperLog.java`
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/log/LogSanitizer.java`
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/log/OperLogRecord.java`
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/log/OperLogRecorder.java`
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/aspect/OperLogAspect.java`
- Create: `RouteGo/RouteGo-common/src/main/java/com/rg/common/util/IpUtils.java`
- Modify: `RouteGo/RouteGo-common/src/main/java/com/rg/common/aspect/LogAspect.java`（参数先过 `LogSanitizer`）
- Test: `RouteGo/RouteGo-common/src/test/java/com/rg/common/log/LogSanitizerTest.java`
- Test: `RouteGo/RouteGo-common/src/test/java/com/rg/common/util/IpUtilsTest.java`

**Interfaces:**
- Consumes: `OperLog`、`BusinessType`、`ObjectMapper`
- Produces:
  - `@OperLog(title, type)`
  - `BusinessType { INSERT(1), UPDATE(2), DELETE(3), QUERY(4), EXPORT(5), OTHER(6) }`，`getCode()`
  - `LogSanitizer.toJson(Object, int maxLength)`、`LogSanitizer.maskAndTruncate(String, int)`
  - `OperLogRecorder.record(OperLogRecord)`
  - `IpUtils.resolveClientIp(HttpServletRequest, List<String> trustedProxies)`

- [ ] **Step 1: 写失败测试**

```java
@Test void masksSensitiveKeysAndTruncates() {
    String json = "{\"username\":\"a\",\"password\":\"p@ss\",\"phone\":\"13812348000\"}";
    String out = LogSanitizer.maskAndTruncate(json, 200);
    assertThat(out).doesNotContain("p@ss").doesNotContain("13812348000")
                   .contains("\"password\":\"***\"").contains("\"phone\":\"***\"");
}

@Test void truncatesLongText() {
    assertThat(LogSanitizer.maskAndTruncate("x".repeat(100), 10)).hasSize(10);
}
```

```java
@Test void ignoresForwardedForWhenPeerNotTrusted() {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setRemoteAddr("10.0.0.5");
    req.addHeader("X-Forwarded-For", "1.2.3.4");
    assertThat(IpUtils.resolveClientIp(req, List.of("10.0.0.1"))).isEqualTo("10.0.0.5");
}

@Test void usesLastTrustedHop() {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setRemoteAddr("10.0.0.1");
    req.addHeader("X-Forwarded-For", "1.2.3.4, 10.0.0.9");
    assertThat(IpUtils.resolveClientIp(req, List.of("10.0.0.1"))).isEqualTo("10.0.0.9");
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `... test -Dtest=LogSanitizerTest,IpUtilsTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败。

- [ ] **Step 3: 实现**

`LogSanitizer`：用正则 `"(password|token|secret|phone|idCard|bankCard)"\s*:\s*"[^"]*"` 替换为 `"$1":"***"`；超长截断并追加 `...`；JSON 序列化失败返回 `String.valueOf(value)` 同样截断。

`IpUtils`：直接对端不在 `trustedProxies` 时返回 `getRemoteAddr()`；否则解析 `X-Forwarded-For`，从右向左跳过可信代理，返回第一个不可信地址。

`OperLogAspect`：`@Around("@annotation(operLog)")`，从 `RequestContextHolder` 取请求，采集操作人（`StpUtil.isLogin()` 时取 ID 与 `getSession().get("username")`），参数用 `LogSanitizer.toJson(args, 2000)`，结果同理；成功 `status=1`，异常 `status=0` 并记录 message 后原样抛出；`ObjectProvider<OperLogRecorder>` 无实现时只打印 warn。

同时改现有 `LogAspect`：打印参数前调用 `LogSanitizer.maskAndTruncate`，避免密码/手机号进控制台日志；删除无意义的超长分隔符输出。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add RouteGo/RouteGo-common/src/main/java/com/rg/common/annotation RouteGo/RouteGo-common/src/main/java/com/rg/common/log RouteGo/RouteGo-common/src/main/java/com/rg/common/aspect RouteGo/RouteGo-common/src/main/java/com/rg/common/util/IpUtils.java RouteGo/RouteGo-common/src/test/java/com/rg/common/log RouteGo/RouteGo-common/src/test/java/com/rg/common/util
git commit -m "feat(common): add oper-log annotation, sanitizer and trusted-proxy IP resolution"
```

---

### Task 6: 实体乐观锁、Mapper 修正与用户缓存模型

**Files:**
- Modify: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/entity/SysUser.java`
- Modify: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/entity/SysRole.java`
- Modify: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/entity/SysMenu.java`
- Modify: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/mapper/SysPermissionMapper.java`
- Modify: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/mapper/SysRoleMapper.java`
- Create: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/model/SysUserCache.java`
- Create: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/mapper/AdminRelationMapper.java`
- Test: `RouteGo/RouteGo-modules/RouteGo-admin/src/test/java/com/rg/admin/mapper/MapperContractTest.java`

**Interfaces:**
- Consumes: MyBatis-Plus
- Produces:
  - `SysUser/SysRole/SysMenu.version` 带 `@Version`
  - `SysPermissionMapper.selectPermissionCodesByUserId(Long)` 过滤角色状态
  - `AdminRelationMapper`：`selectUserIdsByRoleId(Long)`、`selectUserIdsByPermissionId(Long)`、`selectUserIdsByMenuId(Long)`、`selectRoleIdsByMenuId(Long)`、`selectRoleIdsByPermissionId(Long)`、`countUsersByRoleId(Long)`、`countRolesByPermissionId(Long)`
  - `SysUserCache`（无密码、明文 phone/email、含 version/status/deleted）

- [ ] **Step 1: 写失败测试**

```java
@Test void versionFieldsAreAnnotated() throws Exception {
    for (Class<?> c : List.of(SysUser.class, SysRole.class, SysMenu.class)) {
        assertThat(c.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }
}

@Test void permissionSqlFiltersRoleState() throws Exception {
    String sql = SysPermissionMapper.class
        .getMethod("selectPermissionCodesByUserId", Long.class)
        .getAnnotation(Select.class).value()[0];
    assertThat(sql).contains("JOIN sys_role").contains("r.status = 1").contains("r.deleted = 0");
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `... -pl RouteGo-modules/RouteGo-admin -am test -Dtest=MapperContractTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 断言失败。

- [ ] **Step 3: 实现**

- 三个实体 `version` 字段加 `com.baomidou.mybatisplus.annotation.Version`。
- `SysPermissionMapper` SQL 增加 `JOIN sys_role r ON r.id = rp.role_id` 与 `r.deleted=0 AND r.status=1`。
- 新建 `AdminRelationMapper`（放在 `com.rg.admin.mapper`，随包扫描）用 `@Select` 实现上述 7 个方法，全部过滤 `deleted=0` 与角色/权限启用状态；菜单相关方法从 `sys_role_menu`、`sys_user_role` 反查。
- `SysUserCache` 用 Lombok `@Data`，字段：id、username、nickname、avatar、phone、email、status、version、deleted；不包含 password；不带 `@Sensitive`。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/entity RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/mapper RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/model RouteGo/RouteGo-modules/RouteGo-admin/src/test/java/com/rg/admin/mapper
git commit -m "fix(admin): enable optimistic locking and align permission queries with role state"
```

---

### Task 7: 认证（验证码、登录锁定、登录日志、改密）

**Files:**
- Modify: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/dto/LoginRequest.java`
- Create: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/dto/ChangePasswordRequest.java`
- Modify: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/service/AuthService.java`
- Modify: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/service/impl/AuthServiceImpl.java`
- Modify: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/controller/AuthController.java`
- Modify: `.../service/SysLoginLogService.java`、`.../service/impl/SysLoginLogServiceImpl.java`（增加 `record` 异步写入）
- Modify: `RouteGo/RouteGo-app/src/main/java/com/rg/app/RouteGoApplication.java`（`@EnableAsync`）
- Modify: `RouteGo/RouteGo-app/src/main/java/com/rg/app/config/SecurityProperties.java`（`trustedProxies`）
- Modify: `RouteGo/RouteGo-app/src/main/resources/application.yaml`（`routego.security.trusted-proxies: []`）
- Test: `RouteGo/RouteGo-modules/RouteGo-admin/src/test/java/com/rg/admin/service/AuthServiceImplTest.java`

**Interfaces:**
- Consumes: `CaptchaService.verify`、`CacheKeyConstants.Admin.*`、`AdminRelationMapper`、`IpUtils`、`CacheHelper`
- Produces:
  - `LoginRequest` 增加 `captchaUuid`、`captchaCode`
  - `AuthService.changePassword(String oldPassword, String newPassword)`
  - `SysLoginLogService.record(String username, String ip, boolean success, String msg)`（`@Async`）
  - `AuthController`：`GET /admin/auth/captcha`、`POST /login`、`POST /logout`、`GET /me`、`PUT /password`

- [ ] **Step 1: 写失败测试**

```java
@Test void rejectsWrongCaptchaBeforePasswordCheck() {
    doThrow(new AdminException(400, "验证码错误")).when(captchaService).verify("u", "zzzz");
    assertThatThrownBy(() -> service.login(req("u", "zzzz"), "1.1.1.1"))
        .isInstanceOf(AdminException.class);
    verifyNoInteractions(passwordEncoder);
}

@Test void locksAfterFiveFailures() {
    when(captchaService...). // 通过验证码
    when(userService.lambdaQuery()...).thenReturn(user);
    when(passwordEncoder.matches(any(), any())).thenReturn(false);
    for (int i = 0; i < 5; i++) assertThrows(...);
    verify(redis).set(startsWith("admin:Auth:loginLock:alice"), any(), eq(Duration.ofMinutes(10)));
}

@Test void dummyHashRunsWhenUserMissing() {
    when(...).thenReturn(null);
    assertThrows(...);
    verify(passwordEncoder).matches(eq("pwd"), eq(AuthServiceImpl.DUMMY_HASH));
}

@Test void successClearsFailureKeysAndWritesLoginLog() {
    ...
    verify(redis).delete(List.of("admin:Auth:loginFail:alice", "admin:Auth:loginLock:alice"));
    verify(loginLogService).record("alice", "1.1.1.1", true, "登录成功");
}

@Test void changePasswordReadsHashFromDbNotCache() {
    try (var stp = mockStatic(StpUtil.class)) {
        stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(userWithPassword("$2a$10$hash"));
        when(passwordEncoder.matches("old", "$2a$10$hash")).thenReturn(true);
        service.changePassword("old", "NewPass123");
        verify(userMapper).selectById(1L);
        verifyNoInteractions(cacheHelper);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `... -pl RouteGo-modules/RouteGo-admin -am test -Dtest=AuthServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败。

- [ ] **Step 3: 实现**

`AuthServiceImpl.login` 顺序：

1. `captchaService.verify(uuid, code)`
2. 查 `LOGIN_LOCK`，存在抛 429
3. 按 username 查 `sys_user`（`deleted=0`）；不存在时用常量 `DUMMY_HASH`（启动时 `passwordEncoder.encode("routego-dummy")`）做一次 `matches`
4. 密码不匹配：`increment(LOGIN_FAIL)`，首次设置 10 分钟 TTL，`>=5` 时写 `LOGIN_LOCK` 10 分钟；写失败日志；抛 401
5. 状态为 0：写失败日志；抛 403
6. `StpUtil.login`，`StpUtil.getSession().set("username", user.getUsername())`
7. 定向更新 `last_login_time/ip`（用 `LambdaUpdateWrapper`，不带 version）
8. 删除失败计数/锁定；写成功日志；返回 `LoginResult`

`changePassword`：当前用户 ID 查库拿哈希 → 校验旧密码 → 校验强度 → BCrypt 更新（带 version）→ `StpUtil.logout(userId)`。

`AuthController` 更新 springdoc 注解，补 captcha 与 password；`resolveClientIp` 改用 `IpUtils` + `SecurityProperties.getTrustedProxies()`。

`SysLoginLogServiceImpl.record` 用 `@Async` 直接保存 `sys_login_log`，浏览器/OS 从 `User-Agent` 简单解析，异常吞掉只记日志。`RouteGoApplication` 加 `@EnableAsync`。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add RouteGo/RouteGo-modules/RouteGo-admin RouteGo/RouteGo-app/src/main/java/com/rg/app
git commit -m "feat(admin): captcha login, attempt lockout, login log and password change"
```

---

### Task 8: 系统用户管理

**Files:**
- Create: `RouteGo/RouteGo-modules/RouteGo-admin/src/main/java/com/rg/admin/dto/UserCreateRequest.java`
- Create: `.../dto/UserUpdateRequest.java`
- Create: `.../dto/UserQuery.java`
- Create: `.../dto/AssignRolesRequest.java`
- Create: `.../dto/ResetPasswordRequest.java`
- Create: `.../vo/SysUserVO.java`
- Create: `.../vo/SysUserDetailVO.java`
- Modify: `.../service/SysUserService.java`
- Create: `.../service/impl/SysUserServiceImpl.java`（替换空实现）
- Modify: `.../controller/SysUserController.java`
- Test: `RouteGo/RouteGo-modules/RouteGo-admin/src/test/java/com/rg/admin/service/SysUserServiceImplTest.java`

**Interfaces:**
- Consumes: `CacheHelper`、`CacheKeyConstants.Admin.USER_*`、`AdminRelationMapper`、`PasswordEncoder`
- Produces:
  - `PageResult<SysUserVO> page(UserQuery)`
  - `SysUserDetailVO detail(Long)`
  - `Long create(UserCreateRequest)`
  - `void update(Long, UserUpdateRequest)`
  - `void updateStatus(Long, Byte)`
  - `void resetPassword(Long, String)`
  - `void assignRoles(Long, List<Long>)`
  - `void deleteUser(Long)`
  - `List<Long> listRoleIds(Long)`
  - `SysUserVO` 带 `version`，`phone`/`email` 带 `@Sensitive`

- [ ] **Step 1: 写失败测试**

```java
@Test void createRejectsCodeReservedByDeletedRow() {
    when(mapper.selectCount(any())).thenReturn(1L); // 包含 deleted=1
    assertThatThrownBy(() -> service.create(req))
        .isInstanceOf(AdminException.class)
        .extracting("code").isEqualTo(409);
}

@Test void createRequiresAtLeastOneRole() {
    ... assertThatThrownBy(...).hasMessageContaining("至少分配一个角色");
}

@Test void maskedPhoneIsNotWrittenBack() {
    SysUser existing = userWithPhone("13812348000");
    when(mapper.selectById(1L)).thenReturn(existing);
    service.update(1L, updateReqWithPhone("138****8000"));
    assertThat(existing.getPhone()).isEqualTo("13812348000");
}

@Test void cannotDisableSelfOrSuperAdmin() {
    try (var stp = mockStatic(StpUtil.class)) {
        stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        assertThatThrownBy(() -> service.updateStatus(1L, (byte) 0)).isInstanceOf(AdminException.class);
    }
}

@Test void assignRolesReplacesAndEvictsCachesAndKicksOut() {
    service.assignRoles(9L, List.of(2L, 3L));
    verify(roleMapper).delete(...); verify(userRoleMapper, times(2)).insert(...);
    verify(cacheHelper).evictAfterCommit(
        "admin:SysUser:getById:9", "admin:SysUser:getPermissions:9",
        "admin:SysUser:getRoles:9", "admin:SysMenu:getUserTree:9");
    verify(StpUtil.class)...
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `... test -Dtest=SysUserServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`

- [ ] **Step 3: 实现**

- 分页：`UserQuery` 继承 `PageQuery`，`LambdaQueryWrapper` 动态条件，`roleId` 先查 `sys_user_role` 得到用户 ID 集合；返回 `PageResult<SysUserVO>`。
- 详情：先读 `USER_BY_ID` 的 `SysUserCache`，未命中查库并写缓存 10 分钟；再查角色 ID 列表；转 `SysUserDetailVO`（手机号/邮箱带 `@Sensitive`）。
- 新增：查重包含 `deleted`；密码强度正则；至少一个角色且角色存在启用；BCrypt；`save` 后插入 `sys_user_role`；事务。
- 编辑：`username` 不接收；`version` 必填；手机号/邮箱按掩码规则忽略；`updateById` 返回 0 抛 409；事务提交后删 `USER_BY_ID`。
- 启停：禁止操作自己；目标用户含 `admin/super_admin` 角色时禁止停用；更新后踢下线并删缓存。
- 重置密码：强度校验、BCrypt、带 version 更新、踢下线。
- 删除：禁止自己/超级管理员；逻辑删除；删 `sys_user_role`；踢下线；删缓存。
- 分配角色：至少一个；校验角色存在启用；事务内删旧插新；提交后删 4 个用户级缓存；`StpUtil.logout(userId)`。
- Controller 全部加 `@Tag/@Operation`、`@SaCheckPermission`、`@OperLog`。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add RouteGo/RouteGo-modules/RouteGo-admin
git commit -m "feat(admin): user management with role assignment and cache eviction"
```

---

### Task 9: 角色管理与授权

**Files:**
- Create: `.../dto/RoleCreateRequest.java`、`RoleUpdateRequest.java`、`RoleQuery.java`、`AssignIdsRequest.java`
- Create: `.../vo/SysRoleVO.java`、`SysRoleDetailVO.java`
- Modify: `.../service/SysRoleService.java`
- Create: `.../service/impl/SysRoleServiceImpl.java`
- Modify: `.../controller/SysRoleController.java`
- Test: `RouteGo/RouteGo-modules/RouteGo-admin/src/test/java/com/rg/admin/service/SysRoleServiceImplTest.java`

**Interfaces:**
- Consumes: `CacheHelper`、`CacheKeyConstants.Admin.ROLE_*`、`AdminRelationMapper`
- Produces:
  - `PageResult<SysRoleVO> page(RoleQuery)`
  - `SysRoleDetailVO detail(Long)`
  - `Long create(RoleCreateRequest)`、`void update(Long, RoleUpdateRequest)`
  - `void updateStatus(Long, Byte)`、`void deleteRole(Long)`
  - `void assignMenus(Long, List<Long>)`、`void assignPermissions(Long, List<Long>)`
  - `List<Long> listMenuIds(Long)`、`List<Long> listPermissionIds(Long)`
  - `PageResult<SysUserVO> pageUsers(Long roleId, PageQuery)`

- [ ] **Step 1: 写失败测试**

```java
@Test void builtInRoleCannotBeDeletedOrDisabled() {
    assertThatThrownBy(() -> service.deleteRole(1L)).isInstanceOf(AdminException.class); // roleCode=super_admin
    assertThatThrownBy(() -> service.updateStatus(1L, (byte) 0)).isInstanceOf(AdminException.class);
}

@Test void roleWithUsersCannotBeDeleted() {
    when(relationMapper.countUsersByRoleId(2L)).thenReturn(3L);
    assertThatThrownBy(() -> service.deleteRole(2L)).hasMessageContaining("角色下存在用户");
}

@Test void assignPermissionsReplacesAndEvictsAffectedUsers() {
    when(relationMapper.selectUserIdsByRoleId(2L)).thenReturn(List.of(7L, 8L));
    service.assignPermissions(2L, List.of(11L, 12L));
    verify(permissionMapper).delete(...); verify(rolePermissionMapper, times(2)).insert(...);
    verify(cacheHelper).evictAfterCommit(
        "admin:SysRole:getPermissions:2",
        "admin:SysUser:getPermissions:7", "admin:SysUser:getPermissions:8");
}

@Test void disablingRoleEvictsUserCachesAndKicksOut() {
    when(relationMapper.selectUserIdsByRoleId(2L)).thenReturn(List.of(7L, 8L));
    try (var stp = mockStatic(StpUtil.class)) {
        service.updateStatus(2L, (byte) 0);
        verify(cacheHelper).evictAfterCommit(
            "admin:SysUser:getRoles:7", "admin:SysUser:getPermissions:7",
            "admin:SysUser:getUserTree:7", "admin:SysUser:getRoles:8",
            "admin:SysUser:getPermissions:8", "admin:SysUser:getUserTree:8");
        stp.verify(() -> StpUtil.logout(7L));
        stp.verify(() -> StpUtil.logout(8L));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `... test -Dtest=SysRoleServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`

- [ ] **Step 3: 实现**

- `role_code` 不可修改；新增查重含已删除；内置 `admin/super_admin` 不可删除/停用。
- 删除前 `countUsersByRoleId>0` 抛 409；删除后清 `ROLE_BY_ID`、`ROLE_MENUS`、`ROLE_PERMISSIONS`，角色下用户缓存删除并踢下线。
- 分配菜单：校验菜单存在；事务替换 `sys_role_menu`；提交后删 `ROLE_MENUS` + 该角色下用户 `USER_MENU_TREE`。
- 分配权限：校验权限存在且启用；事务替换；提交后删 `ROLE_PERMISSIONS` + 用户 `USER_PERMISSIONS`。
- 状态变更：内置角色保护；停用后按角色反查用户，删 3 个用户级缓存并踢下线。
- `pageUsers` 复用 `SysUserService.page` 的查询条件（roleId）。
- Controller 路径 `/admin/system/role`，权限码按设计文档 §6.3。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add RouteGo/RouteGo-modules/RouteGo-admin
git commit -m "feat(admin): role management with menu/permission assignment"
```

---

### Task 10: 菜单与权限点

**Files:**
- Create: `.../dto/MenuCreateRequest.java`、`MenuUpdateRequest.java`、`PermissionCreateRequest.java`、`PermissionUpdateRequest.java`、`PermissionQuery.java`
- Create: `.../vo/MenuNodeVO.java`、`SysMenuVO.java`、`SysPermissionVO.java`、`PermissionGroupVO.java`
- Modify: `.../service/SysMenuService.java`、`.../service/SysPermissionService.java`
- Create: `.../service/impl/SysMenuServiceImpl.java`、`.../service/impl/SysPermissionServiceImpl.java`
- Modify: `.../controller/SysMenuController.java`、`.../controller/SysPermissionController.java`
- Test: `.../service/SysMenuServiceImplTest.java`、`.../service/SysPermissionServiceImplTest.java`

**Interfaces:**
- Consumes: `CacheHelper`、`CacheKeyConstants.Admin.MENU_*` / `PERMISSION_*`、`AdminRelationMapper`
- Produces:
  - `List<MenuNodeVO> tree()`、`List<MenuNodeVO> userTree(Long userId)`
  - `Long createMenu(...)`、`void updateMenu(...)`、`void deleteMenu(Long)`、`void updateMenuStatus/Visible(...)`
  - `PageResult<SysPermissionVO> page(...)`、`List<PermissionGroupVO> grouped()`
  - `Long createPermission(...)` 等

- [ ] **Step 1: 写失败测试**

```java
@Test void buildsTreeSortedBySortThenId() {
    // parent 1 下两个子节点 sort 2/1
    assertThat(service.tree().get(0).getChildren())
        .extracting(MenuNodeVO::getId).containsExactly(3L, 2L);
}

@Test void rejectsParentCycle() {
    when(menuMapper.selectById(2L)).thenReturn(menu(2L, 1L)); // 父级是 1
    assertThatThrownBy(() -> service.updateMenu(1L, reqWithParent(2L)))
        .hasMessageContaining("父级");
}

@Test void deleteBlockedWhenChildrenOrRoleReference() {
    when(menuMapper.selectCount(any())).thenReturn(1L);
    assertThatThrownBy(() -> service.deleteMenu(1L)).isInstanceOf(AdminException.class);
}

@Test void superAdminGetsAllEnabledMenus() {
    when(roleMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("super_admin"));
    assertThat(service.userTree(1L)).isNotEmpty();
}

@Test void permissionDeleteBlockedWhenReferenced() {
    when(relationMapper.countRolesByPermissionId(5L)).thenReturn(1L);
    assertThatThrownBy(() -> service.deletePermission(5L)).isInstanceOf(AdminException.class);
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `... test -Dtest=SysMenuServiceImplTest,SysPermissionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`

- [ ] **Step 3: 实现**

- 菜单树：查 `deleted=0` 全量，按 `sort,id` 升序，`parent_id=0` 为根，递归装配 `children`；`userTree`：超级管理员返回全部 `status=1 && visible=1` 的 M/C；其他用户先查角色菜单 ID 再过滤。
- 环检测：向上遍历父链，遇到自身 ID 抛 400。
- 删除：有子级或 `sys_role_menu` 引用时抛 409；删除后清 `MENU_BY_ID`、`MENU_TREE`，并用 `selectRoleIdsByMenuId` / `selectUserIdsByMenuId` 清 `ROLE_MENUS`、`USER_MENU_TREE`。
- 权限点：`permission_code` 不可改；删除前用 `countRolesByPermissionId` 查引用；`grouped()` 走 `PERMISSION_ALL` 缓存 10 分钟；变更后清 `PERMISSION_ALL`、`PERMISSION_BY_ID`，并按 `selectRoleIdsByPermissionId` → `selectUserIdsByRoleId` 清相关用户 `USER_PERMISSIONS`。
- Controller 路径 `/admin/system/menu`、`/admin/system/permission`。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add RouteGo/RouteGo-modules/RouteGo-admin
git commit -m "feat(admin): menu tree and permission point management"
```

---

### Task 11: 字典与系统参数

**Files:**
- Create: `.../dto/DictTypeCreateRequest.java`、`DictTypeUpdateRequest.java`、`DictTypeQuery.java`、`DictDataCreateRequest.java`、`DictDataUpdateRequest.java`、`DictDataQuery.java`、`ConfigCreateRequest.java`、`ConfigUpdateRequest.java`、`ConfigQuery.java`
- Create: `.../vo/DictTypeVO.java`、`DictDataVO.java`、`SysConfigVO.java`
- Modify: `.../service/SysDictTypeService.java`、`SysDictDataService.java`、`SysConfigService.java`
- Create: `.../service/impl/SysDictTypeServiceImpl.java`、`SysDictDataServiceImpl.java`、`SysConfigServiceImpl.java`
- Modify: `.../controller/SysDictTypeController.java`、`SysDictDataController.java`、`SysConfigController.java`
- Test: `.../service/SysDictServiceImplTest.java`、`.../service/SysConfigServiceImplTest.java`

**Interfaces:**
- Consumes: `CacheHelper`、`CacheKeyConstants.Admin.DICT_*` / `CONFIG_BY_KEY`
- Produces:
  - `List<DictDataVO> listByType(String dictType)`
  - `String getConfigValue(String key)`、`SysConfigVO getByKey(String key)`
  - CRUD 方法按设计文档 §6.6/§6.7

- [ ] **Step 1: 写失败测试**

```java
@Test void dictTypeWithDataCannotBeDeleted() {
    when(dictDataMapper.selectCount(any())).thenReturn(2L);
    assertThatThrownBy(() -> service.deleteType(1L)).hasMessageContaining("字典数据");
}

@Test void settingDefaultClearsOtherDefaults() {
    service.setDefault(9L); // dictType=sys_user_status
    verify(dictDataMapper).update(any(), argThat(w -> w.getSqlSet().contains("is_default")));
}

@Test void listByTypeUsesCacheAndEvictsOnChange() {
    when(cacheHelper.getOrLoad(eq("admin:SysDictData:listByType:sys_user_status"), any(), any()))
        .thenReturn(List.of(vo));
    assertThat(service.listByType("sys_user_status")).hasSize(1);
}

@Test void configGetUsesCacheAndEvictsAfterUpdate() {
    when(cacheHelper.getOrLoad(eq("admin:SysConfig:getByKey:site.name"), any(), any())).thenReturn("RouteGo");
    assertThat(service.getConfigValue("site.name")).isEqualTo("RouteGo");
    service.update(1L, req);
    verify(cacheHelper).evictAfterCommit("admin:SysConfig:getByKey:site.name");
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `... test -Dtest=SysDictServiceImplTest,SysConfigServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`

- [ ] **Step 3: 实现**

- 字典类型：`dict_type` 不可改；删除前查 `sys_dict_data`（含已删除？只查未删除）存在则 409；变更删 `DICT_TYPE_BY_TYPE`。
- 字典数据：类型必须存在启用；同类型 `dict_value` 唯一（含已删除）；`listByType` 走 `getOrLoad` 1 小时，只返回启用项按 `sort,id`；任何写操作删 `DICT_DATA_BY_TYPE:{type}`；设默认先清零同类型 `is_default`。
- 参数：`config_key` 不可改；查重含已删除；`getByKey` 走缓存 1 小时；写操作事务提交后删对应 key；批量更新逐条删 key。
- Controller 路径 `/admin/system/dict/type`、`/admin/system/dict/data`、`/admin/system/config`。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add RouteGo/RouteGo-modules/RouteGo-admin
git commit -m "feat(admin): dictionary and system config with caching"
```

---

### Task 12: 公告与文件

**Files:**
- Create: `.../dto/NoticeCreateRequest.java`、`NoticeUpdateRequest.java`、`NoticeQuery.java`
- Create: `.../vo/SysNoticeVO.java`、`SysFileVO.java`
- Create: `.../config/StorageProperties.java`
- Modify: `RouteGo/RouteGo-app/src/main/resources/application.yaml`（`routego.storage.*`、`spring.servlet.multipart.max-file-size: 20MB`）
- Modify: `.../service/SysNoticeService.java`、`SysFileService.java`
- Create: `.../service/impl/SysNoticeServiceImpl.java`、`SysFileServiceImpl.java`
- Modify: `.../controller/SysNoticeController.java`、`SysFileController.java`
- Test: `.../service/SysNoticeServiceImplTest.java`、`.../service/SysFileServiceImplTest.java`

**Interfaces:**
- Consumes: `CacheHelper`、`CacheKeyConstants.Admin.NOTICE_PUBLISHED` / `FILE_BY_ID`、`StorageProperties`
- Produces:
  - `void publish(Long)`、`void unpublish(Long)`、`void setTop(Long, boolean)`
  - `List<SysNoticeVO> listPublished()`
  - `SysFileVO upload(MultipartFile file)`、`Resource download(Long id)`、`void deleteFile(Long id)`
  - `StorageProperties.localPath` / `maxSize` / `allowedExtensions`
  - `SysFileServiceImpl(SysFileMapper, StorageProperties, CacheHelper)` 构造签名

- [ ] **Step 1: 写失败测试**

```java
@Test void publishSetsTimeAndEvictsPublishedCache() {
    service.publish(5L);
    verify(mapper).updateById(argThat(n -> n.getStatus() == 1 && n.getPublishTime() != null));
    verify(cacheHelper).evictAfterCommit("admin:SysNotice:listPublished");
}

@Test void uploadRejectsDisallowedExtensionAndOversize() {
    assertThatThrownBy(() -> service.upload(mockFile("a.exe", 10))).hasMessageContaining("类型");
    assertThatThrownBy(() -> service.upload(mockFile("a.png", 21 * 1024 * 1024))).hasMessageContaining("大小");
}

@Test void uploadStoresUnderDateFolderWithUuidName(@TempDir Path tmp) throws Exception {
    var props = new StorageProperties(); props.setLocalPath(tmp.toString());
    var svc = new SysFileServiceImpl(..., props);
    var vo = svc.upload(mockFile("照片.png", 100));
    assertThat(vo.getFileName()).endsWith(".png").doesNotContain("照片");
    assertThat(Files.exists(tmp.resolve(vo.getFilePath()))).isTrue();
}

@Test void deleteRemovesPhysicalFileBestEffort(@TempDir Path tmp) throws Exception {
    ... assertThat(Files.exists(path)).isFalse();
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `... test -Dtest=SysNoticeServiceImplTest,SysFileServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`

- [ ] **Step 3: 实现**

- 公告：标题必填；发布写 `publishTime`、`status=1`；下架 `status=0`；置顶 `isTop`；列表按 `isTop desc, publishTime desc`；`listPublished` 走缓存 5 分钟；新增/编辑/删除/发布/下架/置顶全部在提交后删 `NOTICE_PUBLISHED`。
- 文件：校验扩展名白名单与大小；原名保留在 `originalName`；存储名 `UUID.ext`，路径 `yyyy/MM/dd`；写 `sys_file`，`fileUrl=/admin/system/file/{id}/download`；详情走 `FILE_BY_ID` 缓存 10 分钟；下载用 `FileSystemResource`，校验记录未删除、文件存在；删除逻辑删记录 + 物理删除，失败只记日志；提交后清 `FILE_BY_ID`。
- Controller：`/admin/system/notice`、`/admin/system/file`；上传用 `@RequestPart("file")`；下载返回 `ResponseEntity<Resource>`。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add RouteGo/RouteGo-modules/RouteGo-admin
git commit -m "feat(admin): notice management and local file storage"
```

---

### Task 13: 日志查询、OAuth 与操作日志落库

**Files:**
- Create: `.../dto/OperLogQuery.java`、`LoginLogQuery.java`
- Create: `.../vo/SysOperLogVO.java`、`SysLoginLogVO.java`、`SysUserOauthVO.java`
- Modify: `.../service/SysOperLogService.java`、`SysLoginLogService.java`、`SysUserOauthService.java`
- Create: `.../service/impl/SysOperLogServiceImpl.java`、`SysLoginLogServiceImpl.java`、`SysUserOauthServiceImpl.java`
- Create: `.../service/impl/OperLogRecorderImpl.java`
- Modify: `.../controller/SysOperLogController.java`、`SysLoginLogController.java`、`SysUserOauthController.java`
- Test: `.../service/OperLogRecorderImplTest.java`、`.../service/SysUserOauthServiceImplTest.java`

**Interfaces:**
- Consumes: `OperLogRecorder`、`OperLogRecord`
- Produces:
  - `OperLogRecorderImpl implements OperLogRecorder`（`@Async` 写 `sys_oper_log`）
  - `PageResult<SysOperLogVO> page(OperLogQuery)`、`PageResult<SysLoginLogVO> page(LoginLogQuery)`
  - `List<SysUserOauthVO> listByUserId(Long)`、`Long bind(...)`、`void unbind(Long)`

- [ ] **Step 1: 写失败测试**

```java
@Test void recorderMapsAllFields() {
    var record = sampleRecord();
    recorder.record(record);
    verify(operLogMapper).insert(argThat(e ->
        e.getTitle().equals("用户管理") && e.getBusinessType().equals((byte) 1)
        && e.getOperName().equals("alice") && e.getCostTime().equals(12L)));
}

@Test void oauthDuplicateProviderOpenidRejected() {
    when(mapper.selectCount(any())).thenReturn(1L);
    assertThatThrownBy(() -> service.bind(req)).hasMessageContaining("已绑定");
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `... test -Dtest=OperLogRecorderImplTest,SysUserOauthServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`

- [ ] **Step 3: 实现**

- `OperLogRecorderImpl`：`@Async` 注解，DTO → `SysOperLog`，异常只记日志。
- 操作日志/登录日志：只读分页，支持模块、操作人、状态、时间范围筛选；不提供写接口。
- OAuth：按用户查询走 `OAUTH_BY_USER` 缓存 10 分钟；绑定校验 `(provider, openid)` 唯一（含已删除由唯一索引兜底，捕获 `DuplicateKeyException` 转 409）；解绑前确认 `sys_user.password` 非空；写后删缓存。
- Controller 路径 `/admin/system/oper-log`、`/admin/system/login-log`、`/admin/system/user-oauth`。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add RouteGo/RouteGo-modules/RouteGo-admin
git commit -m "feat(admin): log queries, oauth binding and oper-log persistence"
```

---

### Task 14: 初始化 SQL、README 与整体验证

**Files:**
- Create: `sql/admin-init.sql`
- Modify: `RouteGo/README.md`
- Create: `doc/superpowers/plans/2026-11-04-admin-smoke.md`（手工冒烟步骤）

**Interfaces:**
- Consumes: 全部前置任务
- Produces: 可执行的内置角色/菜单/权限初始化数据

- [ ] **Step 1: 写 SQL**

`sql/admin-init.sql` 内容：

- `sys_role`：`admin`（系统管理员）、`super_admin`（超级管理员），`status=1`
- `sys_permission`：与设计文档 §6 的权限码一一对应（`system:user:*`、`system:role:*`、`system:menu:*`、`system:permission:*`、`system:dict:*`、`system:config:*`、`system:notice:*`、`system:file:*`、`system:operlog:list`、`system:loginlog:list`）
- `sys_menu`：系统管理目录 + 用户/角色/菜单/权限/字典/参数/公告/文件/日志菜单项
- `sys_role_menu` / `sys_role_permission`：两个内置角色全量授权
- 不插入 `sys_user`；注释说明用 BCrypt 生成密码后手工插入

- [ ] **Step 2: 验证 SQL 静态一致性**

Run:
```bash
grep -c "INSERT INTO sys_permission" sql/admin-init.sql
grep -c "INSERT INTO sys_menu" sql/admin-init.sql
```
Expected: 均大于 0，且脚本中每个 `@SaCheckPermission` 使用的权限码都能在 README/SQL 清单中找到。

- [ ] **Step 3: 更新 README**

补充：admin 接口清单、缓存 key 规范与失效策略、验证码调用方式、初始化步骤、测试命令（含 PATH 修正）。

- [ ] **Step 4: 全量编译与测试**

Run:
```bash
cd D:/PROJECT/RouteGo/RouteGo
PATH="/usr/bin:$PATH" JAVA_HOME="/d/Program Files/java/java21" ./mvnw -o -q -pl RouteGo-modules/RouteGo-admin -am test
```
Expected: BUILD SUCCESS，全部单测通过。

- [ ] **Step 5: 提交**

```bash
git add sql/admin-init.sql RouteGo/README.md doc/superpowers/plans/2026-11-04-admin-smoke.md
git commit -m "docs(admin): add init sql, smoke checklist and module documentation"
```
