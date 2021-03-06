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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.domain.Cartridge;
import org.apache.stratos.cloud.controller.domain.ClusterContext;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.domain.PortMapping;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.applications.ClusterDataHolder;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Port;
import org.apache.stratos.messaging.domain.topology.ServiceType;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.util.Util;

import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * this is to send the relevant events from cloud controller to topology topic
 */
public class TopologyEventPublisher {
    private static final Log log = LogFactory.getLog(TopologyEventPublisher.class);

    public static void sendServiceCreateEvent(List<Cartridge> cartridgeList) {
        ServiceCreatedEvent serviceCreatedEvent;
        for (Cartridge cartridge : cartridgeList) {
            serviceCreatedEvent = new ServiceCreatedEvent(cartridge.getType(),
                    (cartridge.isMultiTenant() ? ServiceType.MultiTenant
                            : ServiceType.SingleTenant));

            // Add ports to the event
            Port port;
            List<PortMapping> portMappings = cartridge.getPortMappings();
            for (PortMapping portMapping : portMappings) {
                port = new Port(portMapping.getProtocol(),
                        Integer.parseInt(portMapping.getPort()),
                        Integer.parseInt(portMapping.getProxyPort()));
                serviceCreatedEvent.addPort(port);
            }

            if (log.isInfoEnabled()) {
                log.info(String.format(
                        "Publishing service created event: [service] %s",
                        cartridge.getType()));
            }
            publishEvent(serviceCreatedEvent);
        }
    }

    public static void sendServiceRemovedEvent(List<Cartridge> cartridgeList) {
        ServiceRemovedEvent serviceRemovedEvent;
        for (Cartridge cartridge : cartridgeList) {
            serviceRemovedEvent = new ServiceRemovedEvent(cartridge.getType());
            if (log.isInfoEnabled()) {
                log.info(String.format(
                        "Publishing service removed event: [service] %s",
                        serviceRemovedEvent.getServiceName()));
            }
            publishEvent(serviceRemovedEvent);
        }
    }

    public static void sendClusterResetEvent(String appId, String serviceName, String clusterId,
                                             String instanceId) {
        ClusterResetEvent clusterResetEvent = new ClusterResetEvent(appId, serviceName,
                                                                    clusterId, instanceId);

        if (log.isInfoEnabled()) {
            log.info("Publishing cluster reset event: " + clusterId);
        }
        publishEvent(clusterResetEvent);
    }

    public static void sendClusterCreatedEvent(Cluster cluster) {
        ClusterCreatedEvent clusterCreatedEvent = new ClusterCreatedEvent(cluster);

        if (log.isInfoEnabled()) {
            log.info("Publishing cluster created event: " + cluster.getClusterId());
        }
        publishEvent(clusterCreatedEvent);
    }

