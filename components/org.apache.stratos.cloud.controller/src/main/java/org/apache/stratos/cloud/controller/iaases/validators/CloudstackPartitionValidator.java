package org.apache.stratos.cloud.controller.iaases.validators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.InvalidPartitionException;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.services.impl.CloudControllerServiceUtil;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.messaging.domain.topology.Scope;

import java.util.Properties;


public class CloudstackPartitionValidator extends IaasBasedPartitionValidator {


    private static final Log log = LogFactory.getLog(AWSEC2PartitionValidator.class);
    private IaasProvider iaasProvider;
    private Iaas iaas;


    @Override
    public void setIaasProvider(IaasProvider iaas) {
        this.iaasProvider = iaas;
        this.iaas = iaas.getIaas();
    }

    @Override
    public IaasProvider validate(String partitionId, Properties properties) throws InvalidPartitionException {

        try {
            IaasProvider updatedIaasProvider = new IaasProvider(iaasProvider);
            Iaas updatedIaas = CloudControllerServiceUtil.buildIaas(updatedIaasProvider);
            updatedIaas.setIaasProvider(updatedIaasProvider);

            if (properties.containsKey(Scope.zone.toString())) {
                String zone = properties.getProperty(Scope.zone.toString());
                iaas.isValidZone(null, zone);
                updatedIaasProvider.setProperty(CloudControllerConstants.AVAILABILITY_ZONE, zone);
                updatedIaas = CloudControllerServiceUtil.buildIaas(updatedIaasProvider);
                updatedIaas.setIaasProvider(updatedIaasProvider);
            }

        } catch (Exception ex) {
            String msg = "Invalid Partition Detected : "+partitionId+". Cause: "+ex.getMessage();
            log.error(msg, ex);
            throw new InvalidPartitionException(msg, ex);
        }
        return iaasProvider;
    }
}