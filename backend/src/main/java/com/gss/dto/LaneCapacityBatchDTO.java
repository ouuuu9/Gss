package com.gss.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 按日期批量生成/调整车道容量（M3）：整批成功或整批回滚。
 */
@Data
public class LaneCapacityBatchDTO {

    @NotNull(message = "日期不能为空")
    private LocalDate date;

    @Valid
    @NotNull(message = "容量明细不能为空")
    private List<Cell> cells;

    @Data
    public static class Cell {

        @NotNull(message = "车道不能为空")
        private Long laneId;

        @NotBlank(message = "时段不能为空")
        private String timeSlot;

        @NotNull(message = "容量不能为空")
        @Min(value = 0, message = "容量不能为负数")
        private Integer capacity;
    }
}
