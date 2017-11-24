package cn.edu.zju.data;

import java.io.File;

/**
 * Created by gzx-zju on 2017/11/23.
 * 将从数据库中提取的XML格式的数据转换
 */
public class DataTrans {
    private String rootPath = "src/main/resources/patientTrace";

    public static void main(String[] args) {
        String rootPath = "src/main/resources/patientTrace";
        File root = new File(rootPath);

        File[] files = root.listFiles();

        assert files != null;
        for(File file : files) {
            System.out.println(file.getName());
            break;
        }
    }
}
