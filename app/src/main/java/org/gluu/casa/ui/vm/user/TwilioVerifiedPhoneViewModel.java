/*
 * casa available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2019, Gluu
 */
package org.gluu.casa.ui.vm.user;

import org.gluu.casa.misc.Utils;
import org.gluu.casa.plugins.authnmethod.service.TwilioMobilePhoneService;
import org.gluu.casa.plugins.authnmethod.OTPTwilioExtension;
import org.zkoss.bind.annotation.Init;

/**
 * This is the ViewModel of page twilio-phone-detail.zul. It controls the CRUD of verified phones
 */
public class TwilioVerifiedPhoneViewModel extends VerifiedPhoneViewModel {

    public TwilioVerifiedPhoneViewModel() {
        mpService = Utils.managedBean(TwilioMobilePhoneService.class);
        ACR = OTPTwilioExtension.ACR;
    }

    @Init(superclass = true)
    //This dummy method allows parents' init methods to be called
    //Beware: do not change the name of this method to childInit or init (see ZK MVVM book)
    public void subChildInit() { }

}
