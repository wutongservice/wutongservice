package com.borqs.server.platform.util.sender.sms;


import com.borqs.server.platform.io.RW;
import com.borqs.server.platform.io.Writable;
import com.borqs.server.platform.util.DateHelper;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Message implements Writable {
    private String from;
    private String to;
    private String content;
    private long time;

    private Message(String from, String to, long time, String content) {
        this.from = from;
        this.to = to;
        this.time = time;
        this.content = content;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getContent() {
        return content;
    }

    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return String.format("SMS {from:%s, to:%s, time:%s, content:'%s'}", from, to, DateHelper.formatDateAndTime(time), content);
    }

    public static Message forSend(String to, String content) {
        return new Message("", to, DateHelper.nowMillis(), content);
    }

    public static Message forReceive(String from, String to, long time, String content) {
        return new Message(from, to, time, content);
    }

    @Override
    public void write(Encoder out, boolean flush) throws IOException {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("from", from);
        m.put("to", to);
        m.put("content", content);
        m.put("time", time);
        RW.write(out, m, flush);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readIn(Decoder in) throws IOException {
        Map<String, Object> m = (Map) RW.read(in);
        from = (String) m.get("from");
        to = (String) m.get("to");
        content = (String) m.get("content");
        time = (Long) m.get("time");
    }
}
