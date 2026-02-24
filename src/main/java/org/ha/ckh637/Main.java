package org.ha.ckh637;

import java.io.IOException;

import org.ha.ckh637.config.SingletonConfig;
import org.ha.ckh637.service.AppIniService;
import org.ha.ckh637.service.EmailService;
import org.ha.ckh637.service.PayloadHandler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Main {
    public static void main(String[] args) throws IOException {
        AppIniService.readJsonConfigFile(args);
//        PayloadHandler.handleGetUrgSerSpeExcel("lwl526@ho.ha.org.hk");
        SpringApplication.run(Main.class, args);
//        System.out.println("Hello, World!");
    }
}