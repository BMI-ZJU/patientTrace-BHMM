package cn.edu.zju.data;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Map.Entry;

import static cn.edu.zju.data.SortData.listAll;
import static cn.edu.zju.util.Rules.*;
import static cn.edu.zju.util.Utils.sbc2dbc;

/**
 * Created by gzx-zju on 2017/11/23.
 * 将从数据库中提取的XML格式的数据转换
 */
public class DataTrans {

    /**
     *
     * @param rootPath 资源文件根目录
     * @param covered 是否覆盖原有的结果文件
     * @throws DocumentException xml dom 解析错误
     * @throws ParseException 解析日期错误
     * @throws IOException 文件读写错误
     */
    public void processAll(String rootPath, boolean covered) throws DocumentException, ParseException, IOException {
        File root = new File(rootPath);
        File[] files = root.listFiles();

        assert files != null;
        for (File file: files) {
            processOne(file.getPath(), covered);
        }
    }

    // 统计有哪些独立的医嘱、处方、手术项，然后再填入
    public void processOne(String path, boolean covered) throws DocumentException, ParseException, IOException {
        Document document = parse(path);
        String savePath = path.replace(".xml", ".csv"); //新的保存路径，改为csv文件
        savePath = savePath.replace("patientTrace", "patientCSV");

        if (!covered) { // 如果不是覆盖模式，则判断文件是否存在，存在的话就直接返回
            File saveFile = new File(savePath);
            if (saveFile.exists()) return;
        }
        System.out.println(path);

        List<String> headers = new ArrayList<>(); // 列名，[patientId, 第1天, 第2天, ..., 第n天]
        Map<String, Integer> rowName = new LinkedHashMap<>(); // 行名，代表不同的医嘱，处方，手术信息
        double[][] content; // 内容，对于次数类的为int，一些为intensity，需要为double类型

        Element rootElement = document.getRootElement();

        // 确定文件头
        String patientId = rootElement.attributeValue("patientId");
        String admissionTime = rootElement.attributeValue("admissionTime");
        String dischargeTime = rootElement.attributeValue("dischargeTime");
        int hospitalizedDays = calHospitalizedDays(admissionTime, dischargeTime);
        headers.add(patientId);
        IntStream.range(0, hospitalizedDays).forEach(i -> headers.add("第"+i+"天"));
        determine(rootElement, rowName);

        int column = headers.size() - 1;
        int row = rowName.size();
        content = new double[row][column];

        fillIn(rootElement, content, rowName);

        saveOne(savePath, headers, rowName, content);
    }

    //确定有哪些独立的医嘱、处方、手术
    private void determine(Element root, Map<String, Integer> rowName) throws ParseException {
        String admissionTime = root.attributeValue("admissionTime");
        String dischargeTime = root.attributeValue("dischargeTime");

        int elements = 0;

        // 处理处方
        List<Element> prescs = root.element("presces").elements();
        for (Element e : prescs) {
            String name = e.attributeValue("name");

            if (isMatchOperation(name)) continue; // 如果医嘱中的是手术信息，则丢弃，以手术记录表中的为准
            name = transName(name);
            if (!rowName.containsKey(name)) {
                rowName.put(name, elements);
                elements ++;
            }
        }

        // 处理检验信息
        List<Element> labtests = root.element("labtests").elements();
        for (Element e : labtests) {
            String time = e.attributeValue("time");
            if (calHospitalizedDays(admissionTime, time) < 0 || calHospitalizedDays(dischargeTime, time) > 1) {
                continue;
            }

            String name = e.attributeValue("name");

            if (name == null) continue;
            name = transName(name);
            if (!rowName.containsKey(name)) {
                rowName.put(name, elements);
                elements ++;
            }

        }

        // 处理检查信息
        List<Element> exams = root.element("exams").elements();
        for (Element e : exams) {
            String name = e.attributeValue("class");
            name = sbc2dbc(name).toUpperCase();
            String time = e.attributeValue("time");
            if (calHospitalizedDays(admissionTime, time) < 0 || calHospitalizedDays(dischargeTime, time) > 1) {
                continue;
            }
            if (!rowName.containsKey(name)) {
                rowName.put(name, elements);
                elements++;
            }
        }

        // 有病人的手术记录在出院后，直接丢弃数据
        Element operations = root.element("operations");
        String endTime = operations.attributeValue("stopTime");
        if (endTime == null || calHospitalizedDays(dischargeTime, endTime) > 1) {
            return;
        }

        List<Element> items = operations.elements();
        for (Element e : items) {
            String name = e.attributeValue("name");
            if (name == null) continue;
            String[] separate = name.split("[+\\uff0b]"); // 在电子病历中，一个手术的不同项在同一条记录中，用+号分割
            for (String item : separate) {
                item = sbc2dbc(item).toUpperCase();
                if(!rowName.containsKey(item)) {
                    rowName.put(item, elements);
                    elements ++;
                }
            }
        }
    }

