package org.ha.ckh637.service;

import org.ha.ckh637.component.CachedData;
import org.ha.ckh637.component.EmailHTML;
import org.ha.ckh637.component.ExcelSheet;
import org.ha.ckh637.component.RequestData;
import org.ha.ckh637.component.DataCenter;
import org.ha.ckh637.utils.JsonDataParser;

import reactor.core.publisher.Mono;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.poi.ss.usermodel.Workbook;

public final class PayloadHandler {
    private PayloadHandler(){}
    private static final DataCenter DATA_CENTER = DataCenter.getInstance();
    private static final ReentrantReadWriteLock.ReadLock READ_LOCK = ConcurencyControl.getREAD_LOCK();
    private static final ReentrantReadWriteLock.WriteLock WRITE_LOCK = ConcurencyControl.getWRITE_LOCK();
    private static byte[] cachedExcelData = null;
    private static byte[] cachedSQLData = null;

    public static byte[] handleGetZipByBatch(String payload){
        if (isValidYearBatch(payload)){
            String[] yearBatch = splitYearAndBatch(payload);
            if (isNumeric(yearBatch[0]) && isNumeric(yearBatch[1])){
                String year = yearBatch[0];
                String batch = yearBatch[1].length() == 1 ? "0" + yearBatch[1] : yearBatch[1];
                String year_batch = year + "-" + batch;
                try{
                    // we first lock to access the critical section
                    READ_LOCK.lock();
                    Map<String, CachedData> cachedDataMap = DATA_CENTER.getBatchCachedDataMap();
                    if (cachedDataMap.containsKey(year_batch)){
                        CachedData cachedData = cachedDataMap.get(year_batch);
                        return cachedData.getAttachment();
                    }else{
                        // if we can't find the cached data, we unlock the read lock and lock the write lock
                        try{
                            READ_LOCK.unlock();
                            WRITE_LOCK.lock();
                            // here we re-try to get the cached data
                            // because there is a chance that a thread was put to
                            // sleep at the write lock and when it wakes up, cached data
                            // may already be make available by another thread
                            // Doing so avoids unnecessary re-computation e.g. API calls
                            if (cachedDataMap.containsKey(year_batch)){
                                CachedData cachedData = cachedDataMap.get(year_batch);
                                return cachedData.getAttachment();
                            }
                            RequestData requestData = new RequestData(year, batch);
                            eventSequenceBiweekly_V2_multiThreaded(requestData, year_batch);
                        }finally {
                            WRITE_LOCK.unlock();
                        }
                        READ_LOCK.lock();
                        if (cachedDataMap.containsKey(year_batch)){
                            CachedData cachedData = cachedDataMap.get(year_batch);
                            return cachedData.getAttachment();
                        }
                    }
                }finally {
                    READ_LOCK.unlock();
                }
            }
        }
        return null;
    }

    public static byte[] handleGetUrgSerSpeExcel(final String email_address) throws IOException{
        READ_LOCK.lock();
        try {
            if (cachedExcelData != null) {
                return cachedExcelData;
            }
        } finally {
             cachedExcelData = null;
             READ_LOCK.unlock();
        }
        WRITE_LOCK.lock();
        try {
             if (cachedExcelData == null) {
                eventSequenceUrgSerSpeExcel();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                XSSFWorkbook workbook = ExcelSheet.getInstance().getResult();
                
                workbook.write(bos);
                    
                cachedExcelData = bos.toByteArray();

                EmailService.sendEmailWithExcelAttachment(email_address, cachedExcelData);
                // ExcelEmailService.sendEmailWithExcelAttachment(cachedExcelData);

                ExcelSheet.getInstance().clearWorkbookContent(ExcelSheet.getInstance().getResult());
                bos.close();
             }
            
            return cachedExcelData;
        } finally {
            cachedExcelData = null;
            WRITE_LOCK.unlock();
        }
        // eventSequenceUrgSerSpeExcel();
        // ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // XSSFWorkbook workbook = ExcelSheet.getInstance().getResult();
        
        // workbook.write(bos);
            
        // byte[] byteArray = bos.toByteArray();
            

        // bos.close();

        // return byteArray;
    }

