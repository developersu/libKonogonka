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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static libKonogonka.Converter.getLEint;

public class BucketTreeHeader {
    private final String magic;
    private final int version;
    private final int entryCount;
    private final byte[] unknown;

    BucketTreeHeader(byte[] rawBytes){
        magic = new String(Arrays.copyOfRange(rawBytes, 0x0, 0x4), StandardCharsets.US_ASCII);
        version = getLEint(rawBytes, 0x4);
        entryCount = getLEint(rawBytes, 0x8);
        unknown = Arrays.copyOfRange(rawBytes, 0xc, 0x10);
    }

    public String getMagic() {return magic;}
    public int getVersion() {return version;}
    public int getEntryCount() {return entryCount;}
    public byte[] getUnknown() {return unknown;}
}
