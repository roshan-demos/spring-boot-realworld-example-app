package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.api.security.JwtTokenFilter;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.user.UserService;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      UserMutation.class,
      ArticleMutation.class,
      MeDatafetcher.class,
      ArticleDatafetcher.class
    })
@Import({
  WebSecurityConfig.class,
  JwtTokenFilter.class,
  GraphQLCustomizeExceptionHandler.class
})
public class GraphQLErrorHandlingTest extends GraphQLTestBase {

  @MockBean private UserService userService;

  @MockBean private PasswordEncoder passwordEncoder;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

    @MockBean private ArticleQueryService articleQueryService;

    @MockBean private io.spring.application.UserQueryService userQueryService;

    @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
  }

        @Test
      public void should_return_unauthenticated_error_for_protected_mutation() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new AnonymousAuthenticationToken(
                    "anonymous",
                    "anonymousUser",
                    java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

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
  public void should_return_error_for_runtime_exception() {
    when(userService.createUser(any()))
        .thenThrow(new RuntimeException("email already exists"));

    String query =
        "mutation { createUser(input: { email: \"test@test.com\", username: \"testuser\", password: \"password\" }) { "
            + "... on UserPayload { user { email } } "
            + "... on Error { message errors { key value } } } }";

    graphql.ExecutionResult result = dgsQueryExecutor.execute(query);
    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_return_error_for_invalid_login() {
    when(userRepository.findByEmail(any())).thenReturn(java.util.Optional.empty());

    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "anonymous",
                "anonymousUser",
                java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    String query = "mutation { login(email: \"nonexistent@test.com\", password: \"wrong\") { user { email } } }";

    try {
      graphql.ExecutionResult result = dgsQueryExecutor.execute(query);
      assertThat(result.getErrors()).isNotEmpty();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
