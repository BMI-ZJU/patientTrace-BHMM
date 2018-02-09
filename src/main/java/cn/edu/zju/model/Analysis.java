package cn.edu.zju.model;

import cn.edu.zju.util.ReverseScaler;
import cn.edu.zju.util.Scaler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cn.edu.zju.util.Utils.*;

public class Analysis {

    /**
     * 统计数据记录的条数，平均在院时长，所有的干预事件次数
     * @throws IOException
     */
    private static void statistic() throws IOException {
        String rootPath = "resources/patientCSV";
        File rootFile = new File(rootPath);
        File[] files = rootFile.listFiles();
        assert files != null;

        int interventionNum = 0;
        double los = 0; // 住院时长
        int records = files.length; // 记录的条数

        BufferedWriter out = new BufferedWriter(new FileWriter(new File("resources/save/patientList.txt")));

        for (File file : files) {
            boolean everyDay = true; // 是否每天都有Intervention
            String[][] content = readPatientRecord(file.getPath());

            los += content.length - 1;

            for (int i=1; i<content.length; i++) {
                String[] oneDay = content[i];
                int oneDayIntervention = 0;  // 一天内的干预数
                for (int j=1; j<oneDay.length; j++) {
                    String v = oneDay[j];
                    double intensity = Double.parseDouble(v);
                    if(intensity == 0) continue;
                    interventionNum++;
                    oneDayIntervention++;
                }

                if (oneDayIntervention == 0) {
                    everyDay = false;
                }
            }

            if (everyDay) {
                out.write(file.getPath() + "\n");
            }

        }

        out.flush();
        out.close();

        los = los / records;
        System.out.println("记录条数" + records);
        System.out.println("平均在院时长" + los);
        System.out.println("所有的干预事件数目" + interventionNum);
    }

    private static void findPCIandCABG() throws IOException {
        File root = new File("resources/patientCSV");
        File[] files = root.listFiles();
        assert files != null;

        BufferedWriter out = new BufferedWriter(new FileWriter(new File("resources/save/pciPatientList.txt")));

        for (File file : files) {
            boolean pciAndCABG = true;
            String[][] record = readPatientRecord(file.getPath());

            String[] interventions = record[0];

//            for (String intervention : interventions) {
//                if (intervention.equals("PCI术")
//                        || intervention.contains("冠脉球囊")
//                        || intervention.contains("冠状动脉球囊")
//                        || intervention.contains("球囊扩张支架植入术")) {
//                    pciAndCABG = true;
//                    break;
//                }
//                pciAndCABG = false;
//            }
//
//            if (!pciAndCABG) continue;

            for (String intervention : interventions) {
                if (intervention.equals("冠脉旁路移植手术")) {
                    pciAndCABG = true;
                    break;
                }
                pciAndCABG = false;
            }

            if (pciAndCABG) {
                out.write(file.getPath() + "\n");
            }
        }

        out.flush();
        out.close();

    }