    public static void sendApplicationClustersCreated(String appId, List<Cluster> clusters) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Application Clusters Created event for Application: " + appId);
        }

        publishEvent(new ApplicationClustersCreatedEvent(clusters, appId));
    }

    public static void sendApplicationClustersRemoved(String appId, Set<ClusterDataHolder> clusters) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Application Clusters removed event for Application: " + appId);
        }

        publishEvent(new ApplicationClustersRemovedEvent(clusters, appId));
    }

    public static void sendClusterRemovedEvent(ClusterContext ctxt, String deploymentPolicy) {
        ClusterRemovedEvent clusterRemovedEvent = new ClusterRemovedEvent(
                ctxt.getCartridgeType(), ctxt.getClusterId(), deploymentPolicy, ctxt.isLbCluster());
        if (log.isInfoEnabled()) {
            log.info(String
                    .format("Publishing cluster removed event: [service] %s [cluster] %s",
                            ctxt.getCartridgeType(), ctxt.getClusterId()));
        }
        publishEvent(clusterRemovedEvent);

    }

    public static void sendInstanceSpawnedEvent(String serviceName,
                                                String clusterId, String networkPartitionId, String partitionId,
                                                String memberId, String lbClusterId, String publicIp,
                                                String privateIp, MemberContext context) {

        long initTime = context.getInitTime();
        InstanceSpawnedEvent instanceSpawnedEvent = new InstanceSpawnedEvent(
                serviceName, clusterId, networkPartitionId, partitionId,
                memberId, initTime, context.getInstanceId());
        instanceSpawnedEvent.setLbClusterId(lbClusterId);
        instanceSpawnedEvent.setMemberIp(privateIp);
        instanceSpawnedEvent.setMemberPublicIp(publicIp);
        instanceSpawnedEvent.setProperties(CloudControllerUtil
                .toJavaUtilProperties(context.getProperties()));
        log.info(String.format("Publishing instance spawned event: [service] %s [cluster] %s " +
                " [instance-id] %s [network-partition] %s  [partition] %s " +
                "[member]%s [lb-cluster-id] %s",
                serviceName, clusterId, context.getInstanceId(), networkPartitionId, partitionId,
                memberId, lbClusterId));
        publishEvent(instanceSpawnedEvent);
    }

    public static void sendMemberStartedEvent(InstanceStartedEvent instanceStartedEvent) {
        MemberStartedEvent memberStartedEventTopology = new MemberStartedEvent(instanceStartedEvent.getServiceName(),
                instanceStartedEvent.getClusterId(), instanceStartedEvent.getNetworkPartitionId(),
                instanceStartedEvent.getPartitionId(), instanceStartedEvent.getMemberId(),
                instanceStartedEvent.getInstanceId());
        if (log.isInfoEnabled()) {
            log.info(String
                    .format("Publishing member started event: [service] %s [cluster] %s [instance-id] %s " +
                                    "[network-partition] %s [partition] %s [member] %s",
                            instanceStartedEvent.getServiceName(),
                            instanceStartedEvent.getClusterId(),
                            instanceStartedEvent.getInstanceId(),
                            instanceStartedEvent.getNetworkPartitionId(),
                            instanceStartedEvent.getPartitionId(),
                            instanceStartedEvent.getMemberId()));
        }
        publishEvent(memberStartedEventTopology);
    }

    public static void sendMemberActivatedEvent(
            MemberActivatedEvent memberActivatedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String
                    .format("Publishing member activated event: [service] %s [cluster] %s " +
                                    "[instance-id] %s [network-partition] %s [partition] %s [member] %s",
                            memberActivatedEvent.getServiceName(),
                            memberActivatedEvent.getClusterId(),
                            memberActivatedEvent.getInstanceId(),
                            memberActivatedEvent.getNetworkPartitionId(),
                            memberActivatedEvent.getPartitionId(),
                            memberActivatedEvent.getMemberId()));
        }
        publishEvent(memberActivatedEvent);
    }

    public static void sendMemberReadyToShutdownEvent(MemberReadyToShutdownEvent memberReadyToShutdownEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing member Ready to shut down event: [service] %s " +
                            " [instance-id] %s [cluster] %s [network-partition] %s [partition] %s " +
                            "[member] %s [groupId] %s",
                                        memberReadyToShutdownEvent.getServiceName(),
                                        memberReadyToShutdownEvent.getClusterId(),
                                        memberReadyToShutdownEvent.getInstanceId(),
                                        memberReadyToShutdownEvent.getNetworkPartitionId(),
                                        memberReadyToShutdownEvent.getPartitionId(),
                                        memberReadyToShutdownEvent.getMemberId(),
                                        memberReadyToShutdownEvent.getGroupId()));
        }
        // grouping
        memberReadyToShutdownEvent.setGroupId(memberReadyToShutdownEvent.getGroupId());
        publishEvent(memberReadyToShutdownEvent);
    }

    public static void sendMemberMaintenanceModeEvent(MemberMaintenanceModeEvent memberMaintenanceModeEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing Maintenance mode event: [service] %s [cluster] %s " +
                            " [instance-id] %s [network-partition] %s [partition] %s [member] %s " +
                            "[groupId] %s", memberMaintenanceModeEvent.getServiceName(),
                                            memberMaintenanceModeEvent.getClusterId(),
                                            memberMaintenanceModeEvent.getInstanceId(),
                                            memberMaintenanceModeEvent.getNetworkPartitionId(),
                                            memberMaintenanceModeEvent.getPartitionId(),
                                            memberMaintenanceModeEvent.getMemberId(),
                                            memberMaintenanceModeEvent.getGroupId()));
        }

        publishEvent(memberMaintenanceModeEvent);
    }

    public static void sendClusterActivatedEvent(ClusterInstanceActivatedEvent clusterActivatedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing cluster activated event: [service] %s [cluster] %s " +
                            " [instance-id] %s [appId] %s",
                    clusterActivatedEvent.getServiceName(),
                    clusterActivatedEvent.getClusterId(),
                    clusterActivatedEvent.getInstanceId(),
                    clusterActivatedEvent.getAppId()));
        }
        publishEvent(clusterActivatedEvent);
    }

    public static void sendClusterInactivateEvent(ClusterInstanceInactivateEvent clusterInactiveEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing cluster inactive event: [service] %s [cluster] %s " +
                            "[instance-id] %s [appId] %s",
                    clusterInactiveEvent.getServiceName(), clusterInactiveEvent.getClusterId(),
                    clusterInactiveEvent.getInstanceId(), clusterInactiveEvent.getAppId()));
        }
        publishEvent(clusterInactiveEvent);
    }

    public static void sendClusterInstanceCreatedEvent(ClusterInstanceCreatedEvent clusterInstanceCreatedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing cluster Instance Created event: [service] %s [cluster] %s " +
                            " in [network partition] %s [instance-id] %s",
                    clusterInstanceCreatedEvent.getServiceName(), clusterInstanceCreatedEvent.getClusterId(),
                    clusterInstanceCreatedEvent.getNetworkPartitionId(),
                    clusterInstanceCreatedEvent.getClusterInstance().getInstanceId()));
        }
        publishEvent(clusterInstanceCreatedEvent);
    }


    public static void sendMemberTerminatedEvent(String serviceName, String clusterId, String networkPartitionId,
                                                 String partitionId, String memberId,
                                                 Properties properties, String groupId, String instanceId) {
        MemberTerminatedEvent memberTerminatedEvent = new MemberTerminatedEvent(serviceName, clusterId,
                networkPartitionId, partitionId, memberId, instanceId);
        memberTerminatedEvent.setProperties(properties);
        memberTerminatedEvent.setGroupId(groupId);

        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing member terminated event: [service] %s [cluster] %s " +
                            " [instance-id] %s [network-partition] %s [partition] %s [member] %s " +
                            "[groupId] %s", serviceName, clusterId, instanceId, networkPartitionId,
                    partitionId, memberId, groupId));
        }

        publishEvent(memberTerminatedEvent);
    }

    public static void sendCompleteTopologyEvent(Topology topology) {
        CompleteTopologyEvent completeTopologyEvent = new CompleteTopologyEvent(topology);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Publishing complete topology event"));
        }
        publishEvent(completeTopologyEvent);
    }

    public static void sendClusterTerminatingEvent(ClusterInstanceTerminatingEvent clusterTerminatingEvent) {

        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing Cluster terminating event: [appId] %s [cluster id] %s" +
                            " [instance-id] %s ",
                    clusterTerminatingEvent.getAppId(), clusterTerminatingEvent.getClusterId(),
                    clusterTerminatingEvent.getInstanceId()));
        }

        publishEvent(clusterTerminatingEvent);
    }

    public static void sendClusterTerminatedEvent(ClusterInstanceTerminatedEvent clusterTerminatedEvent) {

        if (log.isInfoEnabled()) {
            log.info(String.format("Publishing Cluster terminated event: [appId] %s [cluster id] %s" +
                            " [instance-id] %s ",
                    clusterTerminatedEvent.getAppId(), clusterTerminatedEvent.getClusterId(),
                    clusterTerminatedEvent.getInstanceId()));
        }

        publishEvent(clusterTerminatedEvent);
    }

    public static void publishEvent(Event event) {
        String topic = Util.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
        eventPublisher.publish(event);
    }
}
