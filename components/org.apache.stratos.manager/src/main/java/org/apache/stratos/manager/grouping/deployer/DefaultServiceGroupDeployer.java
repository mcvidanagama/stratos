/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.manager.grouping.deployer;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.stub.AutoScalerServiceAutoScalerExceptionException;
import org.apache.stratos.autoscaler.stub.AutoScalerServiceInvalidServiceGroupExceptionException;
import org.apache.stratos.autoscaler.stub.pojo.Dependencies;
import org.apache.stratos.autoscaler.stub.pojo.ServiceGroup;
import org.apache.stratos.autoscaler.stub.exception.*;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceUnregisteredCartridgeExceptionException;
import org.apache.stratos.manager.client.AutoscalerServiceClient;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.InvalidServiceGroupException;
import org.apache.stratos.manager.exception.ServiceGroupDefinitioException;
import org.apache.stratos.manager.grouping.definitions.DependencyDefinitions;
import org.apache.stratos.manager.grouping.definitions.ServiceGroupDefinition;

import java.lang.reflect.Array;
import java.rmi.RemoteException;
import java.util.*;

public class DefaultServiceGroupDeployer implements ServiceGroupDeployer {

    private static Log log = LogFactory.getLog(DefaultServiceGroupDeployer.class);


    public DefaultServiceGroupDeployer() {
    }

    public void deployServiceGroupDefinition(Object serviceGroupDefinitionObj) throws InvalidServiceGroupException, ServiceGroupDefinitioException, ADCException {

        ServiceGroupDefinition serviceGroupDefinition = null;
        ServiceGroup serviceGroup = null;

        if (serviceGroupDefinitionObj == null) {
            if (log.isDebugEnabled()) {
                log.debug("deploying service group is null ");
            }
            throw new InvalidServiceGroupException("Service Group definition not found");
        }

        if (serviceGroupDefinitionObj instanceof ServiceGroupDefinition) {
            serviceGroupDefinition = (ServiceGroupDefinition) serviceGroupDefinitionObj;

            if (log.isDebugEnabled()) {
                log.debug("deploying service group with name " + serviceGroupDefinition.getName());
            }

            // convert serviceGroupDefinition to serviceGroup
            serviceGroup = this.populateServiceGroupPojo(serviceGroupDefinition);
        } else {
            log.error("trying to deploy invalid service group ");
            throw new InvalidServiceGroupException("Invalid Service Group definition");
        }

        // if any cartridges are specified in the group, they should be already deployed
        if (serviceGroupDefinition.getCartridges() != null) {

            if (log.isDebugEnabled()) {
                log.debug("checking cartridges in service group " + serviceGroupDefinition.getName());
            }

            List<String> cartridgeTypes = serviceGroupDefinition.getCartridges();

            Set<String> duplicates = this.findDuplicates(cartridgeTypes);

            if (duplicates.size() > 0) {

                StringBuffer buf = new StringBuffer();
                for (String dup : duplicates) {
                    buf.append(dup).append(" ");
                }
                if (log.isDebugEnabled()) {
                    log.debug("duplicate cartridges defined: " + buf.toString());
                }
                throw new InvalidServiceGroupException("Invalid Service Group definition, duplicate cartridges defined:" + buf.toString());
            }

            CloudControllerServiceClient ccServiceClient = null;

            try {
                ccServiceClient = CloudControllerServiceClient.getServiceClient();

            } catch (AxisFault axisFault) {
                throw new ADCException(axisFault);
            }

            for (String cartridgeType : cartridgeTypes) {
                try {
                    if (ccServiceClient.getCartridgeInfo(cartridgeType) == null) {
                        // cartridge is not deployed, can't continue
                        log.error("invalid cartridge found in service group " + cartridgeType);
                        throw new InvalidServiceGroupException("No Cartridge Definition found with type " + cartridgeType);
                    }
                } catch (RemoteException e) {
                    throw new ADCException(e);
                } catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
                    throw new ADCException(e);
                }
            }
        }

        // if any sub groups are specified in the group, they should be already deployed
        if (serviceGroupDefinition.getGroups() != null) {

            if (log.isDebugEnabled()) {
                log.debug("checking subGroups in service group " + serviceGroupDefinition.getName());
            }

            List<ServiceGroupDefinition> groupDefinitions = serviceGroupDefinition.getGroups();
            List<String>  groupNames = new ArrayList<String>();
            for (ServiceGroupDefinition groupList : groupDefinitions) {
                groupNames.add(groupList.getName());
            }

            Set<String> duplicates = this.findDuplicates(groupNames);

            if (duplicates.size() > 0) {

                StringBuffer buf = new StringBuffer();
                for (String dup : duplicates) {
                    buf.append(dup).append(" ");
                }
                if (log.isDebugEnabled()) {
                    log.debug("duplicate subGroups defined: " + buf.toString());
                }
                throw new InvalidServiceGroupException("Invalid Service Group definition, duplicate subGroups defined:" + buf.toString());
            }

//            for (String subGroupName : groupNames) {
//                if (getServiceGroupDefinition(subGroupName) == null) {
//                    // sub group not deployed, can't continue
//                    if (log.isDebugEnabled()) {
//                        log.debug("invalid sub group found in service group " + subGroupName);
//                    }
//                    throw new InvalidServiceGroupException("No Service Group Definition found with name " + subGroupName);
//                }
//            }
        }

