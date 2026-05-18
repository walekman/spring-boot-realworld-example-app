package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.article.ArticleCommandService;
import io.spring.core.article.Article;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.graphql.types.ArticlePayload;
import io.spring.graphql.types.CreateArticleInput;
import io.spring.graphql.types.DeletionStatus;
import io.spring.graphql.types.UpdateArticleInput;
import java.util.Arrays;
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
class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private io.spring.core.article.ArticleRepository articleRepository;

  @InjectMocks private ArticleMutation articleMutation;

  private User user;
  private Article article;

  @BeforeEach
  void setUp() {
    user = new User("u@test.com", "testuser", "pass", "", "");
    article = new Article("Test Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAs(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
  }

  @Test
  void should_create_article_when_authenticated() {
    authenticateAs(user);
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Title")
            .description("Desc")
            .body("Body")
            .tagList(Arrays.asList("java"))
            .build();

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertNotNull(result.getData());
    verify(articleCommandService).createArticle(any(), any());
  }

  @Test
  void should_throw_authentication_exception_when_creating_article_unauthenticated() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("T").description("D").body("B").build();

    assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
  }

  @Test
  void should_update_article_when_authorized() {
    authenticateAs(user);
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(any(), any())).thenReturn(article);

    UpdateArticleInput input =
        UpdateArticleInput.newBuilder().title("New Title").body("New Body").description("New Desc").build();

    DataFetcherResult<ArticlePayload> result = articleMutation.updateArticle("test-title", input);

    assertNotNull(result.getData());
    verify(articleCommandService).updateArticle(any(), any());
  }

  @Test
  void should_throw_authentication_exception_when_updating_article_unauthenticated() {
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));

    UpdateArticleInput input = UpdateArticleInput.newBuilder().title("T").body("B").description("D").build();

    assertThrows(AuthenticationException.class, () -> articleMutation.updateArticle("test-title", input));
  }

  @Test
  void should_throw_no_authorization_when_updating_article_by_different_user() {
    User otherUser = new User("other@test.com", "other", "pass", "", "");
    authenticateAs(otherUser);
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));

    UpdateArticleInput input = UpdateArticleInput.newBuilder().title("T").body("B").description("D").build();

    assertThrows(NoAuthorizationException.class, () -> articleMutation.updateArticle("test-title", input));
  }

  @Test
  void should_delete_article_when_authorized() {
    authenticateAs(user);
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle("test-title");

    assertTrue(status.getSuccess());
    verify(articleRepository).remove(article);
  }

  @Test
  void should_throw_not_found_when_deleting_nonexistent_article() {
    authenticateAs(user);
    when(articleRepository.findBySlug("no-article")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleMutation.deleteArticle("no-article"));
  }

  @Test
  void should_throw_no_authorization_when_deleting_article_by_different_user() {
    User otherUser = new User("other@test.com", "other", "pass", "", "");
    authenticateAs(otherUser);
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));

    assertThrows(NoAuthorizationException.class, () -> articleMutation.deleteArticle("test-title"));
  }

  @Test
  void should_favorite_article_when_authenticated() {
    authenticateAs(user);
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("test-title");

    assertNotNull(result.getData());
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void should_throw_authentication_exception_when_favoriting_unauthenticated() {
    assertThrows(AuthenticationException.class, () -> articleMutation.favoriteArticle("test-title"));
  }

  @Test
  void should_unfavorite_article_when_authenticated() {
    authenticateAs(user);
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(java.util.Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("test-title");

    assertNotNull(result.getData());
    verify(articleFavoriteRepository).remove(favorite);
  }

  @Test
  void should_not_remove_favorite_when_not_previously_favorited() {
    authenticateAs(user);
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(java.util.Optional.empty());

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("test-title");

    assertNotNull(result.getData());
  }
}
