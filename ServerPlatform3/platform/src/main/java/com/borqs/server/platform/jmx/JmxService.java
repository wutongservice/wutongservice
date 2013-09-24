package com.borqs.server.platform.jmx;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.service.Service;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;
import org.springframework.jmx.support.ConnectorServerFactoryBean;

import javax.management.JMException;
import javax.management.MBeanServer;
import java.io.IOException;
import java.util.Map;

public class JmxService implements Service {
    public static final String DEFAULT_SERVICE_URL = "service:jmx:jmxmp://localhost:9875";

    private volatile ConnectorServerFactoryBean server;
    private volatile MBeanExporter beanExporter;
    private String address = null;

    private Map<String, Object> beans;

    public JmxService() {
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Map<String, Object> getBeans() {
        return beans;
    }

    public void setBeans(Map<String, Object> beans) {
        this.beans = beans;
    }

    private void startMBeanServer() {
        boolean startSuccess = false;
        try {
            server = new ConnectorServerFactoryBean();
            beanExporter = new MBeanExporter();

            // setup server
            server.setServiceUrl(StringUtils.isBlank(address) ? DEFAULT_SERVICE_URL : address);
            beanExporter.setServer((MBeanServer) server.getObject());

            // setup assembler
            MetadataMBeanInfoAssembler assembler = new MetadataMBeanInfoAssembler();
            assembler.setAttributeSource(new AnnotationJmxAttributeSource());
            beanExporter.setAssembler(assembler);

            // setup beans
            if (MapUtils.isNotEmpty(beans))
                beanExporter.setBeans(beans);

            // Go!
            server.afterPropertiesSet();
            beanExporter.afterPropertiesSet();
            startSuccess = true;
        } catch (JMException e) {
            throw new ServerException(E.JMX, e);
        } catch (IOException e) {
            throw new ServerException(E.JMX, e);
        } finally {
            try {
                if (!startSuccess)
                    stop();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void start() {
        if (beanExporter != null)
            throw new IllegalStateException("The JMX service is started");

        try {
            startMBeanServer();
        } catch (Exception e) {
            throw new ServerException(E.JMX, "Start JMX service error", e);
        }
    }

    @Override
    public void stop() {
        try {
            if (beanExporter != null) {
                server.destroy();
                beanExporter.destroy();
            }
        } catch (IOException e) {
            throw new ServerException(E.JMX, e);
        } finally {
            server = null;
            beanExporter = null;
        }
    }

    @Override
    public boolean isStarted() {
        return beanExporter != null;
    }
}