    // 将干预行为填入
    private void fillIn(Element root, double[][] content, Map<String, Integer> rowName) throws ParseException {
        String admissionTime = root.attributeValue("admissionTime");
        String dischargeTime = root.attributeValue("dischargeTime");
        int hospitalizedDays = calHospitalizedDays(admissionTime, dischargeTime);

        // 检验信息的处理
        List<Element> labtests = root.element("labtests").elements();
        for (Element labtest: labtests) {
            String name = labtest.attributeValue("name");
            if (name == null) continue;
            name = transName(name);
            String date = labtest.attributeValue("time");

            int column = calHospitalizedDays(admissionTime, date) - 1;
            if (column < 0 || calHospitalizedDays(dischargeTime, date) > 1) continue;
            int row = rowName.get(name);
            content[row][column] += 1;
        }

        // 检查信息的处理
        List<Element> exams = root.element("exams").elements();
        for (Element exam: exams) {
            String name = exam.attributeValue("class");
            name = transName(name);
            String date = exam.attributeValue("time");

            int column = calHospitalizedDays(admissionTime, date) - 1;
            if (column < 0 || calHospitalizedDays(dischargeTime, date) > 1) continue;
            int row = rowName.get(name);
            content[row][column] += 1;
        }

        //  处方信息的处理。需要注意的有该处方只有第一天开了，但有一定的量，之后每天都需要填入，同时不应超过出院的日子。
        List<Element> presces = root.element("presces").elements();
        for (Element presc : presces) {
            String name = presc.attributeValue("name");
            name = transName(name);
            String date = presc.attributeValue("date");

            int row = rowName.get(name);
            int column = calHospitalizedDays(admissionTime, date) - 1;
            if (presc.attribute("dosage") != null) {
                double dosage = Double.parseDouble(presc.attributeValue("dosage"));
                double quantity = Double.parseDouble(presc.attributeValue("quantity"));
                String frequency = null;
                if(presc.attribute("frequency") != null) {
                    frequency = presc.attributeValue("frequency");
                    if (frequency.replaceAll("\\D+", "").equals("")) {
                        if (column >= content[0].length || column < 0) continue;
                        content[row][column] += dosage;
                    } else {
                        double freq = Double.parseDouble(frequency.replaceAll("\\D+", ""));
                        int continuedDays = (int) Math.ceil(quantity / (dosage * freq));
                        int endDay = column + continuedDays - 1; // 处方结束日期

                        if (endDay + 1 > hospitalizedDays) {  // 处方结束日期不能超过住院日期
                            endDay = hospitalizedDays - 1;
                        }

                        for (int col=column; col <= endDay; col++) {
                            content[row][col] += dosage * freq;
                        }
                    }
                } else {
                    content[row][column] += 1;
                }
            } else {
                content[row][column] += 1;
            }

        }

        // 手术信息的处理。可能没有手术信息
        if (root.element("operations").element("item") == null) {
            return;
        }
        Element operation = root.element("operations");
        String startTime = operation.attributeValue("startTime");
        int column = calHospitalizedDays(admissionTime, startTime) - 1;
        if (column >= content[0].length || column < 0) {
            return;
        }
        List<Element> items = operation.elements();
        for (Element item : items) {
            String name = item.attributeValue("name");
            if (name == null) continue;
            String[] separate = name.split("[+\\uff0b]");
            for (String i : separate) {
                i = sbc2dbc(i).toUpperCase();
                int row = rowName.get(i);
                content[row][column] += 1;
            }

        }

    }

    private void saveOne(String savePath, List<String> headers, Map<String, Integer> rowName, double[][] content) throws IOException {
        File file = new File(savePath);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "GBK"));

        String h = headers.stream().collect(Collectors.joining(","));
        writer.write(h + "\n");

        for (Entry<String, Integer> r : rowName.entrySet()) {
            String r_name = r.getKey();
            int n_row = r.getValue();
            double[] c = content[n_row];
            String record = r_name + "," + Arrays.toString(c).replace("[","").replace("]", "");
            writer.write(record + "\n");
        }

        writer.flush();
        writer.close();
    }

    // 计算住院天数
    private int calHospitalizedDays(String startTime, String endTime) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date start = sdf.parse(startTime);
        Date end = sdf.parse(endTime);
        return calHospitalizedDays(start, end);
    }

    private int calHospitalizedDays(Date startTime, Date endTime) {
        return (int) ((endTime.getTime() - startTime.getTime()) / (1000*60*60*24)) + 1; //需要加1，入院第一天开始就会有医嘱，处方等信息
    }

    private Document parse(String path) throws DocumentException {
        SAXReader reader = new SAXReader();
        return reader.read(path);
    }

    public static void main(String[] args) throws DocumentException, ParseException, IOException {
        DataTrans dataTrans = new DataTrans();
        dataTrans.processAll("resources/patientTrace/", true);
        listAll();
    }
}
