/*
    Copyright 2019-2023 Dmitry Isaenko

    This file is part of libKonogonka.

    libKonogonka is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    libKonogonka is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with libKonogonka.  If not, see <https://www.gnu.org/licenses/>.
*/
package libKonogonka.aesctr;

import libKonogonka.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class AesCtrDecryptClassic extends AesCtrDecrypt {
    private final SecretKeySpec key;
    private final byte[] ivArray;
    private Cipher cipher;

    public AesCtrDecryptClassic(String keyString, byte[] ivArray) throws Exception{
        super();
        this.key = new SecretKeySpec(Converter.hexStringToByteArray(keyString), "AES");
        this.ivArray = ivArray;
        reset();
    }
    @Override
    public byte[] decryptNext(byte[] encryptedData) {
        return cipher.update(encryptedData);
    }

    @Override
    public void resetAndSkip(long blockCount) throws Exception{
        reset(calculateCtr(blockCount * 0x200));
    }
    private byte[] calculateCtr(long offset){
        BigInteger ctr = new BigInteger(ivArray);
        BigInteger updateTo = BigInteger.valueOf(offset / 0x10L);
        byte[] ctrCalculated = ctr.add(updateTo).toByteArray();
        if (ctrCalculated.length != 0x10) {
            ByteBuffer ctrByteBuffer = ByteBuffer.allocate(0x10);
            ((Buffer) ctrByteBuffer).position(0x10 - ctrCalculated.length);
            ctrByteBuffer.put(ctrCalculated);
            return ctrByteBuffer.array();
        }
        return ctrCalculated;
    }

    @Override
    public void reset() throws Exception{
        reset(ivArray.clone());
    }

    private void reset(byte[] updatedIvArray) throws Exception{
        cipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");
        IvParameterSpec iv = new IvParameterSpec(updatedIvArray);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
    }
}