    private static void analysisParam(int k) throws IOException {

        BHMM bhmm = (BHMM) readObject("resources/save/bhmm_"+ k +"_topic.model");
        assert bhmm != null;
        double[][] phi = bhmm.getPhi();
        double[][][] omega = bhmm.getOmega();

        Map<Integer, String> index2Event = (Map) readObject("resources/save/index2Event.model");
        assert index2Event != null;

        File out_file = new File("resources/save/"+ k +"_topics_k_to_event.txt");
        BufferedWriter out = new BufferedWriter(new FileWriter(out_file));
        for (int i=0; i<k; i++) {
            Integer[] k_a_sort = argsort(phi[i]);
            out.write("主题: " + i + ",");
            for (int j=0; j<30; j++) {
                out.write(index2Event.get(k_a_sort[j]) + ": " + phi[i][k_a_sort[j]] +",");
            }
            out.write("\n");
        }
        out.flush();
        out.close();

        out = new BufferedWriter(new FileWriter(new File("resources/save/" + k + "_topic_k_v_to_event.txt")));

        double[][][] actual_probability = new double[k][omega[0].length][];

        for (int i=0; i<k; i++) {
            for (int j=0; j<omega[0].length; j++) {
                int V = omega[i][j].length;
                actual_probability[i][j] = new double[V];
                for (int v=0; v<V; v++) {
                    actual_probability[i][j][v] = phi[i][j] * omega[i][j][v];
                }
            }
        }

        Map<String, ReverseScaler> eventReverseScaler = (Map<String, ReverseScaler>) readObject("resources/save/eventReverseScaler.model");
        Map<String, List<Double>> eventIntensity = (Map<String, List<Double>>) readObject("resources/save/eventIntensity.model");
        assert eventReverseScaler != null;
        assert eventIntensity != null;

        for (int i=0; i<k; i++) {
            double[][] prob = actual_probability[i];

            String[] evArgsort = argsort(prob);

            out.write("主题" + (i+1) + ",");

            for (int j=0; j<30; j++) {
                String[] ev = evArgsort[j].split(",");
                int eIndex = Integer.valueOf(ev[0]);
                String event = index2Event.get(eIndex);
                int v = Integer.valueOf(ev[1]);
                List<Double> items = eventIntensity.get(event);
                if (items.size() <= 6) {
                    out.write(event + "(" + eventReverseScaler.get(event).scale(v) + "): "+ prob[eIndex][v-1] + ",");
                } else {
                    out.write(event + "(" + eventReverseScaler.get(event).scale(v-1) + "-" + eventReverseScaler.get(event).scale(v) + "): " + prob[eIndex][v-1] + ",");
                }

            }

            out.write("\n");
        }

        out.flush();
        out.close();

    }

    public static double perplexity(int topicNum) throws IOException {
        BHMM bhmm = (BHMM) readObject("resources/save/bhmm_" + topicNum + "_topic.model");
        assert bhmm != null;

        double[][] phi = bhmm.getPhi();
        double[][][] omega = bhmm.getOmega();
        int[][] zt = bhmm.getZT();

        double exponential = 0;
        int num_event = 0;

        File root = new File("resources/patientCSV");
        File[] files = root.listFiles();
        assert files != null;

        for (int i=0; i<zt.length; i++) {
            String[][] content = readPatientRecord(files[i].getPath());
            String[] intervention = content[0];
            int[] trace = zt[i];
            for (int j=0; j<trace.length; j++) {
                String[] oneDay = content[j+1];
                int topic = trace[j];
                for (int k=1; k<oneDay.length; k++) {
                    String event = intervention[k];
                    Scaler scaler = bhmm.eventScaler.get(event);
                    String v = oneDay[k];
                    int intensity = scaler.scale(Double.parseDouble(v));
                    if (intensity < 0) continue;
                    num_event++;
                    exponential += Math.log(phi[topic][bhmm.event2index.get(event)] * omega[topic][bhmm.event2index.get(event)][intensity]);
                }
            }
        }

        return Math.exp(- exponential / num_event);
    }

    public static void oneHundredExamples() throws IOException {
//        BHMM bhmm = (BHMM) readObject("resources/save/bhmm_15_topic.model");
        SNB bhmm = (SNB) readObject("resources/save/snb_15_topic.model");
        assert bhmm != null;
        Map<String, Integer> file2index = (Map<String, Integer>) readObject("resources/save/file2index.model");
        assert file2index != null;

        int[][] zt = bhmm.getZT();

        File[] examples = new File("resources/examples").listFiles();
        assert examples != null;

        BufferedWriter trainOut = new BufferedWriter(new FileWriter(new File("resources/save/trainResult.txt")));
        BufferedWriter predOut = new BufferedWriter(new FileWriter(new File("resources/save/predResult.txt")));

        for (File example : examples) {

            trainOut.write(example.getName() + ": \t");
            for (int t : zt[file2index.get(example.getName())]) {
                trainOut.write(t + " ");
            }
            trainOut.write("\n");

            predOut.write(example.getName() + ": \t");
            for (int t : bhmm.inferOne(example.getPath())) {
                predOut.write(t + " ");
            }
            predOut.write("\n");
        }

        trainOut.flush();
        trainOut.close();
        predOut.flush();
        predOut.close();

    }

