/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.cloud.controller.messaging.topology;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.domain.Cartridge;
import org.apache.stratos.cloud.controller.exception.InvalidCartridgeTypeException;
import org.apache.stratos.cloud.controller.exception.InvalidMemberException;
import org.apache.stratos.cloud.controller.messaging.publisher.StatisticsDataPublisher;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.domain.applications.ClusterDataHolder;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.applications.ApplicationInstanceTerminatedEvent;
import org.apache.stratos.messaging.event.cluster.status.*;
import org.apache.stratos.messaging.event.instance.status.InstanceActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceMaintenanceModeEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceReadyToShutdownEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.metadata.client.defaults.DefaultMetaDataServiceClient;
import org.apache.stratos.metadata.client.defaults.MetaDataServiceClient;

import java.util.*;

/**
 * this is to manipulate the received events by cloud controller
 * and build the complete topology with the events received
 */
public class TopologyBuilder {
    private static final Log log = LogFactory.getLog(TopologyBuilder.class);


    public static void handleServiceCreated(List<Cartridge> cartridgeList) {
        Service service;
        Topology topology = TopologyManager.getTopology();
        if (cartridgeList == null) {
        	log.warn(String.format("Cartridge list is empty"));
        	return;
        }

        try {

            TopologyManager.acquireWriteLock();
            for (Cartridge cartridge : cartridgeList) {
                if (!topology.serviceExists(cartridge.getType())) {
                    service = new Service(cartridge.getType(), cartridge.isMultiTenant() ? ServiceType.MultiTenant : ServiceType.SingleTenant);
                    List<PortMapping> portMappings = cartridge.getPortMappings();
                    Properties properties = new Properties();
                    for (Map.Entry<String, String> entry : cartridge.getProperties().entrySet()) {
                        properties.setProperty(entry.getKey(), entry.getValue());
                    }
                    service.setProperties(properties);
                    Port port;
                    //adding ports to the event
                    for (PortMapping portMapping : portMappings) {
                        port = new Port(portMapping.getProtocol(),
                                Integer.parseInt(portMapping.getPort()),
                                Integer.parseInt(portMapping.getProxyPort()));
                        service.addPort(port);
                    }
                    topology.addService(service);
                    TopologyManager.updateTopology(topology);
                }
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendServiceCreateEvent(cartridgeList);

    }

    public static void handleServiceRemoved(List<Cartridge> cartridgeList) {
        Topology topology = TopologyManager.getTopology();

        for (Cartridge cartridge : cartridgeList) {
            if (topology.getService(cartridge.getType()).getClusters().size() == 0) {
                if (topology.serviceExists(cartridge.getType())) {
                    try {
                        TopologyManager.acquireWriteLock();
                        topology.removeService(cartridge.getType());
                        TopologyManager.updateTopology(topology);
                    } finally {
                        TopologyManager.releaseWriteLock();
                    }
                    TopologyEventPublisher.sendServiceRemovedEvent(cartridgeList);
                } else {
                    log.warn(String.format("Service %s does not exist..", cartridge.getType()));
                }
            } else {
                log.warn("Subscription already exists. Hence not removing the service:" + cartridge.getType()
                        + " from the topology");
            }
        }
    }

    public static void handleClusterCreated(ClusterStatusClusterCreatedEvent event) {
        TopologyManager.acquireWriteLock();
        Cluster cluster;

        try {
            Topology topology = TopologyManager.getTopology();
            Service service = topology.getService(event.getServiceName());
            if (service == null) {
                log.error("Service " + event.getServiceName() +
                        " not found in Topology, unable to update the cluster status to Created");
                return;
            }

            if (service.clusterExists(event.getClusterId())) {
                log.warn("Cluster " + event.getClusterId() + " is already in the Topology ");
                return;
            } else {
                cluster = new Cluster(event.getServiceName(),
                        event.getClusterId(), event.getDeploymentPolicyName(),
                        event.getAutosScalePolicyName(), event.getAppId());
                //cluster.setStatus(Status.Created);
                cluster.setHostNames(event.getHostNames());
                cluster.setTenantRange(event.getTenantRange());
                service.addCluster(cluster);
                TopologyManager.updateTopology(topology);
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }

        TopologyEventPublisher.sendClusterCreatedEvent(cluster);
    }

    public static void handleApplicationClustersCreated(String appId, List<Cluster> appClusters) {

        TopologyManager.acquireWriteLock();

        try {
            Topology topology = TopologyManager.getTopology();

            for (Cluster cluster : appClusters) {
                Service service = topology.getService(cluster.getServiceName());
                if (service == null) {
                    log.error("Service " + cluster.getServiceName()
                            + " not found in Topology, unable to create Application cluster");
                } else {
                    service.addCluster(cluster);
                    log.info("Application Cluster " + cluster.getClusterId() + " created in CC topology");
                }
            }

            TopologyManager.updateTopology(topology);

        } finally {
            TopologyManager.releaseWriteLock();
        }

        TopologyEventPublisher.sendApplicationClustersCreated(appId, appClusters);

    }

    public static void handleApplicationClustersRemoved(String appId, Set<ClusterDataHolder> clusterData) {
        TopologyManager.acquireWriteLock();

        List<Cluster> removedClusters = new ArrayList<Cluster>();
        CloudControllerContext context = CloudControllerContext.getInstance();
        try {
            Topology topology = TopologyManager.getTopology();

            if (clusterData != null) {
                // remove clusters from CC topology model and remove runtime information
                for (ClusterDataHolder aClusterData : clusterData) {
                    Service aService = topology.getService(aClusterData.getServiceType());
                    if (aService != null) {
                        removedClusters.add(aService.removeCluster(aClusterData.getClusterId()));
                    } else {
                        log.warn("Service " + aClusterData.getServiceType() + " not found, unable to remove Cluster " + aClusterData.getClusterId());
                    }
                    // remove runtime data
                    context.removeClusterContext(aClusterData.getClusterId());

                    log.info("Removed application [ " + appId + " ]'s Cluster [ " + aClusterData.getClusterId() + " ] from the topology");
                }
                // persist runtime data changes
                CloudControllerContext.getInstance().persist();
            } else {
                log.info("No cluster data found for application " + appId + " to remove");
            }

            TopologyManager.updateTopology(topology);

        } finally {
            TopologyManager.releaseWriteLock();
        }

        TopologyEventPublisher.sendApplicationClustersRemoved(appId, clusterData);

    }

    public static void handleClusterReset(ClusterStatusClusterResetEvent event) {
        TopologyManager.acquireWriteLock();

        try {
            Topology topology = TopologyManager.getTopology();
            Service service = topology.getService(event.getServiceName());
            if (service == null) {
                log.error("Service " + event.getServiceName() +
                        " not found in Topology, unable to update the cluster status to Created");
                return;
            }

            Cluster cluster = service.getCluster(event.getClusterId());
            if (cluster == null) {
                log.error("Cluster " + event.getClusterId() + " not found in Topology, unable to update " +
                        "status to Created");
                return;
            }

            ClusterInstance context = cluster.getInstanceContexts(event.getInstanceId());
            if (context == null) {
                log.warn("Cluster Instance Context is not found for [cluster] " +
                        event.getClusterId() + " [instance-id] " +
                        event.getInstanceId());
                return;
            }
            ClusterStatus status = ClusterStatus.Created;
            if(context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster Created adding status started for" + cluster.getClusterId());
                TopologyManager.updateTopology(topology);
                //publishing data
                TopologyEventPublisher.sendClusterResetEvent(event.getAppId(), event.getServiceName(),
                        event.getClusterId(), event.getInstanceId());
            } else {
                log.warn(String.format("Cluster state transition is not valid: [cluster-id] %s " +
                                " [instance-id] %s [current-status] %s [status-requested] %s",
                        event.getClusterId(), event.getInstanceId(),
                        context.getStatus(), status));
            }

        } finally {
            TopologyManager.releaseWriteLock();
        }


    }

    public static void handleClusterInstanceCreated(String serviceType, String clusterId,
                                                    String alias, String instanceId, String partitionId,
                                                    String networkPartitionId) {

        TopologyManager.acquireWriteLock();

        try {
            Topology topology = TopologyManager.getTopology();
            Service service = topology.getService(serviceType);
            if (service == null) {
                log.error("Service " + serviceType +
                        " not found in Topology, unable to update the cluster status to Created");
                return;
            }

            Cluster cluster = service.getCluster(clusterId);
            if (cluster == null) {
                log.error("Cluster " + clusterId + " not found in Topology, unable to update " +
                        "status to Created");
                return;
            }

            if(cluster.getInstanceContexts(instanceId) != null) {
                log.warn("The Instance context for the cluster already exists for [cluster] " +
                        clusterId + " [instance-id] " + instanceId);
                return;
            }

            ClusterInstance clusterInstance = new ClusterInstance(alias, clusterId, instanceId);
            clusterInstance.setNetworkPartitionId(networkPartitionId);
            clusterInstance.setPartitionId(partitionId);
            cluster.addInstanceContext(instanceId, clusterInstance);
            TopologyManager.updateTopology(topology);

            ClusterInstanceCreatedEvent clusterInstanceCreatedEvent =
                    new ClusterInstanceCreatedEvent(serviceType, clusterId,
                            clusterInstance);
            clusterInstanceCreatedEvent.setPartitionId(partitionId);
            TopologyEventPublisher.sendClusterInstanceCreatedEvent(clusterInstanceCreatedEvent);

        } finally {
            TopologyManager.releaseWriteLock();
        }
    }



    public static void handleClusterCreated(Registrant registrant, boolean isLb) {
        /*Topology topology = TopologyManager.getTopology();
        Service service;
        try {
            TopologyManager.acquireWriteLock();
            String cartridgeType = registrant.getCartridgeType();
            service = topology.getService(cartridgeType);
            Properties props = CloudControllerUtil.toJavaUtilProperties(registrant.getProperties());

            Cluster cluster;
            String clusterId = registrant.getClusterId();
            if (service.clusterExists(clusterId)) {
                // update the cluster
                cluster = service.getCluster(clusterId);
                cluster.addHostName(registrant.getHostName());
                if (service.getServiceType() == ServiceType.MultiTenant) {
                    cluster.setTenantRange(registrant.getTenantRange());
                }
                if (service.getProperties().getProperty(Constants.IS_PRIMARY) != null) {
                    props.setProperty(Constants.IS_PRIMARY, service.getProperties().getProperty(Constants.IS_PRIMARY));
                }
                cluster.setProperties(props);
                cluster.setLbCluster(isLb);
            } else {
                cluster = new Cluster(cartridgeType, clusterId,
                        registrant.getDeploymentPolicyName(), registrant.getAutoScalerPolicyName(), null);
                cluster.addHostName(registrant.getHostName());
                if (service.getServiceType() == ServiceType.MultiTenant) {
                    cluster.setTenantRange(registrant.getTenantRange());
                }
                if (service.getProperties().getProperty(Constants.IS_PRIMARY) != null) {
                    props.setProperty(Constants.IS_PRIMARY, service.getProperties().getProperty(Constants.IS_PRIMARY));
                }
                cluster.setProperties(props);
                cluster.setLbCluster(isLb);
                //cluster.setStatus(Status.Created);
                service.addCluster(cluster);
            }
            TopologyManager.updateTopology(topology);
            TopologyEventPublisher.sendClusterCreatedEvent(cartridgeType, clusterId, cluster);

        } finally {
            TopologyManager.releaseWriteLock();
        }*/
    }


    private static void setKubernetesCluster(Cluster cluster) {  
    	boolean isKubernetesCluster = (cluster.getProperties().getProperty(StratosConstants.KUBERNETES_CLUSTER_ID) != null);
		if (log.isDebugEnabled()) {
			log.debug(" Kubernetes Cluster ["+ isKubernetesCluster + "] ");
		}
		cluster.setKubernetesCluster(isKubernetesCluster);		
	}

	public static void handleClusterRemoved(ClusterContext ctxt) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(ctxt.getCartridgeType());
        String deploymentPolicy;
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                    ctxt.getCartridgeType()));
            return;
        }

