package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.utils.record.Records;

public abstract class AbstractChartTag extends AbstractFreemarkerJspTag {

    protected String id = "";
    protected String styleClass = "graph";
    protected String style = "";
    protected Records graphData;

    protected AbstractChartTag(String template) {
        super(template);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public Records getGraphData() {
        return graphData;
    }

    public void setGraphData(Records graphData) {
        this.graphData = graphData;
    }
}