    public static void patientExamples() throws IOException {
        BHMM bhmm = (BHMM) readObject("resources/save/bhmm_15_topic.model");
        assert bhmm != null;

        Map<String, Integer> file2index = (Map<String, Integer>) readObject("resources/save/file2index.model");
        assert file2index != null;

        BufferedReader reader = new BufferedReader(new FileReader(new File("resources/save/pciPatientList.txt")));

        String line = reader.readLine();

        BufferedWriter predOut = new BufferedWriter(new FileWriter(new File("resources/save/pciPredResult.txt")));

        while (line != null) {
            predOut.write(line + ": \t");

            for (int t : bhmm.inferOne(line)) {
                predOut.write(t + " ");
            }
            predOut.write("\n");

            line = reader.readLine();
        }

        predOut.flush();
        predOut.close();
    }

    public static void oneHundredExamplesLDA() throws IOException {
        LDA lda = (LDA) readObject("resources/save/lda_15_topics.model");
        assert lda != null;

        Map<String, Integer> file2index = (Map<String, Integer>) readObject("resources/save/file2index.model");
        assert file2index != null;

        double[][][] theta = lda.getTheta();

        File[] examples = new File("resources/examples").listFiles();
        assert examples != null;

        BufferedWriter out = new BufferedWriter(new FileWriter(new File("resources/experiment/LDA_trainResult.csv")));

        for (File example : examples) {
            int index = file2index.get(example.getName());

            double[][] example_theta = theta[index];
            out.write(example.getName() + ": \t");

            for (double[] day_theta : example_theta) {
                out.write(maxIndex(day_theta) + " ");
            }

            out.write("\n");
        }

        out.flush();
        out.close();

    }

    public static void los() throws IOException {
        BHMM bhmm = (BHMM) readObject("resources/save/bhmm_15_topic.model");
        assert bhmm != null;

        double[] loz = new double[15];
        double[] loz_numerator = new double[15];
        double[] loz_denominator = new double[15];

        File root = new File("resources/patientCSV");
        File[] files = root.listFiles();
        assert files != null;

        for (File file : files) {
            List<Integer> zt = bhmm.inferOne(file.getPath());
            String[][] content = readPatientRecord(file.getPath());

            int los = content.length - 1;

            for (int z : zt) {
                for (int k=0; k<15; k++) {
                    if (z == k) {
                        loz_numerator[k] += los;
                        loz_denominator[k] += 1;
                        break;
                    }
                }
            }
        }

        for (int k=0; k<15; k++) {
            loz[k] = loz_numerator[k] / loz_denominator[k];
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(new File("resources/experiment/los.txt")));
        for (int k=0; k<15; k++) {
            out.write("los of topic " + k + " is " + loz[k] + "\n");
        }
        out.flush();
        out.close();
    }

    private static void traceRoute(double threshold) throws IOException {

        File root = new File("resources/patientCSV");
        File[] files = root.listFiles();
        assert files != null;

        BHMM bhmm = (BHMM) readObject("resources/save/bhmm_15_topic.model");
        assert bhmm != null;

        List<List<Integer>> zt = new ArrayList<>();

        for (File file : files) {
            zt.add(bhmm.inferOne(file.getPath()));
        }

        int total = files.length;

        int days = 40;
        double[][] k_rate = new double[15][days]; // 每天中各个主题所占的比例

        double[][][] trans = new double[days-1][15][15];  // 每天主题之间的转移

        for (int i=0; i<total; i++) {
            for (int j=0; j<days; j++) {
                if (j < zt.get(i).size()) {
                    k_rate[zt.get(i).get(j)][j]++;

                    if (j < zt.get(i).size() - 1 && j < days-1) {
                        trans[j][zt.get(i).get(j)][zt.get(i).get(j+1)]++;
                    }
                }
            }
        }

        // 对 k_rate 进行归一化
//        double[] columnSum = sumColumn(k_rate);
//
//        for (int i=0; i<15; i++) {
//            for (int j=0; j<days; j++) {
//                k_rate[i][j] /= columnSum[j];
//            }
//        }

        // 对 trans 进行归一化
//        for (int i=0; i<days-1; i++) {
//            double[][] trans_day_k = trans[i];
//            double[] k_trans = sumRow(trans_day_k);

//            for (int j=0; j<15; j++) {
//                for (int k=0; k<15; k++) {
//                    trans[i][j][k] /= k_rate[j][i];
//                    trans[i][j][k] /= k_trans[j];
//                }
//            }
//        }


        int[] topic_dist = new int[15];
        for (List<Integer> aZt : zt) {
            for (int j = 0; j < 15; j++) {
                if (aZt.contains(j)) {
                    topic_dist[j]++;
                }
            }
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(new File("resources/experiment/traceRoute.csv")));

        for (int i=0; i<15; i++) {
            for (int j=0; j<days-1; j++) {
                if (k_rate[i][j] < threshold) {
                    out.write(",");
                } else {
                    out.write(k_rate[i][j] + ",");
                }
            }
            out.write(k_rate[i][days-1] + "\n");
        }

        out.flush();
        out.close();

        out = new BufferedWriter(new FileWriter(new File("resources/experiment/trans.csv")));

        for (int i=0; i<days-1; i++) {

            for (int j=0; j<15; j++) {
                for (int k=0; k<15; k++) {
                    if (trans[i][j][k] < threshold) {
                        out.write(",");
                    } else {
                        out.write(trans[i][j][k] + ",");
                    }
                }
                out.write("\n");
            }
            out.newLine();

        }

        out.flush();
        out.close();

        out = new BufferedWriter(new FileWriter(new File("resources/experiment/topic_dist.csv")));

        for (int i=0; i<15; i++) {
            out.write("topic " + i+1 + ": " + topic_dist[i] + "\n");
        }

        out.flush();
        out.close();

    }

