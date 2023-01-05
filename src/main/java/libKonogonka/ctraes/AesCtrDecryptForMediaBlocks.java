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

import libKonogonka.Converter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Security;

/**
 * Simplify decryption of the CTR for NCA's AesCtr sections
 */
public class AesCtrDecryptForMediaBlocks {

    private static boolean BCinitialized = false;
    private Cipher cipher;
    private final SecretKeySpec key;

    private long realMediaOffset;
    private byte[] ivArray;

    private final byte[] initialSectionCTR;
    private final long initialRealMediaOffset;

    public AesCtrDecryptForMediaBlocks(byte[] key, byte[] sectionCTR, long realMediaOffset) throws Exception{
        if ( ! BCinitialized)
            initBCProvider();
        this.key = new SecretKeySpec(key, "AES");
        this.initialSectionCTR = sectionCTR;
        this.initialRealMediaOffset = realMediaOffset;
        reset();
    }
    private void initBCProvider(){
        Security.addProvider(new BouncyCastleProvider());
        BCinitialized = true;
    }

    public void skipNext(){
        realMediaOffset += 0x200;
    }

    public void skipNext(long blocksNum){
        realMediaOffset += blocksNum * 0x200;
    }

    public byte[] decryptNext(byte[] encryptedBlock) throws Exception{
        updateIV();
        byte[] decryptedBlock = decrypt(encryptedBlock);
        realMediaOffset += 0x200;
        return decryptedBlock;
    }
    // Populate last 8 bytes calculated. Thanks hactool project!
    private void updateIV(){
        long offset = realMediaOffset >> 4;
        for (int i = 0; i < 0x8; i++){
            ivArray[0x10-i-1] = (byte)(offset & 0xff);         // Note: issues could be here
            offset >>= 8;
        }
    }
    private byte[] decrypt(byte[] encryptedData) throws Exception{
        IvParameterSpec iv = new IvParameterSpec(ivArray);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        return cipher.doFinal(encryptedData);
    }

    public void reset() throws Exception{
        realMediaOffset = initialRealMediaOffset;
        cipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");
        // IV for CTR == 16 bytes
        ivArray = new byte[0x10];
        // Populate first 4 bytes taken from Header's section Block CTR (aka SecureValue)
        System.arraycopy(Converter.flip(initialSectionCTR), 0x0, ivArray, 0x0, 0x8);
    }
}