    public static byte[] handleGetUrgSerSpeVerifySqlZip() {
        READ_LOCK.lock();
        try {
            if (cachedExcelData != null) {
                return cachedExcelData;
            }
        } finally {
        cachedSQLData = null;
        READ_LOCK.unlock();
        }
        WRITE_LOCK.lock();
        try{
            if (cachedSQLData == null) {
                eventSquenceUrgSerSpeSQL();
                return cachedSQLData;
            }
        } finally {
            WRITE_LOCK.unlock();
        }
        return null;
    }

    public static Map<String, String> handleReceiveUrgSerSpeEmail(final String email_address){
        try{
            if (isInvalidEmail(email_address)) {
                return Map.of("status", "error",
                        "message", "\"" + email_address + "\"" + "is an invalid email address.");
            }
            try{
                READ_LOCK.lock();
                String cachedUrgSerSpeEmailHTML = DATA_CENTER.getCachedUrgSerSpeEmailHTML();
                if (!cachedUrgSerSpeEmailHTML.isBlank()){
                    if (EmailService.sendUrgentServiceEmail_V2(email_address, cachedUrgSerSpeEmailHTML)){
                        return Map.of("status", "success",
                                "message", "Success! Please check your email inbox.");
                    }else{
                        return Map.of("status", "error",
                                "message", "Failed to send email. Please try again later.");
                    }
                }else{
                    try{
                        READ_LOCK.unlock();
                        WRITE_LOCK.lock();
                        cachedUrgSerSpeEmailHTML = DATA_CENTER.getCachedUrgSerSpeEmailHTML();
                        if (!cachedUrgSerSpeEmailHTML.isBlank()){
                            if (EmailService.sendUrgentServiceEmail_V2(email_address, cachedUrgSerSpeEmailHTML)){
                                return Map.of("status", "success",
                                        "message", "Success! Please check your email inbox.");
                            }else{
                                return Map.of("status", "error",
                                        "message", "Failed to send email. Please try again later.");
                            }
                        }
                        eventSequenceUrgSerSpe();
                    }finally {
                        WRITE_LOCK.unlock();
                    }
                    READ_LOCK.lock();
                    cachedUrgSerSpeEmailHTML = DATA_CENTER.getCachedUrgSerSpeEmailHTML();
                    if (!cachedUrgSerSpeEmailHTML.isBlank()){
                        if (EmailService.sendUrgentServiceEmail_V2(email_address, cachedUrgSerSpeEmailHTML)){
                            return Map.of("status", "success",
                                    "message", "Success! Please check your email inbox.");
                        }else{
                            return Map.of("status", "error",
                                    "message", "Failed to send email. Please try again later.");
                        }
                    }
                    return Map.of("status", "error",
                            "message", "Failed to send email. Please try again later.");
                }
            }finally {
                READ_LOCK.unlock();
            }
        }catch (Exception e){
            return Map.of("status", "error",
                    "message", "Exception encountered in the backend: " + e.getMessage());
        }
    }


