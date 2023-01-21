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
package libKonogonka.fs.NSO;

import libKonogonka.Converter;

public class SegmentHeader {
    private final int segmentOffset; // useless constant for KIP1
    private final int memoryOffset; // In case of KIP1 it's decompressed size
    private final int size; // as decompressed for NSO0; as compressed for KIP1

    public SegmentHeader(byte[] data){
        this(data, 0);
    }

    public SegmentHeader(byte[] data, int fromOffset){
        this.segmentOffset = Converter.getLEint(data, fromOffset);
        this.memoryOffset = Converter.getLEint(data, fromOffset+4);
        this.size = Converter.getLEint(data, fromOffset+8);
    }

    public int getSegmentOffset() {
        return segmentOffset;
    }

    public int getMemoryOffset() {
        return memoryOffset;
    }
    /**
     * SegmentHeader used in both NSO0 and KIP1 structures
     * @return Size as decompressed if used in NSO0; size of compressed if used in KIP1.
     * */
    public int getSize() {
        return size;
    }
}
