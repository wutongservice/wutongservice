package com.borqs.server.base.io;


import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;

import java.io.IOException;

public interface Serializable {
    void write(Encoder out, boolean flush) throws IOException;
    void readIn(Decoder in) throws IOException;
}