        if (!service.clusterExists(ctxt.getClusterId())) {
            log.warn(String.format("Cluster %s does not exist for service %s",
                    ctxt.getClusterId(),
                    ctxt.getCartridgeType()));
            return;
        }

        try {
            TopologyManager.acquireWriteLock();
            Cluster cluster = service.removeCluster(ctxt.getClusterId());
            deploymentPolicy = cluster.getDeploymentPolicyName();
            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendClusterRemovedEvent(ctxt, deploymentPolicy);
    }

	public static void handleMemberSpawned(String serviceName,
			String clusterId, String partitionId,
			String privateIp, String publicIp, MemberContext context) {
		// adding the new member to the cluster after it is successfully started
		// in IaaS.
		Topology topology = TopologyManager.getTopology();
		Service service = topology.getService(serviceName);
		Cluster cluster = service.getCluster(clusterId);
		String memberId = context.getMemberId();
		String networkPartitionId = context.getNetworkPartitionId();
		String lbClusterId = context.getLbClusterId();
		long initTime = context.getInitTime();

		if (cluster.memberExists(memberId)) {
			log.warn(String.format("Member %s already exists", memberId));
			return;
		}

		try {
			TopologyManager.acquireWriteLock();
			Member member = new Member(serviceName, clusterId,
					networkPartitionId, partitionId, memberId, initTime);
			member.setStatus(MemberStatus.Created);
            member.setInstanceId(context.getInstanceId());
			member.setMemberIp(privateIp);
			member.setLbClusterId(lbClusterId);
			member.setMemberPublicIp(publicIp);
			member.setProperties(CloudControllerUtil.toJavaUtilProperties(context.getProperties()));
            try {

                Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(serviceName);
                List<PortMapping> portMappings = cartridge.getPortMappings();
                Port port;
                if(cluster.isKubernetesCluster()){
                    // Update port mappings with generated service proxy port
                    // TODO: Need to properly fix with the latest Kubernetes version
                    String serviceHostPortStr = CloudControllerUtil.getProperty(context.getProperties(), StratosConstants.ALLOCATED_SERVICE_HOST_PORT);
                    if(StringUtils.isEmpty(serviceHostPortStr)) {
                        log.warn("Kubernetes service host port not found for member: [member-id] " + memberId);
                    }
                    // Adding ports to the member
                    if (StringUtils.isNotEmpty(serviceHostPortStr)) {
                        for (PortMapping portMapping : portMappings) {
                            port = new Port(portMapping.getProtocol(),
                                    Integer.parseInt(serviceHostPortStr),
                                    Integer.parseInt(portMapping.getProxyPort()));
                            member.addPort(port);
                        }
                    }

                } else {

                    // Adding ports to the member
                    for (PortMapping portMapping : portMappings) {

                        port = new Port(portMapping.getProtocol(),
                                Integer.parseInt(portMapping.getPort()),
                                Integer.parseInt(portMapping.getProxyPort()));
                        member.addPort(port);

                    }
                }

            } catch (Exception e) {
                log.error("Could not update member port-map: [member-id] " + memberId, e);
            }
			cluster.addMember(member);
			TopologyManager.updateTopology(topology);
		} finally {
			TopologyManager.releaseWriteLock();
		}
		
		TopologyEventPublisher.sendInstanceSpawnedEvent(serviceName, clusterId,
				networkPartitionId, partitionId, memberId, lbClusterId,
				publicIp, privateIp, context);
	}
    
