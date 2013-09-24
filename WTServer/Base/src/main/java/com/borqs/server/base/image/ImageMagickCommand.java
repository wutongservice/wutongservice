package com.borqs.server.base.image;


import com.borqs.server.base.BaseErrors;
import com.borqs.server.ServerException;
import com.borqs.server.base.util.SystemHelper;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageMagickCommand {
    //private static final Logger L = Logger.get(ImageMagickCommand.class);

    private String command;
    private String input;
    private String output;
    private final List<Op> operations = new ArrayList<Op>();

    public ImageMagickCommand() {
        this(null, "", "");
    }

    public ImageMagickCommand(String input, String output) {
        this(null, input, output);
    }

    public ImageMagickCommand(String command, String input, String output) {
        command(command);
        input(input);
        output(output);
    }

    public String getCommand() {
        return command;
    }

    public ImageMagickCommand command(String command) {
        this.command = command;
        return this;
    }

    public String getInput() {
        return input;
    }

    public ImageMagickCommand input(String input) {
        this.input = ObjectUtils.toString(input);
        return this;
    }

    public String getOutput() {
        return output;
    }

    public ImageMagickCommand output(String output) {
        this.output = ObjectUtils.toString(output);
        return this;
    }

    public ImageMagickCommand op(String op, String arg) {
        operations.add(new Op(ObjectUtils.toString(op), ObjectUtils.toString(arg)));
        return this;
    }

    public ImageMagickCommand op(String op) {
        return op(op, "");
    }

    public ImageMagickCommand resize(String arg) {
        return op("resize", arg);
    }

    public ImageMagickCommand rotate(String arg) {
        return op("rotate", arg);
    }

    public ImageMagickCommand resize(String w, String h, String spec) {
        return resize(ObjectUtils.toString(w) + "x" + ObjectUtils.toString(h) + ObjectUtils.toString(spec));
    }

    public ImageMagickCommand resize(String w, String h) {
        return resize(w, h, "");
    }

    public ImageMagickCommand resize(Integer w, Integer h, String spec) {
        return resize(ObjectUtils.toString(w), ObjectUtils.toString(h), spec);
    }

    public ImageMagickCommand resize(Integer w, Integer h) {
        return resize(w, h, "");
    }

    private String getImCommand() {
        if (StringUtils.isNotEmpty(command)) {
            return command;
        } else {
            String defImCommand;

            if (SystemHelper.osIsWindows()) {
                if (SystemHelper.isLocalRun())
                    defImCommand = SystemHelper.getHomeDirectory() + "\\etc\\local\\convert.exe";
                else
                    defImCommand = SystemHelper.getHomeDirectory() + "\\tool\\convert.exe";
            } else {
                if (SystemHelper.isLocalRun()) {
                    defImCommand = SystemHelper.getHomeDirectory() + "/etc/local/convert";
                } else {
                    defImCommand = SystemHelper.getHomeDirectory() + "/tool/convert";
                    if (!new File(defImCommand).exists())
                        defImCommand = "/usr/local/bin/convert";
                    if (!new File(defImCommand).exists())
                        defImCommand = "/usr/bin/convert";
                }
            }

            return SystemHelper.getEnvOrProp("IM_PATH", defImCommand);
        }
    }

    private String[] makeCommandList() {
        ArrayList<String> l = new ArrayList<String>();
        l.add(getImCommand());
        for (Op op : operations) {
            l.add("-" + op.op);
            l.add(op.arg);
        }
        l.add(input);
        l.add(output);
        return l.toArray(new String[l.size()]);
    }

    public int run() {
        String[] cmdList = makeCommandList();
        //L.debug(null, StringUtils.join(cmdList, " "));
        ProcessBuilder pb = new ProcessBuilder(cmdList);
        String inputDir = FilenameUtils.getFullPathNoEndSeparator(input);
        pb.directory(new File(inputDir));
        try {
            Process p = pb.start();
            return p.waitFor();
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_IMAGE_PROCESS_ERROR, e);
        } catch (InterruptedException e) {
            throw new ServerException(BaseErrors.PLATFORM_IMAGE_PROCESS_ERROR, e);
        }
    }

    public void checkRun() {
        int exitCode = run();
        if (exitCode != 0)
            throw new ServerException(BaseErrors.PLATFORM_IMAGE_PROCESS_ERROR, "Resize image error");
    }

    public static class Op {
        public final String op;
        public final String arg;

        public Op(String op, String arg) {
            this.op = op;
            this.arg = arg;
        }
    }
}
