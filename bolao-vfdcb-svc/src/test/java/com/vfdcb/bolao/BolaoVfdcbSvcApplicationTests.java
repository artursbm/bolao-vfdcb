package com.vfdcb.bolao;

import com.vfdcb.bolao.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BolaoVfdcbSvcApplicationTests {

	@Test
	void contextLoads() {
	}

}
