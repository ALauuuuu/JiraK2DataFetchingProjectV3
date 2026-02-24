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
import org.ha.ckh637.config.SingletonConfig;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class ScheduleService {
    private ScheduleService(){}
    // private static final PromoReleaseEmailConfig PROMO_RELEASE_EMAIL_CONFIG = PromoReleaseEmailConfig.getInstance();
    private static final DataCenter DATA_CENTER = DataCenter.getInstance();
    private static final ReentrantReadWriteLock.ReadLock READ_LOCK = ConcurencyControl.getREAD_LOCK();
    private static final ReentrantReadWriteLock.WriteLock WRITE_LOCK = ConcurencyControl.getWRITE_LOCK();
    private static byte[] cachedExcelData = null;

    /////////////////////////// V2 ///////////////////////////
    // Start at 17:00 every day
    @Scheduled(cron = "0 0 17 * * *")
    public static void sendUrgentServiceExcelEmailDaily() throws IOException{
        WRITE_LOCK.lock();
        try {
            if (cachedExcelData == null) {
                PayloadHandler.eventSequenceUrgSerSpeExcel();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                XSSFWorkbook workbook = ExcelSheet.getInstance().getResult();
                
                workbook.write(bos);
                    
                cachedExcelData = bos.toByteArray();

                EmailService.sendEmailWithExcelAttachment5pm(SingletonConfig.getInstance().getRecipients(), cachedExcelData);

                cachedExcelData = null;
                ExcelSheet.getInstance().clearWorkbookContent(ExcelSheet.getInstance().getResult());
                bos.close();
            }
            
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    // @Scheduled(cron = "0 48 14 * * *")
    // public static void sendUrgentServiceExcelEmailDaily2() throws IOException{
    //     WRITE_LOCK.lock();
    //     try {
    //         if (cachedExcelData == null) {
    //             PayloadHandler.eventSequenceUrgSerSpeExcel();
    //             ByteArrayOutputStream bos = new ByteArrayOutputStream();
    //             XSSFWorkbook workbook = ExcelSheet.getInstance().getResult();

    //             workbook.write(bos);

    //             cachedExcelData = bos.toByteArray();

    //             EmailService.sendEmailWithExcelAttachment(cachedExcelData);


    //             cachedExcelData = null;
    //             ExcelSheet.getInstance().clearWorkbookContent(ExcelSheet.getInstance().getResult());
    //             bos.close();
    //         }

    //     } finally {
    //         WRITE_LOCK.unlock();
    //     }
    // }

    // @Scheduled(cron = "0 49 14 * * *")
    // public static void sendUrgentServiceExcelEmailDaily3() throws IOException{
    //     WRITE_LOCK.lock();
    //     try {
    //         if (cachedExcelData == null) {
    //             PayloadHandler.eventSequenceUrgSerSpeExcel();
    //             ByteArrayOutputStream bos = new ByteArrayOutputStream();
    //             XSSFWorkbook workbook = ExcelSheet.getInstance().getResult();

    //             workbook.write(bos);

    //             cachedExcelData = bos.toByteArray();

    //             EmailService.sendEmailWithExcelAttachment(cachedExcelData);


    //             cachedExcelData = null;
    //             ExcelSheet.getInstance().clearWorkbookContent(ExcelSheet.getInstance().getResult());
    //             bos.close();
    //         }

    //     } finally {
    //         WRITE_LOCK.unlock();
    //     }
    // }

}