    public static void handleMemberStarted(InstanceStartedEvent instanceStartedEvent) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(instanceStartedEvent.getServiceName());
        if (service == null) {
        	log.warn(String.format("Service %s does not exist",
                    instanceStartedEvent.getServiceName()));
        	return;
        }
        if (!service.clusterExists(instanceStartedEvent.getClusterId())) {
        	log.warn(String.format("Cluster %s does not exist in service %s",
                    instanceStartedEvent.getClusterId(),
                    instanceStartedEvent.getServiceName()));
        	return;
        }

        Member member = service.getCluster(instanceStartedEvent.getClusterId()).
                getMember(instanceStartedEvent.getMemberId());
        if (member == null) {
        	log.warn(String.format("Member %s does not exist",
                    instanceStartedEvent.getMemberId()));
        	return;
        }

        try {
            TopologyManager.acquireWriteLock();
            // try update lifecycle state
            if (!member.isStateTransitionValid(MemberStatus.Starting)) {
                log.error("Invalid State Transition from " + member.getStatus() + " to " +
                        MemberStatus.Starting);
                return;
            } else {
                member.setStatus(MemberStatus.Starting);
                log.info("member started event adding status started");

                TopologyManager.updateTopology(topology);
                //memberStartedEvent.
                TopologyEventPublisher.sendMemberStartedEvent(instanceStartedEvent);
                //publishing data
                StatisticsDataPublisher.publish(instanceStartedEvent.getMemberId(),
                        instanceStartedEvent.getPartitionId(),
                        instanceStartedEvent.getNetworkPartitionId(),
                        instanceStartedEvent.getClusterId(),
                        instanceStartedEvent.getServiceName(),
                        MemberStatus.Starting.toString(),
                        null);
            }

        } finally {
            TopologyManager.releaseWriteLock();
        }

    }

    public static void handleMemberActivated(InstanceActivatedEvent instanceActivatedEvent) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(instanceActivatedEvent.getServiceName());
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                                                     instanceActivatedEvent.getServiceName()));
            return;
        }
        
        Cluster cluster = service.getCluster(instanceActivatedEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster %s does not exist",
                                                     instanceActivatedEvent.getClusterId()));
            return;
        }

        Member member = cluster.getMember(instanceActivatedEvent.getMemberId());

        if (member == null) {
        	log.warn(String.format("Member %s does not exist",
                    instanceActivatedEvent.getMemberId()));
        	return;
        }

        MemberActivatedEvent memberActivatedEvent = new MemberActivatedEvent(
                                                        instanceActivatedEvent.getServiceName(),
                                                        instanceActivatedEvent.getClusterId(),
                                                        instanceActivatedEvent.getNetworkPartitionId(),
                                                        instanceActivatedEvent.getPartitionId(),
                                                        instanceActivatedEvent.getMemberId(),
                                                        instanceActivatedEvent.getInstanceId());

        // grouping - set grouid
        //TODO
        memberActivatedEvent.setApplicationId(null);
        try {
            TopologyManager.acquireWriteLock();
            // try update lifecycle state
            if (!member.isStateTransitionValid(MemberStatus.Activated)) {
                log.error("Invalid State Transition from " + member.getStatus() + " to " +
                        MemberStatus.Activated);
                return;
            } else {
                member.setStatus(MemberStatus.Activated);
                log.info("member started event adding status activated");
                Cartridge cartridge = CloudControllerContext.getInstance().
                        getCartridge(instanceActivatedEvent.getServiceName());

                List<PortMapping> portMappings = cartridge.getPortMappings();
                Port port;
                //adding ports to the event
                for (PortMapping portMapping : portMappings) {
                    port = new Port(portMapping.getProtocol(),
                            Integer.parseInt(portMapping.getPort()),
                            Integer.parseInt(portMapping.getProxyPort()));
                    member.addPort(port);
                    memberActivatedEvent.addPort(port);
                }

                memberActivatedEvent.setMemberIp(member.getMemberIp());
                memberActivatedEvent.setMemberPublicIp(member.getMemberPublicIp());
                TopologyManager.updateTopology(topology);

                TopologyEventPublisher.sendMemberActivatedEvent(memberActivatedEvent);
                //publishing data
                StatisticsDataPublisher.publish(memberActivatedEvent.getMemberId(),
                        memberActivatedEvent.getPartitionId(),
                        memberActivatedEvent.getNetworkPartitionId(),
                        memberActivatedEvent.getClusterId(),
                        memberActivatedEvent.getServiceName(),
                        MemberStatus.Activated.toString(),
                        null);
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }

    }

    public static void handleMemberReadyToShutdown(InstanceReadyToShutdownEvent instanceReadyToShutdownEvent)
                            throws InvalidMemberException, InvalidCartridgeTypeException {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(instanceReadyToShutdownEvent.getServiceName());
        //update the status of the member
        if (service == null) {
        	log.warn(String.format("Service %s does not exist",
                                                     instanceReadyToShutdownEvent.getServiceName()));
        	return;
        }

        Cluster cluster = service.getCluster(instanceReadyToShutdownEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster %s does not exist",
                                                     instanceReadyToShutdownEvent.getClusterId()));
            return;
        }


        Member member = cluster.getMember(instanceReadyToShutdownEvent.getMemberId());
        if (member == null) {
            log.warn(String.format("Member %s does not exist",
                    instanceReadyToShutdownEvent.getMemberId()));
            return;
        }
        MemberReadyToShutdownEvent memberReadyToShutdownEvent = new MemberReadyToShutdownEvent(
                                                                instanceReadyToShutdownEvent.getServiceName(),
                                                                instanceReadyToShutdownEvent.getClusterId(),
                                                                instanceReadyToShutdownEvent.getNetworkPartitionId(),
                                                                instanceReadyToShutdownEvent.getPartitionId(),
                                                                instanceReadyToShutdownEvent.getMemberId(),
                                                                instanceReadyToShutdownEvent.getInstanceId());
        try {
            TopologyManager.acquireWriteLock();

            if (!member.isStateTransitionValid(MemberStatus.ReadyToShutDown)) {
                log.error("Invalid State Transition from " + member.getStatus() + " to " +
                        MemberStatus.ReadyToShutDown);
                return;
            }
            member.setStatus(MemberStatus.ReadyToShutDown);
            log.info("Member Ready to shut down event adding status started");

            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        TopologyEventPublisher.sendMemberReadyToShutdownEvent(memberReadyToShutdownEvent);
        //publishing data
        StatisticsDataPublisher.publish(instanceReadyToShutdownEvent.getMemberId(),
                instanceReadyToShutdownEvent.getPartitionId(),
                instanceReadyToShutdownEvent.getNetworkPartitionId(),
                instanceReadyToShutdownEvent.getClusterId(),
                instanceReadyToShutdownEvent.getServiceName(),
                MemberStatus.ReadyToShutDown.toString(),
                null);
        //termination of particular instance will be handled by autoscaler
    }

     public static void handleMemberMaintenance(InstanceMaintenanceModeEvent instanceMaintenanceModeEvent)
                            throws InvalidMemberException, InvalidCartridgeTypeException {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(instanceMaintenanceModeEvent.getServiceName());
        //update the status of the member
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                                                     instanceMaintenanceModeEvent.getServiceName()));
            return;
        }

        Cluster cluster = service.getCluster(instanceMaintenanceModeEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster %s does not exist",
                                                     instanceMaintenanceModeEvent.getClusterId()));
            return;
        }

        Member member = cluster.getMember(instanceMaintenanceModeEvent.getMemberId());
        if (member == null) {
            log.warn(String.format("Member %s does not exist",
                    instanceMaintenanceModeEvent.getMemberId()));
            return;
        }


        MemberMaintenanceModeEvent memberMaintenanceModeEvent = new MemberMaintenanceModeEvent(
                                                                instanceMaintenanceModeEvent.getServiceName(),
                                                                instanceMaintenanceModeEvent.getClusterId(),
                                                                instanceMaintenanceModeEvent.getNetworkPartitionId(),
                                                                instanceMaintenanceModeEvent.getPartitionId(),
                                                                instanceMaintenanceModeEvent.getMemberId(),
                                                                instanceMaintenanceModeEvent.getInstanceId());
        try {
            TopologyManager.acquireWriteLock();
            // try update lifecycle state
            if (!member.isStateTransitionValid(MemberStatus.In_Maintenance)) {
                log.error("Invalid State Transition from " + member.getStatus() + " to "
                        + MemberStatus.In_Maintenance);
                return;
            }
            member.setStatus(MemberStatus.In_Maintenance);
            log.info("member maintenance mode event adding status started");

            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        //publishing data
        TopologyEventPublisher.sendMemberMaintenanceModeEvent(memberMaintenanceModeEvent);

    }

    /***
     * Remove member from topology and send member terminated event.
     * @param serviceName
     * @param clusterId
     * @param networkPartitionId
     * @param partitionId
     * @param memberId
     */
    public static void handleMemberTerminated(String serviceName, String clusterId,
                                              String networkPartitionId, String partitionId,
                                              String memberId) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(serviceName);
        Properties properties;
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                                                     serviceName));
            return;
        }
        Cluster cluster = service.getCluster(clusterId);
        if (cluster == null) {
            log.warn(String.format("Cluster %s does not exist",
                                                     clusterId));
            return;
        }
        
        Member member = cluster.getMember(memberId);
        String instanceId = member.getInstanceId();

		if (member == null) {
			log.warn(String.format("Member with member id %s does not exist",
					memberId));
			return;
		}

        try {
            TopologyManager.acquireWriteLock();
            properties = member.getProperties();
            cluster.removeMember(member);
            TopologyManager.updateTopology(topology);
        } finally {
            TopologyManager.releaseWriteLock();
        }
        /* @TODO leftover from grouping_poc*/
        String groupAlias = null;
        TopologyEventPublisher.sendMemberTerminatedEvent(serviceName, clusterId, networkPartitionId,
                partitionId, memberId, properties, groupAlias, instanceId);
    }

    public static void handleMemberSuspended() {
        //TODO
        try {
            TopologyManager.acquireWriteLock();
        } finally {
            TopologyManager.releaseWriteLock();
        }
    }

    public static void handleClusterActivatedEvent(ClusterStatusClusterActivatedEvent clusterActivatedEvent) {

        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(clusterActivatedEvent.getServiceName());
        //update the status of the cluster
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                    clusterActivatedEvent.getServiceName()));
            return;
        }

        Cluster cluster = service.getCluster(clusterActivatedEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster %s does not exist",
                    clusterActivatedEvent.getClusterId()));
            return;
        }

        ClusterInstanceActivatedEvent clusterActivatedEvent1 =
                new ClusterInstanceActivatedEvent(
                        clusterActivatedEvent.getAppId(),
                        clusterActivatedEvent.getServiceName(),
                        clusterActivatedEvent.getClusterId(),
                        clusterActivatedEvent.getInstanceId());
        try {
            TopologyManager.acquireWriteLock();
            ClusterInstance context = cluster.getInstanceContexts(clusterActivatedEvent.getInstanceId());
            if (context == null) {
                log.warn("Cluster Instance Context is not found for [cluster] " +
                        clusterActivatedEvent.getClusterId() + " [instance-id] " +
                        clusterActivatedEvent.getInstanceId());
                return;
            }
            ClusterStatus status = ClusterStatus.Active;
            if(context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster activated adding status started for" + cluster.getClusterId());
                TopologyManager.updateTopology(topology);
                //publishing data
                TopologyEventPublisher.sendClusterActivatedEvent(clusterActivatedEvent1);
            } else {
                log.error(String.format("Cluster state transition is not valid: [cluster-id] %s " +
                                " [instance-id] %s [current-status] %s [status-requested] %s",
                        clusterActivatedEvent.getClusterId(), clusterActivatedEvent.getInstanceId(),
                        context.getStatus(), status));
                return;
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }

    }

    public static void handleClusterInactivateEvent(
            ClusterStatusClusterInactivateEvent clusterInactivateEvent) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(clusterInactivateEvent.getServiceName());
        //update the status of the cluster
        if (service == null) {
            log.warn(String.format("Service %s does not exist",
                    clusterInactivateEvent.getServiceName()));
            return;
        }

        Cluster cluster = service.getCluster(clusterInactivateEvent.getClusterId());
        if (cluster == null) {
            log.warn(String.format("Cluster %s does not exist",
                    clusterInactivateEvent.getClusterId()));
            return;
        }

        ClusterInstanceInactivateEvent clusterInactivatedEvent1 =
                new ClusterInstanceInactivateEvent(
                        clusterInactivateEvent.getAppId(),
                        clusterInactivateEvent.getServiceName(),
                        clusterInactivateEvent.getClusterId(),
                        clusterInactivateEvent.getInstanceId());
        try {
            TopologyManager.acquireWriteLock();
            ClusterInstance context = cluster.getInstanceContexts(clusterInactivateEvent.getInstanceId());
            if (context == null) {
                log.warn("Cluster Instance Context is not found for [cluster] " +
                        clusterInactivateEvent.getClusterId() + " [instance-id] " +
                        clusterInactivateEvent.getInstanceId());
                return;
            }
            ClusterStatus status = ClusterStatus.Inactive;
            if(context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster Inactive adding status started for" + cluster.getClusterId());
                TopologyManager.updateTopology(topology);
                //publishing data
                TopologyEventPublisher.sendClusterInactivateEvent(clusterInactivatedEvent1);
            } else {
                log.error(String.format("Cluster state transition is not valid: [cluster-id] %s " +
                                " [instance-id] %s [current-status] %s [status-requested] %s",
                        clusterInactivateEvent.getClusterId(), clusterInactivateEvent.getInstanceId(),
                        context.getStatus(), status));
                return;
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }
    }


    private static void deleteAppResourcesFromMetadataService(ApplicationInstanceTerminatedEvent event) {
        try {
            MetaDataServiceClient metadataClient = new DefaultMetaDataServiceClient();
            metadataClient.deleteApplicationProperties(event.getAppId());
        } catch (Exception e) {
            log.error("Error occurred while deleting the application resources frm metadata service ", e);
        }
    }

    public static void handleClusterTerminatedEvent(ClusterStatusClusterTerminatedEvent event) {

        TopologyManager.acquireWriteLock();

        try {
            Topology topology = TopologyManager.getTopology();
            Service service = topology.getService(event.getServiceName());

            //update the status of the cluster
            if (service == null) {
                log.warn(String.format("Service %s does not exist",
                        event.getServiceName()));
                return;
            }

            Cluster cluster = service.getCluster(event.getClusterId());
            if (cluster == null) {
                log.warn(String.format("Cluster %s does not exist",
                        event.getClusterId()));
                return;
            }

            ClusterInstance context = cluster.getInstanceContexts(event.getInstanceId());
            if (context == null) {
                log.warn("Cluster Instance Context is not found for [cluster] " +
                        event.getClusterId() + " [instance-id] " +
                        event.getInstanceId());
                return;
            }
            ClusterStatus status = ClusterStatus.Terminated;
            if(context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster Terminated adding status started for and removing the cluster instance"
                                        + cluster.getClusterId());
                cluster.removeInstanceContext(event.getInstanceId());
                TopologyManager.updateTopology(topology);
                //publishing data
                ClusterInstanceTerminatedEvent clusterTerminatedEvent = new ClusterInstanceTerminatedEvent(event.getAppId(),
                        event.getServiceName(), event.getClusterId(), event.getInstanceId());

                TopologyEventPublisher.sendClusterTerminatedEvent(clusterTerminatedEvent);
            } else {
                log.error(String.format("Cluster state transition is not valid: [cluster-id] %s " +
                                " [instance-id] %s [current-status] %s [status-requested] %s",
                        event.getClusterId(), event.getInstanceId(),
                        context.getStatus(), status));
                return;
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }


    }

    public static void handleClusterTerminatingEvent(ClusterStatusClusterTerminatingEvent event) {

        TopologyManager.acquireWriteLock();

        try {
            Topology topology = TopologyManager.getTopology();
            Cluster cluster = topology.getService(event.getServiceName()).
                    getCluster(event.getClusterId());

            if (!cluster.isStateTransitionValid(ClusterStatus.Terminating, event.getInstanceId())) {
                log.error("Invalid state transfer from " + cluster.getStatus(event.getInstanceId()) + " to " +
                        ClusterStatus.Terminating);
            }
            ClusterInstance context = cluster.getInstanceContexts(event.getInstanceId());
            if (context == null) {
                log.warn("Cluster Instance Context is not found for [cluster] " +
                        event.getClusterId() + " [instance-id] " +
                        event.getInstanceId());
                return;
            }
            ClusterStatus status = ClusterStatus.Terminating;
            if(context.isStateTransitionValid(status)) {
                context.setStatus(status);
                log.info("Cluster Terminating adding status started for" + cluster.getClusterId());
                TopologyManager.updateTopology(topology);
                //publishing data
                ClusterInstanceTerminatingEvent clusterTerminaingEvent = new ClusterInstanceTerminatingEvent(event.getAppId(),
                        event.getServiceName(), event.getClusterId(), event.getInstanceId());

                TopologyEventPublisher.sendClusterTerminatingEvent(clusterTerminaingEvent);
            } else {
                log.error(String.format("Cluster state transition is not valid: [cluster-id] %s " +
                                " [instance-id] %s [current-status] %s [status-requested] %s",
                        event.getClusterId(), event.getInstanceId(),
                        context.getStatus(), status));
                return;
            }
        } finally {
            TopologyManager.releaseWriteLock();
        }
    }
}
