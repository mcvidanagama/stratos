package org.apache.stratos.messaging.event.health.stat;

/**
 * Created by asiri on 8/10/14.
 */
import org.apache.stratos.messaging.event.Event;
public class AverageRequestsServingCapabilityEvent extends  Event{
    private final String networkPartitionId;
    private final String clusterId;
    private final float value;
    private final String instanceId;

    public AverageRequestsServingCapabilityEvent(String networkPartitionId, String clusterId, float value, String instanceId) {
        this.networkPartitionId = networkPartitionId;
        this.clusterId = clusterId;
        this.value = value;

        this.instanceId = instanceId;
    }
    public String getClusterId() {
        return clusterId;
    }

    public float getValue() {
        return value;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }


    public String getInstanceId() {
        return instanceId;
    }
}
