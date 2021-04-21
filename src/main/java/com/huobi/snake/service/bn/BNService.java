package com.huobi.snake.service.bn;

import com.huobi.snake.service.TopService;
import com.huobi.snake.service.market.BNMarketAPIServiceImpl;

import java.time.LocalDateTime;

public interface BNService extends TopService {
    /**
     * 主控制器，每隔一段时间依次调用 getMarketTrade()，获取最新合约价格
     */
    @Override
    void getLatestPrice(LocalDateTime startDT);

    /**
     * 根据合约标识获取最新价格，并最终写入到数据库中
     *
     * @param bnMarketAPIService - 调用的API service
     * @param symbol             - 交易对
     * @return
     */
    void getMarketTradeBySymbol(BNMarketAPIServiceImpl bnMarketAPIService, String symbol);

    /**
     * 根据合约标识获取最新价格，并最终写入到数据库中
     *
     * @param bnMarketAPIService - 调用的API service
     * @param pair               - 标的交易对
     * @return
     */
    void getMarketTradeByPair(BNMarketAPIServiceImpl bnMarketAPIService, String pair);

    /**
     * 将获得的数据进行组装，并插入到数据表中
     *
     * @param contractType - 合约标识
     * @param price        - 合约对应的价格
     * @param time         - 获取价格的时间
     */
    void insertLatestPriceTbl(String contractType, String price, String time);
}
