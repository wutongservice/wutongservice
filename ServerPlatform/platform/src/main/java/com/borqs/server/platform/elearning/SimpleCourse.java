package com.borqs.server.platform.elearning;

import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.platform.authorization.SimpleAuthorization;
import com.borqs.server.service.platform.Platform;
import org.codehaus.plexus.interpolation.util.StringUtils;
import org.jboss.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SimpleCourse extends ConfigurableBase {

    private static final Logger L = LoggerFactory.getLogger(SimpleCourse.class);
    public final Schema course_schema = Schema.loadClassPath(SimpleCourse.class, "course.schema");
    public final Schema course_applicant_schema = Schema.loadClassPath(SimpleCourse.class, "course_applicant.schema");
    public final Schema questionnaire_record_schema = Schema.loadClassPath(SimpleCourse.class, "questionnaire_record.schema");

    private ConnectionFactory connectionFactory;
    private String db;
    private String course_table;
    private String course_applicant_table;
    private String questionnaire_record_table;
    private String course_visit_count_table;

    public static final int COURSE_OPEN = 0;
    public static final int COURSE_PAUSE = 1;
    public static final int COURSE_CLOSED = 2;

    private Configuration conf;

    public void init() {
        conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf
                .getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.course_table = conf.getString("account.simple.courseTable", "course");
        this.course_applicant_table = conf.getString("account.simple.courseApplicantTable", "course_applicant");
        this.questionnaire_record_table = conf.getString("account.simple.questionnaireRecordTable", "questionnaire_record");
        this.course_visit_count_table = conf.getString("account.simple.courseVisitCountTable", "course_visit_count");
    }


    public void destroy() {
        this.course_table = null;
        this.course_applicant_table = null;
        this.questionnaire_record_table = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    //add a new course
    public boolean addCourse(Record record) {
        Schemas.standardize(course_schema, record);
        final String template = "INSERT INTO ${table} ${values_join(alias, info)}";
        String sql = SQLTemplate.merge(
                template,
                "table",
                course_table,
                "alias",
                course_schema.getAllAliases(),
                "info",
                record);
        L.debug("elearning/course/add,sql="+sql);
        SQLExecutor executor = getSqlExecutor();
        long n = executor.executeUpdate(sql);
        L.debug("elearning/course/add,sql n="+n);
        return n > 0;
    }

    //remove course
    public boolean deleteCourse(long course_id) {
        String sql = "delete from " + course_table + " where course_id=" + course_id;
        SQLExecutor executor = getSqlExecutor();
        long n = executor.executeUpdate(sql);
        return n > 0;
    }

    //find course info
    public RecordSet findCourseInfo(long course_id) {
        String sql = "select * from " + course_table + " where course_id=" + course_id + "";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql, null);
        for (Record rec : rs) {
            rec = formatFileUrl(rec);
        }
        return rs;
    }

    private String formatDate(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm");
        String sd = sdf.format(new Date(Long.parseLong(time)));
        return sd;
    }

    private Record formatFileUrl(Record rec) {
        rec.put("start_time", formatDate(rec.getString("schedule")));
        if (rec.getString("attachment_real_name").length() > 3) {
            rec.put("file_url", String.format(conf.checkGetString("platform.fileUrlPattern") + Platform.bucketName + "/" + Platform.bucketName_static_file_key + "elearning/" + rec.getString("attachment_real_name").replace(".pdf",".swf")));
        } else {
            rec.put("file_url", "");
        }
        return rec;
    }

    //get all course, sort by sort_type
    public RecordSet findAllCourse(int sort_type) {
        //sort_type :
        // 0 is default
        // 1 is sort by schedule.
        // 2 is sort by course state and schedule. open->pause->closed

        String sql = "select * from " + course_table;
        String order_by = null;
        if(sort_type == 1) {
            order_by = " ORDER BY schedule DESC";
        } else if(sort_type == 2) {
            order_by = " ORDER BY course_state ASC, schedule ASC";
        } else {
            //sort_type is 0 or other un-supported type
        }
        sql = sql + order_by;
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql, null);
        for (Record rec : rs) {
            rec = formatFileUrl(rec);
        }
        return rs;
    }

    //get all course where course state is open and sort by schedule
    public RecordSet findAllAvailableCourses() {
        String sql = "select * from " + course_table + " where course_state=" + COURSE_OPEN + " ORDER BY schedule DESC";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql, null);
        for (Record rec : rs) {
            rec = formatFileUrl(rec);
        }
        return rs;
    }

    //get all course where course state is closed
    public RecordSet findAllClosedCourse() {
        String sql = "select * from " + course_table + " where course_state=" + COURSE_CLOSED + " ORDER BY schedule DESC";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql, null);
        for (Record rec : rs) {
            rec = formatFileUrl(rec);
        }
        return rs;
    }

    //update attachment file name, when file name is null or empty, mean this course don't have any attachment
    public boolean updateCourseAttachment(long course_id, String file_orig_name, String file_real_name) {
        String sql = "update " + course_table + " set attachment_orig_name =" + file_orig_name + ", attachment_real_name='" + file_real_name +
                "' where course_id=" + course_id;
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    public String replaceE(String instr){
        instr = instr.replace("\"","“");
        instr = instr.replace("'","’");
        instr = instr.replace("%","");
        instr = instr.replace("^","");
        return instr;
    }


    //replace all course info
    public boolean updateCourse(QueryParams qp, boolean has_attachment) {
        if(qp == null) {
            return false;
        }
        long course_id = qp.getInt("course_id", -1);
        String course_name = replaceE(qp.getString("course_name", ""));
        String course_code = replaceE(qp.getString("course_code", ""));
        String duration = replaceE(qp.getString("duration", ""));
        int approve = (int)qp.getInt("approve",0);
        String course_desc = replaceE(qp.getString("course_desc", ""));
        String instructor = replaceE(qp.getString("instructor", ""));
        String instructor_desc = replaceE(qp.getString("instructor_desc", ""));
        String address = replaceE(qp.getString("address", ""));
        String file_orig_name = null;
        String file_real_name = null;
        if(has_attachment) {
            file_orig_name = qp.getString("attachment_orig_name", "");
            file_real_name = qp.getString("attachment_real_name", "");
        }
        if(course_id < 0) {
            return false;
        }
        long schedule = 0;
        String schedule_str = qp.getString("schedule", "");//schedule value is a int value
        try{
            String date_str = schedule_str.split("-")[0];
            String time_str = schedule_str.split("-")[1];
            String[] d_strs = date_str.split("/");
            int month = Integer.parseInt(d_strs[0]);
            int day   = Integer.parseInt(d_strs[1]);
            int year  = Integer.parseInt(d_strs[2]);
            String[] t_strs = time_str.split(":");
            int hour   = Integer.parseInt(t_strs[0]);
            int minute = Integer.parseInt(t_strs[1]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            Date date = new Date(calendar.getTimeInMillis());
            schedule = date.getTime();
        } catch (Exception e) {
            System.out.println("error data format by ["+schedule_str+"]");
            e.printStackTrace();
        }
        String category = qp.getString("category", "");
        int audience_num = (int) qp.getInt("audience_num", 0);
        int course_state = (int) qp.getInt("course_state", 0);
        StringBuffer sql_buf = new StringBuffer();
        sql_buf.append("update " + course_table + " set ");
        sql_buf.append("course_name='" + course_name + "', ");
        sql_buf.append("course_code='" + course_code + "', ");
        sql_buf.append("duration=" + duration + ", " );
        sql_buf.append("approve=" + approve + ", " );
        sql_buf.append("course_desc='" + course_desc + "', ");
        sql_buf.append("instructor='" + instructor + "', ");
        sql_buf.append("instructor_desc='" + instructor_desc + "', ");
        sql_buf.append("address='" + address + "', ");
        sql_buf.append("schedule=" + schedule + ", ");
        sql_buf.append("category='" + category + "', ");
        sql_buf.append("audience_num=" + audience_num + ", ");
        sql_buf.append("course_state=" + course_state + " ");
        if(has_attachment) {
            sql_buf.append(", ");
            sql_buf.append("attachment_orig_name='" + file_orig_name + "', ");
            sql_buf.append("attachment_real_name='" + file_real_name + "' ");
        }
        sql_buf.append(" where course_id=" + course_id);
        L.debug("elearning/course/modify,sql="+sql_buf.toString());
        SQLExecutor se = getSqlExecutor();
        System.out.println("execute sql : ");
        System.out.println(sql_buf.toString());
        long n = se.executeUpdate(sql_buf.toString());
        L.debug("elearning/course/modify,sql n="+n);
        return n > 0;
    }

    //update course, include : course_name, duration, course_desc, instructor, instructor_desc, schedule, audience_num, course_state,
    // attachment_orig_name, attachment_real_name
    public boolean updateCourse(long course_id, String name, Object value) {
        if(name == null) {
            return false;
        }
        String new_value = null;
        if(value instanceof String) {
            new_value = "'" + value + "'";
        } else if(value instanceof Integer) {
            new_value = ((Integer) value).toString();
        } else if(value instanceof Long) {
            new_value = ((Long) value).toString();
        } else {
            //error data type, just return false
            return false;
        }
        String sql = "update " + course_table + " set " + name + "=" + new_value + " where course_id=" + course_id;
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    //add new course applicant
    public boolean addCourseApplicant(Record record) {
        Schemas.standardize(course_applicant_schema, record);
        final String template = "INSERT INTO ${table} ${values_join(alias, info)}";
        String sql = SQLTemplate.merge(
                template,
                "table",
                course_applicant_table,
                "alias",
                course_applicant_schema.getAllAliases(),
                "info",
                record);
        SQLExecutor executor = getSqlExecutor();
        long n = executor.executeUpdate(sql);
        return n > 0;
    }

    //delete course applicant by course_id and user_id
    public boolean deleteCourseApplicant(long course_id, long user_id) {
        String sql = "delete from " + course_applicant_table + " where course_id=" + course_id + " and user_id=" + user_id;
        SQLExecutor executor = getSqlExecutor();
        long n = executor.executeUpdate(sql);
        return n > 0;
    }

    //query course_applicant
    public Record findCourseApplicant(long course_id, long user_id) {
        String sql = "select * from " + course_table + " where course_id=" + course_id + " and user_id=" + user_id;
        SQLExecutor se = getSqlExecutor();
        Record rs = se.executeRecord(sql, null);
        rs = formatFileUrl(rs);
        return rs;
    }

    public RecordSet getApplyCourseList(long user_id) {
        String sql = "select course_id, signed from " + course_applicant_table + " where user_id="+user_id;
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql, null);
        return rs;
    }

    public boolean iJoinedCourse(long user_id, long course_id) {
        String sql = "select course_id from " + course_applicant_table + " where user_id='" + user_id + "' and course_id='" + course_id + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql, null);
        return rs.size() > 0 ? true : false;
    }

    //query all course info and other statistics data
    //statistics data include apply count, applicant count and signed count
    public RecordSet getAllCourses(int role_type, long user_id, int sort_type, boolean opt_approve_count) {
        RecordSet r_set = this.findAllCourse(sort_type);
        //add applicant num to RecordSet
        if(role_type == 0 || role_type == 1 || role_type==2) {
            //admin or leader want to get course list
            if(r_set != null && r_set.size() > 0) {
                Map<Long, Integer> applicant_map = new HashMap<Long, Integer>();
                RecordSet applicant_num_map = this.getApplicantNumMap();
                if(applicant_num_map != null && applicant_num_map.size() > 0) {
                    for(int i=0;i<applicant_num_map.size();i++) {
                        Record record = applicant_num_map.get(i);
                        long course_id = record.getInt("course_id");
                        int count = (int) record.getInt("count(user_id)");
                        applicant_map.put(course_id, count);
                    }
                }
                for(Record r : r_set) {
                    if(r != null) {
                        long course_id = r.getInt("course_id");
                        if(applicant_map.get(course_id) != null &&  applicant_map.get(course_id) > 0) {
                            r.put("applicant_count", applicant_map.get(course_id));
                        } else {
                            r.put("applicant_count", 0);
                        }
                    }
                }
            }
        }

        //add apply state to RecordSet
        //add signed state
        if(role_type == 1 || role_type == 2) {
            //employee and leader, need to get course_id list which is already apply
            if(r_set != null && r_set.size() > 0) {
                RecordSet r_st = this.getApplyCourseList(user_id);
                Map<Long, Integer> course_signed_map = new HashMap<Long, Integer>();
                if(r_st != null && r_st.size() > 0) {
                    for(Record r : r_st) {
                        long course = r.getInt("course_id");
                        int signed = (int) r.getInt("signed");
                        course_signed_map.put(course, signed);
                    }
                }
                for(Record r : r_set) {
                    if(r != null) {
                        long course_id = r.getInt("course_id");
                        if(course_signed_map.containsKey(course_id)) {
                            r.put("apply_state", true);
                        } else {
                            r.put("apply_state", false);
                        }
                        Integer signed = course_signed_map.get(course_id);
                        if(signed == null) {
                            signed = 0;
                        }
                        if(signed == 0) {
                            r.put("signed_state", false);
                        } else {
                            r.put("signed_state", true);
                        }
                    }
                }
            }
        }
        for (Record rec : r_set) {
            rec.put("feed_back", userFeedBack(rec.getString("course_id"), String.valueOf(user_id)));
            //     增加一个显示我这个团队有多少人报名
            int apply_count = getApplicantCountBYCourseId(String.valueOf(user_id), rec.getString("course_id"));
            rec.put("apply_count", apply_count);
            //     增加一个显示我这个团队有多少人等待我审批
            RecordSet rs = findCourseApplicantUserMyTeam1(String.valueOf(user_id), rec.getString("course_id"), 0);
            rec.put("wait_count", rs.size());
            rec.put("i_joined", iJoinedCourse(user_id,Long.parseLong(rec.getString("course_id"))));
        }
        for (Record rec : r_set) {
            rec = formatFileUrl(rec);
        }
        return r_set;
    }

    public int getApplicantCountBYCourseId(String leader_id, String course_id) {
        SQLExecutor se = getSqlExecutor();
        String sql0 = "SELECT DISTINCT(user_id) FROM department_user WHERE user_id<>'" + leader_id + "' and dept_id IN (SELECT dept_id FROM department_user WHERE user_id='" + leader_id + "' and leader=1)";
        RecordSet rs0 = se.executeRecordSet(sql0.toString(), null);
        String other_ids = rs0.joinColumnValues("user_id", ",");
        if (other_ids.length() > 0) {
            String sql = "select distinct(user_id) from " + course_applicant_table + " where course_id=" + course_id + " and approve_by_leader in (0,1,9) and approve_by_admin=0 and signed=0" +
                    " and user_id in (" + other_ids + ")";
            RecordSet rs = se.executeRecordSet(sql.toString(), null);
            return rs.size();
        } else {
            return 0;
        }
    }

    public RecordSet getApplicantNumMap() {
        String sql = "select count(user_id), course_id from " + course_applicant_table + " group by course_id";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql, null);
        return rs;
    }

    public boolean userFeedBack(String course_id, String user_id) {
        String sql = "select course_id from " + questionnaire_record_table + " where user_id='" + user_id + "' and course_id='" + course_id + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql, null);
        return rs.size() > 0 ? true : false;
    }

    public RecordSet findCourseApplicantUserMyTeam1(String viewerId, String course_id,int value) {
        SQLExecutor se = getSqlExecutor();
        String sql0 = "SELECT DISTINCT(user_id) FROM department_user WHERE user_id<>'"+viewerId+"' and dept_id IN (SELECT dept_id FROM department_user WHERE user_id='" + viewerId + "' and leader=1)";
        RecordSet rs0 = se.executeRecordSet(sql0.toString(), null);
        String other_ids = rs0.joinColumnValues("user_id", ",");
        if (other_ids.length() > 0) {
            String sql = "select distinct(user_id) from " + course_applicant_table + " where course_id=" + course_id + " and approve_by_leader="+value+" and approve_by_admin=0 and signed=0" +
                    " and user_id in ("+other_ids+")";

            RecordSet rs = se.executeRecordSet(sql.toString(), null);
            String userIds = rs.joinColumnValues("user_id", ",");
            RecordSet users = getUser(userIds);
            return users;
        } else {
            return new RecordSet();
        }
    }

    public RecordSet findCourseApplicantUserMyTeam2(String viewerId, String course_id,int value) {
        SQLExecutor se = getSqlExecutor();
        String sql0 = "SELECT DISTINCT(user_id) FROM department_user WHERE user_id<>'"+viewerId+"' and  dept_id IN (SELECT dept_id FROM department_user WHERE user_id='" + viewerId + "' and leader=1)";
        RecordSet rs0 = se.executeRecordSet(sql0.toString(), null);
        String other_ids = rs0.joinColumnValues("user_id", ",");
        if (other_ids.length() > 0) {
            String sql = "select distinct(user_id) from " + course_applicant_table + " where course_id=" + course_id + " and approve_by_leader="+value+" and approve_by_admin=0 and signed=0" +
                    " and user_id in ("+other_ids+") order by applicant_time";

            RecordSet rs = se.executeRecordSet(sql.toString(), null);
            String userIds = rs.joinColumnValues("user_id", ",");
            RecordSet users = getUser(userIds);
            return users;
        } else {
            return new RecordSet();
        }
    }

    public RecordSet findCourseAprovedUser(String viewerId,String course_id,int value) {
        String sql = "select distinct(user_id) from " + course_applicant_table + " where course_id=" + course_id + " and approve_by_leader=1 and approve_by_admin="+value+" and signed=0 order by applicant_time";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        String userIds = rs.joinColumnValues("user_id",",");
        RecordSet users = getUser(userIds);
        return users;
    }

    public RecordSet findCourseSignedUser(String viewerId,String course_id,int value) {
        String sql = "select distinct(user_id) from " + course_applicant_table + " where course_id=" + course_id + " and approve_by_leader=1 and approve_by_admin=1 and signed="+value+" order by applicant_time";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql.toString(), null);
        String userIds = rs.joinColumnValues("user_id", ",");
        RecordSet users = getUser(userIds);
        return users;
    }

    public boolean updateUserApproved(String course_id,String user_ids,String column_name,int value) {
        String sql = "update " + course_applicant_table + " set "+column_name+" = "+value+" where course_id=" + course_id + " and user_id in ("+user_ids+")";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    public int selectEmailState(String course_id, String user_id, String column_name) {
        String sql = "select " + column_name + " as email_state from " + course_applicant_table + " where course_id=" + course_id + " and user_id='" + user_id + "' ";
        SQLExecutor se = getSqlExecutor();
        Record rs = se.executeRecord(sql.toString(), null);
        int email_state = 0;
        if (!rs.isEmpty()) {
            email_state = (int) rs.getInt("email_state", 0);
        }
        return email_state;
    }


    //update leader approve state value
    public boolean updateApproveByLeader(long course_id, List<Long> user_ids, boolean isApprove) {
        if(user_ids == null || user_ids.size() == 0) {
            return false;
        }
        List<String> sql_list = new ArrayList<String>();
        for(Long id : user_ids) {
            if(id != null) {
                long l = id;
                String sql = "update " + course_applicant_table + " set approve_by_leader=" + isApprove + " where course_id=" + course_id + " and user_id=" + l;
                sql_list.add(sql);
            }
        }
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql_list);
        return n > 0;
    }

    //update admin approve state value
    public boolean updateApproveByAdmin(long course_id, List<Long> user_ids, boolean isApprove) {
        if(user_ids == null || user_ids.size() == 0) {
            return false;
        }
        List<String> sql_list = new ArrayList<String>();
        for(Long id : user_ids) {
            if(id != null) {
                long l = id;
                String sql = "update " + course_applicant_table + " set approve_by_admin=" + isApprove + " where course_id=" + course_id + " and user_id=" + l;
                sql_list.add(sql);
            }
        }
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql_list);
        return n > 0;
    }

    //update signed state value
    public boolean updateSigned(long course_id, long user_id, boolean is_signed) {
        String sql = "update " + course_applicant_table + " set approve_by_admin=" + is_signed + " where course_id=" + course_id + " and user_id=" + user_id;
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    //add questionnaire record
    public boolean addQuestionnaireRecord(Record record) {
        Schemas.standardize(questionnaire_record_schema, record);
        final String template = "INSERT INTO ${table} ${values_join(alias, info)}";
        String sql = SQLTemplate.merge(
                template,
                "table",
                questionnaire_record_table,
                "alias",
                questionnaire_record_schema.getAllAliases(),
                "info",
                record);
        SQLExecutor executor = getSqlExecutor();
        long n = executor.executeUpdate(sql);
        return n > 0;
    }

    //delete questionnaire record
    public boolean deleteQuestionnaireRecord(long course_id, long user_id) {
        String sql = "delete from " + questionnaire_record_table + " where course_id=" + course_id + " and user_id=" + user_id;
        SQLExecutor executor = getSqlExecutor();
        long n = executor.executeUpdate(sql);
        return n > 0;
    }

    //TODO : questionnaire record don't support modify.

    //get all questionnaire records by course_id
    public RecordSet findQuestionnaireRecordsById(long course_id) {
        String sql = "select * from " + questionnaire_record_table + " where course_id=" + course_id;
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql, null);
        return rs;
    }

    public RecordSet getUser(String user_ids) {
        if (user_ids.length() > 0) {
           List<String> user_ids0 = StringUtils2.splitList(user_ids, ",", true);
            String sql = "select user_id,display_name,login_email1,login_email2,login_email3 from user2 where user_id in (" + user_ids + ") and destroyed_time=0";
            SQLExecutor se = getSqlExecutor();
            RecordSet rs = se.executeRecordSet(sql.toString(), null);
            for (Record rec : rs) {
                if (!rec.getString("login_email1").equals("")) {
                    rec.put("user_email", rec.getString("login_email1"));
                    continue;
                } else if (!rec.getString("login_email2").equals("")) {
                    rec.put("user_email", rec.getString("login_email2"));
                    continue;
                } else if (!rec.getString("login_email3").equals("")) {
                    rec.put("user_email", rec.getString("login_email3"));
                    continue;
                } else {
                    rec.put("user_email", "");
                }
                rec.removeColumns("login_email1", "login_email2", "login_email3");
            }

            RecordSet out_recs = new RecordSet();

            for (String uid : user_ids0) {
                for (Record record : rs) {
                    if (record.getString("user_id").equals(uid)) {
                        out_recs.add(record);
                        break;
                    }
                }
            }
            return out_recs;
        } else {
            return new RecordSet();
        }
    }

    public RecordSet findStatisticalCourse() {
        SQLExecutor se = getSqlExecutor();
        String sql0 = "SELECT * FROM "+course_table+" ORDER BY course_state asc, schedule asc";
        RecordSet recs = se.executeRecordSet(sql0.toString(), null);
        for (Record rec : recs){
            String course_id = rec.getString("course_id");
            int applyed_user_num =0;
            int leader_approved_user_num =0;
            int addmin_approved_user_num =0;
            int signed_user_num =0;
            RecordSet users_num =  getUserNum(course_id);
            applyed_user_num =  users_num.size();
            for (Record rec0 : users_num) {
                if (rec0.getInt("approve_by_leader") == 1)
                    leader_approved_user_num += 1;
                if (rec0.getInt("approve_by_admin") == 1)
                    addmin_approved_user_num += 1;
                if (rec0.getInt("signed") == 1)
                    signed_user_num += 1;
            }

            //1,找到当前报名总数
            rec.put("applyed_user_num",applyed_user_num);
            //2,找到当前teamLeader批准的总数
            rec.put("leader_approved_user_num",leader_approved_user_num);
            //3,找到最终批准的总数
            rec.put("addmin_approved_user_num",addmin_approved_user_num);
            //4,找到最终签到的总数
            rec.put("signed_user_num",signed_user_num);
            //5,汇总打分的总分
             RecordSet all_score = getAllScore(course_id);
            //6,算出打分人数
            rec.put("evaluation_user_num",all_score.size());

            //7,算出平均总分(总数除以打分人数)
            double average_score_sum=0.0;
            if (all_score.size() > 0) {
                for (Record rec_s : all_score) {
                    int all_1 = (int) (rec_s.getInt("score1") + rec_s.getInt("score2") + rec_s.getInt("score3") + rec_s.getInt("score4") + rec_s.getInt("score5") + rec_s.getInt("score6") + rec_s.getInt("score7") + rec_s.getInt("score8") + rec_s.getInt("score9") + rec_s.getInt("score10"));
                    int all_2 = (int) rec_s.getInt("score11");
                    average_score_sum = average_score_sum + (all_1 * 7  + all_2 * 30);
                }
            }
            //7,算出平均分项分(总数除以打分人数)
            double average_score = 0.0f;
            if (all_score.size() > 0)
                average_score = average_score_sum / all_score.size() ;
            rec.put("average_score", average_score);
        }
        return recs;
    }


    public RecordSet getUserNum(String course_id) {
        SQLExecutor se = getSqlExecutor();
        String sql0 = "SELECT approve_by_leader,approve_by_admin,signed FROM " + course_applicant_table + " where course_id='" + course_id + "'";
        RecordSet recs = se.executeRecordSet(sql0.toString(), null);
        return recs;
    }

    public RecordSet getMyLeader(String user_id) {
        SQLExecutor se = getSqlExecutor();
         RecordSet out_recs = new RecordSet();
        //先看看 我本身是不是leader
         String sql00 = "SELECT user_id FROM department_user WHERE leader=1 and user_id='"+user_id+"'";
        RecordSet recs0 = se.executeRecordSet(sql00.toString(), null);
        if (recs0.size() == 0) {   //本身不是，就找本部门的其他leader
            String sql0 = "SELECT DISTINCT(user_id) FROM department_user WHERE user_id<>'" + user_id + "' and leader=1 and dept_id IN (SELECT dept_id FROM department_user WHERE user_id='" + user_id + "' and leader=0)";
            out_recs = se.executeRecordSet(sql0.toString(), null);
        } else {
            //本身是leader，看看是不是另一个部门的员工，如果是，就要找那个部门的leader
            //本身是leader，看看是不是另一个部门的员工，如果不是，还是找本部门的其他leader
            String sql01 = "SELECT user_id FROM department_user WHERE user_id='" + user_id + "' AND leader=0";
            RecordSet recs01 = se.executeRecordSet(sql01.toString(), null);
            if (recs01.size() > 0) {
                String sql02 = "SELECT DISTINCT(user_id) FROM department_user  WHERE user_id<>'" + user_id + "' AND leader=1 AND dept_id IN (SELECT dept_id FROM department_user WHERE user_id='" + user_id + "' AND leader=0)";
                out_recs = se.executeRecordSet(sql02.toString(), null);
            } else {
                String sql02 = "SELECT DISTINCT(user_id) FROM department_user  WHERE user_id<>'" + user_id + "' AND leader=1 AND dept_id IN (SELECT dept_id FROM department_user WHERE user_id='" + user_id + "' AND leader=1)";
                out_recs = se.executeRecordSet(sql02.toString(), null );
            }
        }
        return out_recs;
    }

    public RecordSet getAllScore(String course_id) {
        SQLExecutor se = getSqlExecutor();
//        String sql0 = "SELECT score1+score2+score3+score4+score5+score6+score7+score8+score9+score10+score11+score12+score13 AS rows_sum FROM " + questionnaire_record_table + " where course_id='" + course_id + "'";
        String sql0 = "SELECT score1,score2,score3,score4,score5,score6,score7,score8,score9,score10,score11 FROM " + questionnaire_record_table + " where course_id='" + course_id + "'";
        RecordSet recs = se.executeRecordSet(sql0.toString(), null);
        return recs;
    }

    public Record getUserScore(String course_id,String user_id) {
        SQLExecutor se = getSqlExecutor();
        String sql0 = "SELECT * FROM " + questionnaire_record_table + " where course_id='" + course_id + "' and user_id='"+user_id+"'";
        Record rec = se.executeRecord(sql0.toString(), null);
        return rec;
    }

    public RecordSet findScoreById(String course_id) {
        String sql = "select * from " + questionnaire_record_table + " where course_id='" + course_id + "' order by created_time desc";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql, null);
        for (Record rec : rs){
             String user_id=rec.getString("user_id");
             Record user = getUser(user_id).getFirstRecord();
             rec.put("user_name",user.getString("display_name"));
        }
        return rs;
    }

    public RecordSet findIJoinedScoreByUserId(String user_id) {
        String sql = "select * from " + course_table + " where course_id in (select course_id from "+course_applicant_table+" where user_id='"+user_id+"' and approve_by_admin=1) ORDER BY course_state asc, schedule asc";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql, null);
        for (Record rec : rs) {
            rec.put("feed_back", userFeedBack(rec.getString("course_id"), String.valueOf(user_id)));
            rec.put("signed_state", findUserSignState(rec.getString("course_id"), String.valueOf(user_id)));

        }
        return rs;
    }

    public boolean findUserSignState(String course_id, String user_id) {
        String sql = "select signed from " + course_applicant_table + " where user_id='" + user_id + "' and course_id='" + course_id + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql, null);
        if (rs.size() > 0) {
            return rs.getFirstRecord().getBoolean("signed", false);
        } else {
            return false;
        }
    }

    public boolean visit(String user_id) {
        String sql = "insert into " + course_visit_count_table + " (visit_user,visit_time) values ('"+user_id+"',"+ DateUtils.nowMillis()+")";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    //update attachment file name, when file name is null or empty, mean this course don't have any attachment
    public Record getCourseVisitCount() throws ParseException {
         timeGet tt = new timeGet();
        //计算本日的，
        // 当天日期
        long nowDayBegin = formatOwnDate(tt.formatEng(tt.getNowTime("yyyy-MM-dd"))+" 00:00:00");
        long nowDayEnd = nowDayBegin + 24 * 60 * 60 * 1000L;
        //计算本周的
         long nowWeekBegin = formatOwnDate(tt.formatEng(tt.getPreviousWeekSunday())+" 00:00:00");
        long nowWeekEnd = nowWeekBegin + 7 * 24 * 60 * 60 * 1000L;


        //计算本月的
        long nowMonthBegin = formatOwnDate(tt.formatEng(tt.getFirstDayOfMonth())+" 00:00:00");
        long nowMonthEnd = formatOwnDate(tt.formatEng(tt.getDefaultDay())+" 23:59:59");

        //计算本年的
        long nowYearBegin = formatOwnDate(tt.formatEng(tt.getCurrentYearFirst())+" 00:00:00");
        long nowYearEnd = formatOwnDate(tt.formatEng(tt.getCurrentYearEnd())+" 23:59:59");

        Record rec = new Record();
        rec.put("all_count",visitGetCount(0,Long.MAX_VALUE));
        rec.put("day_count",visitGetCount(nowDayBegin,nowDayEnd));
        rec.put("week_count",visitGetCount(nowWeekBegin,nowWeekEnd));
        rec.put("month_count",visitGetCount(nowMonthBegin,nowMonthEnd));
        rec.put("year_count",visitGetCount(nowYearBegin,nowYearEnd));
        return rec;
    }

    private long formatOwnDate(String dateStr) throws ParseException {
        // "yyyy-MM-dd HH:mm:ss"
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = format.parse(dateStr);
        return date.getTime();
    }

    public long visitGetCount(long since,long max) {
        String sql = "select count(*) as count1 from "+course_visit_count_table+" where visit_time>="+since+" and visit_time<="+max+"";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return rec.isEmpty()?0:rec.getInt("count1");
    }

    public RecordSet getBlackBoxApply(String course_id,int type) {
        String sql = "select user_id from "+course_applicant_table+" where course_id='"+course_id+"' and approve_by_leader="+type+" and approve_by_admin=0";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        String user_ids = recs.joinColumnValues("user_id",",");
        RecordSet out_recs = new RecordSet();
        if (user_ids.length()>0)
            out_recs = getUser(user_ids);
        return out_recs;
    }

    public Record timetest() {
        timeGet tt = new timeGet();
        Record rec = new Record();
        try {
            rec.put("t1", "获取当天日期:" + tt.formatEng(tt.getNowTime("yyyy-MM-dd")));
        } catch (Exception e) {
        }

        try {
            rec.put("t2", "获取本周日的日期~:" + tt.formatEng(tt.getPreviousWeekSunday()));
        } catch (Exception e) {
        }

        try {
            rec.put("t5", "获取本月第一天日期:" + tt.formatEng(tt.getFirstDayOfMonth()));
        } catch (Exception e) {
        }

        try {
            rec.put("t6", "获取本月最后一天日期:" + tt.formatEng(tt.getDefaultDay()));
        } catch (Exception e) {
        }

        try {
            rec.put("t3", "获取上周日日期:" + tt.formatEng(tt.getPreviousWeekSunday()));
        } catch (Exception e) {
        }

        try {
            rec.put("t4", "获取下周一日期:" + tt.formatEng(tt.getNextMonday()));
        } catch (Exception e) {
        }

        try {
            rec.put("t7", "获取本年的第一天日期:" + tt.formatEng(tt.getCurrentYearFirst()));
        } catch (Exception e) {
        }

        try {
            rec.put("t8", "获取本年最后一天日期:" + tt.formatEng(tt.getCurrentYearEnd()));
        } catch (Exception e) {
        }

        return rec;
    }

}
