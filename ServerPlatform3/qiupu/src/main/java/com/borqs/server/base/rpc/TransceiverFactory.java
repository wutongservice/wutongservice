package com.borqs.server.base.rpc;


import com.borqs.server.base.conf.ConfigurableBase;
import org.apache.avro.ipc.Transceiver;


public abstract class TransceiverFactory extends ConfigurableBase {
    public abstract Transceiver getTransceiver(Class ifaceClass);
}
