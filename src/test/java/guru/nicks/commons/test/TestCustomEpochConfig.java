package guru.nicks.commons.test;

import guru.nicks.commons.utils.TimeUtils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

/**
 * Calls {@link TimeUtils#setCustomEpoch(Instant)}.
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@Slf4j
public class TestCustomEpochConfig {


    @PostConstruct
    private void init() {
        TimeUtils.setCustomEpoch(Instant.parse("2024-08-24T00:00:00Z"));
    }

}
