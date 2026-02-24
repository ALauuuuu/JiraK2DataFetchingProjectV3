package org.ha.ckh637.utils;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ha.ckh637.component.DataCenter;
import org.ha.ckh637.component.PromoForm;
import org.ha.ckh637.component.VerifyScript;
import org.ha.ckh637.service.APIQueryService;
import org.ha.ckh637.service.AppIniService;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonDataParser {
    private JsonDataParser(){}
    private static final ObjectMapper OBJECT_MAPPER = AppIniService.getObjectMapper();
//    private static final PromoReleaseEmailConfig PROMO_RELEASE_EMAIL_CONFIG = com.crc2jasper.jiraK2DataFetching.component.PromoReleaseEmailConfig.getInstance();
    private static final DataCenter DATA_CENTER = DataCenter.getInstance();
    private static final String AFFECTED_HOSP_REGEX = "(Affected Hospital|Effective Date).|\\{color:.{0,8}}|\\{color}|\\\\u[0-9A-Fa-f]{4}\"|<[^>]*>|&[a-zA-Z0-9#]+;|[*{}]";

    private static boolean isImpHospOrImpCorp(List<String> allTypes){
        return allTypes.contains("imp-hosp-db") || allTypes.contains("imp-corp-db");
    }

    private static String extractPPMSummary(String ITOCMS_PPM){
        //e.g. ITOCMS-35975, PPM2024_U0237
        // or ITOCMS-35975
        String[] parts = ITOCMS_PPM.split(", ");
        for (String part: parts){
            if(part.contains("PPM")){
                return part;
            }
        }
        return ITOCMS_PPM;
    }

    public static Map<String, String> retrieveTicketSummaryAndRelatedTickets(String jiraResp){
        Map<String, String> ticketSummaryAndChildTickets = new HashMap<>();
        try {
            JsonNode issues = OBJECT_MAPPER.readTree(jiraResp).get("issues");
            for (JsonNode currIssue: issues){
                String key_ITOCMS = currIssue.get("key").asText();
                JsonNode fields = currIssue.get("fields");
                String summary = fields.get("summary").asText();
                String relatedTickets = fields.get("customfield_11599").asText();
                ticketSummaryAndChildTickets.put("key_ITOCMS", key_ITOCMS);
                ticketSummaryAndChildTickets.put("summary", summary);
                ticketSummaryAndChildTickets.put("relatedTickets", relatedTickets);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ticketSummaryAndChildTickets;
    }

    public static void parseJiraUrgentServiceForBiweeklyResp(Mono<String> monoJiraResp){
        String jiraResp = monoJiraResp.block();
        try {
            JsonNode issues = OBJECT_MAPPER.readTree(jiraResp).get("issues");
            for (JsonNode currIssue: issues){
                try{
                    // "customfield_11400": "https://wfeng-svc/Runtime/Runtime/Form/CMS__Promotion__Form?formnumber=M-ITOCMS-24-1244"
                    JsonNode fields = currIssue.get("fields");
                    String k2FormLink = fields.get("customfield_11400").asText();
                    String k2FormNo = retrieveK2FormNoFromLink(k2FormLink);
                    // Here, with the form no. e.g. M-ITOCMS-24-1244, we have to fetch JFrog for the types
                    String jFrogResp = APIQueryService.fetchJFrogAPIForTypes(k2FormNo);
                    Map<String, List<String>> retrievedResults = retrieveTypePathsAndImpManualItemsFromJFrogResp(jFrogResp, true);
                    List<String> allTypePaths = retrievedResults.get("allTypePaths");
                    List<String> allImpManualItems = retrievedResults.get("allImpManualItems");
                    List<String> allTypes = retrieveFinalTypesFromTypePaths(allTypePaths);
                    if(isImpHospOrImpCorp(allTypes)){
                        PromoForm promoForm = new PromoForm().k2FormLink(k2FormLink)
                                .k2FormNo(k2FormNo).types(allTypes).addImpManualItems(allImpManualItems);
                        String parentTicket = currIssue.get("key").asText();
                        Map<String, String> ticketSummaryAndRelatedTickets = APIQueryService.fetchTicketSummaryAndRelatedTickets(parentTicket);
                        String key_ITOCMS = ticketSummaryAndRelatedTickets.get("key_ITOCMS");
                        String summary = ticketSummaryAndRelatedTickets.get("summary");
                        String relatedTickets = ticketSummaryAndRelatedTickets.get("relatedTickets");

                        promoForm.key_ITOCMS(key_ITOCMS).summary(summary)
                                .allTickets(new ArrayList<>(Arrays.asList(relatedTickets.split(", "))));

                        Map<String, Set<String>> endingTicketRelationshipMap = processIssueLinks(fields.get("issuelinks"));

                        // in relatedTickets, check to see if it's only parent ticket or there are child tickets too
                        String[] allTickets = relatedTickets.split(", ");
                        if (allTickets.length > 1){
                            String[] childTickets = Arrays.copyOfRange(allTickets, 1, allTickets.length);
                            String response = APIQueryService.fetchTicketIssueLinks(String.join(",", childTickets));
                            JsonNode childTicketIssues = OBJECT_MAPPER.readTree(response).get("issues");
                            for (JsonNode childTicketIssue: childTicketIssues){
                                Map<String, Set<String>> childTicketRelationshipMap = processIssueLinks(childTicketIssue.get("fields").get("issuelinks"));
                                endingTicketRelationshipMap.putAll(childTicketRelationshipMap);
                            }
                        }
                        promoForm.endingTicketRelationshipMap(endingTicketRelationshipMap);

                        Map<String, String> tempEndingTicketSummaryMap = new HashMap<>();
                        for(String endingTicket: endingTicketRelationshipMap.keySet()){
                            String endingTicketSummary = APIQueryService.fetchTicketSummary(endingTicket);;
                            tempEndingTicketSummaryMap.put(endingTicket, endingTicketSummary);
                        }
                        promoForm.endingTicketSummaryMap(tempEndingTicketSummaryMap);

                        String affectedHosp = retrieveAffectedHosp(fields);
                        promoForm.affectedHosp(affectedHosp);
                        DATA_CENTER.addUrgentServiceForm(key_ITOCMS, promoForm);
                    }
                }catch (Exception e){
                    System.out.println("Exception raised when fetching Jira Urgent/Service Promotions for Bi-weekly: " + e.getMessage() + "\n");
                }
            }
        } catch (Exception e) {
            System.out.println("Exception raised when fetching Jira Urgent/Service Promotions for Bi-weekly: " + e.getMessage() + "\n");
        }
    }

    public static void parseStandardJiraResp(final String year_batch, Mono<String> monoJiraResp, boolean isBiweekly, boolean isSQL){
        String jiraResp = monoJiraResp.block();
        try{
            JsonNode issues = OBJECT_MAPPER.readTree(jiraResp).get("issues");
            for(JsonNode issue: issues){
                JsonNode fields = issue.get("fields");
                if(!isCurrentBatch(fields, year_batch)) continue;
                String status = fields.get("status").get("name").asText();
                if(status.equalsIgnoreCase("Withdrawn") || status.equalsIgnoreCase("Rejected")) continue;
                String key_ITOCMS = issue.get("key").asText();
                String summary_PPM = fields.get("summary").asText();
                String targetDate = fields.get("customfield_11628").asText();
                String description = fields.get("description").asText();
                PromoForm promoForm = new PromoForm().targetDate(targetDate).key_ITOCMS(key_ITOCMS)
                        .summary(summary_PPM).description(description).status(status);
                DATA_CENTER.addPromoForm(key_ITOCMS, promoForm);
//                if(isBiweekly){
                String allTicketString = fields.get("customfield_11599").asText();
                String[] allTickets = allTicketString.split(", ");
                promoForm.allTickets(new ArrayList<>(Arrays.asList(allTickets)));
//                }
                APIQueryService.jiraTicketInfoFromITOCMSKey(key_ITOCMS, isBiweekly);
                List<String> allTypePaths = null;
                List<String> allImpManualItems = new ArrayList<>();
                if(summary_PPM.contains("PPM")){
                    // fetch from jFrog
                    String k2FormNo = promoForm.getK2FormNo();
                    String jFrogResp = APIQueryService.fetchJFrogAPIForTypes(k2FormNo);
//                    allTypePaths = retrieveTypePathsAndImpManualItemsFromJFrogResp(jFrogResp);
                    Map<String, List<String>> retrievedResults = retrieveTypePathsAndImpManualItemsFromJFrogResp(jFrogResp, isBiweekly);
                    if (isSQL) {
                        String sqlContent = retrieveSQLContent(jFrogResp);
                        if (!(sqlContent.isEmpty() || sqlContent==null)) {
                            promoForm.impHospSql(sqlContent);
                            VerifyScript.addPromoForm(promoForm);
                        }
                    }
                    allTypePaths = retrievedResults.get("allTypePaths");
                    allImpManualItems = retrievedResults.get("allImpManualItems");
                }else{
                    String rawK2FormLink = fields.get("customfield_11400").asText();
                    String k2FormLink = "", k2FormNo = "N/A";
                    if (rawK2FormLink.contains("M-ITOCMS")){
                        k2FormLink = retrieveK2FormLink(rawK2FormLink);
                        k2FormNo = retrieveK2FormNoFromLink(k2FormLink);
                    }
                    promoForm.k2FormLink(k2FormLink).k2FormNo(k2FormNo);
                    // from customfield_14500
                    String cd_configuration = fields.get("customfield_14500").asText();
                    if (!cd_configuration.equalsIgnoreCase("null")){
                        allTypePaths = retrieveTypePathsfromCF14500(cd_configuration);
                    }else{
                        // from http://cdrasvn:90/
                        String parentTicket = promoForm.getAllTickets().getFirst();
                        allTypePaths = APIQueryService.collabNetInitialAPI(parentTicket);
                    }
                }
                List<String> allTypes = retrieveFinalTypesFromTypePaths(allTypePaths);
                promoForm.types(allTypes).addImpManualItems(allImpManualItems);
                promoForm.isImpHospOrImpCorp(isImpHospOrImpCorp(allTypes));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static List<String> retrieveFinalTypesFromTypePaths(List<String> allTypePaths) {
        Map<Integer, String> sequenceTypeMap = new TreeMap<>();
        for (String path: allTypePaths){
            // String[] type:
            // index 0: sequence no. (still as string, not parsed to int yet)
            // index 1: actual type e.g. imp-hosp-db, imp-corp-db
            String[] seqAndType = extractSeqAndTypeFromPath(path);
            sequenceTypeMap.put(Integer.parseInt(seqAndType[0]), seqAndType[1]);
        }
        return new ArrayList<>(sequenceTypeMap.values());
    }

    private static String[] extractSeqAndTypeFromPath(String pathPart){
        try{
            String[] parts = pathPart.split("_");
            return new String[]{parts[1], parts[2]};
        }catch (Exception e){
            System.out.println("Error occurred in extractTypeFromPath().");
            e.printStackTrace();
            return new String[]{"-1", "ERROR"};
        }
    }

    private static List<String> retrieveTypePathsfromCF14500(String cd_configuration){
        List<String> allTypePaths = new ArrayList<>();
        try{
            JsonNode configNode = OBJECT_MAPPER.readTree(cd_configuration);
            if (configNode.isArray()) {
                for (JsonNode currConfig : configNode) {
                    JsonNode deployPackages = currConfig.get("deployPackageFolder");
                    if (deployPackages != null) {
                        for (JsonNode currDeployPackage : deployPackages) {
                            // DP_110_ecp_cms-vts-common-svc, DP_100_manual_updateSecret
                            currDeployPackage.fieldNames().forEachRemaining(allTypePaths::add);
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return allTypePaths;
    }

    private static Map<String, List<String>> retrieveTypePathsAndImpManualItemsFromJFrogResp(String jFrogResp, boolean isBiweekly){
        List<String> allTypePaths = new ArrayList<>();
        List<String> allImpManualItems = new ArrayList<>();
        Map<String, List<String>> extractedResults = new HashMap<>(2);
        try {
            JsonNode results = OBJECT_MAPPER.readTree(jFrogResp).get("results");
            for (JsonNode currResult: results){
                String path = currResult.get("path").asText();
                // path e.g. CMS/OPMOE/CMS_MOE_CMSAF_APP_JDK8/M-ITOCMS-24-1232/DP_40_corp-db_UpdateForwarder/DB_SERVER_LIST_CORP/corp
                String[] pathParts = path.split("/");
                int keyIndex = getTypeIndexFromJFrogPathParts(pathParts);
                if (keyIndex >= 0 && keyIndex < pathParts.length){ // && pathParts[keyIndex].contains("DP") -> not necessary due to revised API payload
                    allTypePaths.add(pathParts[keyIndex]);
                    if(isBiweekly && pathParts[keyIndex].contains("imp-manual")){
                        String impManualItem = currResult.get("name").asText();
                        // e.g. "name": "752_OPMOE226_alter_tb_drug_intent_data_hdr.sql"
                        allImpManualItems.add(impManualItem);
                    }
                }// else allTypePaths.add("N/A");  // actually, if the program is working fine, shouldn't have N/A at all
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        extractedResults.put("allTypePaths", allTypePaths);
        extractedResults.put("allImpManualItems", allImpManualItems);
        return extractedResults;
    }

    private static int getTypeIndexFromJFrogPathParts(String[] pathParts){
        for (int i = 0; i < pathParts.length; i++){
            if(pathParts[i].contains("ITOCMS")) return i + 1;
        }
        return -1;
    }

    public static void parseJiraTicketInfoFromITOCMSKeyResp(String jiraResp, String key_ITOCMS, boolean isBiweekly){
        String affectedHosp = "";
        String k2FormLink = "";
        try{
            JsonNode issues = OBJECT_MAPPER.readTree(jiraResp).get("issues");
            PromoForm promoForm = DATA_CENTER.getPromoFormByKey_ITOCMS(key_ITOCMS);
            Map<String, Set<String>> allEndingTicketRelationshipMap = new LinkedHashMap<>();
            for(JsonNode issue: issues){
                JsonNode fields = issue.get("fields");
                if(affectedHosp.isBlank()){
                    affectedHosp = retrieveAffectedHosp(fields);
                    promoForm.affectedHosp(affectedHosp);
                }
                if(k2FormLink.isBlank()){
                    String srcK2FormLink = fields.get("customfield_11400").asText();
                    if (!srcK2FormLink.equalsIgnoreCase("null")) {
                        k2FormLink = srcK2FormLink;
                        String k2FormNo = retrieveK2FormNoFromLink(k2FormLink);
                        promoForm.k2FormLink(k2FormLink).k2FormNo(k2FormNo);
                    }
                }
                if(isBiweekly){
                    JsonNode issueLinks = fields.get("issuelinks");
                    allEndingTicketRelationshipMap.putAll(processIssueLinks(issueLinks));
                }
            }
            if (isBiweekly) {
                promoForm.endingTicketRelationshipMap(allEndingTicketRelationshipMap);
                Map<String, String> tempEndingTicketSummaryMap = new HashMap<>();
                for(String endingTicket: allEndingTicketRelationshipMap.keySet()){
                    String endingTicketSummary = APIQueryService.fetchTicketSummary(endingTicket);;
                    tempEndingTicketSummaryMap.put(endingTicket, endingTicketSummary);
                }
                promoForm.endingTicketSummaryMap(tempEndingTicketSummaryMap);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static Map<String, Set<String>> processIssueLinks(JsonNode issueLinks){
        Map<String, Set<String>> endingTicketRelationshipMap = new LinkedHashMap<>();
        for(JsonNode link: issueLinks){
            String endingTicket = "", relationship = "";
            try{
                if (link.has("outwardIssue")) {
                    endingTicket = link.get("outwardIssue").get("key").asText();
                    relationship = link.get("type").get("outward").asText();
                } else {
                    endingTicket = link.get("inwardIssue").get("key").asText();
                    relationship = link.get("type").get("inward").asText();
                }
                if (relationship.contains("has to")){
                    // e.g. has to be done before -> done before
                    String abridgedRelationship = relationship.replaceAll("has to be", "").strip();
                    if(endingTicketRelationshipMap.containsKey(endingTicket)){
                        endingTicketRelationshipMap.get(endingTicket).add(abridgedRelationship);
                    }else{
                        Set<String> relationshipSet = new LinkedHashSet<>();
                        relationshipSet.add(abridgedRelationship);
                        endingTicketRelationshipMap.put(endingTicket, relationshipSet);
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return endingTicketRelationshipMap;
        // after getting the results, should check if the map is empty
        // empty means no relationship at all
    }

    private static String retrieveK2FormNoFromLink(String k2FormLink){
        try{
            return k2FormLink.split("=")[1];
        }catch (Exception e){
            System.out.println(k2FormLink);
            e.printStackTrace();
            return "N/A";
        }
    }

    private static String retrieveK2FormLink(String rawLink){
        try{
            return rawLink.substring("Promotion Form: ".length(), rawLink.indexOf("\n")).replaceAll("(\r|\n|\r\n)", "");
        }catch(StringIndexOutOfBoundsException e) {
            return rawLink.substring("Promotion Form: ".length()).replaceAll("(\r|\n|\r\n)", "");
            // likely the link does not have any \n at the end
        }
    }

    private static String retrieveAffectedHosp(JsonNode fields){
        String result = "";
        try{
            String rawAffectedHosp = fields.get("customfield_11887").asText();
            if(!rawAffectedHosp.equalsIgnoreCase("null")){
                String[] regexModified = rawAffectedHosp
                        .replaceAll(AFFECTED_HOSP_REGEX, "")
                        .split("(\r\n|\r|\n)");
                List<String> relevant = new ArrayList<>();
                for(String line: regexModified){
                    line = line.replaceAll("[\\s\\u00A0]+", " ").strip();
                    if(line.matches("\\W+") || line.isBlank()) continue;
                    relevant.add(line);
                }
                result =  String.join("\n", relevant);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    private static boolean isCurrentBatch(JsonNode fields, final String year_batch){
        String biweeklyHint = fields.get("customfield_10519").asText();
        if(biweeklyHint.equalsIgnoreCase("null")) return true;
        String promoSchedule = "";
        try {
            promoSchedule = fields.get("customfield_10519").get("value").asText();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // "value": "2024-13 ( PPM: 26-Sep-2024; AAT: 03-Oct-2024)
        return promoSchedule.contains(year_batch);
    }

    public static String retrieveTicketSummary(String response) {
        // from the following jql: e.g. jql=cf[11599]~ENOTI-380&fields=summary
        String result = "";
        try {
            JsonNode issues = OBJECT_MAPPER.readTree(response).get("issues");
            if (issues.isArray()){
                for (JsonNode currIssue: issues){
                    String summary = currIssue.get("fields").get("summary").asText();
                    if (!summary.equalsIgnoreCase("null") && !summary.contains("\\s+")) {
                        return summary;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    //************ testing stage
    private static String retrieveSQLContent(String jFrogResp){
        Mono<String> resp;
        String resultSqlContent = "";
        String resultSqlDeclaration = "";

        try {
            JsonNode results = OBJECT_MAPPER.readTree(jFrogResp).get("results");
            for (JsonNode currResult: results){
                String path = currResult.get("path").asText();
                String name = currResult.get("name").asText();
                String fullPath;

                int keyIndex = path.indexOf("imp-hosp-db");

                if (keyIndex >=0){ 
                    fullPath = path + "/" + name;
                    resp = APIQueryService.fetchSQLContent(fullPath);
                    // System.out.println("filename: " + name);
                    resultSqlDeclaration = parseJfrogImpHospSqlDeclaration(resp);
                    resultSqlContent = parseJfrogImpHospSql(resp);
                    // System.out.println("SQL Content in retrievePathForSQLContent: \n");
                    // System.out.println(resultSqlContent.isEmpty() ? "" : resultSqlDeclaration + "\n" + resultSqlContent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultSqlContent.isEmpty() ? "" : resultSqlDeclaration + resultSqlContent + "\nGO";
    }

    public static String parseJfrogImpHospSql(Mono<String> resp) {
        String sqlText = resp.block();
        StringBuilder selectStatements = new StringBuilder();
        
        // Normalize the input by removing excess whitespace
        sqlText = sqlText.replaceAll("\\s+", " ").trim();
        
        // Pattern to match each update statement
        Pattern updatePattern = Pattern.compile(
            "(?i)update\\s+([\\w@_-]+)\\s+set\\s+(.*?)\\s+where\\s+(.*?)(?=;|(?i)update|$)"
        );
        Matcher matcher = updatePattern.matcher(sqlText);
        
        
        while (matcher.find()) {
            String tableName = matcher.group(1).trim();
            String setClause = matcher.group(2);
            String whereClause = matcher.group(3).trim();
            
            // Extract columns from set clause
            List<String> columnNames = new ArrayList<>();
            String[] setParts = setClause.split(",");
            for (String setPart : setParts) {
                if (setPart.contains("=")) {
                    setPart = setPart.split("=")[0].trim();
                    String columnName = setPart.split("=")[0].trim();
                    columnNames.add(columnName);
                }
            }

            // Extract valid conditions
            String validWhereClause = extractValidConditions(whereClause);

            if (validWhereClause != null) {
                // Construct the select statement only if the where clause is valid
                String selectClause = String.join(", ", columnNames);
                String selectStatement = String.format(
                    "SELECT %s FROM %s WHERE %s",
                    selectClause, tableName, validWhereClause
                );
                selectStatements.append(selectStatement).append("\n");
            }
        }

        return selectStatements.toString();
    }

    private static String extractValidConditions(String whereClause) {
        String conditionPattern1 = "(?i)\\b\\w[\\w@_-]*\\s*(=|!=|is\\s+not|is|in)\\s*(null|@[\\w@_-]+|'[^']*'|\\d+(\\.\\d+)?|\\([^()]*\\))\\s*(?=(AND|OR|and|or)\\b|\\z)";
        String conditionPattern2 = "(?i)\\b\\w[\\w@_-]*\\s*(=|!=|is\\s+not|is|in)\\s*(null|@[\\w@_-]+|'[^']*'|\\d+(\\.\\d+)?|\\([^()]*\\))";
        Matcher matcher1 = Pattern.compile(conditionPattern1).matcher(whereClause);
        Matcher matcher2 = Pattern.compile(conditionPattern2).matcher(whereClause);
    
        StringBuilder validConditions = new StringBuilder();
        int index = 0;
    
        while (matcher1.find(index) || matcher2.find(index)) {
            // System.out.println(validConditions.toString());
            
            if (matcher1.find(index)) {
                if (matcher2.find(index)) {
                    if (matcher1.end()-1 <= matcher2.end()) {
                        validConditions.append(matcher1.group().trim()).append(" ");
                        index = matcher1.end();
                    } else {
                        validConditions.append(matcher2.group().trim()).append(" ");
                        break;
                    }
                } else {
                    validConditions.append(matcher1.group().trim()).append(" ");
                    index = matcher1.end();
                }
            } else {
                validConditions.append(matcher2.group().trim()).append(" ");
                break;
            }
    
            if (index < whereClause.length()) {
                String nextSegment = whereClause.substring(index).trim();
    
                // Capture the logical operator if exists
                if (nextSegment.startsWith("AND ") || nextSegment.startsWith("OR ") ||
                    nextSegment.startsWith("and ") || nextSegment.startsWith("or ")) {
                    String operator = nextSegment.split("\\s+")[0];
                    validConditions.append(operator).append(" ");
                    index += operator.length() + 1;  // Move past the operator and its trailing space
                }
            }
        }
    
        String result = validConditions.toString().trim();
        return result.isEmpty() ? null : result; // Return null if no valid conditions are found
    }

    public static String parseJfrogImpHospSqlDeclaration(Mono<String> resp) {
        // String sqlText = resp.block().replace("\t", "");
        String sqlText = resp.block().replace("\t", "");
        String result = "";

        // String initialRegex = "(?i)declare[\\s\\S]*?\\n(?:set|select)[\\s\\S]*?\\n";
        String initialRegex = "(?i)declare[\\s\\S]*?\\n(?:set|select)?[\\s\\S]*?\\n";
        String setBlockRegex = "(?i)set\\s+\\w+\\s+on[\\r\\n]+[\\s\\S]*?[\\r\\n]+set\\s+\\w+\\s+off[\\r\\n]*";

        Pattern pattern = Pattern.compile(initialRegex);
        Matcher matcher = pattern.matcher(sqlText);
        
//  String setBlockRegex2 = "(?i)^(?!\\s*--)(?!\\s*\\*).*\\b(?:select)\\b.*\\b\\w[\\w@_-]*\\s*(=)\\s*('[^']*'|\\d+(\\.\\d+)?)[\\s]*?\\n";
// String setBlockRegex2 = "^(?!\\s*--)(?!\\s*\\*).*\\bselect\\b.*\\b\\w[\\w@_-]*\\s*(=)\\s*('[^']*'|\\d+(\\.\\d+)?)[\\s]*?\\n";
// String setBlockRegex2 = "(?i)\\b(?:select)\\b.*\\b\\w[\\w@_-]*\\s*(=)\\s*('[^']*'|\\d+(\\.\\d+)?)[\\s]*?\\n";

        String setBlockRegex2 = "(?i)(?:^\\s*|--|/\\*)?\\s?\\b(?:select)\\b.*\\b\\w[\\w@_-]*\\s*(=)\\s*('[^']*'|\\d+(\\.\\d+)?|null)[\\s]*?\\n";

// String setBlockRegex2 = "(?i)(select)\\b.*\\b\\w[\\w@_-]*\\s*(=)\\s*('[^']*'|\\d+(\\.\\d+)?)[\\s]*?\\n";
// String setBlockRegex2 = "(?i)\\bselect\\b.*?\\b\\w[\\w@_-]*\\s*=\\s*('[^']*'|\\d+(\\.\\d+)?)\\s*\\n$";
        Pattern pattern2 = Pattern.compile(setBlockRegex2);
        Matcher matcher2 = pattern2.matcher(sqlText);

        if (matcher.find()) {
            do {
                result = result + matcher.group().replaceAll("(?m)^\\s+(?i)(declare)", "$1");
            } while(matcher.find());
            Pattern setBlockPattern = Pattern.compile(setBlockRegex);
            Matcher setBlockMatcher = setBlockPattern.matcher(sqlText);
            
            String[] resultWords = result.split("\n");
            // for (int i = 0; i < resultWords.length; i++) {
            //     String word = resultWords[i];
            //     System.out.println("Original: " + word.trim().replace("\n", ""));
            //     if (word != null && (word.trim().startsWith("--") || word.trim().startsWith("/*"))) {
            //         resultWords[i] = "";
            //     }
            //     System.out.println("edited: " + resultWords[i]);
            // }
            
            StringBuilder modifiedResult = new StringBuilder();
            
            for (int i = 0; i < resultWords.length; i++) {
                String word = resultWords[i].trim();
                if (!word.startsWith("--") && !word.startsWith("/*") && !word.equals("")) {
                    if (modifiedResult.length() > 0) {
                        modifiedResult.append("\n");
                    }
                    modifiedResult.append(resultWords[i]);
                }
            }
            
            result = modifiedResult.toString();

            if (setBlockMatcher.find()) {
                String[] addWords = setBlockMatcher.group().split("\n");
                LinkedHashSet<String> allWords = new LinkedHashSet<>(Arrays.asList(resultWords));
                
                for (String word : addWords) {
                    System.out.println("Set block: " + word);
                    if (!allWords.contains(word)) {
                        allWords.add(word);
                    }
                }
                result = String.join("\n", allWords);
            }
            
            if (matcher2.find()) {
                LinkedHashSet<String> allWords = new LinkedHashSet<>(Arrays.asList(resultWords));
                String temp;
                
                do {
                    temp = matcher2.group();
                    temp = temp.trim().replace("\n", "").replace("\n", "");
                    if (temp.startsWith("--") || temp.startsWith("/*")) {
                        continue;
                    }
                    else if (!allWords.contains(temp)) {
                        allWords.add(temp);
                    }
                } while (matcher2.find());
                result = String.join("\n", allWords);
            }
            
        }
        return result.isEmpty() ? "" : result + "\n";
    }


    //*************Confirmed
    public static String parseSQLFileName(PromoForm promoForm) {
        Pattern patternDescription = Pattern.compile("([a-zA-Z]{1,15}-\\d{1,10}): (.+)");
        Matcher matcherDescription;
        String jiraTicket;
        matcherDescription = patternDescription.matcher(promoForm.getDescription());
        if (matcherDescription.find()) {
            jiraTicket = matcherDescription.group(1);
            return "Verify_" + promoForm.getSummary().trim() + "_" + jiraTicket.trim() + ".sql";
        }
        return "";
    }
}
