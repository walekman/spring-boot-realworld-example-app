---
name: add-rest-endpoint
description: Use when adding a new REST endpoint to the walekman-spring-boot-realworld-example-app project — covers the exact layer sequence, file locations, naming conventions, and test structure for this specific codebase.
---

# Add REST Endpoint

## Overview

This project uses a strict 4-layer architecture: **API → Application → Infrastructure → Core**. Missing any layer or putting code in the wrong layer will break the pattern. Read and write paths differ — follow the correct one.

## Layer Map

```
src/main/java/io/spring/
  api/                          ← @RestController (HTTP in/out only)
  application/                  ← QueryService (read) or CommandService (write)
  application/data/             ← Read-model DTOs (ArticleData, UserData, …)
  infrastructure/mybatis/
    readservice/                ← @Mapper interfaces for SELECT queries
    mapper/                     ← @Mapper interfaces for INSERT/UPDATE/DELETE
  core/                         ← Domain entities + Repository interfaces
  infrastructure/repository/    ← MyBatisXxx implementations of core repositories

src/main/resources/mapper/
  XxxReadService.xml            ← SQL for read services
  XxxMapper.xml                 ← SQL for write mappers
```

## READ endpoint (GET — no state change)

**Files to create/modify, in order:**

1. **`application/data/XxxData.java`** — Read-model DTO (if new shape needed)
2. **`infrastructure/mybatis/readservice/XxxReadService.java`** — `@Mapper` interface
3. **`src/main/resources/mapper/XxxReadService.xml`** — SQL, namespace = FQN of step 2
4. **`application/XxxQueryService.java`** — `@Service` that calls the read service, enriches data
5. **`api/XxxApi.java`** — `@RestController`, injects QueryService, returns `ResponseEntity`

**Key conventions:**
- `@Mapper` on the read service interface (no implementation needed — MyBatis generates it)
- Use `@Param("x")` on every method parameter in the mapper interface
- QueryService is `@Service @AllArgsConstructor`; it may call multiple read services
- Controller is `@RestController @RequestMapping @AllArgsConstructor`
- Wrap response body: `ResponseEntity.ok(new HashMap<>() {{ put("article", data); }})`
- Optional auth: `@AuthenticationPrincipal User user` — can be null for public endpoints

## WRITE endpoint (POST/PUT/DELETE — state change)

**Files to create/modify, in order:**

1. **`core/xxx/Xxx.java`** — Domain entity (if new aggregate)
2. **`core/xxx/XxxRepository.java`** — Repository interface (save/find/delete)
3. **`infrastructure/mybatis/mapper/XxxMapper.java`** — `@Mapper` for write operations
4. **`src/main/resources/mapper/XxxMapper.xml`** — SQL, namespace = FQN of step 3
5. **`infrastructure/repository/MyBatisXxxRepository.java`** — Implements core repository using mapper
6. **`application/xxx/XxxParam.java`** — Request param with `@NotBlank`/`@Valid` annotations
7. **`application/xxx/XxxCommandService.java`** — `@Service @Validated @Transactional`, calls repository
8. **`api/XxxApi.java`** — Controller injects CommandService (and QueryService to build response)

**Key conventions:**
- `@Validated` on CommandService, `@Valid` on method params — triggers Bean Validation
- `@Transactional` on mutating methods in CommandService
- Param classes use `jakarta.validation.constraints.*` (not `javax.*`)
- After write, fetch read-model to return: delegate to QueryService, not the mapper directly

## Test structure

Every controller gets a `@WebMvcTest` test. The canonical pattern:

```java
@WebMvcTest({XxxApi.class})
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class XxxApiTest extends TestWithCurrentUser {
  @Autowired private MockMvc mvc;

  @MockBean private XxxQueryService xxxQueryService;   // read
  @MockBean private XxxCommandService xxxCommandService; // write (if needed)

  @Override @BeforeEach
  public void setUp() throws Exception {
    super.setUp();                          // sets up user + token mocks
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_do_thing_success() throws Exception {
    when(xxxQueryService.findById(any(), any())).thenReturn(Optional.of(someData));

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)  // omit for public endpoints
        .body(requestBody)
        .when().post("/xxx")
        .then().statusCode(200)
        .body("xxx.field", equalTo(expectedValue));
  }
}
```

- `token` and `user` come from `TestWithCurrentUser` — don't redefine them
- Mock `@MockBean` for every service the controller injects
- Test validation errors: send blank/missing fields, assert `statusCode(422)` and `body("errors.field[0]", equalTo(...))`

## XML mapper skeleton

Read service XML:
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="io.spring.infrastructure.mybatis.readservice.XxxReadService">
  <select id="findById" resultMap="transfer.data.xxxData">
    select x.id xId, x.name xName from xxx x where x.id = #{id}
  </select>
</mapper>
```

Write mapper XML:
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="io.spring.infrastructure.mybatis.mapper.XxxMapper">
  <insert id="insert">
    insert into xxx(id, name) values(#{xxx.id}, #{xxx.name})
  </insert>
</mapper>
```

The `resultMap` for complex DTOs is defined in `TransferData.xml` — add new result maps there.

## Common mistakes

| Mistake | Fix |
|---|---|
| `javax.validation.*` imports | Use `jakarta.validation.*` everywhere |
| Missing `@Param` on mapper method | Every parameter needs `@Param("name")` |
| Returning domain entity from controller | Fetch read-model DTO via QueryService instead |
| `@Autowired` fields | Use `@AllArgsConstructor` (constructor injection) |
| Mapper XML namespace wrong | Must exactly match the fully-qualified interface name |
| Skipping `@Validated` on CommandService | `@Valid` on method params does nothing without it |