    public static Map<String, String> handleReceiveBiweeklyEmail(String email_address, String year_batch){
        try{
            if (isInvalidEmail(email_address)) {
                return Map.of("status", "error",
                        "message", "\"" + email_address + "\"" + "is an invalid email address.");
            }
            if(isValidYearBatch(year_batch)){
                String[] yearBatch = splitYearAndBatch(year_batch);
                if (isNumeric(yearBatch[0]) && isNumeric(yearBatch[1])){
                    String year = yearBatch[0];
                    String batch = yearBatch[1].length() == 1 ? "0" + yearBatch[1] : yearBatch[1];
                    year_batch = year + "-" + batch;
                    try{
                        READ_LOCK.lock();
                        Map<String, CachedData> cachedDataMap = DATA_CENTER.getBatchCachedDataMap();
                        if (cachedDataMap.containsKey(year_batch)){
                            if (EmailService.sendBiweeklyEmailWithAttachment(email_address, year_batch, cachedDataMap.get(year_batch))){
                                return Map.of("status", "success",
                                        "message", "Success! Please check your email inbox.");
                            }else{
                                return Map.of("status", "error",
                                        "message", "Failed to send email. Please try again later.");
                            }
                        }else{
                            try{
                                READ_LOCK.unlock();
                                WRITE_LOCK.lock();
                                if (cachedDataMap.containsKey(year_batch)){
                                    if (EmailService.sendBiweeklyEmailWithAttachment(email_address, year_batch, cachedDataMap.get(year_batch))){
                                        return Map.of("status", "success",
                                                "message", "Success! Please check your email inbox.");
                                    }else{
                                        return Map.of("status", "error",
                                                "message", "Failed to send email. Please try again later.");
                                    }
                                }
                                RequestData requestData = new RequestData(year, batch);
                                eventSequenceBiweekly_V2_multiThreaded(requestData, year_batch);
//                            return Map.of("status", "success",
//                                    "message", "Success! Please check your email inbox.");
                            }finally {
                                WRITE_LOCK.unlock();
                            }
                            READ_LOCK.lock();
                            if (cachedDataMap.containsKey(year_batch)){
                                if (EmailService.sendBiweeklyEmailWithAttachment(email_address, year_batch, cachedDataMap.get(year_batch))){
                                    return Map.of("status", "success",
                                            "message", "Success! Please check your email inbox.");
                                }else{
                                    return Map.of("status", "error",
                                            "message", "Failed to send email. Please try again later.");
                                }
                            }
                        }
                    }finally {
                        READ_LOCK.unlock();
                    }
                }
                return Map.of("status", "error",
                        "message", "Payload year-batch is of incorrect format.");
            }
            return Map.of("status", "error",
                    "message", "Payload year-batch is of incorrect format.");
        } catch (Exception e) {
            return Map.of("status", "error",
                    "message", "Exception encountered in the backend: " + e.getMessage());
        }
    }

    public static byte[] handleGetBiweeklyVerifySqlZip() {
        return null;
    }

    private static boolean isInvalidEmail(String email){
        return !(email.contains("@") && email.contains("."));
    }

    private static boolean isValidYearBatch(String year_batch){
        return year_batch.contains("-");
    }

    private static String[] splitYearAndBatch(String year_batch){
        return year_batch.split("-");
    }

