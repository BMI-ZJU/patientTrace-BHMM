package cn.edu.zju.model;

import cn.edu.zju.util.Scaler;

import java.io.IOException;
import java.util.Map;

import static cn.edu.zju.util.Utils.loadDataSet;
import static cn.edu.zju.util.Utils.readObject;
import static cn.edu.zju.util.Utils.writeObject;

public class LDA {

    int K;
    int A;
    int M;
    int iterations;
    float alpha;
    float beta;

    int zt[][][]; // N * T, N为所有记录的条数，T为记录的天数（可变）
    int nkt[][]; // 每个主题的事件
    int nktSum[];  // sum for each row in nkt
    int nmk[][][]; // 每天的主题分布
    int nmkSum[][];  // sum for each row in nmk

    double[][] phi;
    double[][][] theta;

    Map<String, Integer> event2Index;
    Map<String, Scaler> eventScaler;

    public LDA() {
        this.K = 10;
        this.iterations = 1000;
        this.alpha = 1f;
        this.beta = 0.1f;
    }

    public LDA(int k, int iterations, float alpha, float beta) {
        this.K = k;
        this.iterations = iterations;
        this.alpha = alpha;
        this.beta = beta;
    }

    public void initializeModel(String rootPath) throws IOException {
        String[][][] allRecords = loadDataSet(rootPath);

        event2Index = (Map<String, Integer>) readObject("resources/save/event2index.model");
        eventScaler = (Map<String, Scaler>) readObject("resources/save/eventScaler.model");
        assert event2Index != null;
        assert eventScaler != null;

        A = event2Index.size();

        M = allRecords.length;

        this.zt = new int[M][][];
        this.nkt = new int[K][A];
        this.nktSum = new int[K];
        this.nmk = new int[M][][];
        this.nmkSum = new int[M][];

        this.phi = new double[K][A];
        this.theta = new double[M][][];

        for (int i=0; i<M; i++) {
            String[][] content = allRecords[i];
            int days = content.length - 1;
            int columns = content[0].length - 1;
            String[] interventions = content[0];
            this.zt[i] = new int[days][interventions.length - 1];
            this.theta[i] = new double[days][K];

            for (int j=0; j<days; j++){
                String[] oneDay = content[j+1];

                int eventsSum = 0; // 在某天中的干预的数量，对应于一篇文档的词数
                for (int k=0; k<columns; k++) {
                    String event = interventions[k+1];
                    int v = eventScaler.get(event).scale(Double.parseDouble(oneDay[k+1])) + 1;
                    if (v < 1) continue;
                    int initTopic = (int) (Math.random() * K);
                    int eventIndex = event2Index.get(event);
                    zt[i][j][k] = initTopic;
                    nkt[initTopic][eventIndex] += v;
                    nktSum[initTopic] += v;
                    nmk[i][j][initTopic] += v;
                    eventsSum += v;
                }
                nmkSum[i][j] = eventsSum;
            }
        }
    }

    public void inferenceModel() throws IOException {
        String[][][] allRecords = loadDataSet("resources/patientCSV");

        for (int iter=0; iter < iterations; iter++) {
            for (int i=0; i<M; i++) {
                String[][] content = allRecords[i];
                String[] interventions = content[0];
                for (int j=1; j<content.length; j++) {
                    String[] oneDay = content[j];
                    for (int k=1; k<oneDay.length; k++) {
                        String event = interventions[k];
                        int v = eventScaler.get(event).scale(Double.parseDouble(oneDay[k]));
                        if (v < 0) continue;
                        int eventIndex = event2Index.get(event);
                        int newTopic = sampleTopic(i, j-1, eventIndex, k-1, v);
                        zt[i][j][k] = newTopic;
                    }
                }
            }
        }
    }

    private int sampleTopic(int i, int j, int eventIndex, int k, int v) {
        int oldTopic = zt[i][j][k];
        nmk[i][j][oldTopic] -= v;
        nmkSum[i][j] -= v;
        nkt[oldTopic][eventIndex] -= v;
        nktSum[oldTopic] -= v;

        double[] p = new double[K];
        for (int l=0; l<K; l++) {
            p[l] = (nkt[l][eventIndex] + beta) / (nktSum[l] + A * beta)
                    * (nmk[i][j][l] + alpha) / (nmkSum[i][j] + K * alpha);
        }

        for (int l=1; l<K; l++) {
            p[l] += p[l-1];
        }

        double u = Math.random() * p[K-1];
        int newTopic;
        for (newTopic=0; newTopic<K; newTopic++) {
            if (u < p[newTopic]) {
                break;
            }
        }

        nmk[i][j][newTopic] += v;
        nmkSum[i][j] += v;
        nkt[newTopic][eventIndex] += v;
        nktSum[newTopic] += v;

        return newTopic;
    }

    public void updateParameters() {
        for (int k=0; k<K; k++) {
            for (int a=0; a<A; a++) {
                phi[k][a] = (nkt[k][a] + beta) / (nktSum[k] + A * beta);
            }
        }

        for (int m=0; m<M; m++) {
            for (int n=0; n<zt[m].length; n++) {
                for (int k=0; k<K; k++) {
                    theta[m][n][k] = (nmk[m][n][k] + alpha) / (nmkSum[m][n] + K * alpha);
                }
            }
        }
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
        return K;
    }

    public int[][][] getZT() {
        return zt;
    }

    public double[][] getPhi() {
        return phi;
    }

    public double[][][] getTheta() {
        return theta;
    }


}
