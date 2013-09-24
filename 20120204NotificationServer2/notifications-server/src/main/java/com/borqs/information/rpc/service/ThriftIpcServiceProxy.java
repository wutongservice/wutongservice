package com.borqs.information.rpc.service;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TBinaryProtocol.Factory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.server.TThreadPoolServer.Args;
import org.apache.thrift.transport.TServerSocket;

import com.borqs.notifications.thrift.INotificationsThriftService;
import com.borqs.notifications.thrift.INotificationsThriftService.Iface;

public class ThriftIpcServiceProxy {
	private String host = "0.0.0.0";
	private int port = 8084;
	
	private INotificationsThriftService.Iface serviceImpl;
	
	private TServer server;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public INotificationsThriftService.Iface getServiceImpl() {
		return serviceImpl;
	}

	public void setServiceImpl(INotificationsThriftService.Iface serviceImpl) {
		this.serviceImpl = serviceImpl;
	}

	public boolean start(){
		System.out.println("start Thrift service-->"+port);
        try {
        	 TServerSocket serverTransport = new TServerSocket(port);
        	 INotificationsThriftService.Processor<Iface> processor = new INotificationsThriftService.Processor(serviceImpl);
        	 Factory protocalFactory = new TBinaryProtocol.Factory(true, true);

        	 Args args = new Args(serverTransport)
        	 				.processor(processor)
        	 				.protocolFactory(protocalFactory);
        	 
        	 server = new TThreadPoolServer(args);
        	 server.serve();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
	
	public void stop() {
		System.out.println("stopping Thrift service");
		if(null!=server) {
			server.stop();
		}
	}
}
