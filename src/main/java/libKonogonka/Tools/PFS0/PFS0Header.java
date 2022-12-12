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
package libKonogonka.Tools.PFS0;

import libKonogonka.Converter;
import libKonogonka.RainbowDump;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static libKonogonka.Converter.getLEint;
import static libKonogonka.Converter.getLElong;

public class PFS0Header {
    private final static Logger log = LogManager.getLogger(PFS0Header.class);

    private final String magic;
    private final int filesCount;
    private final int stringTableSize;
    private final byte[] padding;
    private final PFS0subFile[] pfs0subFiles;

    public PFS0Header(BufferedInputStream stream) throws Exception{
        byte[] fileStartingBytes = new byte[0x10];
        if (0x10 != stream.read(fileStartingBytes))
            throw new Exception("Reading stream suddenly ended while trying to read starting 0x10 bytes");
        // Check PFS0Provider
        magic = new String(fileStartingBytes, 0x0, 0x4, StandardCharsets.US_ASCII);
        if (! magic.equals("PFS0")){
            throw new Exception("Bad magic");
        }
        // Get files count
        filesCount = getLEint(fileStartingBytes, 0x4);
        if (filesCount <= 0 ) {
            throw new Exception("Files count is too small");
        }
        // Get string table
        stringTableSize = getLEint(fileStartingBytes, 0x8);
        if (stringTableSize <= 0 ){
            throw new Exception("String table is too small");
        }
        padding = Arrays.copyOfRange(fileStartingBytes, 0xc, 0x10);
        //-------------------------------------------------------------------
        pfs0subFiles = new PFS0subFile[filesCount];

        long[] offsetsSubFiles = new long[filesCount];
        long[] sizesSubFiles = new long[filesCount];
        int[] strTableOffsets = new int[filesCount];
        byte[][] zeroBytes = new byte[filesCount][];

        byte[] fileEntryTable = new byte[0x18];
        for (int i=0; i < filesCount; i++){
            if (0x18 != stream.read(fileEntryTable))
                throw new Exception("Reading stream suddenly ended while trying to read File Entry Table #"+i);

            offsetsSubFiles[i] = getLElong(fileEntryTable, 0);
            sizesSubFiles[i] = getLElong(fileEntryTable, 0x8);
            strTableOffsets[i] = getLEint(fileEntryTable, 0x10);
            zeroBytes[i] = Arrays.copyOfRange(fileEntryTable, 0x14, 0x18);
        }
        //*******************************************************************
        // Here pointer is in front of String table
        String[] subFileNames = new String[filesCount];
        byte[] stringTbl = new byte[stringTableSize];
        if (stream.read(stringTbl) != stringTableSize){
            throw new Exception("Read PFS0Provider String table failure. Can't read requested string table size ("+stringTableSize+")");
        }

        for (int i=0; i < filesCount; i++){
            int j = 0;
            while (stringTbl[strTableOffsets[i]+j] != (byte)0x00)
                j++;
            subFileNames[i] = new String(stringTbl, strTableOffsets[i], j, StandardCharsets.UTF_8);
        }
        for (int i = 0; i < filesCount; i++){
            pfs0subFiles[i] = new PFS0subFile(
                    subFileNames[i],
                    offsetsSubFiles[i],
                    sizesSubFiles[i],
                    zeroBytes[i]);
        }
    }

    public String getMagic() {return magic;}
    public int getFilesCount() {return filesCount;}
    public int getStringTableSize() {return stringTableSize;}
    public byte[] getPadding() {return padding;}
    public PFS0subFile[] getPfs0subFiles() {return pfs0subFiles;}

    public void printDebug(){
        log.debug(".:: PFS0Header ::.\n" +
                "Magic                     " + magic + "\n" +
                "Files count               " + RainbowDump.formatDecHexString(filesCount) + "\n" +
                "String Table Size         " + RainbowDump.formatDecHexString(stringTableSize) + "\n" +
                "Padding                   " + Converter.byteArrToHexString(padding) + "\n\n"
        );
        for (PFS0subFile subFile : pfs0subFiles){
            log.debug("\nName:                     " + subFile.getName() + "\n" +
                    "Offset                    " + RainbowDump.formatDecHexString(subFile.getOffset()) + "\n" +
                    "Size                      " + RainbowDump.formatDecHexString(subFile.getSize()) + "\n" +
                    "Zeroes                    " + Converter.byteArrToHexString(subFile.getZeroes()) + "\n" +
                    "----------------------------------------------------------------"
            );
        }
    }
}
