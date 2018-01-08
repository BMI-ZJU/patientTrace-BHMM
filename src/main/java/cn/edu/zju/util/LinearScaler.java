package cn.edu.zju.util;

import java.io.Serializable;

public class LinearScaler implements Scaler, Serializable{
    private double max;

    public LinearScaler(double max) {
        this.max = max;
    }

    @Override
    public int scale(double value) {
        return (int) Math.ceil(value / max * 5) - 1;
    }
}
