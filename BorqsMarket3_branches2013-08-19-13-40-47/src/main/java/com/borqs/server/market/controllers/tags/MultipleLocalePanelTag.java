package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.models.AvailableLocales;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.jsptags.WritableLoopTagSupport;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;


import javax.servlet.jsp.JspTagException;

public class MultipleLocalePanelTag extends WritableLoopTagSupport {
    private String[] locales = AvailableLocales.LOCALES;
    private int currentLocaleIndex = Integer.MAX_VALUE;

    public MultipleLocalePanelTag() {
    }

    public String getLocales() {
        return StringUtils.join(locales, ",");
    }
    public void setLocales(String locales) {
        if (StringUtils.isBlank(locales)) {
            this.locales = new String[0];
        } else {
            this.locales = StringUtils.split(locales, ",");
        }
    }

    @Override
    protected Object next() throws JspTagException {
        return locales[currentLocaleIndex++];
    }

    @Override
    protected boolean hasNext() throws JspTagException {
        return !ArrayUtils.isEmpty(locales) && currentLocaleIndex < locales.length;
    }

    @Override
    protected void prepare() throws JspTagException {
        currentLocaleIndex = ArrayUtils.isNotEmpty(locales) ? 0 : Integer.MAX_VALUE;
    }

    private String getCurrentLocale() {
        return (String) getCurrent();
    }

    @Override
    protected void doWriteStartTag() throws Exception {
        String text = TagFreemarkerConfiguration.render("MultipleLocalePanelStartTag.ftl", CC.map("locales=>", locales, "id=>", id), pageContext);
        pageContext.getOut().write(text);
    }

    @Override
    protected void doWriteEndTag() throws Exception {
        pageContext.getOut().write("</div></div>\n");
    }

    @Override
    protected void doWriteStartStep() throws Exception {
        String locale = getCurrentLocale();
        pageContext.getOut().write(String.format("<div class=\"tab-pane %s\" id=\"%s_%s\">",
                currentLocaleIndex == 1 ? "active" : "", id, locale));
    }

    @Override
    protected void doWriteEndStep() throws Exception {
        pageContext.getOut().write("</div>\n");
    }
}