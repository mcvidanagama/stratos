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
package org.apache.stratos.autoscaler.monitor.cluster;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.context.cluster.KubernetesClusterContext;
import org.apache.stratos.autoscaler.exception.InvalidArgumentException;
import org.apache.stratos.autoscaler.monitor.events.MonitorScalingEvent;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;

import java.util.Arrays;
import java.util.List;

/*
 * It is monitoring a kubernetes service cluster periodically.
 */
public final class KubernetesServiceClusterMonitor extends KubernetesClusterMonitor {

    private static final Log log = LogFactory.getLog(KubernetesServiceClusterMonitor.class);

    private String lbReferenceType;

    public KubernetesServiceClusterMonitor(Cluster cluster) {
        super(cluster);
        readConfigurations();
    }

    @Override
    public void run() {

        if (log.isDebugEnabled()) {
            log.debug("KubernetesServiceClusterMonitor is running..." + this.toString());
        }
        try {

            //TODO to get status from correct instance if (!ClusterStatus.Active.getNextStates().contains(getStatus())) {
                monitor();
            /*} else {
                if (log.isDebugEnabled()) {
                    log.debug("KubernetesServiceClusterMonitor is suspended as the cluster is in "
                            + getStatus() + "state");
                }
            }*/
        } catch (Exception e) {
            log.error("KubernetesServiceClusterMonitor: Monitor failed." + this.toString(),
                    e);
        }
    }

//    @Override
//    public void monitor() {
//        final String instanceId = this.getKubernetesClusterCtxt().getInstanceId();
//        Runnable monitoringRunnable = new Runnable() {
//
//            @Override
//            public void run() {
//                obsoleteCheck();
//                minCheck();
//                scaleCheck(instanceId);
//            }
//        };
//        monitoringRunnable.run();
//    }
//
//
//    private void scaleCheck(String instanceId) {
//        boolean rifReset = getKubernetesClusterCtxt().isRifReset();
//        boolean memoryConsumptionReset = getKubernetesClusterCtxt().isMemoryConsumptionReset();
//        boolean loadAverageReset = getKubernetesClusterCtxt().isLoadAverageReset();
//        if (log.isDebugEnabled()) {
//            log.debug("flag of rifReset : " + rifReset
//                    + " flag of memoryConsumptionReset : "
//                    + memoryConsumptionReset + " flag of loadAverageReset : "
//                    + loadAverageReset);
//        }
//        String kubernetesClusterID = getKubernetesClusterCtxt().getKubernetesClusterID();
//        String clusterId = getClusterId();
//        if (rifReset || memoryConsumptionReset || loadAverageReset) {
//            getScaleCheckKnowledgeSession().setGlobal("clusterId", clusterId);
//            getScaleCheckKnowledgeSession().setGlobal("autoscalePolicy", getAutoscalePolicy(instanceId));
//            getScaleCheckKnowledgeSession().setGlobal("rifReset", rifReset);
//            getScaleCheckKnowledgeSession().setGlobal("mcReset", memoryConsumptionReset);
//            getScaleCheckKnowledgeSession().setGlobal("laReset", loadAverageReset);
//            if (log.isDebugEnabled()) {
//                log.debug(String.format(
//                        "Running scale check for [kub-cluster] : %s [cluster] : %s ", kubernetesClusterID, getClusterId()));
//            }
//            scaleCheckFactHandle = AutoscalerRuleEvaluator.evaluate(
//                    getScaleCheckKnowledgeSession(), scaleCheckFactHandle, getKubernetesClusterCtxt());
//            getKubernetesClusterCtxt().setRifReset(false);
//            getKubernetesClusterCtxt().setMemoryConsumptionReset(false);
//            getKubernetesClusterCtxt().setLoadAverageReset(false);
//        } else if (log.isDebugEnabled()) {
//            log.debug(String.format("Scale check will not run since none of the statistics have not received yet for "
//                    + "[kub-cluster] : %s [cluster] : %s", kubernetesClusterID, clusterId));
//        }
//    }
//
//    private AutoscalePolicy getAutoscalePolicy(String instanceId) {
//        KubernetesClusterContext kubernetesClusterContext = (KubernetesClusterContext) this.clusterContext;
//        return kubernetesClusterContext.getAutoscalePolicy();
//    }
//
//    private void minCheck() {
//        getMinCheckKnowledgeSession().setGlobal("clusterId", getClusterId());
//        String kubernetesClusterID = getKubernetesClusterCtxt().getKubernetesClusterID();
//        if (log.isDebugEnabled()) {
//            log.debug(String.format(
//                    "Running min check for [kub-cluster] : %s [cluster] : %s ", kubernetesClusterID, getClusterId()));
//        }
//        minCheckFactHandle = AutoscalerRuleEvaluator.evaluate(
//                getMinCheckKnowledgeSession(), minCheckFactHandle,
//                getKubernetesClusterCtxt());
//    }
//
//    private void obsoleteCheck() {
//        getObsoleteCheckKnowledgeSession().setGlobal("clusterId", getClusterId());
//        String kubernetesClusterID = getKubernetesClusterCtxt().getKubernetesClusterID();
//        if (log.isDebugEnabled()) {
//            log.debug(String.format(
//                    "Running obsolete check for [kub-cluster] : %s [cluster] : %s ", kubernetesClusterID, getClusterId()));
//        }
//        obsoleteCheckFactHandle = AutoscalerRuleEvaluator.evaluate(
//                getObsoleteCheckKnowledgeSession(), obsoleteCheckFactHandle,
//                getKubernetesClusterCtxt());
//    }
//
//    @Override
//    public void destroy() {
//        getMinCheckKnowledgeSession().dispose();
//        getObsoleteCheckKnowledgeSession().dispose();
//        getScaleCheckKnowledgeSession().dispose();
//        setDestroyed(true);
//        stopScheduler();
//        if (log.isDebugEnabled()) {
//            log.debug("KubernetesServiceClusterMonitor Drools session has been disposed. " + this.toString());
//        }
//    }
//
//    @Override
//    protected void readConfigurations() {
//        XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
//        int monitorInterval = conf.getInt(AutoScalerConstants.KubernetesService_Cluster_MONITOR_INTERVAL, 60000);
//        setMonitorIntervalMilliseconds(monitorInterval);
//        if (log.isDebugEnabled()) {
//            log.debug("KubernetesServiceClusterMonitor task interval set to : " + getMonitorIntervalMilliseconds());
//        }
//    }
//
//    @Override
//    public String toString() {
//        return "KubernetesServiceClusterMonitor for " + "[ clusterId=" + getClusterId() + "]";
//    }
//
//    public String getLbReferenceType() {
//        return lbReferenceType;
//    }
//
//    public void setLbReferenceType(String lbReferenceType) {
//        this.lbReferenceType = lbReferenceType;
//    }
//
//    @Override
//    public void handleDynamicUpdates(Properties properties) throws InvalidArgumentException {
//
//        if (properties != null) {
//            Property[] propertyArray = properties.getProperties();
//            if (propertyArray == null) {
//                return;
//            }
//            List<Property> propertyList = Arrays.asList(propertyArray);
//
//            for (Property property : propertyList) {
//                String key = property.getName();
//                String value = property.getValue();
//
//                if (StratosConstants.KUBERNETES_MIN_REPLICAS.equals(key)) {
//                    int min = Integer.parseInt(value);
//                    int max = getKubernetesClusterCtxt().getMaxReplicas();
//                    if (min > max) {
//                        String msg = String.format("%s should be less than %s . But %s is not less than %s.",
//                                StratosConstants.KUBERNETES_MIN_REPLICAS, StratosConstants.KUBERNETES_MAX_REPLICAS, min, max);
//                        log.error(msg);
//                        throw new InvalidArgumentException(msg);
//                    }
//                    getKubernetesClusterCtxt().setMinReplicas(min);
//                    break;
//                }
//            }
//
//        }
//    }
//
//    @Override
//    public void terminateAllMembers(String instanceId, String networkPartitionId) {
//
//    }
//
//    @Override
//    public void onChildScalingEvent(MonitorScalingEvent scalingEvent) {
//
//    }
//
//    @Override
//    public void onParentScalingEvent(MonitorScalingEvent scalingEvent) {
//
//    }
}
