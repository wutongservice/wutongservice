package com.borqs.server.platform.feature.account;


import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.data.Values;
import com.borqs.server.platform.feature.TargetInfo;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.util.ClassHelper;
import com.borqs.server.platform.util.ColumnsExpander;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.ObjectHelper;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class User extends Addons implements JsonBean, Copyable<User> {

    public static final String COL_USER_ID = "user_id";
    public static final String COL_PASSWORD = "password";
    public static final String COL_CREATED_TIME = "created_time";
    public static final String COL_DESTROYED_TIME = "destroyed_time";
    public static final String COL_NAME = "name";
    public static final String COL_NICKNAME = "nickname";
    public static final String COL_PERHAPS_NAME = "perhaps_name";
    public static final String COL_DISPLAY_NAME = "display_name";
    public static final String COL_PHOTO = "photo";
    public static final String COL_PROFILE = "profile";
    public static final String COL_DATE = "date";
    public static final String COL_TEL = "tel";
    public static final String COL_EMAIL = "email";
    public static final String COL_IM = "im";
    public static final String COL_SIP_ADDRESS = "sip_address";
    public static final String COL_URL = "url";
    public static final String COL_ORGANIZATION = "organization";
    public static final String COL_ADDRESS = "address";
    public static final String COL_WORK_HISTORY = "work_history";
    public static final String COL_EDUCATION_HISTORY = "education_history";
    public static final String COL_MISCELLANEOUS = "miscellaneous";


    public static final String[] STANDARD_COLUMNS = {
            COL_USER_ID,
            COL_NAME,
            COL_NICKNAME,
            COL_PERHAPS_NAME,
            COL_DISPLAY_NAME,
            COL_PHOTO,
    };

    public static final String[] FULL_COLUMNS = {
            COL_USER_ID,
            COL_PASSWORD,
            COL_CREATED_TIME,
            COL_DESTROYED_TIME,
            COL_NAME,
            COL_NICKNAME,
            COL_PERHAPS_NAME,
            COL_DISPLAY_NAME,
            COL_PHOTO,
            COL_PROFILE,
            COL_DATE,
            COL_TEL,
            COL_EMAIL,
            COL_IM,
            COL_SIP_ADDRESS,
            COL_URL,
            COL_ORGANIZATION,
            COL_ADDRESS,
            COL_WORK_HISTORY,
            COL_EDUCATION_HISTORY,
            COL_MISCELLANEOUS,
    };

    private static Map<String, String[]> columnAliases = new ConcurrentHashMap<java.lang.String, java.lang.String[]>();

    static {
        registerColumnsAlias("@full,#full", FULL_COLUMNS);
        registerColumnsAlias("@std,#std", STANDARD_COLUMNS);
    }

    public static String[] expandColumns(String[] cols) {
        return ColumnsExpander.expand(cols, columnAliases);
    }

    public static void registerColumnsAlias(String alias, String[] cols) {
        columnAliases.put(alias, cols);
    }

    public static void unregisterColumnsAlias(String alias) {
        columnAliases.remove(alias);
    }


    // Note: Do NOT change/remove the below keys!
    public static final int KEY_NAME = 13;
    public static final int KEY_NICKNAME = 14;
    public static final int KEY_PHOTO = 15;
    public static final int KEY_PROFILE = 16;
    public static final int KEY_DATE = 17;
    public static final int KEY_TEL = 18;
    public static final int KEY_EMAIL = 19;
    public static final int KEY_IM = 20;
    public static final int KEY_SIP_ADDRESS = 21;
    public static final int KEY_URL = 22;
    public static final int KEY_ORGANIZATION = 23;
    public static final int KEY_ADDRESS = 24;
    public static final int KEY_WORK_HISTORY = 25;
    public static final int KEY_EDUCATION_HISTORY = 26;
    public static final int KEY_MISCELLANEOUS = 27;
    public static final int KEY_PERHAPS_NAME = 28;

    public static interface JsonGenerateExpansion {
        void expand(JsonGenerator jg, User user, String[] cols) throws IOException;
    }

    private static final List<JsonGenerateExpansion> USER_JSON_FORMAT_EXPANSIONS = new ArrayList<JsonGenerateExpansion>();

    static {
        USER_JSON_FORMAT_EXPANSIONS.add(new JsonGenerateExpansion() {
            @Override
            public void expand(JsonGenerator jg, User user, String[] cols) throws IOException {
                if (user == null)
                    return;

                if (cols == null || ArrayUtils.contains(cols, COL_DISPLAY_NAME)) {
                    NameInfo name = (NameInfo) user.getProperty(COL_NAME, null);
                    if (name != null)
                        jg.writeStringField(COL_DISPLAY_NAME, name.getDisplayName());
                }
            }
        });
    }

    private long userId;
    private String password;
    private long createdTime;
    private long destroyedTime;
    private final Map<String, Object> properties = new LinkedHashMap<String, Object>();
    private Map<String, Long> updatedTimes;

    public User() {
    }

    public User(long userId) {
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }

    public PeopleId getPeopleId() {
        return PeopleId.user(getUserId());
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getDestroyedTime() {
        return destroyedTime;
    }

    public void setDestroyedTime(long destroyedTime) {
        this.destroyedTime = destroyedTime;
    }

    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    public boolean hasProperty(String col) {
        return properties.containsKey(col);
    }

    public Set<String> getPropertyColumnsSet() {
        return properties.keySet();
    }

    public void removeProperty(String col) {
        properties.remove(col);
    }

    public String[] getPropertyColumns() {
        Set<String> keys = getPropertyColumnsSet();
        return keys.toArray(new String[keys.size()]);
    }

    public void setProperty(String col, Object value) {
        Validate.notEmpty(col);
        properties.put(col, Schema.checkForSet(col, value));
    }

    public void setProperties(Map<String, Object> values) {
        for (Map.Entry<String, Object> e : values.entrySet())
            setProperty(e.getKey(), e.getValue());
    }

    public Object getProperty(String col, Object def) {
        return properties.containsKey(col) ? properties.get(col) : def;
    }

    public long getPropertyUpdatedTime(String col) {
        return MapUtils.getLong(updatedTimes, col, 0L);
    }

    public long getMaxPropertyUpdatedTime(String... cols) {
        long t = 0;
        for (String col : cols) {
            long t0 = getPropertyUpdatedTime(col);
            if (t0 > t)
                t = t0;
        }
        return t;
    }

    public boolean getBooleanProperty(String col, boolean def) {
        return Values.toBoolean(getProperty(col, def));
    }

    public long getIntProperty(String col, long def) {
        return Values.toInt(getProperty(col, def));
    }

    public double getFloatProperty(String col, double def) {
        return Values.toFloat(getProperty(col, def));
    }

    public String getStringProperty(String col, String def) {
        return Values.toString(getProperty(col, def));
    }

    public PropertyEntries writeProperties(PropertyEntries reuse, Set<Integer> affectedKeys) {
        PropertyEntries entries = reuse != null ? reuse : new PropertyEntries();
        for (Map.Entry<String, Object> e : properties.entrySet()) {
            String col = e.getKey();
            Schema.Column c = Schema.column(col);
            if (affectedKeys != null)
                affectedKeys.add((int) c.key);

            // c != null
            switch (c.type) {
                case SIMPLE:
                    entries.addEntry(c.key, 0, 0, e.getValue());
                    break;

                case SIMPLE_ARRAY: {
                    List l = (List) e.getValue();
                    for (int i = 0; i < l.size(); i++)
                        entries.addEntry(c.key, 0, i, l.get(i));
                }
                break;

                case OBJECT: {
                    PropertyBundle bundle = (PropertyBundle) e.getValue();
                    Map<Integer, Object> props = bundle.writeProperties(null);
                    for (Map.Entry<Integer, Object> e1 : props.entrySet())
                        entries.addEntry(c.key, e1.getKey(), 0, e1.getValue());
                }
                break;

                case OBJECT_ARRAY: {
                    Map<Integer, Object> props = new LinkedHashMap<Integer, Object>();
                    List l = (List) e.getValue();
                    for (int i = 0; i < l.size(); i++) {
                        props.clear();
                        PropertyBundle bundle = (PropertyBundle) l.get(i);
                        bundle.writeProperties(props);
                        for (Map.Entry<Integer, Object> e1 : props.entrySet())
                            entries.addEntry(c.key, e1.getKey(), i, e1.getValue());
                    }
                }
                break;

                default:
                    throw new IllegalStateException();
            }
        }
        return entries;
    }

    private void setUpdatedTime(String col, long updatedTime) {
        if (updatedTime > 0) {
            if (updatedTimes == null)
                updatedTimes = new LinkedHashMap<String, Long>();

            updatedTimes.put(col, updatedTime);
        }
    }

    @SuppressWarnings("unchecked")
    public void readProperties(PropertyEntries entries) {
        for (Schema.Column c : Schema.columns()) {
            switch (c.type) {
                case SIMPLE: {
                    Object val = entries.getSimple(c.key);
                    if (val != null) {
                        properties.put(c.column, val);
                        setUpdatedTime(c.column, entries.getMaxUpdatedTime(c.key));
                    }
                }
                break;

                case SIMPLE_ARRAY: {
                    List l = entries.getSimpleArray(c.key);
                    if (CollectionUtils.isNotEmpty(l)) {
                        properties.put(c.column, l);
                        setUpdatedTime(c.column, entries.getMaxUpdatedTime(c.key));
                    }
                }
                break;

                case OBJECT: {
                    Map<Integer, Object> obj = entries.getObject(c.key);
                    if (MapUtils.isNotEmpty(obj)) {
                        PropertyBundle bundle = (PropertyBundle) ClassHelper.newInstance(c.clazz);
                        bundle.readProperties(obj, false);
                        properties.put(c.column, bundle);
                        setUpdatedTime(c.column, entries.getMaxUpdatedTime(c.key));
                    }
                }
                break;

                case OBJECT_ARRAY: {
                    Map<Integer, Object>[] objArr = entries.getObjectArray(c.key);
                    if (ArrayUtils.isNotEmpty(objArr)) {
                        ArrayList l = new ArrayList(objArr.length);
                        for (Map<Integer, Object> obj : objArr) {
                            PropertyBundle bundle = (PropertyBundle) ClassHelper.newInstance(c.clazz);
                            bundle.readProperties(obj, false);
                            l.add(bundle);
                        }
                        properties.put(c.column, l);
                        setUpdatedTime(c.column, entries.getMaxUpdatedTime(c.key));
                    }
                }
                break;
            }
        }
    }

    public static JsonBean deserializeObjectProperty(JsonNode jn, Class clazz) {
        JsonBean obj = (JsonBean) ClassHelper.newInstance(clazz);
        obj.deserialize(jn);
        return obj;
    }

    @Override
    public void deserialize(JsonNode jn) {
        if (jn.has(COL_USER_ID))
            setUserId(jn.path(COL_USER_ID).getValueAsLong());
        if (jn.has(COL_PASSWORD))
            setPassword(jn.path(COL_PASSWORD).getTextValue());
        if (jn.has(COL_CREATED_TIME))
            setCreatedTime(jn.path(COL_CREATED_TIME).getValueAsLong());
        if (jn.has(COL_DESTROYED_TIME))
            setDestroyedTime(jn.path(COL_DESTROYED_TIME).getValueAsLong());

        for (Schema.Column c : Schema.columns()) {
            String col = c.column;
            if (jn.has(col)) {
                Object v = propertyFromJsonNode(c, jn.path(col));
                setProperty(col, v);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Object propertyFromJsonNode(Schema.Column c, JsonNode jn) {
        switch (c.type) {
            case SIMPLE: {
                return JsonHelper.trimSimpleJsonNode(jn);
            }

            case SIMPLE_ARRAY: {
                if (!jn.isArray())
                    throw new IllegalArgumentException("Illegal node type " + jn.toString());
                ArrayList l = new ArrayList(jn.size());
                for (int i = 0; i < jn.size(); i++) {
                    l.add(JsonHelper.trimSimpleJsonNode(jn.get(i)));
                }
                return l;
            }

            case OBJECT: {
                if (!jn.isObject())
                    throw new IllegalArgumentException("Illegal node type " + jn.toString());

                return deserializeObjectProperty(jn, c.clazz);
            }

            case OBJECT_ARRAY: {
                if (!jn.isArray())
                    throw new IllegalArgumentException("Illegal node type " + jn.toString());

                ArrayList l = new ArrayList(jn.size());
                for (int i = 0; i < jn.size(); i++) {
                    JsonNode objNode = jn.get(i);
                    if (!objNode.isObject())
                        throw new IllegalArgumentException("Illegal node type " + objNode.toString());

                    l.add(deserializeObjectProperty(objNode, c.clazz));
                }
                return l;
            }

            default:
                throw new IllegalArgumentException();
        }
    }


    public static User fromJsonNode(JsonNode jn) {
        User user = new User();
        user.deserialize(jn);
        return user;
    }


    public static User fromJson(String json) {
        return fromJsonNode(JsonHelper.parse(json));
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        serialize(jg, (String[]) null);
    }

    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartObject();
        if (outputColumn(cols, COL_USER_ID))
            jg.writeNumberField(COL_USER_ID, getUserId());
        if (outputColumn(cols, COL_PASSWORD))
            jg.writeStringField(COL_PASSWORD, getPassword());
        if (outputColumn(cols, COL_CREATED_TIME))
            jg.writeNumberField(COL_CREATED_TIME, getCreatedTime());
        if (outputColumn(cols, COL_DESTROYED_TIME))
            jg.writeNumberField(COL_DESTROYED_TIME, getDestroyedTime());

        for (Schema.Column c : Schema.columns()) {
            if (outputColumn(cols, c.column)) {
                Object val;
                if (properties.containsKey(c.column)) {
                    val = properties.get(c.column);
                } else {
                    val = c.newDefaultValue();
                }

                jg.writeFieldName(c.column);
                JsonHelper.writeValue(jg, val, null);
            }
        }

        writeAddonsJson(jg, cols);

        for (JsonGenerateExpansion jgexp : USER_JSON_FORMAT_EXPANSIONS)
            jgexp.expand(jg, this, cols);

        jg.writeEndObject();
    }

    public String toJson(final String[] cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serialize(jg, cols);
            }
        }, human);
    }

    @Override
    public String toString() {
        return toJson(null, true);
    }

    @Override
    public User copy() {
        User user = new User();
        user.setUserId(userId);
        user.setPassword(password);
        user.setCreatedTime(createdTime);
        user.setDestroyedTime(destroyedTime);
        user.properties.putAll(properties);
        user.addons = copyAddons();
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        User other = (User) o;
        return equalsWithoutUpdatedTimesAndAddons(other)
                && ObjectUtils.equals(updatedTimes, other.updatedTimes)
                && ObjectUtils.equals(addons, other.addons);
    }

    public boolean propertiesEquals(User other) {
        return other != null && ObjectUtils.equals(properties, other.properties);
    }

    public boolean equalsWithoutUpdatedTimesAndAddons(User other) {
        boolean b = other != null
                && userId == other.userId
                && StringUtils.equals(password, other.password)
                && createdTime == other.createdTime
                && destroyedTime == other.destroyedTime;

        if (!b)
            return false;

        b = ObjectUtils.equals(properties, other.properties);
        return b;
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(userId, password, createdTime, destroyedTime, properties, updatedTimes, addons);
    }


    // shortcut methods

    public void setName(NameInfo name) {
        setProperty(COL_NAME, name);
    }

    public NameInfo getName() {
        return (NameInfo) getProperty(COL_NAME, null);
    }

    public void setNickname(String nickname) {
        setProperty(COL_NICKNAME, nickname);
    }

    public String getPerhapsname() {
        return getStringProperty(COL_PERHAPS_NAME, null);
    }

    public void setPerhapsname(String perhapsName) {
        setProperty(COL_PERHAPS_NAME, perhapsName);
    }

    public String getNickname() {
        return getStringProperty(COL_NICKNAME, null);
    }

    public String getDisplayName() {
        NameInfo name = getName();
        return name == null ? "" : name.getDisplayName();
    }

    public void setPhoto(PhotoInfo photo) {
        setProperty(COL_PHOTO, photo);
    }

    public PhotoInfo getPhoto() {
        return (PhotoInfo) getProperty(COL_PHOTO, null);
    }

    public void setProfile(ProfileInfo profile) {
        setProperty(COL_PROFILE, profile);
    }

    public ProfileInfo getProfile() {
        return (ProfileInfo) getProperty(COL_PROFILE, null);
    }

    @SuppressWarnings("unchecked")
    public List<DateInfo> getDate() {
        return (List<DateInfo>) getProperty(COL_DATE, null);
    }

    public void setDate(List<DateInfo> date) {
        setProperty(COL_DATE, date);
    }

    public void setDate(DateInfo... date) {
        setDate(Arrays.asList(date));
    }

    @SuppressWarnings("unchecked")
    public List<TelInfo> getTel() {
        return (List<TelInfo>) getProperty(COL_TEL, null);
    }

    public void setTel(List<TelInfo> tel) {
        setProperty(COL_TEL, tel);
    }

    public void setTel(TelInfo... tel) {
        setTel(Arrays.asList(tel));
    }

    @SuppressWarnings("unchecked")
    public List<EmailInfo> getEmail() {
        return (List<EmailInfo>) getProperty(COL_EMAIL, null);
    }

    public void setEmail(List<EmailInfo> email) {
        setProperty(COL_EMAIL, email);
    }

    public void setEmail(EmailInfo... email) {
        setEmail(Arrays.asList(email));
    }

    @SuppressWarnings("unchecked")
    public List<ImInfo> getIm() {
        return (List<ImInfo>) getProperty(COL_IM, null);
    }

    public void setIm(List<ImInfo> im) {
        setProperty(COL_IM, im);
    }

    public void setIm(ImInfo... im) {
        setIm(Arrays.asList(im));
    }

    @SuppressWarnings("unchecked")
    public List<SipAddressInfo> getSipAddress() {
        return (List<SipAddressInfo>) getProperty(COL_SIP_ADDRESS, null);
    }

    public void setSipAddress(List<SipAddressInfo> sipAddress) {
        setProperty(COL_SIP_ADDRESS, sipAddress);
    }

    public void setSipAddress(SipAddressInfo... sipAddress) {
        setSipAddress(Arrays.asList(sipAddress));
    }

    @SuppressWarnings("unchecked")
    public List<UrlInfo> getUrl() {
        return (List<UrlInfo>) getProperty(COL_URL, null);
    }

    public void setUrl(List<UrlInfo> url) {
        setProperty(COL_URL, url);
    }

    public void setUrl(UrlInfo... url) {
        setUrl(Arrays.asList(url));
    }

    @SuppressWarnings("unchecked")
    public List<OrgInfo> getOrganization() {
        return (List<OrgInfo>) getProperty(COL_ORGANIZATION, null);
    }

    public void setOrganization(List<OrgInfo> org) {
        setProperty(COL_ORGANIZATION, org);
    }

    public void setOrganization(OrgInfo... org) {
        setOrganization(Arrays.asList(org));
    }

    @SuppressWarnings("unchecked")
    public List<AddressInfo> getAddress() {
        return (List<AddressInfo>) getProperty(COL_ADDRESS, null);
    }

    public void setAddress(List<AddressInfo> address) {
        setProperty(COL_ADDRESS, address);
    }

    public void setAddress(AddressInfo... address) {
        setAddress(Arrays.asList(address));
    }

    @SuppressWarnings("unchecked")
    public List<WorkHistory> getWorkHistory() {
        return (List<WorkHistory>) getProperty(COL_WORK_HISTORY, null);
    }

    public void setWorkHistory(List<WorkHistory> workHistory) {
        setProperty(COL_WORK_HISTORY, workHistory);
    }

    public void setWorkHistory(WorkHistory... workHistory) {
        setWorkHistory(Arrays.asList(workHistory));
    }

    @SuppressWarnings("unchecked")
    public List<EduHistory> getEducationHistory() {
        return (List<EduHistory>) getProperty(COL_EDUCATION_HISTORY, null);
    }

    public void setEducationHistory(List<EduHistory> eduHistory) {
        setProperty(COL_EDUCATION_HISTORY, eduHistory);
    }

    public void setEducationHistory(EduHistory... eduHistory) {
        setEducationHistory(Arrays.asList(eduHistory));
    }

    public MiscInfo getMiscellaneous() {
        return (MiscInfo) getProperty(COL_MISCELLANEOUS, null);
    }

    public void setMiscellaneous(MiscInfo misc) {
        setProperty(COL_MISCELLANEOUS, misc);
    }

    public String getBirthday() {
        List<DateInfo> date = getDate();
        if (CollectionUtils.isNotEmpty(date)) {
            for (DateInfo di : date) {
                if (DateInfo.TYPE_BIRTHDAY.equals(di.getType()))
                    return ObjectUtils.toString(di.getInfo());
            }
            return "";
        } else {
            return "";
        }
    }

    public TargetInfo getTargetInfo() {
        PhotoInfo info = getPhoto();
        return TargetInfo.of(getPeopleId(), getDisplayName(), info != null ? ObjectUtils.toString(info.getMiddleUrl()) : "");
    }
}