    private static void transitionRoute() throws IOException {
        File[] files = new File("resources/patientCSV").listFiles();
        assert files != null;

        BHMM bhmm = (BHMM) readObject("resources/save/bhmm_15_topic.model");
        assert bhmm != null;

        List<List<Integer>> zt = new ArrayList<>();

        for (File file : files) {
            zt.add(bhmm.inferOne(file.getPath()));
        }

        int total = files.length;

        // 将每一天的主题转为每一个阶段的主题
        List<List<Integer>> stage_topic = new ArrayList<>();

        int max_stage = 0;

        for (int i=0; i<total; i++) {

            List<Integer> ts = zt.get(i);
            List<Integer> topic = new ArrayList<>();
            topic.add(ts.get(0));

            for (int t : ts) {
                if (t != topic.get(topic.size()-1)) {
                    topic.add(t);
                }
            }

            if (max_stage < topic.size()) {
                max_stage = topic.size();
            }

            stage_topic.add(topic);
        }

        double[][] k_rate = new double[15][max_stage];

        double[][][] trans = new double[max_stage-1][15][15];
        double[][][] trans_rate = new double[max_stage-1][15][15];

        for (int i=0; i<total; i++) {
            List<Integer> topic = stage_topic.get(i);

            for (int j=0; j<topic.size(); j++) {
                k_rate[topic.get(j)][j]++;
            }

            for (int j=0; j<topic.size()-1; j++) {
                trans[j][topic.get(j)][topic.get(j+1)]++;
            }
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(new File("resources/save/transitionRoute.csv")));

        for (double[] k_rate_one : k_rate) {
            for (double num : k_rate_one) {
                out.write(num + ",");
            }

            out.write("\n");
        }

        out.flush();
        out.close();

        for (int i=0; i<max_stage-1; i++) {

            for (int j=0; j<15; j++) {
                for (int k=0; k<15; k++) {
                    trans_rate[i][j][k] = trans[i][j][k] / k_rate[j][i];
                }
            }
        }

        out = new BufferedWriter(new FileWriter(new File("resources/save/trans.csv")));

        for (int i=0; i<max_stage-1; i++) {

            for (int j=0; j<15; j++) {
                for (int k=0; k<15; k++) {
                    out.write(trans[i][j][k] + ",");
                }
                out.write("\n");
            }
            out.newLine();
        }

        out.flush();
        out.close();

    }

    public static void main(String[] args) throws IOException {
//        findPCIandCABG();
//        oneHundredExamplesLDA();
//        oneHundredExamples();
//        patientExamples();
//        statistic();
//        analysisParam(15);
//        traceRoute(0.05);
//        transitionRoute();
        los();
//        perplexity(15);
    }
}
