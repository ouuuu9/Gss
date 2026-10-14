package com.gss.common;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 简易 Excel 导出工具
 */
public class ExcelExporter {

    private final Workbook workbook;
    private final Sheet sheet;
    private final CellStyle headerStyle;

    public ExcelExporter(String sheetName) {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet(sheetName);
        // 表头样式
        headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
    }

    /**
     * 写入表头
     * @param headers 表头列名数组
     */
    public void writeHeader(String[] headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 5000);
        }
    }

    /**
     * 写入数据行
     * @param rows 数据行列表，每行是一个 Object 数组
     */
    public void writeRows(List<Object[]> rows) {
        for (int i = 0; i < rows.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Object[] values = rows.get(i);
            for (int j = 0; j < values.length; j++) {
                Cell cell = row.createCell(j);
                if (values[j] != null) {
                    cell.setCellValue(values[j].toString());
                }
            }
        }
    }

    /**
     * 输出到 HTTP 响应，触发浏览器下载
     */
    public void writeToResponse(HttpServletResponse response, String fileName) throws IOException {
        String encodedName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 一站式导出：构建工作簿 → 写表头/数据 → 文件名追加时间戳后输出到响应
     * @param fileNamePrefix 文件名前缀，自动追加 _yyyyMMddHHmmss
     */
    public static void export(String sheetName, String[] headers, List<Object[]> data,
                              String fileNamePrefix, HttpServletResponse response) throws IOException {
        ExcelExporter exporter = new ExcelExporter(sheetName);
        exporter.writeHeader(headers);
        exporter.writeRows(data);
        String fileName = fileNamePrefix + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        exporter.writeToResponse(response, fileName);
    }

    /* ==================== 导出字典翻译（英文码 → 中文文案） ==================== */

    /** 业务类型 */
    public static String bizTypeLabel(String bizType) {
        if (bizType == null) {
            return null;
        }
        return switch (bizType) {
            case "PI-F" -> "提重";
            case "PI-E" -> "提空";
            case "GR-E" -> "还空";
            case "GR-F" -> "还重";
            default -> bizType;
        };
    }

    /** 单证状态 */
    public static String eirStatusLabel(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "CREATED" -> "已录入";
            case "DISPATCHED" -> "已派单";
            case "APPOINTED" -> "已预约";
            case "GATE_IN" -> "已进闸";
            case "GATE_OUT" -> "已出闸";
            case "DONE" -> "已完成";
            case "CANCELLED" -> "已作废";
            default -> status;
        };
    }

    /** 预约状态 */
    public static String apptStatusLabel(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "ACTIVE" -> "有效";
            case "CANCELLED" -> "已取消";
            case "EXPIRED" -> "已过期";
            default -> status;
        };
    }

    /** 缴费状态：0 未缴 / 1 已缴 */
    public static String payStatusLabel(Integer payStatus) {
        if (payStatus == null) {
            return null;
        }
        return payStatus == 1 ? "已缴" : "未缴";
    }
}
