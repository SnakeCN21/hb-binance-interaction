package com.huobi.snake.service;

import com.huobi.api.service.market.MarketAPIServiceImpl;

public interface LatestPriceService {

    /*
     * 主控制器，每隔一段时间依次调用 getMarketTrade()，获取最新合约价格
     */
    void getLatestPrice();

    /*
     * 根据合约标识获取最新价格，并最终写入到数据库中
     * input: huobiAPIService - 调用的API service
     * input: contractType - 合约标识
     */
    void getMarketTrade(MarketAPIServiceImpl huobiAPIService, String contractType);

    /*
     * 将获得的数据进行组装，并插入到数据表中
     * input: tblName - 表名
     * input: contractType - 合约标识
     * input: price - 合约对应的价格
     * input: time - 获取价格的时间
     */
    void insertLatestPriceTbl(String tblName, String source, String price, String time);
}
