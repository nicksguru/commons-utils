#@disabled
Feature: Exception formatting utilities

  Scenario Outline: Exception formatting with compact stack trace
    Given an exception of type "<exceptionType>" with message "<message>" is created
    When exception is formatted with compact stack trace
    Then output should contain exception class name "<exceptionType>"
    And output should contain message "<expectedMessage>"
    And output should contain "Stack trace with trivial frames omitted:"
    And trivial frames should be omitted from stack trace
    Examples:
      | exceptionType            | message          | expectedMessage                              |
      | IllegalArgumentException | Invalid argument | IllegalArgumentException('Invalid argument') |
      | NullPointerException     | Null value       | NullPointerException('Null value')           |
      | RuntimeException         |                  | RuntimeException                             |
      | IllegalStateException    | Bad state        | IllegalStateException('Bad state')           |

  Scenario: Exception formatting with null exception
    Given exception is null
    When exception is formatted with compact stack trace
    Then output should be empty

  Scenario Outline: Exception formatting with root cause
    Given an exception of type "<exceptionType>" with message "<message>" is created
    And exception has root cause of type "<rootCauseType>" with message "<rootCauseMessage>"
    When exception is formatted with compact stack trace
    Then output should contain exception class name "<exceptionType>"
    And output should contain message "<expectedMessage>"
    And output should contain root cause "<expectedRootCause>"
    And output should contain "Stack trace with trivial frames omitted:"
    Examples:
      | exceptionType         | message     | rootCauseType            | rootCauseMessage | expectedMessage                      | expectedRootCause                      |
      | RuntimeException      | Wrapper     | IllegalArgumentException | Root error       | RuntimeException('Wrapper')          | IllegalArgumentException('Root error') |
      | IllegalStateException | State error | NullPointerException     | Null found       | IllegalStateException('State error') | NullPointerException('Null found')     |

  Scenario Outline: Stack trace filtering for omitted class prefixes
    Given an exception with stack trace containing "<className>" is created
    When exception is formatted with compact stack trace
    Then stack trace "<shouldContain>" contain "<className>"
    Examples:
      | className                                                                           | shouldContain |
      | brave.servlet.TracingFilter                                                         | should not    |
      | java.lang.invoke.MethodHandle                                                       | should not    |
      | jakarta.servlet.FilterChain                                                         | should not    |
      | javax.servlet.ServletRequest                                                        | should not    |
      | jdk.internal.reflect.NativeMethodAccessorImpl                                       | should not    |
      | org.springframework.cglib.proxy.MethodProxy                                         | should not    |
      | org.springframework.security.web.access.ExceptionTranslationFilter                  | should not    |
      | org.springframework.integration.handler.AbstractMessageHandler                      | should not    |
      | org.springframework.messaging.handler.AbstractMessageHandler                        | should not    |
      | org.springframework.aop.framework.ReflectiveMethodInvocation                        | should not    |
      | org.springframework.security.web.context.SecurityContextPersistenceFilter           | should not    |
      | org.springframework.security.web.header.HeaderWriterFilter                          | should not    |
      | org.springframework.security.web.FilterChainProxy                                   | should not    |
      | org.springframework.security.web.ObservationFilterChainDecorator                    | should not    |
      | org.springframework.security.web.session.SessionManagementFilter                    | should not    |
      | org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter | should not    |
      | org.springframework.boot.actuate.metrics.MetricsEndpoint                            | should not    |
      | reactor.core.publisher.Mono                                                         | should not    |
      | okhttp3.internal.http.HttpCodec                                                     | should not    |
      | io.undertow.servlet.handlers.ServletHandler                                         | should not    |
      | org.jboss.threads.EnhancedQueueExecutor                                             | should not    |
      | org.apache.catalina.core.ApplicationFilterChain                                     | should not    |
      | org.apache.coyote.AbstractProcessor                                                 | should not    |
      | org.apache.tomcat.util.net.NioEndpoint                                              | should not    |
      | guru.nicks.utils.ExceptionUtils                                                     | should        |
      | com.example.MyClass                                                                 | should        |
