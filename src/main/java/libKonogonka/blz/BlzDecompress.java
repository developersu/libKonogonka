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
package libKonogonka.blz;

import libKonogonka.Converter;

public class BlzDecompress {
    public static final byte BLZ_MASK = (byte) 0x80;
    /**
     * Decompress BLZ section. Adapted for NS.
     * @param compressed byte array with compressed data
     * @param decompressed byte array where decompressed data should be saved in
    * */
    public int decompress(byte[] compressed, byte[] decompressed) throws Exception{
        /* NOTE: values must be unsigned int ! */
        int additionalLength = Converter.getLEint(compressed, compressed.length-4);
        int headerSize = Converter.getLEint(compressed, compressed.length-2*4); // 'Footer' aka 'Header'
        int compressedAndHeaderSize = Converter.getLEint(compressed, compressed.length-3*4);

        if (additionalLength == 0)
            throw new Exception("File not compressed");
        else if (additionalLength < 0)
            throw new Exception("File not supported. Please file a bug "+additionalLength);

        int delta = compressed.length - compressedAndHeaderSize;
        int compressedOffset = compressedAndHeaderSize - headerSize + delta;
        int finalOffset = compressedAndHeaderSize + additionalLength + delta;
        int resultingSize = finalOffset;
        /*
        System.out.printf(
                "Additional length            : %-21d 0x%-8x %n" +
                "Additional length            : %-21d 0x%-8x %n" +
                "Delta                        : %-21d %n" +
                "Compressed+Header size       : %-21d 0x%-8x           Incorrect: %-8d 0x%-8x %n" +
                "Compressed offset            : %-21d 0x%-8x           Incorrect: %-8d 0x%-8x %n" +
                "Resulting Size,Final offset  : %-21d 0x%-8x           Incorrect: %-8d 0x%-8x %n%n",
                additionalLength, additionalLength,
                headerSize, headerSize,
                delta,
                compressed.length, compressed.length, compressedAndHeaderSize, compressedAndHeaderSize,
                compressedOffset, compressedOffset, compressedAndHeaderSize - headerSize, compressedAndHeaderSize - headerSize,
                finalOffset, finalOffset, compressedAndHeaderSize + additionalLength, compressedAndHeaderSize + additionalLength);
        //*/
        decompress_loop:
        while (true){
            byte control = compressed[--compressedOffset];
            for (int i = 0; i < 8; i++){
                if ((control & BLZ_MASK) == 0) {
                    if (compressedOffset < 1)
                        throw new Exception("BLZ decompression is out of range");
                    decompressed[--finalOffset] = compressed[--compressedOffset];
                }
                else {
                    if (compressedOffset < 2)
                        throw new Exception("BLZ decompression is out of range");
                    compressedOffset -= 2;
                    short segmentValue = (short) (( (compressed[compressedOffset+1]) << 8) | (compressed[compressedOffset] & 0xFF));
                    int segmentSize = ((segmentValue >> 12) & 0xF) + 3;
                    int segmentPosition = (segmentValue & 0xFFF) + 3;

                    if (segmentSize > finalOffset)
                        segmentSize = finalOffset;

                    finalOffset -= segmentSize;

                    for (int j = 0; j < segmentSize; j++)
                        decompressed[finalOffset + j] = decompressed[finalOffset + j + segmentPosition];
                }
                control <<= 1;
                if (finalOffset == delta)
                    break decompress_loop;
            }
        }
        if (delta != 0)
            System.arraycopy(compressed, 0, decompressed, 0, delta);

        return resultingSize;
    }
}
