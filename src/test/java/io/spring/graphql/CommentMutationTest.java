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
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.User;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.CommentPayload;
import io.spring.graphql.types.DeletionStatus;
import java.util.Arrays;
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
class CommentMutationTest {

  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;

  @InjectMocks private CommentMutation commentMutation;

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

  private CommentData sampleCommentData(String commentId) {
    ProfileData profileData = new ProfileData(user.getId(), "testuser", "", "", false);
    return new CommentData(commentId, "comment body", article.getId(),
        new DateTime(), new DateTime(), profileData);
  }

  @Test
  void should_create_comment_when_authenticated() {
    authenticateAs(user);
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), any())).thenReturn(Optional.of(sampleCommentData("cid")));

    DataFetcherResult<CommentPayload> result = commentMutation.createComment("test-title", "hello");

    assertNotNull(result.getData());
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  void should_throw_authentication_exception_when_creating_comment_unauthenticated() {
    assertThrows(AuthenticationException.class,
        () -> commentMutation.createComment("test-title", "hello"));
  }

  @Test
  void should_delete_comment_when_user_is_article_author() {
    authenticateAs(user);
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment("test-title", comment.getId());

    assertTrue(status.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  void should_delete_comment_when_user_is_comment_author() {
    User articleOwner = new User("owner@test.com", "owner", "pass", "", "");
    Article ownerArticle = new Article("Title", "D", "B", Arrays.asList("t"), articleOwner.getId());
    Comment comment = new Comment("body", user.getId(), ownerArticle.getId());

    authenticateAs(user);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(ownerArticle));
    when(commentRepository.findById(ownerArticle.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment("title", comment.getId());

    assertTrue(status.getSuccess());
  }

  @Test
  void should_throw_no_authorization_when_deleting_other_user_comment() {
    User otherUser = new User("other@test.com", "other", "pass", "", "");
    Comment comment = new Comment("body", otherUser.getId(), article.getId());

    authenticateAs(user);
    Article anotherOwnersArticle = new Article("X", "D", "B", Arrays.asList("t"), otherUser.getId());
    when(articleRepository.findBySlug("x")).thenReturn(Optional.of(anotherOwnersArticle));
    when(commentRepository.findById(anotherOwnersArticle.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThrows(NoAuthorizationException.class,
        () -> commentMutation.removeComment("x", comment.getId()));
  }

  @Test
  void should_throw_not_found_when_comment_does_not_exist() {
    authenticateAs(user);
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "bad-id")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> commentMutation.removeComment("test-title", "bad-id"));
  }
}
