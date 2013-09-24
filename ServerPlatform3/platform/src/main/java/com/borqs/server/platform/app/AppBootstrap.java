package com.borqs.server.platform.app;


import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.util.MacroExpander;
import com.borqs.server.platform.util.ProcessHelper;
import com.borqs.server.platform.util.SystemHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;


public class AppBootstrap {
    private static final Logger L = Logger.get(AppBootstrap.class);


    public static void main(String... args) throws Throwable {
        if (args.length < 2) {
            printUsage();
            return;
        }

        try {
            ProcessHelper.checkPidAndSetPidCleaner();

            String path = args[0];
            String mainBeanId = args[1];
            run(path, mainBeanId, Arrays.asList(args).subList(2, args.length).toArray(new String[0]));
        } catch (Throwable t) {
            L.error(null, t, "fatal error");
            throw t;
        }
    }

    private static void printUsage() {
        System.out.printf("java -cp ... %s /your/config/path mainBeanId [args]\n", AppBootstrap.class.getName());
    }

    public static void run(String path, String mainBeanId, String[] args) throws Exception {
        L.info(null, "start app use configuration file " + path);

        ApplicationContext ctx = load(path);
        GlobalSpringAppContext.instance = ctx;
        try {
            AppMain appMain = (AppMain) ctx.getBean(mainBeanId);
            appMain.run(args);
        } finally {
            if (ctx instanceof AbstractApplicationContext)
                ((AbstractApplicationContext) ctx).destroy();
            GlobalSpringAppContext.instance = null;
        }
        L.info(null, "exit app");
    }

    private static ApplicationContext load(String path) {
        Resource[] reses = getXmlResources(path);
        for (Resource res : reses)
            L.info(null, "Load configuration: " + res.getFilename());

        GenericApplicationContext ctx = new GenericApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
        xmlReader.loadBeanDefinitions(reses);
        setPropertyPlaceholder(ctx);
        ctx.refresh();
        return ctx;
    }

    private static ApplicationContext loadDirect(String xml) {
        GenericApplicationContext ctx = new GenericApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
        xmlReader.loadBeanDefinitions(new ByteArrayResource(Charsets.toBytes(xml)));
        setPropertyPlaceholder(ctx);
        ctx.refresh();
        return ctx;
    }

    private static void setPropertyPlaceholder(GenericApplicationContext ctx) {
        String propPath = MacroExpander.expandSystemMacros(SystemHelper.isLocalRun() ? "${BS_HOME}/etc/local/conf.properties" : "${BS_HOME}/etc/conf.properties");
        if (!new File(propPath).exists())
             return;

        PropertyPlaceholderConfigurer cfg = new PropertyPlaceholderConfigurer();
        cfg.setSearchSystemEnvironment(true);
        cfg.setLocation(new FileSystemResource(propPath));
        cfg.postProcessBeanFactory(ctx.getBeanFactory());
    }

    public static void init(String path) {
        GlobalSpringAppContext.instance = load(path);
    }

    public static void initDirect(String xml) {
        GlobalSpringAppContext.instance = loadDirect(xml);
    }


    private static Resource[] getXmlResources(String path) {
        path = StringUtils.removeStart(path, "file://");
        ArrayList<Resource> l = new ArrayList<Resource>();
        l.add(new FileSystemResource(path));
//        try {
//
//            String xml = FileUtils.readFileToString(new File(path), Charsets.DEFAULT);
//            Element root = XmlHelper.loadDocument(xml).getDocumentElement();
//            NodeList children = root.getChildNodes();
//            for (int i = 0; i < children.getLength(); i++) {
//                Node node = children.item(i);
//                if (node instanceof Element && ((Element)node).getTagName().equals("import")) {
//                    String res = ((Element)node).getAttribute("resource");
//                    res = FilenameUtils.concat(FilenameUtils.getFullPath(path), res);
//                    l.add(new FileSystemResource(res));
//                }
//            }
//        } catch (IOException e) {
//            throw new ServerException(E.IO, e);
//        } catch (ParserConfigurationException e) {
//            throw new ServerException(E.IO, e);
//        } catch (SAXException e) {
//            throw new ServerException(E.IO, e);
//        }
        return l.toArray(new Resource[l.size()]);
    }


    public static void destroy() {
        try {
            if (GlobalSpringAppContext.instance instanceof AbstractApplicationContext)
                ((AbstractApplicationContext) GlobalSpringAppContext.instance).destroy();
        } finally {
            GlobalSpringAppContext.instance = null;
        }
    }
}
