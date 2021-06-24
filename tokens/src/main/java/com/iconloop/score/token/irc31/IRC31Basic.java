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
import score.BranchDB;
import score.ByteArrayObjectWriter;
import score.Context;
import score.DictDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

public class IRC31Basic {

    // ================================================
    // Consts
    // ================================================
    public static final Address ZERO_ADDRESS = new Address(new byte[Address.LENGTH]);

    // ================================================
    // SCORE DB 
    // ================================================
    // id => (owner => balance)
    protected final BranchDB<BigInteger, DictDB<Address, BigInteger>> balances = Context.newBranchDB("balances", BigInteger.class);
    // owner => (operator => approved)
    protected final BranchDB<Address, DictDB<Address, Boolean>> operatorApproval = Context.newBranchDB("approval", Boolean.class);
    // id => token URI
    protected final DictDB<BigInteger, String> tokenURIs = Context.newDictDB("token_uri", String.class);

    // ================================================
    // Event Logs
    // ================================================

    /**
     * Must trigger on any successful token transfers, including zero value transfers as well as minting or burning.
     * When minting/creating tokens, the {@code _from} must be set to zero address.
     * When burning/destroying tokens, the {@code _to} must be set to zero address.
     *
     * @param _operator The address of an account/contract that is approved to make the transfer
     * @param _from     The address of the token holder whose balance is decreased
     * @param _to       The address of the recipient whose balance is increased
     * @param _id       ID of the token
     * @param _value    The amount of transfer
     */
    @EventLog(indexed=3)
    public void TransferSingle(Address _operator, Address _from, Address _to, BigInteger _id, BigInteger _value) {}

    /**
     * Must trigger on any successful token transfers, including zero value transfers as well as minting or burning.
     * When minting/creating tokens, the {@code _from} must be set to zero address.
     * When burning/destroying tokens, the {@code _to} must be set to zero address.
     * <p>
     * NOTE: RLP (Recursive Length Prefix) would be used for the serialized bytes to represent list type.
     *
     * @param _operator The address of an account/contract that is approved to make the transfer
     * @param _from     The address of the token holder whose balance is decreased
     * @param _to       The address of the recipient whose balance is increased
     * @param _ids      Serialized bytes of list for token IDs (order and length must match {@code _values})
     * @param _values   Serialized bytes of list for transfer amounts per token (order and length must match {@code _ids})
     */
    @EventLog(indexed=3)
    public void TransferBatch(Address _operator, Address _from, Address _to, byte[] _ids, byte[] _values) {}

    /**
     * Must trigger on any successful approval (either enabled or disabled) for a third party/operator address
     * to manage all tokens for the {@code _owner} address.
     *
     * @param _owner    The address of the token holder
     * @param _operator The address of authorized operator
     * @param _approved True if the operator is approved, false to revoke approval
     */
    @EventLog(indexed=2)
    public void ApprovalForAll(Address _owner, Address _operator, boolean _approved) {}

    /**
     * Must trigger on any successful URI updates for a token ID.
     * URIs are defined in RFC 3986.
     * The URI must point to a JSON file that conforms to the "ERC-1155 Metadata URI JSON Schema".
     *
     * @param _id    ID of the token
     * @param _value The updated URI string
     */
    @EventLog(indexed=1)
    public void URI(BigInteger _id, String _value) {}

    // ================================================
    // Internal methods
    // ================================================

    /**
     * Convert a list of BigInteger to a RLP-encoded byte array
     *
     * @param ids A list of BigInteger
     * @return a RLP encoded byte array
     */
    protected static byte[] rlpEncode(BigInteger[] ids) {
        Context.require(ids != null);

        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");

        writer.beginList(ids.length);
        for (BigInteger v : ids) {
            writer.write(v);
        }
        writer.end();

        return writer.toByteArray();
    }

