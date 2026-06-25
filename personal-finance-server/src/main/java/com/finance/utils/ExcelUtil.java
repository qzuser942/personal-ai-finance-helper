package com.finance.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Excel 导出工具类 (Apache POI)
 * <p>提供两种导出方式：
 * <ul>
 *   <li>{@link #exportToResponse}：普通 XSSFWorkbook（适合 < 5w 行的导出）</li>
 *   <li>{@link #exportStreaming}：SXSSFWorkbook 流式分批（百万行也不 OOM）</li>
 * </ul>
 *
 * @author 胡宪棋
 */
public class ExcelUtil {

    /** 默认流式分批写出大小（行） */
    private static final int STREAM_BATCH_SIZE = 5000;

    /**
     * 普通导出（XSSFWorkbook） - 适合万行以内的导出
     */
    public static void exportToResponse(
            HttpServletResponse response,
            String fileName,
            String sheetName,
            String[] headers,
            List<Map<String, Object>> dataList) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);

        // 表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.autoSizeColumn(i);
        }

        // 填充数据
        if (dataList != null) {
            int rowIdx = 1;
            for (Map<String, Object> rowData : dataList) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = row.createCell(i);
                    Object value = rowData.get("col" + i);
                    if (value != null) {
                        cell.setCellValue(value.toString());
                    }
                }
            }
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 2048, 256 * 30));
        }

        // 设置响应头
        setExcelResponseHeaders(response, fileName);

        // 写入响应流
        try (OutputStream os = response.getOutputStream()) {
            workbook.write(os);
            os.flush();
        } finally {
            workbook.close();
        }
    }

    /**
     * 关键修复：流式分批导出（SXSSFWorkbook），避免一次性加载导致 OOM
     * <p>用法：传入 pageSupplier（按页提供数据）和 totalCount（总行数），每页处理后释放内存。
     *
     * @param response    HTTP响应
     * @param fileName    文件名（不含后缀）
     * @param sheetName   Sheet 名称
     * @param headers     表头
     * @param pageSupplier 分批数据提供器，参数为页码（从 0 开始），返回该批次的行数据（每行 Map col0..colN）
     * @param pageSize    每批大小（默认 5000）
     * @throws IOException IO 异常
     */
    public static void exportStreaming(
            HttpServletResponse response,
            String fileName,
            String sheetName,
            String[] headers,
            PageSupplier pageSupplier,
            int pageSize) throws IOException {

        if (pageSize <= 0) pageSize = STREAM_BATCH_SIZE;

        // 设置响应头（必须在创建 Workbook 之前）
        setExcelResponseHeaders(response, fileName);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(STREAM_BATCH_SIZE);
             OutputStream os = response.getOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // 表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            int page = 0;
            while (true) {
                List<Map<String, Object>> batch = pageSupplier.get(page);
                if (batch == null || batch.isEmpty()) break;
                for (Map<String, Object> rowData : batch) {
                    Row row = sheet.createRow(rowIdx++);
                    // 序号列（如果有 seqMap）
                    if (rowData.containsKey("__seq")) {
                        row.createCell(0).setCellValue(((Number) rowData.get("__seq")).doubleValue());
                        // 其余列从 col1 开始
                        for (int i = 1; i < headers.length; i++) {
                            Object v = rowData.get("col" + i);
                            if (v != null) row.createCell(i).setCellValue(v.toString());
                        }
                    } else {
                        for (int i = 0; i < headers.length; i++) {
                            Object v = rowData.get("col" + i);
                            if (v != null) row.createCell(i).setCellValue(v.toString());
                        }
                    }
                }
                if (batch.size() < pageSize) break;
                page++;
                // 刷新内存中的行
                if ((page % 10) == 0) {
                    workbook.setForceFormulaRecalculation(true);
                }
            }

            workbook.write(os);
            os.flush();
        }
    }

    /**
     * 设置 Excel 响应头
     */
    private static void setExcelResponseHeaders(HttpServletResponse response, String fileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fullFileName = fileName + "_" + timestamp + ".xlsx";
        String encodedFileName = URLEncoder.encode(fullFileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setCharacterEncoding("UTF-8");
    }

    /**
     * 分批数据提供器
     */
    @FunctionalInterface
    public interface PageSupplier {
        /**
         * @param page 页码（从 0 开始）
         * @return 该批次的数据列表；如果返回 null 或空列表表示结束
         */
        List<Map<String, Object>> get(int page);
    }
}
