package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PointService {

    private final UserPointTable userPointTable;

    @Autowired
    public PointService(UserPointTable userPointTable) {
        this.userPointTable = userPointTable;
    }

    public UserPoint charge(long id, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        // 기존 포인트 양 조회
        Long baseAmount = userPointTable.selectById(id).point();
        return userPointTable.insertOrUpdate(id, baseAmount+amount);
    }

    public UserPoint select(long id) {
        return userPointTable.selectById(id);
    }

    public UserPoint use(long id, long amount) {
        return null;
    }
}

