package com.borqs.server.wutong.vacation;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.RandomUtils;
import jxl.Cell;
import jxl.Range;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VacationImpl implements VacationLogic, Initializable {
    private static final Logger L = Logger.getLogger(VacationImpl.class);
    protected final Schema vacationSchema = Schema.loadClassPath(VacationImpl.class, "vacation.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    private String vacation = "vacation";


    public void init() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
    }

    @Override
    public void destroy() {
        connectionFactory.close();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    public boolean setVacation(Context ctx, InputStream inputStream) {
        RecordSet rs = new RecordSet();
        Record errorRecord = new Record();
        try {
            excel2RecordSet(inputStream, rs, errorRecord);
        } catch (BiffException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> sqlList = new ArrayList<String>();
        RecordSet rsError = new RecordSet();
        //拼装Sql
        for (Record r : rs) {
            rsError = checkVacation(rs);
            r.put("id", RandomUtils.generateId());
            r.put("created_time", DateUtils.nowMillis());
            String sql0 = new SQLBuilder.Insert().insertInto(vacation)
                    .values(r).toString();
            sqlList.add(sql0);
        }
        rsError.add(errorRecord);
        L.info(ctx, "------------------------------error record vacation larger than 20------------------------------");
        L.info(ctx, rsError.toString(false, false));
        L.info(ctx, "------------------------------error record vacation larger than 20------------------------------");

        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sqlList);
        return true;
    }

    @Override
    public RecordSet getVacation(Context ctx,String employee) {
        String sql0 = new SQLBuilder.Select().select(" * ")
                .from(this.vacation)
                .where("employee_id=${v(employee)}", "employee", employee)
                .and("destroyed_time=0").toString();
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql0, null);
        Schemas.standardize(vacationSchema, rs);
        return rs;
    }

    private RecordSet checkVacation(RecordSet rs) {
        if (rs == null || rs.size() < 1)
            return new RecordSet();

        RecordSet list = new RecordSet();
        for (Record r : rs) {
            double legal_vacation = Double.parseDouble(r.getString("legal_vacation"));
            double company_vacation = Double.parseDouble(r.getString("company_vacation"));
            double total_vacation = Double.parseDouble(r.getString("total_vacation"));
            double vacation = Double.parseDouble(r.getString("vacation"));
            double remain_vacation = Double.parseDouble(r.getString("remain_vacation"));
            if (legal_vacation > 20 || company_vacation > 20 || total_vacation > 20 || vacation > 20 || remain_vacation > 20)
                list.add(r);

        }
        return list;
    }

    private void excel2RecordSet(InputStream file, RecordSet rs, Record errorRecord) throws BiffException, IOException {
        Workbook workbook = Workbook.getWorkbook(file);
        int sheetSize = workbook.getNumberOfSheets();
        Range[] ranges = null;
        List<String> listYear = new ArrayList<String>();

        for (int i = 0; i < sheetSize; i++) {
            Sheet sheet = workbook.getSheet(i);
            ranges = sheet.getMergedCells();

            for (Range range : ranges) {
                String conent = range.getTopLeft().getContents();
                if (conent.startsWith("20")) {
                    listYear.add(conent);
                }
            }

            Cell cell = null;

            Map<String, List<String>> map = new HashMap<String, List<String>>();
            errorRecord = new Record();

            for (int rowIndex = 2; rowIndex < sheet.getRows(); rowIndex++) {//行
                List<String> list = new ArrayList<String>();

                for (int colIndex = 0; colIndex < sheet.getColumns(); colIndex++) {//列
                    cell = sheet.getCell(colIndex, rowIndex);
                    String content = cell.getContents();

                    list.add(content);
                }
                Record record = new Record();
                if (StringUtils.isBlank(list.get(0)))
                    break;
                if (map.containsKey(list.get(0)))
                    errorRecord.put(list.get(0), "duplicate record!");

                record.put("employee_id", list.get(0));
                record.put("name", list.get(1));
                record.put("join_date", list.get(2));
                record.put("city", list.get(3));
                record.put("company", list.get(4));
                map.put(list.get(0), list);
                rs.addAll(joinHolidayByYear(listYear, list, record));
            }
        }
    }

    /**
     * 拼装个人每年的年假记录
     *
     * @param listYear
     * @param listRecord
     */
    private RecordSet joinHolidayByYear(List<String> listYear, List<String> listRecord, Record listFront) {
        RecordSet rs = new RecordSet();
        int num = listRecord.size() / listYear.size();
        int index = 0;
        for (int i = 5; i < listRecord.size(); i++) {
            if (i % num == 0) {
                int result = i % num + index * num + 5;
                Record r = new Record();
                r.putAll(listFront);
                r.put("year", listYear.get(index++));
                if (StringUtils.isBlank(listRecord.get(result)))
                    continue;
                r.put("legal_vacation", listRecord.get(result));
                r.put("company_vacation", listRecord.get(result + 1));
                r.put("total_vacation", listRecord.get(result + 2));
                r.put("vacation", listRecord.get(result + 3));
                r.put("remain_vacation", listRecord.get(result + 4));
                rs.add(r);
            }
        }
        return rs;
    }

}
