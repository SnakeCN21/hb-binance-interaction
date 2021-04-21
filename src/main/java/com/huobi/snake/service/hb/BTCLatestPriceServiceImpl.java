package com.huobi.snake.service.hb;

import com.huobi.api.response.market.MarketTradeResponse;
import com.huobi.api.service.market.MarketAPIServiceImpl;

import com.huobi.snake.constants.Constants;
import com.huobi.snake.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 通过调用hbdm-java-sdk的MarketTradeResponse.getMarketTrade()接口
 * 获取‘BTC交割合约’中BTC当周和BTC次周的最新价格
 * 然后记录到本地的MySQL中
 * 以便进行后续操作
 */
public class BTCLatestPriceServiceImpl implements HBService {
    private static Logger logger = LoggerFactory.getLogger(BTCLatestPriceServiceImpl.class);

    private static Constants cons = new Constants();
    private static Utils utils = new Utils();

    private static LocalDateTime startDT = null;
    private static LocalDateTime prevTimeReminder = null;
    private static LocalDateTime nextTimeReminder = null;

    private static boolean isFirstRun = true;

    private static String timeReminder = "";
    private static String sleep = "";

    private static Connection conn = null;
    private static Statement stmt = null;

    private static final String BTC_CW = "BTC_CW"; // BTC当周合约
    private static final String BTC_NW = "BTC_NW"; // BTC次周合约
    private static final String BTC_CQ = "BTC_CQ"; // BTC当季合约
    private static final String BTC_NQ = "BTC_NQ"; // BTC次季合约

    private static final String TBL_NAME = "btc_latest_price_tbl";

    private static String insertBTCLatestPriceTbl = "INSERT INTO %s (uuid, source, contract_type, price, time) VALUES ('%s', '%s', '%s', %f, '%s');";

    /**
     * 主控制器，每隔一段时间依次调用 getMarketTrade()，分别获取 BTC_CW 和 BTC_NW 的最新合约价格
     */
    @Override
    public void getLatestPrice(LocalDateTime startDT) {
        MarketAPIServiceImpl huobiAPIService = new MarketAPIServiceImpl();

        //long l = 0;

        timeReminder = utils.getPropValues("time_reminder");
        sleep = utils.getPropValues("sleep");

        if (isFirstRun) {
            this.startDT = startDT;
            this.prevTimeReminder = startDT;
            isFirstRun = false;
        }

        this.nextTimeReminder = utils.getNextTimeReminder(startDT, timeReminder);

        try {
            while (true) {
                getMarketTrade(huobiAPIService, BTC_CW);
                getMarketTrade(huobiAPIService, BTC_NW);

                LocalDateTime now = LocalDateTime.of(
                        LocalDateTime.now().getYear(),
                        LocalDateTime.now().getMonthValue(),
                        LocalDateTime.now().getDayOfMonth(),
                        LocalDateTime.now().getHour(),
                        LocalDateTime.now().getMinute(),
                        LocalDateTime.now().getSecond());

                //if (now.equals(nextTimeReminder)) {
                if (utils.timeCompare(now, nextTimeReminder)) {
                    logger.debug(String.format("HB.BTCLatestPriceServiceImpl 已运行 %s.", utils.getTimeDifference(now, this.startDT, timeReminder)));

                    this.prevTimeReminder = now;
                    this.nextTimeReminder = utils.getNextTimeReminder(now, timeReminder);
                }

                // 默认设置为每1秒进行一次数据采集
                if (sleep.isEmpty()) {
                    sleep = cons.DEFAULT_SLEEP;
                }

                TimeUnit.SECONDS.sleep(Integer.parseInt(sleep));

//                l += 2;
//                if (l == 30) {
//                    throw new NullPointerException();
//                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            logger.debug("HB.BTCLatestPriceServiceImpl.getLatestPrice() 执行终止....");
            logger.debug("HB.BTCLatestPriceServiceImpl.getLatestPrice() 重新执行....");

            getLatestPrice(this.prevTimeReminder);
        }
    }

    /**
     * 根据合约标识获取'BTC交割合约'最新价格，并最终写入到数据库中
     *
     * @param huobiAPIService - 调用的API service
     * @param contractType    - 合约标识
     * @return
     */
    @Override
    public void getMarketTrade(MarketAPIServiceImpl huobiAPIService, String contractType) {
        MarketTradeResponse result = huobiAPIService.getMarketTrade(contractType);

        MarketTradeResponse.TickBean tickList = result.getTick();
        List<MarketTradeResponse.TickBean.DataBean> dataList = tickList.getData();
        String price = dataList.get(0).getPrice();

        String time = utils.getDateTime();

        insertLatestPriceTbl(contractType, price, time);
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

            String sql = String.format(insertBTCLatestPriceTbl, TBL_NAME, utils.get24UUID(), cons.HB, contractType, Double.parseDouble(price), time);
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
        logger.debug("HB.BTCLatestPriceServiceImpl.getLatestPrice() 开始执行....");

        //getBTCLatestPrice();
    }

}
