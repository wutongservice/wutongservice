package com.borqs.server.market.resfile;

import com.borqs.server.market.models.VersionedProductId;
import com.borqs.server.market.utils.JsonUtils;
import com.borqs.server.market.utils.StringUtils2;
import org.apache.commons.io.IOUtils;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

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
        int version = selectIntValue(root, "version", 0);
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
        manifest.setMinAppVersion(selectIntValue(root, "minAppVC", 0));
        manifest.setMaxAppVersion(selectIntValue(root, "maxAppVC", Integer.MAX_VALUE));
        manifest.setSupportedMod(StringUtils2.splitArray(selectTextValue(root, "supportedMod", "", true), ',', true));
        manifest.setLogoPath(selectTextValue(root, "logo", null, true));
        manifest.setCoverPath(selectTextValue(root, "cover", null, true));
        manifest.setScreenshot1Path(selectTextValue(root, "screenshot1", null, true));
        manifest.setScreenshot2Path(selectTextValue(root, "screenshot2", null, true));
        manifest.setScreenshot3Path(selectTextValue(root, "screenshot3", null, true));
        manifest.setScreenshot4Path(selectTextValue(root, "screenshot4", null, true));
        manifest.setScreenshot5Path(selectTextValue(root, "screenshot5", null, true));
        manifest.setDependencies(readDependencies(root.element("dependencies")));
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
                int version = selectIntValue(depElem, "version", 0);
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
        return JsonUtils.toJson(all, true);

    }

}
