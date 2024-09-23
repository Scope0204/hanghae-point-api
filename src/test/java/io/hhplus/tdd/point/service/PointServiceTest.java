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
import static org.junit.jupiter.api.Assertions.*;
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
    @DisplayName("ID 없는 경우 예외 반환")
    void isEmptyIdTest() {
        when(userPointTable.selectById(anyLong())).thenReturn(null);
        assertThrows(IllegalArgumentException.class,
                () -> pointService.charge(1L, 100L), "아이디가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("해당 유저 ID가 유효하지 않는 경우 예외 반환")
    void isInValidIdTest() {
        Long id = 123L;
        when(userPointTable.selectById(id)).thenReturn(UserPoint.empty(id));
        assertThrows(IllegalArgumentException.class,
                () -> pointService.charge(id, anyLong()), "아이디가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("유저 ID는 유효하지만, 0원 이하의 금액을 충전하는 경우 예외 반환")
    void chargeInValidAmountTest() {
        Long id = 1L;
        // 해당 id의 임의의 포인트를 가진 결과 생성
        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, anyLong(), System.currentTimeMillis()));
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

        // 기존 사용자 포인트 + 포인트 충전
        when(userPointTable.insertOrUpdate(anyLong(), anyLong())).thenAnswer(invocationOnMock -> {
            long invocationID = invocationOnMock.getArgument(0);
            long invocationAmount = invocationOnMock.getArgument(1);
            long updatedAmount = userPointTable.selectById(invocationID).point() + invocationAmount;
            return new UserPoint(invocationID, updatedAmount, System.currentTimeMillis());
        });

        long chargeAmount = 100L;
        UserPoint userPoint = pointService.charge(id, chargeAmount);

        // 현재 포인트 값이 기존 포인트와 충전 포인트의 합과 일치하는지 검증
        assertThat(userPoint.point()).isEqualTo(baseAmount + chargeAmount);
    }
}