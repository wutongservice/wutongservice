package com.borqs.server.market.services.impl;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.models.FieldTrimmer;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.PrimitiveTypeConverter;
import com.borqs.server.market.utils.mybatis.SqlSessionHandler;
import com.borqs.server.market.utils.mybatis.SqlSessionUtils2;
import org.apache.commons.collections.IteratorUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static com.borqs.server.market.Errors.*;

public abstract class ServiceSupport {

    protected SqlSessionFactory sqlSessionFactory;
    protected FieldTrimmer fieldTrimmer;

    protected ServiceSupport() {
    }

    public FieldTrimmer getFieldTrimmer() {
        return fieldTrimmer;
    }

    @Autowired
    public void setFieldTrimmer(FieldTrimmer fieldTrimmer) {
        this.fieldTrimmer = fieldTrimmer;
    }

    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    @Autowired
    @Qualifier("db.sqlSessionFactory")
    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    protected <T> T openSession(SqlSessionHandler<T> handler) throws ServiceException {
        try {
            return SqlSessionUtils2.openSession(sqlSessionFactory, handler);
        } catch (Exception e) {
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                throw new ServiceException(Errors.E_PERSISTENCE, "Query data error");
            }
        }
    }

    protected String checkAccount(ServiceContext ctx, int role) throws ServiceException {
        if (!ctx.hasClientId())
            throw new ServiceException(E_PERMISSION, "Illegal permission (id error)");

        if (ctx.getClientRole() != role)
            throw new ServiceException(E_PERMISSION, "Illegal permission (role error)");

        return ctx.getClientId();
    }

    protected boolean languageExists(ServiceContext ctx, SqlSession session, String lang) {
        Object b = SqlSessionUtils2.selectValue(session, "market.commons_languageExists_1", CC.map("lang=>", lang));
        return PrimitiveTypeConverter.toBoolean(b);
    }

    protected void checkLanguage(ServiceContext ctx, SqlSession session, String lang) throws ServiceException {
        if (lang == null)
            return;
        if (!languageExists(ctx, session, lang))
            throw new ServiceException(E_ILLEGAL_LANGUAGE, "Illegal language " + lang);
    }

    protected void checkMultipleLanguageValues(ServiceContext ctx, SqlSession session, JsonNode langs) throws ServiceException {
        if (langs == null)
            return;

        String[] langNames = (String[]) IteratorUtils.toArray(langs.getFieldNames(), String.class);
        for (String lang : langNames) {
            if (!languageExists(ctx, session, lang))
                throw new ServiceException(E_ILLEGAL_LANGUAGE, "Illegal language in some field (" + lang + ")");
            if (!langs.path(lang).isTextual())
                throw new ServiceException(E_ILLEGAL_LANGUAGE, "Illegal language value");
        }
    }

    protected static void checkImageValue(JsonNode jn) {
        // TODO: xx
    }
}
