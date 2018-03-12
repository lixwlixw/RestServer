package com.asiainfo.ocmanager.service.client.v2;

import java.io.IOException;
import java.util.Properties;

import com.asiainfo.ocmanager.utils.ServicesIni;

/**
 * All service clients should extends this class.
 * Client wrapped up service configurations(in <code>../conf/services.ini</code>) together with
 * service subject. Any Other customized client logic should 
 * be customized in extended class. Additionally, privileged actions
 * should be done by calling {@link #doPrivileged(SomeAction)}
 * @author EthanWang
 *
 */
public abstract class ServiceClient implements ServiceClientInterface {
	protected String serviceName;
	private Delegator delegator;
	protected Properties serviceConfig;
	public static final String CLIENT_CLASS = "client.class";
	public static final String AUTH_CLASS = "auth.class";
	
	public ServiceClient(String serviceName, Delegator subject) {
		this.serviceName = serviceName;
		this.delegator = subject;
		this.serviceConfig = ServicesIni.getInstance().getProperties(serviceName);
	}
	
	public Properties getServiceConfigs() {
		return this.serviceConfig;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends ServiceClient> getClientClass() {
		try {
			return (Class<? extends ServiceClient>) Class.forName(this.serviceConfig.getProperty(CLIENT_CLASS));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public Delegator getDelegator() {
		return this.delegator;
	}
	
	public String getServiceName() {
		return this.serviceName;
	}
	
	/**
	 * Execute privileged actions
	 * @param action
	 * @return
	 * @throws IOException
	 */
	public final <T> T doPrivileged(SomeAction<T> action) throws Exception {
		return this.delegator.doAsPrivileged(action);
	}
	
}
