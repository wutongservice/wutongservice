package com.borqs.server.market.utils.mybatis.record;


import com.borqs.server.market.utils.record.Record;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.util.Properties;

public class RecordSessionFactory {
    private final SqlSessionFactoryBean factory = new SqlSessionFactoryBean();

    public RecordSessionFactory() {
        factory.setTypeAliases(new Class[]{Record.class});
    }

    public void setConfigLocation(Resource configLocation) {
        factory.setConfigLocation(configLocation);
    }

    public void setDataSource(DataSource dataSource) {
        factory.setDataSource(dataSource);
    }

    public void setMapperLocations(Resource[] mapperLocations) {
        factory.setMapperLocations(mapperLocations);
    }

    public RecordSession openSession() throws Exception {
        return new RecordSession(factory.getObject().openSession());
    }

    public <T> T openSession(RecordSessionHandler<T> handler) throws Exception {
        RecordSession session = openSession();
        try {
            return handler.handle(session);
        } finally {
            session.close();
        }
    }
}
