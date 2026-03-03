package gov.nih.nci.evs.api.controller;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class StaticContextAccessor implements ApplicationContextAware {

  private static ApplicationContext context;

  public static void setContext(ApplicationContext applicationContext) {
    context = applicationContext;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    setContext(applicationContext);
  }

  public static <T> T getBean(Class<T> beanClass) {
    return context.getBean(beanClass);
  }
}
