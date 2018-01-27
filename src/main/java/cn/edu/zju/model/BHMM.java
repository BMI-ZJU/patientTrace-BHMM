package cn.edu.zju.model;

import cn.edu.zju.util.Scaler;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.edu.zju.util.Utils.*;

public class BHMM implements Serializable{

    private int k; // 主题的数量
    private int A; // 干预的种类数
    private int iterations; // 循环的次数
    private float alpha;
    private float beta;
    private float gamma;

    private int zt[][]; // N * T, N为所有记录的数量，T为每条记录的天数（可变）
    private int n_zz[][]; // 主题之间转变矩阵
    private int zz[]; // sum of row of n_zz
    private int n_za[][]; // 每个主题的事件
    private int za[]; // sum of row of n_za
    private int n_zav[][][]; // 每个主题的事件强度

    private double theta[][];
    private double phi[][];
    private double omega[][][];

    Map<String, Integer> event2index;
    Map<String, Scaler> eventScaler;

    public BHMM(int k, int iterations, float alpha, float beta, float gamma) {
        this.k = k;
        this.iterations = iterations;
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
    }

    public BHMM() {
        this.k = 10;
        this.iterations = 10;
        this.alpha = 1.0f / k;
        this.beta = 1f;
        this.gamma = 1f;
    }

    /**
     * TODO: 读取数据文件，对模型进行初始化
     * 不同种的干预行为，intensity是不同的，是否需要根据这些相同种类的干预行为进行归一化的操作 -> 所有的intensity都减小至5
     * @param rootPath 包含数据文件的根目录
     */
    public void initializeModel(String rootPath) throws IOException {

        File root = new File(rootPath);
        File[] files = root.listFiles();

        event2index = (Map<String, Integer>) readObject("resources/save/event2index.model");
        eventScaler = (Map<String, Scaler>) readObject("resources/save/eventScaler.model");

        assert files != null;
        assert event2index != null;
        assert eventScaler != null;

        int events = event2index.size(); // 干预的种类数量，对数据文件分析得到
        A = events;
        this.zt = new int[files.length][];
        this.n_zz = new int[k][k];
        this.zz = new int[k];
        this.n_za = new int[k][events];
        this.za = new int[k];
        this.n_zav = new int[k][events][];

        this.theta = new double[k][k];
        this.phi = new double[k][events];
        this.omega = new double[k][events][];

        for (Map.Entry<String, Integer> ev : event2index.entrySet()) {
            int a = ev.getValue();
            String e = ev.getKey();
            for (int i=0; i<this.k; i++) {
                this.n_zav[i][a] = new int[eventScaler.get(e).getV()];
                this.omega[i][a] = new double[eventScaler.get(e).getV()];
            }
        }

        Scaler scaler;
        for (int k=0; k<files.length; k++) {
            File file = files[k];
            int lastTopic = -1; // 上一个主题
            int currentTopic; // 当前主题

            String[][] content = readPatientRecord(file.getPath());
            String[] interventions = content[0];

            this.zt[k] = new int[content.length-1];

            for (int i=1; i<content.length; i++) {
                String[] oneDay = content[i];
                currentTopic = (int) (Math.random() * this.k);
                if (lastTopic != -1) {
                    this.n_zz[lastTopic][currentTopic] += 1;
                    this.zz[lastTopic] += 1;
                }
                this.zt[k][i-1] = currentTopic;

                for (int j=1; j<oneDay.length; j++) {
                    String event = interventions[j];
                    scaler = eventScaler.get(event);
                    double v = Double.parseDouble(oneDay[j]);
                    int intensity = scaler.scale(v);
                    if (intensity < 0) continue;  // 在变换中做了减1, 所以intensity=0其实代表有该项干预，<0代表无该项干预
                    int eventIndex = event2index.get(event);
                    this.n_za[currentTopic][eventIndex] += 1;
                    this.za[currentTopic] += 1;
                    this.n_zav[currentTopic][eventIndex][intensity] += 1;
                }
                lastTopic = currentTopic;
            }
        }
    }

    public void inferenceModel(String rootPath) throws IOException {
        String[][][] dataSet = loadDataSet(rootPath);

        Map<Integer, Integer> eAndv = new HashMap<>();

        for (int i=0; i<this.iterations; i++) {

            for (int j=0; j<dataSet.length; j++) {
                String[][] content = dataSet[j];
                String[] interventions = content[0];
                int lastTopic = -1;
                int currentTopic;
                int nextTopic;
                for (int k=1; k<content.length; k++) {
                    eAndv.clear();
                    currentTopic = zt[j][k-1];
                    if (k != content.length - 1) nextTopic = zt[j][k];
                    else nextTopic = -1;
                    String[] oneDay = content[k];

                    for (int l=1; l<oneDay.length; l++) {
                        String event = interventions[l];
                        String v = oneDay[l];
                        int intensity = eventScaler.get(event).scale(Double.parseDouble(v));
                        if (intensity < 0) continue;
                        int eventIndex = event2index.get(event);
                        eAndv.put(eventIndex, intensity);
                    }
                    int newTopic = sampleTopic(lastTopic, currentTopic, nextTopic, eAndv);
                    zt[j][k-1] = newTopic;
                    lastTopic = newTopic;
                }
            }
        }
    }

