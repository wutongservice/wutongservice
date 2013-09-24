package com.borqs.server.platform.service;


public interface Service {
    void start();

    void stop();

    boolean isStarted();
}