    protected void _setTokenURI(BigInteger _id, String _uri) {
        Context.require(_uri.length() > 0,
                "Uri should be set");
        tokenURIs.set(_id, _uri);
        this.URI(_id, _uri);
    }

    // ================================================
    // External methods
    // ================================================

    /**
     * Returns the balance of the owner's tokens.
     *
     * @param _owner The address of the token holder
     * @param _id    ID of the token
     * @return The _owner's balance of the token type requested
     */
    @External(readonly=true)
    public BigInteger balanceOf(Address _owner, BigInteger _id) {
        return balances.at(_id).getOrDefault(_owner, BigInteger.ZERO);
    }

    /**
     * Returns the balance of multiple owner/id pairs.
     *
     * @param _owners The addresses of the token holders
     * @param _ids    IDs of the tokens
     * @return The list of balance (i.e. balance for each owner/id pair)
     */
    @External(readonly=true)
    public BigInteger[] balanceOfBatch(Address[] _owners, BigInteger[] _ids) {
        Context.require(_owners.length == _ids.length,
                "_owners array size must match with _ids array size");

        BigInteger[] balances = new BigInteger[_owners.length];

        for (int i = 0; i < _owners.length; i++) {
            balances[i] = balanceOf(_owners[i], _ids[i]);
        }

        return balances;
    }

    /**
     * Returns an URI for a given token ID.
     *
     * @param _id ID of the token
     * @return The URI string
     */
    @External(readonly=true)
    public String tokenURI(BigInteger _id) {
        return tokenURIs.get(_id);
    }

    /**
     * Transfers {@code _value} amount of an token {@code _id} from one address to another address,
     * and must emit {@code TransferSingle} event to reflect the balance change.
     * <p>
     * When the transfer is complete, this method must invoke {@code onIRC31Received} in {@code _to},
     * if {@code _to} is a contract. If the {@code onIRC31Received} method is not implemented in {@code _to} (receiver contract),
     * then the transaction must fail and the transfer of tokens should not occur.
     * If {@code _to} is an externally owned address, then the transaction must be sent without trying to execute
     * {@code onIRC31Received} in {@code _to}.
     * <p>
     * Additional {@code _data} can be attached to this token transaction, and it should be sent unaltered in call
     * to {@code onIRC31Received} in {@code _to}. {@code _data} can be empty.
     * <p>
     * Throws unless the caller is the current token holder or the approved address for the token ID.
     * Throws if {@code _from} does not have enough amount to transfer for the token ID.
     * Throws if {@code _to} is the zero address.
     *
     * @param _from  Source address
     * @param _to    Target address
     * @param _id    ID of the token
     * @param _value The amount of transfer
     * @param _data  Additional data that should be sent unaltered in call to {@code _to}
     */
    @External
    public void transferFrom(Address _from, Address _to, BigInteger _id, BigInteger _value, @Optional byte[] _data) {
        final Address caller = Context.getCaller();

        Context.require(!_to.equals(ZERO_ADDRESS),
                "_to must be non-zero");
        Context.require(_from.equals(caller) || this.isApprovedForAll(_from, caller),
                "Need operator approval for 3rd party transfers");
        Context.require(BigInteger.ZERO.compareTo(_value) <= 0 && _value.compareTo(balanceOf(_from, _id)) <= 0,
                "Insufficient funds");

        // Transfer funds
        DictDB<Address, BigInteger> balance = balances.at(_id);
        balance.set(_from, balanceOf(_from, _id).subtract(_value));
        balance.set(_to, balanceOf(_to, _id).add(_value));

        // Emit event
        this.TransferSingle(caller, _from, _to, _id, _value);

        if (_to.isContract()) {
            // Call {@code onIRC31Received} if the recipient is a contract
            Context.call(_to, "onIRC31Received", caller, _from, _id, _value, _data == null ? new byte[]{} : _data);
        }
    }

