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

class Arrays {
    public static byte[] copyOf (byte[] buf, int newCap) {
        byte[] result = new byte[newCap];
        for (int i = 0; i < buf.length; i++) {
            result[i] = buf[i];
        }
        return result;
    }
}

public class ByteArrayBuilder {
    private static final int INITIAL_CAP = 8;
    private byte[] buf = new byte[INITIAL_CAP];
    private int size;

    private void ensureCap(int req) {
        if (req > buf.length) {
            int newCap = buf.length * 2;
            if (newCap < req) {
                newCap = req;
            }
            buf = Arrays.copyOf(buf, newCap);
        }
    }

    public void write(int b) {
        ensureCap(size + 1);
        buf[size++] = (byte) b;
    }

    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) {
        ensureCap(size + len);
        System.arraycopy(b, off, buf, size, len);
        size += len;
    }

    public void flush() {
    }

    public void close() {
    }

    public byte[] array() {
        return buf;
    }

    public int size() {
        return size;
    }
}
