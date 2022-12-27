/*
    Copyright 2019-2022 Dmitry Isaenko

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
package libKonogonka.ctraes;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Security;

public class AesCtrClassic {

    private static boolean BCinitialized = false;

    private void initBCProvider(){
        Security.addProvider(new BouncyCastleProvider());
        BCinitialized = true;
    }

    private final Cipher cipher;

    public AesCtrClassic(String keyString, byte[] IVarray) throws Exception{
        if ( ! BCinitialized)
            initBCProvider();
        byte[] keyArray = hexStrToByteArray(keyString);
        SecretKeySpec key = new SecretKeySpec(keyArray, "AES");
        cipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");
        IvParameterSpec iv = new IvParameterSpec(IVarray.clone());
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        //TODO: CipherOutputStream
    }

    public byte[] decryptNext(byte[] encryptedData) {
        return cipher.update(encryptedData);
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
