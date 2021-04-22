package com.huobi.snake.utils;

import com.google.gson.Gson;
import com.huobi.api.exception.ApiException;
import com.huobi.api.util.HbdmHttpClient;

import com.huobi.snake.constants.Constants;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Utils {
    private static Logger logger = LoggerFactory.getLogger(Utils.class);

    private static Constants cons = new Constants();

    private static final String PROP_FILE_NAME = "config.properties";

    // MySQL 8.0 以上版本 - JDBC 驱动名及数据库 URL
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    /**
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
            logger.debug("解析config.properties文件错误: " + e);
        } finally {
            return value;
        }
    }

    /**
     * 验证config.properties文件中的部分配置是否误设了小数形式
     *
     * @param amount - 需要验证的数值
     * @param title  - 具体的参数项, 用于log记录
     * @return true - 包含小数; false - 整数
     */
    public boolean integerVerification(String amount, String title) {
        try {
            int num = Integer.parseInt(amount);

            if (num < 1) {
                logger.debug(title + "不可小于1.");

                return true;
            }
        } catch (NumberFormatException e) {
            logger.debug(title + "不可设为小数形式.");
            e.printStackTrace();

            return true;
        }

        return false;
    }

    /**
     * 分割timeReminder, 组装成一个Map<String, String>返回
     *
     * @param timeReminder
     * @return key1 - amount; key2 - unit
     */
    private Map<String, String> separateTimeReminder(String timeReminder) {
        timeReminder = timeReminder.toLowerCase();

        String amount = timeReminder.substring(0, timeReminder.length() - 1);
        String unit = timeReminder.substring(timeReminder.length() - 1);

        Map<String, String> result = new HashMap<String, String>();

        result.put("amount", amount);
        result.put("unit", unit);

        return result;
    }

    /**
     * 用于检查timeReminder是否小于最低阈值, yes - 重置为cons.MINIMUM_TIME_REMINDER
     *
     * @param timeReminder
     * @return
     */
    public String checkTimeReminder(String timeReminder) {
        String amount = this.separateTimeReminder(timeReminder).get("amount");
        String unit = this.separateTimeReminder(timeReminder).get("unit");

        if (this.integerVerification(amount, "time_reminder")) {
            logger.debug(String.format("time_reminder已重置为%s.", cons.DEFAULT_TIME_REMINDER));

            return cons.DEFAULT_TIME_REMINDER;
        }

        if (Integer.parseInt(amount) < Integer.parseInt(cons.MINIMUM_TIME_AMOUNT) && unit.equals(cons.MINIMUM_TIME_UNIT)) {
            logger.debug(String.format("time_reminder小于最低阈值, 已重置为%s.", cons.MINIMUM_TIME_AMOUNT + cons.MINIMUM_TIME_UNIT));

            return cons.MINIMUM_TIME_AMOUNT + cons.MINIMUM_TIME_UNIT;
        }

        return timeReminder;
    }

    /**
     * 传入一个初始时间, 以及一个时间间隔, 计算出下一个时间节点
     *
     * @param startDT      - 初始时间
     * @param timeReminder - 时间间隔
     * @return LocalDateTime - 下一个时间节点
     */
    public LocalDateTime getNextTimeReminder(LocalDateTime startDT, String timeReminder) {
        if (timeReminder.isEmpty()) {
            timeReminder = cons.DEFAULT_TIME_REMINDER;
        }

        String amount = this.separateTimeReminder(timeReminder).get("amount");
        String unit = this.separateTimeReminder(timeReminder).get("unit");

        if (unit.equals("s")) {
            return startDT.plusSeconds(Long.parseLong(amount));
        } else if (unit.equals("m")) {
            return startDT.plusMinutes(Long.parseLong(amount));
        } else if (unit.equals("h")) {
            return startDT.plusHours(Long.parseLong(amount));
        } else {
            return startDT.plusMonths(Long.parseLong(amount));
        }
    }

    /**
     * 传入两个LocalDateTime变量, 计算彼此之间的时间差, 如果相等则返回true, 否则false
     * 由于精度问题，目前误差设置在3000毫秒内
     *
     * @param dt1 - LocalDateTime - 当前时间
     * @param dt2 - LocalDateTime - 下一个时间节点
     * @return true - 相等, else is false
     */
    public boolean timeCompare(LocalDateTime dt1, LocalDateTime dt2) {
        Duration duration = Duration.between(dt2, dt1);

        long diff = duration.toMillis();

        if (diff >= 0 && diff < Integer.parseInt(cons.TIME_REMINDER_DEVIATION)) {
            return true;
        }

        return false;
    }

    /**
     * 传入两个LocalDateTime变量, 计算彼此之间的时间差, 并按照timeReminder的时间单位格式返回,
     *
     * @param dt1          - LocalDateTime
     * @param dt2          - LocalDateTime
     * @param timeReminder - 时间间隔
     * @return String - 返回特定格式的时间差
     */
    public String getTimeDifference(LocalDateTime dt1, LocalDateTime dt2, String timeReminder) {
        if (timeReminder.isEmpty()) {
            timeReminder = cons.DEFAULT_TIME_REMINDER;
        }

        String unit = this.separateTimeReminder(timeReminder).get("unit");

        Duration duration = Duration.between(dt2, dt1);

        if (unit.equals("s")) {
            return (duration.toMillis() / 1000) + " 秒";
        } else if (unit.equals("m")) {
            return duration.toMinutes() + " 分钟";
        } else if (unit.equals("h")) {
            return duration.toHours() + " 小时";
        } else {
            return duration.toDays() + " 天";
        }
    }

    /**
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

    /**
     * 生成唯一的16位UUID
     * 组成方式为 YYMMdd(6位) + hashCode(10位)
     */
    public String get16UUID() {
        // 1.中间6整数，标识日期
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        String date = now.format(formatter);

        // 2.生成uuid的hashCode值
        int hashCode = UUID.randomUUID().toString().hashCode();

        // 3.可能为负数
        if (hashCode < 0) {
            hashCode = -hashCode;
        }

        // 4.算法处理: 0-代表前面补充0; 10-代表长度为10; d-代表参数为整数型
        String uuid = date + String.format("%010d", hashCode);

        return uuid;
    }

    public String get24UUID() {
        // 1.中间6位整数，标识日期
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        String date = now.format(formatter);

        // 2.生成16位的数字随机数
        String random = RandomStringUtils.randomNumeric(16);
        //String random = RandomStringUtils.random(16, "abcdefgABCDEFG123456789");

        // 3.组装
        String uuid = date + random;

        return uuid;
    }

    /**
     * 返回当前日期时间
     * YYYY-MM-dd HH:mm:ss
     */
    public String getDateTime() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdfTime = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");

        return sdfTime.format(cal.getTime());
    }

    public Date getBNSystemTime() {
        String body;

        try {
            Map<String, Object> params = new HashMap<>();

            body = HbdmHttpClient.getInstance().doGet("https://dapi.binance.com/dapi/v1/time", params);

            Gson gson = new Gson();

            Map<String, Double> map = gson.fromJson(body, HashMap.class);
            Double time = map.get("serverTime");

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(Double.valueOf(time).longValue());

            return cal.getTime();
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    public static void main(String args[]) {
        //Utils utils = new Utils();

        //System.out.println(utils.getPropValues("database"));
        //System.out.println(utils.get16UUID());
        //System.out.println(utils.get24UUID());
        //System.out.println(utils.getDateTime());
    }

}
