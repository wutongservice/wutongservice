package com.borqs.elearning.elearning.convertor;

import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.util.SystemHelper;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.List;

public class pdf2SwfCG {
    private static final Logger L = Logger.getLogger(pdf2SwfCG.class);
    public static int convertPDF2SWF(String pdf_file, String swf_file) throws IOException {

        File source = new File(pdf_file);
        if (!source.exists()) return 0;

        //调用pdf2swf命令进行转换
//        String command= "pdf2swf -t "+pdf_file+" -o "+swf_file+" -s flashversion=9 ";
        String command= "pdf2swf -t "+pdf_file+" -o "+swf_file+" -s flashversion=9 ";

        executeCmdFlash(command);


//        L.debug(null,"elearning/convertPDF2SWF command="+command);
//        Process pro = Runtime.getRuntime().exec(command);
//        L.debug(null,"elearning/pro end="+pro.toString());
//        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(pro.getInputStream()));
//        while (bufferedReader.readLine() != null) ;
//
//        try {
//            pro.waitFor();
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        return pro.exitValue();
        return 1;

    }

    public static void convertPDF2SWF1(String pdf_file, String swf_file) throws IOException {

        String PATH_TO_SWF = " pdf2swf ";
        String fromFilename = pdf_file;
        String saveFilename = swf_file;

        List<String> command = new java.util.ArrayList<String>();
        command.add(PATH_TO_SWF);
        command.add("-o");
        command.add(saveFilename);
        command.add("-t");
        command.add(fromFilename);
        command.add("-s");
//            command.add("languagedir=" + "G:\test\read\xpdf\chinese-simplified");
        command.add(" flashversion=9 ");

        ProcessBuilder builder = new ProcessBuilder();
        String command1= "pdf2swf -t "+pdf_file+" -o "+swf_file+" -s flashversion=9 ";
        L.debug(null,"command1="+command1);
        builder.command(command1);
        L.debug(null,"command1 command");
        try {
            Process convertImgProcess = builder.start();
            final InputStream is1 = convertImgProcess.getInputStream();
            final InputStream is2 = convertImgProcess.getErrorStream();
            new Thread() {
                public void run() {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(is1));
                    try {
                        String lineB = null;

                        while ((lineB = br.readLine()) != null) {
                            if (lineB != null) System.out.println(lineB);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();

            new Thread() {
                public void run() {

                    BufferedReader br2 = new BufferedReader(
                            new InputStreamReader(is2));
                    try {

                        String lineC = null;
                        while ((lineC = br2.readLine()) != null) {

                            if (lineC != null) System.out.println(lineC);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
            convertImgProcess.waitFor();
            L.debug(null,"process end");
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public static synchronized boolean executeCmdFlash(String cmd) {
        StringBuilder stdout= new StringBuilder();
        try {
            final Process process = Runtime.getRuntime().exec(cmd);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    process.destroy();
                }
            });
            InputStreamReader inputstreamreader = new InputStreamReader(process.getInputStream());
            char c = (char) inputstreamreader.read();
            if (c != '\uFFFF')
                stdout.append(c);
            while (c != '\uFFFF') {
                if (!inputstreamreader.ready()) {
                    System.out.println(stdout);
                    try {
                        process.exitValue();
                        break;
                    } catch (IllegalThreadStateException _ex) {
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException _ex2) {
                        }
                    }
                } else {
                    c = (char) inputstreamreader.read();
                    stdout.append(c);
                }
            }
            try {
                inputstreamreader.close();
            } catch (IOException ioexception2) {
                System.err.println("RunCmd : Error closing InputStream " + ioexception2);
                return false;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            L.debug(null,"发生错误：" + e);
            return false;
        }

        return true;
    }

}