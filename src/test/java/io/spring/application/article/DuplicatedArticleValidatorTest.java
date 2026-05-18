package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuplicatedArticleValidatorTest {

  @Mock private ArticleQueryService articleQueryService;

  @InjectMocks private DuplicatedArticleValidator validator;

  @Test
  void should_return_true_when_title_is_not_duplicated() {
    String title = "Unique Article Title";
    when(articleQueryService.findBySlug(Article.toSlug(title), null)).thenReturn(Optional.empty());

    assertTrue(validator.isValid(title, null));
  }

  @Test
  void should_return_false_when_title_is_duplicated() {
    String title = "Existing Article Title";
    when(articleQueryService.findBySlug(Article.toSlug(title), null))
        .thenReturn(Optional.of(mock(ArticleData.class)));

    assertFalse(validator.isValid(title, null));
  }
}
