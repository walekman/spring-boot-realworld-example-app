package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;

  @InjectMocks private UserMutation userMutation;

  @BeforeEach
  void setUpAnonymousAuth() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_create_user_and_return_payload() {
    User createdUser = new User("u@test.com", "testuser", "hashed", "", "");
    when(userService.createUser(any())).thenReturn(createdUser);

    CreateUserInput input =
        CreateUserInput.newBuilder().email("u@test.com").username("testuser").password("pass").build();

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertNotNull(result.getData());
    verify(userService).createUser(any());
  }

  @Test
  void should_return_error_when_create_user_throws_constraint_violation() {
    ConstraintViolationException cve = mock(ConstraintViolationException.class);
    when(cve.getConstraintViolations()).thenReturn(Collections.emptySet());
    when(userService.createUser(any())).thenThrow(cve);

    CreateUserInput input =
        CreateUserInput.newBuilder().email("bad").username("x").password("p").build();

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertNotNull(result.getData());
  }

  @Test
  void should_login_successfully_with_correct_credentials() {
    User user = new User("u@test.com", "testuser", "hashed", "", "");
    when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("plain", "hashed")).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("plain", "u@test.com");

    assertNotNull(result.getData());
  }

  @Test
  void should_throw_invalid_authentication_when_password_is_wrong() {
    User user = new User("u@test.com", "testuser", "hashed", "", "");
    when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrong", "hashed")).thenReturn(false);

    assertThrows(InvalidAuthenticationException.class,
        () -> userMutation.login("wrong", "u@test.com"));
  }

  @Test
  void should_throw_invalid_authentication_when_user_email_not_found() {
    when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

    assertThrows(InvalidAuthenticationException.class,
        () -> userMutation.login("pass", "nobody@test.com"));
  }

  @Test
  void should_update_user_when_authenticated() {
    User user = new User("u@test.com", "testuser", "pass", "bio", "img");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));

    UpdateUserInput input =
        UpdateUserInput.newBuilder().email("new@test.com").username("newname").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertNotNull(result.getData());
    verify(userService).updateUser(any());
  }

  @Test
  void should_throw_when_updating_user_unauthenticated() {
    UpdateUserInput input = UpdateUserInput.newBuilder().email("x@test.com").build();

    assertThrows(
        io.spring.graphql.exception.AuthenticationException.class,
        () -> userMutation.updateUser(input));
  }
}
