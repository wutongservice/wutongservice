package com.borqs.server.base.web.template;


import com.borqs.server.base.io.Charsets;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.base.web.WebException;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;

public class PageTemplate {
    private final Configuration config;

    public PageTemplate(File file) {
        this("file://" + file.getPath());
    }

    public PageTemplate(Class clazz) {
        this("classpath://" + clazz.getName());
    }

    public PageTemplate(String... dirs) {
        try {
            ArrayList<TemplateLoader> loaders = new ArrayList<TemplateLoader>();
            for (String dir : dirs) {
                if (dir.startsWith("classpath://")) {
                    String className = StringUtils.removeStart(dir, "classpath://");
                    loaders.add(new ClassTemplateLoader(ClassUtils2.forName(className), ""));
                } else if (dir.startsWith("file://")) {
                    String dirName = StringUtils.removeStart(dir, "file://");
                    loaders.add(new FileTemplateLoader(new File(dirName)));
                } else {
                    loaders.add(new FileTemplateLoader(new File(dir)));
                }
            }

            config = new Configuration();
            if (loaders.size() == 1) {
                config.setTemplateLoader(loaders.get(0));
            } else if (loaders.size() > 1) {
                config.setTemplateLoader(new MultiTemplateLoader(loaders.toArray(new TemplateLoader[loaders.size()])));
            }
            config.setObjectWrapper(new DefaultObjectWrapper());
        } catch (IOException e) {
            throw new WebException(e);
        }
    }

    public Template getTemplate(String templateName) {
        try {
            return config.getTemplate(templateName, Charsets.DEFAULT);
        } catch (IOException e) {
            throw new WebException(e);
        }
    }

    public void merge(Writer out, String templateName, Map<String, Object> params) {
        Template template = getTemplate(templateName);
        try {
            template.process(params, out);
        } catch (TemplateException e) {
            throw new WebException(e);
        } catch (IOException e) {
            throw new WebException(e);
        }
    }

    public void merge(Writer out, String templateName, Object[][] params) {
        merge(out, templateName, CollectionUtils2.arraysToMap(params));
    }

    public String merge(String templateName, Map<String, Object> params) {
        StringWriter out = new StringWriter();
        merge(out, templateName, params);
        return out.toString();
    }

    public String merge(String templateName, Object[][] params) {
        return merge(templateName, CollectionUtils2.arraysToMap(params));
    }
}
