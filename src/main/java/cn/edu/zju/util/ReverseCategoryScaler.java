package cn.edu.zju.util;

import java.util.Map;

public class ReverseCategoryScaler implements ReverseScaler{

    private Map<Integer, Double> valueDict;

    public ReverseCategoryScaler(Map<Integer, Double> valueDict) {
        this.valueDict = valueDict;
    }

    @Override
    public double scale(int v) {

        return valueDict.get(v);
    }
}
