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
package libKonogonka.Tools.RomFs;

import libKonogonka.Converter;
import libKonogonka.ctraes.AesCtrBufferedInputStream;
import libKonogonka.ctraes.AesCtrDecryptSimple;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;

public class RomFsConstruct {
    //private final static Logger log = LogManager.getLogger(RomFsConstruct.class);

    private Level6Header header;

    private FileSystemEntry rootEntry;
    private final BufferedInputStream stream;
    private int headerSize;
    private byte[] directoryMetadataTable;
    private byte[] fileMetadataTable;


    private final File file;
    private long offsetPositionInFile;
    private final long level6Offset;

    RomFsConstruct(File file,
                   long level6Offset) throws Exception{
        if (level6Offset < 0)
            throw new Exception("Incorrect Level 6 Offset");
        this.file = file;
        this.level6Offset = level6Offset;
        this.stream = new BufferedInputStream(Files.newInputStream(file.toPath()));
        constructEverything();
    }

    RomFsConstruct(File file,
                   long ncaOffset, // NCA offset position
                   long level6Offset,
                   AesCtrDecryptSimple decryptor,
                   long mediaStartOffset,
                   long mediaEndOffset
                            ) throws Exception {
        if (level6Offset < 0)
            throw new Exception("Incorrect Level 6 Offset");
        this.file = file;
        this.level6Offset = level6Offset;
        this.offsetPositionInFile = ncaOffset + (mediaStartOffset * 0x200);
        this.stream = new AesCtrBufferedInputStream(
                decryptor,
                ncaOffset,
                mediaStartOffset,
                mediaEndOffset,
                Files.newInputStream(file.toPath()));
        constructEverything();
    }

    private void constructEverything() throws Exception{
        goToStartingPosition();

        constructHeader();

        directoryMetadataTableLengthCheck();
        directoryMetadataTableConstruct();

        fileMetadataTableLengthCheck();
        fileMetadataTableConstruct();

        constructRootFilesystemEntry();

        stream.close();
    }

    private void goToStartingPosition() throws Exception{
        skipBytes(offsetPositionInFile + level6Offset);
    }

    private void constructHeader() throws Exception{
        byte[] headerSizeBytes = detectHeaderSize();
        byte[] rawDataChunk = new byte[headerSize-0x8];

        if (stream.read(rawDataChunk) != headerSize-0x8)
            throw new Exception(String.format("Failed to read header (0x%x)", (headerSize-0x8)));
        byte[] lv6headerBytes = new byte[headerSize];
        System.arraycopy(headerSizeBytes, 0, lv6headerBytes, 0, 0x8);
        System.arraycopy(rawDataChunk, 0, lv6headerBytes, 0x8, headerSize-0x8);
        this.header = new Level6Header(lv6headerBytes);
    }
    private byte[] detectHeaderSize() throws Exception{
        byte[] lv6HeaderSizeRaw = new byte[0x8];
        if (stream.read(lv6HeaderSizeRaw) != 0x8)
            throw new Exception("Failed to read header size");
        headerSize = Converter.getLEint(lv6HeaderSizeRaw, 0);
        return lv6HeaderSizeRaw;
    }

    private void directoryMetadataTableLengthCheck() throws Exception{
        if (header.getDirectoryMetadataTableLength() < 0)
            throw new Exception("Not supported: DirectoryMetadataTableLength < 0");
    }
    private void directoryMetadataTableConstruct() throws Exception{
        skipBytes(header.getDirectoryMetadataTableOffset() - headerSize);

        directoryMetadataTable = new byte[(int) header.getDirectoryMetadataTableLength()];
        if (stream.read(directoryMetadataTable) != (int) header.getDirectoryMetadataTableLength())
            throw new Exception("Failed to read "+header.getDirectoryMetadataTableLength());
    }

    private void fileMetadataTableLengthCheck() throws Exception{
        if (header.getFileMetadataTableLength() < 0)
            throw new Exception("Not supported: FileMetadataTableLength < 0");
    }
    private void fileMetadataTableConstruct() throws Exception{
        skipBytes(header.getFileMetadataTableOffset() - header.getFileHashTableOffset());

        fileMetadataTable = new byte[(int) header.getFileMetadataTableLength()];
        if (stream.read(fileMetadataTable) != (int) header.getFileMetadataTableLength())
            throw new Exception("Failed to read "+header.getFileMetadataTableLength());
    }

    private void constructRootFilesystemEntry() throws Exception{
        try {
            rootEntry = new FileSystemEntry(directoryMetadataTable, fileMetadataTable);
        }
        catch (Exception e){
            throw new Exception("File: " + file.getName(), e);
        }
    }

    private void skipBytes(long size) throws Exception{
        long mustSkip = size;
        long skipped = 0;
        while (mustSkip > 0){
            skipped += stream.skip(mustSkip);
            mustSkip = size - skipped;
        }
    }

    Level6Header getHeader() { return header; }
    FileSystemEntry getRootEntry(){ return rootEntry; }
    byte[] getDirectoryMetadataTable() { return directoryMetadataTable; }
    byte[] getFileMetadataTable() { return fileMetadataTable;}
}
