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
package org.apache.stratos.autoscaler.applications.dependency;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.*;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationChildContext;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationChildContextFactory;
import org.apache.stratos.autoscaler.exception.application.DependencyBuilderException;
import org.apache.stratos.messaging.domain.applications.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This is to build the startup/termination dependencies
 * across immediate children according to the start order defined.
 */
public class DependencyBuilder {
    private static final Log log = LogFactory.getLog(DependencyBuilder.class);

    private DependencyBuilder() {

    }

    private static class Holder {
        private static final DependencyBuilder INSTANCE = new DependencyBuilder();
    }

    public static DependencyBuilder getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * This will build the dependency tree based on the given dependencies
     * @param component it will give the necessary information to build the tree
     * @return the dependency tree out of the dependency orders
     */
    public DependencyTree buildDependency(ParentComponent component) throws DependencyBuilderException{

        String identifier = component.getUniqueIdentifier();
        DependencyTree dependencyTree = new DependencyTree(identifier);
        DependencyOrder dependencyOrder = component.getDependencyOrder();

        if (dependencyOrder != null) {
            log.info("Building dependency for the Application/Group " + identifier);

            //Parsing the kill behaviour
            String terminationBehaviour = dependencyOrder.getTerminationBehaviour();

            if (Constants.TERMINATE_NONE.equals(terminationBehaviour)) {
                dependencyTree.setTerminationBehavior(DependencyTree.TerminationBehavior.TERMINATE_NONE);
            } else if (Constants.TERMINATE_ALL.equals(terminationBehaviour)) {
                dependencyTree.setTerminationBehavior(DependencyTree.TerminationBehavior.TERMINATE_ALL);
            } else if (Constants.TERMINATE_DEPENDENTS.equals(terminationBehaviour)) {
                dependencyTree.setTerminationBehavior(DependencyTree.TerminationBehavior.TERMINATE_DEPENDENT);
            }

            log.info("Setting the [terminationBehaviour] " + terminationBehaviour + " to the " +
                    "[dependency-tree] " + dependencyTree.getId());


            //Parsing the start up order
            Set<StartupOrder> startupOrders = dependencyOrder.getStartupOrders();
            ApplicationChildContext foundContext;
            ApplicationChildContext parentContext;

            if (startupOrders != null) {
                for (StartupOrder startupOrder : startupOrders) {
                    parentContext = null;
                    for (String startupOrderComponent : startupOrder.getStartupOrderComponentList()) {

                        if (startupOrderComponent != null) {
                            ApplicationChildContext applicationContext = ApplicationChildContextFactory.
                                    getApplicationContext(startupOrderComponent, component, dependencyTree);
                            String id = applicationContext.getId();

                            ApplicationChildContext existingApplicationContext =
                                    dependencyTree.findApplicationContextWithIdInPrimaryTree(id);
                            if (existingApplicationContext == null) {
                                if (parentContext != null) {
                                    //appending the start up order to already added parent group/cluster
                                    parentContext.addApplicationContext(applicationContext);
                                    parentContext = applicationContext;
                                    if (log.isDebugEnabled()) {
                                        log.debug("Found an existing [dependency] " + parentContext.getId() +
                                                " and adding the [dependency] " + id + " as the child");
                                    }
                                } else {
                                    //adding list of startup order to the dependency tree
                                    dependencyTree.addPrimaryApplicationContext(applicationContext);
                                    parentContext = applicationContext;
                                }
                            } else {
                                if (parentContext == null) {
                                    //if existing context found, add it to child of existing context and
                                    //set the existing context as the next parent
                                    existingApplicationContext.addApplicationContext(applicationContext);
                                    parentContext = existingApplicationContext;
                                    if (log.isDebugEnabled()) {
                                        log.debug("Found an existing [dependency] " + id + " and setting it " +
                                                "for the next dependency to follow");
                                    }
                                } else {
                                    String msg = "Startup order is not consistent. It contains the group/cluster " +
                                            "which has been used more than one in another startup order";
                                    throw new DependencyBuilderException(msg);
                                }

                            }
                        }
                    }

                }
            }

            
        }

        //adding the rest of the children who are independent to the top level
        // as they can start in parallel.
        Collection<Group> groups = component.getAliasToGroupMap().values();
        for (Group group1 : groups) {
            if (dependencyTree.findApplicationContextWithIdInPrimaryTree(group1.getAlias()) == null) {
                ApplicationChildContext context = ApplicationChildContextFactory.getGroupChildContext(group1.getAlias(), dependencyTree.isTerminateDependent());
                dependencyTree.addPrimaryApplicationContext(context);
            }
        }
        Collection<ClusterDataHolder> clusterData = component.getClusterDataMap().values();
        for (ClusterDataHolder dataHolder : clusterData) {
            if (dependencyTree.findApplicationContextWithIdInPrimaryTree(dataHolder.getClusterId()) == null) {
                ApplicationChildContext context = ApplicationChildContextFactory.getClusterChildContext(dataHolder,
                        dependencyTree.isTerminateDependent());
                dependencyTree.addPrimaryApplicationContext(context);

            }
        }
        return dependencyTree;
    }
    
    /**
     * 
     * Utility method to build scaling dependencies
     * 
     */
	public Set<String> buildScalingDependencies(ParentComponent component) {
		log.info(" ******* in build scaling dependencies ************ "); // TODO - remove
		Set<String> scalingDependencies = new HashSet<String>();
		if(component.getDependencyOrder() != null && component.getDependencyOrder().getScalingDependents() != null) {
			log.info(" ******* in build scaling dependencies 22 ************ "); // TODO - remove
		for (String string : component.getDependencyOrder().getScalingDependents()) {
	    
			log.info(" ******* in build scaling dependencies 33 ************ "); // TODO - remove
		        if (string.startsWith(Constants.GROUP + ".")) {
		            //getting the group alias            
		            scalingDependencies.add(getGroupFromStartupOrder(string));
		        } else if (string.startsWith(Constants.CARTRIDGE + ".")) {
		            //getting the cluster alias
		            String id = getClusterFromStartupOrder(string);
		            //getting the cluster-id from cluster alias
		            ClusterDataHolder clusterDataHolder = (ClusterDataHolder) component.getClusterDataMap().get(id);
		            scalingDependencies.add(clusterDataHolder.getClusterId());
		        } else {
		            log.warn("[Scaling Dependency]: " + string + " contains unknown reference");
		        }
        }
		}
	    return scalingDependencies;
    }

    /**
     * Utility method to get the group alias from the startup order Eg: group.mygroup
     *
     * @param startupOrder startup order
     * @return group alias
     */
    public static String getGroupFromStartupOrder(String startupOrder) {
        return startupOrder.substring(Constants.GROUP.length() + 1);
    }

    /**
     * Utility method to get the cluster alias from startup order Eg: cartridge.myphp
     *
     * @param startupOrder startup order
     * @return cluster alias
     */
    public static String getClusterFromStartupOrder(String startupOrder) {
        return startupOrder.substring(Constants.CARTRIDGE.length() + 1);
    }



}
