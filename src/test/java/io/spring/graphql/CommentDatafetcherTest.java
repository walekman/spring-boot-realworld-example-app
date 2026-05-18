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
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.CommentsConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CommentDatafetcherTest {

  @Mock private CommentQueryService commentQueryService;

  @InjectMocks private CommentDatafetcher commentDatafetcher;

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
    ProfileData profileData = new ProfileData("user-id", "author", "", "", false);
    return new ArticleData(
        "article-id", "test-slug", "Test Title", "Desc", "Body",
        false, 0, new DateTime(), new DateTime(),
        Arrays.asList("java"), profileData);
  }

  private CommentData sampleCommentData() {
    ProfileData profileData = new ProfileData("user-id", "author", "", "", false);
    return new CommentData(
        "comment-id", "comment body", "article-id", new DateTime(), new DateTime(), profileData);
  }

  private DgsDataFetchingEnvironment buildDfe(ArticleData articleData) {
    Article graphqlArticle = Article.newBuilder().slug(articleData.getSlug()).build();
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put(articleData.getSlug(), articleData);

    DgsDataFetchingEnvironment dfe = mock(DgsDataFetchingEnvironment.class);
    when(dfe.getSource()).thenReturn(graphqlArticle);
    when(dfe.getLocalContext()).thenReturn(localContext);
    return dfe;
  }

  @Test
  void should_throw_when_both_first_and_last_are_null() {
    // dfe is not reached before the guard throws, so null is fine here
    assertThrows(
        IllegalArgumentException.class,
        () -> commentDatafetcher.articleComments(null, null, null, null, null));
  }

  @Test
  void should_return_comments_using_first() {
    ArticleData articleData = sampleArticleData();
    DgsDataFetchingEnvironment dfe = buildDfe(articleData);
    CursorPager<CommentData> pager =
        new CursorPager<>(Arrays.asList(sampleCommentData()), Direction.NEXT, false);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), isNull(), any()))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dfe);

    assertNotNull(result.getData());
    assertEquals(1, result.getData().getEdges().size());
    assertEquals("comment-id", result.getData().getEdges().get(0).getNode().getId());
  }

  @Test
  void should_return_empty_comments_using_last_direction() {
    ArticleData articleData = sampleArticleData();
    DgsDataFetchingEnvironment dfe = buildDfe(articleData);
    CursorPager<CommentData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.PREV, false);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), isNull(), any()))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 10, null, dfe);

    assertNotNull(result.getData());
    assertEquals(0, result.getData().getEdges().size());
  }
}
