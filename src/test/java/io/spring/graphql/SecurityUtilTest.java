package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.core.user.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_return_empty_when_no_authentication_in_context() {
    SecurityContextHolder.clearContext();

    Optional<User> result = SecurityUtil.getCurrentUser();

    assertTrue(result.isEmpty());
  }

  @Test
  void should_return_empty_when_authentication_is_anonymous() {
    AnonymousAuthenticationToken auth =
        new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    Optional<User> result = SecurityUtil.getCurrentUser();

    assertTrue(result.isEmpty());
  }

  @Test
  void should_return_user_when_authenticated() {
    User user = new User("user@example.com", "testuser", "password", "", "");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);

    Optional<User> result = SecurityUtil.getCurrentUser();

    assertTrue(result.isPresent());
    assertEquals(user, result.get());
  }

  @Test
  void should_return_empty_when_principal_is_null() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    Optional<User> result = SecurityUtil.getCurrentUser();

    assertTrue(result.isEmpty());
  }
}
