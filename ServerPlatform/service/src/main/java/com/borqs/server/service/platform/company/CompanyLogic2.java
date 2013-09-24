package com.borqs.server.service.platform.company;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.PageUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Group;
import com.borqs.server.service.platform.Platform;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class CompanyLogic2 implements Initializable {
    private final Platform platform;
    private ConnectionFactory connectionFactory;
    private String db;
    private String employeeListTable;

    public CompanyLogic2(Platform platform) {
        this.platform = platform;
    }

    @Override
    public void init() {
        Configuration conf = platform.getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("employeeList.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("platform.simple.db", null);
        this.employeeListTable = conf.getString("company.simple.employeeListTable", "employee_list");
    }

    @Override
    public void destroy() {
        this.employeeListTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    // company

    public Record createCompany(String name, long creatorId, long viewerId, String appId) throws AvroRemoteException {
        Platform p = platform;

        Record rootDepartmentProps = new Record();
        long rootDepartmentId = p.createGroup(
                Constants.DEPARTMENT_ID_BEGIN,
                Constants.TYPE_DEPARTMENT,
                Constants.ROOT_DEPARTMENT_NAME,
                1000000,
                1,
                0,
                1,
                0,
                0,
                0,
                1,
                0,
                0,
                creatorId,
                "",
                rootDepartmentProps,
                Long.toString(viewerId),
                "",
                "",
                appId);


        Record companyProps = new Record();
        companyProps.set(Constants.COMPANY_COL_ROOT_DEPARTMENT_ID, Long.toString(rootDepartmentId));
        long companyId = p.createGroup(
                Constants.COMPANY_ID_BEGIN,
                Constants.TYPE_COMPANY,
                name,
                1000000,
                1,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                creatorId,
                "",
                companyProps,
                Long.toString(viewerId),
                "",
                "",
                appId
        );

        RecordSet groups = p.getGroups(
                Constants.COMPANY_ID_BEGIN,
                Constants.COMPANY_ID_END,
                Long.toString(companyId),
                ""); // TODO: cols
        if (CollectionUtils.isEmpty(groups))
            throw new RuntimeException(); // TODO: exception

        Record groupRec = groups.get(0);
        return groupAsCompany(groupRec);
    }

    private Record groupAsCompany(Record group) {
        return group; // TODO: xx
    }

    public boolean destroyCompany(final long companyId) throws AvroRemoteException {
        if (!isValidCompanyId(companyId))
            return false;

        long[] allDepartmentIds = getAllDepartmentIds(companyId);
        final long[] companyAndDepartmentGroupIds = ArrayUtils.add(allDepartmentIds, companyId);

        platform.useRawGroup(new Platform.GroupAccessor<Object>() {
            @Override
            public Object access(Group g) throws AvroRemoteException {
                for (long depId : companyAndDepartmentGroupIds) {
                    g.destroyGroup(Long.toString(depId));
                }
                return null;
            }
        });
        return true;
    }

    public RecordSet getCompanies(long... companyIds) throws AvroRemoteException {
        if (ArrayUtils.isEmpty(companyIds))
            return new RecordSet();

        Platform p = platform;

        RecordSet recs = p.getGroups(
                Constants.COMPANY_ID_BEGIN,
                Constants.COMPANY_ID_END,
                StringUtils2.join(companyIds, ","),
                ""); // TODO: COLS
        RecordSet companyRecs = new RecordSet();
        for (Record rec : recs) {
            companyRecs.add(groupAsCompany(rec));
        }
        return companyRecs;
    }

    public Record getCompany(long companyId) throws AvroRemoteException {
        RecordSet recs = getCompanies(companyId);
        return CollectionUtils.isEmpty(recs) ? null : recs.get(0);
    }

    public Record findCompanyByDomain(String domain) {
        // TODO: xx
        return null;
    }


    // employee list

    public RecordSet getEmployeeList(long companyId) throws AvroRemoteException {
        Record companyRec = getCompanyGroup(companyId);
        if (companyRec == null)
            return new RecordSet();

        /*
        company_id bigint NOT NULL,
    email varchar(128) NOT NULL,
    `name` varchar(128) NOT NULL DEFAULT '',
    tel varchar(32) NOT NULL DEFAULT '',
    mobile_tel varchar(32) NOT NULL DEFAULT '',
    comment varchar(4096) NOT NULL DEFAULT '',
         */
        String sql = new SQLBuilder.Select()
                .select("name", "email", "tel", "mobile_tel", "comment")
                .from(employeeListTable)
                .where("company_id=${v(company_id)}", "company_id", companyId)
                .toString();

        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql, null);
    }

    public void updateEmployeeList(long companyId, RecordSet recs) {

    }

    public void addEmployeeToList(long companyId, Record... recs) {

    }

    public void deleteEmployeeInList(long companyId, String... emails) {

    }

    public void clearEmployeeList(long companyId) {
    }

    // ----------------------------------------

    private Record getCompanyGroup(long companyId) throws AvroRemoteException {
        if (isValidCompanyId(companyId))
            return null;

        RecordSet companyRecs = platform.getGroups(
                Constants.COMPANY_ID_BEGIN,
                Constants.COMPANY_ID_END,
                Long.toString(companyId),
                ""); // TODO: cols
        return CollectionUtils.isNotEmpty(companyRecs) ? companyRecs.get(0) : null;
    }


    // department

    private Record getDepartmentGroup(long depId) throws AvroRemoteException {
        if (isValidDepartmentId(depId))
            return null;

        RecordSet depRecs = platform.getGroups(
                Constants.DEPARTMENT_ID_BEGIN,
                Constants.DEPARTMENT_ID_END,
                Long.toString(depId),
                ""); // TODO: COLS
        return CollectionUtils.isNotEmpty(depRecs) ? depRecs.get(0) : null;
    }

    private long getRootDepartmentId(long companyId) throws AvroRemoteException {
        Record companyRec = getCompanyGroup(companyId);
        return companyRec != null ? companyRec.getInt(Constants.COMPANY_COL_ROOT_DEPARTMENT_ID) : 0L;
    }

    private Record getRootDepartment(long companyId) throws AvroRemoteException {
        long rootDepId = getRootDepartmentId(companyId);
        return rootDepId > 0 ? getDepartmentGroup(rootDepId) : null;
    }

    public RecordSet getDirectory(long viewerId, long companyId) throws AvroRemoteException {
        long rootDepId = getRootDepartmentId(companyId);
        if (rootDepId <= 0)
            return new RecordSet();

        String memberIds = platform.getGroupMembers(rootDepId);
        return platform.getUsers(Long.toString(viewerId), memberIds, Platform.USER_STANDARD_COLUMNS);
    }

    public long[] getAllDepartmentIds(long companyId) throws AvroRemoteException {
        long rootDepartmentId = getRootDepartmentId(companyId);
        final LinkedHashSet<Long> depIds = new LinkedHashSet<Long>();
        if (rootDepartmentId > 0) {
            walkDepartment(rootDepartmentId, new DepartmentWalker() {
                @Override
                public void handle(Record departmentRec) {
                    depIds.add(departmentRec.checkGetInt("id"));
                }
            });
        }
        return CollectionUtils2.toLongArray(depIds);
    }

    public RecordSet getAllDepartments(long companyId) throws AvroRemoteException {
        long rootDepartmentId = getRootDepartmentId(companyId);
        final RecordSet recs = new RecordSet();
        if (rootDepartmentId > 0) {
            walkDepartment(rootDepartmentId, new DepartmentWalker() {
                @Override
                public void handle(Record departmentRec) throws Exception {
                    Record rec = new Record();
                    rec.put(DepartmentConstants.COL_ID, departmentRec.checkGetInt("id"));
                    rec.put(DepartmentConstants.COL_NAME, departmentRec.checkGetString("name"));
                    recs.add(rec);
                }
            });
        }
        return recs;
    }

    private void walkDepartment(long depId, DepartmentWalker walker) throws AvroRemoteException {
        Record depRec = getDepartmentGroup(depId);
        if (depRec != null) {
            try {
                walker.handle(depRec);
            } catch (Exception ignored) {
            }
            String subIdsStr = depRec.getString(Constants.DEP_COL_SUB);
            if (StringUtils.isNotBlank(subIdsStr)) {
                long[] subIds = StringUtils2.splitIntArray(subIdsStr, ",");
                for (long subId : subIds) {
                    walkDepartment(subId, walker);
                }
            }
        }
    }


    public static interface DepartmentWalker {
        void handle(Record departmentRec) throws Exception;
    }


    public long[] getDepartmentEmployeeIds(long depId) throws AvroRemoteException {
        if (!isValidDepartmentId(depId))
            return new long[0];
        String ids = platform.getGroupMembers(depId);
        return StringUtils2.splitIntArray(ids, ",");
    }

    public RecordSet getDepartmentEmployee(long viewerId, long depId, String cols, int page, int count) throws AvroRemoteException {
        long[] allEmployeeIds = getDepartmentEmployeeIds(depId);
        long[] pagedEmployeeIds = PageUtils.page(allEmployeeIds, page, count);
        return platform.getUsers(Long.toString(viewerId), StringUtils2.join(pagedEmployeeIds, ","), cols);
    }

    public List<String> getSubDepartmentNames(long companyId, String name) throws AvroRemoteException {
        long rootDepartmentId = getRootDepartmentId(companyId);
        final ArrayList<String> depNames = new ArrayList<String>();
        if (rootDepartmentId > 0) {
            walkDepartment(rootDepartmentId, new DepartmentWalker() {
                @Override
                public void handle(Record departmentRec) throws Exception {
                    depNames.add(departmentRec.checkGetString("name"));
                }
            });
        }
        return depNames;
    }

    public boolean addDepartment(long viewerId, int appId, long parentDepId, String name, long[] adminIds) throws AvroRemoteException {
        RecordSet depRecs = platform.getGroups(Constants.DEPARTMENT_ID_BEGIN,
                Constants.DEPARTMENT_ID_END, Long.toString(parentDepId), ""); // TODO: cols
        if (CollectionUtils.isEmpty(depRecs))
            return false;

        long subDepId = platform.createGroup(
                Constants.DEPARTMENT_ID_BEGIN,
                Constants.TYPE_DEPARTMENT,
                name,
                1000000,
                1,
                1,
                1,
                0,
                1,
                1,
                1,
                0,
                0,
                viewerId,
                "",
                new Record().set(Constants.DEP_COL_PARENT, Long.toString(parentDepId)),
                Long.toString(viewerId),
                "",
                "",
                Integer.toString(appId));

        Record parentDepRec = depRecs.get(0);
        LinkedHashSet<Long> subIds = new LinkedHashSet<Long>(StringUtils2.splitIntList(parentDepRec.getString(Constants.DEP_COL_SUB), ","));
        subIds.add(subDepId);

        return platform.updateGroup(Long.toString(viewerId),
                parentDepId,
                new Record(),
                new Record().set(Constants.DEP_COL_SUB, StringUtils.join(subIds, ",")));
    }

    public boolean deleteDepartment(final long depId) throws AvroRemoteException {
        if (!isValidDepartmentId(depId))
            return false;

        return platform.useRawGroup(new Platform.GroupAccessor<Boolean>() {
            @Override
            public Boolean access(Group g) throws AvroRemoteException {
                g.destroyGroup(Long.toString(depId));
                return null;
            }
        });
    }

    private static boolean isValidDepartmentId(long depId) {
        return depId >= Constants.DEPARTMENT_ID_BEGIN && depId < Constants.DEPARTMENT_ID_END;
    }

    private static boolean isValidCompanyId(long companyId) {
        return companyId >= Constants.COMPANY_ID_BEGIN && companyId < Constants.COMPANY_ID_END;
    }

    private static boolean updateGroupInfo(Group g, long groupId, Record info, Record props) throws AvroRemoteException {
        return g.updateGroup(groupId, info.toByteBuffer(), props.toByteBuffer());
    }

    public boolean moveDepartment(final long depId, final long newParentDepId) throws AvroRemoteException {
        if (!isValidDepartmentId(depId) || !isValidDepartmentId(newParentDepId))
            return false;

        // TODO: 检测newParentDepId是否会出现递归包含depId

        RecordSet depRecs = platform.getGroups(Constants.DEPARTMENT_ID_BEGIN, Constants.DEPARTMENT_ID_END,
                StringUtils2.join(new long[]{depId, newParentDepId}, ","), ""); // TODO: cols

        if (depRecs == null || depRecs.size() < 2)
            return false;

        Record depRec = findDepartmentById(depRecs, depId);
        Record newParentDepRec = findDepartmentById(depRecs, newParentDepId);

        // Change 'sub' for old parent
        final long oldParentDepId = depRec.getInt(Constants.DEP_COL_PARENT, 0);
        if (oldParentDepId > 0) {
            Record oldParentDepRec = getDepartmentGroup(oldParentDepId);
            if (oldParentDepRec != null) {
                final LinkedHashSet<Long> oldParentSubIds = new LinkedHashSet<Long>(StringUtils2.splitIntList(oldParentDepRec.getString(Constants.DEP_COL_SUB, ""), ","));
                oldParentSubIds.remove(depId);
                platform.useRawGroup(new Platform.GroupAccessor<java.lang.Object>() {
                    @Override
                    public Object access(Group g) throws AvroRemoteException {
                        updateGroupInfo(g, oldParentDepId, new Record(), new Record().set(Constants.DEP_COL_SUB, StringUtils.join(oldParentSubIds, ",")));
                        return null;
                    }
                });
            }
        }

        // Change 'sub' for new parent
        final LinkedHashSet<Long> newParentSubIds = new LinkedHashSet<Long>(StringUtils2.splitIntList(newParentDepRec.getString(Constants.DEP_COL_SUB, ""), ","));
        newParentSubIds.add(depId);
        platform.useRawGroup(new Platform.GroupAccessor<java.lang.Object>() {
            @Override
            public Object access(Group g) throws AvroRemoteException {
                updateGroupInfo(g, newParentDepId, new Record(), new Record().set(Constants.DEP_COL_SUB, StringUtils.join(newParentSubIds, ",")));
                return null;
            }
        });

        // Change 'parent' for the department
        platform.useRawGroup(new Platform.GroupAccessor<java.lang.Object>() {
            @Override
            public Object access(Group g) throws AvroRemoteException {
                updateGroupInfo(g, depId, new Record(), new Record().set(Constants.DEP_COL_PARENT, Long.toString(newParentDepId)));
                return null;
            }
        });

        return true;
    }


    private static Record findDepartmentById(RecordSet depRecs, long depId) {
        for (Record rec : depRecs) {
            if (rec.checkGetInt("id") == depId)
                return rec;
        }
        return null;
    }

    private List<Long> getParentDepartmentIds(long depId) throws AvroRemoteException {
        ArrayList<Long> parentDepIds = new ArrayList<Long>();
        if (isValidDepartmentId(depId)) {
            long depId0 = depId;
            while (depId0 > 0) {
                Record depRec = getDepartmentGroup(depId0);
                if (depRec != null) {
                    depId0 = depRec.getInt(Constants.DEP_COL_PARENT, 0);
                }
            }
        }
        return parentDepIds;
    }


    public boolean addEmployeeIntoDepartment(long viewerId, int appId, long depId, long userId, int role) throws AvroRemoteException {
        if (!isValidDepartmentId(depId))
            return false;

        List<Long> parentDepIds = getParentDepartmentIds(depId);
        for (int i = 0; i < parentDepIds.size(); i++) {
            long did = parentDepIds.get(i);
            platform.addMembers(did, new Record().set(Long.toString(userId), i == 0 ? role : Constants.ROLE_MEMBER), false);
        }
        return true;
    }

    public boolean deleteEmployeeInDepartment(long depId, final long userId) throws AvroRemoteException {
        if (!isValidDepartmentId(depId))
            return false;

        final ArrayList<Long> subIds = new ArrayList<Long>();
        walkDepartment(depId, new DepartmentWalker() {
            @Override
            public void handle(Record departmentRec) throws Exception {
                subIds.add(departmentRec.checkGetInt("id"));
            }
        });

        for (final long subDepId : subIds) {
            platform.useRawGroup(new Platform.GroupAccessor<Object>() {
                @Override
                public Object access(Group g) throws AvroRemoteException {
                    g.removeMembers(subDepId, Long.toString(userId));
                    return null;
                }
            });
        }
        return true;
    }


    // stream

    public RecordSet getCompanyTimeline(long companyId, int page, int count) {
        return null;
    }

    public RecordSet getDepartmentTimeline(long companyId, int page, int count) {
        return null;
    }

    // resouce

    public RecordSet getCompanyPhotos(long companyId) {
        return null;
    }

    public RecordSet getDepartmentPhotos(long companyId) {
        return null;
    }

    // TODO: other resources

    // misc

    public static Record companyAsUser(Record companyRec) {
        return null;
    }
}
