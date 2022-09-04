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
package libKonogonka.Tools.NCA;

import libKonogonka.Converter;

import java.util.Arrays;

public class NCAHeaderTableEntry {
    private final long mediaStartOffset;
    private final long mediaEndOffset;
    private final byte[] unknwn1;
    private final byte[] unknwn2;

    public NCAHeaderTableEntry(byte[] table) throws Exception{
        if (table.length < 0x10)
            throw new Exception("Section Table size is too small.");

        this.mediaStartOffset = Converter.getLElongOfInt(table, 0);
        this.mediaEndOffset = Converter.getLElongOfInt(table, 0x4);
        this.unknwn1 = Arrays.copyOfRange(table, 0x8, 0xC);
        this.unknwn2 = Arrays.copyOfRange(table, 0xC, 0x10);
    }

    public long getMediaStartOffset() { return mediaStartOffset; }
    public long getMediaEndOffset() { return mediaEndOffset; }
    public byte[] getUnknwn1() { return unknwn1; }
    public byte[] getUnknwn2() { return unknwn2; }
}