package com.borqs.elearning.elearning;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.web.QueryParams;

import java.text.ParseException;
import java.util.List;

public interface CourseLogic {

    boolean addCourse(Record record);

    boolean deleteCourse(long course_id);

    RecordSet findCourseInfo(long course_id);

    RecordSet findAllCourse(int sort_type);

    RecordSet findAllAvailableCourses();

    RecordSet findAllClosedCourse();

    boolean updateCourseAttachment(long course_id, String file_orig_name, String file_real_name);

    boolean updateCourse(QueryParams qp, boolean has_attachment);

    boolean updateCourse(long course_id, String name, Object value);

    boolean addCourseApplicant(Record record);

    boolean deleteCourseApplicant(long course_id, long user_id);

    Record findCourseApplicant(long course_id, long user_id);

    RecordSet getApplyCourseList(long user_id);

    boolean iJoinedCourse(long user_id, long course_id);

    RecordSet getAllCourses(int role_type, long user_id, int sort_type, boolean opt_approve_count);

    int getApplicantCountBYCourseId(String leader_id, String course_id);

    RecordSet getApplicantNumMap();

    boolean userFeedBack(String course_id, String user_id);

    RecordSet findCourseApplicantUserMyTeam1(String viewerId, String course_id, int value);

    RecordSet findCourseApplicantUserMyTeam2(String viewerId, String course_id, int value);

    RecordSet findCourseAprovedUser(String viewerId, String course_id, int value);

    RecordSet findCourseSignedUser(String viewerId, String course_id, int value);

    boolean updateUserApproved(String course_id, String user_ids, String column_name, int value);

    int selectEmailState(String course_id, String user_id, String column_name);

    boolean updateApproveByLeader(long course_id, List<Long> user_ids, boolean isApprove);

    boolean updateApproveByAdmin(long course_id, List<Long> user_ids, boolean isApprove);

    boolean updateSigned(long course_id, long user_id, boolean is_signed);

    boolean addQuestionnaireRecord(Record record);

    boolean deleteQuestionnaireRecord(long course_id, long user_id);

    RecordSet findQuestionnaireRecordsById(long course_id);

    RecordSet getUser(String user_ids);

    RecordSet findStatisticalCourse();

    RecordSet getUserNum(String course_id);

    RecordSet getMyLeader(String user_id);

    RecordSet getAllScore(String course_id);

    Record getUserScore(String course_id, String user_id);

    RecordSet findScoreById(String course_id);

    RecordSet findIJoinedScoreByUserId(String user_id);

    boolean findUserSignState(String course_id, String user_id);

    boolean visit(String user_id);

    Record getCourseVisitCount() throws ParseException;

    long visitGetCount(long since, long max);

    Record timetest();
    RecordSet getBlackBoxApply(String course_id,int type);

}
