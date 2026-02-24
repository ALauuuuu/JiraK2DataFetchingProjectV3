package org.ha.ckh637.component;

import java.util.*;

import org.apache.logging.log4j.CloseableThreadContext;


public class VerifyScript {
    private static final List<PromoForm> impHospPromo = new ArrayList<>();

    public static void addPromoForm(PromoForm promoForm) {
        impHospPromo.add(promoForm);
    }

    public static List<PromoForm> getImpHospPromo() {
        return impHospPromo;
    }
}
