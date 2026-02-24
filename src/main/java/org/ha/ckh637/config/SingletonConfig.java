package org.ha.ckh637.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public final class SingletonConfig {
    private SingletonConfig(){}
    private static SingletonConfig singletonAPIConfig = new SingletonConfig();
    @JsonIgnore
    private static String iniInputPath;
//    private static File jsonFile;
    public static SingletonConfig getInstance(){return singletonAPIConfig;}

    @Getter
    @Setter
    @NoArgsConstructor
    private class Admin{
        private String username;
        private String password;
        private long cache_time_min;

        @Override
        public String toString(){
            return "\nusername: " + username
                    +"\npassword: " + "*".repeat(password.length())
                    +"\ncache_time_min: " + cache_time_min;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private class APIConfig{
        private String jiraAPI;
        private String jql_urgent_service;
        private String jql_biweekly_prn;
        private String jql_biweekly_urgent_service;
        private String jiraFields;
        private String jfrogAPI;

        @Override
        public String toString(){
            return "\njiraAPI: " + jiraAPI
                    + "\njql_urgent_service: " + jql_urgent_service
                    + "\njql_biweekly_prn: " + jql_biweekly_prn
                    + "\njql_biweekly_urgent_service: " + jql_biweekly_urgent_service
                    + "\njiraFields: " + jiraFields
                    + "\njfrogAPI: " + jfrogAPI;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private class EmailConfig{
        private String subject_urgent_service;
        private String subject_biweekly;

        @Override
        public String toString(){
            return "\nsubject_urgent_service: " + subject_urgent_service
                    +"\nsubject_biweekly: " + subject_biweekly;
        }
    }

    private Admin admin = new Admin();
    private APIConfig apiConfig = new APIConfig();
    private EmailConfig emailConfig = new EmailConfig();
    @Getter
    private List<String> hospList = new ArrayList<>();
    @Getter
    private List<String> recipients = new ArrayList<>();

    @Override
    public String toString(){
        return "Admin: " + admin
                + "\n\nAPIConfig: " + apiConfig
                + "\n\nEmailConfig: " + emailConfig
                + "\n\nHospList: " + hospList
                + "\n\nRecipients: " + recipients;
    }

    /*public static void setJsonFile(File jsonFile){SingletonConfig.jsonFile = jsonFile;}
    public static File getJsonFile(){return SingletonConfig.jsonFile;}*/
    public static void setIniInputPath(String path){SingletonConfig.iniInputPath = path;}
    public static String getIniInputPath(){return SingletonConfig.iniInputPath;}

    public static void updateSingletonConfig(SingletonConfig newSingletonConfig){
        singletonAPIConfig = newSingletonConfig;
    }

    public String getAdminUsername(){return admin.username;}
    public String getAdminPassword(){return admin.password;}
    public long getAdminCacheTimeMin(){return admin.cache_time_min;}

    public String getJiraRestAPI(){return apiConfig.jiraAPI;}
    public String getFullJiraAPIUrgentService(){return apiConfig.jiraAPI + apiConfig.jql_urgent_service + apiConfig.jiraFields;}
    public String getRawJiraAPIBiweeklyPrn(){return apiConfig.jiraAPI + apiConfig.jql_biweekly_prn + apiConfig.jiraFields;}
    public String getRawJiraAPIBiweeklyUrgentService(){return apiConfig.jiraAPI + apiConfig.jql_biweekly_urgent_service + apiConfig.jiraFields;}

    public String getJfrogAPI(){return apiConfig.jfrogAPI;}

    public String getEmailSubjectUrgentService(){return emailConfig.subject_urgent_service;}
    public String getEmailSubjectBiweekly(final String year_batch){
        return String.format(emailConfig.subject_biweekly, year_batch);
    }

    public String getJiraFields(){return apiConfig.jiraFields;}

}
