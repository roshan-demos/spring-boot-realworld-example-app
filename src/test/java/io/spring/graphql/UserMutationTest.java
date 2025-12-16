package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.api.security.JwtTokenFilter;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Optional;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, UserMutation.class, MeDatafetcher.class})
@Import({
  WebSecurityConfig.class,
  JwtTokenFilter.class,
  GraphQLCustomizeExceptionHandler.class
})
public class UserMutationTest extends GraphQLTestBase {

  @MockBean private UserService userService;

  @MockBean private PasswordEncoder passwordEncoder;

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  private String defaultAvatar;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";
  }

  @Test
  public void should_create_user_success() {
    String newEmail = "newuser@test.com";
    String newUsername = "newuser";
    String password = "password123";

    User newUser = new User(newEmail, newUsername, password, "", defaultAvatar);
    when(userService.createUser(any())).thenReturn(newUser);
    when(jwtService.toToken(any())).thenReturn("test-token");

    String query =
        String.format(
            "mutation { createUser(input: { email: \"%s\", username: \"%s\", password: \"%s\" }) { "
                + "... on UserPayload { user { email username token } } "
                + "... on Error { message errors { key value } } } }",
            newEmail,
            newUsername,
            password);

    DocumentContext result = dgsQueryExecutor.executeAndGetDocumentContext(query);
    String email = result.read("$.data.createUser.user.email");
    String username = result.read("$.data.createUser.user.username");
    String token = result.read("$.data.createUser.user.token");

    assertThat(email).isEqualTo(newEmail);
    assertThat(username).isEqualTo(newUsername);
    assertThat(token).isEqualTo("test-token");
  }

  @Test
  public void should_return_error_for_invalid_input() {
    String newEmail = "invalid";
    String newUsername = "";
    String password = "";

    ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
    Path path = Mockito.mock(Path.class);
    ConstraintDescriptor<?> descriptor = Mockito.mock(ConstraintDescriptor.class);

    when(path.toString()).thenReturn("registerParam.password");
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn("can't be empty");
    when(violation.getRootBeanClass()).thenReturn((Class) Object.class);
    when(descriptor.getAnnotation()).thenReturn(Mockito.mock(javax.validation.constraints.NotBlank.class));
    doReturn(descriptor).when(violation).getConstraintDescriptor();

    ConstraintViolationException cve =
        new ConstraintViolationException(java.util.Set.of(violation));
    when(userService.createUser(any())).thenThrow(cve);

    String query =
        String.format(
            "mutation { createUser(input: { email: \"%s\", username: \"%s\", password: \"%s\" }) { "
                + "... on UserPayload { user { email username } } "
                + "... on Error { message errors { key value } } } }",
            newEmail,
            newUsername,
            password);

    DocumentContext result = dgsQueryExecutor.executeAndGetDocumentContext(query);
    String message = result.read("$.data.createUser.message");

    assertThat(message).isEqualTo("BAD_REQUEST");
  }

  @Test
  public void should_login_success() {
    String loginEmail = "john@jacob.com";
    String loginPassword = "123";

    User existingUser = new User(loginEmail, "johnjacob", "encoded123", "", defaultAvatar);
    when(userRepository.findByEmail(eq(loginEmail))).thenReturn(Optional.of(existingUser));
    when(passwordEncoder.matches(eq(loginPassword), eq("encoded123"))).thenReturn(true);
    when(jwtService.toToken(any())).thenReturn("login-token");

    String query =
        String.format(
            "mutation { login(email: \"%s\", password: \"%s\") { user { email username token } } }",
            loginEmail,
            loginPassword);

    DocumentContext result = dgsQueryExecutor.executeAndGetDocumentContext(query);
    String email = result.read("$.data.login.user.email");
    String username = result.read("$.data.login.user.username");
    String token = result.read("$.data.login.user.token");

    assertThat(email).isEqualTo(loginEmail);
    assertThat(username).isEqualTo("johnjacob");
    assertThat(token).isEqualTo("login-token");
  }
}
