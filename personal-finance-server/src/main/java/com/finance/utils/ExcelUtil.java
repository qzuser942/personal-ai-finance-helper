package com.finance.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
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
 *
 * @author 胡宪棋
 */
public class ExcelUtil {

    /**
     * 导出Excel到HTTP响应流
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
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fullFileName = fileName + "_" + timestamp + ".xlsx";
        String encodedFileName = URLEncoder.encode(fullFileName, StandardCharsets.UTF_8).replace("+", "%20");

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setCharacterEncoding("UTF-8");

        // 写入响应流
        try (OutputStream os = response.getOutputStream()) {
            workbook.write(os);
            os.flush();
        } finally {
            workbook.close();
        }
    }
}
