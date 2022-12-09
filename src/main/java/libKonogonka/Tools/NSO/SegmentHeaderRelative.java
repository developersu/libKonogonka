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
package libKonogonka.Tools.NSO;

import libKonogonka.Converter;

public class SegmentHeaderRelative {
    private final int offset;
    private final int size;

    SegmentHeaderRelative(byte[] data){
        this(data, 0);
    }

    SegmentHeaderRelative(byte[] data, int fromOffset){
        this.offset = Converter.getLEint(data, fromOffset);
        this.size = Converter.getLEint(data, fromOffset+4);
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }
}
