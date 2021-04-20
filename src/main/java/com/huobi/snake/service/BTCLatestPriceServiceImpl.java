package com.huobi.snake.service;

import com.alibaba.fastjson.JSON;
import com.huobi.api.response.market.MarketTradeResponse;
import com.huobi.api.service.market.MarketAPIServiceImpl;

import com.huobi.snake.constants.Constants;
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
 * 获取‘BTC交割合约’中BTC当周和BTC次周的最新价格
 * 然后记录到本地的MySQL中
 * 以便进行后续操作
 */
public class BTCLatestPriceServiceImpl implements LatestPriceService {
    private static Logger logger = LoggerFactory.getLogger(BTCLatestPriceServiceImpl.class);

    private static Constants cons = new Constants();

    private static Connection conn = null;
    private static Statement stmt = null;

    private static final String BTC_CW = "BTC_CW"; // BTC当周合约
    private static final String BTC_NW = "BTC_NW"; // BTC次周合约
    private static final String BTC_CQ = "BTC_CQ"; // BTC当季合约
    private static final String BTC_NQ = "BTC_NQ"; // BTC次季合约

    private static final String TBL_NAME = "btc_latest_price_tbl";

    private static String insertBTCLatestPriceTbl = "INSERT INTO %s (uuid, source, contract_type, price, time) VALUES ('%s', '%s', '%s', %f, '%s');";

    /*
     * 主控制器，每隔一段时间依次调用 getMarketTrade()，分别获取 BTC_CW 和 BTC_NW 的最新合约价格
     */
    @Override
    public void getLatestPrice() {
        MarketAPIServiceImpl huobiAPIService = new MarketAPIServiceImpl();
        int i = 0;

        try {
            while (true) {
                getMarketTrade(huobiAPIService, BTC_CW);
                getMarketTrade(huobiAPIService, BTC_NW);

                i += 2;

                String recordReminder = cons.getPropValues("record_reminder");

                // 默认设置为每15分钟进行一次推送提醒
                if (recordReminder.isEmpty()) {
                    recordReminder = cons.RECORD_REMINDER;
                }

                if (i % (Integer.parseInt(recordReminder) * 2) == 0) {
                    logger.debug(String.format("BTCLatestPriceServiceImpl 已记录 %d 条数据.", i));
                }

                String sleep = cons.getPropValues("sleep");

                // 默认设置为每1秒进行一次数据采集
                if (sleep.isEmpty()) {
                    sleep = cons.SLEEP;
                }

                TimeUnit.SECONDS.sleep(Integer.parseInt(sleep));

//                if (i == 6) {
//                    throw new NullPointerException();
//                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            logger.debug("BTCLatestPriceServiceImpl.getLatestPrice() 执行终止....");
            logger.debug("BTCLatestPriceServiceImpl.getLatestPrice() 重新执行....");

            getLatestPrice();
        }
    }

    /*
     * 根据合约标识获取'BTC交割合约'最新价格，并最终写入到数据库中
     * input: huobiAPIService - 调用的API service
     * input: contractType - 合约标识
     */
    @Override
    public void getMarketTrade(MarketAPIServiceImpl huobiAPIService, String contractType) {
        try {
            MarketTradeResponse result = huobiAPIService.getMarketTrade(contractType);

            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(JSON.toJSONString(result));
            JSONObject jTick = (JSONObject) json.get("tick");
            JSONArray jData = (JSONArray) jTick.get("data");
            JSONObject jAZero = (JSONObject) jData.get(0);
            String jPrice = (String) jAZero.get("price");

            //System.out.println(jPrice);

            String time = cons.getDateTime();

            insertLatestPriceTbl(contractType, jPrice, time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /*
     * 将获得的数据进行组装，并插入到数据表中
     * input: contractType - 合约标识
     * input: price - 合约对应的价格
     * input: time - 获取价格的时间
     */
    @Override
    public void insertLatestPriceTbl(String contractType, String price, String time) {
        try {
            if (conn == null || !conn.isValid(1000)) {
                conn = cons.connectedToDB();
            }

            if (stmt == null || stmt.isClosed()) {
                stmt = conn.createStatement();
            }

            String sql = String.format(insertBTCLatestPriceTbl, TBL_NAME, cons.get16UUID(), cons.HB, contractType, Double.parseDouble(price), time);
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
            } catch (SQLException se2) {
                // 什么都不做
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {
        logger.debug("BTCLatestPriceServiceImpl.getLatestPrice() 开始执行....");

        //getBTCLatestPrice();
    }

}
