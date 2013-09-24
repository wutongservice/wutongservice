package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.service.ServiceConsts;
import com.borqs.server.market.utils.CC;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class DisplayVersionTag extends AbstractFreemarkerJspTag {
    private String id = "";
    private String versionName = "";
    private String version = "";
    private String versionStatus = "";
    private String product="";
    private int beta = 0;


    public DisplayVersionTag() {
        super("DisplayVersionTag.ftl");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersionStatus() {
        return versionStatus;
    }

    public void setVersionStatus(String versionStatus) {
        this.versionStatus = versionStatus;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public int getBeta() {
        return beta;
    }

    public void setBeta(int beta) {
        this.beta = beta;
    }

    @Override
    protected Map<String, Object> getData() {
        int versionStatus = StringUtils.isBlank(this.versionStatus) ? -1 : Integer.parseInt(this.versionStatus);
        return CC.map(
                "id=>", id,
                "versionName=>", versionName,
                "_version=>", version,
                "versionStatus=>", versionStatus,
                "active=>", (versionStatus > 0 && (versionStatus & ServiceConsts.PV_STATUS_ACTIVE) == ServiceConsts.PV_STATUS_ACTIVE),
                "product=>", product,
                "beta=>", beta
        );
    }
}
