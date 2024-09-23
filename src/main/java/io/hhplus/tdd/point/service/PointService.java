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
        // 유저 포인트 조회
        UserPoint userPoint = userPointTable.selectById(id);
        if (userPoint == null || userPoint.id() == 0) {
            throw new IllegalArgumentException("아이디가 존재하지 않습니다.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        return userPointTable.insertOrUpdate(id, amount);
    }
}
