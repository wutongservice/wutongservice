package com.borqs.server.platform.authorization;


import com.borqs.server.ErrorCode;
import com.borqs.server.base.BaseException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.service.platform.Platform;
import org.apache.avro.AvroRemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.List;

public class AuthServlet extends WebMethodServlet {

    private static final Logger L = LoggerFactory.getLogger(AuthServlet.class);
    private final GenericTransceiverFactory transceiverFactory = new GenericTransceiverFactory();
    private SimpleAuthorization auth;

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();
        transceiverFactory.setConfig(conf);
        transceiverFactory.init();
        auth = new SimpleAuthorization();
        auth.setConfig(conf);
        auth.init();
      }

    public AuthServlet(){}

    @Override
    public void destroy() {
        transceiverFactory.destroy();
        super.destroy();
    }

    private Platform platform() {
        Platform p = new Platform(transceiverFactory);
        p.setConfig(getConfiguration());
        return p;
    }

    @WebMethod("auth/dept/create")
    public boolean autDept_save(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String dept_name = qp.checkGetString("dept_name");
        Record description = new Record();
        description.put("description", qp.getString("description", ""));
        long parent_id = qp.getInt("parent_id", 0);
        int level =0;
        if (parent_id != 0) {
            Record dept_f = auth.findOwnDept(String.valueOf(parent_id), false);
            level = (int) dept_f.getInt("level");
            level += 1;
        }
        if (auth.findDeptName(dept_name, parent_id))
            throw new BaseException(ErrorCode.PARAM_ERROR, "the name " + dept_name + " has exists in this level");
        return auth.saveDepartment(dept_name, description, level, parent_id);
    }

    @WebMethod("auth/dept/update")
    public boolean authDeptUpdate(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String dept_id = qp.checkGetString("dept_id");
        String dept_name = qp.checkGetString("dept_name");
        return auth.updateDeptName(dept_id, dept_name);
    }

    @WebMethod("auth/dept/delete")
    public boolean authDeptDelete(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String dept_ids = qp.checkGetString("dept_ids");
        List<String> dept_ids_list = StringUtils2.splitList(dept_ids, ",", true);
        for (String dept_id : dept_ids_list) {
            if (!auth.canDeleteDept(dept_id)) {
                throw new BaseException(ErrorCode.PARAM_ERROR, "the department has many son ,can't delete,must delete his son first");
            }
        }
        return auth.deleteDept(dept_ids);
    }

    @WebMethod("auth/dept/add_user")
    public boolean authDeptAddUsers(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String dept_id = qp.checkGetString("dept_id");
        String user_ids = qp.checkGetString("user_ids");
        int leader = (int) qp.getInt("leader", 0);
        boolean delete_old = qp.checkGetBoolean("delete_old");
        return auth.saveDepartmentUser(dept_id, user_ids, leader,delete_old);
    }

    @WebMethod("auth/dept/get")
    public Record findOwnDept(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String dept_id = qp.checkGetString("dept_id");
        boolean get_users = qp.getBoolean("get_users", false);
        Record rec= auth.findOwnDept(dept_id, get_users);
        RecordSet users = RecordSet.fromJson(rec.getString("users"));
        Platform p = platform();
        if (users.size() > 0) {
            for (Record rec0 : users) {
                Record u = auth.getUser(rec0.getString("user_id")).getFirstRecord();
                rec0.put("display_name", u.getString("display_name"));
            }
            rec.put("users",users);
        }
        return rec;
    }

    @WebMethod("auth/dept/delete_user")
    public boolean deleteDepartmentUser(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String dept_id = qp.checkGetString("dept_id");
        String user_ids = qp.checkGetString("user_ids");
        return auth.deleteDepartmentUser(dept_id, user_ids);
    }

    @WebMethod("auth/dept/update_leader")
    public boolean updateDepartmentUserLeader(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String dept_id = qp.checkGetString("dept_id");
        String user_ids = qp.checkGetString("user_ids");
        int leader = (int) qp.getInt("leader", 0);
        return auth.updateDepartmentUserLeader(dept_id, user_ids, leader);
    }

    @WebMethod("auth/dept/get_user")
    public RecordSet findDeptUsers(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String dept_id = qp.checkGetString("dept_id");
        RecordSet base_users = auth.findDeptUsers(dept_id);
        Platform p = platform();
        for (Record rec : base_users) {
            Record u = p.getUser("", rec.getString("user_id"), "display_name,image_url", true);
            rec.put("display_name", u.getString("display_name"));
            rec.put("image_url", u.getString("image_url"));
        }
        return base_users;
    }

    @WebMethod("auth/role/save")
    public boolean saveRole(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String dept_id = qp.checkGetString("dept_id");
        String role_name = qp.checkGetString("role_name");
        String description = qp.getString("description", "");
        if (auth.findRoleName(dept_id, role_name))
            throw new BaseException(ErrorCode.PARAM_ERROR, "the role name " + role_name + " has exists in this department");
        return auth.saveRole(dept_id, role_name, description);
    }

    @WebMethod("auth/role/update_name")
    public boolean updateRoleName(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String role_id = qp.checkGetString("role_id");
        String role_name = qp.checkGetString("role_name");
        return auth.updateRoleName(role_id, role_name);
    }

    @WebMethod("auth/role/delete")
    public boolean deleteRole(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String role_id = qp.checkGetString("role_id");
        return auth.deleteRole(role_id);
    }

    @WebMethod("auth/role/get_user")
    public RecordSet findRoleUsers(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String role_id = qp.checkGetString("role_id");
        RecordSet base_users = auth.findRoleUsers(role_id);
        Platform p = platform();
        for (Record rec : base_users) {
            Record u = p.getUser("", rec.getString("user_id"), "display_name,image_url", false);
            rec.put("display_name", u.getString("display_name"));
            rec.put("image_url", u.getString("image_url"));
        }
        return base_users;
    }

    @WebMethod("auth/role/get")
    public Record findRoleByID(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String role_id = qp.checkGetString("role_id");
        boolean get_users = qp.getBoolean("get_users", false);
        Record rec= auth.findRoleById(role_id, get_users);
        RecordSet users = RecordSet.fromJson(rec.getString("users"));
        if (users.size() > 0) {
            for (Record rec0 : users) {
                Record u = auth.getUser(rec0.getString("user_id")).getFirstRecord();
                rec0.put("display_name", u.getString("display_name"));
            }
            rec.put("users",users);
        }
        return rec;
    }

    @WebMethod("auth/role/delete_user")
    public boolean deleteUserRole(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String role_id = qp.checkGetString("role_id");
        String user_ids = qp.checkGetString("user_ids");
        return auth.deleteUserRole(role_id,user_ids);
    }

    @WebMethod("auth/role/add_user")
    public boolean saveUserRole(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String role_id = qp.checkGetString("role_id");
        String user_ids = qp.checkGetString("user_ids");
        boolean delete_old = qp.checkGetBoolean("delete_old");
        return auth.saveUserRole(role_id, user_ids,delete_old);
    }

    @WebMethod("auth/dept/get_by_user_id")
    public RecordSet findDeptByUserId(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String user_id = qp.checkGetString("user_id");
        return auth.findDeptByUserId(user_id);
    }

    @WebMethod("auth/dept/get_all")
    public RecordSet findAllDept(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String dept_id = qp.checkGetString("dept_id");
        return auth.findAllDept(dept_id);
    }

    @WebMethod("auth/dept/get_top_dept_id")
    public String findParentDeptIdByDeptId(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String dept_id = qp.checkGetString("dept_id");
        return auth.findParentDeptIdByDeptId(dept_id);
    }

    @WebMethod("auth/role/get_all")
    public RecordSet findAllRoleByDeptId(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String dept_id = qp.checkGetString("dept_id");
        return auth.findRoleByDeptId(dept_id);
    }

    @WebMethod("auth/role/get_by_user_id")
    public RecordSet findRolesByUserId(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String user_id = qp.checkGetString("user_id");
        return auth.findRolesByUserId(user_id);
    }

    @WebMethod("auth/find_user")
    public RecordSet findUser(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String type = qp.checkGetString("type");
        String dept_id = qp.getString("dept_id","");
        String role_id = qp.getString("role_id","");
        return auth.findUser(type,dept_id,role_id);
    }

    @WebMethod("auth/user/get_info")
    public Record findUserInfo(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        String user_id = qp.checkGetString("user_id");
        return auth.getUserInfo(user_id);
    }
}
