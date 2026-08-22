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

  Scenario: Exception formatting without message omits empty message parentheses
    Given an exception of type "RuntimeException" with message "" is created
    When exception is formatted with compact stack trace
    Then output should contain exception class name "RuntimeException"
    And stack trace "should not" contain "('"

  Scenario: Exception formatting with messageless root cause omits empty root cause parentheses
    Given an exception of type "RuntimeException" with message "Wrapper" is created
    And exception has root cause of type "IllegalArgumentException" with message ""
    When exception is formatted with compact stack trace
    Then output should contain root cause "IllegalArgumentException"
    And stack trace "should not" contain "IllegalArgumentException('"

  Scenario: Exception formatting when root cause is the exception itself
    Given exception root cause is the exception itself
    When exception is formatted with compact stack trace
    Then output should contain exception class name "IllegalStateException"
    And stack trace "should not" contain "with root cause"

  Scenario: Plain exception is returned unchanged when unwrapping InvocationTargetException
    Given an exception of type "IllegalStateException" with message "Plain" is created
    When the exception is unwrapped from InvocationTargetException
    Then the unwrapped exception should be the exception itself

  Scenario: Single InvocationTargetException wrapper is unwrapped
    Given an exception of type "IllegalStateException" with message "Target" is created
    And the exception is wrapped in 1 nested InvocationTargetExceptions
    When the exception is unwrapped from InvocationTargetException
    Then the unwrapped exception should be the original cause

  Scenario: Nested InvocationTargetException chain is unwrapped to the innermost exception
    Given an exception of type "IllegalStateException" with message "Innermost" is created
    And the exception is wrapped in 3 nested InvocationTargetExceptions
    When the exception is unwrapped from InvocationTargetException
    Then the unwrapped exception should be the original cause

  Scenario: InvocationTargetException without a target is returned as-is
    Given the exception is wrapped in an InvocationTargetException without a target
    When the exception is unwrapped from InvocationTargetException
    Then the unwrapped exception should be the exception itself

  Scenario: Sneaky throw rethrows an unchecked exception unchanged
    Given an exception of type "IllegalStateException" with message "Unchecked failure" is created
    When the exception is sneaky-thrown
    Then the exception should be of type "IllegalStateException"
    And the exception message should contain "Unchecked failure"
    And the sneaky-thrown exception should be the exception itself

  Scenario: Sneaky throw rethrows a checked exception unchanged and unwrapped
    Given a checked exception with message "Checked failure" is created
    When the exception is sneaky-thrown
    Then the exception should be of type "ReflectiveOperationException"
    And the exception message should contain "Checked failure"
    And the sneaky-thrown exception should be the exception itself

  Scenario: Chain of exactly 100 nested InvocationTargetExceptions is unwrapped fully
    Given an exception of type "IllegalStateException" with message "Innermost" is created
    And the exception is wrapped in 100 nested InvocationTargetExceptions
    When the exception is unwrapped from InvocationTargetException
    Then the unwrapped exception should be the exception at unwrapping depth 100
    And the unwrapped exception should be the original cause

  Scenario: Chain deeper than 100 nested InvocationTargetExceptions is unwrapped up to the depth limit
    Given an exception of type "IllegalStateException" with message "Innermost" is created
    And the exception is wrapped in 150 nested InvocationTargetExceptions
    When the exception is unwrapped from InvocationTargetException
    Then the unwrapped exception should be the exception at unwrapping depth 100
    And the unwrapped exception should not be the original cause
    And the unwrapped exception should be an InvocationTargetException

  Scenario: Exception factory creates an exception of the requested class with the cause set
    Given an original cause exception is created
    When an exception factory is obtained for the IllegalStateException class
    And the exception factory is applied to the original cause
    Then no exception should be thrown
    And the factory-created exception should be of type IllegalStateException
    And the factory-created exception cause should be the original cause

  Scenario: Exception factory is cached
    When an exception factory is obtained for the IllegalStateException class
    And the exception factory is obtained again
    Then no exception should be thrown
    And the exception factory should be cached

  Scenario: Exception factory requires a public Throwable constructor
    When an exception factory is obtained for the NoCauseConstructorException class
    Then an exception should be thrown
    And IllegalStateException should be thrown
    And the exception message should name the NoCauseConstructorException class

  Scenario: Business exception factory creates an exception of the requested class with the cause set
    Given an original cause exception is created
    When a business exception factory is obtained for the TestBusinessException class
    And the business exception factory is applied to the original cause
    Then no exception should be thrown
    And the factory-created exception should be of type TestBusinessException
    And the factory-created exception cause should be the original cause

  Scenario: Business exception factory is cached
    When a business exception factory is obtained for the TestBusinessException class
    And the business exception factory is obtained again
    Then no exception should be thrown
    And the business exception factory should be cached

  Scenario: Business exception factory requires a public Throwable constructor
    When a business exception factory is obtained for the NoThrowableConstructorException class
    Then an exception should be thrown
    And IllegalStateException should be thrown
    And the exception message should name the NoThrowableConstructorException class
