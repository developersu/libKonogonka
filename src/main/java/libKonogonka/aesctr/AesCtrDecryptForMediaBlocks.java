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
package libKonogonka.aesctr;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

/**
 * Simplify decryption for NCA's AES CTR sections
 */
public class AesCtrDecryptForMediaBlocks extends AesCtrDecrypt {
    private final SecretKeySpec key;
    private final byte[] ivArray;
    private Cipher cipher;

    private final long initialOffset;

    public AesCtrDecryptForMediaBlocks(byte[] key, byte[] sectionCTR, long realMediaOffset) throws Exception{
        super();
        this.key = new SecretKeySpec(key, "AES");
        this.ivArray = Arrays.copyOf(sectionCTR, 0x10);  // IV for CTR == 16 bytes; Populate first 4 bytes taken from Header's section Block CTR (aka SecureValue)
        this.initialOffset = realMediaOffset;
        reset();
    }
    @Override
    public byte[] decryptNext(byte[] encryptedBlock){
        return cipher.update(encryptedBlock);
    }
    @Override
    public void resetAndSkip(long blockCount) throws Exception{
        cipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");
        long mediaOffset = initialOffset + (blockCount * 0x200L);
        cipher.init(Cipher.DECRYPT_MODE, key, getIv(mediaOffset));
    }
    private IvParameterSpec getIv(long mediaOffset){ // Populate last 8 bytes calculated. Thanks hactool!
        byte[] iv = ivArray.clone();
        long offset = mediaOffset >> 4;
        for (int i = 0; i < 8; i++){
            iv[0x10-i-1] = (byte)(offset & 0xff);
            offset >>= 8;
        }
        return new IvParameterSpec(iv);
    }
    @Override
    public void reset() throws Exception{
        resetAndSkip(0);
    }
}
