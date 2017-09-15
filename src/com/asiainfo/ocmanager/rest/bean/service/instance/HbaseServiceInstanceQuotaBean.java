package com.asiainfo.ocmanager.rest.bean.service.instance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.asiainfo.ocmanager.persistence.model.ServiceInstance;
import com.asiainfo.ocmanager.persistence.model.Tenant;
import com.asiainfo.ocmanager.rest.resource.persistence.ServiceInstancePersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.persistence.TenantPersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.utils.QuotaCommonUtils;
import com.asiainfo.ocmanager.rest.resource.utils.ServiceInstanceQuotaUtils;
import com.asiainfo.ocmanager.rest.resource.utils.TenantQuotaUtils;
import com.asiainfo.ocmanager.rest.resource.utils.model.ServiceInstanceQuotaCheckerResponse;
import com.asiainfo.ocmanager.utils.ServicesDefaultQuotaConf;

/**
 * 
 * @author zhaoyim
 *
 */
public class HbaseServiceInstanceQuotaBean extends ServiceInstanceQuotaBean {

	private long maximumTablesQuota;
	private long maximumRegionsQuota;

	public HbaseServiceInstanceQuotaBean() {

	}

	/**
	 * 
	 * @param serviceType
	 * @param quotaStr
	 */
	public HbaseServiceInstanceQuotaBean(String serviceType, String quotaStr) {
		this.serviceType = serviceType;
		Map<String, String> hdfsQuotaMap = ServiceInstanceQuotaUtils.getServiceInstanceQuota(serviceType, quotaStr);
		this.maximumTablesQuota = hdfsQuotaMap.get("maximumTablesQuota") == null ? 0
				: Long.valueOf(hdfsQuotaMap.get("maximumTablesQuota")).longValue();
		this.maximumRegionsQuota = hdfsQuotaMap.get("maximumRegionsQuota") == null ? 0
				: Long.valueOf(hdfsQuotaMap.get("maximumRegionsQuota")).longValue();

	}

	/**
	 * 
	 * @param serviceType
	 * @param quotaMap
	 */
	public HbaseServiceInstanceQuotaBean(String serviceType, Map<String, String> quotaMap) {
		this.serviceType = serviceType;
		this.maximumTablesQuota = quotaMap.get("maximumTablesQuota") == null ? 0
				: Long.valueOf(quotaMap.get("maximumTablesQuota")).longValue();
		this.maximumRegionsQuota = quotaMap.get("maximumRegionsQuota") == null ? 0
				: Long.valueOf(quotaMap.get("maximumRegionsQuota")).longValue();

	}

	/**
	 * 
	 * @return
	 */
	public static HbaseServiceInstanceQuotaBean createDefaultServiceInstanceQuota() {
		HbaseServiceInstanceQuotaBean defaultServiceInstanceQuota = new HbaseServiceInstanceQuotaBean();
		defaultServiceInstanceQuota.setServiceType("hbase");
		defaultServiceInstanceQuota.setMaximumTablesQuota(
				ServicesDefaultQuotaConf.getInstance().get("hbase").get("maximumTablesQuota").getDefaultQuota());
		defaultServiceInstanceQuota.setMaximumRegionsQuota(
				ServicesDefaultQuotaConf.getInstance().get("hbase").get("maximumRegionsQuota").getDefaultQuota());
		return defaultServiceInstanceQuota;
	}

	@Override
	public ServiceInstanceQuotaCheckerResponse checkCanChangeInst(String backingServiceName, String tenantId) {

		List<ServiceInstance> serviceInstances = ServiceInstancePersistenceWrapper
				.getServiceInstanceByServiceType(tenantId, backingServiceName);
		Tenant parentTenant = TenantPersistenceWrapper.getTenantById(tenantId);

		// get all hbase children bsi quota
		HbaseServiceInstanceQuotaBean hbaseChildrenTotalQuota = new HbaseServiceInstanceQuotaBean(backingServiceName,
				new HashMap<String, String>());

		for (ServiceInstance inst : serviceInstances) {
			HbaseServiceInstanceQuotaBean quota = new HbaseServiceInstanceQuotaBean(backingServiceName,
					inst.getQuota());
			hbaseChildrenTotalQuota.plus(quota);
		}

		// get parent tenant quota
		Map<String, String> parentTenantQuotaMap = TenantQuotaUtils.getTenantQuotaByService(backingServiceName,
				parentTenant.getQuota());
		HbaseServiceInstanceQuotaBean hbaseParentTenantQuota = new HbaseServiceInstanceQuotaBean(backingServiceName,
				parentTenantQuotaMap);

		// calculate the left quota
		hbaseParentTenantQuota.minus(hbaseChildrenTotalQuota);

		// get request bsi quota
		HbaseServiceInstanceQuotaBean hbaseRequestServiceInstanceQuota = HbaseServiceInstanceQuotaBean
				.createDefaultServiceInstanceQuota();

		// left quota minus request quota
		hbaseParentTenantQuota.minus(hbaseRequestServiceInstanceQuota);

		return hbaseParentTenantQuota.checker();
	}

	public ServiceInstanceQuotaCheckerResponse checker() {

		ServiceInstanceQuotaCheckerResponse checkRes = new ServiceInstanceQuotaCheckerResponse();
		StringBuilder resStr = new StringBuilder();
		boolean canChange = true;

		// hbase
		if (this.maximumTablesQuota < 0) {
			resStr.append(QuotaCommonUtils.logAndResStr(this.maximumTablesQuota, "maximumTablesQuota", "hbase"));
			canChange = false;
		}
		if (this.maximumRegionsQuota < 0) {
			resStr.append(QuotaCommonUtils.logAndResStr(this.maximumRegionsQuota, "maximumRegionsQuota", "hbase"));
			canChange = false;
		}

		if (canChange) {
			resStr.append("can change the bsi!");
		}

		checkRes.setCanChange(canChange);
		checkRes.setMessages(resStr.toString());

		return checkRes;
	}

	/**
	 * 
	 * @param otherServiceInstanceQuota
	 */
	public void plus(HbaseServiceInstanceQuotaBean otherServiceInstanceQuota) {
		this.maximumTablesQuota = this.maximumTablesQuota + otherServiceInstanceQuota.getMaximumTablesQuota();
		this.maximumRegionsQuota = this.maximumRegionsQuota + otherServiceInstanceQuota.getMaximumRegionsQuota();
	}

	/**
	 * 
	 * @param otherServiceInstanceQuota
	 */
	public void minus(HbaseServiceInstanceQuotaBean otherServiceInstanceQuota) {
		this.maximumTablesQuota = this.maximumTablesQuota - otherServiceInstanceQuota.getMaximumTablesQuota();
		this.maximumRegionsQuota = this.maximumRegionsQuota - otherServiceInstanceQuota.getMaximumRegionsQuota();
	}

	public long getMaximumTablesQuota() {
		return maximumTablesQuota;
	}

	public void setMaximumTablesQuota(long maximumTablesQuota) {
		this.maximumTablesQuota = maximumTablesQuota;
	}

	public long getMaximumRegionsQuota() {
		return maximumRegionsQuota;
	}

	public void setMaximumRegionsQuota(long maximumRegionsQuota) {
		this.maximumRegionsQuota = maximumRegionsQuota;
	}

	@Override
	public String toString() {
		return "HbaseServiceInstanceQuotaBean [maximumTablesQuota: " + maximumTablesQuota + ", maximumRegionsQuota: "
				+ maximumRegionsQuota + ", serviceType: " + serviceType + "]";
	}
}