package io.spring.application.comment;

import io.spring.application.CommentQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.CommentData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisCommentRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  MyBatisCommentRepository.class,
  MyBatisUserRepository.class,
  CommentQueryService.class,
  MyBatisArticleRepository.class
})
public class CommentQueryServiceTest extends DbTestBase {
  @Autowired private CommentRepository commentRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private CommentQueryService commentQueryService;

  @Autowired private ArticleRepository articleRepository;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("aisensiy@test.com", "aisensiy", "123", "", "");
    userRepository.save(user);
  }

  @Test
  public void should_read_comment_success() {
    Comment comment = new Comment("content", user.getId(), "123");
    commentRepository.save(comment);

    Optional<CommentData> optional = commentQueryService.findById(comment.getId(), user);
    Assertions.assertTrue(optional.isPresent());
    CommentData commentData = optional.get();
    Assertions.assertEquals(commentData.getProfileData().getUsername(), user.getUsername());
  }

  @Test
  public void should_read_comments_of_article() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    articleRepository.save(article);

    User user2 = new User("user2@email.com", "user2", "123", "", "");
    userRepository.save(user2);
    userRepository.saveRelation(new FollowRelation(user.getId(), user2.getId()));

    Comment comment1 = new Comment("content1", user.getId(), article.getId());
    commentRepository.save(comment1);
    Comment comment2 = new Comment("content2", user2.getId(), article.getId());
    commentRepository.save(comment2);

    List<CommentData> comments = commentQueryService.findByArticleId(article.getId(), user);
    Assertions.assertEquals(comments.size(), 2);
  }

  @Test
  public void should_return_empty_when_comment_not_found() {
    Optional<CommentData> result = commentQueryService.findById("nonexistent-id", user);
    Assertions.assertFalse(result.isPresent());
  }

  @Test
  public void should_return_empty_list_when_article_has_no_comments() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    articleRepository.save(article);

    List<CommentData> comments = commentQueryService.findByArticleId(article.getId(), user);
    Assertions.assertTrue(comments.isEmpty());
  }

  @Test
  public void should_return_comments_when_user_is_null() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    articleRepository.save(article);
    Comment comment = new Comment("content", user.getId(), article.getId());
    commentRepository.save(comment);

    List<CommentData> comments = commentQueryService.findByArticleId(article.getId(), null);
    Assertions.assertEquals(1, comments.size());
    Assertions.assertFalse(comments.get(0).getProfileData().isFollowing());
  }

  @Test
  public void should_return_empty_cursor_pager_when_no_comments() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    articleRepository.save(article);

    CursorPager<CommentData> result =
        commentQueryService.findByArticleIdWithCursor(
            article.getId(), user, new CursorPageParameter<>(null, 20, Direction.NEXT));

    Assertions.assertTrue(result.getData().isEmpty());
    Assertions.assertFalse(result.hasNext());
    Assertions.assertFalse(result.hasPrevious());
  }

  @Test
  public void should_return_comment_cursor_pager_with_following_info() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    articleRepository.save(article);

    User follower = new User("follower@test.com", "follower", "123", "", "");
    userRepository.save(follower);
    userRepository.saveRelation(new FollowRelation(follower.getId(), user.getId()));

    Comment comment = new Comment("content", user.getId(), article.getId());
    commentRepository.save(comment);

    CursorPager<CommentData> result =
        commentQueryService.findByArticleIdWithCursor(
            article.getId(), follower, new CursorPageParameter<>(null, 20, Direction.NEXT));

    Assertions.assertEquals(1, result.getData().size());
    Assertions.assertTrue(result.getData().get(0).getProfileData().isFollowing());
    Assertions.assertFalse(result.hasNext());
  }

  @Test
  public void should_return_has_next_when_comments_exceed_limit() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    articleRepository.save(article);
    commentRepository.save(new Comment("first", user.getId(), article.getId()));
    commentRepository.save(new Comment("second", user.getId(), article.getId()));

    CursorPager<CommentData> result =
        commentQueryService.findByArticleIdWithCursor(
            article.getId(), user, new CursorPageParameter<>(null, 1, Direction.NEXT));

    Assertions.assertEquals(1, result.getData().size());
    Assertions.assertTrue(result.hasNext());
    Assertions.assertFalse(result.hasPrevious());
  }

  @Test
  public void should_return_has_previous_when_comments_exceed_limit_prev() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    articleRepository.save(article);
    commentRepository.save(new Comment("first", user.getId(), article.getId()));
    commentRepository.save(new Comment("second", user.getId(), article.getId()));

    CursorPager<CommentData> result =
        commentQueryService.findByArticleIdWithCursor(
            article.getId(), user, new CursorPageParameter<>(null, 1, Direction.PREV));

    Assertions.assertEquals(1, result.getData().size());
    Assertions.assertTrue(result.hasPrevious());
    Assertions.assertFalse(result.hasNext());
  }
}
