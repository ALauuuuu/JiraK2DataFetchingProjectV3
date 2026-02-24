package org.ha.ckh637.component;


import org.ha.ckh637.utils.TimeUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class EmailHTML {
    private EmailHTML(){}
    private static final DataCenter DATA_CENTER = DataCenter.getInstance();
    private static final String EMAIL_HTML_HEADER = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
                <style>
                    table {
                        width: 100%%;
                    }
            
                    table,
                    th,
                    td {
                        border: 1px solid black;
                        border-collapse: collapse;
                    }
            
                    th,
                    td {
                        padding: 15px;
                        text-align: left;
                    }
            
                    table#t01 th {
                        background-color: yellow;
                        color: black;
                    }
            
                    table#t02 th {
                        background-color: gray;
                        color: lightgrey;
                    }
                </style>
            </head>
            <body>
            """;

    private static final String TABLE_HEADER_URGENT_SERVICE = """
            <tr>
                <th max-width="110">Target Date</th>
                <th max-width="110">Affected Hospital</th>
                <th width="130">Summary</th>
                <th max-width="300">Description</th>
                <th width="155">Promotion Form</th>
                <th width="130">Type(s)</th>
                <th max-width="100">Status</th>
            </tr>
            """;

    private static final String TABLE_HEADER_BIWEEKLY = """
            <tr>
                <th width="130">Summary</th>
                <th max-width="110">Affected Hospital</th>
                <th max-width="300">Description</th>
                <th width="155">Promotion Form</th>
                <th width="130">Type(s)</th>
                <th max-width="100">Status</th>
            </tr>
            """;

    public static String dynamicEmailHTMLDom(int relatedCnt, String related, int unrelatedCnt, String unrelated, boolean isUrgentService){
        return String.format("""
                %s
                <h1 style="color: red; text-decoration: underline;">IMP Promotions (%d):</h1>
                <table id="t01">
                    %s
                    %s
                </table>
                <br>
                <hr>
                <h1 style="color: gray;">Unrelated (%d):</h1>
                <table id="t02">
                    %s
                    %s
                </table>
                <br>
                </body>
                </html>
                """,
                EMAIL_HTML_HEADER,
                relatedCnt,
                isUrgentService ? TABLE_HEADER_URGENT_SERVICE : TABLE_HEADER_BIWEEKLY,
                related,
                unrelatedCnt,
                isUrgentService ? TABLE_HEADER_URGENT_SERVICE : TABLE_HEADER_BIWEEKLY,
                unrelated
                );
    }

    /*public static String genTableRowContent(EmailForm emailForm, boolean isUrgentService){
        final String HIGHLIGHT_STYLE = " style=\"background-color: lightgreen; font-weight: bold;\"";
        String addStyle = emailForm.isToday() ? HIGHLIGHT_STYLE : "";
        return isUrgentService ? String.format("""
                <tr%s>
                  	 <td>%s</td>
                  	 <td>%s</td>
                  	 <td><a href="https://hatool.home/jira/browse/%s" target="_blank">%s</a></td>
                  	 <td>%s</td>
                  	 <td><a href="%s" target="_blank">%s</td>
                  	 <td>%s</td>
                  	 <td>%s</td>
                </tr>
                """,
                addStyle,
                dateHighlight(emailForm.getTargetDate()),
                replaceWithHTMLbrTag(emailForm.getAffectedHosp()),
                emailForm.getKey(),
                emailForm.getSummary(),
                replaceWithHTMLbrTag(emailForm.getDescription()),
                emailForm.getPromotionFormLink(),
                emailForm.getPromotionFormNo(),
                formatType(emailForm.getTypes()),
                emailForm.getStatus()
                ) :
                String.format("""
                <tr>
                  	 <td><a href="https://hatool.home/jira/browse/%s" target="_blank">%s</a></td>
                  	 <td>%s</td>
                  	 <td>%s</td>
                  	 <td><a href="%s" target="_blank">%s</td>
                  	 <td>%s</td>
                  	 <td>%s</td>
                """,
                emailForm.getKey(),
                emailForm.getSummary(),
                replaceWithHTMLbrTag(emailForm.getAffectedHosp()),
                replaceWithHTMLbrTag(emailForm.getDescription()),
                emailForm.getPromotionFormLink(),
                emailForm.getPromotionFormNo(),
                formatType(emailForm.getTypes()),
                emailForm.getStatus());
    }*/

    private static String dateHighlight(String date){
        if (date.contains("Sat") || date.contains("Sun")){
            return String.format("<span style=\"color: red; font-weight: bold;\">%s</span>", date);
        }
        return date;
    }

    private static String replaceWithHTMLbrTag(String input){
        return input.isBlank() ? "N/A" : input.replaceAll("(\r\n|\r|\n)", "<br>");
    }

/*    private static String formatType(Map<Integer, String> emailFormTypes){
        StringBuilder results = new StringBuilder();
        for (String type: emailFormTypes.values()){
            results.append(type).append("<br>");
        }
        return results.toString();
    }*/

    private static String formatType_v2(List<String> allTypes){
        StringBuilder results = new StringBuilder();
        allTypes.forEach(type -> results.append(type).append("<br>"));
        return results.toString();
    }

    /*@Deprecated
    public static String emailHTMLDom(int relatedCnt, String related, int unrelatedCnt, String unrelated){
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
                    <style>
                        table {
                            width: 100%%;
                        }
                
                        table,
                        th,
                        td {
                            border: 1px solid black;
                            border-collapse: collapse;
                        }
                
                        th,
                        td {
                            padding: 15px;
                            text-align: left;
                        }
                
                        table#t01 th {
                            background-color: yellow;
                            color: black;
                        }
                
                        table#t02 th {
                            background-color: gray;
                            color: lightgrey;
                        }
                    </style>
                </head>
                <body>
                    <h1 style="color: red; text-decoration: underline;">IMP Promotions (%d):</h1>
                    <table id="t01">
                        <tr>
                            <th max-width="110">Target Date</th>
                            <th max-width="110">Affected Hospital</th>
                            <th width="130">Summary</th>
                            <th max-width="300">Description</th>
                            <th width="155">Promotion Form</th>
                            <th width="130">Type(s)</th>
                            <th max-width="100">Status</th>
                        </tr>
                        %s
                    </table>
                    <br>
                    <hr>
                    <h1 style="color: gray;">Unrelated (%d):</h1>
                    <table id="t02">
                        <tr>
                            <th max-width="110">Target Date</th>
                            <th max-width="110">Affected Hospital</th>
                            <th width="130">Summary</th>
                            <th max-width="300">Description</th>
                            <th width="155">Promotion Form</th>
                            <th width="130">Type(s)</th>
                            <th max-width="100">Status</th>
                        </tr>
                        %s
                    </table>
                    <br>
                </body>
                </html>
                """, relatedCnt, related, unrelatedCnt, unrelated);
    }*/


    ////////////////////////////

    public static String genTableRowContent_V2(PromoForm promoForm, boolean isUrgentService){
        final String HIGHLIGHT_STYLE = " style=\"background-color: lightgreen; font-weight: bold;\"";
        String addStyle = isUrgentService && TimeUtil.checkIsToday(promoForm.getTargetDate()) ? HIGHLIGHT_STYLE : "";
        return isUrgentService ? String.format("""
                <tr%s>
                  	 <td>%s</td>
                  	 <td>%s</td>
                  	 <td><a href="https://hatool.home/jira/browse/%s" target="_blank">%s</a></td>
                  	 <td>%s</td>
                  	 <td><a href="%s" target="_blank">%s</td>
                  	 <td>%s</td>
                  	 <td>%s</td>
                </tr>
                """,
                addStyle,
                dateHighlight(TimeUtil.dateDayOfWeekFormatter(promoForm.getTargetDate())),
                replaceWithHTMLbrTag(promoForm.getAffectedHosp()),
                promoForm.getKey_ITOCMS(),
                promoForm.getSummary(),
                replaceWithHTMLbrTag(promoForm.getDescription()),
                promoForm.getK2FormLink(),
                promoForm.getK2FormNo(),
                formatType_v2(promoForm.getTypes()),
                promoForm.getStatus()
                ) :
                String.format("""
                <tr>
                  	 <td><a href="https://hatool.home/jira/browse/%s" target="_blank">%s</a></td>
                  	 <td>%s</td>
                  	 <td>%s</td>
                  	 <td><a href="%s" target="_blank">%s</td>
                  	 <td>%s</td>
                  	 <td>%s</td>
                """,
                promoForm.getKey_ITOCMS(),
                promoForm.getSummary(),
                replaceWithHTMLbrTag(promoForm.getAffectedHosp()),
                replaceWithHTMLbrTag(promoForm.getDescription()),
                promoForm.getK2FormLink(),
                promoForm.getK2FormNo(),
                formatType_v2(promoForm.getTypes()),
                promoForm.getStatus());
    }

    public static String compileEmailHTMLContent(final boolean isUrgentService){
        StringBuilder relatedTableContent = new StringBuilder();
        StringBuilder unrelatedTableContent = new StringBuilder();
        final AtomicInteger relatedCnt = new AtomicInteger();
        final AtomicInteger unrelatedCnt = new AtomicInteger();

        DATA_CENTER.getKeyPromoFormMap().values().forEach(promoForm -> {
            if (promoForm.isImpHospOrImpCorp()){
                relatedCnt.getAndIncrement();
                relatedTableContent.append(genTableRowContent_V2(promoForm, isUrgentService));
            }else{
                unrelatedCnt.getAndIncrement();
                unrelatedTableContent.append(genTableRowContent_V2(promoForm, isUrgentService));
            }
        });

        return EmailHTML.dynamicEmailHTMLDom(
                relatedCnt.get(),
                relatedTableContent.toString(),
                unrelatedCnt.get(), unrelatedTableContent.toString(),
                isUrgentService);
    }

    public static void compileEmailHTMLContentExcel(final boolean isUrgentService){
        StringBuilder relatedTableContent = new StringBuilder();
        StringBuilder unrelatedTableContent = new StringBuilder();
        final AtomicInteger relatedCnt = new AtomicInteger();
        final AtomicInteger unrelatedCnt = new AtomicInteger();

        DATA_CENTER.getKeyPromoFormMap().values().forEach(promoForm -> {
            if (promoForm.isImpHospOrImpCorp()){
                ExcelSheet.getInstance().setExcelContent(promoForm, isUrgentService, true);
                // relatedCnt.getAndIncrement();
                // relatedTableContent.append(genTableRowContent_V2(promoForm, isUrgentService));
            }else{
                ExcelSheet.getInstance().setExcelContent(promoForm, isUrgentService, false);
                // unrelatedCnt.getAndIncrement();
                // unrelatedTableContent.append(genTableRowContent_V2(promoForm, isUrgentService));
            }
        });

        // return EmailHTML.dynamicEmailHTMLDom(
        //         relatedCnt.get(),
        //         relatedTableContent.toString(),
        //         unrelatedCnt.get(), unrelatedTableContent.toString(),
        //         isUrgentService);
    }
}

// do some edition to compileEmailHTMLContent can create a xlsx file