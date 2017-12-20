package cn.edu.zju.model;

import com.csvreader.CsvReader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class topicModel {

    private int k; // 主题的数量
    private int iterations; // 循环的次数
    private int events; // 干预的种类数量，对数据文件分析得到
    private int intensities; // 干预强度的分级数量，对数据文件分析得到
    private float alpha;
    private float beta;
    private float gamma;

    private int n_zz[][];
    private int n_za[][];
    private int n_zav[][][];

    private Map<String, Integer> event2index; // 记录不同的干预行为的索引

    public topicModel(int k, int iterations, float alpha, float beta, float gamma) {
        this.k = k;
        this.iterations = iterations;
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
    }

    /**
     * TODO: 读取数据文件，对模型进行初始化
     * TODO: 不同种的干预行为，intensity是不同的，是否需要根据这些相同种类的干预行为进行归一化的操作
     * @param rootPath 包含数据文件的根目录
     */
    public void initializeModel(String rootPath) throws IOException {
        File root = new File(rootPath);
        File[] files = root.listFiles();

        event2index = new HashMap<>();
        this.events = 0;
        assert files != null;
        for (File file : files) {
            CsvReader reader = new CsvReader(file.getPath());
            reader.readHeaders();
            while(reader.readRecord()) {
                String event = reader.getValues()[0];
                if (!event2index.containsKey(event)) {
                    event2index.put(event, this.events);
                    this.events++;
                }
            }
            reader.close();
        }

        this.n_zz = new int[k][k];
        this.n_za = new int[k][events];
        this.n_zav = new int[k][events][intensities];

    }

    /**
     * TODO: 对模型进行推断
     */
    public void inferenceModel() {

        for (int i=0; i<this.iterations; i++) {
            sampleTopic();
        }
    }

    /**
     * TODO: 对主题进行重采样
     */
    private void sampleTopic() {

    }

    /**
     * TODO: 计算alpha, beta, gamma三个参数的估计量
     */
    private void calEstimateParameters() {

    }

    /**
     * TODO: 保存模型
     */
    public void saveModel() {

    }

    public static void main(String[] args) {
        String rootPath = "resources/patientCSV";
        File root = new File(rootPath);

        File[] files = root.listFiles();

        for (File file : files) {
            System.out.println(file.getAbsolutePath());
            break;
        }
    }
}
