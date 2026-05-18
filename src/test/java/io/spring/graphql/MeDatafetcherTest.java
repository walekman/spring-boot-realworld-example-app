package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.graphql.types.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MeDatafetcherTest {

  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;

  @InjectMocks private MeDatafetcher meDatafetcher;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_return_null_for_anonymous_user() {
    AnonymousAuthenticationToken auth =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    DataFetcherResult<User> result = meDatafetcher.getMe("", null);

    assertNull(result);
  }

  @Test
  void should_return_user_data_for_authenticated_user() {
    io.spring.core.user.User coreUser = new io.spring.core.user.User("u@test.com", "testuser", "pass", "bio", "img");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(coreUser, null, List.of()));

    UserData userData = new UserData(coreUser.getId(), "u@test.com", "testuser", "bio", "img");
    when(userQueryService.findById(coreUser.getId())).thenReturn(Optional.of(userData));

    DataFetcherResult<User> result = meDatafetcher.getMe("Token my-jwt-token", null);

    User graphqlUser = result.getData();
    assertEquals("u@test.com", graphqlUser.getEmail());
    assertEquals("testuser", graphqlUser.getUsername());
    assertEquals("my-jwt-token", graphqlUser.getToken());
  }
}
