package com.borqs.server.market.deploy;


import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class DeploymentPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer implements ApplicationContextAware, InitializingBean {
    private ApplicationContext applicationContext;

    public DeploymentPropertyPlaceholderConfigurer() {

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String propertiesPath = DeploymentModeResolver.getConfigPropertiesPath();
        setLocation(applicationContext.getResource(propertiesPath));
    }
}
