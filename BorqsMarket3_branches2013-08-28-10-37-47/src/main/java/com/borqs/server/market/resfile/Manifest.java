package com.borqs.server.market.resfile;

import com.borqs.server.market.models.VersionedProductId;
import com.borqs.server.market.utils.JsonUtils;
import com.borqs.server.market.utils.StringUtils2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.borqs.server.market.utils.Dom4jUtils.*;


public class Manifest {
    private VersionedProductId versionId = new VersionedProductId();
    private String defaultLanguage;
    private JsonNode name;
    private JsonNode versionName;
    private JsonNode recentChange;
    private JsonNode description;
    private String appId;
    private String category;
    private int minAppVersion;
    private int maxAppVersion;
    private String[] supportedMod;
    private String logoPath;
    private String coverPath;
    private String screenshot1Path;
    private String screenshot2Path;
    private String screenshot3Path;
    private String screenshot4Path;
    private String screenshot5Path;
    private List<VersionedProductId> dependencies;
    private String authorName;
    private String authorPhone;
    private String authorEmail;
    private String authorWebsite;
    private String tags;

    public Manifest() {
    }

    public VersionedProductId getVersionId() {
        return versionId;
    }

    public String getId() {
        return versionId.getId();
    }

    public void setId(String id) {
        versionId.setId(id);
    }

    public int getVersion() {
        return versionId.getVersion();
    }

