package com.lynxight.common.data_model;

import static com.lynxight.common.Communication.MQTTClient.MqttConnectionStatus.NO_DATA;

import com.lynxight.common.Communication.MQTTClient;

public class PoolData implements Comparable<PoolData> {

    public String poolId;
    public String poolDisplayName;
    public String topicAlias;
    public String swimmerCount;
    public MQTTClient.MqttConnectionStatus poolStatus;

    public PoolData(String poolId, String poolDisplayName, String topicAlias, String swimmerCount)
    {
        this.poolId = poolId;
        this.poolDisplayName = poolDisplayName;
        this.topicAlias = topicAlias;
        this.swimmerCount = swimmerCount;
        this.poolStatus = NO_DATA;
    }

    public PoolData(PoolData ref) {
        poolId = ref.poolId;
        poolDisplayName = ref.poolDisplayName;
        topicAlias = ref.topicAlias;
        swimmerCount = ref.swimmerCount;
        poolStatus = ref.poolStatus;
    }

    public void copy(PoolData ref){
        poolId = ref.poolId;
        poolDisplayName = ref.poolDisplayName;
        topicAlias = ref.topicAlias;
        swimmerCount = ref.swimmerCount;
        poolStatus = ref.poolStatus;
    }
    @Override
    public int compareTo(PoolData o) {
        return this.poolId.compareTo(o.poolId);
    }
}
