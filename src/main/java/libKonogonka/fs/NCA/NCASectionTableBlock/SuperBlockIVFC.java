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
package libKonogonka.fs.NCA.NCASectionTableBlock;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static libKonogonka.Converter.getLEint;
import static libKonogonka.Converter.getLElong;

public class SuperBlockIVFC {
    private final String magic;
    private final int version;
    private final int masterHashSize;
    private final int totalNumberOfLevels;
    private final long lvl1Offset;
    private final long lvl1Size;
    private final int lvl1SBlockSize;
    private final byte[] reserved1;

    private final long lvl2Offset;
    private final long lvl2Size;
    private final int lvl2SBlockSize;
    private final byte[] reserved2;

    private final long lvl3Offset;
    private final long lvl3Size;
    private final int lvl3SBlockSize;
    private final byte[] reserved3;

    private final long lvl4Offset;
    private final long lvl4Size;
    private final int lvl4SBlockSize;
    private final byte[] reserved4;

    private final long lvl5Offset;
    private final long lvl5Size;
    private final int lvl5SBlockSize;
    private final byte[] reserved5;

    private final long lvl6Offset;
    private final long lvl6Size;
    private final int lvl6SBlockSize;
    private final byte[] reserved6;

    private final byte[] signatureSalt;
    private final byte[] masterHash;
    private final byte[] reservedTail;

    /**
     * Also known as IntegrityMetaInfo
     * @param sbBytes - Chunk of data related for IVFC Hash Data table
     */
    SuperBlockIVFC(byte[] sbBytes){
        this.magic = new String(Arrays.copyOfRange(sbBytes, 0, 4), StandardCharsets.US_ASCII);
        this.version = getLEint(sbBytes, 0x4);
        this.masterHashSize = getLEint(sbBytes, 0x8);
        this.totalNumberOfLevels = getLEint(sbBytes, 0xc);

        this.lvl1Offset = getLElong(sbBytes, 0x10);
        this.lvl1Size = getLElong(sbBytes, 0x18);
        this.lvl1SBlockSize = getLEint(sbBytes, 0x20);
        this.reserved1 = Arrays.copyOfRange(sbBytes, 0x24, 0x28);

        this.lvl2Offset = getLElong(sbBytes, 0x28);
        this.lvl2Size = getLElong(sbBytes, 0x30);
        this.lvl2SBlockSize = getLEint(sbBytes, 0x38);
        this.reserved2 = Arrays.copyOfRange(sbBytes, 0x3c, 0x40);

        this.lvl3Offset = getLElong(sbBytes, 0x40);
        this.lvl3Size = getLElong(sbBytes, 0x48);
        this.lvl3SBlockSize = getLEint(sbBytes, 0x50);
        this.reserved3 = Arrays.copyOfRange(sbBytes, 0x54, 0x58);

        this.lvl4Offset = getLElong(sbBytes, 0x58);
        this.lvl4Size = getLElong(sbBytes, 0x60);
        this.lvl4SBlockSize = getLEint(sbBytes, 0x68);
        this.reserved4 = Arrays.copyOfRange(sbBytes, 0x6c, 0x70);

        this.lvl5Offset = getLElong(sbBytes, 0x70);
        this.lvl5Size = getLElong(sbBytes, 0x78);
        this.lvl5SBlockSize = getLEint(sbBytes, 0x80);
        this.reserved5 = Arrays.copyOfRange(sbBytes, 0x84, 0x88);

        this.lvl6Offset = getLElong(sbBytes, 0x88);
        this.lvl6Size = getLElong(sbBytes, 0x90);
        this.lvl6SBlockSize = getLEint(sbBytes, 0x98);
        this.reserved6 = Arrays.copyOfRange(sbBytes, 0x9c, 0xa0);

        this.signatureSalt = Arrays.copyOfRange(sbBytes, 0xa0, 0xc0);
        this.masterHash = Arrays.copyOfRange(sbBytes, 0xc0, 0xe0);
        this.reservedTail = Arrays.copyOfRange(sbBytes, 0xe0, 0xf8);
    }

    public String getMagic() { return magic; }
    public int getVersion() { return version; }
    public int getMasterHashSize() { return masterHashSize; }
    public int getTotalNumberOfLevels() { return totalNumberOfLevels; }
    public long getLvl1Offset() { return lvl1Offset; }
    public long getLvl1Size() { return lvl1Size; }
    public int getLvl1SBlockSize() { return lvl1SBlockSize; }
    public byte[] getReserved1() { return reserved1; }
    public long getLvl2Offset() { return lvl2Offset; }
    public long getLvl2Size() { return lvl2Size; }
    public int getLvl2SBlockSize() { return lvl2SBlockSize; }
    public byte[] getReserved2() { return reserved2; }
    public long getLvl3Offset() { return lvl3Offset; }
    public long getLvl3Size() { return lvl3Size; }
    public int getLvl3SBlockSize() { return lvl3SBlockSize; }
    public byte[] getReserved3() { return reserved3; }
    public long getLvl4Offset() { return lvl4Offset; }
    public long getLvl4Size() { return lvl4Size; }
    public int getLvl4SBlockSize() { return lvl4SBlockSize; }
    public byte[] getReserved4() { return reserved4; }
    public long getLvl5Offset() { return lvl5Offset; }
    public long getLvl5Size() { return lvl5Size; }
    public int getLvl5SBlockSize() { return lvl5SBlockSize; }
    public byte[] getReserved5() { return reserved5; }
    public long getLvl6Offset() { return lvl6Offset; }
    public long getLvl6Size() { return lvl6Size; }
    public int getLvl6SBlockSize() { return lvl6SBlockSize; }
    public byte[] getReserved6() { return reserved6; }
    public byte[] getSignatureSalt() { return signatureSalt; }
    public byte[] getMasterHash() { return masterHash; }
    public byte[] getReservedTail() { return reservedTail; }
}
