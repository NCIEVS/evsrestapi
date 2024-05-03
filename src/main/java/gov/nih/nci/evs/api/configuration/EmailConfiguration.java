package gov.nih.nci.evs.api.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Configuration for the Java Mail Sender to send emails TODO: Update to meet our settings */
@Configuration
public class EmailConfiguration {
  // mail server host
  @Value("${mail.host}")
  String host;

  // mail server port
  @Value("${mail.port}")
  int port;

  // mail server username email
  @Value("${mail.username}")
  String username;

  // mail server password
  @Value("${mail.password}")
  String password;

  /**
   * Java mail sender bean. Handles sending the email for the term suggestion forms
   *
   * @return Java Mail Sender
   */
  @Bean
  public JavaMailSender javaMailSender() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    // Configure mailSender properties
    mailSender.setHost(host);
    mailSender.setPort(port);
    mailSender.setUsername(username);
    mailSender.setPassword(password);

    // Additional properties like TLS/SSL settings, etc.

    return mailSender;
  }
}
