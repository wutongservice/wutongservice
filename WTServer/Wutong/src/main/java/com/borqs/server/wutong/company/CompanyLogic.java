package com.borqs.server.wutong.company;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import org.apache.commons.fileupload.FileItem;

import java.util.Map;


public interface CompanyLogic {
    void joinCompanyOrCreateCompany(Context ctx, String email);

    Record createCompany(Context ctx, String name, String emailDomain);

    boolean destroyCompany(Context ctx, long companyId);

    Record updateCompany(Context ctx, long companyId, Record info);

    Record createCompanyFromGroup(Context ctx, String name, String emailDomain, long groupId);

    RecordSet getCompanies(Context ctx, long... companyIds);

    RecordSet getCompanyDepCircles(Context ctx, long companyId);

    RecordSet searchCompanies(Context ctx, String kw);

    RecordSet belongsCompanies(Context ctx, long userId);

    boolean grant(Context ctx, long companyId, Map<Long, Integer> roles);

    boolean companyExists(long companyId);

    RecordSet uploadEmployees(Context ctx, long companyId, FileItem excelFile, boolean merge);

    RecordSet getEmployeeInfos(Context ctx, long[] userIds);

    RecordSet listEmployees(Context ctx, long companyId, String sort, int page, int count);

    Record addEmployee(Context ctx, long companyId, String name, String email, Record others);

    Record updateEmployee(Context ctx, long companyId, String email, Record info);

    boolean deleteEmployees(Context ctx, long companyId, String... emails);

    RecordSet searchEmployee(Context ctx, long companyId, String kw, String sort, int count);

    RecordSet getCompanyUsers(Context ctx, long companyId, String cols, int page, int count);

    void createDepartmentCircleByEmployeeList(Context ctx, long companyId);
}
