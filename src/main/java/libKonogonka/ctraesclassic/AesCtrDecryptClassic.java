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
package libKonogonka.ctraesclassic;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.Security;

public class AesCtrDecryptClassic {

    private static boolean BCinitialized = false;

    private void initBCProvider(){
        Security.addProvider(new BouncyCastleProvider());
        BCinitialized = true;
    }

    private final SecretKeySpec key;
    private final byte[] ivArray;
    private Cipher cipher;

    public AesCtrDecryptClassic(String keyString, byte[] ivArray) throws Exception{
        if ( ! BCinitialized)
            initBCProvider();
        byte[] keyArray = hexStrToByteArray(keyString);
        this.ivArray = ivArray;
        key = new SecretKeySpec(keyArray, "AES");
        cipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");
        IvParameterSpec iv = new IvParameterSpec(ivArray.clone());
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
    }

    public byte[] decryptNext(byte[] encryptedData) {
        return cipher.update(encryptedData);
    }

    /**
     * Initializes cipher again using updated IV
     * @param blocks - how many blocks from encrypted section start should be skipped. Block size = 0x200
     * */
    public void resetAndSkip(long blocks) throws Exception{
        cipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");
        IvParameterSpec iv = new IvParameterSpec(calculateCtr(blocks * 0x200));
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
    }
    private byte[] calculateCtr(long offset){
        BigInteger ctr = new BigInteger(ivArray);
        BigInteger updateTo = BigInteger.valueOf(offset / 0x10L);
        return ctr.add(updateTo).toByteArray();
    }

    private byte[] hexStrToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
