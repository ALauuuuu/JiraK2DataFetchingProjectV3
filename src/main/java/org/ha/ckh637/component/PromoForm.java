package org.ha.ckh637.component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PromoForm implements Comparable<PromoForm> {
    private String targetDate = "";
    private String affectedHosp = "";
    private String key_ITOCMS = "";
    private String summary = "";
    private String description = "";
    private String k2FormLink = "";
    private String k2FormNo = "";
    private String impHospSql = "";
    private List<String> types = new ArrayList<>();
    private List<String> impManualItems = new ArrayList<>();
    private boolean isImpHospOrImpCorp = false;
    private String status = "";

    private List<String> allTickets = new ArrayList<>();
    private Map<String, Set<String>> endingTicketRelationshipMap = new LinkedHashMap<>();
    private Map<String, String> endingTicketSummaryMap = new HashMap<>();

    private String concatenatedReadmePromoName = "";
    private String concatenatedRelationshipString = "";

    public PromoForm targetDate(String targetDate){
        this.targetDate = targetDate;
        return this;
    }

    public PromoForm affectedHosp(String affectedHosp){
        this.affectedHosp = affectedHosp;
        return this;
    }

    public PromoForm key_ITOCMS(String key_ITOCMS){
        this.key_ITOCMS = key_ITOCMS;
        return this;
    }

    public PromoForm summary(String summary){
        this.summary = summary;
        return this;
    }

    public PromoForm description(String description){
        this.description = description;
        return this;
    }

    public PromoForm k2FormLink(String k2FormLink){
        this.k2FormLink = k2FormLink;
        return this;
    }

    public PromoForm k2FormNo(String k2FormNo){
        this.k2FormNo = k2FormNo;
        return this;
    }
    
    public PromoForm impHospSql(String impHospSql) {
        this.impHospSql = impHospSql;
        return this;
    }

    public PromoForm types(List<String> types){
        this.types = types;
        return this;
    }

    public PromoForm status(String status){
        this.status = status;
        return this;
    }

    public PromoForm allTickets(List<String> allTickets){
        this.allTickets = allTickets;
        return this;
    }

    public PromoForm endingTicketRelationshipMap(Map<String, Set<String>> endingTicketRelationshipMap){
        this.endingTicketRelationshipMap = endingTicketRelationshipMap;
        return this;
    }

    public PromoForm endingTicketSummaryMap(Map<String, String> endingTicketPPMSummaryMap){
        this.endingTicketSummaryMap = endingTicketPPMSummaryMap;
        return this;
    }

    public PromoForm concatenatedReadmePromoName(String concatenatedReadmePromoName){
        this.concatenatedReadmePromoName = concatenatedReadmePromoName;
        return this;
    }

    public PromoForm concatenatedRelationshipString(String concatenatedRelationshipString){
        this.concatenatedRelationshipString = concatenatedRelationshipString;
        return this;
    }

    public PromoForm isImpHospOrImpCorp(boolean isImpHospOrImpCorp){
        this.isImpHospOrImpCorp = isImpHospOrImpCorp;
        return this;
    }

    public PromoForm addImpManualItems(String impManualItem){
        this.impManualItems.add(impManualItem);
        return this;
    }

    public PromoForm addImpManualItems(List<String> impManualItems){
        this.impManualItems.addAll(impManualItems);
        return this;
    }

    @Override
    public int compareTo(PromoForm other) {
        return this.summary.compareTo(other.summary);
    }

    public boolean isActivePromotion(){
        return !(this.status.equalsIgnoreCase("Withdrawn") || this.status.equalsIgnoreCase("Rejected"));
    }
}
