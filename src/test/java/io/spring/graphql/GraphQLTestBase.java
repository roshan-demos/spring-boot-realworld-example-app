package io.spring.graphql;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.api.security.JwtTokenFilter;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  DgsAutoConfiguration.class,
  WebSecurityConfig.class,
  JwtTokenFilter.class,
  GraphQLCustomizeExceptionHandler.class
})
abstract class GraphQLTestBase {
  @MockBean protected UserRepository userRepository;

  @MockBean protected UserReadService userReadService;

  protected User user;
  protected UserData userData;
  protected String token;
  protected String email;
  protected String username;
  protected String defaultAvatar;

  @MockBean protected JwtService jwtService;

  @Autowired protected DgsQueryExecutor dgsQueryExecutor;

  protected void userFixture() {
    email = "john@jacob.com";
    username = "johnjacob";
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";

    user = new User(email, username, "123", "", defaultAvatar);
    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.of(user));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    userData = new UserData(user.getId(), email, username, "", defaultAvatar);
    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);

    token = "token";
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
  }

  @BeforeEach
  public void setUp() throws Exception {
    userFixture();
  }

  protected String executeQuery(String query) {
    return dgsQueryExecutor.executeAndGetDocumentContext(query).jsonString();
  }

  protected String executeQueryWithAuth(String query) {
    org.springframework.security.core.context.SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user, null, java.util.Collections.emptyList()));
    try {
      return dgsQueryExecutor.executeAndGetDocumentContext(query).jsonString();
    } finally {
      org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
  }
}
