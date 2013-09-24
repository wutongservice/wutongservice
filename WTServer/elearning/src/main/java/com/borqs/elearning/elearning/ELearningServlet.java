package com.borqs.elearning.elearning;

import com.ElearningGlobalLogics;
import com.borqs.elearning.authorization.AuthorizationLogic;
import com.borqs.elearning.elearning.convertor.pdf2SwfCG;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.util.SystemHelper;
import com.borqs.server.base.util.email.AsyncSendMailUtil;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.fileupload.FileItem;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ELearningServlet extends WebMethodServlet {
    public static final String TEMP_ELEARNING_FILE_DIR = SystemHelper.getPathInTempDir("temp_elearning_file");
    public static String bucketName = "wutong-data";
    public static String bucketName_static_file_key = "files/" ;
    private static final Logger L = Logger.getLogger(ELearningServlet.class);
    @Override
    public void init() throws ServletException {
        super.init();
    }


   //add a new course
    @WebMethod("elearning/course/add")
    public boolean addCourse(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws  UnsupportedEncodingException {
        Record record = new Record();
        FileItem file_item = null;
        if(qp.get("file") != null) {
            try {
                file_item = qp.checkGetFile("file");
            } catch (Exception e) {}
        }
        if(file_item != null && file_item.getSize() > 0) {
            //exist file, need to upload
            String fileName = file_item.getName().substring(file_item.getName().lastIndexOf("\\") + 1, file_item.getName().length());
            String expName = "";
            if (fileName.contains(".")) {
                expName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
            }
            String new_file_name = Long.toString(RandomUtils.generateId()) + "." + expName;
            try {
                new_file_name = uploadFile(file_item, new_file_name);
            } catch (IOException e) {
            }
            if(new_file_name != null) {
                record.put("attachment_real_name", new_file_name);
                record.put("attachment_orig_name", file_item.getName());
            } else {
                System.out.println("elearning upload file failed !");
                return false;
            }
        }
        String course_name = qp.getString("course_name", "");
        int approve = (int)qp.getInt("approve", 0);
        String course_code = qp.getString("course_code", "");
        String duration = qp.getString("duration", "");
        String course_desc = qp.getString("course_desc", "");
        String instructor = qp.getString("instructor", "");
        String instructor_desc = qp.getString("instructor_desc", "");
        String address = qp.getString("address", "");
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

        int course_state = CourseImpl.COURSE_OPEN;//course init state is open.

        record.put("course_name", course_name);
        record.put("course_code", course_code);
        record.put("duration", duration);
        record.put("course_desc", course_desc);
        record.put("instructor", instructor);
        record.put("instructor_desc", instructor_desc);
        record.put("schedule", schedule);
        record.put("approve", approve);
        record.put("address", address);
        record.put("category", category);
        record.put("audience_num", audience_num);
        record.put("course_state", course_state);
        return ElearningGlobalLogics.getCourse().addCourse(record);
    }

    //this method modify course info. include course_name, duration, course_desc, instructor, instructor_desc, schedule, category, audience_num, course_state
    @WebMethod("elearning/course/modify")
    public boolean modifyCourseInfo(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws  UnsupportedEncodingException {
        int modify_type = (int) qp.getInt("modify_type", -1);
        //modify_type 0 mean modify all fields of the course, 1 mean just modify one field.
        if(modify_type == 0) {
            FileItem file_item = null;
            if(qp.get("file") != null) {
                try {
                    file_item = qp.getFile("file");
                } catch (Exception e) {}
            }
            boolean has_attachment = false;
            if(file_item != null && file_item.getSize() > 0) {
                has_attachment = true;
                //exist file, need to upload
                String fileName = file_item.getName().substring(file_item.getName().lastIndexOf("\\") + 1, file_item.getName().length());
                String expName = "";
                if (fileName.contains(".")) {
                    expName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
                }
                String new_file_name = Long.toString(RandomUtils.generateId()) + "." + expName;
                try {
                    new_file_name = uploadFile(file_item, new_file_name);
                } catch (IOException e) {
                }
                if(new_file_name != null) {
                    qp.setString("attachment_real_name", new_file_name);
                    qp.setString("attachment_orig_name", file_item.getName());
                } else {
                    return false;
                }
            }
            return ElearningGlobalLogics.getCourse().updateCourse(qp, has_attachment);
        } else if(modify_type == 1) {
            long course_id = qp.checkGetInt("course_id");
            String name = qp.getString("name_field", "");
            Object value = null;
            boolean legal_field = true;
            if(name.equals("course_name")) {
                value = qp.getString("value_field", "");
            } else if(name.equals("duration")) {
                value = qp.getString("value_field", "");
            } else if(name.equals("course_desc")) {
                value = qp.getString("value_field", "");
            } else if(name.equals("instructor")) {
                value = qp.getString("value_field", "");
            } else if(name.equals("instructor_desc")) {
                value = qp.getString("value_field", "");
            } else if(name.equals("schedule")) {
                value = new Long(qp.getInt("value_field", 0));
            } else if(name.equals("category")) {
                value = qp.getString("value_field", "");
            } else if(name.equals("audience_num")) {
                value = new Integer((int) qp.getInt("value_field", 0));
            } else if(name.equals("course_state")) {
                value = new Integer((int) qp.getInt("value_field", 0));
            } else if(name.equals("video_name")) {
                value = qp.getString("video_name", "");
            } else {
                //in this case, nothing to do.
                legal_field = false;
            }
            if(legal_field) {
                return ElearningGlobalLogics.getCourse().updateCourse(course_id, name, value);
            }
            return false;
        }
        return false;
    }

    //remove course by id
    @WebMethod("elearning/course/delete")
    public boolean removeCourse(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException {
        long course_id = qp.checkGetInt("course_id");
        return ElearningGlobalLogics.getCourse().deleteCourse(course_id);
    }

    //get course info.
    @WebMethod("elearning/course/get")
    public RecordSet findCourseByID(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException {
        int query_type = (int) qp.getInt("query_type", -1);
        //role_value is 0 mean admin, role_value is 1 mean leader, role_value is 2 mean employee
        if(query_type == 0) {
            //get course info by course_id
            long course_id = qp.getInt("course_id", -1);
            return ElearningGlobalLogics.getCourse().findCourseInfo(course_id);
        } else if(query_type == 1) {
            //get all course. sort by create time
            int role_type  = (int) qp.getInt("role_type", -1);
            int user_id    = (int) qp.getInt("user_id", 0);
            //need to get apply record count number.
            boolean opt_approve_count = qp.getBoolean("opt-apply-count", false);
            //sort_type : 0 no sort, 1 sort by schedule, 2 sort by course_state and schedule
            int sort_type  = 2;
            RecordSet result_set = ElearningGlobalLogics.getCourse().getAllCourses(role_type, user_id, sort_type, opt_approve_count);
            //get all available course. available mean course state is open.
            return result_set;
        } else if(query_type == 2) {
            //get all closed course. don't support now
            return ElearningGlobalLogics.getCourse().findAllClosedCourse();
        }
        return null;//return null is ok ?
    }

    private String uploadFile(FileItem file_item,String real_fileName) throws IOException {
            String file_name = file_item.getName();
            long file_size = file_item.getSize();
            boolean legal_file = true;
            if(file_name == null || file_name.trim().length() == 0) {
                legal_file = false;
            } else {
                file_name = file_name.trim().toLowerCase();
                String suffix = file_name.substring(file_name.lastIndexOf(".") + 1);
                if(!suffix.equals("pdf")) {
                    //just support pdf ?
                    legal_file = false;
                }
            }
            if(file_size <= 0) {
                legal_file = false;
            }
            if(file_size > 10 * 1024 * 1024) {
                //max file size is 10M
                System.out.println("too big file upload. max size is 10M, current file size is ["+file_size+"]");
                legal_file = false;
            }
            if(legal_file) {
                OssSFS ossStorage = new OssSFS(bucketName);
                //  先保存在本地，用完了再删除
                String tmp = "/home/wutong/bpc_web/elearning/files" + File.separator ;
                String tmpFile_pdf = tmp + real_fileName;
                String tmpFile_swf = tmp + real_fileName.replace("pdf", "swf");
                File f0 = new File(tmp);
                if (!f0.exists()) {
                    f0.mkdirs();
                }
                File f = new File(tmpFile_pdf);
                try {
                    file_item.write(f);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                pdf2SwfCG.convertPDF2SWF(tmpFile_pdf, tmpFile_swf);

                try {
                    File fileTmp = new File(tmpFile_swf);
                    FileInputStream fileInputStream = new FileInputStream(fileTmp);
                    L.debug(null,"swf fileTmp.size="+fileTmp.length());
                    SFSUtils.saveToOSS(ossStorage, bucketName_static_file_key + "elearning/" + real_fileName.replace("pdf", "swf"), fileTmp.length(), fileInputStream);
//                    fileTmp.delete();
                } catch (Exception e) {
                    L.debug(null,"fileTmp e="+e);
                }

                try {
                    File fileTmp = new File(tmpFile_pdf);
                    FileInputStream fileInputStream = new FileInputStream(fileTmp);
                    SFSUtils.saveToOSS(ossStorage, bucketName_static_file_key + "elearning/" + real_fileName, fileTmp.length(), fileInputStream);
//                    fileTmp.delete();
                } catch (Exception e) {
                }

//                f.delete();
                L.debug(null,"real_fileName="+real_fileName);
                return real_fileName;
            } else {
                return null;
            }
        }

//    //upload attachment. just support one attachment ?
//    @WebMethod("elearning/file/upload")
//    public boolean uploadAttachment(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws  UnsupportedEncodingException {
//        FileItem file_item = qp.checkGetFile("file");
//        String new_file_name = uploadFile(file_item);
//        if(new_file_name != null) {
//            //update db. set file name, new file name to db.
//
//            return true;
//        }
//        return false;
//    }

    //user apply course.
    @WebMethod("elearning/course/apply")
    public boolean applyCourse(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException {
        long user_id = qp.getInt("user_id", 0);
        long course_id = qp.getInt("course_id", 0);

        Record course0 = ElearningGlobalLogics.getCourse().findCourseInfo(course_id).getFirstRecord();
        int approve = (int)course0.getInt("approve", 0);

        Record record = new Record();
        record.put("course_id", course_id);
        record.put("user_id", user_id);
        record.put("applicant_time", System.currentTimeMillis());
        if (approve==0) {
            record.put("approve_by_leader", 1);
            record.put("leader_approved_email", 1);
        } else {
            record.put("approve_by_leader", 0);
            record.put("leader_approved_email", 0);
        }
        boolean b = ElearningGlobalLogics.getCourse().addCourseApplicant(record);

        if (b && approve==1) {
            //给我的领导发邮件告诉他我申请了
            RecordSet leader_users = ElearningGlobalLogics.getCourse().getMyLeader(String.valueOf(user_id));
            Record from_user = ElearningGlobalLogics.getCourse().getUser(String.valueOf(user_id)).getFirstRecord();
            for (Record rec : leader_users) {
                Record to_user = ElearningGlobalLogics.getCourse().getUser(rec.getString("user_id")).getFirstRecord();
                String to_ = to_user.getString("user_email");//"guo.chen@borqs.com";//
                String title = "E-Learning message for your team member applyed training : " + course0.getString("course_name") +"";
                formatEmailContentAndSend("applyed", true, course0.getString("course_name"), from_user.getString("display_name"), to_user.getString("display_name"),title,to_,course0.getString("start_time"),course0.getString("address"));
            }
        }
        return b;
    }

    //admin or leader approve course apply
    @WebMethod("elearning/course/approve")
    public boolean approveCourse(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException {
        int approve_type = (int) qp.getInt("approve_type", -1);
        long course_id = qp.getInt("course_id", 0);
        String user_id_str = qp.getString("user_list", "");
        List<Long> user_id_list = new ArrayList<Long>();
        if(user_id_str.trim().length() > 0) {
            String[] strs = user_id_str.split(",");
            for(String str : strs) {
                str = str.trim();
                if(str.length() > 0) {
                    try {
                        long id = Long.parseLong(str);
                        user_id_list.add(id);
                    } catch (Exception e) {}
                }
            }
        }
        boolean approve_state = qp.getBoolean("approve_state", false);
        if(approve_type == 0) {
            //leader approve
            return ElearningGlobalLogics.getCourse().updateApproveByLeader(course_id, user_id_list, approve_state);
        } else if(approve_type == 1) {
            //admin approve
            return ElearningGlobalLogics.getCourse().updateApproveByAdmin(course_id, user_id_list, approve_state);
        }
        return false;
    }

    //attendee sign
    @WebMethod("elearning/course/sign")
    public boolean attendeeSign(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException {
        long user_id = qp.getInt("user_id", 0);
        long course_id = qp.getInt("course_id", 0);
        boolean sign_state = qp.getBoolean("sign_state", false);
        return ElearningGlobalLogics.getCourse().updateSigned(course_id, user_id, sign_state);
    }

    @WebMethod("elearning/course/get_user")
    public RecordSet findCourseUser(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException {
        String viewerId = qp.checkGetString("user_id");
        String course_id = qp.getString("course_id", "");
        int type = (int) qp.getInt("type", 1);
        RecordSet out_recs = new RecordSet();
        if (type == 1)
            out_recs = ElearningGlobalLogics.getCourse().findCourseApplicantUserMyTeam1(viewerId,course_id,9);
        if (type == 2)
            out_recs = ElearningGlobalLogics.getCourse().findCourseApplicantUserMyTeam1(viewerId,course_id,0);
        if (type == 3)
            out_recs = ElearningGlobalLogics.getCourse().findCourseApplicantUserMyTeam2(viewerId, course_id,1);

        if (type == 4)
            out_recs = ElearningGlobalLogics.getCourse().findCourseAprovedUser(viewerId,course_id,9);
        if (type == 5)
            out_recs = ElearningGlobalLogics.getCourse().findCourseAprovedUser(viewerId,course_id,0);
        if (type == 6)
            out_recs = ElearningGlobalLogics.getCourse().findCourseAprovedUser(viewerId,course_id,1);

        if (type == 7)
            out_recs = ElearningGlobalLogics.getCourse().findCourseSignedUser(viewerId,course_id,0);
        if (type == 8)
            out_recs = ElearningGlobalLogics.getCourse().findCourseSignedUser(viewerId,course_id,1);
        return out_recs;
    }

    @WebMethod("elearning/course/update_approved_new")
    public boolean updateUserApprovedNew(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException {
        String course_id = qp.checkGetString("course_id");
        Record course0 = ElearningGlobalLogics.getCourse().findCourseInfo(Long.parseLong(course_id)).getFirstRecord();

        String declined_user_ids = qp.checkGetString("declined_user_ids");
        String pending_user_ids = qp.checkGetString("pending_user_ids");
        String approved_user_ids = qp.checkGetString("approved_user_ids");

        String column_name = "";
        String email_state_str="";

        String step = qp.getString("step", "leader");
        if (step.equals("leader"))  {
            column_name = "approve_by_leader";
            email_state_str = "leader_approved_email";
        }
        if (step.equals("admin")) {
            column_name = "approve_by_admin";
            email_state_str = "approve_accept_email";
        }

        if (step.equals("sign")) {
            column_name = "signed";
            email_state_str = "sign_accept_email";
        }


        // 三个列表，一个一个的处理
        if (declined_user_ids.length() > 0) {
            boolean b = ElearningGlobalLogics.getCourse().updateUserApproved(course_id, declined_user_ids, column_name, 9);
            List<String> declined_user_ids0 = StringUtils2.splitList(declined_user_ids, ",", true);
            for (String declined_user_id : declined_user_ids0) {
                int email_state = ElearningGlobalLogics.getCourse().selectEmailState(course_id, declined_user_id, email_state_str);

                Record to_user = ElearningGlobalLogics.getCourse().getUser(declined_user_id).getFirstRecord();
                String to_ = to_user.getString("user_email");//"guo.chen@borqs.com";
                if (email_state==0 || email_state==1){ //之前是pending状态或者同意状态
                     if (!to_.equals("") && b){
                         String title = "E-Learning message for declined of the training:"+course0.getString("course_name");
                         formatEmailContentAndSend(email_state_str, false, course0.getString("course_name"), "", to_user.getString("display_name"), title, to_,course0.getString("start_time"),course0.getString("address"));
                         ElearningGlobalLogics.getCourse().updateUserApproved(course_id, declined_user_id, email_state_str, 9);
                     }
                }


            }
        }

        if (pending_user_ids.length() > 0) {
            boolean b = ElearningGlobalLogics.getCourse().updateUserApproved(course_id, pending_user_ids, column_name, 0);
        }

        if (approved_user_ids.length() > 0) {
            boolean b = ElearningGlobalLogics.getCourse().updateUserApproved(course_id, approved_user_ids, column_name, 1);
            List<String> approved_user_ids0 = StringUtils2.splitList(approved_user_ids, ",", true);
            for (String approved_user_id : approved_user_ids0) {
                int email_state = ElearningGlobalLogics.getCourse().selectEmailState(course_id, approved_user_id, email_state_str);

                Record to_user = ElearningGlobalLogics.getCourse().getUser(approved_user_id).getFirstRecord();
                String to_ = to_user.getString("user_email");//"guo.chen@borqs.com";
                if (email_state == 0 || email_state == 9) { //之前是pending状态或者同意状态
                    if (!to_.equals("") && b) {
                       String title = "E-Learning message for approved of the training:"+course0.getString("course_name");
                       formatEmailContentAndSend(email_state_str, true, course0.getString("course_name"), "", to_user.getString("display_name"), title, to_,course0.getString("start_time"),course0.getString("address"));
                        ElearningGlobalLogics.getCourse().updateUserApproved(course_id, approved_user_id, email_state_str, 1);
                    }
                }
            }
        }
        return true;
    }


    public String formatEmailContentAndSend(String email_type, boolean accept, String course_name, String from_display_name, String to_display_name, String title, String to,String start_time,String location) {
        String content = "";
        if (email_type.equals("applyed")) {
            content="Please approved "+from_display_name+"'s application of the training ("+course_name+")";
        }
        if (email_type.equals("new_course")) {
            content="E-Learning system added new courses of ("+course_name+"). You can pay attention to it. ";
        }
        if (email_type.equals("attend_course")) {
            if (course_name.equalsIgnoreCase("English Training")) {
                content = "The course (" + course_name + ") will start at " + location + ". Please attend the course on time. ";
            } else {
                content = "The course (" + course_name + ") will start on (" + start_time + ") at " + location + ". Please attend the course on time. ";
            }
        }
        if ( email_type.equals("leader_approved_email") && accept) {
            content="Your application of the training Training the trainer("+course_name+") is approved by your team leader. You will receive the formal confirmation email upon the final approval by HR and function head.\n" +
                    "But the final approval will be confirmed upon Management decision.";
        }
        if (email_type.equals("leader_approved_email") && !accept) {
            content="Your application of the training ("+course_name+") is declined by your team leader.";
        }

        if (email_type.equals("approve_accept_email") && accept) {
            content="You are invited to attend the training of ("+course_name+"). Thanks.";
        }
        if (email_type.equals("approve_accept_email") && !accept) {
            content="Thank you for our application on the training course of ("+course_name+").Due to the limited seats,we are sorry to inform that your application is declined and will be reserved for the similar training if it is reorganized.";
        }
        if (email_type.equals("sign_accept_email")) {
            content="Thanks for attending the training of ("+course_name+"). Please fill in the Evaluation Form. ";
        }

        Record mailParams = new Record();
        mailParams.set("display_name", to_display_name);
        mailParams.set("content", content);
        PageTemplate pageTemplate = new PageTemplate(ELearningServlet.class);
        String html = pageTemplate.merge("elearning.ftl", mailParams);
        try {
            AsyncSendMailUtil.sendEmailElearningHTML(title, to, "", html, GlobalConfig.get(), "email.essential", "eng");
        } catch (Exception e) {
        }

        return content;
    }

    //submit a questionnaire.
    @WebMethod("elearning/course/questionnaire")
    public boolean addQuestionnaire(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException {
        long user_id = qp.getInt("user_id", 0);
        long course_id = qp.getInt("course_id", 0);
        long created_time = System.currentTimeMillis();
        //default value is 1, because score can't be zero
        int score1 = (int) qp.getInt("score1", 1);
        int score2 = (int) qp.getInt("score2", 1);
        int score3 = (int) qp.getInt("score3", 1);
        int score4 = (int) qp.getInt("score4", 1);
        int score5 = (int) qp.getInt("score5", 1);
        int score6 = (int) qp.getInt("score6", 1);
        int score7 = (int) qp.getInt("score7", 1);
        int score8 = (int) qp.getInt("score8", 1);
        int score9 = (int) qp.getInt("score9", 1);
        int score10 = (int) qp.getInt("score10", 1);
        int score11 = (int) qp.getInt("score11", 1);
        int score12 = (int) qp.getInt("score12",0);
        int score13 = (int) qp.getInt("score13", 0);
        String remark1 = qp.getString("remark1","");
        String remark2 = qp.getString("remark1","");
        String remark3 = qp.getString("remark3","");
        float average = ((score1 + score2 + score3 + score4 + score5 + score6 + score7 + score8 + score9 + score10) * 70 +score11 * 30)/100;
        NumberFormat formatter   =   NumberFormat.getNumberInstance();
        formatter.setMinimumFractionDigits(1);
        formatter.format(average);
        Record record = new Record();
        record.put("user_id", user_id);
        record.put("course_id", course_id);
        record.put("created_time", created_time);
        record.put("average", average);
        record.put("score1", score1);
        record.put("score2", score2);
        record.put("score3", score3);
        record.put("score4", score4);
        record.put("score5", score5);
        record.put("score6", score6);
        record.put("score7", score7);
        record.put("score8", score8);
        record.put("score9", score9);
        record.put("score10", score10);
        record.put("score11", score11);
        record.put("score12", score12);
        record.put("score13", score13);
        record.put("remark1", remark1);
        record.put("remark2", remark2);
        record.put("remark3", remark3);
        return ElearningGlobalLogics.getCourse().addQuestionnaireRecord(record);
    }

    @WebMethod("elearning/course/questionnaire_get")
    public RecordSet findQuestionnaireRecordsById(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException {
        String course_id = qp.getString("course_id", "0");
        return ElearningGlobalLogics.getCourse().findScoreById(course_id);
    }

    @WebMethod("elearning/course/statistical")
    public RecordSet findStatisticalCourse(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException {
        return ElearningGlobalLogics.getCourse().findStatisticalCourse();
    }

    @WebMethod("elearning/course/joined")
    public RecordSet findIJoinedScoreByUserId(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException {
        String user_id = qp.checkGetString("user_id");
        return ElearningGlobalLogics.getCourse().findIJoinedScoreByUserId(user_id);
    }

    @WebMethod("elearning/course/user_get")
    public Record getUserScore(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException {
        String course_id = qp.checkGetString("course_id");
        String user_id = qp.checkGetString("user_id");
        return ElearningGlobalLogics.getCourse().getUserScore(course_id, user_id);
    }

    @WebMethod("elearning/get_visit")
    public Record getCourseVisitCount(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException, ParseException {
        Record rec =  ElearningGlobalLogics.getCourse().getCourseVisitCount();
        return rec;
    }

    @WebMethod("elearning/get_user_apply_state")
    public RecordSet getBlackBoxApply(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException, ParseException {
        int type=(int)qp.checkGetInt("type") ;
        String course_id=qp.checkGetString("course_id");
        RecordSet recs =  ElearningGlobalLogics.getCourse().getBlackBoxApply(course_id,type);
        return recs;
    }

    @WebMethod("elearning/visit")
    public boolean insertVisit(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException, ParseException {
        return ElearningGlobalLogics.getCourse().visit(qp.checkGetString("user_id"));
    }

    @WebMethod("elearning/timetest")
    public Record timetest(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException, ParseException {
        return ElearningGlobalLogics.getCourse().timetest();
    }

    @WebMethod("elearning/send_email")
    public boolean sendCourseAddEmail(HttpServletRequest req, QueryParams qp) throws  UnsupportedEncodingException, ParseException {
        String course_id = qp.checkGetString("course_id");
        Record course0 = ElearningGlobalLogics.getCourse().findCourseInfo(Long.parseLong(course_id)).getFirstRecord();
        AuthorizationLogic simpleAuthorization = ElearningGlobalLogics.getAuth();
        RecordSet borqs_user = simpleAuthorization.findUser("", "", "");
        for (Record rec : borqs_user) {
            Record to_user = ElearningGlobalLogics.getCourse().getUser(rec.getString("user_id")).getFirstRecord();
            String to_ = to_user.getString("user_email");//"guo.chen@borqs.com";//
            String title = "E-Learning message for new training:" + course0.getString("course_name") + "";
            formatEmailContentAndSend("new_course", true, course0.getString("course_name"), "", to_user.getString("display_name"), title, to_,course0.getString("start_time"),course0.getString("address"));
        }
        ElearningGlobalLogics.getCourse().updateCourse(Long.parseLong(course_id),"email_attention",1);
        return true;
    }

    @WebMethod("elearning/send_email_join")
    public boolean sendUserJoinCourseEmail(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException, ParseException {
        String course_id = qp.checkGetString("course_id");
        long DateNow = DateUtils.nowMillis();
        long inDate = qp.checkGetInt("inTime");
        Calendar cal0 = Calendar.getInstance();
        cal0.setTime(new Date());
        int n = cal0.get(Calendar.DAY_OF_WEEK);
        if (DateNow - inDate > (10 * 60 * 1000))
            return false;
        Record course0 = ElearningGlobalLogics.getCourse().findCourseInfo(Long.parseLong(course_id)).getFirstRecord();
        RecordSet borqs_user = ElearningGlobalLogics.getCourse().findCourseSignedUser("", course_id, 0);
        for (Record rec : borqs_user) {
            String to_ = rec.getString("user_email");//"guo.chen@borqs.com";//
            String title = "E-Learning message for remind you attend the training:" + course0.getString("course_name") + "";
            if (n == 4) {
                title += " At Wednesday 17:30~18:30";
            }
            if (n == 5) {
                title += " At Thursday 17:30~18:30";
            }
            formatEmailContentAndSend("attend_course", true, course0.getString("course_name"), "", rec.getString("display_name"), title, to_, course0.getString("start_time"), course0.getString("address"));
        }
        return true;
    }
}