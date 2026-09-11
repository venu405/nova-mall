package com.novamall.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 用户链路：注册 → 登录 → 带 token 访问鉴权接口；未带 token 返回 416
 */
class UserApiTest extends BaseApiTest {

    @Test
    void registerLoginAndAccessWithToken() {
        String phone = randomPhone();
        String token = registerAndLogin(phone, "123456");
        assertNotNull(token);
        assertEquals(32, token.length());

        Map<String, Object> userInfo = getForMap("/api/v1/user/info", token);
        assertEquals(200, userInfo.get("resultCode"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) userInfo.get("data");
        assertNotNull(data);
        assertEquals(phone, data.get("loginName"));
    }

    @Test
    void accessProtectedApiWithoutTokenReturns416() {
        Map<String, Object> result = getForMap("/api/v1/user/info", null);
        assertEquals(416, result.get("resultCode"));
    }
}
