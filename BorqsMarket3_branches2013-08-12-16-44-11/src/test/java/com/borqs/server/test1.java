package com.borqs.server;


import org.apache.commons.lang.time.DateUtils;
import org.apache.http.impl.cookie.DateParseException;
import org.junit.Test;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 13-6-28
 * Time: 下午5:21
 * To change this template use File | Settings | File Templates.
 */
public class test1 {
    @Test
    public void test1() throws IOException, DateParseException, ParseException {
        String d = "2013-07-09";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = dateFormat.parse(d);

        date = DateUtils.addMonths(date, -1);
        System.out.println(date);

    }

    @Test
    public void convertor() throws InterruptedException, IOException {
        long time = System.nanoTime();
        FileInputStream fis = new FileInputStream("D:/test.sql");
        FileOutputStream fos = new FileOutputStream("D:/test1.sql");

        while (fis.available() > 0) {
            fos.write(fis.read());
        }

        fis.close();
        fos.close();
        System.out.println(System.nanoTime() - time);
    }

    @Test
    public void convertor2() throws InterruptedException, IOException {
        long time = System.nanoTime();
        FileInputStream fis = new FileInputStream("D:/test.sql");
        BufferedInputStream bi = new BufferedInputStream(fis);
        FileOutputStream fos = new FileOutputStream("D:/test1.sql");
        BufferedOutputStream bo = new BufferedOutputStream(fos);
        while (bi.available() > 0) {
            bo.write(bi.read());
        }

        bi.close();
        bi.close();
        fis.close();
        fos.close();
        System.out.println(System.nanoTime() - time);
    }

    @Test
    public void test3() throws IOException {
        long time = System.nanoTime();
        FileInputStream fis = new FileInputStream("D:/test.sql");
        BufferedInputStream bi = new BufferedInputStream(fis);
        FileOutputStream fos = new FileOutputStream("D:/test1.sql");
        BufferedOutputStream bo = new BufferedOutputStream(fos);

        int len = 0;
        byte [] b = new byte[1024];
        while((len = bi.read(b))!=-1){
            bo.write(b);
        }
        bi.close();
        bo.close();
        fis.close();
        fos.close();
        System.out.println(System.nanoTime() - time);
    }


}
