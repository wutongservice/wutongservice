package com.borqs.server.market.controllers.tags;


import com.borqs.server.market.utils.CC;

import java.util.Map;

public class TextInputTag extends AbstractFreemarkerJspTag {

    protected String label = "";
    protected String id = "";
    protected String name = "";
    protected String placeholder = "";
    protected String errorMessage = "";
    protected String value = "";
    protected boolean readonly = false;
    protected String locale = "";
    protected boolean required = false;
    protected int maxCharacter = 0;

    protected String controlGroupSpan = "";
    protected String labelSpan = "";
    protected String controlsSpan = "";
    protected String inputSpan = "";



    public TextInputTag() {
        super("TextInputTag.ftl");
    }

    protected TextInputTag(String template) {
        super(template);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }


    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public int getMaxCharacter() {
        return maxCharacter;
    }

    public void setMaxCharacter(int maxCharacter) {
        this.maxCharacter = maxCharacter;
    }

    public String getControlGroupSpan() {
        return controlGroupSpan;
    }

    public void setControlGroupSpan(String controlGroupSpan) {
        this.controlGroupSpan = controlGroupSpan;
    }

    public String getLabelSpan() {
        return labelSpan;
    }

    public void setLabelSpan(String labelSpan) {
        this.labelSpan = labelSpan;
    }

    public String getControlsSpan() {
        return controlsSpan;
    }

    public void setControlsSpan(String controlsSpan) {
        this.controlsSpan = controlsSpan;
    }

    public String getInputSpan() {
        return inputSpan;
    }

    public void setInputSpan(String inputSpan) {
        this.inputSpan = inputSpan;
    }

    @Override
    protected Map<String, Object> getData() {
        return CC.map(
                "label=>", label,
                "id=>", id,
                "name_=>", name,
                "placeholder=>", placeholder,
                "errorMessage=>", errorMessage,
                "value=>", value,
                "readonly=>", readonly,
                "locale=>", locale,
                "required=>", required,
                "maxCharacter=>", maxCharacter,
                "controlGroupSpan=>", controlGroupSpan,
                "labelSpan=>", labelSpan,
                "controlsSpan=>", controlsSpan,
                "inputSpan=>", inputSpan
        );
    }
}
