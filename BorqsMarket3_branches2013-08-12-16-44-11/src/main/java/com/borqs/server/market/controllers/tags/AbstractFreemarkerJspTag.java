package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.resources.tags._TagPackageClass_;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.*;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

public abstract class AbstractFreemarkerJspTag extends SimpleTagSupport {


    private String template;

    protected AbstractFreemarkerJspTag() {
    }

    protected AbstractFreemarkerJspTag(String template) {
        this.template = template;
    }

    protected String getTemplate() {
        return template;
    }

    protected void setTemplate(String template) {
        this.template = template;
    }

    protected abstract Map<String, Object> getData();

    @Override
    public void doTag() throws JspException, IOException {
        try {
            Map<String, Object> data = getData();
            String text = TagFreemarkerConfiguration.render(template, data, (PageContext) getJspContext());
            JspWriter out = getJspContext().getOut();
            out.print(text);
        } catch (TemplateException e) {
            throw new JspException("Merge freemarker tag error", e);
        }
    }
}
