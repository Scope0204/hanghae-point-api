package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private UserPointTable userPointTable;

    @BeforeEach
    void setUp() {
        // Mockito 애노테이션 초기화
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("포인트를 조회하는 경우")
    void selectPointTest(){
        Long id = 1L;
        Long amount = 100L;
        // 해당 id에 기본값 포인트 설정
        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, amount, System.currentTimeMillis()));

        // 유저 포인트 정보에 담긴 포인트 양 검증
        UserPoint userPoint = pointService.select(id);
        assertThat(userPoint.point()).isEqualTo(amount);
    }

    @Test
    @DisplayName("0원 이하의 금액을 충전하는 경우")
    void chargeInValidAmountTest() {
        Long id = 1L;
        // 해당 id의 임의의 포인트 설정
        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, anyLong(), System.currentTimeMillis()));

        //0원 이하를 충전하는 경우 예외 발생
        assertThatThrownBy(() -> pointService.charge(id, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("유효한 금액을 충전하려고 하는 경우")
    void chargeValidAmountTest() {
        long id = 1L;
        long baseAmount = 50L;
        // 해당 id에 기본값 포인트 설정
        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, baseAmount, System.currentTimeMillis()));

        // 해당 id에 amount 만큼 Point 업데이트
        when(userPointTable.insertOrUpdate(anyLong(), anyLong())).thenAnswer(invocationOnMock -> {
            long invocationID = invocationOnMock.getArgument(0);
            long invocationAmount = invocationOnMock.getArgument(1);
            return new UserPoint(invocationID, invocationAmount, System.currentTimeMillis());
        });

        long chargeAmount = 100L;
        UserPoint userPoint = pointService.charge(id, chargeAmount);

        // 현재 포인트 값이 기존 포인트와 충전 포인트의 합과 일치하는지 검증
        assertThat(userPoint.point()).isEqualTo(baseAmount + chargeAmount);
    }
}