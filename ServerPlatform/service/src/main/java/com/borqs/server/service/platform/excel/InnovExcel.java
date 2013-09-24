package com.borqs.server.service.platform.excel;

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
        titleList.add("申报人");
        titleList.add("部门");
        titleList.add("项目名称");
        titleList.add("其它项目成员");
        titleList.add("申报日期");

        return jExcelUtils.createExcelBuffer("统计单"+DateUtils.formatDateMinute(DateUtils.nowMillis()),titleList,dataList);
    }
}