/**
 * Copyright 2019 Mek Global Limited.
 */
package com.kucoin.sdk.rest.interfaces.retrofit;

import com.kucoin.sdk.rest.response.AllTickersResponse;
import com.kucoin.sdk.rest.response.KucoinResponse;
import com.kucoin.sdk.rest.response.SymbolResponse;
import com.kucoin.sdk.rest.response.SymbolTickResponse;
import com.kucoin.sdk.rest.response.TickerResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.util.List;

/**
 * Created by chenshiwei on 2019/1/11.
 */
public interface SymbolAPIRetrofit {

    @Deprecated
    @GET("api/v1/symbols")
    Call<KucoinResponse<List<SymbolResponse>>> getSymbols();

    @GET("api/v2/symbols")
    Call<KucoinResponse<List<SymbolResponse>>> getSymbols(@Query("market") String market);

    @GET("api/v1/market/orderbook/level1")
    Call<KucoinResponse<TickerResponse>> getTicker(@Query("symbol") String symbol);

    @GET("api/v1/market/stats")
    Call<KucoinResponse<SymbolTickResponse>> getMarketStats(@Query("symbol") String symbol);

    @GET("api/v1/market/allTickers")
    Call<KucoinResponse<AllTickersResponse>> getAllTickers();

    @GET("api/v1/markets")
    Call<KucoinResponse<List<String>>> getMarketList();
}
