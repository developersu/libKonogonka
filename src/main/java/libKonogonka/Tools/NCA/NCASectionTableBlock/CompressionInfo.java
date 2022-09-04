/*
    Copyright 2018-2022 Dmitry Isaenko
     
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
package libKonogonka.Tools.NCA.NCASectionTableBlock;

import java.util.Arrays;

import static libKonogonka.Converter.getLElong;

public class CompressionInfo {
    private final long offset;
    private final long size;
    private final BucketTreeHeader bktr;
    private final byte[] unknown;

    CompressionInfo(byte[] rawTable){
        offset = getLElong(rawTable, 0);
        size = getLElong(rawTable, 0x8);
        bktr = new BucketTreeHeader(Arrays.copyOfRange(rawTable, 0x10, 0x20));
        unknown = Arrays.copyOfRange(rawTable, 0x20, 0x28);
    }

    public long getOffset() {return offset;}
    public long getSize() {return size;}
    public String getBktrMagic() { return bktr.getMagic(); }
    public int getBktrVersion() { return bktr.getVersion(); }
    public int getBktrEntryCount() { return bktr.getEntryCount(); }
    public byte[] getBktrUnknown() { return bktr.getUnknown(); }
    public byte[] getUnknown() {return unknown;}
}
