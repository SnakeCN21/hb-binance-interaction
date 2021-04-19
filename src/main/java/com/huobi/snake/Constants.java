package com.huobi.snake;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

public class Constants {
    private static final String PROP_FILE_NAME = "config.properties";

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

    public static void main(String args[]){
        Constants cons = new Constants();

        //System.out.println(cons.getPropValues("database"));
        //System.out.println(cons.get16UUID());
        //System.out.println(cons.getDateTime());
    }

}
