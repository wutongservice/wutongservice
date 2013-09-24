package com;

import com.borqs.elearning.authorization.AuthorizationImpl;
import com.borqs.elearning.authorization.AuthorizationLogic;
import com.borqs.server.base.util.InitUtils;
import com.borqs.server.base.web.JettyServer;
import com.borqs.elearning.elearning.CourseImpl;
import com.borqs.elearning.elearning.CourseLogic;

public class ElearningGlobalLogics {

    private static AuthorizationLogic auth = new AuthorizationImpl();
    private static CourseLogic course = new CourseImpl();


    public static void init() {
        InitUtils.batchInit(auth, course);

    }

    public static void destroy() {
       InitUtils.batchInit(auth, course);
    }



    public static AuthorizationLogic getAuth() {
        return auth;
    }

    public static CourseLogic getCourse() {
        return course;
    }

    public static class ServerLifeCycle implements JettyServer.LifeCycle {
        @Override
        public void before() throws Exception {
            ElearningGlobalLogics.init();
        }

        @Override
        public void after() throws Exception {
            ElearningGlobalLogics.destroy();
        }
    }
}
