package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PointServiceConcurrencyTest {

    private final PointService pointService;
    private final UserPointTable userPointTable;

    @Autowired
    public PointServiceConcurrencyTest(PointService pointService, UserPointTable userPointTable) {
        this.pointService = pointService;
        this.userPointTable = userPointTable;
    }

    @Test
    @DisplayName("포인트를 동시에 여러번 충전을 하여도 순차적으로 충전이된다.")
    void testSequentialPointCharging() throws InterruptedException {
        // Given
        Long userId = 1L;
        Long amount = 10L;
        int numberOfThreads = 100; // 동시에 실행할 스레드 수
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // When 
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    pointService.charge(userId, amount); // 포인트 충전
                } finally {
                    latch.countDown(); // 스레드가 완료될 때마다 카운트 다운
                }
            });
        }
        latch.await();  // 모든 스레드가 완료될 때까지 충분히 대기
        executor.shutdown(); // 더 이상 작업을 추가할 수 없으며, 모든 작업이 완료되면 ExecutorService를 종료

        // Then: 최종 포인트 검증
        UserPoint finalPoints = pointService.select(userId);
        assertThat(amount * numberOfThreads).isEqualTo(finalPoints.point());
    }

    @Test
    @DisplayName("포인트를 동시에 여러번 사용을 하여도 순차적으로 사용이된다.")
    void testSequentialPointUsage() throws InterruptedException {
        // Given
        Long userId = 1L;
        Long basePoint = 1000L;
        Long amount = 10L;
        // Base 포인트를 유저에 저장 후 순차적으로 amount 수치만큼 사용
        userPointTable.insertOrUpdate(userId,basePoint);

        int numberOfThreads = 100; // 동시에 실행할 스레드 수
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // When
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    pointService.use(userId, amount); // 포인트 사용
                } finally {
                    latch.countDown(); // 스레드가 완료될 때마다 카운트 다운
                }
            });
        }
        latch.await();  // 모든 스레드가 완료될 때까지 충분히 대기
        executor.shutdown(); // 더 이상 작업을 추가할 수 없으며, 모든 작업이 완료되면 ExecutorService를 종료

        // Then: 최종 포인트 검증
        UserPoint finalPoints = pointService.select(userId);
        assertThat(basePoint-(amount * numberOfThreads)).isEqualTo(finalPoints.point());
    }
}
