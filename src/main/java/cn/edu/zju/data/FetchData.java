package cn.edu.zju.data;

import cn.edu.zju.util.ManageConnection;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

import static cn.edu.zju.util.ManageConnection.close;

/**
 * Created by gzx-zju on 2017/11/21.
 * 从数据库中提取数据并且保存成XML的格式
 */
public class FetchData {

    private String url = "jdbc:oracle:thin:@172.16.200.24:1521:plaacs";
    private String username = "louis";
    private String password = "louis";
    private Connection connection = null;

    private OutputFormat format = new OutputFormat();

    public FetchData() {
        init();
    }

    public FetchData(String url, String username, String password) {
        this.url = url;
        this.password = password;
        this.username = username;
        init();
    }

    private void init() {
        connection = ManageConnection.getConnection(url, username, password);
        format.setEncoding("UTF-8");
        format.setNewlines(true);
        format.setIndent(true);
        format.setIndent("    ");
    }

    public void fetchAndSave() {
        String selectPatient = "SELECT * FROM DATA_SOURCE.PATIENT";
        String selectVisit = "SELECT * FROM DATA_SOURCE.VISIT WHERE PATIENT_ID = ?";

        try {
            PreparedStatement patientStatement = connection.prepareStatement(selectPatient);
            ResultSet patientSet = patientStatement.executeQuery();

            while (patientSet.next()) {
                String patientId = patientSet.getString("PATIENT_ID");
                PreparedStatement visitStatement = connection.prepareStatement(selectVisit);
                visitStatement.setString(1, patientId);
                ResultSet visitSet = visitStatement.executeQuery();
                while (visitSet.next()) {
                    // 只有当该病人有visit的情况下才进行记录，且每次visit作为一个记录保存下来
                    saveOne(visitSet, patientId);
                }
                close(visitSet);
                close(visitStatement);
                break;
            }

            // 关闭所使用的资源
            close(patientSet);
            close(patientStatement);
        }catch(SQLException e) {
            e.printStackTrace();
        } finally {
            // 关闭连接
            close(connection);
        }
    }