    /**
     * 对主题进行重采样
     */
    private int sampleTopic(int last_z, int current_z, int next_z, Map<Integer, Integer> eAndv) {
        if (last_z != -1) {
            n_zz[last_z][current_z] -= 1;
            zz[last_z] -= 1;
        }
        if (next_z != -1) {
            n_zz[current_z][next_z] -= 1;
            zz[current_z] -= 1;
        }
        for (Map.Entry<Integer, Integer> entry : eAndv.entrySet()) {
            n_za[current_z][entry.getKey()] -= 1;
            za[current_z] -= 1;
            n_zav[current_z][entry.getKey()][entry.getValue()] -= 1;
        }

        double p[] = new double[k];
        for (int i=0; i<this.k; i++) {
            if (last_z == -1) {
                p[i] = 1.0 / this.k;
            } else {
                p[i] = (n_zz[last_z][i] + alpha) / (zz[last_z] + this.k * alpha);
            }
            for (Map.Entry<Integer, Integer> entry : eAndv.entrySet()) {
                int[] av = n_zav[i][entry.getKey()];
                int V = av.length;
                p[i] = p[i] * (n_za[i][entry.getKey()] + beta) / (za[i] + A * beta)
                            * (n_zav[i][entry.getKey()][entry.getValue()] + gamma) / (n_za[i][entry.getKey()] + V * gamma);
            }
        }

        // sample topic
        for (int i=1; i<this.k; i++) {
            p[i] += p[i-1];
        }

        double u = Math.random() * p[this.k - 1];
        int newTopic;
        for (newTopic=0; newTopic < this.k; newTopic++) {
            if (u < p[newTopic]) {
                break;
            }
        }

        if (last_z != -1) {
            n_zz[last_z][newTopic] += 1;
            zz[last_z] += 1;
        }
        if (next_z != -1) {
            n_zz[newTopic][next_z] += 1;
            zz[newTopic] += 1;
        }
        for (Map.Entry<Integer, Integer> entry : eAndv.entrySet()) {
            n_za[newTopic][entry.getKey()] += 1;
            za[newTopic] += 1;
            n_zav[newTopic][entry.getKey()][entry.getValue()] += 1;
        }

        return newTopic;
    }

    private void calEstimateParameters() {
        // cal theta
        for (int i=0; i<k; i++) {
            for (int j=0; j<k; j++) {
                theta[i][j] = (n_zz[i][j] + alpha) / (zz[i] + k * alpha);
            }
        }

        // cal phi
        for (int i=0; i<k; i++) {
            for (int j=0; j<A; j++) {
                phi[i][j] = (n_za[i][j] + beta) / (za[i] + A * beta);
            }
        }

        // cal omega
        for (int i=0; i<k; i++) {
            for (int j=0; j<A; j++) {
                int[] av = n_zav[i][j];
                int V = av.length;
                for (int l=0; l<V; l++) {
                    omega[i][j][l] = (n_zav[i][j][l] + gamma) / (n_za[i][j] + V * gamma);
                }
            }
        }

    }

    public List<Integer> inferOne(String path) throws IOException {
        List<Integer> result = new ArrayList<>();
        int lastTopic = -1;
        int currentTopic;
        String[][] content = readPatientRecord(path);
        String[] interventions = content[0];
        Map<Integer, Integer> eAndv = new HashMap<>();
        for (int i=1; i<content.length; i++) {
            String[] oneDay = content[i];
            for (int j=1; j<oneDay.length; j++) {
                String e = interventions[j];
                String v = oneDay[j];
                int intensity = eventScaler.get(e).scale(Double.parseDouble(v));
                if (intensity < 0) continue;
                eAndv.put(event2index.get(e), intensity);
            }

            double p[] = new double[this.k];
            for (int k=0; k<this.k; k++) {
                if (lastTopic == -1) {
                    p[k] = 1.0 / this.k;
                }else {
                    p[k] = theta[lastTopic][k];
                }
                for (Map.Entry<Integer, Integer> ev : eAndv.entrySet()) {
                    p[k] = p[k] * phi[k][ev.getKey()] * omega[k][ev.getKey()][ev.getValue()];
                }
            }

            currentTopic = maxIndex(p);
            result.add(currentTopic);
            lastTopic = currentTopic;
        }

        return result;
    }

    public void saveModel() {
        saveModel("resources/save/bhmm.model");
    }

    public void saveModel(String path) {
        writeObject(path, this);
    }

    public static BHMM loadModel(String path) {
        return (BHMM) readObject(path);
    }

    public int topicNum() {
        return this.k;
    }

    public float getAlpha() {
        return alpha;
    }

    public float getBeta() {
        return beta;
    }

    public float getGamma() {
        return gamma;
    }

    public double[][] getTheta() {
        return theta;
    }

    public int[][] getZT() {
        return zt;
    }

    public double[][] getPhi() {
        return phi;
    }

    public double[][][] getOmega() {
        return omega;
    }

    public static void main(String[] args) throws IOException {
        // 训练模型
        BHMM bhmm = new BHMM(10, 1000, 10f, 0.1f, 0.1f);
        System.out.println("Initialize model");
        bhmm.initializeModel("resources/patientCSV");
        bhmm.saveModel("resources/save/initializedModel.model");
        System.out.println("Inference model");
        bhmm.inferenceModel("resources/patientCSV");
        System.out.println("update parameters");
        bhmm.calEstimateParameters();
        System.out.println("Save model");
        bhmm.saveModel("resources/save/bhmm_15_topic.model");




        // 患者样本分析
//        BHMM bhmm = (BHMM) readObject("resources/save/bhmm1-19-22-25.model");
//        assert bhmm != null;
//        List<Integer> result = bhmm.inferOne("resources/patientCSV/109502_3.csv");
//        for (Integer i : result) {
//            System.out.print(i + " ");
//        }


    }
}
