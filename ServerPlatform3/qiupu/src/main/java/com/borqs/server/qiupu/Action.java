package com.borqs.server.qiupu;

import com.borqs.server.service.qiupu.Qiupu;

public class Action {
	public String user;
	public int action;
	public String packageName;
	public int versionCode = 0;
	public String versionName = "";
	public int architecture = Qiupu.ARCH_ARM;
        public String deviceid = "";
}