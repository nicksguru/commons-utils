@utils #@disabled
Feature: Resilience4j Utils
  Regression coverage for shared-registry reuse: factories must return the same instance for the same
  name so that state (metrics, breaker status, rate limit windows) accumulates across calls.

  Scenario: Same-name retriers share one instance and accumulate metrics
    When two default retriers are created with the same name "regression-retry"
    Then the retriers should be the same instance
    And the retrier metrics should accumulate across successful executions

  Scenario: Same-name circuit breakers share one instance and accumulate metrics
    When two default circuit breakers are created with the same name "regression-breaker"
    Then the circuit breakers should be the same instance
    And the circuit breaker metrics should accumulate across successful executions

  Scenario: Same-name rate limiters share one instance
    When two default rate limiters are created with the same name "regression-rate-limiter"
    Then the rate limiters should be the same instance
