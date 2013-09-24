package com.borqs.server.base.rpc;


import org.apache.avro.ipc.Transceiver;


public abstract class TransceiverFactory {
    public abstract Transceiver getTransceiver(Class ifaceClass);
}
