package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Profile;
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
class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;

  @InjectMocks private ProfileDatafetcher profileDatafetcher;

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

  private ProfileData sampleProfileData(String username) {
    return new ProfileData("user-id", username, "bio", "img", false);
  }

  @Test
  void should_return_profile_payload_for_known_username() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getArgument("username")).thenReturn("testuser");
    when(profileQueryService.findByUsername(eq("testuser"), isNull()))
        .thenReturn(Optional.of(sampleProfileData("testuser")));

    ProfilePayload payload = profileDatafetcher.queryProfile("testuser", dfe);

    assertNotNull(payload.getProfile());
    assertEquals("testuser", payload.getProfile().getUsername());
    assertEquals("bio", payload.getProfile().getBio());
  }

  @Test
  void should_throw_not_found_when_profile_username_does_not_exist() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getArgument("username")).thenReturn("nobody");
    when(profileQueryService.findByUsername(eq("nobody"), isNull())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> profileDatafetcher.queryProfile("nobody", dfe));
  }

  @Test
  void should_return_profile_with_following_true_for_authenticated_follower() {
    User currentUser = new User("u@test.com", "follower", "pass", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null, List.of()));

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getArgument("username")).thenReturn("author");
    ProfileData profileData = new ProfileData("author-id", "author", "", "", true);
    when(profileQueryService.findByUsername(eq("author"), eq(currentUser)))
        .thenReturn(Optional.of(profileData));

    ProfilePayload payload = profileDatafetcher.queryProfile("author", dfe);

    assertEquals("author", payload.getProfile().getUsername());
  }

  @Test
  void should_return_profile_from_get_user_profile_field_resolver() {
    User coreUser = new User("u@test.com", "testuser", "pass", "", "");

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(coreUser);
    when(profileQueryService.findByUsername(eq("testuser"), isNull()))
        .thenReturn(Optional.of(sampleProfileData("testuser")));

    Profile profile = profileDatafetcher.getUserProfile(dfe);

    assertNotNull(profile);
    assertEquals("testuser", profile.getUsername());
  }
}
