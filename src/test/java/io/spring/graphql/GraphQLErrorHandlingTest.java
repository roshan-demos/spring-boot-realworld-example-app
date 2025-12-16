package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
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
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void should_return_unauthenticated_error_for_protected_mutation() {
    SecurityContextHolder.clearContext();

    String query =
        "mutation { createArticle(input: { title: \"Test\", description: \"Test\", body: \"Test\" }) "
            + "{ article { title } } }";

    DocumentContext result = dgsQueryExecutor.executeAndGetDocumentContext(query);
    List<Object> errors = result.read("$.errors");

    assertThat(errors).isNotNull();
    assertThat(errors).isNotEmpty();

    String errorType = result.read("$.errors[0].extensions.errorType");
    assertThat(errorType).isEqualTo("UNAUTHENTICATED");
  }

  @Test
  public void should_return_bad_request_for_constraint_violations() {
    ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
    Path path = Mockito.mock(Path.class);
    ConstraintDescriptor<?> descriptor = Mockito.mock(ConstraintDescriptor.class);

    when(path.toString()).thenReturn("registerParam.password");
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn("can't be empty");
    when(violation.getRootBeanClass()).thenReturn((Class) Object.class);
    when(descriptor.getAnnotation())
        .thenReturn(Mockito.mock(javax.validation.constraints.NotBlank.class));
    doReturn(descriptor).when(violation).getConstraintDescriptor();

    ConstraintViolationException cve = new ConstraintViolationException(Set.of(violation));
    when(userService.createUser(any())).thenThrow(cve);

    String query =
        "mutation { createUser(input: { email: \"test@test.com\", username: \"testuser\", password: \"\" }) { "
            + "... on UserPayload { user { email } } "
            + "... on Error { message errors { key value } } } }";

    DocumentContext result = dgsQueryExecutor.executeAndGetDocumentContext(query);
    String message = result.read("$.data.createUser.message");

    assertThat(message).isEqualTo("BAD_REQUEST");

    List<Object> errorItems = result.read("$.data.createUser.errors");
    assertThat(errorItems).isNotEmpty();
  }

  @Test
  public void should_handle_multiple_constraint_violations() {
    ConstraintViolation<?> violation1 = Mockito.mock(ConstraintViolation.class);
    Path path1 = Mockito.mock(Path.class);
    ConstraintDescriptor<?> descriptor1 = Mockito.mock(ConstraintDescriptor.class);

    when(path1.toString()).thenReturn("registerParam.email");
    when(violation1.getPropertyPath()).thenReturn(path1);
    when(violation1.getMessage()).thenReturn("should be an email");
    when(violation1.getRootBeanClass()).thenReturn((Class) Object.class);
    when(descriptor1.getAnnotation())
        .thenReturn(Mockito.mock(javax.validation.constraints.Email.class));
    doReturn(descriptor1).when(violation1).getConstraintDescriptor();

    ConstraintViolation<?> violation2 = Mockito.mock(ConstraintViolation.class);
    Path path2 = Mockito.mock(Path.class);
    ConstraintDescriptor<?> descriptor2 = Mockito.mock(ConstraintDescriptor.class);

    when(path2.toString()).thenReturn("registerParam.username");
    when(violation2.getPropertyPath()).thenReturn(path2);
    when(violation2.getMessage()).thenReturn("can't be empty");
    when(violation2.getRootBeanClass()).thenReturn((Class) Object.class);
    when(descriptor2.getAnnotation())
        .thenReturn(Mockito.mock(javax.validation.constraints.NotBlank.class));
    doReturn(descriptor2).when(violation2).getConstraintDescriptor();

    ConstraintViolationException cve =
        new ConstraintViolationException(Set.of(violation1, violation2));
    when(userService.createUser(any())).thenThrow(cve);

    String query =
        "mutation { createUser(input: { email: \"invalid\", username: \"\", password: \"password\" }) { "
            + "... on UserPayload { user { email } } "
            + "... on Error { message errors { key value } } } }";

    DocumentContext result = dgsQueryExecutor.executeAndGetDocumentContext(query);
    String message = result.read("$.data.createUser.message");

    assertThat(message).isEqualTo("BAD_REQUEST");

    List<Object> errorItems = result.read("$.data.createUser.errors");
    assertThat(errorItems).hasSizeGreaterThanOrEqualTo(1);
  }
}
