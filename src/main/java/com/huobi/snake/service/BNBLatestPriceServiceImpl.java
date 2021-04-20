package com.huobi.snake.service;

import com.alibaba.fastjson.JSON;
import com.huobi.api.response.market.SwapMarketTradeResponse;
import com.huobi.api.service.market.MarketAPIServiceImpl;
import com.huobi.snake.constants.Constants;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/*
 * 通过调用huobi_Linear_swap_Java的SwapMarketTradeResponse.getSwapMarketTrade()接口
 * 获取'USDT本位永续'中'BNB/USDT永续'的最新价格
 * 然后记录到本地的MySQL中
 * 以便进行后续操作
 */
public class BNBLatestPriceServiceImpl implements LatestPriceService {
    private static Logger logger = LoggerFactory.getLogger(BNBLatestPriceServiceImpl.class);

    private static Constants cons = new Constants();

    private static Connection conn = null;
    private static Statement stmt = null;

    private static final String BNB_USDT = "BNB-USDT"; // BNB/USDT永续
    private static final String HB_BNB_USDT = "BNB/USDT永续"; // BNB/USDT永续

    private static final String TBL_NAME = "bnb_latest_price_tbl";
    private static String insertBNBLatestPriceTbl = "INSERT INTO %s (uuid, source, contract_type, price, time) VALUES ('%s', '%s', '%s', %f, '%s');";

    /*
     * 主控制器，每隔一段时间依次调用 getMarketTrade()，分别获取 BTC_CW 和 BTC_NW 的最新合约价格
     */
    @Override
    public void getLatestPrice() {
        MarketAPIServiceImpl huobiAPIService = new MarketAPIServiceImpl();
        int i = 0;

        try {
            while (true) {
                getMarketTrade(huobiAPIService, BNB_USDT);

                i ++;

                String recordReminder = cons.getPropValues("record_reminder");

                // 默认设置为每15分钟进行一次推送提醒
                if (recordReminder.isEmpty()) {
                    recordReminder = cons.RECORD_REMINDER;
                }

                if (i % Integer.parseInt(recordReminder) == 0) {
                    logger.debug(String.format("BNBLatestPriceServiceImpl 已记录 %d 条数据.", i));
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
            logger.debug("BNBLatestPriceServiceImpl.getLatestPrice() 执行终止....");
            logger.debug("BNBLatestPriceServiceImpl.getLatestPrice() 重新执行....");

            getLatestPrice();
        }
    }

    /*
     * 根据合约标识获取'USDT本位永续'合约最新价格，并最终写入到数据库中
     * input: huobiAPIService - 调用的API service
     * input: contractType - 合约标识
     */
    @Override
    public void getMarketTrade(MarketAPIServiceImpl huobiAPIService, String contractType) {
        try {
            SwapMarketTradeResponse result = huobiAPIService.getSwapMarketTrade(contractType);

            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(JSON.toJSONString(result));
            JSONArray jTick = (JSONArray) json.get("tick");
            JSONObject jATZero = (JSONObject) jTick.get(0);
            JSONArray jData = (JSONArray) jATZero.get("data");
            JSONObject jADZero = (JSONObject) jData.get(0);
            String jPrice = (String) jADZero.get("price");

            //System.out.println(jPrice);

            String time = cons.getDateTime();

            insertLatestPriceTbl(HB_BNB_USDT, jPrice, time);
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
    public void insertLatestPriceTbl(String contract_type, String price, String time) {
        try {
            if (conn == null || !conn.isValid(1000)) {
                conn = cons.connectedToDB();
            }

            if (stmt == null || stmt.isClosed()) {
                stmt = conn.createStatement();
            }

            String sql = String.format(insertBNBLatestPriceTbl, TBL_NAME, cons.get16UUID(), cons.HB, contract_type, Double.parseDouble(price), time);
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
            }// 什么都不做
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
        logger.debug("BNBLatestPriceServiceImpl.getLatestPrice() 开始执行....");

        //getBNBLatestPrice();
    }

}