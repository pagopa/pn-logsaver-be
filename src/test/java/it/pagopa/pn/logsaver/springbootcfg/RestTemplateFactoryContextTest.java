package it.pagopa.pn.logsaver.springbootcfg;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringJUnitConfig(RestTemplateFactory.class)
class RestTemplateFactoryContextTest {

    @Autowired
    private RestTemplate restTemplate;

    @Test
    void restTemplateBeanShouldLoad() {
        assertNotNull(restTemplate);
    }
}
