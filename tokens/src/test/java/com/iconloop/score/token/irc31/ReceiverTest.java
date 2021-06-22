/*
 * Copyright 2021 ICONation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iconloop.score.token.irc31;

import com.iconloop.testsvc.Account;
import com.iconloop.testsvc.Score;
import com.iconloop.testsvc.ServiceManager;
import com.iconloop.testsvc.TestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import score.Address;

public class ReceiverTest extends TestBase {
    
    protected static final ServiceManager sm = getServiceManager();
    protected static final Account owner = sm.createAccount();
    protected static final Account minter = sm.createAccount();
    protected static final Account burner = sm.createAccount();
    protected static final Account alice = sm.createAccount();
    protected static final Account bob = sm.createAccount();
    protected static final Account eve = sm.createAccount();
    protected static final BigInteger EXA = BigInteger.TEN.pow(18);

    protected static Score scoreRcv;
    protected static TokenReceiver spyRcv;

    protected static Score scoreToken;
    protected static IRC31MinBurnToken spyToken;
    
    void receiver_setup () throws Exception {
        scoreRcv = sm.deploy(owner, TokenReceiver.class);
        spyRcv = (TokenReceiver) spy(scoreRcv.getInstance());
        scoreRcv.setInstance(spyRcv);
    }

    void token_setup () throws Exception {
      scoreToken = sm.deploy(owner, IRC31MinBurnToken.class);
      spyToken = (IRC31MinBurnToken) spy(scoreToken.getInstance());
      scoreToken.setInstance(spyToken);
    }
  
    BigInteger mint_token (BigInteger supply) {
      BigInteger newId = getTokenId();
      String uri = "https://craft.network/" + newId;
      scoreToken.invoke(owner, "mint", newId, supply, uri);
      return newId;
    }

    BigInteger getTokenId () {
        return BigInteger.valueOf((int) (Math.random() * 1000000));
    }

    @BeforeEach
    void setup() throws Exception {
      receiver_setup();
      token_setup();
      reset(spyRcv);
      reset(spyToken);
    }
    
    @Test
    void testSetOriginator() {
      scoreRcv.invoke(owner, "setOriginator", scoreToken.getAddress(), true);
    }
    
    @Test
    void testSetOriginatorNotOwner() {
      assertThrows(AssertionError.class, () -> 
        scoreRcv.invoke(eve, "setOriginator", scoreToken.getAddress(), true));
    }
    
    @Test
    void testSetOriginatorInvalidContractAddress() {
      assertThrows(AssertionError.class, () -> 
        scoreRcv.invoke(owner, "setOriginator", alice.getAddress(), true));
    }
    
    @Test
    void testOnReceived() throws Exception {

      BigInteger supply = BigInteger.valueOf(100);
      BigInteger newSupply = supply.divide(BigInteger.TWO);
      BigInteger newId = mint_token(supply);
      reset(spyToken);

      byte[] data = "Hello".getBytes();

      scoreRcv.invoke(owner, "setOriginator", scoreToken.getAddress(), true);

      scoreToken.invoke(owner, "transferFrom", 
        owner.getAddress(), 
        scoreRcv.getAddress(), 
        newId, 
        newSupply,
        data);
        
      /*
        @EventLog(indexed=3)
        public void TransferSingle(
            Address _operator, 
            Address _from, 
            Address _to, 
            Integer _id, 
            BigInteger _value) {}
      */
      ArgumentCaptor<Address> operator = ArgumentCaptor.forClass(Address.class);
      ArgumentCaptor<Address> from = ArgumentCaptor.forClass(Address.class);
      ArgumentCaptor<Address> to = ArgumentCaptor.forClass(Address.class);
      ArgumentCaptor<BigInteger> id = ArgumentCaptor.forClass(BigInteger.class);
      ArgumentCaptor<BigInteger> value = ArgumentCaptor.forClass(BigInteger.class);

      verify(spyToken).TransferSingle(
          operator.capture(),
          from.capture(),
          to.capture(),
          id.capture(),
          value.capture());
      
      // Check TransferSingle event
      assertEquals(operator.getValue(), owner.getAddress());
      assertEquals(from.getValue(), owner.getAddress());
      assertEquals(to.getValue(), scoreRcv.getAddress());
      assertEquals(id.getValue(), newId);
      assertEquals(value.getValue(), newSupply);

      /*
        @EventLog(indexed=3)
        public void IRC31Received(
            Address _origin,  
            Address _operator, 
            Address _from, 
            Integer _id, 
            BigInteger _value, 
            byte[] _data) {}
      */
      ArgumentCaptor<Address> origin = ArgumentCaptor.forClass(Address.class);
      ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);

      verify(spyRcv).IRC31Received(
          origin.capture(),
          operator.capture(),
          from.capture(),
          id.capture(),
          value.capture(),
          dataCaptor.capture());
      
      // Check IRC31Received event
      assertEquals(origin.getValue(), scoreToken.getAddress());
      assertEquals(operator.getValue(), owner.getAddress());
      assertEquals(from.getValue(), owner.getAddress());
      assertEquals(id.getValue(), newId);
      assertEquals(value.getValue(), newSupply);
      assertEquals(dataCaptor.getValue(), data);
    }
  
    @Test
    void testOnBatchReceived() {
      BigInteger supply = BigInteger.valueOf(100);
      
      BigInteger[] ids = new BigInteger[3];
      for (int i = 0; i < ids.length; i++) {
        ids[i] = mint_token(supply);
      }

      BigInteger[] values = {BigInteger.valueOf(50), BigInteger.valueOf(60), BigInteger.valueOf(70)};

      scoreRcv.invoke(owner, "setOriginator", scoreToken.getAddress(), true);

      scoreToken.invoke(owner, "transferFromBatch", 
        owner.getAddress(), scoreRcv.getAddress(), ids, values, "test".getBytes());
    }
}
