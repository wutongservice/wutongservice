package com.borqs.server.platform.web.doc;


import com.borqs.server.platform.util.ClassHelper;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.platform.util.VfsHelper;
import com.borqs.server.platform.web.doc.xml.XmlOutput;
import com.borqs.server.platform.web.topaz.Route;
import com.sun.javadoc.*;
import com.sun.tools.javadoc.Main;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

public class HttpApiDoclet {

    public static final String GROUP_TAG = "group";
    public static final String HTTP_RETURN = "http-return";
    public static final String HTTP_EXAMPLE_TAG = "http-example";
    public static final String LOGIN_TAG = "login";
    public static final String HTTP_PARAM_TAG = "http-param";
    public static final String REMARK_TAG = "remark";


    public static void main(String[] args) throws Exception {
        System.out.println("Scan http api");

        ApiDocs apiDocs = scan(args);
        Output output = createOutput(args);
        String outputDir = getOutputDir(args);
        System.out.println("Output class: " + output.getClass().getName());
        System.out.println("Output dir:  " + outputDir);

        output.output(apiDocs, outputDir, getOutputArgs(args));
    }

    private static Output createOutput(String[] args) {
        String outputClass = null;
        for (String arg : args) {
            if (arg.startsWith("-c")) {
                outputClass = StringUtils.removeStart(arg, "-c");
                break;
            }
        }
        if (outputClass == null)
            outputClass = XmlOutput.class.getName();

        return (Output)ClassHelper.newInstance(outputClass);
    }

    private static String getOutputDir(String[] args) {
        String outputDir = null;
        for (String arg : args) {
            if (arg.startsWith("-o")) {
                outputDir = StringUtils.removeStart(arg, "-o");
                break;
            }
        }
        if (outputDir == null)
            outputDir = new File("").getAbsolutePath();
        return outputDir;
    }

    private static String[] getOutputArgs(String[] args) {
        ArrayList<String> l = new ArrayList<String>();
        for (String arg : args) {
            if (arg.startsWith("-a"))
                l.add(StringUtils.removeStart(arg, "-a"));
        }
        return l.toArray(new String[l.size()]);
    }

    private static ApiDocs result = null;

    public static boolean start(RootDoc root) {
        ClassDoc[] classDocs = root.classes();
        if (ArrayUtils.isNotEmpty(classDocs)) {
            for (ClassDoc classDoc : root.classes())
                scanClassDoc(result.docs, classDoc);
        }
        return true;
    }


    public static ApiDocs scan(String... files) {
        try {
            result = new ApiDocs();
            LinkedHashSet<String> sources = new LinkedHashSet<String>();
            for (String file : files)
                findSourceFiles(sources, file);

            Main.execute("borqs_server_api_doc_scanner",
                    HttpApiDoclet.class.getName(),
                    sources.toArray(new String[sources.size()]));
            return result;
        } finally {
            result = null;
        }
    }

    private static void findSourceFiles(LinkedHashSet<String> files, String file) {
        File f = new File(file);
        if (f.isFile()) {
            String ext = FilenameUtils.getExtension(f.getName());
            if ("java".equalsIgnoreCase(ext))
                files.add(file);
        } else if (f.isDirectory()) {
            for (String sub : f.list()) {
                if (sub.equals(".") || sub.equals(".."))
                    continue;

                findSourceFiles(files, FilenameUtils.concat(f.getAbsolutePath(), sub));
            }
        }
    }

    private static void scanClassDoc(List<ApiDoc> apiDocs, ClassDoc classDoc) {
        AnnotationDesc[] annDescs = classDoc.annotations();
        for (AnnotationDesc annDesc : annDescs) {
            if (IgnoreDocument.class.getName().equals(annDesc.annotationType().qualifiedTypeName()))
                return;
        }


        MethodDoc[] methodDocs = classDoc.methods();
        if (ArrayUtils.isNotEmpty(methodDocs)) {
            for (MethodDoc methodDoc : methodDocs) {
                ApiDoc apiDoc = scanMethodDoc(classDoc, methodDoc);
                if (apiDoc != null)
                    apiDocs.add(apiDoc);
            }
        }


        ClassDoc[] innerClassDocs = classDoc.innerClasses();
        if (ArrayUtils.isNotEmpty(innerClassDocs)) {
            for (ClassDoc innerClassDoc : innerClassDocs)
                scanClassDoc(apiDocs, innerClassDoc);
        }
    }

