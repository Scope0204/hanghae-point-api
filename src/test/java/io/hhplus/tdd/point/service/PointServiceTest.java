package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static io.hhplus.tdd.point.TransactionType.CHARGE;
import static io.hhplus.tdd.point.TransactionType.USE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        // Mockito 애노테이션 초기화
        MockitoAnnotations.openMocks(this);
    }

    // ============================ 포인트 충전/사용 내역 조회 테스트 ============================
    @Test
    @DisplayName("포인트 충전/사용 내역 조회")
    void selectPointHistory(){
        Long userId = 1L;
        PointHistory history1 = new PointHistory(1L, userId, 100L, CHARGE, System.currentTimeMillis());
        PointHistory history2 = new PointHistory(2L, userId, 200L, USE, System.currentTimeMillis());
        List<PointHistory> exampleHistory = List.of(history1, history2);

        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(exampleHistory);

        List<PointHistory> pointHistory = pointService.selectPointHistory(userId);
        assertThat(pointHistory).isEqualTo(exampleHistory);
    }

    @Test
    @DisplayName("포인트 사용,충전 후 히스토리 이력 조회")
    void selectHistoryAfterPointUsageTest(){
        Long userId = 1L;
        Long baseAmount  = 100L;
        // 해당 id에 기본값 포인트 설정
        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, baseAmount, System.currentTimeMillis()));

        // 해당 id에 히스토리 이력 저장 설정
        when(pointHistoryTable.insert(eq(userId), anyLong(), any(), anyLong()))
                .thenAnswer(invocationOnMock -> {
                    long inUserId = invocationOnMock.getArgument(0);
                    long inAmount = invocationOnMock.getArgument(1);
                    TransactionType inType = invocationOnMock.getArgument(2);
                    long inUpdatedMills = System.currentTimeMillis();

                    return new PointHistory(1L, inUserId, inAmount, inType, inUpdatedMills);
                });

        //포인트 사용 히스토리 호출 횟수 및 값 검증
        pointService.use(userId, 100L);
        verify(pointHistoryTable, times(1)).insert(eq(userId), eq(100L), eq(TransactionType.USE), anyLong());

        //포인트 충전 히스토리 호출 횟수 및 값 검증
        pointService.charge(userId, 100L);
        verify(pointHistoryTable, times(1)).insert(eq(userId), eq(100L), eq(TransactionType.CHARGE), anyLong());
        pointService.charge(userId, 200L);
        verify(pointHistoryTable, times(1)).insert(eq(userId), eq(200L), eq(TransactionType.CHARGE), anyLong());

        // 연속 충전 시 횟수만 검증
        pointService.charge(userId, 300L);
        pointService.charge(userId, 400L);
        // 이전 충전 과정에서 히스토리 호출 횟수까지 검증 결과에 포함
        verify(pointHistoryTable, times(4)).insert(eq(userId), anyLong(), eq(TransactionType.CHARGE), anyLong());
    }

    // ============================ 포인트 사용 테스트 ============================
    @Test
    @DisplayName("포인트 사용 금액이 0원보다 작은 경우")
    void usePointNegativeAmount(){
        Long id = 1L;
        Long amount = 0L;

        //0원 이하를 사용하는 경우 예외 발생
        assertThatThrownBy(() -> pointService.use(id, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("포인트 사용 금액이 현재 보유한 포인트 보다 많은 경우")
    void usePointExceedsBalance(){
        Long id = 1L;
        Long baseAmount = 50L;
        // 해당 id에 기본값 포인트 설정
        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, baseAmount, System.currentTimeMillis()));

        Long useAmount = 100L;
        // 현재 보유한 포인트 양보다 많은 포인트를 사용하는 경우 예외발생
        assertThatThrownBy(() -> pointService.use(id, useAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용하고자하는 포인트가 보유한 포인트보다 많습니다.");
    }

    @Test
    @DisplayName("포인트 사용 성공")
    void usePoint(){
        Long id = 1L;
        Long baseAmount = 50L;
        // 해당 id에 기본값 포인트 설정
        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, baseAmount, System.currentTimeMillis()));

        // 해당 id에 amount 만큼 Point 업데이트
        when(userPointTable.insertOrUpdate(anyLong(), anyLong())).thenAnswer(invocationOnMock -> {
            long invocationID = invocationOnMock.getArgument(0);
            long invocationAmount = invocationOnMock.getArgument(1);
            return new UserPoint(invocationID, invocationAmount, System.currentTimeMillis());
        });

        Long useAmount = 20L;
        UserPoint userPoint = pointService.use(id, useAmount);

        // 현재 보유한 포인트가 기존 포인트에서 사용한 포인트 양을 차감한 수치인지 검증
        assertThat(userPoint.point()).isEqualTo(baseAmount - useAmount);
    }

    // ============================ 포인트 조회 테스트 ============================
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

    // ============================ 포인트 충전 테스트 ============================
    @Test
    @DisplayName("최대 금액 이상의 금액을 충전하는 경우")
    void chargeExceedMaxAmountTest() {
        Long id = 1L;
        long baseAmount = 50L;
        // 해당 id에 기본값 포인트 설정
        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, baseAmount, System.currentTimeMillis()));

        //최대 금액 이상의 포인트를 충전하는 경우 에러 발생
        assertThatThrownBy(() -> pointService.charge(id, 10000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 잔고 이상으로 충전할 수 없습니다.");
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