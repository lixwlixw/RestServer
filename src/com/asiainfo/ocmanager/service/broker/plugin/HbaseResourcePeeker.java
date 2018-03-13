package com.asiainfo.ocmanager.service.broker.plugin;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.log4j.Logger;

import com.asiainfo.ocmanager.service.broker.imp.BaseResourcePeeker;
import com.asiainfo.ocmanager.service.client.v2.ServiceClient;
import com.asiainfo.ocmanager.service.client.v2.ServiceClientInterface;
import com.asiainfo.ocmanager.service.client.v2.ServiceClientPool;
import com.asiainfo.ocmanager.service.client.v2.HbaseClient;

/**
 * Implementing broker of ResourcePeeker for Hbase service.
 * 
 * @author EthanWang
 *
 */
public class HbaseResourcePeeker extends BaseResourcePeeker {
	private static final Logger LOG = Logger.getLogger(HbaseResourcePeeker.class);
	private static final String KEY_MAX_TABLE = "hbase.namespace.quota.maxtables";
	private static final String KEY_MAX_REGION = "hbase.namespace.quota.maxregions";
	private HbaseClient client;

	public HbaseResourcePeeker(String serviceName) {
		super(serviceName);
		if (serviceName.isEmpty()) return;
		try {
			ServiceClientInterface cli = ServiceClientPool.getInstance().getClient(serviceName);
			if (!(cli instanceof HbaseClient)) {
				LOG.error("Client type error for serviceName: " + serviceName + ", error type: " + cli.getClass().getName());
				throw new RuntimeException("Client type error for serviceName: " + serviceName + ", error type: " + cli.getClass().getName());
			}
			client = (HbaseClient)cli;
		} catch (Exception e) {
			LOG.error("Exception when init peeker: ", e);
			throw new RuntimeException("Exception when init peeker: ", e);
		}
	}
	
	@Override
	protected void setup() {
	}

	@Override
	protected void cleanup() {
	}

	@Override
	protected Long fetchTotalQuota(String resourceType, String nsName) {
		Admin admin = null;
		try {
			admin = client.createAdmin();
			NamespaceDescriptor des = admin.getNamespaceDescriptor(nsName);
			if (resourceType.equals("maximumTablesQuota")) {
				String max = des.getConfigurationValue(KEY_MAX_TABLE);
				return max != null ? Long.valueOf(des.getConfigurationValue(KEY_MAX_TABLE)) : -1l;
			} else if (resourceType.equals("maximumRegionsQuota")) {
				String max = des.getConfigurationValue(KEY_MAX_REGION);
				return max != null ? Long.valueOf(des.getConfigurationValue(KEY_MAX_REGION)) : -1l;
			} else {
				LOG.error("Unknown resourceType: " + resourceType);
				throw new RuntimeException("Unknown resourceType: " + resourceType);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} finally {
			if (admin != null) {
				try {
					admin.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	protected Long fetchUsedQuota(String resourceType, String nsName) {
		Admin admin = null;
		try {
			admin = client.createAdmin();
			TableName[] tables = admin.listTableNamesByNamespace(nsName);
			if (resourceType.equals("maximumTablesQuota")) {
				return Long.valueOf(tables.length);
			} else if (resourceType.equals("maximumRegionsQuota")) {
				return countRegions(tables, admin);
			} else {
				LOG.error("Unknown resourceType: " + resourceType);
				throw new RuntimeException("Unknown resourceType: " + resourceType);
			}

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} finally {
			if (admin != null) {
				try {
					admin.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Count the total regions of the tables.
	 * 
	 * @param tables
	 * @param admin
	 * @return
	 * @throws IOException
	 */
	private Long countRegions(TableName[] tables, Admin admin) throws IOException {
		int number = 0;
		for (TableName t : tables) {
			number = number + admin.getTableRegions(t).size();
		}
		return Long.valueOf(number);
	}

	@Override
	public List<String> resourceTypes() {
		return Arrays.asList("maximumTablesQuota", "maximumRegionsQuota");
	}


	@Override
	public Class<? extends ServiceClient> getClientClass() {
		return HbaseClient.class;
	}

}
