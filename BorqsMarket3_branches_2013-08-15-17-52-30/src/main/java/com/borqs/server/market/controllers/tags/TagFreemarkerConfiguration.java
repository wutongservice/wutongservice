package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.resources.tags._TagPackageClass_;
import com.borqs.server.market.utils.i18n.SpringMessage;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TagFreemarkerConfiguration {
    private static final Configuration freemarkerConfig = createConfig();

    private static Configuration createConfig() {
        Configuration config = new Configuration();
        config.setTemplateLoader(new ClassTemplateLoader(_TagPackageClass_.class, ""));
        return config;
    }

    public static Configuration getFreemarkerConfiguration() {
        return freemarkerConfig;
    }

    public static Template getTemplate(String template) throws IOException {
        return freemarkerConfig.getTemplate(template);
    }

    public static void render(Writer out, String template, Map<String, Object> data, PageContext pageContext) throws IOException, TemplateException {
        HashMap<String, Object> data1 = data != null ? new HashMap<String, Object>(data) : new HashMap<String, Object>();
        data1.put("spring", new SpringMethods(pageContext));
        Template t = getTemplate(template);
        t.process(data1, out);
    }

    public static String render(String template, Map<String, Object> data, PageContext pageContext) throws IOException, TemplateException {
        StringWriter w = new StringWriter();
        render(w, template, data, pageContext);
        return w.toString();
    }

    public static class SpringMethods {
        final PageContext pageContext;

        public SpringMethods(PageContext pageContext) {
            this.pageContext = pageContext;
        }

        public String message(String code) {
            return SpringMessage.get(code, pageContext);
        }
    }
}
