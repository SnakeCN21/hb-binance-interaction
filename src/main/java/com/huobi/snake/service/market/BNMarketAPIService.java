package com.huobi.snake.service.market;

import com.huobi.snake.response.market.BNMarketTradeResponse;

import java.util.List;

public interface BNMarketAPIService {

    /**
     * 返回最近价格
     *
     * symbol 和 pair 不接受同时发送
     * 发送 pair 的,返回 pair 对应所有正在交易的 symbol 数据
     * symbol, pair 都没有发送的,返回所有 symbol 数据
     *
     * @param symbol 交易对
     * @return BNMarketTradeResponse 已经组装好的对象
     */
    List<BNMarketTradeResponse> getMarketTradeBySymbol(String symbol);

    /**
     * 返回最近价格
     *
     * symbol 和 pair 不接受同时发送
     * 发送 pair 的,返回 pair 对应所有正在交易的 symbol 数据
     * symbol, pair 都没有发送的,返回所有 symbol 数据
     *
     * @param pair   标的交易对
     * @return BNMarketTradeResponse 已经组装好的对象
     */
    List<BNMarketTradeResponse> getMarketTradeByPair(String pair);

}
