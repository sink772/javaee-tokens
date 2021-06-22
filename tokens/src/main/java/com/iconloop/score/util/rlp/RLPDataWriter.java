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

package com.iconloop.score.util.rlp;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import scorex.util.ArrayList;

public class RLPDataWriter {
    
    private static final int SHORT_BASE = 0x80;
    private static final int SHORT_LEN_LIMIT = 55;
    private static final int LONG_BASE = 0xb7;

    private ArrayList<ByteArrayBuilder> frames = new ArrayList<>();
    private ByteArrayBuilder os;

    public RLPDataWriter() {
        os = new ByteArrayBuilder();
        frames.add(os);
    }

    private void writeRLPString(byte[] bs) {
        int l = bs.length;
        if (l == 1 && (bs[0] & 0Xff) < SHORT_BASE) {
            os.write(bs[0]);
        } else if (l <= SHORT_LEN_LIMIT) {
            os.write(SHORT_BASE + l);
            os.write(bs, 0, l);
        } else if (l <= 0Xff) {
            os.write(LONG_BASE + 1);
            os.write(l);
            os.write(bs, 0, l);
        } else if (l <= 0Xffff) {
            os.write(LONG_BASE + 2);
            os.write(l >> 8);
            os.write(l);
            os.write(bs, 0, l);
        } else if (l <= 0Xffffff) {
            os.write(LONG_BASE + 3);
            os.write(l >> 16);
            os.write(l >> 8);
            os.write(l);
            os.write(bs, 0, l);
        } else {
            os.write(LONG_BASE + 4);
            os.write(l >> 24);
            os.write(l >> 16);
            os.write(l >> 8);
            os.write(l);
            os.write(bs, 0, l);
        }
    }

    public void write(boolean v) {
        writeRLPString(BigInteger.valueOf(v ? 1 : 0).toByteArray());
    }

    public void write(byte v) {
        writeRLPString(BigInteger.valueOf(v).toByteArray());
    }

    public void write(short v) {
        writeRLPString(BigInteger.valueOf(v).toByteArray());
    }

    public void write(char v) {
        writeRLPString(BigInteger.valueOf((int) v).toByteArray());
    }

    public void write(int v) {
        writeRLPString(BigInteger.valueOf(v).toByteArray());
    }

    public void write(float v) {
        int i = Float.floatToRawIntBits(v);
        os.write(SHORT_BASE + 4);
        os.write((i >> 24) & 0xff);
        os.write((i >> 16) & 0xff);
        os.write((i >> 8) & 0xff);
        os.write(i & 0xff);
    }

    public void write(long v) {
        writeRLPString(BigInteger.valueOf(v).toByteArray());
    }

    public void write(double v) {
        long i = Double.doubleToRawLongBits(v);
        os.write(SHORT_BASE + 8);
        os.write(((int) (i >> 54)) & 0xff);
        os.write(((int) (i >> 48)) & 0xff);
        os.write(((int) (i >> 40)) & 0xff);
        os.write(((int) (i >> 32)) & 0xff);
        os.write(((int) (i >> 24)) & 0xff);
        os.write(((int) (i >> 16)) & 0xff);
        os.write(((int) (i >> 8)) & 0xff);
        os.write(((int) i) & 0xff);
    }

    public void write(BigInteger v) {
        writeRLPString(v.toByteArray());
    }

    public void write(String v) {
        writeRLPString(v.getBytes(StandardCharsets.UTF_8));
    }

    public void write(byte[] v) {
        writeRLPString(v);
    }

    public void writeNullity(boolean nullity) {
        if (nullity) {
            writeNull();
        }
    }

    public void writeListHeader(int l) {
        _writeRLPListHeader();
    }

    public void writeMapHeader(int l) {
        _writeRLPListHeader();
    }

    private void _writeRLPListHeader() {
        os = new ByteArrayBuilder();
        frames.add(os);
    }

    private void _writeRLPListFooter() {
        var prev = os;
        var l = prev.size();
        frames.remove(frames.size() - 1);
        os = frames.get(frames.size() - 1);
        if (l <= 55) {
            os.write(0xc0 + l);
            os.write(prev.array(), 0, prev.size());
        } else if (l <= 0xff) {
            os.write(0xf8);
            os.write(l);
            os.write(prev.array(), 0, prev.size());
        } else if (l <= 0xffff) {
            os.write(0xf9);
            os.write((l >> 8) & 0xff);
            os.write(l & 0xff);
            os.write(prev.array(), 0, prev.size());
        } else if (l <= 0xffffff) {
            os.write(0xfa);
            os.write((l >> 16) & 0xff);
            os.write((l >> 8) & 0xff);
            os.write(l & 0xff);
            os.write(prev.array(), 0, prev.size());
        } else {
            os.write(0xfb);
            os.write((l >> 24) & 0xff);
            os.write((l >> 16) & 0xff);
            os.write((l >> 8) & 0xff);
            os.write(l & 0xff);
            os.write(prev.array(), 0, prev.size());
        }
    }

    public void writeFooter() {
        _writeRLPListFooter();
    }

    private void writeNull() {
        os.write(0xf8);
        os.write(0x00);
    }

    public void flush() {
        os.flush();
    }

    public byte[] toByteArray() {
        return Arrays.copyOfRange(os.array(), 0, os.size());
    }

    public long getTotalWrittenBytes() {
        return os.size();
    }
}
