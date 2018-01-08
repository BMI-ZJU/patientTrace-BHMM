package cn.edu.zju.model;

import cn.edu.zju.util.Scaler;
import com.csvreader.CsvReader;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;

import static cn.edu.zju.util.Utils.readObject;
import static cn.edu.zju.util.Utils.readPatientRecord;

public class BHMM {

    private int k; // 主题的数量
    private int iterations; // 循环的次数
    private float alpha;
    private float beta;
    private float gamma;

    private int z[];  // 每个主题的数量
    private int zt[][]; // N * T, N为所有记录的数量，T为每条记录的天数（可变）
    private int n_zz[][]; // 主题之间转变矩阵
    private int n_za[][]; // 每个主题的事件
    private int n_zav[][][]; // 每个主题的事件强度

    Map<String, Integer> event2index;
    Map<String, Scaler> eventScaler;

    public BHMM(int k, int iterations, float alpha, float beta, float gamma) {
        this.k = k;
        this.iterations = iterations;
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
    }

    /**
     * TODO: 读取数据文件，对模型进行初始化
     * 不同种的干预行为，intensity是不同的，是否需要根据这些相同种类的干预行为进行归一化的操作 -> 所有的intensity都减小至5
     * @param rootPath 包含数据文件的根目录
     */
    public void initializeModel(String rootPath) throws IOException {
        File root = new File(rootPath);
        File[] files = root.listFiles();

        event2index = (Map<String, Integer>) readObject("resources/save/events2index.model");
        eventScaler = (Map<String, Scaler>) readObject("resources/save/eventScaler.model");

        assert files != null;
        assert event2index != null;
        assert eventScaler != null;

        int events = event2index.size(); // 干预的种类数量，对数据文件分析得到
        int intensities = 6; // 干预强度的分级数量，对数据文件分析得到
        this.z = new int[k];
        this.zt = new int[files.length][];
        this.n_zz = new int[k][k];
        this.n_za = new int[k][events];
        this.n_zav = new int[k][events][intensities];

        Scaler scaler;
        for (int k=0; k<files.length; k++) {
            File file = files[k];
            int lastTopic = (int) (Math.random() * k); // 上一个主题
            int currentTopic; // 当前主题

            String[][] content = readPatientRecord(file.getPath());
            String[] interventions = content[0];

            this.zt[k] = new int[content.length-1];

            for (int i=1; i<content.length; i++) {
                String[] oneDay = content[i];
                currentTopic = (int) (Math.random() * k);
                this.n_zz[lastTopic][currentTopic] += 1;
                this.zt[k][i] = currentTopic;

                for (int j=1; j<oneDay.length; j++) {
                    String event = interventions[j];
                    scaler = eventScaler.get(event);
                    double v = Double.parseDouble(oneDay[j]);
                    int intensity = scaler.scale(v);
                    if (intensity < 0) continue;  // 在变换中做了减1, 所以intensity=0其实代表有该项干预，<0代表无该项干预
                    int eventIndex = event2index.get(event);
                    this.z[currentTopic] += 1;
                    this.n_za[currentTopic][eventIndex] += 1;
                    this.n_zav[currentTopic][eventIndex][intensity] += 1;
                }
                lastTopic = currentTopic;
            }
        }
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
    }
}
