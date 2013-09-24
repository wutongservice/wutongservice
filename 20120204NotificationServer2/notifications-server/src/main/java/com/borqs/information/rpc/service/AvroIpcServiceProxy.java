package com.borqs.information.rpc.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.avro.ipc.Ipc;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificResponder;

public class AvroIpcServiceProxy {
	private String scheme = "avro";
	private String host = "127.0.0.1";
	private int port = 8083;
	
    private Class serviceInterface;
	private Object serviceImpl;
	
	private Server serviceContainer;
	
	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

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

	public Class getServiceInterface() {
		return serviceInterface;
	}

	public void setServiceInterface(Class serviceInterface) {
		this.serviceInterface = serviceInterface;
	}

	public Object getServiceImpl() {
		return serviceImpl;
	}

	public void setServiceImpl(Object serviceImpl) {
		this.serviceImpl = serviceImpl;
	}

	public boolean start(){
		System.out.println("start AVRO service-->"+serviceInterface.getName());
        Responder responder = new SpecificResponder(serviceInterface, serviceImpl);
        try {
            URI uri = new URI(scheme, null, host, port, null,null, null);
            serviceContainer = Ipc.createServer(responder, uri);
            serviceContainer.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
	
	public void stop() {
		if(null!=serviceContainer) {
			serviceContainer.close();
		}
	}
}
