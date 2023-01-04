/**
 * Copyright 2019 Mek Global Limited.
 */
package com.kucoin.sdk;

import com.kucoin.sdk.model.enums.PrivateChannelEnum;
import com.kucoin.sdk.rest.request.OrderCreateApiRequest;
import com.kucoin.sdk.rest.request.StopOrderCreateRequest;
import com.kucoin.sdk.rest.response.AccountBalancesResponse;
import com.kucoin.sdk.rest.response.OrderCancelResponse;
import com.kucoin.sdk.rest.response.OrderCreateResponse;
import com.kucoin.sdk.rest.response.StopOrderResponse;
import com.kucoin.sdk.websocket.event.AccountChangeEvent;
import com.kucoin.sdk.websocket.event.AdvancedOrderEvent;
import com.kucoin.sdk.websocket.event.OrderActivateEvent;
import com.kucoin.sdk.websocket.event.OrderChangeEvent;
import org.hamcrest.core.Is;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertTrue;

/**
 * Created by chenshiwei on 2019/1/23.
 * <p>
 * Run with -Dorg.slf4j.simpleLogger.defaultLogLevel=debug for debug logging
 */
public class KucoinPrivateWSClientTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(KucoinPrivateWSClientTest.class);

    private static KucoinRestClient kucoinRestClient;
    private static KucoinPrivateWSClient kucoinPrivateWSClient;

    /*@BeforeClass
    public static void setupClass() throws Exception {
        KucoinClientBuilder builder = new KucoinClientBuilder().withBaseUrl("https://openapi-v2.kucoin.com")
                .withApiKey("6392f98ccc568b000128f309", "ed16b6fd-a272-44de-8348-2eff834cae21", "20221209");
        kucoinRestClient = builder.buildRestClient();
        kucoinPrivateWSClient = builder.buildPrivateWSClient();
    }*/

    /*@AfterClass
    public static void afterClass() throws Exception {
        kucoinPrivateWSClient.close();
    }*/

    @Test
    public void onOrderActivate() throws Exception {
        AtomicReference<OrderActivateEvent> event = new AtomicReference<>();
        CountDownLatch gotEvent = new CountDownLatch(1);

        kucoinPrivateWSClient.onOrderActivate(response -> {
            LOGGER.info("Got response");
            event.set(response.getData());
            kucoinPrivateWSClient.unsubscribe(PrivateChannelEnum.ORDER, "ETH-BTC", "KCS-BTC");
            gotEvent.countDown();
        }, "ETH-BTC", "KCS-BTC");

        Thread.sleep(1000);

        new Thread(() -> {
            while (event.get() == null) {
                try {
                    placeOrderAndCancelOrder();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        LOGGER.info("Waiting...");
        assertTrue(gotEvent.await(20, TimeUnit.SECONDS));
        System.out.println(event.get());
    }

    @Test
    public void onOrderChange() throws Exception {
        AtomicReference<OrderChangeEvent> event = new AtomicReference<>();
        CountDownLatch gotEvent = new CountDownLatch(1);

        kucoinPrivateWSClient.onOrderChange(response -> {
            LOGGER.info("Got response");
            event.set(response.getData());
            kucoinPrivateWSClient.unsubscribe(PrivateChannelEnum.ORDER_CHANGE, "ETH-BTC", "BTC-USDT");
            gotEvent.countDown();
        }, "ETH-BTC", "BTC-USDT");

        Thread.sleep(1000);

        new Thread(() -> {
            while (event.get() == null) {
                try {
                    placeOrderAndCancelOrder();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        LOGGER.info("Waiting...");
        assertTrue(gotEvent.await(20, TimeUnit.SECONDS));
        System.out.println(event.get());
    }

    @Test
    public void onAccountBalance() throws Exception {
        AtomicReference<AccountChangeEvent> event = new AtomicReference<>();
        CountDownLatch gotEvent = new CountDownLatch(1);

        kucoinPrivateWSClient.onAccountBalance(response -> {
            LOGGER.info("Got response");
            event.set(response.getData());
            kucoinPrivateWSClient.unsubscribe(PrivateChannelEnum.ACCOUNT);
            gotEvent.countDown();
        });

        Thread.sleep(1000);

        new Thread(() -> {
            while (event.get() == null) {
                try {
                    innerTransfer();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        LOGGER.info("Waiting...");
        assertTrue(gotEvent.await(20, TimeUnit.SECONDS));
        System.out.println(event.get());
    }

    @Test
    public void ping() throws Exception {
        KucoinClientBuilder kucoinClientBuilder = new KucoinClientBuilder().withBaseUrl("https://openapi-v2.kucoin.com")
                .withApiKey("6392f98ccc568b000128f309", "ed16b6fd-a272-44de-8348-2eff834cae21", "20221209");
        KucoinPrivateWSClient kucoinPrivateWSClient = kucoinClientBuilder.buildPrivateWSClient();
        while (true) {
            String requestId = System.currentTimeMillis() + "";
            String ping = kucoinPrivateWSClient.ping(requestId);
            System.out.println(ping);
            assertThat(ping, Is.is(requestId));
            Thread.sleep(3000);
        }
    }

    @Test
    public void onAdvancedOrder() throws Exception {
        AtomicReference<AdvancedOrderEvent> event = new AtomicReference<>();
        CountDownLatch gotEvent = new CountDownLatch(1);

        kucoinPrivateWSClient.onAdvancedOrder(response -> {
            LOGGER.info("Got response {}", response);
            event.set(response.getData());
            kucoinPrivateWSClient.unsubscribe(PrivateChannelEnum.ADVANCED_ORDER, "ETH-BTC");
            gotEvent.countDown();
        }, "ETH-BTC");

        Thread.sleep(1000);

        new Thread(() -> {
            while (event.get() == null) {
                try {
                    StopOrderCreateRequest request = StopOrderCreateRequest.builder()
                            .price(BigDecimal.valueOf(0.0001)).size(BigDecimal.ONE).side("buy")
                            .stop("loss").stopPrice(BigDecimal.valueOf(0.0002))
                            .symbol("ETH-BTC").type("limit").clientOid(UUID.randomUUID().toString()).build();
                    OrderCreateResponse stopOrder = kucoinRestClient.stopOrderAPI().createStopOrder(request);

                    kucoinRestClient.stopOrderAPI().cancelStopOrder(stopOrder.getOrderId());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        LOGGER.info("Waiting...");
        assertTrue(gotEvent.await(20, TimeUnit.SECONDS));
        System.out.println(event.get());
    }

    private void placeOrderAndCancelOrder() throws IOException {
        OrderCreateApiRequest request = OrderCreateApiRequest.builder()
                .price(BigDecimal.valueOf(0.000001)).size(BigDecimal.ONE).side("buy")
                .symbol("ETH-BTC").type("limit").clientOid(UUID.randomUUID().toString()).build();
        OrderCreateResponse order = kucoinRestClient.orderAPI().createOrder(request);
        kucoinRestClient.orderAPI().cancelOrder(order.getOrderId());
    }

    private void innerTransfer() throws IOException {
        List<AccountBalancesResponse> accountBalancesResponses = kucoinRestClient.accountAPI().listAccounts("BTC", null);
        assertThat(accountBalancesResponses.size(), Is.is(2));
        Optional<AccountBalancesResponse> main = accountBalancesResponses.stream()
                .filter(accountBalancesResponse -> accountBalancesResponse.getType().equals("main")).findFirst();
        Optional<AccountBalancesResponse> trade = accountBalancesResponses.stream()
                .filter(accountBalancesResponse -> accountBalancesResponse.getType().equals("trade")).findFirst();
        String mainAccountId = main.get().getId();
        String tradeAccountId = trade.get().getId();
        kucoinRestClient.accountAPI().innerTransfer(String.valueOf(System.currentTimeMillis()), mainAccountId, BigDecimal.valueOf(0.00000001), tradeAccountId);
        kucoinRestClient.accountAPI().innerTransfer(String.valueOf(System.currentTimeMillis()), tradeAccountId, BigDecimal.valueOf(0.00000001), mainAccountId);
    }

}