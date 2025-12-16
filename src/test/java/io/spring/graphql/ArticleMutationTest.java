package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.api.security.JwtTokenFilter;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(
    classes = {DgsAutoConfiguration.class, ArticleMutation.class, ArticleDatafetcher.class})
@Import({
  WebSecurityConfig.class,
  JwtTokenFilter.class,
  GraphQLCustomizeExceptionHandler.class
})
public class ArticleMutationTest extends GraphQLTestBase {

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean private ArticleQueryService articleQueryService;

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void should_create_article_with_authentication() {
    String title = "Test Article";
    String description = "Test Description";
    String body = "Test Body";

    Article article = new Article(title, description, body, Arrays.asList("tag1", "tag2"), user.getId());
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    ProfileData profileData = new ProfileData(user.getId(), username, "", defaultAvatar, false);
    ArticleData articleData =
        new ArticleData(
            article.getId(),
            article.getSlug(),
            title,
            description,
            body,
            false,
            0,
            DateTime.now(),
            DateTime.now(),
            Arrays.asList("tag1", "tag2"),
            profileData);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

    String query =
        String.format(
            "mutation { createArticle(input: { title: \"%s\", description: \"%s\", body: \"%s\", "
                + "tagList: [\"tag1\", \"tag2\"] }) { article { title description body tagList slug } } }",
            title,
            description,
            body);

    try {
      DocumentContext result = dgsQueryExecutor.executeAndGetDocumentContext(query);
      String resultTitle = result.read("$.data.createArticle.article.title");
      String resultDescription = result.read("$.data.createArticle.article.description");
      String resultBody = result.read("$.data.createArticle.article.body");

      assertThat(resultTitle).isEqualTo(title);
      assertThat(resultDescription).isEqualTo(description);
      assertThat(resultBody).isEqualTo(body);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

        @Test
      public void should_fail_create_article_without_authentication() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new AnonymousAuthenticationToken(
                    "anonymous",
                    "anonymousUser",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        String query =
            "mutation { createArticle(input: { title: \"Test\", description: \"Test\", body: \"Test\" }) "
                + "{ article { title } } }";

        try {
          graphql.ExecutionResult result = dgsQueryExecutor.execute(query);
          assertThat(result.getErrors()).isNotEmpty();
        } finally {
          SecurityContextHolder.clearContext();
        }
      }

  @Test
  public void should_favorite_article_and_update_count() {
    String slug = "test-article";
    Article article =
        new Article("Test Article", "Description", "Body", Collections.emptyList(), user.getId());

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    ProfileData profileData = new ProfileData(user.getId(), username, "", defaultAvatar, false);
    ArticleData articleData =
        new ArticleData(
            article.getId(),
            slug,
            "Test Article",
            "Description",
            "Body",
            true,
            1,
            DateTime.now(),
            DateTime.now(),
            Collections.emptyList(),
            profileData);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

    String query =
        String.format(
            "mutation { favoriteArticle(slug: \"%s\") { article { slug favorited favoritesCount } } }",
            slug);

    try {
      DocumentContext result = dgsQueryExecutor.executeAndGetDocumentContext(query);
      String resultSlug = result.read("$.data.favoriteArticle.article.slug");
      Boolean favorited = result.read("$.data.favoriteArticle.article.favorited");
      Integer favoritesCount = result.read("$.data.favoriteArticle.article.favoritesCount");

      assertThat(resultSlug).isEqualTo(slug);
      assertThat(favorited).isTrue();
      assertThat(favoritesCount).isEqualTo(1);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
