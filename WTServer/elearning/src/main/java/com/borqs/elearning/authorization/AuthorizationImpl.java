package com.borqs.elearning.authorization;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AuthorizationImpl implements AuthorizationLogic{

    private static final Logger L = LoggerFactory.getLogger(AuthorizationImpl.class);
    public final Schema departmentSchema = Schema.loadClassPath(AuthorizationImpl.class, "department.schema");
    public final Schema departmentUserSchema = Schema.loadClassPath(AuthorizationImpl.class, "department_user.schema");
    public final Schema roleSchema = Schema.loadClassPath(AuthorizationImpl.class, "role.schema");
    public final Schema userRoleSchema = Schema.loadClassPath(AuthorizationImpl.class, "user_role.schema");

    private ConnectionFactory connectionFactory;
    private String db;
    private String departmentTable;
    private String departmentUserTable;
    private String roleTable;
    private String userRoleTable;

    public AuthorizationImpl() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf
                .getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("platform.account.db", null);
        this.departmentTable = conf.getString("account.simple.departmentTable", "department");
        this.departmentUserTable = conf.getString("account.simple.departmentUserTable", "department_user");
        this.roleTable = conf.getString("account.simple.roleTable", "role");
        this.userRoleTable = conf.getString("account.simple.userRoleTable", "user_role");
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    //保存部门信息
    public boolean saveDepartment(String dept_name, Record dept_description, int level, long parent_id) {
        Record department0 = new Record();
        String dept_id = Long.toString(RandomUtils.generateId());
        department0.put("dept_id", dept_id);
        department0.put("dept_name", dept_name);
        department0.put("dept_description", dept_description.toString());
        department0.put("level", level);
        department0.put("parent_id", level == 0 ? dept_id : parent_id);

        final String SQL = "INSERT INTO ${table} ${values_join(alias, department)}";

        String sql = SQLTemplate.merge(SQL,
                "table", departmentTable, "alias", departmentSchema.getAllAliases(),
                "department", department0);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    //获取本级部门内 ，是否已经存在这个部门名称
    public boolean findDeptName(String dept_name, long parent_id) {
        String sql = "select dept_id from " + departmentTable + " where parent_id='" + parent_id + "' and dept_name='" + dept_name + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        return rs.size() > 0;
    }

    //修改部门名称
    public boolean updateDeptName(String dept_id, String dept_name) {
        boolean success = true;
        String sql = "select dept_name from " + departmentTable + " where dept_id<>'" + dept_id + "' and dept_name='" + dept_name + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        boolean can_update = (rs.size() <= 0);
        if (can_update) {
            String sql1 = "update " + departmentTable + " set dept_name='" + dept_name + "' where  dept_id='" + dept_id + "'";
            long n = se.executeUpdate(sql1);
            success = n > 0;
        } else {
            success = false;
        }
        return success;
    }

    //找下级部门
    public RecordSet findSonDept(String dept_id) {
        String sql = "select * from " + departmentTable + " where parent_id='" + dept_id + "' and parent_id<>dept_id  order by dept_name";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        if (rs.size() > 0) {
            for (Record rec : rs) {
                rec.put("str_dept_id", String.valueOf(rec.getString("dept_id")));
                rec.put("str_parent_id", String.valueOf(rec.getString("parent_id")));
                RecordSet son_dept = findSonDept(rec.getString("dept_id"));
                if (son_dept.size() > 0) {
                    rec.put("son_dept", son_dept);
                } else {
                    rec.put("son_dept", new RecordSet());
                }
            }
        }
        return rs;
    }

    //找全部部门部门
    public RecordSet findAllDept(String dept_id) {
        String sql = "select * from " + departmentTable + " where dept_id='" + dept_id + "' order by dept_name";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        if (rs.size() > 0) {
            for (Record rec : rs) {
                rec.put("str_dept_id", String.valueOf(rec.getString("dept_id")));
                rec.put("str_parent_id", String.valueOf(rec.getString("parent_id")));
                rec.put("son_dept", findSonDept(rec.getString("dept_id")));
            }
        }
        return rs;
    }

    //找本级部门
    public Record findOwnDept(String dept_id, boolean get_users) {
        String sql = "select * from " + departmentTable + " where dept_id='" + dept_id + "'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql.toString(), null);
        rec.put("str_dept_id", String.valueOf(rec.getString("dept_id")));
        rec.put("str_parent_id", String.valueOf(rec.getString("parent_id")));
        if (get_users) {
            RecordSet users = findDeptUsers(dept_id);
            rec.put("users", users);
        }else {
            rec.put("users", new RecordSet());
        }
        return rec;
    }

    //找父级部门
    public Record findFatherDept(String parent_id) {
        String sql = "select * from " + departmentTable + " where dept_id='" + parent_id + "'";
        SQLExecutor se = getSqlExecutor();
        Record rs = se.executeRecord(sql.toString(), null);
        rs.put("str_dept_id", String.valueOf(rs.getString("dept_id")));
        rs.put("str_parent_id", String.valueOf(rs.getString("parent_id")));
        return rs;
    }

    //部门是否能被删除，如果有下级部门，不允许删除
    public boolean canDeleteDept(String dept_id) {
        String sql = "select dept_id from " + departmentTable + " where parent_id='" + dept_id + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        boolean can_delete = (rs.size() <= 0);
        return can_delete;
    }

    //删除部门
    public boolean deleteDept(String dept_ids) {
        List<String> dept_ids_list = StringUtils2.splitList(dept_ids, ",", true);
        SQLExecutor se = getSqlExecutor();
        List<String> sqls = new ArrayList<String>();
        for (String dept_id : dept_ids_list) {
            RecordSet rs = findDeptUsers(dept_id);
            String user_ids = rs.joinColumnValues("user_id", ",");
            if (user_ids.length() > 0) {
                String sql1 = "delete from " + departmentUserTable + " where dept_id='" + dept_id + "' and user_id in (" + user_ids + ")";
                sqls.add(sql1);
            }
            String sql2 = "delete from " + departmentTable + " where dept_id='" + dept_id + "'";
            sqls.add(sql2);
        }
        se.executeUpdate(sqls);
        return true;
    }

    //保存部门里面的用户
    public boolean saveDepartmentUser(String dept_id, String user_ids, int leader,boolean delete_old) {
        SQLExecutor se = getSqlExecutor();
        List<String> sqls = new ArrayList<String>();

        String leader_ids =  "";
        if (delete_old){
            //先记住本部门里面哪些人是leader
            String sql0 = "select user_id from " + departmentUserTable + " where dept_id='" + dept_id + "' and leader=1";
            RecordSet rs = se.executeRecordSet(sql0.toString(), null);
            leader_ids = rs.joinColumnValues("user_id",",");

            String sql1 = "delete from " + departmentUserTable + " where dept_id='" + dept_id + "'";
            se.executeUpdate(sql1);
        }
        List<String> gl = StringUtils2.splitList(user_ids, ",", true);
        final String SQL = "INSERT INTO ${table} ${values_join(alias, department_user)}";
        for (String gl0 : gl) {
            Record department_user0 = new Record();
            department_user0.put("dept_id", dept_id);
            department_user0.put("user_id", gl0);
            department_user0.put("leader", leader);

            String sql = SQLTemplate.merge(SQL,
                    "table", departmentUserTable, "alias", departmentUserSchema.getAllAliases(),
                    "department_user", department_user0);
            sqls.add(sql);
        }

        long n = se.executeUpdate(sqls);
        if (leader_ids.length()>0){
            String sql1 = "update " + departmentUserTable + " set leader=1 where dept_id='" + dept_id + "' and user_id in ("+leader_ids+")";
            se.executeUpdate(sql1);
        }

        return n > 0;
    }

    //删除部门里面的用户
    public boolean deleteDepartmentUser(String dept_id, String user_ids) {
        String sql = "delete from " + departmentUserTable + " where dept_id='" + dept_id + "' and user_id in (" + user_ids + ")";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    //修改部门里面的领导
    public boolean updateDepartmentUserLeader(String dept_id, String user_ids, int leader) {
        String sql = "update " + departmentUserTable + " set leader='" + leader + "' where dept_id='" + dept_id + "' and user_id in ("+user_ids+")";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        List<String> user_id_l = StringUtils2.splitList(user_ids,",",true);


        String sql1 = "select * from "+roleTable+" where role_name='team leader'" ;
        RecordSet rs_role = se.executeRecordSet(sql1.toString(), null);
        String role_id = rs_role.getFirstRecord().getString("role_id");

        if (!role_id.equals("")) {
            if (leader == 1) {
                List<String> sqls = new ArrayList<String>();
                for (String user_id : user_id_l) {
                    //insert into role
                    if (user_id.length() > 0 && !role_id.equals("")) {
                        sqls.add("insert into " + userRoleTable + " (role_id,user_id) values ('" + role_id + "','" + user_id + "')");
                    }
                }
                se.executeUpdate(sqls);
            } else {
                //看看此人还有几个leader的身份  ，如果没有了，就完全干掉
                List<String> sqls = new ArrayList<String>();
                for (String user_id : user_id_l) {
                    //insert into role
                    String sql2 = "select * from "+departmentUserTable+" where user_id='" + user_id + "' and leader=1";
                    RecordSet rs_leader = se.executeRecordSet(sql2.toString(), null);
                    if (rs_leader.size() <= 0) {
                        if (user_id.length() > 0 && !role_id.equals("")) {
                            sqls.add("delete from " + userRoleTable + " where user_id='" + user_id + "' and role_id='" + role_id + "'");
                        }
                    }
                }
                se.executeUpdate(sqls);
            }
        }

        return n > 0;
    }

    //获取部门里面的用户
    public RecordSet findDeptUsers(String dept_id) {
        String sql = "select user_id,leader from " + departmentUserTable + " where dept_id='" + dept_id + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        return rs;
    }

    //保存角色名称
    public boolean saveRole(String dept_id, String role_name, String role_description) {
        boolean success = true;
        if (!findRoleName(dept_id, role_name)) {
            Record rols0 = new Record();
            rols0.put("role_id", Long.toString(RandomUtils.generateId()));
            rols0.put("dept_id", dept_id);
            rols0.put("role_name", role_name);
            rols0.put("role_description", role_description);

            final String SQL = "INSERT INTO ${table} ${values_join(alias, rols0)}";
            String sql = SQLTemplate.merge(SQL,
                    "table", roleTable, "alias", roleSchema.getAllAliases(),
                    "rols0", rols0);

            SQLExecutor se = getSqlExecutor();
            long n = se.executeUpdate(sql);
            success = n > 0;
        } else {
            success = false;
        }
        return success;
    }

    //获取本公司内 ，是否已经存在这个角色名称
    public boolean findRoleName(String dept_id, String role_name) {
        String sql = "select dept_id from " + roleTable + " where dept_id='" + dept_id + "' and role_name='" + role_name + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        return rs.size() > 0;
    }

    //修改角色名称
    public boolean updateRoleName(String role_id, String role_name) {
        boolean success = true;
        String sql = "select role_name from " + roleTable + " where role_id<>'" + role_id + "' and role_name='" + role_name + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        boolean can_update = (rs.size() <= 0);
        if (can_update) {
            String sql1 = "update " + roleTable + " set role_name='" + role_name + "' where  role_id='" + role_id + "'";
            long n = se.executeUpdate(sql1);
            success = n > 0;
        } else {
            success = false;
        }
        return success;
    }

    //删除角色
    public boolean deleteRole(String role_id) {
        RecordSet role_users = findRoleUsers(role_id);
        String user_ids = role_users.joinColumnValues("user_id", ",");
        deleteUserRole(role_id, user_ids);
        String sql = "delete from " + roleTable + " where role_id='" + role_id + "'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    //获取本角色的所有用户
    public RecordSet findRoleUsers(String role_id) {
        String sql = "select user_id from " + userRoleTable + " where role_id='" + role_id + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        return rs;
    }

    //获取本角色的信息
    public Record findRoleById(String role_id,boolean get_users) {
        String sql = "";
        if (role_id.equals("999")) {
            sql = "select * from " + roleTable + " order by role_id limit 1";
        } else {
            sql = "select * from " + roleTable + " where role_id='" + role_id + "'";
        }
        SQLExecutor se = getSqlExecutor();
        Record rs = se.executeRecord(sql.toString(), null);
        rs.put("str_role_id", String.valueOf(rs.getString("role_id")));
        if (get_users) {
            RecordSet users = findRoleUsers(role_id);
            rs.put("users", users);
        } else {
            rs.put("users", new RecordSet());
        }
        return rs;
    }

    //获取本公司所有角色的信息
    public RecordSet findRoleByDeptId(String dept_id) {
        String sql = "select * from " + roleTable + " where dept_id='" + dept_id + "' order by role_id";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        for (Record rec : rs) {
            rec.put("str_role_id", String.valueOf(rec.getString("role_id")));
        }
        return rs;
    }

    //根据ID删除本角色下的一些用户
    public boolean deleteUserRole(String role_id, String user_ids) {
        String sql1 = "delete from " + userRoleTable + " where role_id='" + role_id + "' and  user_id in (" + user_ids + ")";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql1);
        return n > 0;
    }

    //为本角色增加用户
    public boolean saveUserRole(String role_id, String user_ids,boolean delete_old) {
        SQLExecutor se = getSqlExecutor();
        if (delete_old) {
            String sql1 = "delete from " + userRoleTable + " where role_id='" + role_id + "'";
            se.executeUpdate(sql1);
        }

        List<String> gl = StringUtils2.splitList(user_ids, ",", true);
        final String SQL = "INSERT INTO ${table} ${values_join(alias, user_rols0)}";
        List<String> sqls = new ArrayList<String>();

        for (String gl0 : gl) {
            Record user_rols0 = new Record();
            user_rols0.put("user_id", gl0);
            user_rols0.put("role_id", role_id);
            String sql = SQLTemplate.merge(SQL,
                    "table", userRoleTable, "alias", userRoleSchema.getAllAliases(),
                    "user_rols0", user_rols0);
            sqls.add(sql);
        }
        long n = se.executeUpdate(sqls);
        return n > 0;
    }


    //==========================================================================================
    //跟据用户ID获取我所在的小部门
    public RecordSet findDeptByUserId(String user_id) {
        String sql = "select dept_id,leader from " + departmentUserTable + " where user_id='" + user_id + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        for (Record rec : rs) {
            Record own_dept = findOwnDept(rec.getString("dept_id"), false);
            rec.put("dept_name", own_dept.getString("dept_name"));
            rec.put("dept_description", own_dept.getString("dept_description"));
            rec.put("level", own_dept.getString("level"));
            rec.put("parent_id", own_dept.getString("parent_id"));
            rec.put("str_dept_id", String.valueOf(rec.getString("dept_id")));
            rec.put("str_parent_id", String.valueOf(rec.getString("parent_id")));
        }
        return rs;
    }

    //根据部门ID获取到顶级部门的ID
    public String findParentDeptIdByDeptId(String dept_id) {
        Record now_own_dept = findOwnDept(dept_id, false);
        String now_parent_id = now_own_dept.getString("parent_id");
        if (now_parent_id.equals(dept_id))
            return now_parent_id;

        while (true) {
            Record rec_father = findFatherDept(now_parent_id);
            if (now_parent_id.equals(rec_father.getString("parent_id"))) {
                break;
            } else {
                now_parent_id = rec_father.getString("parent_id");
            }
        }

        return now_parent_id;
    }

    //跟据用户ID获取我所有的角色
    public RecordSet findRolesByUserId(String user_id) {
        String sql = "select role_id from " + userRoleTable + " where user_id='" + user_id + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        for (Record rec0 : rs) {
            //根据roleId获取deptId
            Record role0 = findRoleById(rec0.getString("role_id"),false);
            rec0.put("role_name", role0.getString("role_name"));
            rec0.put("role_description", role0.getString("role_description"));
            rec0.put("str_role_id", String.valueOf(rec0.getString("role_id")));
        }
        return rs;
    }

    //跟据用户ID获取我的领导是谁
    public RecordSet findLeaderByUserId(String user_id) {
        String sql = "SELECT DISTINCT(user_id) FROM department_user " +
                "WHERE leader=1 and dept_id IN (SELECT dept_id FROM department_user WHERE user_id='"+user_id+"' and leader=0)";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
         String leader_ids = rs.joinColumnValues("user_id",",");
        RecordSet out_user = new RecordSet();
        if (leader_ids.length()>0){
              out_user = getUser(leader_ids);
        }
        return out_user;
    }

    //获取我可以我部门的其他同事
    public RecordSet findWorkMatesByUserId(String user_id) {
        String sql = "SELECT DISTINCT(user_id) FROM department_user WHERE user_id <> '"+user_id+"' and dept_id IN (SELECT dept_id FROM department_user WHERE user_id='"+user_id+"')";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
         String leader_ids = rs.joinColumnValues("user_id",",");
        RecordSet out_user = new RecordSet();
        if (leader_ids.length()>0){
              out_user = getUser(leader_ids);
        }
        return out_user;
    }


    public RecordSet findUser(String type,String dept_id,String role_id) {
        String sql = "select user_id,display_name from user2 where length(display_name)>0 and " +
                "(instr(login_email1,'@borqs')>0 or instr(login_email2,'@borqs')>0 or instr(login_email3,'@borqs')>0)";
        if (type.equals("d1"))
            sql += " and user_id in (select user_id from " + departmentUserTable + " where dept_id='" + dept_id + "')";
        if (type.equals("d0"))
            sql += " and user_id not in (select user_id from " + departmentUserTable + " where dept_id='" + dept_id + "')";
        if (type.equals("r1"))
            sql += " and user_id in (select user_id from " + userRoleTable + " where role_id='" + role_id + "')";
        if (type.equals("r0"))
            sql += " and user_id not in (select user_id from " + userRoleTable + " where role_id='" + role_id + "')";
        sql += " and destroyed_time=0 order by display_name";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);

        return rs;
    }

    public RecordSet getUser(String user_ids) {
        String sql = "select user_id,display_name from user2 where user_id in ("+user_ids+") and destroyed_time=0";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        return rs;
    }

    //用户登录后，根据ID获得我的全部信息
    public Record getUserInfo(String user_id) {
        Record rec = new Record();
        rec.put("roles", findRolesByUserId(user_id));
        rec.put("departments", findDeptByUserId(user_id));
        rec.put("my_leaders", findLeaderByUserId(user_id));
        rec.put("my_workmates", findWorkMatesByUserId(user_id));

        return rec;
    }

}
