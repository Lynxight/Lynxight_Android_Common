package com.lynxight.common.data_model;

import com.google.gson.annotations.SerializedName;
import com.lynxight.common.managers.SiteData;

import java.io.Serializable;
import java.util.ArrayList;

public class PoolFullData implements Serializable {
    @SerializedName("pool_id")
    public String poolId;

    @SerializedName("pool_display_name")
    public String poolDisplayName;

    @SerializedName("topic_alias")
    public String topicAlias;

    @SerializedName("pool_image")
    public String poolImage;

    @SerializedName("transform_matrix")
    public ArrayList<Float> transformMatrix;

    public PoolFullData(String poolId, String poolDisplayName,
                        String topicAlias, String poolImage, ArrayList<Float> transformMatrix)
    {
        this.poolId = poolId;
        this.poolDisplayName = poolDisplayName;
        this.topicAlias = topicAlias;
        this.poolImage = poolImage;
        this.transformMatrix = transformMatrix;
    }

    public PoolData toPoolData() {
        return new PoolData(poolId, poolDisplayName, topicAlias, SiteData.getInstance().getSwimmerCount(poolId));
    }

    public PoolImageData toPoolImageData() {
        return new PoolImageData(poolId, poolImage, transformMatrix);
    }
}