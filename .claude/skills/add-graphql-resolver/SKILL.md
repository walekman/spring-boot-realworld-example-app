---
name: add-graphql-resolver
description: Use when adding a new GraphQL query, mutation, or sub-field resolver to the walekman-spring-boot-realworld-example-app project — covers schema changes, codegen, DGS annotations, payload pattern, localContext threading, and test structure.
---

# Add GraphQL Resolver

## Overview

GraphQL in this project uses the Netflix DGS framework. Every resolver is a method in a `@DgsComponent` class. The application/infrastructure layers are shared with REST — only the `graphql/` package is GraphQL-specific.

## Step 0: Schema first

All changes start in `src/main/resources/schema/schema.graphqls`. Add your new type, field, input, or mutation there, then regenerate:

```
./gradlew generateJava
```

This updates `build/generated/sources/dgs-codegen/io/spring/graphql/types/` and `DgsConstants`. The `DgsConstants` class is the source of truth for `TYPE_NAME` and field name constants — always use it instead of string literals.

## Query resolver (datafetcher)

File: `src/main/java/io/spring/graphql/XxxDatafetcher.java`

```java
@DgsComponent
@AllArgsConstructor
public class XxxDatafetcher {
  private XxxQueryService xxxQueryService;

  // Top-level query field
  @DgsQuery(field = QUERY.Xxx)
  public DataFetcherResult<XxxType> getXxx(@InputArgument("slug") String slug) {
    User current = SecurityUtil.getCurrentUser().orElse(null); // null OK for public
    XxxData data = xxxQueryService.findBySlug(slug, current)
        .orElseThrow(ResourceNotFoundException::new);
    XxxType result = XxxType.newBuilder().field(data.getField()).build();
    return DataFetcherResult.<XxxType>newResult()
        .data(result)
        .localContext(data)   // pass to sub-field resolvers
        .build();
  }

  // Sub-field resolver on a parent type
  @DgsData(parentType = ARTICLE.TYPE_NAME, field = ARTICLE.SomeField)
  public SomeType getSubField(DataFetchingEnvironment dfe) {
    ArticleData article = dfe.getLocalContext(); // data threaded from parent
    Article source = dfe.getSource();            // the parent GraphQL type
    // ...
  }
}
```

**Key rules:**
- `@DgsQuery` is shorthand for `@DgsData(parentType = DgsConstants.QUERY_TYPE, field = ...)`
- `@InputArgument("name")` maps schema argument names — must match schema exactly
- `SecurityUtil.getCurrentUser()` reads `SecurityContextHolder`; returns empty for anonymous
- Thread data to child resolvers via `localContext` — sub-fields receive it via `dfe.getLocalContext()`
- Use codegen builder: `XxxType.newBuilder().field(value).build()` — never `new XxxType(...)`

## Mutation resolver

File: `src/main/java/io/spring/graphql/XxxMutation.java`

```java
@DgsComponent
@AllArgsConstructor
public class XxxMutation {
  private XxxCommandService xxxCommandService;

  @DgsMutation(field = MUTATION.CreateXxx)
  public DataFetcherResult<XxxPayload> createXxx(@InputArgument("input") CreateXxxInput input) {
    User user = SecurityUtil.getCurrentUser().orElseThrow(AuthenticationException::new);
    Xxx entity;
    try {
      entity = xxxCommandService.create(input, user);
    } catch (ConstraintViolationException cve) {
      // Validation error path — return Error union type
      return DataFetcherResult.<XxxPayload>newResult()  // adjust if using union
          .data(GraphQLCustomizeExceptionHandler.getErrorsAsData(cve))
          .build();
    }
    return DataFetcherResult.<XxxPayload>newResult()
        .data(XxxPayload.newBuilder().build())
        .localContext(entity)   // entity passed to XxxPayload.xxx sub-field resolver
        .build();
  }
}
```

**Payload pattern — how mutations return read-model data:**

Mutations never query data themselves. Instead:
1. Mutation stores the domain entity in `localContext`
2. A `@DgsData` method on the payload type fetches the read model:

```java
// In XxxDatafetcher or XxxMutation — resolves XxxPayload.xxx
@DgsData(parentType = XXXPAYLOAD.TYPE_NAME, field = XXXPAYLOAD.Xxx)
public DataFetcherResult<XxxType> getXxxPayloadField(DataFetchingEnvironment dfe) {
  Xxx entity = dfe.getLocalContext();
  XxxData data = xxxQueryService.findById(entity.getId(), null)
      .orElseThrow(ResourceNotFoundException::new);
  XxxType result = XxxType.newBuilder().field(data.getField()).build();
  return DataFetcherResult.<XxxType>newResult().data(result).build();
}
```

## Paginated queries (cursor-based)

Use `CursorPager` + `PageInfo` from codegen (not `graphql.relay.PageInfo`):

```java
import io.spring.graphql.types.PageInfo;

private PageInfo buildPageInfo(CursorPager<?> pager) {
  return PageInfo.newBuilder()
      .startCursor(pager.getStartCursor() == null ? null : pager.getStartCursor().toString())
      .endCursor(pager.getEndCursor() == null ? null : pager.getEndCursor().toString())
      .hasPreviousPage(pager.hasPrevious())
      .hasNextPage(pager.hasNext())
      .build();
}
```

Always validate: `if (first == null && last == null) throw new IllegalArgumentException(...)`.

## Test structure

GraphQL tests are plain JUnit 5 + Mockito — **no Spring context**, unlike REST tests.

```java
@ExtendWith(MockitoExtension.class)
class XxxMutationTest {
  @Mock private XxxCommandService xxxCommandService;
  @InjectMocks private XxxMutation xxxMutation;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User("u@test.com", "testuser", "pass", "", "");
    // Anonymous by default
    SecurityContextHolder.getContext().setAuthentication(
        new AnonymousAuthenticationToken("key", "anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @AfterEach
  void tearDown() { SecurityContextHolder.clearContext(); }

  private void authenticateAs(User u) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(u, null, List.of()));
  }

  @Test
  void should_create_xxx_when_authenticated() {
    authenticateAs(user);
    when(xxxCommandService.create(any(), any())).thenReturn(someEntity);
    DataFetcherResult<XxxPayload> result = xxxMutation.createXxx(someInput);
    assertNotNull(result.getData());
    verify(xxxCommandService).create(any(), any());
  }

  @Test
  void should_throw_when_unauthenticated() {
    assertThrows(AuthenticationException.class, () -> xxxMutation.createXxx(someInput));
  }
}
```

**Security setup is manual** — `SecurityContextHolder` must be set/cleared in every test class that touches auth. There is no shared base class for graphql tests (unlike `TestWithCurrentUser` in the REST layer).

## Common mistakes

| Mistake | Fix |
|---|---|
| Using `graphql.relay.PageInfo` | Use `io.spring.graphql.types.PageInfo` (codegen-generated) |
| String literals for type/field names | Use `DgsConstants.TYPENAME.TYPE_NAME` / `.FieldName` |
| `new XxxType(...)` constructor | Use `XxxType.newBuilder().field(v).build()` |
| Querying read model inside mutation | Put it in the payload sub-field resolver, thread entity via `localContext` |
| Forgetting `./gradlew generateJava` after schema change | `DgsConstants` and types go stale; build will fail with missing symbols |
| Not clearing `SecurityContextHolder` in `@AfterEach` | Auth state leaks between tests |
| `@WebMvcTest` on a GraphQL test | Use `@ExtendWith(MockitoExtension.class)` — no Spring context needed |
