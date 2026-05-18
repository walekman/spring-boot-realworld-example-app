package io.spring.application.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UserWithTokenTest {

  @Test
  void should_map_all_fields_from_user_data() {
    UserData userData = new UserData("user-id", "u@test.com", "testuser", "my bio", "http://img");
    UserWithToken userWithToken = new UserWithToken(userData, "jwt-token");

    assertEquals("u@test.com", userWithToken.getEmail());
    assertEquals("testuser", userWithToken.getUsername());
    assertEquals("my bio", userWithToken.getBio());
    assertEquals("http://img", userWithToken.getImage());
    assertEquals("jwt-token", userWithToken.getToken());
  }

  @Test
  void should_store_token_independently_of_user_data() {
    UserData userData = new UserData("id", "a@b.com", "user", "", "");
    UserWithToken first = new UserWithToken(userData, "token-one");
    UserWithToken second = new UserWithToken(userData, "token-two");

    assertEquals("token-one", first.getToken());
    assertEquals("token-two", second.getToken());
    assertEquals(first.getEmail(), second.getEmail());
  }
}
