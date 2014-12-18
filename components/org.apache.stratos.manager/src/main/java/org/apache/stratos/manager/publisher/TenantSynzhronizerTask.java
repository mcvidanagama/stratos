/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.manager.publisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.manager.internal.DataHolder;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.SubscriptionDomain;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.tenant.Subscription;
import org.apache.stratos.messaging.domain.tenant.Tenant;
import org.apache.stratos.messaging.event.tenant.CompleteTenantEvent;
import org.apache.stratos.messaging.util.Util;
import org.apache.stratos.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.ntask.core.Task;
import org.wso2.carbon.user.core.tenant.TenantManager;

/**
 * Tenant synchronizer task for publishing complete tenant event periodically
 * to message broker.
 */
public class TenantSynzhronizerTask implements Task {

	private static final Log log = LogFactory.getLog(TenantSynzhronizerTask.class);

	@Override
	public void init() {
	}

	@Override
	public void execute() {
		try {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Publishing complete tenant event"));
			}
			Tenant tenant;
			List<Tenant> tenants = new ArrayList<Tenant>();
			TenantManager tenantManager = DataHolder.getRealmService().getTenantManager();
			org.wso2.carbon.user.api.Tenant[] carbonTenants = tenantManager.getAllTenants();
			for (org.wso2.carbon.user.api.Tenant carbonTenant : carbonTenants) {
				// Create tenant
				if (log.isDebugEnabled()) {
					log.debug(String.format("Tenant found: [tenant-id] %d [tenant-domain] %s",
					                        carbonTenant.getId(), carbonTenant.getDomain()));
				}
				tenant = new Tenant(carbonTenant.getId(), carbonTenant.getDomain());

				if (!org.apache.stratos.messaging.message.receiver.tenant.TenantManager.getInstance()
				                                                                       .tenantExists(carbonTenant.getId())) {
					// if the tenant is not already there in TenantManager,
					// trigger TenantCreatedEvent
					TenantInfoBean tenantBean = new TenantInfoBean();
					tenantBean.setTenantId(carbonTenant.getId());
					tenantBean.setTenantDomain(carbonTenant.getDomain());
					TenantMgtUtil.triggerAddTenant(tenantBean);
					// add tenant to Tenant Manager
					org.apache.stratos.messaging.message.receiver.tenant.TenantManager.getInstance()
					                                                                  .addTenant(tenant);
				}

				// Add subscriptions
				// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				// List<CartridgeSubscriptionInfo> cartridgeSubscriptions =
				// PersistenceManager.getSubscriptionsForTenant(tenant.getTenantId());
				// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				Collection<CartridgeSubscription> cartridgeSubscriptions =
				                                                           new DataInsertionAndRetrievalManager().getCartridgeSubscriptions(tenant.getTenantId());
				if (cartridgeSubscriptions != null && !cartridgeSubscriptions.isEmpty()) {
					for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {
						if (log.isDebugEnabled()) {
							log.debug(String.format("Tenant subscription found: [tenant-id] %d [tenant-domain] %s [service] %s",
							                        carbonTenant.getId(), carbonTenant.getDomain(),
							                        cartridgeSubscription.getType()));
						}
						HashSet<String> clusterIds = new HashSet<String>();
						clusterIds.add(cartridgeSubscription.getCluster().getClusterDomain());
						Subscription subscription =
						                            new Subscription(
						                                             cartridgeSubscription.getType(),
						                                             clusterIds);
						for (SubscriptionDomain subscriptionDomain : cartridgeSubscription.getSubscriptionDomains()) {
							subscription.addSubscriptionDomain(subscriptionDomain.getDomainName(),
							                                   subscriptionDomain.getApplicationContext());
						}
						tenant.addSubscription(subscription);
					}
				}
				tenants.add(tenant);
			}
			CompleteTenantEvent event = new CompleteTenantEvent(tenants);
			String topic = Util.getMessageTopicName(event);
			EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
			eventPublisher.publish(event);
		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error("Could not publish complete tenant event", e);
			}
		}
	}

	@Override
	public void setProperties(Map<String, String> stringStringMap) {
	}
}
