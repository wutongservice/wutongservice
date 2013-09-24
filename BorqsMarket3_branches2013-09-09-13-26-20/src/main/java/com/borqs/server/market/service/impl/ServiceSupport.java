package com.borqs.server.market.service.impl;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.models.LocaleSelector;
import com.borqs.server.market.models.UrlCompleter;
import com.borqs.server.market.utils.mybatis.record.RecordSessionFactory;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class ServiceSupport {
    protected RecordSessionFactory recordSessionFactory;
    protected LocaleSelector localeSelector;
    protected UrlCompleter urlCompleter;

    protected ServiceSupport() {
    }

    public RecordSessionFactory getRecordSessionFactory() {
        return recordSessionFactory;
    }

    @Autowired
    @Qualifier("db.recordSessionFactory")
    public void setRecordSessionFactory(RecordSessionFactory recordSessionFactory) {
        this.recordSessionFactory = recordSessionFactory;
    }

    public LocaleSelector getLocaleSelector() {
        return localeSelector;
    }

    @Autowired
    @Qualifier("helper.localeSelector")
    public void setLocaleSelector(LocaleSelector localeSelector) {
        this.localeSelector = localeSelector;
    }

    public UrlCompleter getUrlCompleter() {
        return urlCompleter;
    }

    @Autowired
    @Qualifier("helper.urlCompleter")
    public void setUrlCompleter(UrlCompleter urlCompleter) {
        this.urlCompleter = urlCompleter;
    }

    protected <T> T openSession(RecordSessionHandler<T> handler)  {
        try {
            return recordSessionFactory.openSession(handler);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw ServiceException.wrap(e);
            }
        }
    }

    protected static void checkAccountId(ServiceContext ctx, boolean mustBeBorqs)  {
        if (!ctx.hasAccountId())
            throw new ServiceException(Errors.E_PERMISSION, "Need signin");

        if (mustBeBorqs && !ctx.isBorqs())
            throw new ServiceException(Errors.E_PERMISSION, "Must be a Borqs ID");
    }
}
