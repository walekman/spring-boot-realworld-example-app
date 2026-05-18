package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleCommandServiceTest {

  @Mock private ArticleRepository articleRepository;

  @InjectMocks private ArticleCommandService articleCommandService;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User("user@example.com", "testuser", "password", "", "");
  }

  @Test
  void should_create_article_with_correct_fields_and_save() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article result = articleCommandService.createArticle(param, user);

    assertNotNull(result.getId());
    assertEquals("Test Title", result.getTitle());
    assertEquals("Test Description", result.getDescription());
    assertEquals("Test Body", result.getBody());
    assertEquals(user.getId(), result.getUserId());
    verify(articleRepository).save(result);
  }

  @Test
  void should_update_article_fields_when_params_are_non_empty() {
    Article article =
        new Article("Old Title", "Old Desc", "Old Body", Arrays.asList("java"), user.getId());
    UpdateArticleParam updateParam = new UpdateArticleParam("New Title", "New Body", "New Desc");

    Article result = articleCommandService.updateArticle(article, updateParam);

    assertEquals("New Title", result.getTitle());
    assertEquals("New Desc", result.getDescription());
    assertEquals("New Body", result.getBody());
    verify(articleRepository).save(result);
  }

  @Test
  void should_keep_original_fields_when_update_params_are_empty() {
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    UpdateArticleParam updateParam = new UpdateArticleParam("", "", "");

    Article result = articleCommandService.updateArticle(article, updateParam);

    assertEquals("Title", result.getTitle());
    assertEquals("Desc", result.getDescription());
    assertEquals("Body", result.getBody());
    verify(articleRepository).save(result);
  }
}
