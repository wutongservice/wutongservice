package com.borqs.server.wutong.company;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.commons.WutongContext;
import org.apache.commons.fileupload.FileItem;

import javax.servlet.ServletException;
import java.util.HashMap;

public class CompanyServlet extends WebMethodServlet {

    private StaticFileStorage photoStorage;

    public CompanyServlet() {
    }

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = GlobalConfig.get();
        photoStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.photoStorage", ""));
        photoStorage.init();
    }

    @Override
    public void destroy() {
        photoStorage.destroy();
    }

    @WebMethod("company/show")
    public RecordSet showCompanies(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);

        long[] companyIds = StringUtils2.splitIntArray(qp.checkGetString("companies"), ",");
        CompanyLogic cl = GlobalLogics.getCompany();
        return cl.getCompanies(ctx, companyIds);
    }

    @WebMethod("company/update")
    public Record updateCompany(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);

        long companyId = qp.checkGetInt("company");
        Record info = new Record();
        info.putIf("email_domain1", qp.getString("email_domain1", ""), qp.containsKey("email_domain1"));
        info.putIf("email_domain2", qp.getString("email_domain2", ""), qp.containsKey("email_domain2"));
        info.putIf("email_domain3", qp.getString("email_domain3", ""), qp.containsKey("email_domain3"));
        info.putIf("email_domain4", qp.getString("email_domain4", ""), qp.containsKey("email_domain4"));
        info.putIf("name", qp.getString("name", ""), qp.containsKey("name"));
        info.putIf("email", qp.getString("email", ""), qp.containsKey("email"));
        info.putIf("address", qp.getString("address", ""), qp.containsKey("address"));
        info.putIf("website", qp.getString("website", ""), qp.containsKey("website"));
        info.putIf("tel", qp.getString("tel", ""), qp.containsKey("tel"));
        info.putIf("fax", qp.getString("fax", ""), qp.containsKey("fax"));
        info.putIf("zip_code", qp.getString("zip_code", ""), qp.containsKey("zip_code"));
        info.putIf("small_logo_url", qp.getString("small_logo_url", ""), qp.containsKey("small_logo_url"));
        info.putIf("logo_url", qp.getString("logo_url", ""), qp.containsKey("logo_url"));
        info.putIf("large_logo_url", qp.getString("large_logo_url", ""), qp.containsKey("large_logo_url"));
        info.putIf("small_cover_url", qp.getString("small_cover_url", ""), qp.containsKey("small_cover_url"));
        info.putIf("cover_url", qp.getString("cover_url", ""), qp.containsKey("cover_url"));
        info.putIf("large_cover_url", qp.getString("large_cover_url", ""), qp.containsKey("large_cover_url"));
        info.putIf("description", qp.getString("description", ""), qp.containsKey("description"));

        CompanyLogic cl = GlobalLogics.getCompany();
        return cl.updateCompany(ctx, companyId, info);
    }

    private String[] saveCompanyImages(long companyId, String type, FileItem fi) {
        String[] urls = new String[3];

        long uploaded_time = DateUtils.nowMillis();
        String imageName = type + "_" + companyId + "_" + uploaded_time;

        String sfn = imageName + "_S.jpg";
        String ofn = imageName + "_M.jpg";
        String lfn = imageName + "_L.jpg";
        urls[0] = sfn;
        urls[1] = ofn;
        urls[2] = lfn;

        if (photoStorage instanceof OssSFS) {
            lfn = "media/photo/" + lfn;
            ofn = "media/photo/" + ofn;
            sfn = "media/photo/" + sfn;
        }

        SFSUtils.saveScaledUploadImage(fi, photoStorage, sfn, "50", "50", "jpg");
        SFSUtils.saveScaledUploadImage(fi, photoStorage, ofn, "80", "80", "jpg");
        SFSUtils.saveScaledUploadImage(fi, photoStorage, lfn, "180", "180", "jpg");

        return urls;
    }

    @WebMethod("company/upload_logo")
    public Record uploadCompanyLogo(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long companyId = qp.checkGetInt("company");
        FileItem fi = qp.checkGetFile("file");
        String[] urls = saveCompanyImages(companyId, "c_logo", fi);
        Record info = new Record();
        info.put("small_logo_url", urls[0]);
        info.put("logo_url", urls[1]);
        info.put("large_logo_url", urls[2]);
        CompanyLogic cl = GlobalLogics.getCompany();
        return cl.updateCompany(ctx, companyId, info);
    }

    @WebMethod("company/upload_cover")
    public Record uploadCompanyCover(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long companyId = qp.checkGetInt("company");
        FileItem fi = qp.checkGetFile("file");
        String[] urls = saveCompanyImages(companyId, "c_cover", fi);
        Record info = new Record();
        info.put("small_cover_url", urls[0]);
        info.put("cover_url", urls[1]);
        info.put("large_cover_url", urls[2]);
        CompanyLogic cl = GlobalLogics.getCompany();
        return cl.updateCompany(ctx, companyId, info);
    }

    @WebMethod("company/belongs")
    public RecordSet belongsCompanies(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        CompanyLogic cl = GlobalLogics.getCompany();
        return cl.belongsCompanies(ctx, ctx.getViewerId());
    }

    @WebMethod("company/search")
    public RecordSet searchCompanies(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        String kw = qp.checkGetString("kw");
        CompanyLogic cl = GlobalLogics.getCompany();
        return cl.searchCompanies(ctx, kw);
    }

    @WebMethod("company/grant")
    public boolean companyGrant(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long companyId = qp.checkGetInt("company");
        HashMap<Long, Integer> roles = new HashMap<Long, Integer>();
        if (qp.containsKey("admins")) {
            long[] uids = StringUtils2.splitIntArray(qp.checkGetString("admins"), ",");
            for (long uid : uids)
                roles.put(uid, Constants.ROLE_ADMIN);
        }
        if (qp.containsKey("members")) {
            long[] uids = StringUtils2.splitIntArray(qp.checkGetString("members"), ",");
            for (long uid : uids)
                roles.put(uid, Constants.ROLE_MEMBER);
        }

        CompanyLogic cl = GlobalLogics.getCompany();
        return roles.isEmpty() || cl.grant(ctx, companyId, roles);
    }

    @WebMethod("company/upload_employees")
    public RecordSet uploadEmployees(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long companyId = qp.checkGetInt("company");
        boolean merge = qp.getBoolean("merge", false);
        FileItem excelFile = qp.checkGetFile("file");
        CompanyLogic cl = GlobalLogics.getCompany();
        return cl.uploadEmployees(ctx, companyId, excelFile, merge);
    }

    @WebMethod("company/employee/list")
    public RecordSet listEmployees(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long companyId = qp.checkGetInt("company");
        String sort = qp.getString("sort", EmployeeListConstants.COL_NAME);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 50);

        CompanyLogic cl = GlobalLogics.getCompany();
        return cl.listEmployees(ctx, companyId, sort, page, count);
    }

    @WebMethod("company/employee/add")
    public Record addEmployee(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long companyId = qp.checkGetInt("company");
        String name = qp.checkGetString("name");
        String email = qp.checkGetString("email");

        CompanyLogic cl = GlobalLogics.getCompany();
        Record other = new Record();
        other.put(EmployeeListConstants.COL_EMPLOYEE_ID, qp.getString("employee_id", ""));
        other.put(EmployeeListConstants.COL_DEPARTMENT, qp.getString("department", ""));
        other.put(EmployeeListConstants.COL_JOB_TITLE, qp.getString("job_title", ""));
        other.put(EmployeeListConstants.COL_TEL, qp.getString("tel", ""));
        other.put(EmployeeListConstants.COL_MOBILE_TEL, qp.getString("mobile_tel", ""));
        return cl.addEmployee(ctx, companyId, name, email, other);
    }

    @WebMethod("company/employee/update")
    public Record updateEmployee(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long companyId = qp.checkGetInt("company");
        String email = qp.checkGetString("email");


        CompanyLogic cl = GlobalLogics.getCompany();
        Record other = new Record();
        other.put(EmployeeListConstants.COL_NAME, qp.getString("name", ""));
        other.put(EmployeeListConstants.COL_EMPLOYEE_ID, qp.getString("employee_id", ""));
        other.put(EmployeeListConstants.COL_DEPARTMENT, qp.getString("department", ""));
        other.put(EmployeeListConstants.COL_JOB_TITLE, qp.getString("job_title", ""));
        other.put(EmployeeListConstants.COL_TEL, qp.getString("tel", ""));
        other.put(EmployeeListConstants.COL_MOBILE_TEL, qp.getString("mobile_tel", ""));
        return cl.updateEmployee(ctx, companyId, email, other);
    }

    @WebMethod("company/employee/delete")
    public boolean deleteEmployees(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long companyId = qp.checkGetInt("company");
        String[] emails = StringUtils2.splitArray(qp.checkGetString("emails"), ",", true);

        CompanyLogic cl = GlobalLogics.getCompany();
        return cl.deleteEmployees(ctx, companyId, emails);
    }

    @WebMethod("company/employee/search")
    public RecordSet searchEmployee(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        long companyId = qp.checkGetInt("company");
        String kw = qp.checkGetString("kw");

        CompanyLogic cl = GlobalLogics.getCompany();
        return cl.searchEmployee(ctx, companyId,
                kw,
                qp.getString("sort", EmployeeListConstants.COL_NAME),
                (int) qp.getInt("count", 100));
    }

    @WebMethod("company/employee/info")
    public RecordSet getEmployeeInfo(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long[] userIds = StringUtils2.splitIntArray(qp.checkGetString("users"), ",");
        CompanyLogic cl = GlobalLogics.getCompany();
        return cl.getEmployeeInfos(ctx, userIds);
    }

    @WebMethod("company/users")
    public RecordSet getCompanyUsers(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long companyId = qp.checkGetInt("company");
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        String cols = qp.getString("cols", AccountLogic.USER_LIGHT_COLUMNS);

        CompanyLogic cl = GlobalLogics.getCompany();
        return cl.getCompanyUsers(ctx, companyId, cols, page, count);
    }

    @WebMethod("company/department_circles")
    public RecordSet getSubDeps(QueryParams qp){
        Context ctx = WutongContext.getContext(qp, true);
        long companyId = qp.checkGetInt("company");
        CompanyLogic cl = GlobalLogics.getCompany();
        return cl.getCompanyDepCircles(ctx, companyId);
    }

    /*
    @WebMethod("company/create_from_circle")
    public Record createCompanyFromCircle(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        long groupId = qp.checkGetInt("group");
        String name = qp.checkGetString("name");
        String emailDomain = qp.checkGetString("email_domain");
        CompanyLogic cl = GlobalLogics.getCompany();
        return cl.createCompanyFromGroup(ctx, name, emailDomain, groupId);
    }

    @WebMethod("company/auto_create_departments")
    public RecordSet autoCreateDepartments(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        long companyId = qp.checkGetInt("company");
        CompanyLogic cl = GlobalLogics.getCompany();
        cl.createDepartmentCircleByEmployeeList(ctx, companyId);
        return cl.getCompanyDepCircles(ctx, companyId);
    }
    */
}
