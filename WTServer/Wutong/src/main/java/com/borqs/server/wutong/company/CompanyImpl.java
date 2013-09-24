package com.borqs.server.wutong.company;

import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.log.TraceCall;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.*;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.group.GroupLogic;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class CompanyImpl implements CompanyLogic {

    private static final Logger L = Logger.getLogger(CompanyImpl.class);

    private ConnectionFactory connectionFactory;
    private String db;
    private String companyTable;
    private String companyEmailDomainBlacklistTable;
    private String employeeListTable;
    private String companyImagePattern;

    public CompanyImpl() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("employeeList.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("platform.simple.db", null);
        this.companyTable = conf.getString("company.simple.companyTable", "company");
        this.companyEmailDomainBlacklistTable = conf.getString("company.simple.companyEmailDomainBlacklistTable", "company_email_domain_blacklist");
        this.employeeListTable = conf.getString("company.simple.employeeListTable", "employee_list");
        this.companyImagePattern = conf.getString("company.imageUrlPattern", "http://oss.aliyuncs.com/wutong-data/media/photo/%s");
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
                .select("company_id", "name", "name_en", "address", "address_en", "email", "website", "tel", "fax", "logo_url", "small_logo_url", "large_logo_url", "cover_url", "small_cover_url", "large_cover_url", "description", "department_id")
                .from(companyTable)
                .where("destroyed_time=0 AND (email_domain1=${v(ed)} OR email_domain2=${v(ed)} OR email_domain3=${v(ed)} OR email_domain4=${v(ed)})", "ed", ObjectUtils.toString(emailDomain))
                .toString();
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return MapUtils.isEmpty(rec) ? null : rec;
    }

    @TraceCall
    @Override
    public void joinCompanyOrCreateCompany(Context ctx, String email) {
        if (!isCompanyEmail(email))
            return;

        String emailDomain = getEmailDomain(email);
        Record companyRec = findCompanyByEmailDomain(emailDomain);
        if (MapUtils.isEmpty(companyRec)) {
            String companyName = StringUtils.substringBeforeLast(emailDomain, ".");
            createCompany(ctx, companyName, emailDomain);
        } else {
            final long depId = findDepartmentId(companyRec.checkGetInt("company_id"));
            GroupLogic g = GlobalLogics.getGroup();
            g.addSimpleMember(ctx, depId, ctx.getViewerId(), Constants.ROLE_MEMBER);
        }
    }

    @TraceCall
    @Override
    public Record createCompany(Context ctx, String name, String emailDomain) {
        long viewerId = ctx.getViewerId();
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
                        .set("address", "")
                        .set("address_en", "")
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
            depId = GlobalLogics.getGroup().createGroup(ctx,
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
                            .set(Constants.DEP_COL_IS_ROOT, "1")
                    );
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

        RecordSet companyRecs = getCompanies(ctx, companyId);
        return CollectionUtils.isNotEmpty(companyRecs) ? companyRecs.get(0) : null;
    }

    @TraceCall
    @Override
    public boolean destroyCompany(Context ctx, long companyId) {
        long depId = findDepartmentId(companyId);
        if (depId > 0) {
            GlobalLogics.getGroup().destroyGroup(ctx, Long.toString(depId));
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

    @TraceCall
    @Override
    public Record updateCompany(Context ctx, long companyId, Record info){
        long viewerId = ctx.getViewerId();
        checkAdmin(ctx, companyId);



        String sql = new SQLBuilder.Update()
                .update(companyTable)
                .valueIf(info.has("email_domain1"), "email_domain1", info.getString("email_domain1"))
                .valueIf(info.has("email_domain2"), "email_domain2", info.getString("email_domain2"))
                .valueIf(info.has("email_domain3"), "email_domain3", info.getString("email_domain3"))
                .valueIf(info.has("email_domain4"), "email_domain4", info.getString("email_domain4"))
                .valueIf(info.has("name"), "name", info.getString("name"))
                .valueIf(info.has("name_en"), "name_en", info.getString("name_en"))
                .valueIf(info.has("address"), "address", info.getString("address"))
                .valueIf(info.has("address_en"), "address_en", info.getString("address_en"))
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
        RecordSet recs = getCompanies(ctx, companyId);
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

    @TraceCall
    @Override
    public Record createCompanyFromGroup(Context ctx, String name, String emailDomain, final long groupId) {
        GroupLogic g = GlobalLogics.getGroup();
        long creatorId = g.getCreator(ctx, groupId);
        if (creatorId == 0L)
            return null;

        final LinkedHashSet<Long> userIds = new LinkedHashSet<Long>();
        userIds.add(creatorId);

        Record companyRec = createCompany(ctx, name, emailDomain);
        long companyId = companyRec.checkGetInt("company_id");
        final long depId = companyRec.checkGetInt("department_id");

        String adminIds = ObjectUtils.toString(g.getMembersByRole(ctx, groupId, Constants.ROLE_ADMIN, -1, -1, ""));
        String memberIds = ObjectUtils.toString(g.getMembersByRole(ctx, groupId, Constants.ROLE_MEMBER, -1, -1, ""));
        Record roles = new Record();
        for (String s : StringUtils2.splitSet(adminIds, ",", true)) {
            roles.put(s, Constants.ROLE_ADMIN);
            userIds.add(Long.parseLong(s));
        }
        for (String s : StringUtils2.splitSet(memberIds, ",", true)) {
            roles.put(s, Constants.ROLE_MEMBER);
            userIds.add(Long.parseLong(s));
        }
        g.addSimpleMembers(ctx, depId, roles);


        RecordSet recs = getCompanies(ctx, companyId);
        return CollectionUtils.isNotEmpty(recs) ? recs.get(0) : null;
    }

    private void renameCircles(RecordSet recs) {
        recs.renameColumn("id", "circle_id");
        recs.renameColumn("name", "circle_name");
    }

    @TraceCall
    @Override
    public RecordSet getCompanies(Context ctx, long... companyIds) {
        long viewerId = ctx.getViewerId();
        if (ArrayUtils.isEmpty(companyIds))
            return new RecordSet();

        String sql = new SQLBuilder.Select()
                .select("company_id", "created_time", "updated_time", "email_domain1", "email_domain2", "email_domain3", "email_domain4", "name", "name_en", "address", "address_en", "email", "website", "tel", "fax", "zip_code", "logo_url", "small_logo_url", "large_logo_url", "cover_url", "small_cover_url", "large_cover_url", "description", "department_id")
                .from(companyTable)
                .where("destroyed_time=0 AND company_id IN (${company_ids})", "company_ids", StringUtils2.join(companyIds, ","))
                .orderBy("company_id", "ASC")
                .toString();
        SQLExecutor se = getSqlExecutor();
        RecordSet companyRecs =  se.executeRecordSet(sql, null);
        for (Record companyRec : companyRecs) {
            addCompanyImagePrefix(companyImagePattern, companyRec);
        }
        expandCompanyInfo(ctx, companyRecs, 5);
        return companyRecs;
    }

    @TraceCall
    @Override
    public RecordSet getCompanyDepCircles(Context ctx, final long companyId) {
        long viewerId = ctx.getViewerId();
        // sub_department
        GroupLogic g = GlobalLogics.getGroup();

        String subIds = g.findGroupIdsByProperty(ctx, "company", Long.toString(companyId), 0);
        RecordSet recs = g.getGroups(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END, Long.toString(viewerId), subIds, "id,name,small_image_url,large_image_url,image_url,company,is_department,is_company_root", false);
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

    private void expandCompanyInfo(Context ctx, RecordSet companyRecs, final int maxSubDepCount) {
        long viewerId = ctx.getViewerId();
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
            GroupLogic g = GlobalLogics.getGroup();
            String userIds = ObjectUtils.toString(g.getAllMembers(ctx, depId, 0, 5, ""));
            RecordSet userRecs = GlobalLogics.getAccount().getUsers(ctx, Long.toString(viewerId), userIds, "user_id, display_name, small_image_url, image_url, large_image_url", false);
            rec.put("employee_count", employeeCount);
            rec.put("some_members", userRecs);

            // sub_department
            String subIds = ObjectUtils.toString(g.findGroupIdsByProperty(ctx, "company", Long.toString(companyId), 0));


            RecordSet subCircles = g.getGroups(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END, Long.toString(viewerId), subIds, "id,name,small_image_url,large_image_url,image_url,company,is_department,is_company_root", false);
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

    @TraceCall
    @Override
    public RecordSet searchCompanies(Context ctx, String kw) {
        long viewerId = ctx.getViewerId();
        if (StringUtils.isBlank(kw))
            return new RecordSet();

        String sql = new SQLBuilder.Select()
                .select("company_id", "created_time", "updated_time", "email_domain1", "email_domain2", "email_domain3", "email_domain4", "name", "name_en", "address", "address_en", "email", "website", "tel", "fax", "zip_code", "small_logo_url", "logo_url", "large_logo_url", "description", "department_id")
                .from(companyTable)
                .where("destroyed_time=0 AND name like ${v(kw)}", "kw", "%" + kw + "%")
                .orderBy("company_id", "ASC")
                .toString();

        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql, null);
    }

    private Map<Long, Integer> getDepartmentRoles(Context ctx, final long userId) {
        long viewerId = ctx.getViewerId();
        final LinkedHashMap<Long, Integer> m = new LinkedHashMap<Long, Integer>();
        GroupLogic g = GlobalLogics.getGroup();

        String s = ObjectUtils.toString(g.findGroupIdsByMember(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END, userId));
        long[] depIds = StringUtils2.splitIntArray(s, ",");
        for (long depId : depIds) {
            int role = Constants.ROLE_GUEST;
            if (g.hasRight(ctx, depId, userId, Constants.ROLE_CREATOR)) {
                role = Constants.ROLE_CREATOR;
            } else if (g.hasRight(ctx, depId, userId, Constants.ROLE_ADMIN)) {
                role = Constants.ROLE_ADMIN;
            } else if (g.hasRight(ctx, depId, userId, Constants.ROLE_MEMBER)) {
                role = Constants.ROLE_MEMBER;
            }
            m.put(depId, role);
        }

        return m;
    }

    @TraceCall
    @Override
    public RecordSet belongsCompanies(Context ctx, final long userId) {
//        final ArrayList<Long> depIds = new ArrayList<Long>();
//        platform.useRawGroup(new Platform.GroupAccessor<java.lang.Object>() {
//            @Override
//            public Object access(Group g) {
//                String s = ObjectUtils.toString(g.findGroupIdsByMember(Constants.DEPARTMENT_ID_BEGIN, Constants.DEPARTMENT_ID_END, userId));
//                depIds.addAll(StringUtils2.splitIntList(s, ","));
//                return null;
//            }
//        });
        long viewerId = ctx.getViewerId();

        Map<Long, Integer> depId2RoleMap = getDepartmentRoles(ctx, userId);
        ArrayList<Long> depIds = new ArrayList<Long>(depId2RoleMap.keySet());
        if (depIds.isEmpty())
            return new RecordSet();

        Map<Long, Long> depId2CompanyIdMap = findCompanyIds(ArrayUtils.toPrimitive(depIds.toArray(new Long[depIds.size()])));
        long[] companyIds = CollectionUtils2.toLongArray(depId2CompanyIdMap.values());
        RecordSet recs = getCompanies(ctx, companyIds);
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

    @TraceCall
    @Override
    public boolean grant(Context ctx, long companyId, Map<Long, Integer> roles) {
        long depId = findDepartmentId(companyId);
        if (depId <= 0)
            return false;

        Record roles1 = new Record();
        for (Map.Entry<Long, Integer> e : roles.entrySet()) {
            roles1.put(Long.toString(e.getKey()), e.getValue());
        }
        return GlobalLogics.getGroup().grants(ctx, depId, roles1);
    }

    @TraceCall
    @Override
    public boolean companyExists(long companyId) {
        String sql = new SQLBuilder.Select()
                .select("COUNT(*)")
                .from(companyTable)
                .where("destroyed_time=0 AND company_id = ${v(company_id)}", "company_id", companyId)
                .toString();
        SQLExecutor se = getSqlExecutor();
        long n = se.executeIntScalar(sql, 0L);
        return n > 0;
    }

    private boolean isAdmin(Context ctx, final long companyId) {
        final long viewerId = ctx.getViewerId();
        final long depId = findDepartmentId(companyId);
        if (depId <= 0)
            return false;

        GroupLogic g = GlobalLogics.getGroup();
        return g.hasRight(ctx, depId, viewerId, Constants.ROLE_ADMIN);
    }

    public void checkAdmin(Context ctx, long companyId) {
        if (!isAdmin(ctx, companyId))
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR);
    }

    @TraceCall
    @Override
    public RecordSet uploadEmployees(Context ctx, long companyId, FileItem excelFile, boolean merge) {
        long viewerId = ctx.getViewerId();
        checkAdmin(ctx, companyId);
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
                            .set("name_en", rec.getString("name_en", ""))
                            .set("tel", rec.getString("tel", ""))
                            .set("mobile_tel", rec.getString("mobile_tel", ""))
                            .set("department", rec.getString("department", ""))
                            .set("job_title", rec.getString("job_title", ""))
                            .set("job_title_en", rec.getString("job_title_en", ""))
                            .set("comment", rec.getString("comment", ""))
                    )
                    .toString();
            sqls.add(sql);
        }
        se.executeUpdate(sqls);
        for (Record rec : l) {
            addUsersByEmail(rec.getString("email"));
        }
        return listEmployees(ctx, companyId, EmployeeListConstants.COL_NAME, 0, 1000);
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

    @TraceCall
    @Override
    public RecordSet getEmployeeInfos(Context ctx, long[] userIds) {
        final String[] loginEmailCols = new String[]{"login_email1", "login_email2", "login_email3"};

        long viewerId = ctx.getViewerId();

        if (ArrayUtils.isEmpty(userIds))
            return new RecordSet();

        GroupLogic g = GlobalLogics.getGroup();

        RecordSet userRecs = GlobalLogics.getAccount().getUsers(ctx, ctx.getViewerIdString(), StringUtils2.join(userIds, ","), AccountLogic.USER_FULL_COLUMNS, false);
        if (CollectionUtils.isEmpty(userRecs))
            return new RecordSet();

        for (Record userRec : userRecs) {
            final long userId = userRec.getInt("user_id");
            String s = ObjectUtils.toString(g.findGroupIdsByMember(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END, userId));
            long[] depIds = StringUtils2.splitIntArray(s, ",");

            RecordSet employeeRecs = new RecordSet();
            Map<Long, Long> depId2CompanyIdMap = findCompanyIds(depIds);
            for (long companyId : depId2CompanyIdMap.values()) {
                Record employeeRec = null;
                for (String col : loginEmailCols) {
                    String loginEmail = userRec.getString(col);
                    if (StringUtils.isNotBlank(loginEmail)) {
                        employeeRec = getEmployee(ctx, companyId, loginEmail);
                        if (MapUtils.isNotEmpty(employeeRec))
                            break;
                    }
                }
                if (employeeRec != null) {
                    RecordSet companyRecs = getCompanies(ctx, companyId);
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
        final int NAME_EN_INDEX = 7;


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
                rec.put("name_en", cells.length > NAME_EN_INDEX ? getCellText(cells[NAME_EN_INDEX]) : "");
                recs.add(rec);
            }
        } catch (Exception e) {
            recs.clear();
        } finally {
        }

        return recs;
    }




    @TraceCall
    @Override
    public RecordSet listEmployees(Context ctx, long companyId, String sort, int page, int count) {
        long viewerId = ctx.getViewerId();

        Sort sort1 = Sort.parse(sort);
        if (!ArrayUtils.contains(new String[]{"name", "name_en", "email", "tel", "mobile_tel", "department", "employee_id"}, sort1.orderBy))
            sort1.orderBy = "name";

        if (!companyExists(companyId))
            return new RecordSet();

        if (page < 0)
            page = 0;
        if (count <= 0)
            count = 20;

        String sql = new SQLBuilder.Select()
                .select("name", "name_en", "employee_id", "email", "tel", "mobile_tel", "department", "job_title")
                .from(employeeListTable)
                .where("company_id=${v(company_id)}", "company_id", companyId)
                .orderBy(sort1.orderBy, sort1.ascOrDesc)
                .page(page, count)
                .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        attachUserInfo(ctx, companyId, recs);
        return recs;
    }

    private Record getEmployee(Context ctx, long companyId, String email) {
        if (!companyExists(companyId))
            return null;

        String sql = new SQLBuilder.Select()
                .select("name", "name_en", "employee_id", "email", "tel", "mobile_tel", "department", "job_title")
                .from(employeeListTable)
                .where("company_id=${v(company_id)} AND email=${v(email)}", "company_id", companyId, "email", email)
                .toString();

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return MapUtils.isEmpty(rec) ? null : rec;
    }

    public static String makeEmployeeSK(String name, String email, String tel, String mobileTel, String department) {
        StringBuilder buff = new StringBuilder();
        buff.append(ObjectUtils.toString(name)).append(" ");
        buff.append(PinyinUtils.toFullPinyin(name, " ")).append(" ");
        buff.append(PinyinUtils.toShortPinyin(name)).append(" ");
        buff.append(ObjectUtils.toString(email)).append(" ");
        buff.append(ObjectUtils.toString(tel)).append(" ");
        buff.append(ObjectUtils.toString(mobileTel)).append(" ");
        buff.append(ObjectUtils.toString(department));
        return buff.toString();
    }

    @TraceCall
    @Override
    public Record addEmployee(Context ctx, long companyId, String name, String email, Record others) {
        long viewerId = ctx.getViewerId();
        checkAdmin(ctx, companyId);
        String sql = new SQLBuilder.Insert()
                .insertIgnoreInto(employeeListTable)
                .values(new Record()
                        .set("company_id", companyId)
                        .set("employee_id", others.getString("employee_id", ""))
                        .set("email", ObjectUtils.toString(email))
                        .set("name", ObjectUtils.toString(name))
                        .set("name_en", others.getString("name_en", ""))
                        .set("tel", others.getString("tel", ""))
                        .set("mobile_tel", others.getString("mobile_tel", ""))
                        .set("department", others.getString("department", ""))
                        .set("job_title", others.getString("job_title", ""))
                        .set("comment", others.getString("comment", ""))
                        .set("sk", makeEmployeeSK(
                                ObjectUtils.toString(name),
                                ObjectUtils.toString(email),
                                others.getString("tel", ""),
                                others.getString("mobile_tel", ""),
                                others.getString("department", "")
                        ))
                )
                .toString();

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        if (n == 0)
            throw new ServerException(WutongErrors.COMPANY_EMPLOYEE_ERROR);

        addUsersByEmail(email);

        Record rec = getEmployee(ctx, companyId, email);
        if (rec == null)
            throw new ServerException(WutongErrors.COMPANY_EMPLOYEE_ERROR);
        return rec;
    }

    @TraceCall
    @Override
    public Record updateEmployee(Context ctx, long companyId, String email, Record info) {
        checkAdmin(ctx, companyId);

        if (MapUtils.isNotEmpty(info)) {
            info.put("sk", makeEmployeeSK(
                    info.getString("name"),
                    info.getString("email"),
                    info.getString("tel", ""),
                    info.getString("mobile_tel", ""),
                    info.getString("department", "")
            ));

            //Record m = new Record();
            String sql = new SQLBuilder.Update()
                    .update(employeeListTable)
                    .values(info)
                    .where("company_id=${v(company_id)} AND email=${v(email)}", "company_id", companyId, "email", ObjectUtils.toString(email))
                    .toString();
            SQLExecutor se = getSqlExecutor();
            se.executeUpdate(sql);
            info.remove("sk");
        }
        Record rec = getEmployee(ctx, companyId, email);
        if (rec == null)
            throw new ServerException(WutongErrors.COMPANY_EMPLOYEE_ERROR);
        return rec;
    }


    @TraceCall
    @Override
    public boolean deleteEmployees(Context ctx, long companyId, String... emails) {
        checkAdmin(ctx, companyId);
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


    @TraceCall
    @Override
    public RecordSet searchEmployee(Context ctx, long companyId, String kw, String sort, int count) {
        if (StringUtils.isBlank(kw) || !companyExists(companyId))
            return new RecordSet();

        Sort sort1 = Sort.parse(sort);
        if (!ArrayUtils.contains(new String[]{"name", "name_en", "email", "tel", "mobile_tel", "department"}, sort1.orderBy))
            sort1.orderBy = "name";

        if (count <= 1)
            count = 100;

        kw = kw.trim();
        String sql = new SQLBuilder.Select()
                .select("name", "name_en", "employee_id", "email", "tel", "mobile_tel", "department", "job_title")
                .from(employeeListTable)
                //.where("company_id=${v(company_id)} AND (`name`=${v(kw)} OR email=${v(kw)} OR department=${v(kw)} OR job_title=${v(kw)})", "company_id", companyId, "kw", kw)
                .where("company_id=${v(company_id)} AND sk LIKE ${v(kw)}", "company_id", companyId, "kw", "%" + StringUtils.trimToEmpty(kw) + "%")
                .orderBy(sort1.orderBy, sort1.ascOrDesc)
                .page(0, count)
                .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        attachUserInfo(ctx, companyId, recs);
        return recs;
    }

    private void attachUserInfo(Context ctx, long companyId, RecordSet employeeRecs) {

        RecordSet userRecs = getCompanyUsers(ctx, companyId, AccountLogic.USER_LIGHT_COLUMNS, 0, 100000);
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

    @TraceCall
    @Override
    public RecordSet getCompanyUsers(Context ctx, long companyId, String cols, int page, int count) {
        long depId = findDepartmentId(companyId);
        if (depId <= 0)
            return new RecordSet();

        if (page < 0)
            page = 0;
        if (count <= 0)
            count = 20;


        long[] memberIds = StringUtils2.splitIntArray(GlobalLogics.getGroup().getAllMembers(ctx, depId, -1, -1, ""), ",");
        memberIds = PageUtils.page(memberIds, page, count);
        return GlobalLogics.getAccount().getUsers(ctx, ctx.getViewerIdString(), StringUtils2.join(memberIds, ","), cols, false);
    }

    @TraceCall
    @Override
    public void createDepartmentCircleByEmployeeList(Context ctx, long companyId) {
        HashMap<String, Set<Long>> m = new HashMap<String, Set<Long>>();
        RecordSet employeeRecs = listEmployees(ctx, companyId, "name", 0, 100000);
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
                depId = GlobalLogics.getGroup().createGroup(ctx,
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
                                .set(Constants.DEP_COL_IS_DEP, "1")
                        );
                createdDepIds.add(depId);
            } catch (Exception ignored) {
                allSuccess = false;
            }
        }

        if (!allSuccess) {
            for (final long depId : createdDepIds) {
                GlobalLogics.getGroup().destroyGroup(ctx, Long.toString(depId));
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
