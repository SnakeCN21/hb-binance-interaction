package com.huobi.snake.service.bn;

import com.huobi.snake.constants.Constants;
import com.huobi.snake.response.market.BNMarketTradeResponse;
import com.huobi.snake.service.market.BNMarketAPIServiceImpl;

import com.huobi.snake.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class BNBNBLatestPriceServiceImpl implements BNService {
    private static Logger logger = LoggerFactory.getLogger(BNBNBLatestPriceServiceImpl.class);

    private static Constants cons = new Constants();
    private static Utils utils = new Utils();

    private static Connection conn = null;
    private static Statement stmt = null;

    private static final String BNBUSD_PERP = "BNBUSD_PERP"; // 'BNBUSD 永续'合约标识
    private static final String BNBUSD = "BNBUSD"; // BNBUSD pair 标识

    private static final String BNBUSD_PERP_CONTRACT_TYPE = "BNBUSD 永续";
    private static final String BNBUSD_CONTRACT_TYPE = "BNBUSD 当季";

    private static final String TBL_NAME = "bnb_latest_price_tbl";
    private static String insertBNBLatestPriceTbl = "INSERT INTO %s (uuid, source, contract_type, price, time) VALUES ('%s', '%s', '%s', %f, '%s');";

    /**
     * 主控制器，每隔一段时间依次调用 getMarketTrade()，获取最新合约价格
     */
    @Override
    public void getLatestPrice() {
        BNMarketAPIServiceImpl bnMarketAPIService = new BNMarketAPIServiceImpl();
        int i = 0;

        try {
            while (true) {
                //getMarketTradeBySymbol(bnMarketAPIService, BNBUSD_PERP);
                getMarketTradeByPair(bnMarketAPIService, BNBUSD);

                i += 2;

                String recordReminder = utils.getPropValues("record_reminder");

                // 默认设置为每15分钟进行一次推送提醒
                if (recordReminder.isEmpty()) {
                    recordReminder = cons.RECORD_REMINDER;
                }

                if (i % (Integer.parseInt(recordReminder) * 2) == 0) {
                    logger.debug(String.format("BN.BNBLatestPriceServiceImpl 已记录 %d 条数据.", i));
                }

                String sleep = utils.getPropValues("sleep");

                // 默认设置为每1秒进行一次数据采集
                if (sleep.isEmpty()) {
                    sleep = cons.SLEEP;
                }

                TimeUnit.SECONDS.sleep(Integer.parseInt(sleep));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            logger.debug("BN.BNBLatestPriceServiceImpl.getLatestPrice() 执行终止....");
            logger.debug("BN.BNBLatestPriceServiceImpl.getLatestPrice() 重新执行....");

            getLatestPrice();
        }

    }

    /**
     * 根据合约标识获取最新价格，并最终写入到数据库中
     *
     * @param bnMarketAPIService - 调用的API service
     * @param symbol             - 交易对
     * @return
     */
    @Override
    public void getMarketTradeBySymbol(BNMarketAPIServiceImpl bnMarketAPIService, String symbol) {
        List<BNMarketTradeResponse> result = bnMarketAPIService.getMarketTradeBySymbol(symbol);

        String price = result.get(0).getPrice();
        String time = utils.getDateTime();

        insertLatestPriceTbl(BNBUSD_PERP_CONTRACT_TYPE, price, time);
    }

    /**
     * 根据合约标识获取最新价格，并最终写入到数据库中
     *
     * @param bnMarketAPIService - 调用的API service
     * @param pair               - 标的交易对
     * @return
     */
    @Override
    public void getMarketTradeByPair(BNMarketAPIServiceImpl bnMarketAPIService, String pair) {
        try {
            String time = utils.getDateTime();

            /*
             * List.size() 可能为1, 也可能为3, 取决于pair是什么
             */
            List<BNMarketTradeResponse> result = bnMarketAPIService.getMarketTradeByPair(pair);

            for (int i=0; i<result.size(); i++) {
                BNMarketTradeResponse marketObj = result.get(i);

                if (marketObj.getSymbol().equals(BNBUSD_PERP)) {
                    insertLatestPriceTbl(BNBUSD_PERP_CONTRACT_TYPE, marketObj.getPrice(), time);

                    result.remove(i);
                    i --;
                }
            }

            BNMarketTradeResponse marketObjA = result.get(0);
            BNMarketTradeResponse marketObjB = result.get(1);

            String strA = marketObjA.getSymbol().replace(BNBUSD + "_", "");
            String strB = marketObjB.getSymbol().replace(BNBUSD + "_", "");

            DateFormat df = new SimpleDateFormat("yyMMdd", Locale.ENGLISH);
            Date dateA =  df.parse(strA);
            Date dateB =  df.parse(strB);

            if (dateA.before(dateB)) {
                insertLatestPriceTbl(BNBUSD_CONTRACT_TYPE, marketObjA.getPrice(), time);
            } else {
                insertLatestPriceTbl(BNBUSD_CONTRACT_TYPE, marketObjB.getPrice(), time);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将获得的数据进行组装，并插入到数据表中
     *
     * @param contractType - 合约标识
     * @param price        - 合约对应的价格
     * @param time         - 获取价格的时间
     */
    @Override
    public void insertLatestPriceTbl(String contractType, String price, String time) {
        try {
            if (conn == null || !conn.isValid(1000)) {
                conn = utils.connectedToDB();
            }

            if (stmt == null || stmt.isClosed()) {
                stmt = conn.createStatement();
            }

            String sql = String.format(insertBNBLatestPriceTbl, TBL_NAME, utils.get16UUID(), cons.BN, contractType, Double.parseDouble(price), time);
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

        BNBNBLatestPriceServiceImpl bn = new BNBNBLatestPriceServiceImpl();
        bn.getLatestPrice();
    }

}