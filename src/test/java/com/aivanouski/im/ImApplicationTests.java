package com.aivanouski.im;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.aivanouski.im.testsupport.TestcontainersConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ImApplicationTests {

    @Test
    void contextLoads() {
    }

}
