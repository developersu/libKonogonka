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
package libKonogonka.Tools.NCA.NCASectionTableBlock;

import libKonogonka.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import static libKonogonka.Converter.getLElong;

public class SparseInfo {
    private final static Logger log = LogManager.getLogger(SparseInfo.class);

    private final long offset;
    private final long size;
    private final BucketTreeHeader bktr;
    private final long physicalOffset;
    private final byte[] generation;
    private final byte[] unknown;

    SparseInfo(byte[] rawTable){
        offset = getLElong(rawTable, 0);
        size = getLElong(rawTable, 0x8);
        bktr = new BucketTreeHeader(Arrays.copyOfRange(rawTable, 0x10, 0x20));
        physicalOffset = getLElong(rawTable, 0x20);
        generation = Arrays.copyOfRange(rawTable, 0x28, 0x2a);
        unknown = Arrays.copyOfRange(rawTable, 0x2a, 0x30);
    }

    public long getOffset() { return offset; }
    public long getSize() { return size; }
    public String getBktrMagic() { return bktr.getMagic(); }
    public int getBktrVersion() { return bktr.getVersion(); }
    public int getBktrEntryCount() { return bktr.getEntryCount(); }
    public byte[] getBktrUnknown() { return bktr.getUnknown(); }
    public long getPhysicalOffset() {return physicalOffset;}
    public byte[] getGeneration() {return generation;}
    public byte[] getUnknown() {return unknown;}

    public void printDebug(){
        log.debug("SparseInfo:\n" +
        "Offset            : " + offset + "\n" +
        "Size              : " + size + "\n");
        bktr.printDebug();
        log.debug(
        "\nPhysicalOffset    : " + physicalOffset + "\n" +
        "Generation        : " + Converter.byteArrToHexStringAsLE(generation) + "\n" +
        "Unknown           : " + Converter.byteArrToHexStringAsLE(unknown) + "\n");
    }
}