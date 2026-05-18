package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.ArticlesConnection;
import io.spring.graphql.types.Profile;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.joda.time.DateTime;
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
class ArticleDatafetcherTest {

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;

  @InjectMocks private ArticleDatafetcher articleDatafetcher;

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

  private ArticleData sampleArticleData() {
    ProfileData profileData = new ProfileData("user-id", "testuser", "", "", false);
    return new ArticleData(
        "article-id", "test-slug", "Test Title", "Desc", "Body",
        false, 0, new DateTime(), new DateTime(),
        Arrays.asList("java"), profileData);
  }

  private CursorPager<ArticleData> singleNextPager() {
    return new CursorPager<>(Arrays.asList(sampleArticleData()), Direction.NEXT, false);
  }

  @Test
  void should_throw_when_both_first_and_last_are_null_for_articles() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getArticles(null, null, null, null, null, null, null, null));
  }

  @Test
  void should_get_articles_using_first() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), isNull()))
        .thenReturn(singleNextPager());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, null);

    assertNotNull(result.getData());
    assertEquals(1, result.getData().getEdges().size());
    assertEquals("test-slug", result.getData().getEdges().get(0).getNode().getSlug());
  }

  @Test
  void should_get_articles_using_last() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), isNull()))
        .thenReturn(new CursorPager<>(Arrays.asList(sampleArticleData()), Direction.PREV, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(null, null, 5, null, null, null, null, null);

    assertNotNull(result.getData());
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void should_find_article_by_slug() {
    when(articleQueryService.findBySlug("test-slug", null))
        .thenReturn(Optional.of(sampleArticleData()));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("test-slug");

    assertNotNull(result.getData());
    assertEquals("Test Title", result.getData().getTitle());
    assertEquals("test-slug", result.getData().getSlug());
  }

  @Test
  void should_throw_not_found_when_slug_does_not_exist() {
    when(articleQueryService.findBySlug("no-slug", null)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleDatafetcher.findArticleBySlug("no-slug"));
  }

  @Test
  void should_throw_when_both_first_and_last_are_null_for_feed() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getFeed(null, null, null, null, null));
  }

  @Test
  void should_get_feed_with_authenticated_user_using_first() {
    User user = new User("u@test.com", "testuser", "pass", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));

    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(singleNextPager());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, null);

    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void should_get_feed_with_unauthenticated_user() {
    when(articleQueryService.findUserFeedWithCursor(isNull(), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, null);

    assertEquals(0, result.getData().getEdges().size());
  }

  private Profile sampleProfile() {
    return Profile.newBuilder().username("testuser").build();
  }

  private DgsDataFetchingEnvironment buildProfileDfe(Profile profile) {
    DgsDataFetchingEnvironment dfe = mock(DgsDataFetchingEnvironment.class);
    when(dfe.getSource()).thenReturn(profile);
    return dfe;
  }

  @Test
  void should_throw_when_both_first_and_last_are_null_for_user_feed() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFeed(null, null, null, null, null));
  }

  @Test
  void should_get_user_feed_using_first() {
    User user = new User("u@test.com", "testuser", "pass", "", "");
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any())).thenReturn(singleNextPager());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(10, null, null, null, buildProfileDfe(sampleProfile()));

    assertEquals(1, result.getData().getEdges().size());
    assertEquals("test-slug", result.getData().getEdges().get(0).getNode().getSlug());
  }

  @Test
  void should_get_user_feed_using_last() {
    User user = new User("u@test.com", "testuser", "pass", "", "");
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.PREV, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(null, null, 5, null, buildProfileDfe(sampleProfile()));

    assertEquals(0, result.getData().getEdges().size());
  }

  @Test
  void should_throw_not_found_when_profile_user_not_found_for_user_feed() {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.userFeed(10, null, null, null, buildProfileDfe(sampleProfile())));
  }

  @Test
  void should_throw_when_both_first_and_last_are_null_for_user_favorites() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFavorites(null, null, null, null, null));
  }

  @Test
  void should_get_user_favorites_using_first() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("testuser"), any(), isNull()))
        .thenReturn(singleNextPager());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(10, null, null, null, buildProfileDfe(sampleProfile()));

    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void should_get_user_favorites_using_last() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("testuser"), any(), isNull()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.PREV, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(null, null, 5, null, buildProfileDfe(sampleProfile()));

    assertEquals(0, result.getData().getEdges().size());
  }

  @Test
  void should_throw_when_both_first_and_last_are_null_for_user_articles() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userArticles(null, null, null, null, null));
  }

  @Test
  void should_get_user_articles_using_first() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("testuser"), isNull(), any(), isNull()))
        .thenReturn(singleNextPager());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(10, null, null, null, buildProfileDfe(sampleProfile()));

    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void should_get_user_articles_using_last() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("testuser"), isNull(), any(), isNull()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.PREV, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(null, null, 5, null, buildProfileDfe(sampleProfile()));

    assertEquals(0, result.getData().getEdges().size());
  }
}
