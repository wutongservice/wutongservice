package com.borqs.server.market.utils;

import jxl.*;
import jxl.format.Alignment;
import jxl.write.*;

import java.io.*;
import java.lang.Boolean;
import java.util.*;


public class JExcelUtils {

    WritableWorkbook workbook;
    WritableSheet sheet;
    String path, sheetName;

    /**
     * 初始化工作表
     *
     * @param filePath 文件路径
     * @param fileName 文件名字
     */
    public WritableSheet creatSheet(String filePath, String fileName) {

        this.path = filePath;
        this.sheetName = fileName;
        try {
            OutputStream os = new FileOutputStream(path);//输出流指定文件路径
            workbook = Workbook.createWorkbook(os);//创建工作薄
            sheet = workbook.createSheet(sheetName, 0); //添加第一个工作表
            initialSheetSetting(sheet);//初始化表格属性(公共方法)
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sheet;
    }

    /**
     * 初始化表格属性
     *
     * @param sheet
     */
    public void initialSheetSetting(WritableSheet sheet) {
        try {
            sheet.getSettings().setDefaultColumnWidth(15); //设置列的默认宽度
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 插入公式
     *
     * @param sheet
     * @param col
     * @param row
     * @param formula
     * @param format
     */
    public void insertFormula(WritableSheet sheet, Integer col, Integer row, String formula, WritableCellFormat format) {
        try {
            Formula f = new Formula(col, row, formula, format);
            sheet.addCell(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 插入一行数据
     *
     * @param sheet  工作表
     * @param row    行号
     * @param data   内容
     * @param format 风格
     */
    public void insertRowData(WritableSheet sheet, Integer row, List data, WritableCellFormat format) {
        try {
            Label label;
            for (int i = 0; i < data.size(); i++) {
                label = new Label(i, row, (String) data.get(i), format);
                sheet.addCell(label);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 插入单元格数据
     *
     * @param sheet
     * @param col
     * @param row
     * @param data
     */
    public void insertOneCellData(WritableSheet sheet, Integer col, Integer row, Object data, WritableCellFormat format) {

        try {
            if (data instanceof Double) {
                jxl.write.Number labelNF = new jxl.write.Number(col, row, (Double) data, format);
                sheet.addCell(labelNF);
            } else if (data instanceof Boolean) {

                jxl.write.Boolean labelB = new jxl.write.Boolean(col, row, (Boolean) data, format);
                sheet.addCell(labelB);
            } else if (data instanceof Date) {
                DateTime labelDT = new DateTime(col, row, (Date) data, format);
                sheet.addCell(labelDT);
                setCellComments(labelDT, "这是个创建表的日期说明！"); //给单元格加注释(公共方法)
            } else {
                Label label = new Label(col, row, data.toString(), format);
                sheet.addCell(label);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 合并单元格，并插入数据
     *
     * @param sheet
     * @param col_start
     * @param row_start
     * @param col_end
     * @param row_end
     * @param data
     * @param format
     */
    public void mergeCellsAndInsertData(WritableSheet sheet, Integer col_start, Integer row_start, Integer col_end, Integer row_end, Object data, WritableCellFormat format) {
        try {
            sheet.mergeCells(col_start, row_start, col_end, row_end);//左上角到右下角
            insertOneCellData(sheet, col_start, row_start, data, format);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 给单元格加注释
     *
     * @param label    ：lable对象
     * @param comments ：注释内容
     */
    public void setCellComments(Object label, String comments) {
        WritableCellFeatures cellFeatures = new WritableCellFeatures();
        cellFeatures.setComment(comments);
        if (label instanceof jxl.write.Number) {
            jxl.write.Number num = (jxl.write.Number) label;
            num.setCellFeatures(cellFeatures);
        } else if (label instanceof jxl.write.Boolean) {
            jxl.write.Boolean bool = (jxl.write.Boolean) label;
            bool.setCellFeatures(cellFeatures);
        } else if (label instanceof DateTime) {
            DateTime dt = (DateTime) label;
            dt.setCellFeatures(cellFeatures);
        } else {
            Label _label = (Label) label;
            _label.setCellFeatures(cellFeatures);
        }
    }
    /**
     * 读取excel
     *
     * @param inputFile
     * @param inputFileSheetIndex
     * @throws Exception
     */
    public static ArrayList<String> readDataFromExcel(InputStream inputFile, int inputFileSheetIndex) {

        ArrayList<String> list = new ArrayList<String>();
        Workbook book = null;
        Cell cell = null;
        WorkbookSettings setting = new WorkbookSettings();
        Locale locale = new Locale("zh", "CN"); //本地
        setting.setLocale(locale);
        setting.setEncoding("ISO-8859-1");   //设置字符集编码

        try {
            book = Workbook.getWorkbook(inputFile, setting);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Sheet sheet = book.getSheet(inputFileSheetIndex);
        for (int rowIndex = 0; rowIndex < sheet.getRows(); rowIndex++) {//行
            for (int colIndex = 0; colIndex < sheet.getColumns(); colIndex++) {//列
                cell = sheet.getCell(colIndex, rowIndex);
                list.add(cell.getContents());
            }
        }
        book.close();
        return list;
    }
    /**
     * 读取excel
     *
     * @param inputFile
     * @param inputFileSheetIndex
     * @throws Exception
     */
    public static ArrayList<String> readDataFromExcel(File inputFile, int inputFileSheetIndex) {

        ArrayList<String> list = new ArrayList<String>();
        Workbook book = null;
        Cell cell = null;
        WorkbookSettings setting = new WorkbookSettings();
        Locale locale = new Locale("zh", "CN"); //本地
        setting.setLocale(locale);
        setting.setEncoding("ISO-8859-1");   //设置字符集编码

        try {
            book = Workbook.getWorkbook(inputFile, setting);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Sheet sheet = book.getSheet(inputFileSheetIndex);
        for (int rowIndex = 0; rowIndex < sheet.getRows(); rowIndex++) {//行
            for (int colIndex = 0; colIndex < sheet.getColumns(); colIndex++) {//列
                cell = sheet.getCell(colIndex, rowIndex);
                list.add(cell.getContents());
            }
        }
        book.close();
        return list;
    }

    /**
     * excel 数据为2列 ,返回key value形式的map
     * @param inputFile
     * @param inputFileSheetIndex
     * @return
     * @throws Exception
     */
    public Map<String,String> readMapFromExcel(File inputFile, int inputFileSheetIndex) throws Exception {

        Map<String,String> map = new HashMap<String,String>();
        Workbook book = null;
        Cell cell = null;
        WorkbookSettings setting = new WorkbookSettings();
        Locale locale = new Locale("zh", "CN"); //本地
        setting.setLocale(locale);
        setting.setEncoding("ISO-8859-1");   //设置字符集编码

        try {
            book = Workbook.getWorkbook(inputFile, setting);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Sheet sheet = book.getSheet(inputFileSheetIndex);
        for (int rowIndex = 0; rowIndex < sheet.getRows(); rowIndex++) {//行
            Cell[] cells  = sheet.getRow(rowIndex);
            if(cells.length!=2)
                throw new Exception("columns is not 2");

            map.put(cells[0].getContents(),cells[1].getContents());
        }
        book.close();
        return map;
    }

    /**
     * 得到数据表头格式
     *
     * @return
     */
    public WritableCellFormat getTitleCellFormat() {
        WritableCellFormat wcf = null;
        try {
            // 字体样式(字体,大小,是否粗体,是否斜体)
            WritableFont wf = new WritableFont(WritableFont.TIMES, 11, WritableFont.BOLD, false);
            wcf = new WritableCellFormat(wf);//实例化文字格式化
            // 对齐方式
            wcf.setAlignment(Alignment.LEFT);   //水平
            wcf.setVerticalAlignment(VerticalAlignment.CENTRE); //垂直
        } catch (WriteException e) {
            e.printStackTrace();
        }
        return wcf;
    }

    /**
     * 得到数据格式(默认左对齐)
     *
     * @return
     */
    public WritableCellFormat getDataCellFormat(CellType type) {
        WritableCellFormat wcf = null;
        try {
            // 字体样式
            if (type == CellType.NUMBER || type == CellType.NUMBER_FORMULA) {//数字
                NumberFormat nf = new NumberFormat("#.00");   //保留小数点后两位
                wcf = new WritableCellFormat(nf);
            } else if (type == CellType.DATE || type == CellType.DATE_FORMULA) {//日期
                DateFormat df = new DateFormat("yyyy-MM-dd hh:mm:ss"); //时间显示格式
                wcf = new WritableCellFormat(df);
            } else {
                WritableFont wf = new WritableFont(WritableFont.TIMES, 11, WritableFont.NO_BOLD, false);//字体样式(字体,大小,是否粗体,是否斜体)
                wcf = new WritableCellFormat(wf);
            }
            // 对齐方式
            wcf.setAlignment(Alignment.LEFT);
            wcf.setVerticalAlignment(VerticalAlignment.CENTRE);

            wcf.setWrap(false);//自动换行
        } catch (WriteException e) {
            e.printStackTrace();
        }

        return wcf;
    }

    /**
     * 得到数据格式(重载)
     *
     * @return
     */
    public WritableCellFormat getDataCellFormat(CellType type, Alignment align) {
        WritableCellFormat wcf = null;
        try {
            // 字体样式
            if (type == CellType.NUMBER || type == CellType.NUMBER_FORMULA) {//数字
                NumberFormat nf = new NumberFormat("#.00");   //保留小数点后两位
                wcf = new WritableCellFormat(nf);
            } else if (type == CellType.DATE || type == CellType.DATE_FORMULA) {//日期
                DateFormat df = new DateFormat("yyyy-MM-dd hh:mm:ss"); //时间显示格式
                wcf = new WritableCellFormat(df);
            } else {
                WritableFont wf = new WritableFont(WritableFont.TIMES, 12, WritableFont.NO_BOLD, false);//字体样式(字体,大小,是否粗体,是否斜体)
                wcf = new WritableCellFormat(wf);
            }
            // 对齐方式
            wcf.setAlignment(align);
            wcf.setVerticalAlignment(VerticalAlignment.CENTRE);

            wcf.setWrap(true);//自动换行
        } catch (WriteException e) {
            e.printStackTrace();
        }

        return wcf;
    }

    /**
     * 创建目录
     *
     * @param destDirName 目录路径
     */
    public static void createDir(String destDirName) {
        File dir = new File(destDirName);
        //如果目录不存在则创建目录
        if (!dir.exists()) {

            if (!destDirName.endsWith(File.separator))
                destDirName = destDirName + File.separator;

            // 创建单个目录
            if (dir.mkdirs()) {
                System.out.println("创建目录" + destDirName + "成功！");

            } else {
                System.out.println("创建目录" + destDirName + "成功！");
            }
        }

    }


    /**
     * 生成并关闭工作簿
     */
    public void writeAndClose() {
        try {
            workbook.write();
            workbook.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (WriteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 生成Excel文件(适合一个标题,剩余全是数据)
     *
     * @param path       文件路径
     * @param sheetName  工作表名称
     * @param dataTitles 数据标题
     */
    public void createExcelFile(String path, String sheetName, List dataTitles, List datas) {
        try {

            OutputStream os = new FileOutputStream(path);//输出流指定文件路径
            workbook = Workbook.createWorkbook(os);//创建工作薄
            sheet = workbook.createSheet(sheetName, 0); //添加第一个工作表
            initialSheetSetting(sheet);//初始化表格属性(公共方法)

            // 添加数据标题
            insertRowData(sheet, 0, dataTitles, getTitleCellFormat());

            // 插入一行   (公共方法)
            for (int i = 0; i < datas.size(); i++) {
                List data = (List) datas.get(i);
                insertRowData(sheet, i + 1, data, getDataCellFormat(CellType.STRING_FORMULA));
            }

            workbook.write();
            workbook.close();

            os.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] createExcelBuffer(String sheetName, List dataTitles, List datas) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();//输出流指定文件路径
            workbook = Workbook.createWorkbook(os);//创建工作薄
            sheet = workbook.createSheet(sheetName, 0); //添加第一个工作表
            initialSheetSetting(sheet);//初始化表格属性(公共方法)

            // 添加数据标题
            insertRowData(sheet, 0, dataTitles, getTitleCellFormat());

            // 插入一行   (公共方法)
            for (int i = 0; i < datas.size(); i++) {
                List data = (List) datas.get(i);
                insertRowData(sheet, i + 1, data, getDataCellFormat(CellType.STRING_FORMULA));
            }

            workbook.write();
            workbook.close();

            byte[] buff = os.toByteArray();
            os.close();
            return buff;
        } catch (Exception e) {
            return null;
        }
    }

    //测试
    public static void main(String[] args) {

        JExcelUtils jxl = new JExcelUtils();
        //创建目录
        jxl.createDir("C:/ExcelTEMP/");
        //文件路径
        String filePath = "C:/ExcelTEMP/test.xls";

        //模拟标题
        List<String> titles = new ArrayList<String>();
        titles.add("学号");
        titles.add("姓名");
        titles.add("语文");
        titles.add("数学");
        titles.add("英语");
        titles.add("总分");
        //模拟数据集
        List<List<String>> datas = new ArrayList<List<String>>();
        for (int i = 0; i < 10; i++) {
            List<String> list = new ArrayList<String>();
            list.add("200201001");
            list.add("张 三");
            list.add("100");
            list.add("60");
            list.add("100");
            list.add("260");

            datas.add(list);
        }
        //创建Excel表格
        jxl.createExcelFile(filePath, "成绩单", titles, datas);


    }


}
