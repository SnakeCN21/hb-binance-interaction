package com.huobi.snake;

import com.alibaba.fastjson.JSON;
import com.huobi.api.response.market.MarketTradeResponse;
import com.huobi.api.service.market.MarketAPIServiceImpl;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.concurrent.TimeUnit;

/*
* 通过调用hbdm-java-sdk的MarketTradeResponse.getMarketTrade()接口
* 获取BTC当周和BTC次周的最新价格
* 然后记录到本地的MySQL中
* 以便进行后续操作
 */
public class MarketServiceImpl {
    private static Logger logger = LoggerFactory.getLogger(MarketServiceImpl.class);

    private static Constants cons= new Constants();

    private static Connection conn = null;
    private static Statement stmt = null;

    // MySQL 8.0 以上版本 - JDBC 驱动名及数据库 URL
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final String LATEST_PRICE_TBL = "latest_price_tbl";
    private static final String BTC_CW = "BTC_CW"; // BTC当周合约
    private static final String BTC_NW = "BTC_NW"; // BTC次周合约
    private static final String BTC_CQ = "BTC_CQ"; // BTC当季合约
    private static final String BTC_NQ = "BTC_NQ"; // BTC次季合约

    private static String insertLatestPriceTbl = "INSERT INTO %s (uuid, contract_type, price, time) VALUES ('%s', '%s', %f, '%s');";

    /*
    * 将获得的数据进行组装，并插入到数据库表中
    * input: contractType - 合约标识
    * input: price - 合约对应的价格
    * input: time - 获取价格的时间
     */
    private static void insertLatestPriceTbl(String contractType, String price, String time) {
        try {
            if (conn == null || !conn.isValid(1000)) {
                connectedToDB();
            }

            if (stmt == null || stmt.isClosed()) {
                stmt = conn.createStatement();
            }

            String sql = String.format(insertLatestPriceTbl, LATEST_PRICE_TBL, cons.get16UUID(), contractType, Double.parseDouble(price), time);
            stmt.executeUpdate(sql);

            // 完成后关闭
            stmt.close();
            conn.close();
        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (stmt != null) {
                    stmt.close();
                }
            }catch(SQLException se2){
            }// 什么都不做
            try{
                if (conn != null) {
                    conn.close();
                }
            }catch(SQLException se){
                se.printStackTrace();
            }
        }
    }

    /*
    * 创建JDBC连接
     */
    private static void connectedToDB() {
        try {
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);

            // 打开链接
            //System.out.println("连接数据库...");
            conn = DriverManager.getConnection(String.format(DB_URL, cons.getPropValues("database")), cons.getPropValues("db_user"), cons.getPropValues("db_pwd"));

            //logger.debug("Database %s 连接成功.", cons.getPropValues("database"));
        } catch (ClassNotFoundException e) {
            logger.error("Database %s 连接失败.", cons.getPropValues("database"));

            // 处理 Class.forName 错误
            e.printStackTrace();
        } catch (SQLException se) {
            logger.error("Database %s 连接失败.", cons.getPropValues("database"));

            // 处理 JDBC 错误
            se.printStackTrace();
        }
    }

    /*
    * 主控制器，每隔1秒依次调用 getMarketTrade()，分别获取 BTC_CW 和 BTC_NW 的最新合约价格
     */
    private static void latestPriceImpl() {
        MarketAPIServiceImpl huobiAPIService = new MarketAPIServiceImpl();
        int i = 0;

        try {
            while (true) {
                String time = cons.getDateTime();

                getMarketTrade(huobiAPIService, BTC_CW);
                getMarketTrade(huobiAPIService, BTC_NW);

                i += 2;

                String recordReminder = cons.getPropValues("record_reminder");

                // 默认设置为每15分钟进行一次推送提醒
                if (recordReminder.isEmpty()) {
                    recordReminder = "1800";
                }

                if (i % Integer.parseInt(recordReminder) == 0) {
                    logger.debug(String.format("已记录 %d 条数据.", i));
                }

                String sleep = cons.getPropValues("sleep");

                // 默认设置为每1秒进行一次数据采集
                if (sleep.isEmpty()) {
                    sleep = "1";
                }

                TimeUnit.SECONDS.sleep(Integer.parseInt(sleep));

//                if (i == 6) {
//                    throw new NullPointerException();
//                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            logger.debug("MarketServiceImpl.latestPriceImpl() 执行终止....");
            logger.debug("MarketServiceImpl.latestPriceImpl() 重新执行....");

            latestPriceImpl();
        }
    }

    /*
    * 根据合约标识获取合约最新价格，并最终写入到数据库中
    * input: huobiAPIService - 调用的API service
    * input: contractType - 合约标识
     */
    private static void getMarketTrade(MarketAPIServiceImpl huobiAPIService, String contractType) throws ParseException {
        MarketTradeResponse result = huobiAPIService.getMarketTrade(contractType);

        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(JSON.toJSONString(result));
        JSONObject jTick = (JSONObject)json.get("tick");
        JSONArray jData = (JSONArray)jTick.get("data");
        JSONObject jAZero = (JSONObject)jData.get(0);
        String jPrice = (String)jAZero.get("price");

        //System.out.println(jPrice);

        String time = cons.getDateTime();

        insertLatestPriceTbl(contractType, jPrice, time);
    }

    public static void main(String args[]){
        logger.debug("MarketServiceImpl.latestPriceImpl() 开始执行....");

        latestPriceImpl();
    }

}