    private static boolean isNumeric(String input){
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void eventSequenceUrgSerSpe(){
        DATA_CENTER.clearAllApiData();
        Thread t1 = new Thread(() -> {
            Mono<String> monoJiraResp = APIQueryService.fetchJiraUrgentServiceAPI_V2();
            JsonDataParser.parseStandardJiraResp(null, monoJiraResp, false, false);
        });
        t1.start();
        try{
            t1.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        String finalContent = EmailHTML.compileEmailHTMLContent(true);
        DATA_CENTER.setCachedUrgSerSpeEmailHTML(finalContent);
        DATA_CENTER.clearAllApiData();
        new BackgroundService().scheduleDeleteUrgSerSpeCachedData();
    }


    public static void eventSequenceUrgSerSpeExcel(){
        DATA_CENTER.clearAllApiData();
        Thread t1 = new Thread(() -> {
            Mono<String> monoJiraResp = APIQueryService.fetchJiraUrgentServiceAPI_V2();
            // JsonDataParser.parseJiraUrgentServiceForBiweeklyResp(monoJiraResp);
            JsonDataParser.parseStandardJiraResp(null, monoJiraResp, false, false);
        });
        t1.start();
        try{
            t1.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        EmailHTML.compileEmailHTMLContentExcel(true);

        DATA_CENTER.clearAllApiData();

        // DATA_CENTER.setCachedUrgSerSpeEmailHTML(finalContent);
        // DATA_CENTER.clearAllApiData();
        // new BackgroundService().scheduleDeleteUrgSerSpeCachedData();

    }

    public static void eventSquenceUrgSerSpeSQL() {
        DATA_CENTER.clearAllApiData();
        Thread t1 = new Thread( () -> {
            Mono<String> monoJiraResp = APIQueryService.fetchJiraUrgentServiceAPI_V2();
            // JsonDataParser.parseJiraUrgentServiceForBiweeklyResp(monoJiraResp);
            JsonDataParser.parseStandardJiraResp(null, monoJiraResp, false, true);
        });
        t1.start();
        try {
            t1.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            cachedSQLData = ZipService.compressSQLFileToZip();
        } catch (IOException e) {
            e.printStackTrace();
            cachedSQLData = null;
        }
        DATA_CENTER.clearAllApiData();
    }


    public static void eventSquenceBiweeklySQL() {
        DATA_CENTER.clearAllApiData();
        Thread t1 = new Thread( () -> {
            Mono<String> monoJiraResp = APIQueryService.fetchJiraUrgentServiceAPI_V2();
            // JsonDataParser.parseJiraUrgentServiceForBiweeklyResp(monoJiraResp);
            JsonDataParser.parseStandardJiraResp(null, monoJiraResp, false, true);
        });
        t1.start();
        try {
            t1.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            cachedSQLData = ZipService.compressSQLFileToZip();
        } catch (IOException e) {
            e.printStackTrace();
            cachedSQLData = null;
        }
        DATA_CENTER.clearAllApiData();
    }


    private static void eventSequenceBiweekly_V2_multiThreaded(RequestData requestData, final String year_batch) {
        // Make sure no trailing data left from previous computation
        DATA_CENTER.clearAllApiData();
        Thread t1 = new Thread(() -> {
            Mono<String> monoJiraResp = APIQueryService.fetchJiraBiweeklyAPI_V2(requestData);
            JsonDataParser.parseStandardJiraResp(year_batch, monoJiraResp, true, false);
        });
        Thread t2 = new Thread(() -> {
            Mono<String> monoJiraResp = APIQueryService.fetchJiraUrgentServiceForBiweeklyAPI_V2(requestData);
            JsonDataParser.parseJiraUrgentServiceForBiweeklyResp(monoJiraResp);
        });
        t1.start(); t2.start();
        try {
            t1.join(); t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread t3 = new Thread(() -> ReadmeService.createReadmeFile(year_batch, ReadmeService.genReadmeContent()));
        Thread t4 = new Thread(()->{
            String part1 = CompileSeqService.compilePart1Forwarder();
            String part2 = CompileSeqService.compilePart2BackendScript();
            String part3 = CompileSeqService.compilePart3UrgentServicePromotion();
            CompileSeqService.createCompileSeqFile(year_batch, CompileSeqService.genCompileSeqContent(year_batch, part1, part2, part3));
        });
        Thread t5 = new Thread(() -> UrlService.genAllUrlFiles_V2(year_batch));
        t3.start(); t4.start(); t5.start();
        try {
            t3.join(); t4.join(); t5.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ZipService.compressFileToZip(year_batch);
        String emailContent = EmailHTML.compileEmailHTMLContent(false);
        String targetZip = ZipService.getZipFilePath(year_batch);
        byte[] zipFileByte;
        try{
            zipFileByte = Files.readAllBytes(Paths.get(targetZip));
        }catch (IOException e){
            zipFileByte = null;
        }
        DATA_CENTER.getBatchCachedDataMap().put(year_batch, new CachedData(emailContent, targetZip, zipFileByte));
        // Once we have cached the finalized data, we can clear the original source api data
        DATA_CENTER.clearAllApiData();
        // With the final zip created, we can safely delete the
        // temporary directory for holding the various files
        DirectoryService.delDir(year_batch);
        // Schedule to delete the cached data to ensure freshness
        new BackgroundService().scheduleDeleteBiweeklyCachedData(year_batch);
    }


}
