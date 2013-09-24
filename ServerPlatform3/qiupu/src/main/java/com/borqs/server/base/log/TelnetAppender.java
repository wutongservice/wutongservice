package com.borqs.server.base.log;


import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;

public class TelnetAppender extends AppenderBase<ILoggingEvent> {

    private Layout<ILoggingEvent> layout;

    public TelnetAppender() {
    }

    public Layout<ILoggingEvent> getLayout() {
        return layout;
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    @Override
    protected void append(ILoggingEvent e) {
        TelnetAppenderService.getInstance().append(e, layout);
    }
}
