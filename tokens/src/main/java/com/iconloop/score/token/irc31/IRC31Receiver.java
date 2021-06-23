/*
 * Copyright 2021 ICON Foundation
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

import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

/**
 * Smart contracts that want to receive tokens from IRC31-compatible token contracts must implement all of the following receiver methods to accept transfers.
 */
public class IRC31Receiver {

    // ================================================
    // SCORE DB 
    // ================================================
    // allowlist of token contracts
    final private DictDB<Address, Boolean> originators = Context.newDictDB("originators", Boolean.class);

    // ================================================
    // Event Logs
    // ================================================
    @EventLog(indexed=3)
    public void IRC31Received(
        Address _origin,  
        Address _operator, 
        Address _from, 
        BigInteger _id, 
        BigInteger _value, 
        byte[] _data) {}

    // ================================================
    // External methods
    // ================================================
    @External
    public void setOriginator (Address _origin, boolean _approved) {
        Context.require(Context.getOwner().equals(Context.getCaller()),
            "Not contract owner");
        Context.require(_origin.isContract(),
            "Not contract address");
        originators.set(_origin, _approved);
    }

    @External
    public void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, @Optional byte[] _data) {
        /*
            A method for handling a single token type transfer, which is called from the multi token contract.
            It works by analogy with the fallback method of the normal transactions and returns nothing.
            
            Throws if it rejects the transfer.
            @param _operator  The address which initiated the transfer
            @param _from      The address which previously owned the token
            @param _id        The ID of the token being transferred
            @param _value     The amount of tokens being transferred
            @param _data      Additional data with no specified format
        */
        final Address caller = Context.getCaller();
        Context.require(originators.get(caller) != null,
            "Unrecognized token contract");
        this.IRC31Received(caller, _operator, _from, _id, _value, _data);
    }

    @External
    public void onIRC31BatchReceived(Address _operator, Address _from, BigInteger[] _ids, BigInteger[] _values, @Optional byte[] _data) {
        /*
            A method for handling multiple token type transfers, which is called from the multi token contract.
            It works by analogy with the fallback method of the normal transactions and returns nothing.
            
            Throws if it rejects the transfer.
            @param _operator  The address which initiated the transfer
            @param _from      The address which previously owned the token
            @param _ids       The list of IDs of each token being transferred (order and length must match `_values` list)
            @param _values    The list of amounts of each token being transferred (order and length must match `_ids` list)
            @param _data      Additional data with no specified format
        */
        final Address caller = Context.getCaller();
        Context.require(originators.get(caller) != null,
            "Unrecognized token contract");

        for (int i = 0; i < _ids.length; i++) {
            BigInteger _id = _ids[i];
            BigInteger _value = _values[i];
            this.IRC31Received(caller, _operator, _from, _id, _value, _data);
        }
    }}
