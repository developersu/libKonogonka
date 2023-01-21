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

import java.util.Arrays;

import static libKonogonka.Converter.getLEint;
import static libKonogonka.Converter.getLElong;

public class SuperBlockPFS0 {
    private final byte[] SHA256hash;
    private final int blockSize;
    private final int layerCount;
    private final long hashTableOffset;
    private final long hashTableSize;
    private final long pfs0offset;
    private final long pfs0size;
    private final byte[] zeroes;

    /**
     * Also known as HierarchicalSha256Data
     * @param sbBytes - Chunk of data related for PFS0 Hash Data table
     */
    SuperBlockPFS0(byte[] sbBytes){
        SHA256hash = Arrays.copyOfRange(sbBytes, 0, 0x20);
        blockSize = getLEint(sbBytes, 0x20);
        layerCount = getLEint(sbBytes, 0x24);
        hashTableOffset = getLElong(sbBytes, 0x28);
        hashTableSize = getLElong(sbBytes, 0x30);
        pfs0offset = getLElong(sbBytes, 0x38);
        pfs0size = getLElong(sbBytes, 0x40);
        zeroes = Arrays.copyOfRange(sbBytes, 0x48, 0xf0);
    }

    public byte[] getSHA256hash() { return SHA256hash; }
    public int getBlockSize() { return blockSize; }
    public int getLayerCount() { return layerCount; }
    public long getHashTableOffset() { return hashTableOffset; }
    public long getHashTableSize() { return hashTableSize; }
    public long getPfs0offset() { return pfs0offset; }
    public long getPfs0size() { return pfs0size; }
    public byte[] getZeroes() { return zeroes; }
}
