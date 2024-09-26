package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.UserPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.hhplus.tdd.point.TransactionType.CHARGE;
import static io.hhplus.tdd.point.TransactionType.USE;

@Service
public class PointService {

    private static final Logger log = LoggerFactory.getLogger(PointService.class);

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Autowired
    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint charge(long id, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        // ReentrantLock을 사용하여 유저별 락 생성 및 반환
        Lock lock = locks.computeIfAbsent(id, k -> new ReentrantLock(true)); // 공정성 설정. 락을 대기한 순서대로 스레드가 락을 획득
        lock.lock();
        try{
            // 기존 포인트 양 조회
            Long baseAmount = userPointTable.selectById(id).point();
            long updateAmount = baseAmount + amount;

            // 포인트 충전 내역 저장
            PointHistory insertPoint = pointHistoryTable.insert(id, amount, CHARGE, System.currentTimeMillis());
            log.info("포인트 충전 완료 : " + insertPoint);
            log.info("남은 포인트 : " + updateAmount);

            return userPointTable.insertOrUpdate(id, updateAmount);
        } finally {
            lock.unlock();
        }
    }

    public UserPoint select(long id) {
        return userPointTable.selectById(id);
    }

    public UserPoint use(long id, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }
        // ReentrantLock을 사용하여 유저별 락 생성 및 반환
        Lock lock = locks.computeIfAbsent(id, k -> new ReentrantLock(true)); // 공정성 설정. 락을 대기한 순서대로 스레드가 락을 획득
        lock.lock();
        try{
            // 기존 포인트 양 조회
            Long baseAmount = userPointTable.selectById(id).point();
            if(baseAmount < amount) {
                throw new IllegalArgumentException("사용하고자하는 포인트가 보유한 포인트보다 많습니다.");
            }
            long updateAmount = baseAmount - amount;

            // 포인트 충전 내역 저장
            PointHistory insertPoint = pointHistoryTable.insert(id, amount, USE, System.currentTimeMillis());
            log.info("포인트 사용 완료 : " + insertPoint);
            log.info("남은 포인트 : " + updateAmount);

            return userPointTable.insertOrUpdate(id, updateAmount);
        } finally {
            lock.unlock();
        }
    }

    public List<PointHistory> selectPointHistory(Long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }
}

