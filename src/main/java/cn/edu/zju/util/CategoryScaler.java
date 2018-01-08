package cn.edu.zju.util;

import java.io.Serializable;
import java.util.Map;

public class CategoryScaler implements Scaler, Serializable{
    private Map<Double, Integer> valueDict;

    public CategoryScaler(Map<Double, Integer> valueDict) {
        this.valueDict = valueDict;
    }

    @Override
    public int scale(double value) {
        return valueDict.get(value) - 1;
    }
}
