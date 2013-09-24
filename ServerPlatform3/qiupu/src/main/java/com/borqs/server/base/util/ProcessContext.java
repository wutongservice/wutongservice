package com.borqs.server.base.util;


import org.apache.commons.lang.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class ProcessContext {
    private Map<String, String> environments;
    private String directory;
    private InputStream input;
    private OutputStream output;
    private OutputStream error;
    private boolean redirectError = false;


    public ProcessContext() {
    }

    public boolean hasEnvironments() {
        return environments != null && !environments.isEmpty();
    }

    public Map<String, String> getEnvironments() {
        return environments;
    }

    public ProcessContext setEnvironments(Map<String, String> environments) {
        this.environments = environments;
        return this;
    }

    public boolean hasDirectory() {
        return StringUtils.isNotEmpty(directory);
    }

    public String getDirectory() {
        return directory;
    }

    public ProcessContext setDirectory(String directory) {
        this.directory = directory;
        return this;
    }

    public boolean hasInput() {
        return input != null;
    }

    public InputStream getInput() {
        return input;
    }

    public ProcessContext setInput(InputStream input) {
        this.input = input;
        return this;
    }

    public boolean hasOutput() {
        return output != null;
    }

    public OutputStream getOutput() {
        return output;
    }

    public ProcessContext setOutput(OutputStream output) {
        this.output = output;
        return this;
    }

    public boolean hasError() {
        return error != null;
    }

    public OutputStream getError() {
        return error;
    }

    public ProcessContext setError(OutputStream error) {
        this.error = error;
        return this;
    }

    public boolean isRedirectError() {
        return redirectError;
    }

    public ProcessContext setRedirectError(boolean redirectError) {
        this.redirectError = redirectError;
        return this;
    }
}

