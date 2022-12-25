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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static libKonogonka.Converter.getLEint;

public class BucketTreeHeader {
    private final static Logger log = LogManager.getLogger(BucketTreeHeader.class);

    private final String magic;
    private final int version;
    private final int entryCount;
    private final byte[] unknown;

    BucketTreeHeader(byte[] rawBytes){
        this.magic = new String(Arrays.copyOfRange(rawBytes, 0x0, 0x4), StandardCharsets.US_ASCII);
        this.version = getLEint(rawBytes, 0x4);
        this.entryCount = getLEint(rawBytes, 0x8);
        this.unknown = Arrays.copyOfRange(rawBytes, 0xc, 0x10);
    }

    public String getMagic() {return magic;}
    public int getVersion() {return version;}
    public int getEntryCount() {return entryCount;}
    public byte[] getUnknown() {return unknown;}

    public void printDebug(){
        log.debug("BucketTreeHeader\n" +
                "Magic       : " + magic  + "\n" +
                "Version     : " + version  + "\n" +
                "EntryCount  :" + entryCount  + "\n" +
                "Unknown     :" + Converter.byteArrToHexStringAsLE(unknown) + "\n"
        );
    }
}
