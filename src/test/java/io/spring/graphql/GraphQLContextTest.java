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
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
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
    classes = {
      DgsAutoConfiguration.class,
      ArticleDatafetcher.class,
      MeDatafetcher.class,
      ProfileDatafetcher.class
    })
@Import({
  WebSecurityConfig.class,
  JwtTokenFilter.class,
  GraphQLCustomizeExceptionHandler.class
})
public class GraphQLContextTest extends GraphQLTestBase {

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ProfileQueryService profileQueryService;

  @MockBean private io.spring.application.UserQueryService userQueryService;

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  private ArticleData articleData;
  private ProfileData authorProfileData;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();

    authorProfileData =
        new ProfileData("author-id", "authoruser", "Author bio", defaultAvatar, false);

    DateTime now = DateTime.now();
    articleData =
        new ArticleData(
            "article-id",
            "test-article",
            "Test Article",
            "Description",
            "Body",
            false,
            5,
            now,
            now,
            Arrays.asList("tag1", "tag2"),
            authorProfileData);
  }

    @Test
    public void should_resolve_article_with_nested_author_profile() {
      when(articleQueryService.findBySlug(eq("test-article"), any()))
          .thenReturn(Optional.of(articleData));
      when(profileQueryService.findByUsername(eq("authoruser"), any()))
          .thenReturn(Optional.of(authorProfileData));

      SecurityContextHolder.getContext()
          .setAuthentication(
              new AnonymousAuthenticationToken(
                  "anonymous",
                  "anonymousUser",
                  Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

      String query =
          "query { article(slug: \"test-article\") { slug title author { username bio following } } }";

      try {
        DocumentContext result = dgsQueryExecutor.executeAndGetDocumentContext(query);

        String slug = result.read("$.data.article.slug");
        String title = result.read("$.data.article.title");
        String authorUsername = result.read("$.data.article.author.username");
        String authorBio = result.read("$.data.article.author.bio");
        Boolean following = result.read("$.data.article.author.following");

        assertThat(slug).isEqualTo("test-article");
        assertThat(title).isEqualTo("Test Article");
        assertThat(authorUsername).isEqualTo("authoruser");
        assertThat(authorBio).isEqualTo("Author bio");
        assertThat(following).isFalse();
      } finally {
        SecurityContextHolder.clearContext();
      }
    }

  @Test
  public void should_resolve_author_following_status_from_local_context() {
    ProfileData followingAuthorProfile =
        new ProfileData("author-id", "authoruser", "Author bio", defaultAvatar, true);

    ArticleData articleWithFollowingAuthor =
        new ArticleData(
            "article-id",
            "test-article",
            "Test Article",
            "Description",
            "Body",
            false,
            5,
            DateTime.now(),
            DateTime.now(),
            Arrays.asList("tag1"),
            followingAuthorProfile);

    when(articleQueryService.findBySlug(eq("test-article"), any()))
        .thenReturn(Optional.of(articleWithFollowingAuthor));
    when(profileQueryService.findByUsername(eq("authoruser"), any()))
        .thenReturn(Optional.of(followingAuthorProfile));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

    String query =
        "query { article(slug: \"test-article\") { slug author { username following } } }";

    try {
      DocumentContext result = dgsQueryExecutor.executeAndGetDocumentContext(query);

      String authorUsername = result.read("$.data.article.author.username");
      Boolean following = result.read("$.data.article.author.following");

      assertThat(authorUsername).isEqualTo("authoruser");
      assertThat(following).isTrue();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  public void should_pass_local_context_between_article_and_profile_resolvers() {
    when(articleQueryService.findBySlug(eq("test-article"), any()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq("authoruser"), any()))
        .thenReturn(Optional.of(authorProfileData));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "anonymous",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    String query =
        "query { article(slug: \"test-article\") { slug title description body favorited "
            + "favoritesCount tagList author { username bio image following } } }";

    try {
      DocumentContext result = dgsQueryExecutor.executeAndGetDocumentContext(query);

      String slug = result.read("$.data.article.slug");
      String title = result.read("$.data.article.title");
      String description = result.read("$.data.article.description");
      String body = result.read("$.data.article.body");
      Boolean favorited = result.read("$.data.article.favorited");
      Integer favoritesCount = result.read("$.data.article.favoritesCount");

      assertThat(slug).isEqualTo("test-article");
      assertThat(title).isEqualTo("Test Article");
      assertThat(description).isEqualTo("Description");
      assertThat(body).isEqualTo("Body");
      assertThat(favorited).isFalse();
      assertThat(favoritesCount).isEqualTo(5);

      String authorUsername = result.read("$.data.article.author.username");
      String authorBio = result.read("$.data.article.author.bio");
      String authorImage = result.read("$.data.article.author.image");
      Boolean following = result.read("$.data.article.author.following");

      assertThat(authorUsername).isEqualTo("authoruser");
      assertThat(authorBio).isEqualTo("Author bio");
      assertThat(authorImage).isEqualTo(defaultAvatar);
      assertThat(following).isFalse();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
