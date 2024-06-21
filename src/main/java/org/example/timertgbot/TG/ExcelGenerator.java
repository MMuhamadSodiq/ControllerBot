package org.example.timertgbot.TG;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.timertgbot.Entity.Credit;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelGenerator {

    public static String generateExcel(List<Credit> credits, String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Credits");

        // Create Header Row with style
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Клиент", "Сумма", "Персонал", "Дата Окончания", "Вознаграждение", "Компенсация", "Статус"};

        // Create a cell style for headers
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Apply header style and set column widths
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Define formatters
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        // Create a cell style for regular cells
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setWrapText(true);
        cellStyle.setAlignment(HorizontalAlignment.LEFT);
        cellStyle.setVerticalAlignment(VerticalAlignment.TOP);

        // Create cell styles for status-based background colors in "Актив" column
        CellStyle activeStyle = workbook.createCellStyle();
        activeStyle.cloneStyleFrom(cellStyle);
        activeStyle.setFillForegroundColor(IndexedColors.PINK.getIndex());
        activeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle expiredStyle = workbook.createCellStyle();
        activeStyle.cloneStyleFrom(cellStyle);
        activeStyle.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        activeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle completedStyle = workbook.createCellStyle();
        completedStyle.cloneStyleFrom(cellStyle);
        completedStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        completedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Populate Data Rows
        int rowNum = 1;
        DecimalFormat formatter = new DecimalFormat("#,###");
        for (Credit credit : credits) {
            String formattedAmount = formatter.format(credit.getAmount()).replace(',', '.');
            String formattedReward = formatter.format(credit.getReward()).replace(',', '.');
            String formattedComp = formatter.format(credit.getCompensation()).replace(',', '.');

            String endDateString = credit.getEndDate().toString();
            LocalDateTime parsedDate = LocalDateTime.parse(endDateString, inputFormatter);

            // Format the parsed date to the desired format
            String formattedDate = parsedDate.format(outputFormatter);

            Row row = sheet.createRow(rowNum++);
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(credit.getClient_name());
            cell0.setCellStyle(cellStyle);

            Cell cell1 = row.createCell(1);
            cell1.setCellValue(credit.getAmount());
            cell1.setCellStyle(cellStyle);

            Cell cell2 = row.createCell(2);
            cell2.setCellValue(credit.getStaff().getName() + (credit.getStaff().getStaffStatus().equals(StaffStatus.ARCHIVE)?" [Персонал был удален]":""));
            cell2.setCellStyle(cellStyle);

            Cell cell3 = row.createCell(3);
            cell3.setCellValue(formattedDate);
            cell3.setCellStyle(cellStyle);

            Cell cell4 = row.createCell(4);
            cell4.setCellValue(credit.getReward());
            cell4.setCellStyle(cellStyle);

            Cell cell5 = row.createCell(5);
            cell5.setCellValue(credit.getCompensation());
            cell5.setCellStyle(cellStyle);

            // Apply status-based cell style only to "Актив" column
            Cell cell6 = row.createCell(6);
            cell6.setCellValue(credit.getCreditStatus()==CreditStatus.ACTIVE?"Активно":credit.getCreditStatus()==CreditStatus.EXPIRED?"Просрочено": "Завершено");
            CellStyle statusStyle = credit.getCreditStatus() == CreditStatus.ACTIVE ? activeStyle :  credit.getCreditStatus()== CreditStatus.EXPIRED? expiredStyle : completedStyle;
            cell6.setCellStyle(statusStyle);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }

        workbook.close();
        return filePath;
    }
    public static String generateExcelStaff(List<Credit> credits, String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Credits");

        // Create Header Row with style
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Клиент", "Сумма", "Дата Окончания", "Вознаграждение", "Компенсация", "Статус"};

        // Create a cell style for headers
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Apply header style and set column widths
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Define formatters
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        // Create a cell style for regular cells
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setWrapText(true);
        cellStyle.setAlignment(HorizontalAlignment.LEFT);
        cellStyle.setVerticalAlignment(VerticalAlignment.TOP);

        // Create cell styles for status-based background colors in "Актив" column
        CellStyle activeStyle = workbook.createCellStyle();
        activeStyle.cloneStyleFrom(cellStyle);
        activeStyle.setFillForegroundColor(IndexedColors.PINK.getIndex());
        activeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle completedStyle = workbook.createCellStyle();
        completedStyle.cloneStyleFrom(cellStyle);
        completedStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        completedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle expiredStyle = workbook.createCellStyle();
        activeStyle.cloneStyleFrom(cellStyle);
        activeStyle.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        activeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Populate Data Rows
        int rowNum = 1;
        DecimalFormat formatter = new DecimalFormat("#,###");
        for (Credit credit : credits) {
            String formattedAmount = formatter.format(credit.getAmount()).replace(',', '.');
            String formattedReward = formatter.format(credit.getReward()).replace(',', '.');
            String formattedComp = formatter.format(credit.getCompensation()).replace(',', '.');

            String endDateString = credit.getEndDate().toString();
            LocalDateTime parsedDate = LocalDateTime.parse(endDateString, inputFormatter);

            // Format the parsed date to the desired format
            String formattedDate = parsedDate.format(outputFormatter);

            Row row = sheet.createRow(rowNum++);
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(credit.getClient_name());
            cell0.setCellStyle(cellStyle);

            Cell cell1 = row.createCell(1);
            cell1.setCellValue(credit.getAmount());
            cell1.setCellStyle(cellStyle);


            Cell cell3 = row.createCell(2);
            cell3.setCellValue(formattedDate);
            cell3.setCellStyle(cellStyle);

            Cell cell4 = row.createCell(3);
            cell4.setCellValue(credit.getReward());
            cell4.setCellStyle(cellStyle);

            Cell cell5 = row.createCell(4);
            cell5.setCellValue(credit.getCompensation());
            cell5.setCellStyle(cellStyle);

            // Apply status-based cell style only to "Актив" column
            Cell cell6 = row.createCell(5);
            cell6.setCellValue(credit.getCreditStatus()==CreditStatus.ACTIVE?"Активно":credit.getCreditStatus()==CreditStatus.EXPIRED?"Просрочено": "Завершено");
            CellStyle statusStyle = credit.getCreditStatus() == CreditStatus.ACTIVE ? activeStyle :  credit.getCreditStatus()== CreditStatus.EXPIRED? expiredStyle : completedStyle;
            cell6.setCellStyle(statusStyle);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }

        workbook.close();
        return filePath;
    }
}
