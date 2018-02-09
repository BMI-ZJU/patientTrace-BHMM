package cn.edu.zju.util;

public class ReverseLinearScaler implements ReverseScaler{
    private double max;

    public ReverseLinearScaler(double max) {
        this.max = max;
    }

    @Override
    public double scale(int v) {
        return max / 5 * (v + 1);
    }
}
