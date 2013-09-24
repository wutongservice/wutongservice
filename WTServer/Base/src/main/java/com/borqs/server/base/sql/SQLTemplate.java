package com.borqs.server.base.sql;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.io.Charsets;
import com.borqs.server.base.util.CollectionUtils2;
import freemarker.template.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SQLTemplate {
    private static final Configuration CONF = initConf();
    private static Map<String, SQLTemplate> cache = new ConcurrentHashMap<String, SQLTemplate>();
    private Template template;

    private static Configuration initConf() {
        try {
            Configuration conf = new Configuration();
            conf.setObjectWrapper(new DefaultObjectWrapper());
            HashMap<String, Object> methods = new HashMap<String, Object>();
            methods.put("v", new SQLValueMethod());
            methods.put("join", new JoinMethod(false));
            methods.put("vjoin", new JoinMethod(true));
            methods.put("as_join", new AsJoinMethod());
            methods.put("pair_join", new PairJoinMethod());
            methods.put("values_join", new ValuesJoinMethod());

            conf.setAllSharedVariables(new SimpleHash(methods));
            return conf;
        } catch (TemplateModelException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_TEMPLATE_ERROR, e);
        }
    }

    private SQLTemplate() {
    }

    public String merge(Map<String, Object> data) {
        try {
            StringWriter w = new StringWriter();
            template.process(data, w);
            return w.toString();
        } catch (TemplateException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_TEMPLATE_ERROR, e);
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_TEMPLATE_ERROR, e);
        }
    }

    public static String merge(String s, Map<String, Object> data) {
        //System.out.println("-------" + s);
        SQLTemplate st = get(s);
        return st.merge(data);
    }

    public static String merge(String s, String k1, Object v1) {
        return merge(s, CollectionUtils2.<String, Object>of(k1, v1));
    }

    public static String merge(String s, String k1, Object v1, String k2, Object v2) {
        return merge(s, CollectionUtils2.<String, Object>of(k1, v1, k2, v2));
    }

    public static String merge(String s, String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        return merge(s, CollectionUtils2.<String, Object>of(k1, v1, k2, v2, k3, v3));
    }

    public static String merge(String s, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
        return merge(s, CollectionUtils2.<String, Object>of(k1, v1, k2, v2, k3, v3, k4, v4));
    }

    public static String merge(String s, Object[][] args) {
        return merge(s, CollectionUtils2.arraysToMap(args));
    }

    public static SQLTemplate compile(String s) {
        Validate.notNull(s);
        try {
            SQLTemplate st = new SQLTemplate();
            st.template = new Template("", new StringReader(s), CONF, Charsets.DEFAULT);
            return st;
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_TEMPLATE_ERROR, e);
        }
    }

    public static SQLTemplate get(String s) {
        Validate.notNull(s);
        SQLTemplate st = cache.get(s);
        if (st == null) {
            st = compile(s);
            cache.put(s, st);
        }
        return st;
    }

    private static String str(TemplateModel o) {
        try {
            if (o == null) {
                return "";
            } if (o instanceof TemplateBooleanModel) {
                return Boolean.toString(((TemplateBooleanModel) o).getAsBoolean());
            } else if (o instanceof TemplateNumberModel) {
                return ((TemplateNumberModel) o).getAsNumber().toString();
            } else if (o instanceof TemplateScalarModel) {
                return ((TemplateScalarModel) o).getAsString();
            } else {
                throw new TemplateModelException("Object type error");
            }
        } catch (TemplateModelException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_TEMPLATE_ERROR, e);
        }
    }

    private static String vstr(TemplateModel o) {
        try {
            if (o == null) {
                return "";
            } if (o instanceof TemplateBooleanModel) {
                return Boolean.toString(((TemplateBooleanModel) o).getAsBoolean());
            } else if (o instanceof TemplateNumberModel) {
                return ((TemplateNumberModel) o).getAsNumber().toString();
            } else if (o instanceof TemplateScalarModel) {
                return SQLUtils.toSql(((TemplateScalarModel) o).getAsString());
            } else {
                throw new TemplateModelException("Object type error");
            }
        } catch (TemplateModelException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_TEMPLATE_ERROR, e);
        }
    }

    private static List<TemplateModel> readList(TemplateModel o) {
        try {
            ArrayList<TemplateModel> l = new ArrayList<TemplateModel>();
            if (o instanceof TemplateSequenceModel) {
                TemplateSequenceModel seq = (TemplateSequenceModel) o;
                for (int i = 0; i < seq.size(); i++)
                    l.add(seq.get(i));
            } else if (o instanceof TemplateCollectionModel) {
                TemplateCollectionModel coll = (TemplateCollectionModel) o;
                TemplateModelIterator iter = coll.iterator();
                while (iter.hasNext())
                    l.add(iter.next());
            } else if (o instanceof TemplateBooleanModel) {
                l.add(o);
            } else if (o instanceof TemplateNumberModel) {
                l.add(o);
            } else if (o instanceof TemplateScalarModel) {
                l.add(o);
            }
            return l;
        } catch (TemplateModelException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_TEMPLATE_ERROR, e);
        }
    }


    private static class SQLValueMethod implements TemplateMethodModelEx {
        @Override
        public Object exec(List args) throws TemplateModelException {
            return args.isEmpty() ? "" : vstr((TemplateModel) args.get(0));
        }
    }


    private static class JoinMethod implements TemplateMethodModelEx {
        private final boolean valueJoin;

        public JoinMethod(boolean valueJoin) {
            this.valueJoin = valueJoin;
        }

        @Override
        public Object exec(List args) throws TemplateModelException {
            if (args.isEmpty())
                return "";

            TemplateModel a1 = (TemplateModel) args.get(0);
            String sep = args.size() >= 2 ? ((TemplateScalarModel) args.get(1)).getAsString() : ", ";

            ArrayList<String> l = new ArrayList<String>();
            for (TemplateModel v : readList(a1)) {
                String s = valueJoin ? vstr(v) : str(v);
                l.add(s);
            }
            return StringUtils.join(l, sep);
        }
    }

    private static class AsJoinMethod implements TemplateMethodModelEx {
        @Override
        public Object exec(List args) throws TemplateModelException {
            if (args.isEmpty())
                return "";

            TemplateHashModelEx a1 = (TemplateHashModelEx) args.get(0);
            List<TemplateModel> cols = args.size() >= 2 ? readList((TemplateModel)args.get(1)) : null;
            if (cols == null)
                cols = readList(a1.keys());

            ArrayList<String> l = new ArrayList<String>();
            for (TemplateModel col : cols) {
                String cv = vstr(col);
                String c = str(col);
                String f = str(a1.get(c));
                l.add(f + " AS " + cv);
            }
            return StringUtils.join(l, ", ");
        }
    }

    private static class PairJoinMethod implements TemplateMethodModelEx {
        @Override
        public Object exec(List args) throws TemplateModelException {
            if (args.isEmpty())
                return "";

            TemplateHashModelEx a1 = (TemplateHashModelEx) args.get(0);
            List<TemplateModel> keys = readList(a1.keys());
            List<TemplateModel> vals = readList(a1.values());

            ArrayList<String> l = new ArrayList<String>();
            for (int i = 0; i < keys.size(); i++) {
                TemplateModel key = keys.get(i);
                TemplateModel val = vals.get(i);
                l.add(str(key) + "=" + vstr(val));
            }
            return StringUtils.join(l, ", ");
        }
    }

    private static class ValuesJoinMethod implements TemplateMethodModelEx {
        @Override
        public Object exec(List args) throws TemplateModelException {
            if (args.isEmpty())
                return "";

            TemplateHashModelEx aliases = (TemplateHashModelEx) args.get(0);
            TemplateHashModelEx rec = (TemplateHashModelEx) args.get(1);
            TemplateHashModelEx add = args.size() >= 3 ? (TemplateHashModelEx)args.get(2) : null;

            List<TemplateModel> recKeys = readList(rec.keys());
            List<TemplateModel> recVals = readList(rec.values());

            ArrayList<String> fl = new ArrayList<String>();
            ArrayList<String> vl = new ArrayList<String>();
            for (int i = 0; i < rec.size(); i++) {
                TemplateModel key = recKeys.get(i);
                TemplateModel val = recVals.get(i);
                String key1 = str(aliases.get(str(key)));
                fl.add(key1);
                vl.add(vstr(val));
            }

            if (add != null) {
                recKeys = readList(add.keys());
                recVals = readList(add.values());
                for (int i = 0; i < add.size(); i++) {
                    fl.add(str(recKeys.get(i)));
                    vl.add(vstr(recVals.get(i)));
                }
            }

            return String.format("(%s) VALUES (%s)", StringUtils.join(fl, ","), StringUtils.join(vl, ","));
        }
    }
}
