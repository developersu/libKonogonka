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
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Security;

public class AesCtrForMediaBlocks {

    private static boolean BCinitialized = false;

    private void initBCProvider(){
        Security.addProvider(new BouncyCastleProvider());
        BCinitialized = true;
    }

    private final Cipher cipher;
    private final SecretKeySpec key;

    AesCtrForMediaBlocks(byte[] keyArray) throws Exception{
        if ( ! BCinitialized)
            initBCProvider();

        key = new SecretKeySpec(keyArray, "AES");
        cipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");
    }

    byte[] decrypt(byte[] encryptedData, byte[] IVarray) throws Exception{
        IvParameterSpec iv = new IvParameterSpec(IVarray);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        return cipher.doFinal(encryptedData);
    }
}
