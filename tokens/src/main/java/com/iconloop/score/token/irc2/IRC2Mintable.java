/*
 * Copyright 2020 ICONLOOP Inc.
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

package com.iconloop.score.token.irc2;

import score.Address;
import score.Context;
import score.annotation.External;

import java.math.BigInteger;

public abstract class IRC2Mintable extends IRC2Basic {
    public IRC2Mintable(String _name, String _symbol, int _decimals) {
        super(_name, _symbol, _decimals);
    }

    /**
	 * Creates amount number of tokens, and assigns to caller
	 * Increases the balance of that account and the total supply.
     */
    @External
    public void mint(BigInteger _amount) {
        // simple access control - only the contract owner can mint new token
        Context.require(Context.getCaller().equals(Context.getOwner()));
        _mint(Context.getCaller(), _amount);
    }

    /**
     * Creates amount number of tokens, and assigns to _account
     * Increases the balance of that account and the total supply.
     */
    @External
    public void mintTo(Address _account, BigInteger _amount) {
        // simple access control - only the contract owner can mint new token
        Context.require(Context.getCaller().equals(Context.getOwner()));
        _mint(_account, _amount);
    }

}
