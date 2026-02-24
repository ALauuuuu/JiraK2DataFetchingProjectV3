package org.ha.ckh637.controller;

import org.ha.ckh637.service.EmailService;
import org.ha.ckh637.service.PayloadHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Controller
@CrossOrigin
@RequestMapping(value = "/api")
public class RestController {

    @ResponseBody
    @GetMapping(value = "/v1/get-zip-by-batch/{year_batch}", produces = "application/zip")
    public byte[] getZipByBatch(@PathVariable("year_batch") String year_batch) {
        return PayloadHandler.handleGetZipByBatch(year_batch);
    }

    @ResponseBody
    @GetMapping(value = "/v1/receive-biweekly-email/email={email_address}&batch={year_batch}")
    public Map<String, String> receiveBiweeklyEmail(@PathVariable("email_address") String email_address, @PathVariable("year_batch") String year_batch) {
        return PayloadHandler.handleReceiveBiweeklyEmail(email_address, year_batch);
    }

    @ResponseBody
    @GetMapping(value = "/v1/receive-urgent-service-special-email/email={email_address}")
    public Map<String, String> receiveUrgSerSpecEmail(@PathVariable("email_address") String email_address) {
        return PayloadHandler.handleReceiveUrgSerSpeEmail(email_address);
    }

    @ResponseBody
    @GetMapping(value = "/v1/get-urgent-service-special-excel/email={email_address}", produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public byte[] getUrgSerSpecExcel(@PathVariable("email_address") String email_address) throws IOException {
        return PayloadHandler.handleGetUrgSerSpeExcel(email_address);
    }

    @ResponseBody
    @GetMapping(value = "/v1/get-urgent-service-special-verify-sql-zip", produces="application/zip")
    public byte[] getUrgSerSpecVerifySqlZip() {
        return PayloadHandler.handleGetUrgSerSpeVerifySqlZip();
    }

    @ResponseBody
    @GetMapping(value = "/v1/get-Biweekly-verify-sql-zip", produces="application/zip")
    public byte[] getBiweeklyVerifySqlZip() {
        return PayloadHandler.handleGetBiweeklyVerifySqlZip();
    }
}


