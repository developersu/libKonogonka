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

/**
 * Simplify decryption of the CTR
 */
public class AesCtrDecrypt {

    private long realMediaOffset;
    private byte[] IVarray;
    private AesCtr aesCtr;

    private final byte[] initialKey;
    private final byte[] initialSectionCTR;
    private final long initialRealMediaOffset;

    public AesCtrDecrypt(String key, byte[] sectionCTR, long realMediaOffset) throws Exception{
        this.initialKey = hexStrToByteArray(key);
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
        byte[] decryptedBlock = aesCtr.decrypt(encryptedBlock, IVarray);
        realMediaOffset += 0x200;
        return decryptedBlock;
    }

    public void reset() throws Exception{
        realMediaOffset = initialRealMediaOffset;
        aesCtr = new AesCtr(initialKey);
        IVarray = initialSectionCTR;//Converter.flip();
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
