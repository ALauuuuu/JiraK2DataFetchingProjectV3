package org.ha.ckh637.component;

import org.ha.ckh637.utils.TimeUtil;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
@Getter
// @NoArgsConstructor
@Setter
public class ExcelSheet {
    private XSSFWorkbook workbook = new XSSFWorkbook();
    private Sheet related = workbook.createSheet("Related_Urgent_Service_Specia");
    // private Sheet unrelated = workbook.createSheet("Unrelated_Urgent_Service_Special");
    private int relatedCount = 0;
    private int unrelatedCount = 0;

    private static final ExcelSheet instance = new ExcelSheet();

    private ExcelSheet() {
        initializeSheetHeaders(related);
        relatedCount++;
        // initializeSheetHeaders(unrelated);
        // unrelatedCount++;
    }

    public static ExcelSheet getInstance() {
        return instance;
    }

    private void initializeSheetHeaders(Sheet sheet) {
        Row row = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        String[] columnNames = {"Target date", "Target time", "Condition", "Hosp", "PPM#", "JIRA#", "Description"};
        int[] pixelWidths = {170, 95, 150, 233, 76, 123, 883};

        for (int i = 0; i < columnNames.length; i++) {
            int pixelWidth = pixelWidths[i];
            int widthUnits = (int) ((pixelWidth - 5) / 7.0 * 256);
            sheet.setColumnWidth(i, widthUnits);

            Cell cell = row.createCell(i);
            cell.setCellValue(columnNames[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    public void setExcelContent(PromoForm promoForm, boolean isUrgent, boolean isRelated) {
        Row row;
        Cell cell;
        Integer i=0;
        // further integration: Urgent promotion set to" style=\"background-color: lightgreen; font-weight: bold;\""
        if (isRelated) {
            XSSFCellStyle cellBasicStyle = workbook.createCellStyle();
            cellBasicStyle.setWrapText(true);
            cellBasicStyle.setAlignment(HorizontalAlignment.LEFT);
            cellBasicStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            row = related.createRow(relatedCount);
            cell = row.createCell(i++);

            String date = TimeUtil.dateDayOfWeekFormatter(promoForm.getTargetDate());
            String targetDateRegex = "(?i)\\b(saturday|sunday)\\b";
            Pattern targetDatePattern = Pattern.compile(targetDateRegex);
            Matcher targetDateMatcher = targetDatePattern.matcher(date.toLowerCase());
            boolean weekend = targetDateMatcher.find();

            // Check if the pattern is found in the input string
            XSSFCellStyle cellInRedStyle = this.workbook.createCellStyle();
            XSSFFont font = this.workbook.createFont();
            font.setColor(IndexedColors.RED.getIndex());
            cellInRedStyle.setFont(font);
            cellInRedStyle.setWrapText(true);
            cellInRedStyle.setAlignment(HorizontalAlignment.LEFT);
            cellInRedStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            if (weekend) {
                cell.setCellStyle(cellInRedStyle);
            } else {
                cell.setCellStyle(cellBasicStyle);
            }
            cell.setCellValue(date);

            String hosp=promoForm.getAffectedHosp();
            // String timePattern = "\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b";
            // String timePattern = "\\b((?:after|on/before)?\\s*([01]?\\d|2[0-3]):([0-5]\\d)(?:\\s*-\\s*([01]?\\d|2[0-3]):([0-5]\\d))?)\\b";
            String time = "";
            String timePattern = "\\b((?:after|on/before)?\\s*((([01]?\\d|2[0-3]):([0-5]\\d)(?:\\s*-\\s*([01]?\\d|2[0-3]):([0-5]\\d))?)|((1[0-2]|0?[1-9])(am|pm|AM|PM))|((1[0-2]|0?[1-9]):([0-5]\\d)([aApP][mM])-(1[0-2]|0?[1-9]):([0-5]\\d)([aApP][mM])))\\b)|\\b((?:before|after)\\s*(\\d{2}/\\d{2}/\\d{4})\\s*([01]?\\d|2[0-3]):([0-5]\\d))\\b";
            Pattern pattern = Pattern.compile(timePattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(hosp);
            int CMW_index = hosp.toLowerCase().indexOf("cmw");
            if (CMW_index != -1) { 
                time = extractCMWAndDate(hosp);
            }
            else if (matcher.find()) {
                time = matcher.group();
            }
            cell = row.createCell(i++);
            if (weekend) {
                cell.setCellStyle(cellInRedStyle);
            } else {
                cell.setCellStyle(cellBasicStyle);
            }
            cell.setCellValue(time);


            concatenateTicketRelationships(promoForm).getConcatenatedRelationshipString();
            String originalString = promoForm.getDescription();


            ArrayList<String> jiraTickets = new ArrayList<>();

            String description = originalString;
            String important = originalString;
            String allCondition = "";
            String condition = "";

            Pattern patternDescription = Pattern.compile("([a-zA-Z]{1,15}-\\d{1,10}): (.+)");
            Matcher matcherDescription = patternDescription.matcher(originalString);

            while (matcherDescription.find()) {
                jiraTickets.add(matcherDescription.group(1));
            }
            String[] parts;
            if (jiraTickets.size() == 1) {
                parts = originalString.split(": ", 2);
                important = parts[1];
                description = parts[1];
            }

            String[] parts2 = new String[2];
            if (important.contains("===== Please take note of below CR information =====")) {
                parts2 = important.split("\n===== Please take note of below CR information =====", 2);
            } else if (important.contains("===== Please take note of below IssueLink information =====")) {
                parts2 = important.split("\n===== Please take note of below IssueLink information =====", 2);
            }
            if (parts2[0]!=null && parts2[1]!=null) {
                description = parts2[0];
                allCondition = parts2[1];
            }
            // int index = allCondition.toLowerCase().indexOf("done");
            // if (index != -1) { 
            //     condition = allCondition.substring(index);
            // }
            Pattern patternCondition = Pattern.compile("(?i)done([^\n]*)");
            Matcher matcherCondition = patternCondition.matcher(allCondition);
            String lastCondition = "";
            String currentCondition;

            while (matcherCondition.find()) {
                currentCondition = matcherCondition.group(0);
                if (!currentCondition.equals(lastCondition)) {
                    condition = condition + currentCondition + "\n";
                }
                lastCondition = currentCondition;
            }
            condition = condition.replace("[", "").replace("]", "");

            cell = row.createCell(i++);
            cell.setCellValue(condition);
            cell.setCellStyle(cellBasicStyle);

            // String concatenatedRelationshipString = concatenateTicketRelationships(promoForm).getConcatenatedRelationshipString();
            // if(!concatenatedRelationshipString.isBlank()){
            //     cell.setCellValue(concatenatedRelationshipString);
            // }

            // else{
            //     // cell.setCellValue(promoForm.getConcatenatedReadmePromoName());
            //     cell.setCellValue(concatenateTicketRelationships(promoForm).getConcatenatedRelationshipString());
            // }
            
            // String concatenatedRelationshipString = concatenateTicketRelationships(promoForm).getConcatenatedRelationshipString();
            // if(!concatenatedRelationshipString.isBlank()){
            //     cell.setCellValue(concatenatedRelationshipString);
            // }
            cell = row.createCell(i++);

            String datePattern = "^\\d{2}[- ](?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[- ]\\d{4}$";

            Pattern pattern_date = Pattern.compile(datePattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher_date = pattern_date.matcher(hosp);

            cell.setCellValue(promoForm.getAffectedHosp());
            if (matcher_date.matches() || hosp.toLowerCase().indexOf("all")!=-1 || hosp.isBlank()) {
                cell.setCellValue("All");
            }
            cell.setCellStyle(cellBasicStyle);


            cell = row.createCell(i++);
            originalString = promoForm.getSummary();
            int underscoreIndex = originalString.indexOf('_');
            String ppm = originalString.substring(underscoreIndex + 1);
            cell.setCellValue(ppm);
            cell.setCellStyle(cellBasicStyle);

            String jiraTicket = "";
            for (String s: jiraTickets) {
                jiraTicket = jiraTicket + s + "\n";
            }
            
            cell = row.createCell(i++);
            cell.setCellValue(jiraTicket);
            cell.setCellStyle(cellBasicStyle);

            cell = row.createCell(i++);
            cell.setCellValue(description);
            cell.setCellStyle(cellBasicStyle);

            relatedCount++;

            // cell = row.createCell(i++);
            // cell.setCellValue(concatenateTicketRelationships(promoForm).getConcatenatedRelationshipString());
            // cell = row.createCell(i++);
            // cell.setCellValue(promoForm.getDescription());
            // relatedCount++;
        }
        else {

        }
    }

    public XSSFWorkbook getResult() {
        return workbook;
    }

    private static PromoForm concatenateTicketRelationships(PromoForm promoForm){
        Map<String, Set<String>> endingTicketRelationship = promoForm.getEndingTicketRelationshipMap();
        List<String> relationshipStringList = new ArrayList<>();
        if(!endingTicketRelationship.isEmpty()){
            Map<String, String> endingTicketSummary = promoForm.getEndingTicketSummaryMap();
            for (Map.Entry<String, Set<String>> entry: endingTicketRelationship.entrySet()){
                String endingSummary = endingTicketSummary.get(entry.getKey());
                String endingTicket = entry.getKey() + (endingSummary.isBlank() ? "" : "_" + endingSummary);
                String relationshipString = String.join(" and ", entry.getValue());
                relationshipStringList.add(relationshipString + " " + endingTicket);
            }
            return promoForm.concatenatedRelationshipString(String.join("; ", relationshipStringList));
        }
        return promoForm.concatenatedRelationshipString("");
    }

    public void clearWorkbookContent(XSSFWorkbook workbook) {
        if (workbook == null) {
            throw new IllegalArgumentException();
        }

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);

            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {

                    for (int cellIndex = row.getFirstCellNum(); cellIndex < row.getLastCellNum(); cellIndex++) {
                        Cell cell = row.getCell(cellIndex);
                        if (cell != null) {
                            cell.setBlank();
                        }
                    }
                }
            }
        }
        initializeSheetHeaders(this.related);
        clearRelatedCount();
        this.relatedCount++;
    }
    public void clearRelatedCount() {
        relatedCount = 0;
    }

    public static String extractCMWAndDate(String text) {
        String lowerText = text.toLowerCase();
        int cmwIndex = lowerText.indexOf("cmw");
        
        if (cmwIndex != -1) {
            int startBracket = lowerText.lastIndexOf('(', cmwIndex);
            int endBracket = lowerText.indexOf(')', cmwIndex);

            int start = startBracket != -1 ? Math.min(startBracket, cmwIndex) : cmwIndex;
            int end = endBracket != -1 ? Math.max(endBracket + 1, cmwIndex + 3) : cmwIndex + 3;

            return text.substring(start, end).trim();
        }

        return null;
    }
}



        // List<PromoForm> allUrgentServiceForms = new ArrayList<>(DATA_CENTER.getKeyUrgentServiceFormMap().values());
        // Collections.sort(allUrgentServiceForms);
        // for (PromoForm promoForm: allUrgentServiceForms){
        //     String affectedHosp = promoForm.getAffectedHosp();
        //     if (!affectedHosp.isBlank()){
        //         String[] parts = affectedHosp.split("\n");
        //         content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", promoForm.getConcatenatedReadmePromoName(), parts[0]));
        //         if(parts.length > 1){
        //             for(int i = 1; i < parts.length; i++){
        //                 content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", "", parts[i]));
        //             }
        //         }
        //         String concatenatedRelationshipString = concatenateTicketRelationships(promoForm).getConcatenatedRelationshipString();
        //         if(!concatenatedRelationshipString.isBlank()){
        //             content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", "", concatenatedRelationshipString));
        //         }
        //     }else{
        //         content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", promoForm.getConcatenatedReadmePromoName(), concatenateTicketRelationships(promoForm).getConcatenatedRelationshipString()));
        //     }
        //     content.append("\n");
        // }

// public class ExcelSheet() {
//     // further integration: Urgent promotion set to" style=\"background-color: lightgreen; font-weight: bold;\""
//     private XSSFWorkbook workbook = new XSSFWorkbook();
//     private Sheet related = workbook.createSheet("Related Urgent/Service/Special Promotion");
//     private Sheet unrelated = workbook.createSheet("Unrelated Urgent/Service/Special Promotion");
//     private Integer related_count=0;
//     private Integer unrelated_count=0;

//     // private void setInitialContent(Sheet sheet) {
//     //     Row row;
//     //     Cell cell;
//     //     sheet.createRow();
//     // }

//     private ExcelSheet() {
//         Row row;
//         Cell cell;
//         String[] columnName = {"Target date", "Target time", "Condition", "Hosp", "PPM#", "JIRA#", "Description"};

//         row = related.createRow(related_count++);
//         for (int i=0; i<7; i++) {
//             cell = row.createCell(i);
//             cell.setCellValue(columnName[i]);
//         }

//         row = unrelated.createRow(unrelated_count++);
//         for (int i=0; i<7; i++) {
//             cell = row.createCell(i);
//             cell.setCellValue(columnName[i]);
//         }
//     }

//     private static ExcelSheet instance = new ExcelSheet();
//     public static ExcelSheet getInstance() {return instance;}


// }







        // return isUrgentService ? String.format("""
        //         <tr%s>
        //           	 <td>%s</td>
        //           	 <td>%s</td>
        //           	 <td><a href="https://hatool.home/jira/browse/%s" target="_blank">%s</a></td>
        //           	 <td>%s</td>
        //           	 <td><a href="%s" target="_blank">%s</td>
        //           	 <td>%s</td>
        //           	 <td>%s</td>
        //         </tr>
        //         """,
        //         addStyle,
        //         dateHighlight(TimeUtil.dateDayOfWeekFormatter(promoForm.getTargetDate())),
        //         replaceWithHTMLbrTag(promoForm.getAffectedHosp()),
        //         promoForm.getKey_ITOCMS(),
        //         promoForm.getSummary(),
        //         replaceWithHTMLbrTag(promoForm.getDescription()),
        //         promoForm.getK2FormLink(),
        //         promoForm.getK2FormNo(),
        //         formatType_v2(promoForm.getTypes()),
        //         promoForm.getStatus()
        //         ) :
        //         String.format("""
        //         <tr>
        //           	 <td><a href="https://hatool.home/jira/browse/%s" target="_blank">%s</a></td>
        //           	 <td>%s</td>
        //           	 <td>%s</td>
        //           	 <td><a href="%s" target="_blank">%s</td>
        //           	 <td>%s</td>
        //           	 <td>%s</td>
        //         """,
        //         promoForm.getKey_ITOCMS(),
        //         promoForm.getSummary(),
        //         replaceWithHTMLbrTag(promoForm.getAffectedHosp()),
        //         replaceWithHTMLbrTag(promoForm.getDescription()),
        //         promoForm.getK2FormLink(),
        //         promoForm.getK2FormNo(),
        //         formatType_v2(promoForm.getTypes()),
        //         promoForm.getStatus());


