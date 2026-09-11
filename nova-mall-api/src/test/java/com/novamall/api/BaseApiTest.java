package com.novamall.api;

import com.novamall.api.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 接口测试基类：随机端口启动完整应用上下文，提供注册/登录/带 token 请求等公共方法。
 * 测试使用独立库 novamall_db_test 与 Redis database 1，与生产环境隔离。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseApiTest {

    protected static final long TEST_GOODS_ID = 10700L;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    /** 每次运行生成不重复的手机号，保证测试可重复执行 */
    protected String randomPhone() {
        return "138" + String.format("%08d", ThreadLocalRandom.current().nextInt(100000000));
    }

    /** 注册并登录，返回 token */
    @SuppressWarnings("unchecked")
    protected String registerAndLogin(String phone, String password) {
        Map<String, Object> registerParam = new HashMap<>();
        registerParam.put("loginName", phone);
        registerParam.put("password", password);
        Map<String, Object> registerResult = postForMap("/api/v1/user/register", registerParam, null);
        assertResultSuccess(registerResult, "注册");

        Map<String, Object> loginParam = new HashMap<>();
        loginParam.put("loginName", phone);
        loginParam.put("passwordMd5", MD5Util.MD5Encode(password, "UTF-8"));
        Map<String, Object> loginResult = postForMap("/api/v1/user/login", loginParam, null);
        assertResultSuccess(loginResult, "登录");
        return (String) loginResult.get("data");
    }

    protected void assertResultSuccess(Map<String, Object> result, String step) {
        if (result == null || !Integer.valueOf(200).equals(result.get("resultCode"))) {
            throw new IllegalStateException(step + "失败，响应：" + result);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> postForMap(String url, Object body, String token) {
        return restTemplate.postForObject(url, new HttpEntity<>(body, headers(token)), Map.class);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getForMap(String url, String token) {
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers(token)), Map.class);
        return response.getBody();
    }

    protected HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("token", token);
        }
        return headers;
    }

    /** 重置测试商品库存并清理详情缓存，保证用例间互不影响 */
    protected void resetGoods(long goodsId, int stockNum) {
        jdbcTemplate.update("update tb_novamall_goods_info set stock_num = ? where goods_id = ?", stockNum, goodsId);
        stringRedisTemplate.delete("novamall:goods:detail:" + goodsId);
    }

    protected int queryStock(long goodsId) {
        Integer stock = jdbcTemplate.queryForObject(
                "select stock_num from tb_novamall_goods_info where goods_id = ?", Integer.class, goodsId);
        return stock == null ? -1 : stock;
    }

    /** 走完 加购物车→建地址→下单 全链路，返回订单号 */
    @SuppressWarnings("unchecked")
    protected String createOrder(String token, long goodsId, int goodsCount) {
        Map<String, Object> cartParam = new HashMap<>();
        cartParam.put("goodsId", goodsId);
        cartParam.put("goodsCount", goodsCount);
        assertResultSuccess(postForMap("/api/v1/shop-cart", cartParam, token), "加购物车");

        Map<String, Object> addressParam = new HashMap<>();
        addressParam.put("userName", "测试用户");
        addressParam.put("userPhone", "13800000000");
        addressParam.put("provinceName", "浙江省");
        addressParam.put("cityName", "杭州市");
        addressParam.put("regionName", "西湖区");
        addressParam.put("detailAddress", "测试街道1号");
        addressParam.put("defaultFlag", 1);
        assertResultSuccess(postForMap("/api/v1/address", addressParam, token), "创建地址");

        Map<String, Object> addressList = getForMap("/api/v1/address", token);
        assertResultSuccess(addressList, "查询地址列表");
        List<Map<String, Object>> addresses = (List<Map<String, Object>>) addressList.get("data");
        long addressId = ((Number) addresses.get(0).get("addressId")).longValue();

        Map<String, Object> cartList = getForMap("/api/v1/shop-cart", token);
        assertResultSuccess(cartList, "查询购物车");
        List<Map<String, Object>> cartItems = (List<Map<String, Object>>) cartList.get("data");
        long cartItemId = ((Number) cartItems.get(0).get("cartItemId")).longValue();

        Map<String, Object> orderParam = new HashMap<>();
        orderParam.put("cartItemIds", new long[]{cartItemId});
        orderParam.put("addressId", addressId);
        Map<String, Object> orderResult = postForMap("/api/v1/saveOrder", orderParam, token);
        return (String) orderResult.get("data");
    }

    /** 为用户预先准备好 购物车+地址，返回下单参数（用于并发下单场景） */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> prepareOrderParam(String token, long goodsId, int goodsCount) {
        Map<String, Object> cartParam = new HashMap<>();
        cartParam.put("goodsId", goodsId);
        cartParam.put("goodsCount", goodsCount);
        assertResultSuccess(postForMap("/api/v1/shop-cart", cartParam, token), "加购物车");

        Map<String, Object> addressParam = new HashMap<>();
        addressParam.put("userName", "测试用户");
        addressParam.put("userPhone", "13800000000");
        addressParam.put("provinceName", "浙江省");
        addressParam.put("cityName", "杭州市");
        addressParam.put("regionName", "西湖区");
        addressParam.put("detailAddress", "测试街道1号");
        addressParam.put("defaultFlag", 1);
        assertResultSuccess(postForMap("/api/v1/address", addressParam, token), "创建地址");

        Map<String, Object> addressList = getForMap("/api/v1/address", token);
        List<Map<String, Object>> addresses = (List<Map<String, Object>>) addressList.get("data");
        long addressId = ((Number) addresses.get(0).get("addressId")).longValue();

        Map<String, Object> cartList = getForMap("/api/v1/shop-cart", token);
        List<Map<String, Object>> cartItems = (List<Map<String, Object>>) cartList.get("data");
        long cartItemId = ((Number) cartItems.get(0).get("cartItemId")).longValue();

        Map<String, Object> orderParam = new HashMap<>();
        orderParam.put("cartItemIds", new long[]{cartItemId});
        orderParam.put("addressId", addressId);
        return orderParam;
    }
}
