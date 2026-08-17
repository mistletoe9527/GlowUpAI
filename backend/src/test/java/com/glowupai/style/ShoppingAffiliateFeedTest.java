package com.glowupai.style;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Affiliate feed 商品推荐测试。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:glowup-feed-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "glowup.upload-dir=target/test-uploads-feed",
        "glowup.shopping.feed.path=src/test/resources/affiliate-feed-test.csv"
})
@AutoConfigureMockMvc
class ShoppingAffiliateFeedTest {

    /**
     * Mock MVC 客户端。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证配置 feed 后购物推荐优先来自 feed。
     *
     * @throws Exception 请求异常
     */
    @Test
    void shoppingRecommendationsUseConfiguredAffiliateFeed() throws Exception {
        startSubscriptionFixture("feed-user-1", "Monthly");

        mockMvc.perform(get("/api/shopping/recommendations")
                        .param("userId", "feed-user-1")
                        .param("occasion", "Date"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].brand", is("Feed Brand")))
                .andExpect(jsonPath("$.data[0].name", is("Feed Date Dress")))
                .andExpect(jsonPath("$.data[0].buyUrl", is("https://shop.example/date-dress")))
                .andExpect(jsonPath("$.data[2].name", is("Feed Universal Heel")));
    }

    /**
     * 创建测试订阅记录。
     *
     * @param userId 用户 ID
     * @param plan 套餐标签
     * @throws Exception 请求异常
     */
    private void startSubscriptionFixture(String userId, String plan) throws Exception {
        mockMvc.perform(post("/api/subscriptions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "plan": "%s"
                                }
                                """.formatted(userId, plan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));
    }
}
