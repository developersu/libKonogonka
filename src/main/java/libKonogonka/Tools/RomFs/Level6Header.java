/*
 * Copyright 2019-2022 Dmitry Isaenko
 *
 * This file is part of libKonogonka.
 *
 * libKonogonka is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * libKonogonka is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with libKonogonka.  If not, see <https://www.gnu.org/licenses/>.
 */

package libKonogonka.Tools.RomFs;

import libKonogonka.Converter;
import libKonogonka.RainbowDump;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 * This class stores information contained in Level 6 Header of the RomFS image
 * ------------------------------------
 * | Header Length (usually 0x50)     |
 * | Directory Hash Table Offset      | Not used by this library | '<< 32' to get real offset: see implementation
 * | Directory Hash Table Length      | Not used by this library
 * | Directory Metadata Table Offset  |
 * | Directory Metadata Table Length  |
 * | File Hash Table Offset           | Not used by this library
 * | File Hash Table Length           | Not used by this library
 * | File Metadata Table Offset       |
 * | File Metadata Table Length       |
 * | File Data Offset                 |
 * ------------------------------------
 * */
public class Level6Header {
    private final static Logger log = LogManager.getLogger(Level6Header.class);

    private final long headerLength;
    private long directoryHashTableOffset;
    private final long directoryHashTableLength;
    private final long directoryMetadataTableOffset;
    private final long directoryMetadataTableLength;
    private final long fileHashTableOffset;
    private final long fileHashTableLength;
    private final long fileMetadataTableOffset;
    private final long fileMetadataTableLength;
    private final long fileDataOffset;
    
    private final byte[] headerBytes;
    private int _cursor;
    
    Level6Header(byte[] headerBytes) throws Exception{
        this.headerBytes = headerBytes;
        if (headerBytes.length < 0x50)
            throw new Exception("Level 6 Header section is too small");
        headerLength = getNext();
        directoryHashTableOffset = getNext();
        directoryHashTableOffset <<= 32;
        directoryHashTableLength = getNext();
        directoryMetadataTableOffset = getNext();
        directoryMetadataTableLength = getNext();
        fileHashTableOffset = getNext();
        fileHashTableLength = getNext();
        fileMetadataTableOffset = getNext();
        fileMetadataTableLength = getNext();
        fileDataOffset = getNext();
    }
    
    private long getNext(){
        final long result = Converter.getLEint(headerBytes, _cursor);
        _cursor += 0x8;
        return result;
    }
    
    public long getHeaderLength() { return headerLength; }
    public long getDirectoryHashTableOffset() { return directoryHashTableOffset; }
    public long getDirectoryHashTableLength() { return directoryHashTableLength; }
    public long getDirectoryMetadataTableOffset() { return directoryMetadataTableOffset; }
    public long getDirectoryMetadataTableLength() { return directoryMetadataTableLength; }
    public long getFileHashTableOffset() { return fileHashTableOffset; }
    public long getFileHashTableLength() { return fileHashTableLength; }
    public long getFileMetadataTableOffset() { return fileMetadataTableOffset; }
    public long getFileMetadataTableLength() { return fileMetadataTableLength; }
    public long getFileDataOffset() { return fileDataOffset; }
    
    public void printDebugInfo(){
        log.debug("== Level 6 Header ==\n" +
                "Header Length (usually 0x50)    "+ RainbowDump.formatDecHexString(headerLength)+"   (size of this structure within first 0x200 block of LEVEL 6 part)\n" +
                "Directory Hash Table Offset     "+ RainbowDump.formatDecHexString(directoryHashTableOffset)+"   (against THIS block where HEADER contains)\n" +
                "Directory Hash Table Length     "+ RainbowDump.formatDecHexString(directoryHashTableLength) + "\n" +
                "Directory Metadata Table Offset "+ RainbowDump.formatDecHexString(directoryMetadataTableOffset) + "\n" +
                "Directory Metadata Table Length "+ RainbowDump.formatDecHexString(directoryMetadataTableLength) + "\n" +
                "File Hash Table Offset          "+ RainbowDump.formatDecHexString(fileHashTableOffset) + "\n" +
                "File Hash Table Length          "+ RainbowDump.formatDecHexString(fileHashTableLength) + "\n" +
                "File Metadata Table Offset      "+ RainbowDump.formatDecHexString(fileMetadataTableOffset) + "\n" +
                "File Metadata Table Length      "+ RainbowDump.formatDecHexString(fileMetadataTableLength) + "\n" +
                "File Data Offset                "+ RainbowDump.formatDecHexString(fileDataOffset) + "\n" +
                "-------------------------------------------------------------"
        );
    }
}
