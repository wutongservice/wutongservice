package com.borqs.elearning.authorization;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

public interface AuthorizationLogic {

     boolean saveDepartment(String dept_name, Record dept_description, int level, long parent_id);

     boolean findDeptName(String dept_name, long parent_id);

     boolean updateDeptName(String dept_id, String dept_name);

     RecordSet findSonDept(String dept_id);

     RecordSet findAllDept(String dept_id);

     Record findOwnDept(String dept_id, boolean get_users);

     Record findFatherDept(String parent_id);

     boolean canDeleteDept(String dept_id);

     boolean deleteDept(String dept_ids);

     boolean saveDepartmentUser(String dept_id, String user_ids, int leader, boolean delete_old);

     boolean deleteDepartmentUser(String dept_id, String user_ids);

     boolean updateDepartmentUserLeader(String dept_id, String user_ids, int leader);

     RecordSet findDeptUsers(String dept_id);

     boolean saveRole(String dept_id, String role_name, String role_description);

     boolean findRoleName(String dept_id, String role_name);

     boolean updateRoleName(String role_id, String role_name);

     boolean deleteRole(String role_id);

     RecordSet findRoleUsers(String role_id);

     Record findRoleById(String role_id, boolean get_users);

     RecordSet findRoleByDeptId(String dept_id);

     boolean deleteUserRole(String role_id, String user_ids);

     boolean saveUserRole(String role_id, String user_ids, boolean delete_old);

     RecordSet findDeptByUserId(String user_id);

     String findParentDeptIdByDeptId(String dept_id);

     RecordSet findRolesByUserId(String user_id);

     RecordSet findLeaderByUserId(String user_id);

     RecordSet findWorkMatesByUserId(String user_id);

     RecordSet findUser(String type, String dept_id, String role_id);

     RecordSet getUser(String user_ids);

     Record getUserInfo(String user_id);


}
