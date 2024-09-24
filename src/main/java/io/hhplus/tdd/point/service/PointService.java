package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.UserPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.hhplus.tdd.point.TransactionType.CHARGE;
import static io.hhplus.tdd.point.TransactionType.USE;

@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    @Autowired
    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint charge(long id, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        // 기존 포인트 양 조회
        Long baseAmount = userPointTable.selectById(id).point();
        long updateAmount = baseAmount + amount;

        //포인트 충전 내역 저장
        PointHistory insertPoint = pointHistoryTable.insert(id, amount, CHARGE, System.currentTimeMillis());

        return userPointTable.insertOrUpdate(id, updateAmount);
    }

    public UserPoint select(long id) {
        return userPointTable.selectById(id);
    }

    public UserPoint use(long id, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }
        // 기존 포인트 양 조회
        Long baseAmount = userPointTable.selectById(id).point();
        if(baseAmount < amount) {
            throw new IllegalArgumentException("사용하고자하는 포인트가 보유한 포인트보다 많습니다.");
        }
        long updateAmount = baseAmount - amount;

        //포인트 충전 내역 저장
        PointHistory insertPoint = pointHistoryTable.insert(id, amount, USE, System.currentTimeMillis());

        return userPointTable.insertOrUpdate(id, updateAmount);
    }

    public List<PointHistory> selectPointHistory(Long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }
}

