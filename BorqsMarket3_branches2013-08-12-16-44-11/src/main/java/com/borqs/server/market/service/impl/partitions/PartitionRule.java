package com.borqs.server.market.service.impl.partitions;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.Paging;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.JsonNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public abstract class PartitionRule {

    private static final Map<String, PartitionRule> rules = new ConcurrentHashMap<String, PartitionRule>();

    protected PartitionRule() {
    }

    public static Map<String, PartitionRule> getRules() {
        return rules;
    }

    public abstract String getId();

    public abstract JsonNode getName();

    public abstract RecordsWithTotal getProductIds(ServiceContext ctx, Params options);

    public static void registerRule(PartitionRule rule) {
        if (rule != null)
            rules.put(rule.getId(), rule);
    }

    public static void unregisterRule(String id) {
        rules.remove(id);
    }

    public static PartitionRule getRule(String id) {
        return rules.get(id);
    }

    public static class Setup {
        public Setup() {
        }

        public void setRuleClasses(String[] classnames) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
            if (ArrayUtils.isNotEmpty(classnames)) {
                for (String className : classnames) {
                    PartitionRule rule = (PartitionRule) Class.forName(className).newInstance();
                    registerRule(rule);
                }
            }
        }
    }
}
