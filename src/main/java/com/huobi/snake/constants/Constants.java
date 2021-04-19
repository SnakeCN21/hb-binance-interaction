package com.huobi.snake.constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

public class Constants {
    private static Logger logger = LoggerFactory.getLogger(Constants.class);

    private static final String PROP_FILE_NAME = "config.properties";

    // MySQL 8.0 以上版本 - JDBC 驱动名及数据库 URL
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static final String SWITCH_OFF = "0";
    public static final String SWITCH_ON = "1";

    public static final String RECORD_REMINDER = "1800"; // 设置程序每插入多少条记录提醒一次
    public static final String SLEEP = "1"; // 设置程序每隔多久进行一次数据采集

    /*
     * 解析config.properties文件
     * input: key
     * output: value
     */
    public String getPropValues(String key) {
        String value = "";

        try {
            Properties prop = new Properties();

            prop.load(this.getClass().getClassLoader().getResourceAsStream(PROP_FILE_NAME));

            value = prop.getProperty(key);


        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            return value;
        }
    }

    /*
     * 创建JDBC连接
     */
    public Connection connectedToDB() {
        Connection conn = null;

        try {
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);

            // 打开链接
            //System.out.println("连接数据库...");
            conn = DriverManager.getConnection(String.format(DB_URL, this.getPropValues("database")), this.getPropValues("db_user"), this.getPropValues("db_pwd"));

            //logger.debug("Database %s 连接成功.", cons.getPropValues("database"));
        } catch (ClassNotFoundException e) {
            logger.error("Database %s 连接失败.", this.getPropValues("database"));

            // 处理 Class.forName 错误
            e.printStackTrace();
        } catch (SQLException se) {
            logger.error("Database %s 连接失败.", this.getPropValues("database"));

            // 处理 JDBC 错误
            se.printStackTrace();
        } finally {
            return conn;
        }
    }

    /*
     * 生成唯一的16位UUID
     * 组成方式为 YYMMdd(6位) + hashCode(10位)
     */
    public String get16UUID() {
        // 1.开头两位，标识业务代码或机器代码（可变参数）
        //String machineId = 11;

        // 2.中间六位整数，标识日期
        SimpleDateFormat sdf = new SimpleDateFormat("YYMMdd");
        String dayTime = sdf.format(new Date());

        // 3.生成uuid的hashCode值
        int hashCode = UUID.randomUUID().toString().hashCode();

        // 4.可能为负数
        if (hashCode < 0) {
            hashCode = -hashCode;
        }

        // 5.算法处理: 0-代表前面补充0; 10-代表长度为10; d-代表参数为整数型
        //String uuid = machineId + dayTime + String.format("%010d", hashCode);
        String uuid = dayTime + String.format("%010d", hashCode);

        return uuid;
    }

    /*
     * 返回当前日期时间
     * YYYY-MM-dd HH:mm:ss
     */
    public String getDateTime() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdfTime = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");

        return sdfTime.format(cal.getTime());
    }

    public static void main(String args[]) {
        Constants cons = new Constants();

        //System.out.println(cons.getPropValues("database"));
        //System.out.println(cons.get16UUID());
        //System.out.println(cons.getDateTime());
    }

}
