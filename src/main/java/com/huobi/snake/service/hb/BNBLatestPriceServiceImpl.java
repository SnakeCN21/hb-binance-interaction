package com.huobi.snake.service.hb;

import com.huobi.api.response.market.SwapMarketTradeResponse;
import com.huobi.api.service.market.MarketAPIServiceImpl;
import com.huobi.snake.constants.Constants;

import com.huobi.snake.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 通过调用huobi_Linear_swap_Java的SwapMarketTradeResponse.getSwapMarketTrade()接口
 * 获取'USDT本位永续'中'BNB/USDT永续'的最新价格
 * 然后记录到本地的MySQL中
 * 以便进行后续操作
 */
public class BNBLatestPriceServiceImpl implements HBService {
    private static Logger logger = LoggerFactory.getLogger(BNBLatestPriceServiceImpl.class);

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

    private static final String BNB_USDT = "BNB-USDT"; // 'BNB/USDT永续'合约标识
    private static final String HB_BNB_USDT = "BNB/USDT永续"; // BNB/USDT永续

    private static final String TBL_NAME = "bnb_latest_price_tbl";
    private static String insertBNBLatestPriceTbl = "INSERT INTO %s (uuid, source, contract_type, price, time) VALUES ('%s', '%s', '%s', %f, '%s');";

    /**
     * 主控制器，每隔一段时间依次调用 getMarketTrade()，分别获取 BTC_CW 和 BTC_NW 的最新合约价格
     */
    @Override
    public void getLatestPrice(LocalDateTime startDT) {
        MarketAPIServiceImpl huobiAPIService = new MarketAPIServiceImpl();

        if (isFirstRun) {
            this.startDT = startDT;
            this.prevTimeReminder = startDT;

            timeReminder = utils.checkTimeReminder(utils.getPropValues("time_reminder"));
            sleep = utils.getPropValues("sleep");

            if (sleep.isEmpty() || utils.integerVerification(sleep, "sleep")) {
                logger.debug(String.format("sleep已重置为%s.", cons.DEFAULT_SLEEP));

                sleep = cons.DEFAULT_SLEEP;
            }

            isFirstRun = false;
        }

        this.nextTimeReminder = utils.getNextTimeReminder(startDT, timeReminder);

        try {
            while (true) {
                getMarketTrade(huobiAPIService, BNB_USDT);

                LocalDateTime now = LocalDateTime.of(
                        LocalDateTime.now().getYear(),
                        LocalDateTime.now().getMonthValue(),
                        LocalDateTime.now().getDayOfMonth(),
                        LocalDateTime.now().getHour(),
                        LocalDateTime.now().getMinute(),
                        LocalDateTime.now().getSecond());

                if (utils.timeCompare(now, nextTimeReminder)) {
                    logger.debug(String.format("HB.BNBLatestPriceServiceImpl 已运行 %s.", utils.getTimeDifference(now, this.startDT, timeReminder)));

                    this.prevTimeReminder = now;
                    this.nextTimeReminder = utils.getNextTimeReminder(now, timeReminder);
                }

                TimeUnit.SECONDS.sleep(Integer.parseInt(sleep));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            logger.debug("HB.BNBLatestPriceServiceImpl.getLatestPrice() 执行终止....");
            logger.debug("HB.BNBLatestPriceServiceImpl.getLatestPrice() 重新执行....");

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
        SwapMarketTradeResponse result = huobiAPIService.getSwapMarketTrade(contractType);

        List<SwapMarketTradeResponse.TickBean> tickList = result.getTick();
        List<SwapMarketTradeResponse.TickBean.DataBean> dataList = tickList.get(0).getData();
        String price = dataList.get(0).getPrice();

        String time = utils.getDateTime();

        insertLatestPriceTbl(HB_BNB_USDT, price, time);
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

            String sql = String.format(insertBNBLatestPriceTbl, TBL_NAME, utils.get24UUID(), cons.HB, contractType, Double.parseDouble(price), time);
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
        logger.debug("HB.BNBLatestPriceServiceImpl.getLatestPrice() 开始执行....");

        //getBNBLatestPrice();
    }

}