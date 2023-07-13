package com.lynxight.common.data_model;

import java.util.ArrayList;

public class PoolImageData {
    public String poolId;
    public String b64Image;
    public ArrayList<Float> transformMatrix;

    public PoolImageData(String poolId, String b64Image, ArrayList<Float> transformMatrix) {
        this.poolId = poolId;
        this.b64Image = b64Image;
        this.transformMatrix = transformMatrix;
    }

    public boolean withTransformMatrix() { return transformMatrix != null; }
}
