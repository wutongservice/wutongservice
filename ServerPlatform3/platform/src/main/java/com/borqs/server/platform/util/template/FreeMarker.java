package com.borqs.server.platform.util.template;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.util.ClassHelper;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.I18nHelper;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FreeMarker {
    private static final String FILE_PREFIX = "file://";
    private static final String RES_PREFIX = "res://";
    private final Configuration config;
    private static final Map<String, Template> rawCache = new ConcurrentHashMap<String, Template>();

    public FreeMarker(File file) {
        this(FILE_PREFIX + file.getPath());
    }

    public FreeMarker(Class clazz) {
        this(RES_PREFIX + clazz.getName());
    }

    public FreeMarker(String... dirs) {
        try {
            ArrayList<TemplateLoader> loaders = new ArrayList<TemplateLoader>();
            for (String dir : dirs) {
                if (dir.startsWith(RES_PREFIX)) {
                    String className = StringUtils.removeStart(dir, RES_PREFIX);
                    loaders.add(new ClassTemplateLoader(ClassHelper.forName(className), ""));
                } else if (dir.startsWith(FILE_PREFIX)) {
                    String dirName = StringUtils.removeStart(dir, FILE_PREFIX);
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
            throw new ServerException(E.TEMPLATE, e);
        }
    }

    public Template getTemplate(String templateName, Locale locale) {
        if (locale == null)
            locale = I18nHelper.DEFAULT_LOCALE;
        try {
            return config.getTemplate(templateName, locale, Charsets.DEFAULT);
        } catch (IOException e) {
            throw new ServerException(E.TEMPLATE, e);
        }
    }

    public Template getTemplate(String templateName, String locale) {
        return getTemplate(templateName, I18nHelper.parseLocale(locale));
    }

    public Template getTemplate(String templateName) {
        try {
            return config.getTemplate(templateName, Charsets.DEFAULT);
        } catch (IOException e) {
            throw new ServerException(E.TEMPLATE, e);
        }
    }

    private void merge0(Template template, Writer out, Map<String, Object> params) {
        try {
            template.process(params, out);
        } catch (TemplateException e) {
            throw new ServerException(E.TEMPLATE, e);
        } catch (IOException e) {
            throw new ServerException(E.TEMPLATE, e);
        }
    }

    public void merge(Writer out, String templateName, Map<String, Object> params) {
        Template template = getTemplate(templateName);
        merge0(template, out, params);
    }

    public void merge(Writer out, String templateName, Locale locale, Map<String, Object> params) {
        Template template = getTemplate(templateName, locale);
        merge0(template, out, params);
    }

    public void merge(Writer out, String templateName, String locale, Map<String, Object> params) {
        Template template = getTemplate(templateName, locale);
        merge0(template, out, params);
    }

    public void merge(Writer out, String templateName, Object[][] params) {
        merge(out, templateName, CollectionsHelper.arraysToMap(params));
    }

    public String merge(String templateName, Map<String, Object> params) {
        StringWriter out = new StringWriter();
        merge(out, templateName, params);
        return out.toString();
    }

    public String merge(String templateName, Locale locale, Map<String, Object> params) {
        StringWriter out = new StringWriter();
        merge(out, templateName, locale, params);
        return out.toString();
    }

    public String merge(String templateName, String locale, Map<String, Object> params) {
        StringWriter out = new StringWriter();
        merge(out, templateName, locale, params);
        return out.toString();
    }

    public String merge(String templateName, Object[][] params) {
        return merge(templateName, CollectionsHelper.arraysToMap(params));
    }

    public String merge(String templateName, Locale locale, Object[][] params) {
        return merge(templateName, locale, CollectionsHelper.arraysToMap(params));
    }

    public String merge(String templateName, String locale, Object[][] params) {
        return merge(templateName, locale, CollectionsHelper.arraysToMap(params));
    }

    public Template compileTemplate(String s) {
        Validate.notNull(s);
        try {
            return new Template("", new StringReader(s), config, Charsets.DEFAULT);
        } catch (IOException e) {
            throw new ServerException(E.TEMPLATE, e);
        }
    }

    public void mergeRaw(Writer out, String template, Map<String, Object> params) {
        if (StringUtils.isEmpty(template))
            return;

        Template temp = rawCache.get(template);
        if (temp == null) {
            temp = compileTemplate(template);
            rawCache.put(template, temp);
        }
        try {
            temp.process(params, out);
        } catch (TemplateException e) {
            throw new ServerException(E.TEMPLATE, e);
        } catch (IOException e) {
            throw new ServerException(E.TEMPLATE, e);
        }
    }

    public void mergeRaw(Writer out, String template, Object[][] params) {
        mergeRaw(out, template, CollectionsHelper.arraysToMap(params));
    }

    public String mergeRaw(String template, Map<String, Object> params) {
        StringWriter out = new StringWriter();
        mergeRaw(out, template, params);
        return out.toString();
    }

    public String mergeRaw(String template, Object[][] params) {
        return mergeRaw(template, CollectionsHelper.arraysToMap(params));
    }
}
