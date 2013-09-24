package com.borqs.server.impl.migration;


import com.borqs.server.platform.app.AppBootstrap;
import com.borqs.server.platform.app.AppMain;
import com.borqs.server.platform.app.GlobalSpringAppContext;
import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.util.VfsHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MigrationMain implements AppMain {
    public static Properties p = new Properties();
    public static Map<String, CMDRunner> mapCMD;

    public MigrationMain() {
        init();
    }

    public void init() {
        getConfig();
    }

    @Override
    public void run(String[] args) throws Exception {
        String cmd = "";
        if (ArrayUtils.isNotEmpty(args))
            cmd = args[0];


        CMDRunner runner = null;
        String beanName = null;
        if (cmd.equals("friend")) {
            runner = (CMDRunner) GlobalSpringAppContext.getBean("friend.mig");
            beanName = "friend.mig";
        } else if (cmd.equals("account")) {
            runner = (CMDRunner) GlobalSpringAppContext.getBean("account.mig");
            beanName = "account.mig";
        } else if (cmd.equals("circle")) {
            runner = (CMDRunner) GlobalSpringAppContext.getBean("circle.mig");
            beanName = "circle.mig";
        } else if (cmd.equals("comment")) {
            runner = (CMDRunner) GlobalSpringAppContext.getBean("comment.mig");
            beanName = "comment.mig";
        } else if (cmd.equals("contact")) {
            runner = (CMDRunner) GlobalSpringAppContext.getBean("contact.mig");
            beanName = "contact.mig";
        } else if (cmd.equals("conversation")) {
            runner = (CMDRunner) GlobalSpringAppContext.getBean("conversation.mig");
            beanName = "conversation.mig";
        } else if (cmd.equals("photo")) {
            runner = (CMDRunner) GlobalSpringAppContext.getBean("photo.mig");
            beanName = "photo.mig";
        } else if (cmd.equals("setting")) {
            runner = (CMDRunner) GlobalSpringAppContext.getBean("setting.mig");
            beanName = "setting.mig";
        } else if (cmd.equals("stream")) {
            runner = (CMDRunner) GlobalSpringAppContext.getBean("stream.mig");
            beanName = "stream.mig";
        } else if (cmd.equals("suggest")) {
            runner = (CMDRunner) GlobalSpringAppContext.getBean("suggest.mig");
            beanName = "suggest.mig";
        } else if (cmd.equals("ticket")) {
            runner = (CMDRunner) GlobalSpringAppContext.getBean("ticket.mig");
            beanName = "ticket.mig";
        } else if (cmd.equals("request")) {
            runner = (CMDRunner) GlobalSpringAppContext.getBean("request.mig");
            beanName = "request.mig";
        } else {
            runner = null;
        }


        mapCMD = GlobalSpringAppContext.getInstance().getBeansOfType(CMDRunner.class);

        List<String> dependencies = new ArrayList<String>();
        if (runner != null) {
            dependencies = runner.getDependencies();
            if(dependencies == null){
                dependencies = new ArrayList<String>();
                dependencies.add(beanName);
            }else {
                dependencies.add(beanName);
            }

        }
        List<String> result0 = new ArrayList<String>();

        List<CMDRunner> cmdPool = new ArrayList<CMDRunner>();
        List<String> result = getDependency(dependencies, mapCMD, result0, cmdPool);

        /*if (beanName != null)
            result.add(beanName);*/


        for (String cmd0 : result) {
            CMDRunner r = (CMDRunner) GlobalSpringAppContext.getBean(cmd0);
            r.run(cmd0, p);
        }

    }

    public static void main(String[] args) {
        try {
            AppBootstrap.run("D:\\work\\work\\migration\\src\\main\\java\\com\\borqs\\server\\impl\\migration/config.xml", "migration.main", args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static List<String> getDependency(List<String> de, Map<String, CMDRunner> map, List<String> result, List<CMDRunner> cmdPool) {
        // if de is null , add all table for migrate
        if (CollectionUtils.isEmpty(de)) {
            for (String s : map.keySet()) {
                if (StringUtils.isNotEmpty(s))
                    result.add(s);
            }
        } else {

            for (String d : de) {
                if (!result.contains(d))
                    result.add(d);
                CMDRunner cmd = map.get(d);

                List<String> inDen = cmd.getDependencies();
                if (inDen != null) {
                    getDependency(inDen, map, result, cmdPool);
                }
            }
        }
        return result;
    }


    public static void getConfig() {

        try {
            String path = VfsHelper.classpathFileToPath(MigrationMain.class, "migration.properties");
            InputStream in = IOUtils.toInputStream(VfsHelper.loadText(path), Charsets.DEFAULT);

            p.load(in);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
