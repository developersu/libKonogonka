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

/**
 * Simplify decryption of the CTR for NCA's AesCtr sections
 */
public class AesCtrDecryptSimple {

    private long realMediaOffset;
    private byte[] IVarray;
    private AesCtrForMediaBlocks aesCtr;

    private final byte[] initialKey;
    private final byte[] initialSectionCTR;
    private final long initialRealMediaOffset;

    public AesCtrDecryptSimple(byte[] key, byte[] sectionCTR, long realMediaOffset) throws Exception{
        this.initialKey = key;
        this.initialSectionCTR = sectionCTR;
        this.initialRealMediaOffset = realMediaOffset;
        reset();
    }

    public void skipNext(){
        realMediaOffset += 0x200;
    }

    public void skipNext(long blocksNum){
        realMediaOffset += blocksNum * 0x200;
    }

    public byte[] decryptNext(byte[] encryptedBlock) throws Exception{
        updateIV();
        byte[] decryptedBlock = aesCtr.decrypt(encryptedBlock, IVarray);
        realMediaOffset += 0x200;
        return decryptedBlock;
    }
    // Populate last 8 bytes calculated. Thanks hactool project!
    private void updateIV(){
        long offset = realMediaOffset >> 4;
        for (int i = 0; i < 0x8; i++){
            IVarray[0x10-i-1] = (byte)(offset & 0xff);         // Note: issues could be here
            offset >>= 8;
        }
    }

    public void reset() throws Exception{
        realMediaOffset = initialRealMediaOffset;
        aesCtr = new AesCtrForMediaBlocks(initialKey);
        // IV for CTR == 16 bytes
        IVarray = new byte[0x10];
        // Populate first 4 bytes taken from Header's section Block CTR (aka SecureValue)
        System.arraycopy(Converter.flip(initialSectionCTR), 0x0, IVarray, 0x0, 0x8);
    }
}
