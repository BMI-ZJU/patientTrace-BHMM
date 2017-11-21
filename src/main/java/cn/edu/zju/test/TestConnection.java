package cn.edu.zju.test;

import java.sql.*;

/**
 * Created by gzx-zju on 2017/11/17.
 * test connection to oracle database
 */
public class TestConnection {

    public static Connection getConnection() {
        Connection con = null;
        try{
            Class.forName("oracle.jdbc.driver.OracleDriver");
            String url = "jdbc:oracle:thin:@172.16.200.24:1521:plaacs";
            String username = "louis";
            String password = "louis";
            con = DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return con;
    }

    public static void close(PreparedStatement statement) {
        if(statement != null){
            try{
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(ResultSet set) {
        if(set != null) {
            try{
                set.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){
        Connection con = getConnection();
        PreparedStatement statement = null;
        ResultSet set = null;

        String sql = "select NAME, SEX from DATA_SOURCE.PATIENT where PATIENT_ID = ?";

        try{
            statement = con.prepareStatement(sql);
            statement.setString(1, "F638132");
            set = statement.executeQuery();
            if (set.next()) {
                System.out.println(set.getString("NAME"));
                System.out.println(set.getString("SEX"));
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
