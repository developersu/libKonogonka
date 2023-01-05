/*
    Copyright 2019-2023 Dmitry Isaenko

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
package libKonogonka.Tools.other.System2.ini1;

import libKonogonka.Converter;
import libKonogonka.RainbowDump;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class Ini1Header {
    private final static Logger log = LogManager.getLogger(Ini1Header.class);

    private final String magic;
    private final int size;
    private final int kipNumber;
    private final byte[] reserved;

    public Ini1Header(byte[] headerBytes){
        this.magic = new String(headerBytes, 0, 4);
        this.size = Converter.getLEint(headerBytes, 0x4);
        this.kipNumber = Converter.getLEint(headerBytes, 0x8);
        this.reserved = Arrays.copyOfRange(headerBytes, 0xC, 0x10);
    }

    public String getMagic() { return magic; }
    public int getSize() { return size; }
    public int getKipNumber() { return kipNumber; }
    public byte[] getReserved() { return reserved; }

    public void printDebug(){
        log.debug("..:: INI1 Header ::..\n" +
                "Magic         : " + magic + "\n" +
                "Size          : " + RainbowDump.formatDecHexString(size) + "\n" +
                "KPIs number   : " + RainbowDump.formatDecHexString(kipNumber) + "\n" +
                "Reserved      : " + Converter.byteArrToHexStringAsLE(reserved));
    }
}
