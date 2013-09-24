package com.borqs.server.base.excel;

import com.borqs.server.base.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class InnovExcel {
    private JExcelUtils jExcelUtils = new JExcelUtils();

    /**
     * dataList style : List<List<String>>
     * @param dataList
     */
    public byte[] genInnovSignUpExcel(List<List<String>> dataList) {
        List<String> titleList = new ArrayList<String>();
        titleList.add("序号");
        titleList.add("请假人");
        titleList.add("部门");
        titleList.add("上级领导");
        titleList.add("假期类型");
        titleList.add("请假开始时间");
        titleList.add("请假天数");

        return jExcelUtils.createExcelBuffer("统计单"+ DateUtils.formatDateMinute(DateUtils.nowMillis()),titleList,dataList);
    }
}