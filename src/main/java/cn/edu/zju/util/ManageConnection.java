package cn.edu.zju.util;

import java.sql.*;

/**
 * Created by gzx-zju on 2017/11/17.
 * test connection to oracle database
 */
public class ManageConnection {

    public static Connection getConnection(String url, String username, String password) {
        Connection con = null;
        try{
            Class.forName("oracle.jdbc.driver.OracleDriver");
            con = DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException | SQLException e) {
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

}
