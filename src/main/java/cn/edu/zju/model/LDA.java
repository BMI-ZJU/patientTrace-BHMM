package cn.edu.zju.model;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static cn.edu.zju.util.Utils.loadDataSet;
import static cn.edu.zju.util.Utils.readObject;
import static cn.edu.zju.util.Utils.writeObject;

public class LDA {

    int k;
    int A;
    int M;
    int iterations;
    float alpha;
    float beta;

    int[][] zt; // N * T, N为所有记录的条数，T为记录的天数（可变）
    int[][] nkt; // 每个主题的事件
    int[]   nktSum;  // sum for each row in nkt
    int[][] nmk; // 每天的主题分布
    int[]   nmkSum;  // sum for each row in nmk

    double[][] phi;
    double[][] theta;

    Map<String, Integer> event2Index;

    public LDA() {
        this.k = 10;
        this.iterations = 1000;
        this.alpha = 1f;
        this.beta = 0.1f;
    }

    public LDA(int k, int iterations, float alpha, float beta) {
        this.k = k;
        this.iterations = iterations;
        this.alpha = alpha;
        this.beta = beta;
    }

    public void initializeModel(String rootPath) throws IOException {
        File root = new File(rootPath);
        File[] files = root.listFiles();
        assert files != null;

        String[][][] allRecords = loadDataSet(rootPath);

        event2Index = (Map<String, Integer>) readObject("resources/save/event2index.model");
        assert event2Index != null;

        int events = event2Index.size();
        A = events;

        this.zt = new int[files.length][];

    }

    public void inferenceModel() {

    }

    private int sampleTopic() {
        return 0;
    }

    public void updateParameters() {

    }

    public void saveModel() {
        saveModel("resources/save/LDA.model");
    }

    public void saveModel(String path) {
        writeObject(path, this);
    }

    public LDA loadModel(String path) {
        return (LDA) readObject(path);
    }

    public int getK() {
        return k;
    }

    public double[][] getPhi() {
        return phi;
    }

    public double[][] getTheta() {
        return theta;
    }


}
