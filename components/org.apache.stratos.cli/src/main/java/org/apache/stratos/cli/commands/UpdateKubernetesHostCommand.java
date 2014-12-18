/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.cli.commands;

import org.apache.commons.cli.*;
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.RestCommandLineService;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;
import org.apache.stratos.cli.utils.CliUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Update kubernetes host command.
 */
public class UpdateKubernetesHostCommand implements Command<StratosCommandContext> {

    private static final Logger logger = LoggerFactory.getLogger(DeployKubernetesGroupCommand.class);

    private Options options;

    public UpdateKubernetesHostCommand() {
        options = new Options();
        Option clusterIdOption = new Option(CliConstants.CLUSTER_ID_OPTION, CliConstants.CLUSTER_ID_LONG_OPTION, true,
                "Kubernetes cluster id");
        Option hostIdOption = new Option(CliConstants.HOST_ID_OPTION, CliConstants.HOST_ID_LONG_OPTION, true,
                "Kubernetes host id");
        Option resourcePathOption = new Option(CliConstants.RESOURCE_PATH, CliConstants.RESOURCE_PATH_LONG_OPTION, true,
                "Kubernetes host resource path");
        options.addOption(clusterIdOption);
        options.addOption(hostIdOption);
        options.addOption(resourcePathOption);
    }
    @Override
    public String getName() {
        return "update-kubernetes-host";
    }

    @Override
    public String getDescription() {
        return "Update kubernetes host";
    }

    @Override
    public String getArgumentSyntax() {
    	return null;
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public int execute(StratosCommandContext context, String[] args) throws CommandException {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing command: ", getName());
        }
        
        if ((args == null) || (args.length <= 0)) {
            context.getStratosApplication().printUsage(getName());
            return CliConstants.COMMAND_FAILED;
        }
        
        try {
            CommandLineParser parser = new GnuParser();
            CommandLine commandLine = parser.parse(options, args);
            
            if((commandLine.hasOption(CliConstants.RESOURCE_PATH)) && (commandLine.hasOption(CliConstants.HOST_ID_OPTION)) 
            		&& (commandLine.hasOption(CliConstants.CLUSTER_ID_OPTION))) {
            	               
                // get cluster id arg value
            	String clusterId = commandLine.getOptionValue(CliConstants.CLUSTER_ID_OPTION);
                if (clusterId == null) {
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }
                
                // get host id arg value
            	String hostId = commandLine.getOptionValue(CliConstants.HOST_ID_OPTION);
                if (hostId == null) {
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }
                
                // get resource path arg value
            	String resourcePath = commandLine.getOptionValue(CliConstants.RESOURCE_PATH);
                if (resourcePath == null) {
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }
                String resourceFileContent = CliUtils.readResource(resourcePath);
                
                RestCommandLineService.getInstance().updateKubernetesHost(resourceFileContent, clusterId, hostId);
                return CliConstants.COMMAND_SUCCESSFULL;
            } else {
                context.getStratosApplication().printUsage(getName());
                return CliConstants.COMMAND_FAILED;
            }
        } catch (ParseException e) {
            logger.error("Error parsing arguments", e);
            System.out.println(e.getMessage());
            return CliConstants.COMMAND_FAILED;
        } catch (IOException e) {
            System.out.println("Invalid resource path");
            return CliConstants.COMMAND_FAILED;
        } catch (Exception e) {
            String message = "Unknown error occurred: " + e.getMessage();
            System.out.println(message);
            logger.error(message, e);
            return CliConstants.COMMAND_FAILED;
        }
    }
}
