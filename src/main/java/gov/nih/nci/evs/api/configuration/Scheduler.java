package gov.nih.nci.evs.api.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Scheduler. */
@Configuration
@EnableScheduling
public class Scheduler {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(Scheduler.class);

  /** Instantiates an empty {@link Scheduler}. */
  public Scheduler() {
    log.debug("Creating instance of class Scheduler");
  }
}
