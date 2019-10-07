/*
 * casa available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2019, Gluu
 */
package org.gluu.casa.ui.vm.user;

import org.gluu.casa.misc.Utils;
import org.gluu.casa.plugins.authnmethod.service.SmppMobilePhoneService;
import org.gluu.casa.plugins.authnmethod.OTPSmppExtension;
import org.zkoss.bind.annotation.Init;

/**
 * This is the ViewModel of page smpp-phone-detail.zul. It controls the CRUD of verified phones
 */
public class SmppVerifiedPhoneViewModel extends VerifiedPhoneViewModel {

    public SmppVerifiedPhoneViewModel() {
        mpService = Utils.managedBean(SmppMobilePhoneService.class);
        ACR = OTPSmppExtension.ACR;
    }

    @Init(superclass = true)
    //This dummy method allows parents' init methods to be called
    //Beware: do not change the name of this method to childInit or init (see ZK MVVM book)
    public void subChildInit() { }

}