        AutoscalerServiceClient asServiceClient = null;

        try {
            asServiceClient = AutoscalerServiceClient.getServiceClient();

            if (log.isDebugEnabled()) {
                log.debug("deplying to cloud controller service group " + serviceGroupDefinition.getName());
            }

            asServiceClient.deployServiceGroup(serviceGroup);

        } catch (AxisFault axisFault) {
            throw new ADCException(axisFault);
        } catch (RemoteException e) {
            throw new ADCException(e);
        } catch (AutoScalerServiceInvalidServiceGroupExceptionException e) {
            e.printStackTrace();
        }
    }

    public ServiceGroupDefinition getServiceGroupDefinition(String serviceGroupDefinitionName) throws ADCException, ServiceGroupDefinitioException {

        if (log.isDebugEnabled()) {
            log.debug("getting service group from cloud controller " + serviceGroupDefinitionName);
        }

        AutoscalerServiceClient asServiceClient = null;

        try {
            asServiceClient = AutoscalerServiceClient.getServiceClient();

            if (log.isDebugEnabled()) {
                log.debug(String.format("Calling AS to get service group %s", serviceGroupDefinitionName));
            }

            ServiceGroup serviceGroup = asServiceClient.getServiceGroup(serviceGroupDefinitionName);
            if (serviceGroup == null) {
                return null;
            }

            ServiceGroupDefinition serviceGroupDef = populateServiceGroupDefinitionPojo(serviceGroup);
            return serviceGroupDef;

        } catch (AxisFault axisFault) {
            throw new ADCException(axisFault);
        } catch (RemoteException e) {
            throw new ADCException(e);
        }
    }

    public ServiceGroupDefinition[] getServiceGroupDefinitions() throws ADCException {

        if (log.isDebugEnabled()) {
            log.debug("getting service group from cloud controller ");
        }

        AutoscalerServiceClient asServiceClient;

        try {
            asServiceClient = AutoscalerServiceClient.getServiceClient();

            ServiceGroup[] serviceGroups = asServiceClient.getServiceGroups();
            if (serviceGroups == null || serviceGroups.length == 0) {
                return null;
            }

            ServiceGroupDefinition[] serviceGroupDefinitions = new ServiceGroupDefinition[serviceGroups.length];
            for (int i = 0; i < serviceGroups.length; i++) {
                serviceGroupDefinitions[i] = populateServiceGroupDefinitionPojo(serviceGroups[i]);
            }
            return serviceGroupDefinitions;

        } catch (AxisFault axisFault) {
            throw new ADCException(axisFault);
        } catch (RemoteException e) {
            throw new ADCException(e);
        } catch (AutoScalerServiceAutoScalerExceptionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
    }


    public void undeployServiceGroupDefinition(String name) throws ADCException, ServiceGroupDefinitioException {

        //throw new ServiceGroupDefinitioException("method not supported");

        AutoscalerServiceClient autoscalerServiceClient = null;

        try {
            autoscalerServiceClient = AutoscalerServiceClient.getServiceClient();

            if (log.isDebugEnabled()) {
                log.debug("undeploying service group from cloud controller " + name);
            }

            autoscalerServiceClient.undeployServiceGroupDefinition(name);

        } catch (AxisFault axisFault) {
            throw new ADCException(axisFault);
        } catch (RemoteException e) {
            throw new ADCException(e);
        } catch (AutoScalerServiceAutoScalerExceptionException e) {
            throw new ADCException(e);
        }
    }


    private ServiceGroup populateServiceGroupPojo(ServiceGroupDefinition serviceGroupDefinition) throws ServiceGroupDefinitioException {
        ServiceGroup servicegroup = new ServiceGroup();

        // implement conversion (mostly List -> Array)
        servicegroup.setGroupscalingEnabled(serviceGroupDefinition.isGroupScalingEnabled());
        List<ServiceGroupDefinition> groupsDef = serviceGroupDefinition.getGroups();
        List<String> cartridgesDef = serviceGroupDefinition.getCartridges();

        servicegroup.setName(serviceGroupDefinition.getName());
        
        if (groupsDef == null) {
            groupsDef = new ArrayList<ServiceGroupDefinition>(0);
        }

        if (cartridgesDef == null) {
            cartridgesDef = new ArrayList<String>(0);
        }

        ServiceGroup[] subGroups = new ServiceGroup[groupsDef.size()];
        String[] cartridges = new String[cartridgesDef.size()];

        int i = 0;
        for (ServiceGroupDefinition groupDefinition : groupsDef) {
            subGroups[i] = populateServiceGroupPojo(groupDefinition);
            ++i;
        }

        servicegroup.setGroups(subGroups);
        cartridges = cartridgesDef.toArray(cartridges);
        servicegroup.setCartridges(cartridges);

        DependencyDefinitions depDefs = serviceGroupDefinition.getDependencies();

        if (depDefs != null) {
            Dependencies deps = new Dependencies();
            List<String> startupOrdersDef = depDefs.getStartupOrders();
            if (startupOrdersDef != null) {
                String[] startupOrders = new String[startupOrdersDef.size()];
                startupOrders = startupOrdersDef.toArray(startupOrders);
                deps.setStartupOrders(startupOrders);
            }
            // validate termination behavior
            validateTerminationBehavior(depDefs.getTerminationBehaviour());
            deps.setTerminationBehaviour(depDefs.getTerminationBehaviour());
			if (depDefs.getScalingDependants() != null) {
				deps.setScalingDependants(depDefs.getScalingDependants()
				        .toArray(new String[depDefs.getScalingDependants().size()]));
			}
            servicegroup.setDependencies(deps);
        }

        return servicegroup;
    }

    private ServiceGroupDefinition populateServiceGroupDefinitionPojo(ServiceGroup serviceGroup) {
        ServiceGroupDefinition servicegroupDef = new ServiceGroupDefinition();
        servicegroupDef.setName(serviceGroup.getName());
        String[] cartridges = serviceGroup.getCartridges();
        ServiceGroup[] groups = serviceGroup.getGroups();
        org.apache.stratos.autoscaler.stub.pojo.Dependencies deps = serviceGroup.getDependencies();

        List<ServiceGroupDefinition> groupDefinitions = new ArrayList<ServiceGroupDefinition>(groups.length);
        for (ServiceGroup group : groups) {
            if (group != null) {
                groupDefinitions.add(populateServiceGroupDefinitionPojo(group));
            }
        }

        if (deps != null) {
            DependencyDefinitions depsDef = new DependencyDefinitions();
            String[] startupOrders = deps.getStartupOrders();
            if (startupOrders != null && startupOrders[0] != null) {
                List<String> startupOrdersDef = Arrays.asList(startupOrders);
                depsDef.setStartupOrders(startupOrdersDef);
            }

            String [] scalingDependants = deps.getScalingDependants();
            if (scalingDependants != null && scalingDependants[0] != null) {
                List<String> scalingDependenciesDef = Arrays.asList(scalingDependants);
                depsDef.setScalingDependants(scalingDependenciesDef);
            }

            depsDef.setTerminationBehaviour(deps.getTerminationBehaviour());
            servicegroupDef.setDependencies(depsDef);
        }

        List<String> cartridgesDef = new ArrayList<String>(Arrays.asList(cartridges));
        //List<ServiceGroupDefinition> subGroupsDef = new ArrayList<ServiceGroupDefinition>(groups.length);
        if (cartridges[0] != null) {
            servicegroupDef.setCartridges(cartridgesDef);
        }
        if (groups != null) {
            servicegroupDef.setGroups(groupDefinitions);
        }

        return servicegroupDef;
    }

    /**
     * Validates terminationBehavior. The terminationBehavior should be one of the following:
     * 1. terminate-none
     * 2. terminate-dependents
     * 3. terminate-all
     *
     * @throws ServiceGroupDefinitioException if terminationBehavior is different to what is
     *                                        listed above
     */
    private static void validateTerminationBehavior(String terminationBehavior) throws ServiceGroupDefinitioException {

        if (!(terminationBehavior == null || "terminate-none".equals(terminationBehavior) ||
                "terminate-dependents".equals(terminationBehavior) || "terminate-all".equals(terminationBehavior))) {
            throw new ServiceGroupDefinitioException("Invalid Termination Behaviour specified: [ " +
                    terminationBehavior + " ], should be one of 'terminate-none', 'terminate-dependents', " +
                    " 'terminate-all' ");
        }
    }

    /**
     * returns any duplicates in a List
     *
     * @param checkedList
     * @return
     */
    private Set<String> findDuplicates(List<String> checkedList) {
        final Set<String> retVals = new HashSet<String>();
        final Set<String> set1 = new HashSet<String>();

        for (String val : checkedList) {

            if (!set1.add(val)) {
                retVals.add(val);
            }
        }
        return retVals;
    }

}