    private static final Pattern REF_PATT = Pattern.compile("\\$\\{(\\w|\\$|\\.)+\\}");
    private static String expandText(String s) {
        return StringHelper.replaceRegex(s, REF_PATT, new StringHelper.ReplaceHandler() {
            @Override
            public String replace(String replaced) {
                String constName = StringHelper.removeStartAndEnd(replaced, "${", "}");
                Object o = ClassHelper.getConstant(constName, null);
                if (o == null)
                    return "";

                Class vt = o.getClass();
                if (vt.isArray()) {
                    StringBuilder buff = new StringBuilder();
                    int len = Array.getLength(o);
                    for (int i = 0; i < len; i++) {
                        Object elem = Array.get(o, i);
                        if (elem == null)
                            continue;

                        if (buff.length() > 0)
                            buff.append(", ");

                        buff.append(elem.toString());
                    }
                    return buff.toString();
                } else {
                    return ObjectUtils.toString(o);
                }
            }
        });
    }

    private static String[] expandTexts(String... ss) {
        String[] r = new String[ss.length];
        for (int i = 0; i < r.length; i++)
            r[i] = expandText(ss[i]);
        return r;
    }

    private static ApiDoc scanMethodDoc(ClassDoc classDoc, MethodDoc methodDoc) {
        String routePrefix = "";

        // RoutePrefix for class
        for (AnnotationDesc annDesc : classDoc.annotations()) {
            if (RoutePrefix.class.getName().equals(annDesc.annotationType().qualifiedTypeName())) {
                routePrefix = annotationToString(annDesc, "value", "");
                break;
            }
        }


        AnnotationDesc[] annDescs = methodDoc.annotations();
        if (ArrayUtils.isEmpty(annDescs))
            return null;


        for (AnnotationDesc annDesc : annDescs) {
            if (IgnoreDocument.class.getName().equals(annDesc.annotationType().qualifiedTypeName()))
                return null;
        }

        // RoutePrefix for method
        for (AnnotationDesc annDesc : annDescs) {
            if (RoutePrefix.class.getName().equals(annDesc.annotationType().qualifiedTypeName())) {
                routePrefix = annotationToString(annDesc, "value", "");
                break;
            }
        }


        Class httpExamplePackageClass = null;
        String[] routes = null;
        String[] httpMethods = {"GET", "POST"};
        boolean deprecated = false;
        for (AnnotationDesc annDesc : annDescs) {
            if (Route.class.getName().equals(annDesc.annotationType().qualifiedTypeName())) {
                for (AnnotationDesc.ElementValuePair annValue : annDesc.elementValues()) {
                    String name = annValue.element().name();
                    if ("url".equals(name)) {
                        routes = annotationToStringArray(annValue.value().value());
                    } else if ("method".equals(name)) {
                        httpMethods = annotationToStringArray(annValue.value().value());
                    }
                }
            }

            if (Deprecated.class.getName().equals(annDesc.annotationType().qualifiedTypeName()))
                deprecated = true;

            if (HttpExamplePackage.class.getName().equals(annDesc.annotationType().qualifiedTypeName())) {
                for (AnnotationDesc.ElementValuePair annValue : annDesc.elementValues()) {
                    String name = annValue.element().name();
                    if ("value".equals(name)) {
                        ClassDoc httpExamplePackageClassAnn = (ClassDoc)annValue.value().value();
                        httpExamplePackageClass = ClassHelper.forName(httpExamplePackageClassAnn.qualifiedTypeName());
                    }
                }
            }
        }
        if (routes == null)
            return null;

        if (httpExamplePackageClass == null) {
            annDescs = classDoc.annotations();
            for (AnnotationDesc annDesc : annDescs) {
                if (HttpExamplePackage.class.getName().equals(annDesc.annotationType().qualifiedTypeName())) {
                    for (AnnotationDesc.ElementValuePair annValue : annDesc.elementValues()) {
                        String name = annValue.element().name();
                        if ("value".equals(name)) {
                            ClassDoc httpExamplePackageClassAnn = (ClassDoc)annValue.value().value();
                            httpExamplePackageClass = ClassHelper.forName(httpExamplePackageClassAnn.qualifiedTypeName());
                        }
                    }
                }
            }
        }

        ApiDoc apiDoc = new ApiDoc();
        apiDoc.group = getTag(methodDoc, GROUP_TAG, "");
        apiDoc.routes = addRoutePrefix(routes, routePrefix);
        apiDoc.httpMethods = httpMethods;
        apiDoc.description = expandText(methodDoc.commentText());
        apiDoc.login = parseBoolean(getTag(methodDoc, LOGIN_TAG, "y"));
        apiDoc.deprecated = deprecated;
        apiDoc.httpReturn = getTag(methodDoc, HTTP_RETURN, "");

        apiDoc.remark = expandText(getTag(methodDoc, REMARK_TAG, ""));
        apiDoc.className = classDoc.qualifiedTypeName();
        apiDoc.httpExamplePackageClass = httpExamplePackageClass;
        String[] httpExamples = expandTexts(getTags(methodDoc, HTTP_EXAMPLE_TAG));
        for (int i = 0; i < httpExamples.length; i++) {
            String httpExample = httpExamples[i];
            if (httpExample.startsWith("@")) {
                if (apiDoc.httpExamplePackageClass != null) {
                    httpExamples[i] = VfsHelper.loadTextInClasspath(apiDoc.httpExamplePackageClass, StringUtils.removeStart(httpExample, "@"));
                } else {
                    httpExamples[i] = "";
                }
            }
        }

        apiDoc.httpExamples = httpExamples;

        ArrayList<ParamDoc> l = new ArrayList<ParamDoc>();
        for (String httpParamTag : getTags(methodDoc, HTTP_PARAM_TAG)) {
            ParamDoc httpParamDoc = parseParamDoc(httpParamTag);
            if (httpParamDoc != null)
                l.add(httpParamDoc);
        }
        apiDoc.httpParams = l.toArray(new ParamDoc[l.size()]);
        return apiDoc;
    }