    /**
     * Transfers {@code _values} amount(s) of token(s) {@code _ids} from one address to another address,
     * and must emit {@code TransferSingle} or {@code TransferBatch} event(s) to reflect all the balance changes.
     * <p>
     * When all the transfers are complete, this method must invoke {@code onIRC31Received} or
     * {@code onIRC31BatchReceived(Address,Address,int[],int[],bytes)} in {@code _to},
     * if {@code _to} is a contract. If the {@code onIRC31Received} method is not implemented in {@code _to} (receiver contract),
     * then the transaction must fail and the transfers of tokens should not occur.
     * <p>
     * If {@code _to} is an externally owned address, then the transaction must be sent without trying to execute
     * {@code onIRC31Received} in {@code _to}.
     * <p>
     * Additional {@code _data} can be attached to this token transaction, and it should be sent unaltered in call
     * to {@code onIRC31Received} in {@code _to}. {@code _data} can be empty.
     * <p>
     * Throws unless the caller is the current token holder or the approved address for the token IDs.
     * Throws if length of {@code _ids} is not the same as length of {@code _values}.
     * Throws if {@code _from} does not have enough amount to transfer for any of the token IDs.
     * Throws if {@code _to} is the zero address.
     *
     * @param _from   Source address
     * @param _to     Target address
     * @param _ids    IDs of the tokens (order and length must match {@code _values} list)
     * @param _values Transfer amounts per token (order and length must match {@code _ids} list)
     * @param _data   Additional data that should be sent unaltered in call to {@code _to}
     */
    @External
    public void transferFromBatch(Address _from, Address _to, BigInteger[] _ids, BigInteger[] _values, @Optional byte[] _data) {
        final Address caller = Context.getCaller();

        Context.require(!_to.equals(ZERO_ADDRESS),
                "_to must be non-zero");
        Context.require(_ids.length == _values.length,
                "id/value pairs mismatch");
        Context.require(_from.equals(caller) || this.isApprovedForAll(_from, caller),
                "Need operator approval for 3rd party transfers");

        for (int i = 0; i < _ids.length; i++) {
            BigInteger _id = _ids[i];
            BigInteger _value = _values[i];

            Context.require(_value.compareTo(BigInteger.ZERO) >= 0,
                    "Insufficient funds");

            BigInteger balanceFrom = balanceOf(_from, _id);

            Context.require(_value.compareTo(balanceFrom) <= 0,
                    "Insufficient funds");

            // Transfer funds
            BigInteger balanceTo = balanceOf(_to, _id);
            DictDB<Address, BigInteger> balance = balances.at(_id);
            balance.set(_from, balanceFrom.subtract(_value));
            balance.set(_to, balanceTo.add(_value));
        }

        // Emit event
        this.TransferBatch(caller, _from, _to, rlpEncode(_ids), rlpEncode(_values));

        if (_to.isContract()) {
            // call {@code onIRC31BatchReceived} if the recipient is a contract
            Context.call(_to, "onIRC31BatchReceived",
                    caller, _from, _ids, _values, _data == null ? new byte[]{} : _data);
        }
    }

    /**
     * Enables or disables approval for a third party ("operator") to manage all of the caller's tokens,
     * and must emit {@code ApprovalForAll} event on success.
     *
     * @param _operator Address to add to the set of authorized operators
     * @param _approved True if the operator is approved, false to revoke approval
     */
    @External
    public void setApprovalForAll(Address _operator, boolean _approved) {
        final Address caller = Context.getCaller();

        operatorApproval.at(caller).set(_operator, _approved);
        this.ApprovalForAll(caller, _operator, _approved);
    }

    /**
     * Returns the approval status of an operator for a given owner.
     *
     * @param _owner    The owner of the tokens
     * @param _operator The address of authorized operator
     * @return True if the operator is approved, false otherwise
     */
    @External(readonly=true)
    public boolean isApprovedForAll(Address _owner, Address _operator) {
        return operatorApproval.at(_owner).getOrDefault(_operator, false);
    }
}
