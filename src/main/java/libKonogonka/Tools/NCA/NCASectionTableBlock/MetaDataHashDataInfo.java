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

public class MetaDataHashDataInfo {
    private final long offset;
    private final long size;
    private final byte[] tableHash;
    MetaDataHashDataInfo(byte[] rawTable){
        offset = getLElong(rawTable, 0);
        size = getLElong(rawTable, 0x8);
        tableHash = Arrays.copyOfRange(rawTable, 0x10, 0x20);
    }

    public long getOffset() {return offset;}
    public long getSize() {return size;}
    public byte[] getTableHash() {return tableHash;}
}
