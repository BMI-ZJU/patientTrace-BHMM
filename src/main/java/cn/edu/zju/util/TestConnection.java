package cn.edu.zju.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static cn.edu.zju.util.ManageConnection.close;
import static cn.edu.zju.util.ManageConnection.getConnection;

/**
 * Created by gzx-zju on 2017/11/21.
 * 测试数据库连接
 */
public class TestConnection {

    public static void main(String[] args){
        String url = "jdbc:oracle:thin:@172.16.200.24:1521:plaacs";
        String username = "louis";
        String password = "louis";
        Connection con = getConnection(url, username, password);
        PreparedStatement statement = null;
        ResultSet set = null;

        String sql = "select * from DATA_SOURCE.PATIENT where PATIENT_ID = ?";

        try{
            statement = con.prepareStatement(sql);
            statement.setString(1, "F638132");
            set = statement.executeQuery();
            if (set.next()) {
                System.out.println(set.getString(1));
                System.out.println(set.getString("SEX"));
                System.out.println(set.getDate("DATE_OF_BIRTH"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(set);
            close(statement);
            close(con);
        }
    }
}
