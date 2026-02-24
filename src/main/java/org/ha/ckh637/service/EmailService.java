package org.ha.ckh637.service;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import org.ha.ckh637.component.CachedData;
import org.ha.ckh637.component.DataCenter;
import org.ha.ckh637.component.EmailHTML;
import org.ha.ckh637.component.PromoForm;
import org.ha.ckh637.config.SingletonConfig;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class EmailService {
    private EmailService(){}
    private static final DataCenter DATA_CENTER = DataCenter.getInstance();
    private static final ReentrantReadWriteLock.ReadLock READ_LOCK = ConcurencyControl.getREAD_LOCK();

    public static boolean sendBiweeklyEmailWithAttachment(final String email_address, final String year_batch, final CachedData cachedData){
        try{
            Dispatch mail = Dispatch
                    .invoke(new ActiveXComponent("Outlook.Application"),
                            "CreateItem",
                            Dispatch.Get,
                            new Object[]{"0"},
                            new int[0])
                    .toDispatch();

            SingletonConfig singletonConfig = SingletonConfig.getInstance();
            Dispatch.put(mail, "Subject", singletonConfig.getEmailSubjectBiweekly(year_batch));
            Dispatch.put(mail, "To", email_address);
            String emailContent = cachedData.getEmailContent();
            String attachmentPath = cachedData.getAttachmentPath();
            Dispatch.put(mail, "HTMLBody", emailContent);
            // Attach a document
            Dispatch attachments = Dispatch.get(mail, "Attachments").toDispatch();
            Dispatch.call(attachments, "Add", attachmentPath);
            // Set reminder properties
            Dispatch.put(mail, "ReminderSet", false);
            Dispatch.call(mail, "Send");
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public static boolean sendUrgentServiceEmail_V2(final String email_address, final String cachedUrgSerSpeEmailHTML){
        try{
            Dispatch mail = Dispatch
                    .invoke(new ActiveXComponent("Outlook.Application"),
                            "CreateItem",
                            Dispatch.Get,
                            new Object[]{"0"},
                            new int[0])
                    .toDispatch();

            SingletonConfig singletonConfig = SingletonConfig.getInstance();
            Dispatch.put(mail, "Subject", singletonConfig.getEmailSubjectUrgentService());
            Dispatch.put(mail, "To", email_address);

            ////////////////////////////////////////////
            Dispatch.put(mail, "HTMLBody", cachedUrgSerSpeEmailHTML);
            //////////////////////////////////////////

            // Set reminder properties
            Dispatch.put(mail, "ReminderSet", false);
            Dispatch.call(mail, "Send");
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public static boolean sendEmailWithExcelAttachment(final String email_address, byte[] bytes){
        try{
            Dispatch mail = Dispatch
                    .invoke(new ActiveXComponent("Outlook.Application"),
                            "CreateItem",
                            Dispatch.Get,
                            new Object[]{"0"},
                            new int[0])
                    .toDispatch();

            Dispatch.put(mail, "Subject", "IMP JIRA URGENT & SERVICE Promotions Daily Updates(Excel)");
            Dispatch.put(mail, "To", email_address);

            String htmlContent = "<html><body>"
                            + "<p>Auto Update Google Sheet: <a href=\"https://docs.google.com/spreadsheets/d/1L9I3LOIz90VEvmWclOC8fGrPxtgTuf8R2pnUMZVjvkU/edit?gid=0#gid=0\">https://docs.google.com/spreadsheets/d/1L9I3LOIz90VEvmWclOC8fGrPxtgTuf8R2pnUMZVjvkU/edit?gid=0#gid=0</a></p></body></html>";
            Dispatch.put(mail, "HTMLBody", htmlContent);

            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String formattedDate = today.format(formatter);

            java.nio.file.Path tempFilePath = java.nio.file.Files.createTempFile("temp", ".xlsx");
            java.nio.file.Path renamedFilePath = tempFilePath.resolveSibling("PromotionExcel" + formattedDate + ".xlsx");

            java.nio.file.Files.write(renamedFilePath, bytes);


            Dispatch attachments = Dispatch.get(mail, "Attachments").toDispatch();
            Dispatch.call(attachments, "Add", renamedFilePath.toFile().getAbsolutePath());
            
            Dispatch.put(mail, "ReminderSet", true);
            Dispatch.call(mail, "Send");

            // java.nio.file.Files.write(tempFilePath, bytes);
            // Attach a document

            return true;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    public static boolean sendEmailWithExcelAttachment5pm(List<String> email_addresses, byte[] bytes){
        try{
            Dispatch mail = Dispatch
                    .invoke(new ActiveXComponent("Outlook.Application"),
                            "CreateItem",
                            Dispatch.Get,
                            new Object[]{"0"},
                            new int[0])
                    .toDispatch();

            Dispatch.put(mail, "Subject", "IMP JIRA URGENT & SERVICE Promotions Daily Updates(Excel)");
            String email_address = String.join("; ", email_addresses);
            Dispatch.put(mail, "To", email_address);

            String htmlContent = "<html><body>"
                            + "<p>Auto Update Google Sheet: <a href=\"https://docs.google.com/spreadsheets/d/1L9I3LOIz90VEvmWclOC8fGrPxtgTuf8R2pnUMZVjvkU/edit?gid=0#gid=0\">https://docs.google.com/spreadsheets/d/1L9I3LOIz90VEvmWclOC8fGrPxtgTuf8R2pnUMZVjvkU/edit?gid=0#gid=0</a></p></body></html>";
            Dispatch.put(mail, "HTMLBody", htmlContent);

            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String formattedDate = today.format(formatter);

            java.nio.file.Path tempFilePath = java.nio.file.Files.createTempFile("temp", ".xlsx");
            java.nio.file.Path renamedFilePath = tempFilePath.resolveSibling("PromotionExcel" + formattedDate + ".xlsx");

            java.nio.file.Files.write(renamedFilePath, bytes);


            Dispatch attachments = Dispatch.get(mail, "Attachments").toDispatch();
            Dispatch.call(attachments, "Add", renamedFilePath.toFile().getAbsolutePath());
            
            Dispatch.put(mail, "ReminderSet", true);
            Dispatch.call(mail, "Send");

            // java.nio.file.Files.write(tempFilePath, bytes);
            // Attach a document

            return true;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}

    // public static boolean sendEmail(byte[] bytes){
    //     try{
    //         Dispatch mail = Dispatch
    //                 .invoke(new ActiveXComponent("Outlook.Application"),
    //                         "CreateItem",
    //                         Dispatch.Get,
    //                         new Object[]{"0"},
    //                         new int[0])
    //                 .toDispatch();

    //         Dispatch.put(mail, "Subject", "haha");
    //         Dispatch.put(mail, "To", "hahaha@gmail.com");

    //         java.nio.file.Path tempFilePath = java.nio.file.Files.createTempFile("temp", ".xlsx");
    //         java.nio.file.Files.write(tempFilePath, bytes);
    //         // Attach a document
    //         Dispatch attachments = Dispatch.get(mail, "Attachments").toDispatch();
    //         Dispatch.call(attachments, "Add", tempFilePath.toFile().getAbsolutePath());
    //         // Set reminder properties
    //         Dispatch.put(mail, "ReminderSet", false);
    //         Dispatch.call(mail, "Send");
    //         return true;
    //     }catch (Exception e){
    //         return false;
    //     }
    // }
