package com.borqs.server.base.util;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;

public class ProcessUtils {

    public static int execute(String... args) {
        return execute(null, args);
    }

    public static int execute(boolean redirectError, String... args) {
        return execute(new ProcessContext().setRedirectError(redirectError), args);
    }

    public static int execute(ProcessContext ctx, String... args) {
        try {

            ProcessBuilder pb = new ProcessBuilder(args);
            if (ctx != null) {
                if (ctx.hasDirectory())
                    pb.directory(new File(ctx.getDirectory()));
                if (ctx.hasEnvironments())
                    pb.environment().putAll(ctx.getEnvironments());

                pb.redirectErrorStream(ctx.isRedirectError());
            }
            Process process = pb.start();
            if (ctx != null && (ctx.hasInput() || ctx.hasOutput() || ctx.hasError())) {
//                byte[] buffer = new byte[1024 * 8];
//
//                while (!isTerminated(process)) {
//                    int n;
//                    if (ctx.hasOutput()) {
//                        n = readUntil(process.getInputStream(), buffer, '\n');
//                        if (n > 0)
//                            ctx.getOutput().write(buffer, 0, n);
//                    }
//                    if (ctx.hasError()) {
//                        n = readUntil(process.getErrorStream(), buffer, '\n');
//                        if (n > 0)
//                            ctx.getError().write(buffer, 0, n);
//                    }
//                    if (ctx.hasInput()) {
//                        n = ctx.getInput().read(buffer);
//                        if (n > 0)
//                            process.getOutputStream().write(buffer, 0, n);
//                    }
//                }
                if (ctx.hasOutput())
                    IOUtils.copy(process.getInputStream(), ctx.getOutput());
                if (ctx.hasError())
                    IOUtils.copy(process.getErrorStream(), ctx.getError());
                if (ctx.hasInput())
                    IOUtils.copy(ctx.getInput(), process.getOutputStream());

                process.waitFor();
            } else {
                process.waitFor();
            }
            return process.exitValue();
        } catch (Throwable t) {
            throw new ServerException(BaseErrors.PLATFORM_EXECUTE_PROCESS_ERROR, t);
        }
    }

    public static boolean isTerminated(Process process) {
        try {
            process.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            return false;
        }
    }

    public static String executeOutput(boolean redirectError, String... args) {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        execute(new ProcessContext().setRedirectError(redirectError).setOutput(bytesOut), args);
        return Charsets.fromBytes(bytesOut.toByteArray());
    }

//    private static int readUntil(InputStream in, byte[] buffer, int until) throws IOException {
//        int n = 0;
//        for (int i = 0; i < buffer.length; i++) {
//            int c = in.read();
//            if (c == -1)
//                break;
//
//            buffer[n++] = (byte)c;
//            if (c == until)
//                break;
//        }
//        return n;
//    }


    public static boolean isCanExecuted(File file) {
        Validate.notNull(file);
        return file.isFile() && file.exists() && file.canExecute();
    }

    public static boolean isCanExecuted(String path) {
        Validate.notNull(path);
        return isCanExecuted(new File(path));
    }

    public static int getProcessId() {
        String s = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(StringUtils.substringBefore(s, "@").trim());
    }

    public static int writeProcessId(String file) throws IOException {
        int pid = getProcessId();
        FileUtils.writeStringToFile(new File(file), Integer.toString(pid));
        return pid;
    }
}
