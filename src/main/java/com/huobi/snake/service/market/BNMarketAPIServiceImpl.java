package com.huobi.snake.service.market;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.huobi.api.exception.ApiException;
import com.huobi.api.util.HbdmHttpClient;
import com.huobi.snake.constants.BNAPIConstants;
import com.huobi.snake.response.market.BNMarketTradeResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BNMarketAPIServiceImpl implements BNMarketAPIService {
    private static final String BASE_URL = "https://dapi.binance.com";

    /**
     * 返回最近价格
     *
     * symbol 和 pair 不接受同时发送
     * 发送 pair 的,返回 pair 对应所有正在交易的 symbol 数据
     * symbol, pair 都没有发送的, 返回所有 symbol 数据
     *
     * @param symbol - 交易对
     * @return BNMarketTradeResponse - 已经组装好的对象
     */
    @Override
    public List<BNMarketTradeResponse> getMarketTradeBySymbol(String symbol) {
        String body;

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("symbol", symbol.toUpperCase());

            body = HbdmHttpClient.getInstance().doGet(BASE_URL + BNAPIConstants.LATEST_PRICE, params);
            //logger.debug("body:{}", body);

            List<BNMarketTradeResponse> response = new ArrayList<BNMarketTradeResponse>();

            List<JSONObject> result = JSON.parseArray(body, JSONObject.class);

            for (int i=0; i<result.size(); i++) {
                BNMarketTradeResponse bnObj = JSON.toJavaObject(result.get(i), BNMarketTradeResponse.class);
                response.add(bnObj);
            }

            return response;
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    /**
     * 返回最近价格
     *
     * symbol 和 pair 不接受同时发送
     * 发送 pair 的,返回 pair 对应所有正在交易的 symbol 数据
     * symbol, pair 都没有发送的, 返回所有 symbol 数据
     *
     * @param pair - 标的交易对
     * @return BNMarketTradeResponse - 已经组装好的对象
     */
    @Override
    public List<BNMarketTradeResponse> getMarketTradeByPair(String pair) {
        String body;

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("pair", pair.toUpperCase());

            body = HbdmHttpClient.getInstance().doGet(BASE_URL + BNAPIConstants.LATEST_PRICE, params);
            //logger.debug("body:{}", body);

            List<BNMarketTradeResponse> response = new ArrayList<BNMarketTradeResponse>();

            List<JSONObject> result = JSON.parseArray(body, JSONObject.class);

            for (int i=0; i<result.size(); i++) {
                BNMarketTradeResponse bnObj = JSON.toJavaObject(result.get(i), BNMarketTradeResponse.class);
                response.add(bnObj);
            }

            return response;
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

}
