package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.ProfilePayload;
import java.util.List;
import java.util.Optional;
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

@ExtendWith(MockitoExtension.class)
class RelationMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;

  @InjectMocks private RelationMutation relationMutation;

  private User currentUser;
  private User targetUser;

  @BeforeEach
  void setUp() {
    currentUser = new User("current@test.com", "current", "pass", "", "");
    targetUser = new User("target@test.com", "target", "pass", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAs(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
  }

  @Test
  void should_follow_user_successfully() {
    authenticateAs(currentUser);
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(targetUser));
    ProfileData profileData = new ProfileData(targetUser.getId(), "target", "", "", true);
    when(profileQueryService.findByUsername(eq("target"), eq(currentUser)))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = relationMutation.follow("target");

    assertNotNull(result.getProfile());
    assertEquals("target", result.getProfile().getUsername());
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  void should_throw_authentication_exception_when_following_unauthenticated() {
    assertThrows(AuthenticationException.class, () -> relationMutation.follow("target"));
  }

  @Test
  void should_throw_not_found_when_follow_target_user_does_not_exist() {
    authenticateAs(currentUser);
    when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.follow("nobody"));
  }

  @Test
  void should_unfollow_user_successfully() {
    authenticateAs(currentUser);
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(targetUser));
    FollowRelation relation = new FollowRelation(currentUser.getId(), targetUser.getId());
    when(userRepository.findRelation(currentUser.getId(), targetUser.getId()))
        .thenReturn(Optional.of(relation));
    ProfileData profileData = new ProfileData(targetUser.getId(), "target", "", "", false);
    when(profileQueryService.findByUsername(eq("target"), eq(currentUser)))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = relationMutation.unfollow("target");

    assertNotNull(result.getProfile());
    assertEquals("target", result.getProfile().getUsername());
    verify(userRepository).removeRelation(relation);
  }

  @Test
  void should_throw_authentication_exception_when_unfollowing_unauthenticated() {
    assertThrows(AuthenticationException.class, () -> relationMutation.unfollow("target"));
  }

  @Test
  void should_throw_not_found_when_unfollow_target_user_does_not_exist() {
    authenticateAs(currentUser);
    when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("nobody"));
  }

  @Test
  void should_throw_not_found_when_relation_does_not_exist_on_unfollow() {
    authenticateAs(currentUser);
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(currentUser.getId(), targetUser.getId()))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> relationMutation.unfollow("target"));
  }
}
