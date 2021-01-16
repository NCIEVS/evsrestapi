package gov.nih.nci.evs.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ApplicationTests {

    /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(ApplicationTests.class);

    @Autowired
    ApplicationContext context;

    @Test
    public void contextLoads() {
        assertThat(this.context).isNotNull();
        log.debug("context loaded successfully");
    }

}
