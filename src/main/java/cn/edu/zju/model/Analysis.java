package cn.edu.zju.model;

import cn.edu.zju.model.BHMM;
import cn.edu.zju.util.Scaler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import static cn.edu.zju.util.Utils.argsort;
import static cn.edu.zju.util.Utils.readObject;
import static cn.edu.zju.util.Utils.readPatientRecord;

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
            String[] interventions = content[0];

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

    private static void analysisParam() throws IOException {

        int k = 15; // 主题的数量

        BHMM bhmm = (BHMM) readObject("resources/save/bhmm_"+ k +"_topic.model");
        assert bhmm != null;
        double[][] phi = bhmm.getPhi();
        double[][][] omegaa = bhmm.getOmega();

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

    }

    public static double perplexity() throws IOException {
        BHMM bhmm = (BHMM) readObject("resources/save/bhmm_20_topic.model");
        assert bhmm != null;

        double[][] phi = bhmm.getPhi();
        double[][][] omega = bhmm.getOmega();
        int[][] zt = bhmm.getZT();  // 这段代码需要放到BHMM类的main函数中运行，因为bhmm中的zt是私有变量，同时这段改为int[][] zt = bhmm.zt;

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

    public static void main(String[] args) throws IOException {
        System.out.println(perplexity());
    }
}
