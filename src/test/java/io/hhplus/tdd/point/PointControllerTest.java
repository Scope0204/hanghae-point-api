package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PointController.class)
class PointControllerTest {

    @Autowired
    public MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PointService pointService;

    @Test
    @DisplayName("PATCH 포인트 조회 요청 성공")
    void usePointSuccessTest() throws Exception{
        // Given
        long id = 1L;
        long amount = 50L;
        UserPoint mockUserPoint = new UserPoint(id, amount, System.currentTimeMillis());
        when(pointService.use(anyLong(), anyLong())).thenReturn(mockUserPoint);
        // 사용 포인트를 json 으로 변환
        String jsonContent = objectMapper.writeValueAsString(amount);

        // When & Then : patch 요청 수행 후 응답 상태와 반환되는 값 검증
        mockMvc.perform(patch("/point/{id}/use", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk()) // response 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(amount));
    }

    @Test
    @DisplayName("GET 포인트 조회 요청 성공")
    void selectPointSuccessTest() throws Exception{
        // Given
        long id = 1L;
        long amount = 50L;
        UserPoint userPoint = new UserPoint(id, amount, System.currentTimeMillis());
        when(pointService.select(id)).thenReturn(userPoint);

        // When & Then : get 요청 수행 후 응답 상태 확인
        mockMvc.perform(get("/point/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH 포인트 충전 요청 요청 성공")
    void chargeSuccessTest() throws Exception{
        // Given
        Long id = 1L;
        Long amount = 50L;
        UserPoint mockUserPoint = new UserPoint(id, amount, System.currentTimeMillis());
        when(pointService.charge(anyLong(), anyLong())).thenReturn(mockUserPoint);
        // 충전 포인트를 json 으로 변환
        String jsonContent = objectMapper.writeValueAsString(amount);

        // When & Then : patch 요청 수행 후 응답 상태와 반환되는 값 검증
        mockMvc.perform(patch("/point/{id}/charge", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk()) // response 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(amount));
    }
}