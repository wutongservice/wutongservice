package com.borqs.server.service.platform.company;

import com.borqs.server.ErrorCode;
import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.*;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Group;
import com.borqs.server.service.platform.Platform;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class CompanyLogic {

    private final Platform platform;
    private ConnectionFactory connectionFactory;
    private String db;
    private String companyTable;
    private String companyEmailDomainBlacklistTable;
    private String employeeListTable;
    private String companyImagePattern;

    public CompanyLogic(Platform platform) {
        this.platform = platform;
        Configuration conf = platform.getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("employeeList.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("platform.simple.db", null);
        this.companyTable = conf.getString("company.simple.companyTable", "company");
        this.companyEmailDomainBlacklistTable = conf.getString("company.simple.companyEmailDomainBlacklistTable", "company_email_domain_blacklist");
        this.employeeListTable = conf.getString("company.simple.employeeListTable", "employee_list");
        this.companyImagePattern = conf.getString("company.imageUrlPattern", "http://storage.aliyun.com/wutong-data/media/photo/%s");
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    public static String getEmailDomain(String email) {
        email = ObjectUtils.toString(StringUtils.trim(email));
        return StringUtils.contains(email, "@") ? StringUtils.substringAfter(email, "@") : "";
    }

    public boolean isCompanyEmail(String email) {
        String emailDomain = getEmailDomain(email);
        if (StringUtils.isEmpty(emailDomain))
            return false;

        String sql = new SQLBuilder.Select()
                .select("email_domain")
                .from(companyEmailDomainBlacklistTable)
                .where("email_domain=${v(ed)}", "ed", emailDomain)
                .toString();

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return MapUtils.isEmpty(rec);
    }

    private Record findCompanyByEmailDomain(String emailDomain) {
        String sql = new SQLBuilder.Select()
                .select("company_id", "name", "address", "email", "website", "tel", "fax", "logo_url", "small_logo_url", "large_logo_url", "cover_url", "small_cover_url", "large_cover_url", "description", "department_id")
                .from(companyTable)
                .where("destroyed_time=0 AND (email_domain1=${v(ed)} OR email_domain2=${v(ed)} OR email_domain3=${v(ed)} OR email_domain4=${v(ed)})", "ed", ObjectUtils.toString(emailDomain))
                .toString();
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return MapUtils.isEmpty(rec) ? null : rec;
    }

    public void joinCompanyOrCreateCompany(final long viewerId, int appId, String email) throws AvroRemoteException {
        if (!isCompanyEmail(email))
            return;

        String emailDomain = getEmailDomain(email);
        Record companyRec = findCompanyByEmailDomain(emailDomain);
        if (MapUtils.isEmpty(companyRec)) {
            String companyName = StringUtils.substringBeforeLast(emailDomain, ".");
            createCompany(viewerId, appId, companyName, emailDomain);
        } else {
            final long depId = findDepartmentId(companyRec.checkGetInt("company_id"));
            platform.useRawGroup(new Platform.GroupAccessor<java.lang.Object>() {
                @Override
                public Object access(Group g) throws AvroRemoteException {
                    g.addMember(depId, viewerId, Constants.ROLE_MEMBER);
                    return null;
                }
            });
        }
    }

    public Record createCompany(long viewerId, int appId, String name, String emailDomain) throws AvroRemoteException {
        if (findCompanyByEmailDomain(emailDomain) != null)
            return null;

        long now = DateUtils.nowMillis();
        String sql = new SQLBuilder.Insert()
                .insertIgnoreInto(companyTable)
                .values(new Record()
                        .set("created_time", now)
                        .set("updated_time", now)
                        .set("destroyed_time", 0L)
                        .set("email_domain1", ObjectUtils.toString(emailDomain))
                        .set("email_domain2", "")
                        .set("email_domain3", "")
                        .set("email_domain4", "")
                        .set("name", ObjectUtils.toString(name))
                        .set("name_en", "")
                        .set("email", "")
                        .set("website", "")
                        .set("tel", "")
                        .set("fax", "")
                        .set("zip_code", "")
                        .set("small_logo_url", "")
                        .set("logo_url", "")
                        .set("large_logo_url", "")
                        .set("small_cover_url", "")
                        .set("cover_url", "")
                        .set("large_cover_url", "")
                        .set("description", "")
                        .set("description_en", "")
                        .set("department_id", 0L)
                ).toString();

        SQLExecutor se = getSqlExecutor();
        ObjectHolder<Long> idHolder = new ObjectHolder<Long>(0L);
        long n = se.executeUpdate(sql, idHolder);
        if (n == 0)
            return null;

        long companyId = idHolder.value;


        long depId = 0;
        try {
            depId = platform.createGroup(
                    Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.TYPE_PUBLIC_CIRCLE,
                    ObjectUtils.toString(name), // name
                    100000, // limit
                    1,      // stream public
                    0,      // search
                    1,      // view members
                    2,      // join
                    0,      // member invite
                    0,      // member approve
                    1,      // member post
                    0,      // member quit
                    0,      // need invite confirm
                    viewerId,
                    "",
                    new Record().set(Constants.DEP_COL_COMPANY, Long.toString(companyId))
                            .set(Constants.DEP_COL_IS_DEP, "1")
                            .set(Constants.DEP_COL_IS_ROOT, "1"),
                    Long.toString(viewerId),
                    "",
                    "",
                    Integer.toString(appId));
        } catch (Exception ignored) {
            ignored.printStackTrace(System.err);
        }
        if (depId == 0) {
            sql = new SQLBuilder.Delete()
                    .deleteFrom(companyTable)
                    .where("company_id=${v(company_id)}", "company_id", companyId)
                    .toString();
            se.executeUpdate(sql);
            return null;
        }

        sql = new SQLBuilder.Update()
                .update(companyTable)
                .values(new Record().set("department_id", depId))
                .where("company_id=${v(company_id)}", "company_id", companyId)
                .toString();
        se.executeUpdate(sql);

        RecordSet companyRecs = getCompanies(viewerId, companyId);
        return CollectionUtils.isNotEmpty(companyRecs) ? companyRecs.get(0) : null;
    }

    public boolean destroyCompany(long viewerId, long companyId) throws AvroRemoteException {
        long depId = findDepartmentId(companyId);
        if (depId > 0) {
            platform.destroyGroup(Long.toString(viewerId), Long.toString(depId));
        }
        String sql = new SQLBuilder.Update()
                .update(companyTable)
                .values(new Record().set("destroyed_time", DateUtils.nowMillis()))
                .where("destroyed_time=0 AND company_id=${v(company_id)}", "company_id", companyId)
                .toString();
        SQLExecutor se = getSqlExecutor();
        return se.executeUpdate(sql) > 0;
    }

    //void checkEmailDomains(0)

    public Record updateCompany(long viewerId, long companyId, Record info) throws AvroRemoteException{
        checkAdmin(viewerId, companyId);



        String sql = new SQLBuilder.Update()
                .update(companyTable)
                .valueIf(info.has("email_domain1"), "email_domain1", info.getString("email_domain1"))
                .valueIf(info.has("email_domain2"), "email_domain2", info.getString("email_domain2"))
                .valueIf(info.has("email_domain3"), "email_domain3", info.getString("email_domain3"))
                .valueIf(info.has("email_domain4"), "email_domain4", info.getString("email_domain4"))
                .valueIf(info.has("name"), "name", info.getString("name"))
                .valueIf(info.has("address"), "address", info.getString("address"))
                .valueIf(info.has("email"), "email", info.getString("email"))
                .valueIf(info.has("website"), "website", info.getString("website"))
                .valueIf(info.has("tel"), "tel", info.getString("tel"))
                .valueIf(info.has("fax"), "fax", info.getString("fax"))
                .valueIf(info.has("zip_code"), "zip_code", info.getString("zip_code"))
                .valueIf(info.has("small_logo_url"), "small_logo_url", info.getString("small_logo_url"))
                .valueIf(info.has("logo_url"), "logo_url", info.getString("logo_url"))
                .valueIf(info.has("large_logo_url"), "large_logo_url", info.getString("large_logo_url"))
                .valueIf(info.has("small_cover_url"), "small_cover_url", info.getString("small_cover_url"))
                .valueIf(info.has("cover_url"), "cover_url", info.getString("cover_url"))
                .valueIf(info.has("large_cover_url"), "large_cover_url", info.getString("large_cover_url"))
                .valueIf(info.has("description"), "description", info.getString("description"))
                .valueIf(!info.isEmpty(), "updated_time", DateUtils.nowMillis())
                .where("destroyed_time=0 AND company_id=${v(company_id)}", "company_id", companyId)
                .toString();

        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql);
        RecordSet recs = getCompanies(viewerId, companyId);
        return CollectionUtils.isEmpty(recs) ? null : recs.get(0);
    }

    private long findDepartmentId(long companyIdOrDepId) {
        if (companyIdOrDepId >= Constants.PUBLIC_CIRCLE_ID_BEGIN && companyIdOrDepId < Constants.PUBLIC_CIRCLE_ID_END)
            return companyIdOrDepId;

        // company id
        String sql = new SQLBuilder.Select()
                .select("department_id")
                .from(companyTable)
                .where("destroyed_time=0 AND company_id=${v(company_id)}", "company_id", companyIdOrDepId)
                .toString();

        SQLExecutor se = getSqlExecutor();
        return se.executeIntScalar(sql, 0L);
    }

    private Map<Long, Long> findCompanyIds(long[] depIds) {
        final HashMap<Long, Long> m = new HashMap<Long, Long>();
        if (ArrayUtils.isNotEmpty(depIds)) {
            String sql = new SQLBuilder.Select()
                    .select("company_id", "department_id")
                    .from(companyTable)
                    .where("destroyed_time=0 AND department_id IN (${dep_ids})", "dep_ids", StringUtils2.join(depIds, ","))
                    .toString();
            SQLExecutor se = getSqlExecutor();
            se.executeRecordHandler(sql, new RecordHandler() {
                @Override
                public void handle(Record rec) {
                    m.put(rec.checkGetInt("department_id"), rec.checkGetInt("company_id"));
                }
            });
        }
        return m;
    }

    public Record createCompanyFromGroup(int appId, String name, String emailDomain, final long groupId) throws AvroRemoteException {
        long creatorId = platform.useRawGroup(new Platform.GroupAccessor<java.lang.Long>() {
            @Override
            public Long access(Group g) throws AvroRemoteException {
                return g.getCreator(groupId);
            }
        });
        if (creatorId == 0L)
            return null;

        final LinkedHashSet<Long> userIds = new LinkedHashSet<Long>();
        userIds.add(creatorId);

        Record companyRec = createCompany(creatorId, appId, name, emailDomain);
        long companyId = companyRec.checkGetInt("company_id");
        final long depId = companyRec.checkGetInt("department_id");
        platform.useRawGroup(new Platform.GroupAccessor<java.lang.Object>() {
            @Override
            public Object access(Group g) throws AvroRemoteException {
                String adminIds = ObjectUtils.toString(g.getMembersByRole(groupId, Constants.ROLE_ADMIN, -1, -1, ""));
                String memberIds = ObjectUtils.toString(g.getMembersByRole(groupId, Constants.ROLE_MEMBER, -1, -1, ""));
                Record roles = new Record();
                for (String s : StringUtils2.splitSet(adminIds, ",", true)) {
                    roles.put(s, Constants.ROLE_ADMIN);
                    userIds.add(Long.parseLong(s));
                }
                for (String s : StringUtils2.splitSet(memberIds, ",", true)) {
                    roles.put(s, Constants.ROLE_MEMBER);
                    userIds.add(Long.parseLong(s));
                }
                g.addMembers(depId, roles.toByteBuffer());
                return null;
            }
        });

        RecordSet recs = getCompanies(creatorId, companyId);
        return CollectionUtils.isNotEmpty(recs) ? recs.get(0) : null;
    }

    private void renameCircles(RecordSet recs) {
        recs.renameColumn("id", "circle_id");
        recs.renameColumn("name", "circle_name");
    }

    public RecordSet getCompanies(long viewerId, long... companyIds) throws AvroRemoteException {
        if (ArrayUtils.isEmpty(companyIds))
            return new RecordSet();

        String sql = new SQLBuilder.Select()
                .select("company_id", "created_time", "updated_time", "email_domain1", "email_domain2", "email_domain3", "email_domain4", "name", "address", "email", "website", "tel", "fax", "zip_code", "logo_url", "small_logo_url", "large_logo_url", "cover_url", "small_cover_url", "large_cover_url", "description", "department_id")
                .from(companyTable)
                .where("destroyed_time=0 AND company_id IN (${company_ids})", "company_ids", StringUtils2.join(companyIds, ","))
                .orderBy("company_id", "ASC")
                .toString();
        SQLExecutor se = getSqlExecutor();
        RecordSet companyRecs =  se.executeRecordSet(sql, null);
        for (Record companyRec : companyRecs) {
            addCompanyImagePrefix(companyImagePattern, companyRec);
        }
        expandCompanyInfo(viewerId, companyRecs, 5);
        return companyRecs;
    }

    public RecordSet getCompanyDepCircles(long viewerId, final long companyId) throws AvroRemoteException {
        // sub_department
        String subIds = platform.useRawGroup(new Platform.GroupAccessor<String>() {
            @Override
            public String access(Group g) throws AvroRemoteException {
                String s = ObjectUtils.toString(g.findGroupIdsByProperty("company", Long.toString(companyId), 0));
                return s;
            }
        });


        RecordSet recs = platform.getGroups(Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END, Long.toString(viewerId), subIds, "id,name,small_image_url,large_image_url,image_url,company,is_department,is_company_root", false);
        renameCircles(recs);
        removeRootDep(recs);
        return recs;
    }

    private void removeRootDep(RecordSet recs) {
        int rootIndex = -1;
        for (int i = 0; i < recs.size(); i++) {
            Record rec = recs.get(i);
            if (rec.getString("is_company_root", "0").equals("1")) {
                rootIndex = i;
            }
        }
        if (rootIndex >= 0)
            recs.remove(rootIndex);
    }

    private void expandCompanyInfo(long viewerId, RecordSet companyRecs, final int maxSubDepCount) throws AvroRemoteException {
        SQLExecutor se = getSqlExecutor();
        for (Record rec : companyRecs) {
            final long companyId = rec.checkGetInt("company_id");

            // employee_count
            String sql = new SQLBuilder.Select()
                    .select("COUNT(*)")
                    .from(employeeListTable)
                    .where("company_id=${v(company_id)}", "company_id", companyId)
                    .toString();
            long employeeCount = se.executeIntScalar(sql, 0L);

            // some_members
            final long depId = rec.checkGetInt("department_id");
            String userIds = platform.useRawGroup(new Platform.GroupAccessor<String>() {
                @Override
                public String access(Group g) throws AvroRemoteException {
                    return ObjectUtils.toString(g.getAllMembers(depId, 0, 5, ""));
                }
            });
            RecordSet userRecs = platform.getUsers(Long.toString(viewerId), userIds, "user_id, display_name, small_image_url, image_url, large_image_url", false);
            rec.put("employee_count", employeeCount);
            rec.put("some_members", userRecs);

            // sub_department
            String subIds = platform.useRawGroup(new Platform.GroupAccessor<String>() {
                @Override
                public String access(Group g) throws AvroRemoteException {
                    String s = ObjectUtils.toString(g.findGroupIdsByProperty("company", Long.toString(companyId), 0));
                    return s;
                }
            });


            RecordSet subCircles = platform.getGroups(Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END, Long.toString(viewerId), subIds, "id,name,small_image_url,large_image_url,image_url,company,is_department,is_company_root", false);
            renameCircles(subCircles);
            removeRootDep(subCircles);
            int count = subCircles.size();
            rec.put("sub_department_count", count);
            if (subCircles.size() > maxSubDepCount) {
                subCircles.slice(0, maxSubDepCount);
            }
            rec.put("sub_department", subCircles);
        }
    }

    public RecordSet searchCompanies(long viewerId, String kw) {
        if (StringUtils.isBlank(kw))
            return new RecordSet();

        String sql = new SQLBuilder.Select()
                .select("company_id", "created_time", "updated_time", "email_domain1", "email_domain2", "email_domain3", "email_domain4", "name", "address", "email", "website", "tel", "fax", "zip_code", "small_logo_url", "logo_url", "large_logo_url", "description", "department_id")
                .from(companyTable)
                .where("destroyed_time=0 AND name like ${v(kw)}", "kw", "%" + kw + "%")
                .orderBy("company_id", "ASC")
                .toString();

        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql, null);
    }

    private Map<Long, Integer> getDepartmentRoles(long viewerId, final long userId) throws AvroRemoteException {
        final LinkedHashMap<Long, Integer> m = new LinkedHashMap<Long, Integer>();
        platform.useRawGroup(new Platform.GroupAccessor<java.lang.Object>() {
            @Override
            public Object access(Group g) throws AvroRemoteException {
                String s = ObjectUtils.toString(g.findGroupIdsByMember(Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END, userId));
                long[] depIds = StringUtils2.splitIntArray(s, ",");
                for (long depId : depIds) {
                    int role = Constants.ROLE_GUEST;
                    if (g.hasRight(depId, userId, Constants.ROLE_CREATOR)) {
                        role = Constants.ROLE_CREATOR;
                    } else if (g.hasRight(depId, userId, Constants.ROLE_ADMIN)) {
                        role = Constants.ROLE_ADMIN;
                    } else if (g.hasRight(depId, userId, Constants.ROLE_MEMBER)) {
                        role = Constants.ROLE_MEMBER;
                    }
                    m.put(depId, role);
                }
                return null;
            }
        });
        return m;
    }

    public RecordSet belongsCompanies(long viewerId, final long userId) throws AvroRemoteException {
//        final ArrayList<Long> depIds = new ArrayList<Long>();
//        platform.useRawGroup(new Platform.GroupAccessor<java.lang.Object>() {
//            @Override
//            public Object access(Group g) throws AvroRemoteException {
//                String s = ObjectUtils.toString(g.findGroupIdsByMember(Constants.DEPARTMENT_ID_BEGIN, Constants.DEPARTMENT_ID_END, userId));
//                depIds.addAll(StringUtils2.splitIntList(s, ","));
//                return null;
//            }
//        });

        Map<Long, Integer> depId2RoleMap = getDepartmentRoles(viewerId, userId);
        ArrayList<Long> depIds = new ArrayList<Long>(depId2RoleMap.keySet());
        if (depIds.isEmpty())
            return new RecordSet();

        Map<Long, Long> depId2CompanyIdMap = findCompanyIds(ArrayUtils.toPrimitive(depIds.toArray(new Long[depIds.size()])));
        long[] companyIds = CollectionUtils2.toLongArray(depId2CompanyIdMap.values());
        RecordSet recs = getCompanies(viewerId, companyIds);
        for (Record rec : recs) {
            long depId = rec.getInt("department_id", 0);
            int role = Constants.ROLE_GUEST;
            if (depId > 0) {
                if (depId2RoleMap.containsKey(depId))
                    role = depId2RoleMap.get(depId);
            }
            rec.put("role", role);
        }
        return recs;
    }

    public boolean grant(long viewerId, long companyId, Map<Long, Integer> roles) throws AvroRemoteException {
        long depId = findDepartmentId(companyId);
        if (depId <= 0)
            return false;

        Record roles1 = new Record();
        for (Map.Entry<Long, Integer> e : roles.entrySet()) {
            roles1.put(Long.toString(e.getKey()), e.getValue());
        }
        return platform.grants(Long.toString(viewerId), depId, roles1);
    }

    public boolean companyExists(long companyId) throws AvroRemoteException {
        String sql = new SQLBuilder.Select()
                .select("COUNT(*)")
                .from(companyTable)
                .where("destroyed_time=0 AND company_id = ${v(company_id)}", "company_id", companyId)
                .toString();
        SQLExecutor se = getSqlExecutor();
        long n = se.executeIntScalar(sql, 0L);
        return n > 0;
    }

    private boolean isAdmin(final long viewerId, final long companyId) throws AvroRemoteException {
        final long depId = findDepartmentId(companyId);
        if (depId <= 0)
            return false;

        boolean b = platform.useRawGroup(new Platform.GroupAccessor<Boolean>() {
            @Override
            public Boolean access(Group g) throws AvroRemoteException {
                return g.hasRight(depId, viewerId, Constants.ROLE_ADMIN);
            }
        });
        return b;
    }

    public void checkAdmin(long viewerId, long companyId) throws AvroRemoteException {
        if (!isAdmin(viewerId, companyId))
            throw new ServerException(ErrorCode.GROUP_ERROR);
    }

    public RecordSet uploadEmployees(long viewerId, long companyId, FileItem excelFile, boolean merge) throws AvroRemoteException {
        checkAdmin(viewerId, companyId);
        RecordSet l = loadExcelFile(excelFile);

        SQLExecutor se = getSqlExecutor();
        if (!merge) {
            String sql = new SQLBuilder.Delete()
                    .deleteFrom(employeeListTable)
                    .where("company_id=${v(company_id)}", "company_id", companyId)
                    .toString();
            se.executeUpdate(sql);
        }

        ArrayList<String> sqls = new ArrayList<String>();
        for (Record rec : l) {
            String sql = new SQLBuilder.Replace()
                    .replaceInto(employeeListTable)
                    .values(new Record()
                            .set("company_id", companyId)
                            .set("employee_id", rec.getString("employee_id", ""))
                            .set("email", rec.getString("email", ""))
                            .set("name", rec.getString("name", ""))
                            .set("tel", rec.getString("tel", ""))
                            .set("mobile_tel", rec.getString("mobile_tel", ""))
                            .set("department", rec.getString("department", ""))
                            .set("job_title", rec.getString("job_title", ""))
                            .set("comment", rec.getString("comment", ""))
                    )
                    .toString();
            sqls.add(sql);
        }
        se.executeUpdate(sqls);
        for (Record rec : l) {
            addUsersByEmail(rec.getString("email"));
        }
        return listEmployees(viewerId, companyId, EmployeeListConstants.COL_NAME, 0, 1000);
    }

    private static void addCompanyImagePrefix(String imagePattern, Record rec) {
        if (rec.has("logo_url")) {
            if (!rec.getString("logo_url", "").startsWith("http:") && StringUtils.isNotBlank(rec.getString("logo_url")))
                rec.put("logo_url", String.format(imagePattern, rec.getString("logo_url")));
        }

        if (rec.has("small_logo_url")) {
            if (!rec.getString("small_logo_url", "").startsWith("http:")  && StringUtils.isNotBlank(rec.getString("small_logo_url")))
                rec.put("small_logo_url", String.format(imagePattern, rec.getString("small_logo_url")));
        }

        if (rec.has("large_logo_url")) {
            if (!rec.getString("large_logo_url", "").startsWith("http:") && StringUtils.isNotBlank(rec.getString("large_logo_url")))
                rec.put("large_logo_url", String.format(imagePattern, rec.getString("large_logo_url")));
        }

        if (rec.has("cover_url")) {
            if (!rec.getString("cover_url", "").startsWith("http:") && StringUtils.isNotBlank(rec.getString("cover_url")))
                rec.put("cover_url", String.format(imagePattern, rec.getString("cover_url")));
        }

        if (rec.has("small_cover_url")) {
            if (!rec.getString("small_cover_url", "").startsWith("http:") && StringUtils.isNotBlank(rec.getString("small_cover_url")))
                rec.put("small_cover_url", String.format(imagePattern, rec.getString("small_cover_url")));
        }

        if (rec.has("large_cover_url")) {
            if (!rec.getString("large_cover_url", "").startsWith("http:") && StringUtils.isNotBlank(rec.getString("large_cover_url")))
                rec.put("large_cover_url", String.format(imagePattern, rec.getString("large_cover_url")));
        }
    }

    public RecordSet getEmployeeInfos(long viewerId, long[] userIds) throws AvroRemoteException {
        final String[] loginEmailCols = new String[]{"login_email1", "login_email2", "login_email3"};

        if (ArrayUtils.isEmpty(userIds))
            return new RecordSet();

        RecordSet userRecs = platform.getUsers(Long.toString(viewerId), StringUtils2.join(userIds, ","), Platform.USER_FULL_COLUMNS, false);
        if (CollectionUtils.isEmpty(userRecs))
            return new RecordSet();

        for (Record userRec : userRecs) {
            final long userId = userRec.getInt("user_id");
            long[] depIds = platform.useRawGroup(new Platform.GroupAccessor<long[]>() {
                @Override
                public long[] access(Group g) throws AvroRemoteException {
                    String s = ObjectUtils.toString(g.findGroupIdsByMember(Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END, userId));
                    return StringUtils2.splitIntArray(s, ",");
                }
            });
            RecordSet employeeRecs = new RecordSet();
            Map<Long, Long> depId2CompanyIdMap = findCompanyIds(depIds);
            for (long companyId : depId2CompanyIdMap.values()) {
                Record employeeRec = null;
                for (String col : loginEmailCols) {
                    String loginEmail = userRec.getString(col);
                    if (StringUtils.isNotBlank(loginEmail)) {
                        employeeRec = getEmployee(viewerId, companyId, loginEmail);
                        if (MapUtils.isNotEmpty(employeeRec))
                            break;
                    }
                }
                if (employeeRec != null) {
                    RecordSet companyRecs = getCompanies(viewerId, companyId);
                    if (CollectionUtils.isNotEmpty(companyRecs)) {
                        employeeRec.put("company", companyRecs.get(0));
                    } else {
                        employeeRec = null;
                    }
                }
                if (employeeRec != null)
                    employeeRecs.add(employeeRec);
            }
            if (CollectionUtils.isNotEmpty(employeeRecs))
                userRec.put("employee_info", employeeRecs);
        }
        return userRecs;
    }

    private static String getCellText(Cell c) {
        return ObjectUtils.toString(c != null ? c.getContents() : "");
    }
    private static final String excelDir = FilenameUtils.concat(FileUtils.getTempDirectoryPath(), "uploaded_employee_list_excel");
    private static RecordSet loadExcelFile(FileItem fi) {
        //"name": "员工名称",
        //"employee_id":"工号"
        //"email":"员工email",
        //"tel":"员工电话",
        //"mobile_tel":"员工手机",
        //"department":"员工所属部门",
        //"job_title":"员工头衔",

        final int NAME_INDEX = 0;
        final int EMPLOYEE_ID_INDEX = 1;
        final int EMAIL_INDEX = 2;
        final int TEL_INDEX = 3;
        final int MOBILE_TEL_INDEX = 4;
        final int DEPARTMENT_INDEX = 5;
        final int JOB_TITLE_INDEX = 6;


        RecordSet recs = new RecordSet();
        try {
            Workbook wb = Workbook.getWorkbook(fi.getInputStream());
            Sheet sheet = wb.getSheet(0);
            int rows = sheet.getRows();

            for (int i = 1; i < rows; i++) { // 从1开始，忽略title行
                Cell[] cells = sheet.getRow(i);
                Cell c;
                Record rec = new Record();
                rec.put("name", cells.length > NAME_INDEX ? getCellText(cells[NAME_INDEX]) : "");
                rec.put("employee_id", cells.length > EMPLOYEE_ID_INDEX ? getCellText(cells[EMPLOYEE_ID_INDEX]) : "");
                rec.put("email", cells.length > EMAIL_INDEX ? getCellText(cells[EMAIL_INDEX]) : "");
                rec.put("tel", cells.length > TEL_INDEX ? getCellText(cells[TEL_INDEX]) : "");
                rec.put("mobile_tel", cells.length > MOBILE_TEL_INDEX ? getCellText(cells[MOBILE_TEL_INDEX]) : "");
                rec.put("department", cells.length > DEPARTMENT_INDEX ? getCellText(cells[DEPARTMENT_INDEX]) : "");
                rec.put("job_title", cells.length > JOB_TITLE_INDEX ? getCellText(cells[JOB_TITLE_INDEX]) : "");
                recs.add(rec);
            }
        } catch (Exception e) {
            recs.clear();
        } finally {
        }

        return recs;
    }

    private static class Sort {
        public String orderBy;
        public final String ascOrDesc;

        private Sort(String orderBy, String ascOrDesc) {
            this.orderBy = orderBy;
            this.ascOrDesc = ascOrDesc;
        }

        public static Sort parse(String sort) {
            if (sort.startsWith("-")) {
                String orderBy = StringUtils.removeStart(sort, "-");
                return new Sort(StringUtils.lowerCase(orderBy), "DESC");
            } else {
                String orderBy = StringUtils.removeStart(sort, "+");
                return new Sort(StringUtils.lowerCase(orderBy), "ASC");
            }
        }
    }


    public RecordSet listEmployees(long viewerId, long companyId, String sort, int page, int count) throws AvroRemoteException {
        Sort sort1 = Sort.parse(sort);
        if (!ArrayUtils.contains(new String[]{"name", "email", "tel", "mobile_tel", "department", "employee_id"}, sort1.orderBy))
            sort1.orderBy = "name";

        if (!companyExists(companyId))
            return new RecordSet();

        if (page < 0)
            page = 0;
        if (count <= 0)
            count = 20;

        String sql = new SQLBuilder.Select()
                .select("name", "employee_id", "email", "tel", "mobile_tel", "department", "job_title")
                .from(employeeListTable)
                .where("company_id=${v(company_id)}", "company_id", companyId)
                .orderBy(sort1.orderBy, sort1.ascOrDesc)
                .page(page, count)
                .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        attachUserInfo(viewerId, companyId, recs);
        return recs;
    }

    private Record getEmployee(long viewerId, long companyId, String email) throws AvroRemoteException {
        if (!companyExists(companyId))
            return null;

        String sql = new SQLBuilder.Select()
                .select("name", "employee_id", "email", "tel", "mobile_tel", "department", "job_title")
                .from(employeeListTable)
                .where("company_id=${v(company_id)} AND email=${v(email)}", "company_id", companyId, "email", email)
                .toString();

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return MapUtils.isEmpty(rec) ? null : rec;
    }

    public Record addEmployee(long viewerId, long companyId, String name, String email, Record others) throws AvroRemoteException {
        checkAdmin(viewerId, companyId);
        String sql = new SQLBuilder.Insert()
                .insertIgnoreInto(employeeListTable)
                .values(new Record()
                        .set("company_id", companyId)
                        .set("employee_id", others.getString("employee_id", ""))
                        .set("email", ObjectUtils.toString(email))
                        .set("name", ObjectUtils.toString(name))
                        .set("tel", others.getString("tel", ""))
                        .set("mobile_tel", others.getString("mobile_tel", ""))
                        .set("department", others.getString("department", ""))
                        .set("job_title", others.getString("job_title", ""))
                        .set("comment", others.getString("comment", ""))
                )
                .toString();

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        if (n == 0)
            throw new ServerException(ErrorCode.EMPLOYEE_ERROR);

        addUsersByEmail(email);

        Record rec = getEmployee(viewerId, companyId, email);
        if (rec == null)
            throw new ServerException(ErrorCode.EMPLOYEE_ERROR);
        return rec;
    }

    public Record updateEmployee(long viewerId, long companyId, String email, Record info) throws AvroRemoteException {
        checkAdmin(viewerId, companyId);
        if (MapUtils.isNotEmpty(info)) {
            Record m = new Record();
            String sql = new SQLBuilder.Update()
                    .update(employeeListTable)
                    .values(info)
                    .where("company_id=${v(company_id)} AND email=${v(email)}", "company_id", companyId, "email", ObjectUtils.toString(email))
                    .toString();
            SQLExecutor se = getSqlExecutor();
            se.executeUpdate(sql);
        }
        Record rec = getEmployee(viewerId, companyId, email);
        if (rec == null)
            throw new ServerException(ErrorCode.EMPLOYEE_ERROR);
        return rec;
    }


    public boolean deleteEmployees(long viewerId, long companyId, String... emails) throws AvroRemoteException {
        checkAdmin(viewerId, companyId);
        if (ArrayUtils.isEmpty(emails))
            return true;

        String sql = new SQLBuilder.Delete()
                .deleteFrom(employeeListTable)
                .where("company_id=${v(company_id)} AND email IN (${vjoin(emails)})",
                        "company_id", companyId, "emails", Arrays.asList(emails))
                .toString();

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }


    public RecordSet searchEmployee(long viewerId, long companyId, String kw, String sort, int count) throws AvroRemoteException {
        if (StringUtils.isBlank(kw) || !companyExists(companyId))
            return new RecordSet();

        Sort sort1 = Sort.parse(sort);
        if (!ArrayUtils.contains(new String[]{"name", "email", "tel", "mobile_tel", "department"}, sort1.orderBy))
            sort1.orderBy = "name";

        if (count <= 1)
            count = 100;

        kw = kw.trim();
        String sql = new SQLBuilder.Select()
                .select("name", "employee_id", "email", "tel", "mobile_tel", "department", "job_title")
                .from(employeeListTable)
                .where("company_id=${v(company_id)} AND (`name`=${v(kw)} OR email=${v(kw)} OR department=${v(kw)} OR job_title=${v(kw)})", "company_id", companyId, "kw", kw)
                .orderBy(sort1.orderBy, sort1.ascOrDesc)
                .page(0, count)
                .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        attachUserInfo(viewerId, companyId, recs);
        return recs;
    }

    private void attachUserInfo(long viewerId, long companyId, RecordSet employeeRecs) throws AvroRemoteException {
        RecordSet userRecs = getCompanyUsers(viewerId, companyId, Platform.USER_LIGHT_COLUMNS, 0, 100000);
        HashMap<String, Record> emailToUser = new HashMap<String, Record>();
        for (Record userRec : userRecs) {
            String email = userRec.getString("login_email1");
            if (StringUtils.isNotEmpty(email))
                emailToUser.put(email, userRec);

            email = userRec.getString("login_email2");
            if (StringUtils.isNotEmpty(email))
                emailToUser.put(email, userRec);

            email = userRec.getString("login_email3");
            if (StringUtils.isNotEmpty(email))
                emailToUser.put(email, userRec);
        }

        for (Record employeeRec : employeeRecs) {
            String email = employeeRec.getString("email");
            Record userRec = emailToUser.get(email);
            if (userRec != null) {
                employeeRec.put("user", new Record()
                        .set("user_id", userRec.getInt("user_id"))
                        .set("display_name", userRec.getString("display_name"))
                        .set("small_image_url", userRec.getString("small_image_url"))
                        .set("image_url", userRec.getString("image_url"))
                        .set("large_image_url", userRec.getString("large_image_url"))
                );
            } else {
                employeeRec.put("user", new Record()
                        .set("user_id", 0L)
                        .set("display_name", "")
                        .set("small_image_url", "")
                        .set("image_url", "")
                        .set("large_image_url", "")
                );
            }
        }
    }

    public RecordSet getCompanyUsers(long viewerId, long companyId, String cols, int page, int count) throws AvroRemoteException {
        long depId = findDepartmentId(companyId);
        if (depId <= 0)
            return new RecordSet();

        if (page < 0)
            page = 0;
        if (count <= 0)
            count = 20;


        long[] memberIds = StringUtils2.splitIntArray(platform.getGroupMembers(depId), ",");
        memberIds = PageUtils.page(memberIds, page, count);
        return platform.getUsers(Long.toString(viewerId), StringUtils2.join(memberIds, ","), cols, false);
    }

    public void createDepartmentCircleByEmployeeList(long viewerId, long companyId) throws AvroRemoteException {
        HashMap<String, Set<Long>> m = new HashMap<String, Set<Long>>();
        RecordSet employeeRecs = listEmployees(viewerId, companyId, "name", 0, 100000);
        for (Record rec : employeeRecs) {
            String depName = rec.getString("department", null);
            String email = rec.getString("email", null);
            Record userRec = (Record) rec.get("user");
            long userId = 0L;
            if (userRec != null) {
                userId = userRec.getInt("user_id", 0L);
            }

            if (StringUtils.isBlank(depName) || StringUtils.isBlank(email) || userId <= 0)
                continue;

            Set<Long> userIds = m.get(depName);
            if (userIds == null) {
                userIds = new HashSet<Long>();
                m.put(depName,  userIds);
            }

            userIds.add(userId);
        }

        HashSet<Long> createdDepIds = new HashSet<Long>();
        boolean allSuccess = true;
        for (Map.Entry<String, Set<Long>> e : m.entrySet()) {
            String depName = e.getKey();
            Set<Long> memberIds = e.getValue();
            long depId = 0;
            try {
                depId = platform.createGroup(
                        Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.TYPE_PUBLIC_CIRCLE,
                        ObjectUtils.toString(depName), // name
                        100000, // limit
                        1,      // stream public
                        0,      // search
                        1,      // view members
                        2,      // join
                        0,      // member invite
                        0,      // member approve
                        1,      // member post
                        0,      // member quit
                        0,      // need invite confirm
                        memberIds.iterator().next(),
                        "",
                        new Record().set(Constants.DEP_COL_COMPANY, Long.toString(companyId))
                                .set(Constants.DEP_COL_IS_DEP, "1"),
                        Long.toString(viewerId),
                        "",
                        "",
                        Integer.toString(0),
                        false);
                createdDepIds.add(depId);
            } catch (Exception ignored) {
                allSuccess = false;
            }
        }

        if (!allSuccess) {
            for (final long depId : createdDepIds) {
                platform.useRawGroup(new Platform.GroupAccessor<java.lang.Object>() {
                    @Override
                    public Object access(Group g) throws AvroRemoteException {
                        g.destroyGroup(Long.toString(depId));
                        return null;
                    }
                });
            }
        }
    }

    private void addUsersByEmail(String email) {
//        if (StringUtils.isBlank(email))
//            return;
//
//        if (!StringUtils.contains(email, "@"))
//            return;
//
//        RecordSet recs = platform.getUserIds(email);
//        if (CollectionUtils.isNotEmpty(recs)) {
//            Record rec = recs.get(0);
//            if (StringUtils.equals(rec.getString("login_name"), email)) {
//
//            }
//        }
    }


    private static Record dummyCompany() {
        /*
        [{

 "name":"borqs", // 公司名称

"address":"beijing", // 公司地址

 "email":"xxx @ borqs.com", // 公司对外联系邮件

 "website": "http: //www.borqs.com", // 公司网站

 "tel": "13800", // 公司电话

 "fax": "xx", // 公司传真

 "logo_url":"xxx", // 公司logo url

 "description":"xx", // 公司简介

"employee_circle_id", // 如果登录用户属于此公司，则返回这个公司所属的全员circle id，否则为值为0

}]

         */
        return new Record()
                .set("name", "borqs")
                .set("address", "beijing")
                .set("email", "xx@borqs.com")
                .set("website", "http://www.borqs.com")
                .set("tel", "13900")
                .set("fax", "14000")
                .set("logo_url", "http://www.borqs.com/img/ilogo.jpg")
                .set("description", "领衔新兴科技企业，播思入选德勤高科技、高成长中国50强")
                .set("department_id", 0L);
    }

    private static Record dummyEmployee1() {
        /*
        [{

"name": "员工名称",

"employee_id":"工号"

"email":"员工email",

"tel":"员工电话",

"mobile_tel":"员工手机",

"department":"员工所属部门",

"job_title":"员工头衔",

"user": {

"user_id":"员工在wutong内的id",                   // 如果此值为0，则说明此员工尚未加入到wutong中，display_name和image_url都为空

"display_name":"员工在wutong内的现实名称",

"image_url":"员工在梧桐内的头像url"

}

}]


         */
        return new Record()
                .set("name", "乔不死")
                .set("employee_id", "b890")
                .set("email", "busi.qiao@borqs.com")
                .set("tel", "100000")
                .set("mobile_tel", "120000")
                .set("department", "天堂事业部")
                .set("job_title", "天堂事业部经理兼唯一员工")
                .set("user", new Record()
                        .set("user_id", 10000)
                        .set("display_name", "乔不死")
                        .set("image_url", "http://e.hiphotos.baidu.com/baike/s%3D220/sign=bd191c66b31bb0518b24b42a067bda77/b3fb43166d224f4a34ca635709f790529822d126.jpg"));
    }

    private static Record dummyEmployee2() {
        return new Record()
                .set("name", "比盖茨")
                .set("employee_id", "b891")
                .set("email", "gaici.bi@borqs.com")
                .set("tel", "100000")
                .set("mobile_tel", "120000")
                .set("department", "人间事业部")
                .set("job_title", "人间事业部大唐经理")
                .set("user", new Record()
                        .set("user_id", 0)
                        .set("display_name", "")
                        .set("image_url", ""));
    }
}
