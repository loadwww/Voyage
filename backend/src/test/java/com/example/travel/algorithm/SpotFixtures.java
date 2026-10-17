package com.example.travel.algorithm;

import com.example.travel.model.ScenicSpot;

import java.util.List;

/**
 * 测试用景点构造器，集中管理 ScenicSpot 22 个参数的默认值。
 */
final class SpotFixtures {
    private SpotFixtures() {
    }

    static ScenicSpot spot(int id) {
        return spot(id, 50);
    }

    static ScenicSpot spot(int id, int popularity) {
        return new ScenicSpot(
                id,
                "景点" + id,
                "成都",
                "武侯区",
                "测试地址" + id,
                104.0 + id * 0.01,
                30.6 + id * 0.01,
                "历史",
                60,
                2.0,
                "09:00",
                "18:00",
                popularity,
                List.of("历史"),
                "测试景点",
                "https://example.com/" + id + ".jpg",
                false,
                0.3,
                0.3,
                0.4,
                0.2,
                0.5
        );
    }
}