    /**
     * 将一个病人的一次就诊记录保存下来
     *
     * @param visitSet 根据patientId查询获得的一个Result Set，包含了该病人的就诊记录
     * @param patientId 病人的ID
     */
    private void saveOne(ResultSet visitSet, String patientId) {
        // TODO : 将整个函数分解成小函数
        String selectOrders = "SELECT * FROM DATA_SOURCE.ORDERS WHERE PATIENT_ID = ? AND VISIT_ID = ?";
        String selectPrescMaster = "SELECT PRESC_NO, PRESC_DATE FROM DATA_SOURCE.PRESC_MASTER WHERE PATIENT_ID = ? AND VISIT_ID = ?";
        String selectPrescDetail = "SELECT * FROM DATA_SOURCE.PRESC_DETAIL WHERE PRESC_NO = ? AND to_char(PRESC_DATE, 'yyyy-mm-dd')=?";
        String selectOperation = "SELECT OPER_ID, START_DATE_TIME, END_DATE_TIME FROM DATA_SOURCE.OPERATION WHERE PATIENT_ID = ? AND VISIT_ID = ?";
        String selectOperItem = "SELECT OPERATION FROM DATA_SOURCE.OPER_ITEM WHERE PATIENT_ID = ? AND VISIT_ID = ? AND OPER_ID = ?";

        PreparedStatement orderStatement = null;
        ResultSet orderSet = null;

        PreparedStatement prescMasterStatement = null;
        ResultSet prescMasterSet = null;
        PreparedStatement prescDetailStatement = null;
        ResultSet prescDetailSet = null;

        PreparedStatement operationStatement = null;
        ResultSet operationSet = null;
        PreparedStatement operItemStatement = null;
        ResultSet operItemSet = null;

        try {
            // 生成文档保存的路径
            String visitId = visitSet.getString("VISIT_ID");
            String rootPath = "src/main/resources/patientTrace/";
            String fileName = rootPath + patientId + "_" + visitId + ".xml";

            Document document = DocumentHelper.createDocument();
            String admissionTime = visitSet.getString("ADMISSION_DATE_TIME");
            String dischargeTime = visitSet.getString("DISCHARGE_DATE_TIME");
            Element root = document.addElement("patientTrace")
                    .addAttribute("patientId", patientId)
                    .addAttribute("admissionTime", admissionTime)
                    .addAttribute("dischargeTime", dischargeTime);

            // 医嘱信息的处理流程
            orderStatement = connection.prepareStatement(selectOrders);
            orderStatement.setString(1, patientId);
            orderStatement.setString(2, visitId);
            orderSet = orderStatement.executeQuery();

            Element orders = root.addElement("orders");
            while (orderSet.next()) {
                String name = orderSet.getString("ORDER_TEXT");
                String dosage = orderSet.getString("DOSAGE");
                String unit = orderSet.getString("DOSAGE_UNITS");
                String startTime = orderSet.getString("START_DATE_TIME");
                String stopTime = orderSet.getString("STOP_DATE_TIME");
                orders.addElement("order")
                        .addAttribute("name", name)
                        .addAttribute("dosage", dosage)
                        .addAttribute("unit", unit)
                        .addAttribute("startTime", startTime)
                        .addAttribute("stopTime", stopTime);
            }

            // 处方信息的处理
            prescMasterStatement = connection.prepareStatement(selectPrescMaster);
            prescMasterStatement.setString(1, patientId);
            prescMasterStatement.setString(2, visitId);
            prescMasterSet = prescMasterStatement.executeQuery();

            Element presces = root.addElement("presces");
            while (prescMasterSet.next()) {
                int prescNo = prescMasterSet.getInt("PRESC_NO");
                String prescDate = prescMasterSet.getString("PRESC_DATE").substring(0, 10);
                prescDetailStatement = connection.prepareStatement(selectPrescDetail);
                prescDetailStatement.setInt(1, prescNo);
                prescDetailStatement.setString(2, prescDate);
                prescDetailSet = prescDetailStatement.executeQuery();

                while (prescDetailSet.next()) {
                    presces.addElement("presc")
                            .addAttribute("name", prescDetailSet.getString("DRUG_NAME"))
                            .addAttribute("dosage", prescDetailSet.getString("DOSAGE"))
                            .addAttribute("unit", prescDetailSet.getString("QUANTITY"))
                            .addAttribute("frequency", prescDetailSet.getString("FREQUENCY"))
                            .addAttribute("date", prescDate);
                }
            }

            // TODO: 手术信息的处理
            operationStatement = connection.prepareStatement(selectOperation);
            operationStatement.setString(1, patientId);
            operationStatement.setString(2, visitId);
            operationSet = operationStatement.executeQuery();

            Element operations = root.addElement("operations");
            while(operationSet.next()) {
                int operId = operationSet.getInt(1);
                operations.addAttribute("startTime", operationSet.getString("START_DATE_TIME"))
                        .addAttribute("stopTime", operationSet.getString("END_DATE_TIME"));

                operItemStatement = connection.prepareStatement(selectOperItem);
                operItemStatement.setString(1, patientId);
                operItemStatement.setString(2, visitId);
                operItemStatement.setInt(3, operId);
                operItemSet = operItemStatement.executeQuery();

                while(operItemSet.next()) {
                    operations.addElement("item")
                            .addAttribute("name", operItemSet.getString(1));
                }
            }

            // 写入文件
            FileWriter out = new FileWriter(fileName);
            XMLWriter writer = new XMLWriter(out, format);
            writer.write(document);
            writer.close();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        } finally {
            close(orderSet);
            close(orderStatement);
            close(prescMasterSet);
            close(prescDetailSet);
            close(prescMasterStatement);
            close(prescDetailStatement);
            close(operationSet);
            close(operationStatement);
            close(operItemSet);
            close(operItemStatement);
        }
    }

    public static void main(String[] args) {
        FetchData fetchData = new FetchData();
        fetchData.fetchAndSave();
    }
}
