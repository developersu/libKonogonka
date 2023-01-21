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
package libKonogonka.fs.NPDM.ACI0;

import libKonogonka.Converter;

import java.util.Arrays;

/**
 * For ACI0 Provider
 * */
public class FSAccessHeaderProvider {

    private final byte version;
    private final byte[] padding;
    private final long permissionsBitmask;
    private final int dataSize;
    private final int contentOwnIdSectionSize;
    private final int dataOwnerSizes;
    private final int saveDataOwnSectionSize;
    private final byte[] unknownData;

    public FSAccessHeaderProvider(byte[] bytes) {
        version = bytes[0];
        padding = Arrays.copyOfRange(bytes, 1, 0x4);
        permissionsBitmask = Converter.getLElong(bytes, 0x4);
        dataSize = Converter.getLEint(bytes, 0xC);
        contentOwnIdSectionSize = Converter.getLEint(bytes, 0x10);
        dataOwnerSizes = Converter.getLEint(bytes, 0x14);
        saveDataOwnSectionSize = Converter.getLEint(bytes, 0x18);
        unknownData = Arrays.copyOfRange(bytes, 0x1C, bytes.length);
    }

    public byte getVersion() { return version; }
    public byte[] getPadding() { return padding; }
    public long getPermissionsBitmask() { return permissionsBitmask; }
    public int getDataSize() { return dataSize; }
    public int getContentOwnIdSectionSize() { return contentOwnIdSectionSize; }
    public int getDataNownerSizes() { return dataOwnerSizes; }
    public int getSaveDataOwnSectionSize() { return saveDataOwnSectionSize; }
    public byte[] getUnknownData() { return unknownData; }
}
