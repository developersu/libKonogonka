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
package libKonogonka.Tools.NPDM.ACID;

import libKonogonka.Converter;

import java.util.Arrays;

/**
 * For ACID Provider
 * */
public class FSAccessControlProvider {

    private final byte version;
    private final byte[] padding;
    private final long permissionsBitmask;
    private final byte[] reserved;

    public FSAccessControlProvider(byte[] bytes) {
        version = bytes[0];
        padding = Arrays.copyOfRange(bytes, 1, 0x4);
        permissionsBitmask =  Converter.getLElong(bytes, 0x4);
        reserved = Arrays.copyOfRange(bytes, 0xC, 0x2C);
    }

    public byte getVersion() { return version; }
    public byte[] getPadding() { return padding; }
    public long getPermissionsBitmask() { return permissionsBitmask; }
    public byte[] getReserved() { return reserved; }
}