    private static String[] addRoutePrefix(String[] routes, String routePrefix) {
        if (ArrayUtils.isEmpty(routes))
            return routes;

        if (StringUtils.isEmpty(routePrefix))
            return routes;

        String[] r = new String[routes.length];
        for (int i = 0; i < r.length; i++)
            r[i] = routePrefix + routes[i];
        return r;
    }

    private static String annotationToString(AnnotationDesc annDesc, String annName, String def) {
        for (AnnotationDesc.ElementValuePair e : annDesc.elementValues()) {
            if (StringUtils.equals(e.element().name(), annName))
                return annotationToString(e.value().value());
        }
        return def;
    }

    private static String annotationToString(Object annValue) {
        if (annValue instanceof String)
            return (String)annValue;
        else
            throw new IllegalArgumentException();
    }

    private static String[] annotationToStringArray(Object annValue) {
        if (annValue instanceof String) {
            return new String[]{(String) annValue};
        } else if (annValue instanceof AnnotationValue[]) {
            AnnotationValue[] annValues = (AnnotationValue[]) annValue;
            String[] r = new String[annValues.length];
            for (int i = 0; i < r.length; i++)
                r[i] = (String) annValues[i].value();
            return r;
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static String getTag(MethodDoc methodDoc, String tagName, String def) {
        Tag[] tags = methodDoc.tags(tagName);
        return ArrayUtils.isNotEmpty(tags) ? tags[0].text() : def;
    }

    private static String[] getTags(MethodDoc methodDoc, String tagName) {
        Tag[] tags = methodDoc.tags(tagName);
        if (ArrayUtils.isNotEmpty(tags)) {
            String[] a = new String[tags.length];
            for (int i = 0; i < a.length; i++)
                a[i] = tags[i].text();

            return a;
        } else {
            return new String[0];
        }
    }

    public static boolean parseBoolean(String s) {
        s = StringUtils.strip(s);
        return "y".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "true".equalsIgnoreCase(s) || "1".equalsIgnoreCase(s);
    }

    public static String booleanToString(boolean b) {
        return b ? "Y" : "N";
    }



    private static ParamDoc parseParamDoc(String s) {
        String param = StringUtils.substringBefore(s, " ").trim();
        ParamDoc paramDoc = new ParamDoc();
        paramDoc.description = expandText(StringUtils.substringAfter(s, " "));
        paramDoc.names = StringHelper.splitArray(StringUtils.substringBefore(param, ":"), "|", true);
        boolean hasDefault = StringUtils.contains(param, ":");
        paramDoc.must = !hasDefault;
        paramDoc.def = expandText(hasDefault ? StringUtils.substringAfter(param, ":") : "");
        return paramDoc;
    }


    public static class DocSupport {
        public static String getDisplayGroupName(String group) {
            return StringUtils.isNotEmpty(group) ? group : "Ungrouped";
        }
    }

    public static class ApiDoc extends DocSupport {
        public String className;
        public Class httpExamplePackageClass;
        public String group;
        public String[] routes;
        public String[] httpMethods;
        public String description;
        public boolean login;
        public boolean deprecated;
        public String httpReturn;
        public String[] httpExamples;
        public ParamDoc[] httpParams;
        public String remark;

        public String getRouteHtmlFile() {
            String route = routes[0];
            String s = StringUtils.replace(route, "/", "_S_");
            s = StringUtils.replace(s, ":", "");
            return s + ".html";
        }

        public String getRoute() {
            return routes[0];
        }

        public String[] getDisplayTitles() {
            String[] r = new String[routes.length];
            for (int i = 0; i < r.length; i++)
                r[i] = StringUtils.join(httpMethods, "|") + "    " + routes[i];
            return r;
        }

        public String getDisplayDescription() {
            return StringUtils.isNotEmpty(description) ? description : "No description";
        }

        public String getDisplayGroupName() {
            return getDisplayGroupName(group);
        }

        public String getDisplayNeedLogin() {
            return booleanToString(login);
        }

        public String getDisplayDeprecated() {
            return booleanToString(deprecated);
        }

        public String getDisplayHttpReturn() {
            return ObjectUtils.toString(httpReturn);
        }

        public String[] getDisplayHttpExamples() {
            ArrayList<String> l = new ArrayList<String>();
            if (ArrayUtils.isNotEmpty(httpExamples)) {
                for (String example : httpExamples) {
                    if (StringUtils.isNotBlank(example))
                        l.add(example);
                }
            }
            return l.toArray(new String[l.size()]);
        }

        public String getDisplayRemark() {
            return ObjectUtils.toString(remark);
        }

        public boolean hasHttpReturnSections() {
            return StringUtils.isNotEmpty(httpReturn) || ArrayUtils.isNotEmpty(httpExamples);
        }

        public boolean hasHttpParams() {
            return ArrayUtils.isNotEmpty(httpParams);
        }

        public ParamDoc[] getHttpParams() {
            return httpParams != null ? httpParams : new ParamDoc[0];
        }
    }

    public static class ParamDoc {
        public String[] names;
        public String description;
        public boolean must;
        public String def;

        public String getDisplayNames() {
            return ArrayUtils.isNotEmpty(names) ? StringUtils.join(names, ", ") : "";
        }

        public String getDisplayDescription() {
            return StringUtils.isNotEmpty(description) ? description : "No description";
        }

        public String getDisplayMust() {
            return booleanToString(must);
        }

        public String getDisplayDefault() {
            return ObjectUtils.toString(def);
        }
    }


    public static class ApiDocs extends DocSupport {
        public final List<ApiDoc> docs = new ArrayList<ApiDoc>();

        public ApiDocs() {
        }

        public ApiDocs(Collection<? extends ApiDoc> c) {
            docs.addAll(c);
        }

        public String[] getGroupNames() {
            LinkedHashSet<String> groups = new LinkedHashSet<String>();
            for (ApiDoc apiDoc : docs) {
                groups.add(apiDoc.group);
            }
            return groups.toArray(new String[groups.size()]);
        }

        public Map<String, String[]> getGroups() {
            LinkedHashMap<String, List<String>> m = new LinkedHashMap<String, List<String>>();
            for (ApiDoc apiDoc : docs) {
                List<String> l = m.get(apiDoc.group);
                if (l == null) {
                    l = new ArrayList<String>();
                    m.put(apiDoc.group, l);
                }
                Collections.addAll(l, apiDoc.routes);
            }
            return CollectionsHelper.listMapToArrayMap(m, String.class);
        }

        public String[] getGroupRoutes(String group) {
            LinkedHashSet<String> routes = new LinkedHashSet<String>();
            for (ApiDoc apiDoc : docs) {
                if (StringUtils.equals(group, apiDoc.group))
                    Collections.addAll(routes, apiDoc.routes);
            }
            return routes.toArray(new String[routes.size()]);
        }

        public ApiDoc getDocByRoute(String route) {
            for (ApiDoc doc : docs) {
                if (ArrayUtils.contains(doc.routes, route))
                    return doc;
            }
            return null;
        }
    }

    public static interface Output {
        void output(ApiDocs docs, String outputDir, String[] args) throws Exception;
    }
}
