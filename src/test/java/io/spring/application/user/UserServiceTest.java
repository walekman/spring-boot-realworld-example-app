package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {

  // BCrypt hash of "oldPassword"
  private static final String PASSWORD_HASH =
      "$2a$10$vV9wgU.x2x/F0Q5G1H6mKOXEbYzR9h8r11nQEoIB7c0.0KUpOSCf2";

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserService userService;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = new BCryptPasswordEncoder();
    userService = new UserService(userRepository, "https://default.image/img.png", passwordEncoder);
  }

  @Test
  void updateUser_shouldStoreBCryptHash_whenPasswordProvided() {
    String newPassword = "new Plaintext";
    User user = new User("test@test.com", "testuser", PASSWORD_HASH, "", "");
    UpdateUserParam param = UpdateUserParam.builder().password(newPassword).build();

    userService.updateUser(new UpdateUserCommand(user, param));

    assertNotEquals(newPassword, user.getPassword());
    assertNotEquals(PASSWORD_HASH, user.getPassword());
    assertTrue(passwordEncoder.matches(newPassword, user.getPassword()));
  }

  @Test
  void updateUser_shouldNotChangePassword_whenPasswordIsEmpty() {
    User user = new User("test@test.com", "testuser", PASSWORD_HASH, "", "");
    UpdateUserParam param = UpdateUserParam.builder().build(); // password defaults to ""

    userService.updateUser(new UpdateUserCommand(user, param));

    assertEquals(PASSWORD_HASH, user.getPassword());
  }

  @Test
  void updateUser_shouldNotChangePassword_whenPasswordIsNull() {
    User user = new User("test@test.com", "testuser", PASSWORD_HASH, "", "");
    UpdateUserParam param = UpdateUserParam.builder().password(null).build();

    userService.updateUser(new UpdateUserCommand(user, param));

    assertEquals(PASSWORD_HASH, user.getPassword());
  }
}
