package com.borqs.server.platform.service;


import com.borqs.server.platform.app.AppMain;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.util.ClassHelper;
import com.borqs.server.platform.util.Initializable;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.quartz.impl.DirectSchedulerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class ScheduleService implements Service, Initializable {

    private static final Logger L = Logger.get(ScheduleService.class);

    private Scheduler scheduler;

    private List<Job> jobs;

    private int maxThreads = 10;

    public ScheduleService() {
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }


    private Scheduler createScheduler() throws SchedulerException {
        int maxThreads = this.maxThreads > 0 ? this.maxThreads : 10;
        DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();
        factory.createVolatileScheduler(maxThreads);
        return factory.getScheduler();
    }

    @Override
    public void init() throws Exception {
        try {
            this.scheduler = createScheduler();
            if (CollectionUtils.isNotEmpty(jobs)) {
                for (Job job : jobs) {
                    if (job == null)
                        continue;

                    JobDetail jobDetail = createJob(job.getName(), job.getMain(), job.getArgsArray());
                    Trigger trig = createTrigger(job.getExpression());
                    this.scheduler.scheduleJob(jobDetail, trig);
                }
            }

        } catch (SchedulerException e) {
            L.error(null, e, "Init scheduler error");
        }
    }

    @Override
    public void destroy() {
        stop();
    }

    @Override
    public void start() {
        if (isStarted())
            throw new IllegalStateException();

        try {
            scheduler.start();
        } catch (SchedulerException e) {
            L.error(null, e, "Start schedule error");
        }
    }

    @Override
    public void stop() {
        if (scheduler != null) {
            try {
                scheduler.shutdown(true);
            } catch (SchedulerException e) {
                L.warn(null, e, "Close schedule error");
            } finally {
                scheduler = null;
            }
        }
    }

    @Override
    public boolean isStarted() {
        try {
            return scheduler.isStarted();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }


    private static final String SCHEDULE_GROUP = "borqs_schedule";

    public static JobDetail createJob(String name, Object appMain, String[] args) {
        String jobName = StringUtils.isBlank(name) ? makeJobName(appMain) : name;
        JobDataMap data = new JobDataMap();
        data.put(AppMainJob.JOB_NAME_KEY, jobName);
        data.put(AppMainJob.APP_MAIN_KEY, appMain);
        data.put(AppMainJob.ARGS_KEY, args);
        return newJob(AppMainJob.class)
                .withIdentity(jobName, SCHEDULE_GROUP)
                .usingJobData(data)
                .build();
    }

    private static String makeJobName(Object appMain) {
        if (appMain instanceof AppMain) {
            return "JOB:" + appMain.getClass().getName();
        } else if (appMain instanceof CharSequence) {
            return "JOB:" + appMain.toString();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static Trigger createTrigger(String cronExpr) {
        return newTrigger()
                .withIdentity("TRIG " + cronExpr, SCHEDULE_GROUP)
                .startNow()
                .withSchedule(cronSchedule(cronExpr))
                .build();
    }

    public static class Job {
        private String name = "";
        private String expression;
        private Object main;
        private String args = "";

        public Job() {
        }

        public Job(String name, String expression, Object main, String args) {
            this.name = name;
            this.expression = expression;
            this.main = main;
            this.args = args;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }

        public Object getMain() {
            return main;
        }

        public void setMain(Object main) {
            this.main = main;
        }

        public String getArgs() {
            return args;
        }

        public void setArgs(String args) {
            this.args = args;
        }

        public String[] getArgsArray() {
            // TODO: split args with " and '
            return StringHelper.splitArray(args, " ", true);
        }
    }


    public static class AppMainJob implements org.quartz.Job {
        public static final String JOB_NAME_KEY = "jobName";
        public static final String APP_MAIN_KEY = "appMain";
        public static final String ARGS_KEY = "args";


        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            JobDataMap data = context.getJobDetail().getJobDataMap();
            Object appMain = data.get(APP_MAIN_KEY);
            if (appMain == null) {
                L.error(null, "The object appMain is null");
                return;
            }

            String jobName = data.getString(JOB_NAME_KEY);
            try {
                L.info(null, "Run schedule job " + jobName);
                if (appMain instanceof AppMain) {
                    String[] args = (String[]) data.get(ARGS_KEY);
                    if (args == null)
                        args = new String[0];

                    ((AppMain) appMain).run(args);
                } else if (appMain instanceof CharSequence) {
                    String className = appMain.toString();
                    Class clazz = ClassHelper.forNameSafe(className);
                    if (clazz == null)
                        throw new JobExecutionException("Class " + className + "is not found");

                    Method m = ClassHelper.getMethodNoThrow(clazz, "main", String[].class);
                    if (m != null) {
                        String[] args = (String[]) data.get(ARGS_KEY);
                        if (args == null)
                            args = new String[0];

                        m.invoke(null, new Object[]{args});
                    } else {
                        throw new JobExecutionException("Class " + className + ".main(String[]) is not found");
                    }
                }
                L.info(null, "Complete schedule job " + jobName);
            } catch (Exception e) {
                L.error(null, e, "Execute job error @%s", jobName);
                if (e instanceof JobExecutionException) {
                    throw (JobExecutionException) e;
                } else if (e instanceof InvocationTargetException) {
                    throw new JobExecutionException(((InvocationTargetException) e).getTargetException());
                } else {
                    throw new JobExecutionException(e);
                }
            }
        }
    }

}
