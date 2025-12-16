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
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    classes = {DgsAutoConfiguration.class, ArticleDatafetcher.class, ProfileDatafetcher.class})
@Import({
  WebSecurityConfig.class,
  JwtTokenFilter.class,
  GraphQLCustomizeExceptionHandler.class
})
public class ArticleDatafetcherTest extends GraphQLTestBase {

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private io.spring.application.ProfileQueryService profileQueryService;

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  private ArticleData articleData1;
  private ArticleData articleData2;
  private ArticleData articleData3;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();

    profileData = new ProfileData(user.getId(), username, "", defaultAvatar, false);

    DateTime now = DateTime.now();
    articleData1 =
        new ArticleData(
            "id1",
            "test-article-1",
            "Test Article 1",
            "Description 1",
            "Body 1",
            false,
            0,
            now,
            now,
            Arrays.asList("tag1"),
            profileData);

    articleData2 =
        new ArticleData(
            "id2",
            "test-article-2",
            "Test Article 2",
            "Description 2",
            "Body 2",
            false,
            0,
            now.minusHours(1),
            now.minusHours(1),
            Arrays.asList("tag2"),
            profileData);

    articleData3 =
        new ArticleData(
            "id3",
            "test-article-3",
            "Test Article 3",
            "Description 3",
            "Body 3",
            false,
            0,
            now.minusHours(2),
            now.minusHours(2),
            Arrays.asList("tag3"),
            profileData);

    when(profileQueryService.findByUsername(eq(username), any()))
        .thenReturn(Optional.of(profileData));
  }

  @Test
  public void should_get_articles_with_pagination() {
    List<ArticleData> articles = Arrays.asList(articleData1, articleData2, articleData3);
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, true);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "anonymous",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    String query =
        "query { articles(first: 3) { edges { cursor node { slug title description } } "
            + "pageInfo { hasNextPage hasPreviousPage startCursor endCursor } } }";

    try {
      DocumentContext result = dgsQueryExecutor.executeAndGetDocumentContext(query);

      List<Object> edges = result.read("$.data.articles.edges");
      assertThat(edges).hasSize(3);

      String firstSlug = result.read("$.data.articles.edges[0].node.slug");
      assertThat(firstSlug).isEqualTo("test-article-1");

      Boolean hasNextPage = result.read("$.data.articles.pageInfo.hasNextPage");
      assertThat(hasNextPage).isTrue();

      Boolean hasPreviousPage = result.read("$.data.articles.pageInfo.hasPreviousPage");
      assertThat(hasPreviousPage).isFalse();

      String startCursor = result.read("$.data.articles.pageInfo.startCursor");
      assertThat(startCursor).isNotNull();

      String endCursor = result.read("$.data.articles.pageInfo.endCursor");
      assertThat(endCursor).isNotNull();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  public void should_get_article_by_slug() {
    when(articleQueryService.findBySlug(eq("test-article-1"), any()))
        .thenReturn(Optional.of(articleData1));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "anonymous",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    String query =
        "query { article(slug: \"test-article-1\") { slug title description body tagList favorited favoritesCount } }";

    try {
      DocumentContext result = dgsQueryExecutor.executeAndGetDocumentContext(query);

      String slug = result.read("$.data.article.slug");
      String title = result.read("$.data.article.title");
      String description = result.read("$.data.article.description");
      String body = result.read("$.data.article.body");

      assertThat(slug).isEqualTo("test-article-1");
      assertThat(title).isEqualTo("Test Article 1");
      assertThat(description).isEqualTo("Description 1");
      assertThat(body).isEqualTo("Body 1");
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  public void should_get_feed_with_authentication() {
    List<ArticleData> articles = Arrays.asList(articleData1, articleData2);
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);

    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(cursorPager);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

    String query =
        "query { feed(first: 10) { edges { cursor node { slug title } } "
            + "pageInfo { hasNextPage hasPreviousPage } } }";

    try {
      DocumentContext result = dgsQueryExecutor.executeAndGetDocumentContext(query);

      List<Object> edges = result.read("$.data.feed.edges");
      assertThat(edges).hasSize(2);

      String firstSlug = result.read("$.data.feed.edges[0].node.slug");
      assertThat(firstSlug).isEqualTo("test-article-1");

      Boolean hasNextPage = result.read("$.data.feed.pageInfo.hasNextPage");
      assertThat(hasNextPage).isFalse();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
