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

import score.Address;
import score.Context;
import score.DictDB;

import java.math.BigInteger;

public class IRC31MintBurn extends IRC31Basic {

    // ================================================
    // SCORE DB 
    // ================================================
    // id ==> creator
    protected final DictDB<BigInteger, Address> creators = Context.newDictDB("creators", Address.class);

    // ================================================
    // External methods
    // ================================================

    /**
     * Creates a new token type and assigns _supply to creator
     *
     * @param _owner  Owner address of the tokens
     * @param _id     ID of the token
     * @param _supply The initial token supply
     * @param _uri    The token URI
     */
    protected void _mint(Address _owner, BigInteger _id, BigInteger _supply, String _uri) {
        Context.require(creators.get(_id) == null,
                "Token is already minted");
        Context.require(_supply.compareTo(BigInteger.ZERO) > 0,
                "Supply should be positive");
        Context.require(_uri.length() > 0,
                "Uri should be set");

        creators.set(_id, _owner);
        balances.at(_id).set(_owner, _supply);

        // emit transfer event for Mint semantic
        this.TransferSingle(_owner, ZERO_ADDRESS, _owner, _id, _supply);

        // set token URI and emit event
        this._setTokenURI(_id, _uri);
    }

    /**
     * Creates a new token type and assigns _supply to caller
     *
     * @param _id     ID of the token
     * @param _supply The initial token supply
     * @param _uri    The token URI
     */
    protected void _mint(BigInteger _id, BigInteger _supply, String _uri) {
        _mint(Context.getCaller(), _id, _supply, _uri);
    }

    /**
     * Destroys tokens for a given amount
     *
     * @param _id     ID of the token
     * @param _amount The amount of tokens to burn
     */
    protected void _burn(BigInteger _id, BigInteger _amount) {
        Context.require(creators.get(_id) != null,
                "Invalid token id");
        Context.require(_amount.compareTo(BigInteger.ZERO) > 0,
                "Amount should be positive");

        final Address caller = Context.getCaller();

        BigInteger balance = balanceOf(caller, _id);

        Context.require(BigInteger.ZERO.compareTo(_amount) <= 0 && _amount.compareTo(balance) <= 0,
                "Not an owner or invalid amount");

        balances.at(_id).set(caller, balance.subtract(_amount));

        // emit transfer event for Burn semantic
        this.TransferSingle(caller, caller, ZERO_ADDRESS, _id, _amount);
    }

    /**
     * Updates the given token URI
     *
     * @param _id  ID of the token
     * @param _uri The token URI
     */
    protected void _setTokenURI(BigInteger _id, String _uri) {
        Context.require(creators.get(_id).equals(Context.getCaller()),
                "Not token creator");

        super._setTokenURI(_id, _uri);
    }
}
