package com.borqs.server.wutong.action.actions.holiday;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.excel.InnovExcel;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.NoResponse;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.commons.WutongContext;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class HoliServlet extends WebMethodServlet {
    private String serverHost = null;
    private String day_end = null;
    private String day_start = null;


    private void initDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 1);
        cal.set(Calendar.MINUTE, 0);
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        day_start = df.format(cal.getTime());

        //当前月＋1，即下个月
        cal.add(cal.MONTH, 1);
        //将下个月1号作为日期初始zhii
        cal.set(cal.DATE, 1);
        //下个月1号减去一天，即得到当前月最后一天
        cal.add(cal.DATE, -1);

        java.text.SimpleDateFormat df2 = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        day_end = df2.format(cal.getTime());
    }


    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();
        serverHost = conf.getString("server.host", "api.borqs.com");

    }

    @WebMethod("holiday/getTotalRecords")
    public RecordSet getTotalRecords(QueryParams qp) {
        initDate();

        Context ctx = WutongContext.getContext(qp, true);
        String owner = qp.getString("owner", "");
        String approver = qp.getString("approver", "");
        String holi_type = qp.getString("holi_type", "");
        String dep = qp.getString("dep", "");
        String holi_status = qp.getString("holi_status", "");

        String apply_time_start = qp.getString("apply_time_start", day_start);
        String apply_time_end = qp.getString("apply_time_end", day_end);

        Record record = new Record();
        record.put("owner", owner);
        record.put("approver", approver);
        record.put("holi_type", holi_type);
        record.put("dep", dep);
        record.put("holi_status", holi_status);
        record.put("apply_time_start", apply_time_start);
        record.put("apply_time_end", apply_time_end);
        HolidayRecordLogic holidayRecordLogic = GlobalLogics.getHolidayRecord();
        return holidayRecordLogic.getTotalRecords(ctx, record);
    }

    @WebMethod("holiday/getDetailRecords")
    public RecordSet getDetialRecords(QueryParams qp) {
        initDate();

        Context ctx = WutongContext.getContext(qp, true);
        String owner = qp.getString("owner", "");
        String approver = qp.getString("approver", "");
        String holi_type = qp.getString("holi_type", "");
        String dep = qp.getString("dep", "");
        String holi_status = qp.getString("holi_status", "");


        String apply_time_start = qp.getString("apply_time_start", day_start);
        String apply_time_end = qp.getString("apply_time_end", day_end);

        Record record = new Record();
        record.put("owner", owner);
        record.put("approver", approver);
        record.put("holi_type", holi_type);
        record.put("dep", dep);
        record.put("holi_status", holi_status);
        record.put("apply_time_start", apply_time_start);
        record.put("apply_time_end", apply_time_end);
        HolidayRecordLogic holidayRecordLogic = GlobalLogics.getHolidayRecord();
        return holidayRecordLogic.getDetailRecords(ctx, record);
    }


    @WebMethod("holiday/getTotalExcel")
    public NoResponse getExcel(QueryParams qp, HttpServletResponse resp, HttpServletRequest req) throws IOException {
        initDate();

        Context ctx = WutongContext.getContext(qp, true);
        String owner = qp.getString("owner", "");
        String approver = qp.getString("approver", "");
        String holi_type = qp.getString("holi_type", "");
        String dep = qp.getString("dep", "");
        String holi_status = qp.getString("holi_status", "");

        String apply_time_start = qp.getString("apply_time_start", day_start);
        String apply_time_end = qp.getString("apply_time_end", day_end);

        Record record = new Record();
        record.put("owner", owner);
        record.put("approver", approver);
        record.put("holi_type", holi_type);
        record.put("dep", dep);
        record.put("holi_status", holi_status);
        record.put("apply_time_start", apply_time_start);
        record.put("apply_time_end", apply_time_end);

        HolidayRecordLogic holidayRecordLogic = GlobalLogics.getHolidayRecord();
        RecordSet rs = holidayRecordLogic.getDetailRecords(ctx, record);

        InnovExcel ie = new InnovExcel();
        List<List<String>> dataList = new ArrayList<List<String>>();
        int n = 1;

        for (Record statRec : rs) {
            // // applicant, department, product, members_names, date, file_url
            dataList.add(Arrays.asList(
                    Integer.toString(n++),
                    statRec.getString("owner"),
                    statRec.getString("dep"),
                    statRec.getString("approver"),
                    //statRec.getString("mate"),
                    statRec.getString("holi_type"),
                    statRec.getString("holi_start_time"),
                    statRec.getString("days")
            ));
        }


        byte[] buff = ie.genInnovSignUpExcel(dataList);
        if (buff != null) {
            resp.setContentType("application/vnd.ms-excel");
            resp.setHeader("Content-Disposition", "attachment; filename=\"HolidaySummary" + DateUtils.formatDate(DateUtils.nowMillis()) + ".xls\"");
            IOUtils.copy(new ByteArrayInputStream(buff), resp.getOutputStream());
        } else {
            resp.setStatus(404);
        }
        return NoResponse.get();
    }
}
