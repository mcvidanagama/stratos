/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.stratos.manager.test;

import junit.framework.TestCase;
import org.apache.stratos.cloud.controller.stub.domain.CartridgeInfo;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.factory.CartridgeSubscriptionFactory;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionMultiTenantBehaviour;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionSingleTenantBehaviour;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionTenancyBehaviour;

public class CartridgeSubscriptionTest extends TestCase {

    private CartridgeSubscription getCartridgeInstance (CartridgeInfo cartridgeInfo) {

        SubscriptionTenancyBehaviour tenancyBehaviour;
        if(cartridgeInfo.getMultiTenant()) {
            tenancyBehaviour = new SubscriptionMultiTenantBehaviour();
        } else {
            tenancyBehaviour = new SubscriptionSingleTenantBehaviour();
        }

        try {
            return CartridgeSubscriptionFactory.getCartridgeSubscriptionInstance(cartridgeInfo, tenancyBehaviour);

        } catch (ADCException e) {
            throw new RuntimeException(e);
        }
    }

    public void testCarbonCartridgeSubscription() {

        CartridgeInfo cartridgeInfo1 = new CartridgeInfo();
        cartridgeInfo1.setProvider("carbon");
        cartridgeInfo1.setMultiTenant(true);
        cartridgeInfo1.setType("esb");
        CartridgeSubscription cartridgeSubscription1 = getCartridgeInstance(cartridgeInfo1);
        assertNotNull(cartridgeSubscription1);


        CartridgeInfo cartridgeInfo2 = new CartridgeInfo();
        cartridgeInfo2.setProvider("carbon");
        cartridgeInfo2.setMultiTenant(false);
        cartridgeInfo2.setType("as");
        CartridgeSubscription cartridgeSubscription2 = getCartridgeInstance(cartridgeInfo2);
        assertNotNull(cartridgeSubscription2);
    }

    public void testPhpCartridgeSubscription() {

        CartridgeInfo cartridgeInfo = new CartridgeInfo();
        cartridgeInfo.setProvider("php-provider");
        cartridgeInfo.setMultiTenant(false);
        cartridgeInfo.setType("php");
        CartridgeSubscription cartridgeSubscription = getCartridgeInstance(cartridgeInfo);
        assertNotNull(cartridgeSubscription);
    }

    public void testMySqlCartridgeSubscription() {

        CartridgeInfo cartridgeInfo = new CartridgeInfo();
        cartridgeInfo.setProvider("data");
        cartridgeInfo.setMultiTenant(false);
        cartridgeInfo.setType("mysql");
        CartridgeSubscription cartridgeSubscription = getCartridgeInstance(cartridgeInfo);
        assertNotNull(cartridgeSubscription);
    }

    public void testTomcatCartridgeSubscription() {

        CartridgeInfo cartridgeInfo = new CartridgeInfo();
        cartridgeInfo.setProvider("tomcat-provider");
        cartridgeInfo.setMultiTenant(false);
        cartridgeInfo.setType("tomcat");
        CartridgeSubscription cartridgeSubscription = getCartridgeInstance(cartridgeInfo);
        assertNotNull(cartridgeSubscription);
    }
}