    public void setVersion(int version) {
        versionId.setVersion(version);
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public JsonNode getName() {
        return name;
    }

    public void setName(JsonNode name) {
        this.name = name;
    }

    public JsonNode getVersionName() {
        return versionName;
    }

    public void setVersionName(JsonNode versionName) {
        this.versionName = versionName;
    }

    public JsonNode getRecentChange() {
        return recentChange;
    }

    public void setRecentChange(JsonNode recentChange) {
        this.recentChange = recentChange;
    }

    public JsonNode getDescription() {
        return description;
    }

    public void setDescription(JsonNode description) {
        this.description = description;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getMinAppVersion() {
        return minAppVersion;
    }

    public void setMinAppVersion(int minAppVersion) {
        this.minAppVersion = minAppVersion;
    }

    public int getMaxAppVersion() {
        return maxAppVersion;
    }

    public void setMaxAppVersion(int maxAppVersion) {
        this.maxAppVersion = maxAppVersion;
    }

    public String[] getSupportedMod() {
        return supportedMod;
    }

    public void setSupportedMod(String[] supportedMod) {
        this.supportedMod = supportedMod;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    public String getScreenshot1Path() {
        return screenshot1Path;
    }

    public void setScreenshot1Path(String screenshot1Path) {
        this.screenshot1Path = screenshot1Path;
    }

    public String getScreenshot2Path() {
        return screenshot2Path;
    }

    public void setScreenshot2Path(String screenshot2Path) {
        this.screenshot2Path = screenshot2Path;
    }

    public String getScreenshot3Path() {
        return screenshot3Path;
    }

    public void setScreenshot3Path(String screenshot3Path) {
        this.screenshot3Path = screenshot3Path;
    }

    public String getScreenshot4Path() {
        return screenshot4Path;
    }

    public void setScreenshot4Path(String screenshot4Path) {
        this.screenshot4Path = screenshot4Path;
    }

    public String getScreenshot5Path() {
        return screenshot5Path;
    }

    public void setScreenshot5Path(String screenshot5Path) {
        this.screenshot5Path = screenshot5Path;
    }

    public List<VersionedProductId> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<VersionedProductId> dependencies) {
        this.dependencies = dependencies;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorPhone() {
        return authorPhone;
    }

    public void setAuthorPhone(String authorPhone) {
        this.authorPhone = authorPhone;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getAuthorWebsite() {
        return authorWebsite;
    }

    public void setAuthorWebsite(String authorWebsite) {
        this.authorWebsite = authorWebsite;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    private String getSingleText(JsonNode mlTextNode, String def) {
        String defLang = getDefaultLanguage();
        if (mlTextNode == null)
            return def;
        if (mlTextNode.isTextual())
            return mlTextNode.asText();
        if (!mlTextNode.isObject())
            return def;

        if (mlTextNode.has(defLang)) {
            String t = mlTextNode.get(defLang).asText();
            if (t == null) {
                t = def;
            }
            return t;
        } else {
            if (mlTextNode.size() == 0) {
                return def;
            } else {
                String firstField = mlTextNode.getFieldNames().next();
                String t = mlTextNode.get(firstField).asText();
                if (t == null) {
                    t = def;
                }
                return def;
            }
        }
    }

    public String getDefaultName(String def) {
        return getSingleText(getName(), def);
    }

    public String getDefaultDescription(String def) {
        return getSingleText(getDescription(), def);
    }

    public static Manifest loadFile(String path) throws IOException, DocumentException {
        FileInputStream in = new FileInputStream(path);
        try {
            return load(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static Manifest load(InputStream in) throws IOException, DocumentException {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(in);
        Manifest manifest = new Manifest();
        readManifest(doc.getRootElement(), manifest);
        return manifest;
    }

    private static void readManifest(Element root, Manifest manifest) {
        String id = selectTextValue(root, "id", null, true);
        int version = selectIntValue(root, "version", 0, "Illegal version in manifest");
        String defaultLanguage = attributeValue(root, "defaultLang", "en_US");
        manifest.setId(id);
        manifest.setVersion(version);
        manifest.setDefaultLanguage(defaultLanguage);
        manifest.setName(readLangValue(root.element("name"), defaultLanguage));
        manifest.setVersionName(readLangValue(root.element("versionName"), defaultLanguage));
        manifest.setRecentChange(readLangValue(root.element("recentChange"), defaultLanguage));
        manifest.setDescription(readLangValue(root.element("description"), defaultLanguage));
        manifest.setAppId(selectTextValue(root, "app", null, true));
        manifest.setCategory(selectTextValue(root, "category", null, true));
        manifest.setMinAppVersion(selectIntValue(root, "minAppVC", 0, "Illegal minAppVC in manifest"));
        manifest.setMaxAppVersion(selectIntValue(root, "maxAppVC", Integer.MAX_VALUE, "Illegal maxAppVC in manifest"));
        manifest.setSupportedMod(StringUtils2.splitArray(selectTextValue(root, "supportedMod", "", true), ',', true));
        manifest.setLogoPath(selectTextValue(root, "logo", null, true));
        manifest.setCoverPath(selectTextValue(root, "cover", null, true));
        manifest.setScreenshot1Path(selectTextValue(root, "screenshot1", null, true));
        manifest.setScreenshot2Path(selectTextValue(root, "screenshot2", null, true));
        manifest.setScreenshot3Path(selectTextValue(root, "screenshot3", null, true));
        manifest.setScreenshot4Path(selectTextValue(root, "screenshot4", null, true));
        manifest.setScreenshot5Path(selectTextValue(root, "screenshot5", null, true));
        manifest.setDependencies(readDependencies(root.element("dependencies")));
        manifest.setAuthorName(selectTextValue(root, "authorName", null, true));
        manifest.setAuthorEmail(selectTextValue(root, "authorEmail", null, true));
        manifest.setAuthorPhone(selectTextValue(root, "authorPhone", null, true));
        manifest.setAuthorWebsite(selectTextValue(root, "authorWebsite", null, true));
        manifest.setTags(selectTextValue(root, "tags", null, true));
    }

    @SuppressWarnings("unchecked")
    private static JsonNode readLangValue(Element elem, String defaultLanguage) {
        ObjectNode langs = JsonNodeFactory.instance.objectNode();
        if (elem != null) {
            langs.put(defaultLanguage, elem.getText());
            Element langsElem = elem.element("langs");
            if (langsElem != null) {
                for (Element langElem : (List<Element>) langsElem.elements())
                    langs.put(langElem.getName(), langElem.getText());
            }
        } else {
            langs.put(defaultLanguage, "");
        }
        return langs;
    }

    @SuppressWarnings("unchecked")
    private static List<VersionedProductId> readDependencies(Element elem) {
        LinkedHashSet<VersionedProductId> l = new LinkedHashSet<VersionedProductId>();
        if (elem != null) {
            for (Element depElem : (List<Element>) elem.elements("dependency")) {
                String id = selectTextValue(depElem, "id", null, true);
                int version = selectIntValue(depElem, "version", 0, "Illegal version in manifest:dependencies");
                l.add(new VersionedProductId(id, version));
            }
        }
        return new ArrayList<VersionedProductId>(l);
    }

    @Override
    public String toString() {
        LinkedHashMap<String, Object> all = new LinkedHashMap<String, Object>();
        all.put("id", getId());
        all.put("version", getVersion());
        all.put("defaultLanguage", getDefaultLanguage());
        all.put("name", getName());
        all.put("versionName", getVersionName());
        all.put("recentChange", getRecentChange());
        all.put("description", getDescription());
        all.put("appId", getAppId());
        all.put("category", getCategory());
        all.put("minAppVersion", getMinAppVersion());
        all.put("maxAppVersion", getMaxAppVersion());
        all.put("supportedMod", getSupportedMod());
        all.put("logoPath", getLogoPath());
        all.put("coverPath", getCoverPath());
        all.put("screenshot1Path", getScreenshot1Path());
        all.put("screenshot2Path", getScreenshot2Path());
        all.put("screenshot3Path", getScreenshot3Path());
        all.put("screenshot4Path", getScreenshot4Path());
        all.put("screenshot5Path", getScreenshot5Path());
        all.put("dependencies", getDependencies());
        all.put("authorName", getAuthorName());
        all.put("authorEmail", getAuthorEmail());
        all.put("authorPhone", getAuthorPhone());
        all.put("authorWebsite", getAuthorWebsite());
        all.put("tags", getTags());
        return JsonUtils.toJson(all, true);
    }

    private void writeText(StringBuilder buff, String elemName, Object text, boolean required) {
        String s = ObjectUtils.toString(text);
        boolean b = required || !s.isEmpty();
        if (b) {
            buff.append("<").append(elemName).append(">")
                    .append(StringEscapeUtils.escapeXml(s))
                    .append("</").append(elemName).append(">\n");
        }
    }

    private void writeMultipleLangText(StringBuilder buff, String elemName, JsonNode jn, boolean required) {
        boolean b = required || (jn != null && jn.size() > 0);
        if (b) {
            if (jn != null && jn.size() > 0) {
                buff.append("<").append(elemName).append(">\n");
                buff.append("  <langs>\n");
                Iterator<Map.Entry<String, JsonNode>> iter = jn.getFields();
                while (iter.hasNext()) {
                    Map.Entry<String, JsonNode> item = iter.next();
                    buff.append("    ");
                    writeText(buff, item.getKey(), item.getValue().asText(), true);
                }
                buff.append("  </langs>\n");
                buff.append("</").append(elemName).append(">\n");
            } else {
                buff.append("<").append(elemName).append("></").append(elemName).append(">\n");
            }
        }
    }

    public String toXml() {
        StringBuilder buff = new StringBuilder();
        buff.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        buff.append("<BorqsResource version=\"1\" defaultLang=\"")
                .append(ObjectUtils.toString(defaultLanguage, "en_US")).append("\">\n");
        writeText(buff, "id", getId(), true);
        writeText(buff, "app", getAppId(), true);
        writeText(buff, "category", getCategory(), true);
        writeMultipleLangText(buff, "name", name, true);
        writeMultipleLangText(buff, "description", description, false);
        writeText(buff, "authorName", authorName, true);
        writeText(buff, "authorEmail", authorEmail, false);
        writeText(buff, "authorPhone", authorPhone, false);
        writeText(buff, "authorWebsite", authorWebsite, false);
        writeText(buff, "tags", tags, false);
        writeText(buff, "logo", logoPath, true);
        writeText(buff, "cover", coverPath, true);
        writeText(buff, "screenshot1", screenshot1Path, false);
        writeText(buff, "screenshot2", screenshot2Path, false);
        writeText(buff, "screenshot3", screenshot3Path, false);
        writeText(buff, "screenshot4", screenshot4Path, false);
        writeText(buff, "screenshot5", screenshot5Path, false);
        writeText(buff, "version", getVersion(), true);
        if (minAppVersion != 0) {
            writeText(buff, "minAppVC", minAppVersion, true);
        }
        if (maxAppVersion != Integer.MAX_VALUE) {
            writeText(buff, "maxAppVC", maxAppVersion, true);
        }
        if (ArrayUtils.isNotEmpty(supportedMod)) {
            writeText(buff, "supportedMod", StringUtils.join(supportedMod, ",") + ",", true);
        }
        writeMultipleLangText(buff, "versionName", versionName, true);
        writeMultipleLangText(buff, "recentChange", recentChange, false);
        buff.append("</BorqsResource>\n");
        return buff.toString();
    }


}
