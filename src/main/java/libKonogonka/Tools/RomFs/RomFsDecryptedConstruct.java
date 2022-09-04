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

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;
/**
 * Construct header for RomFs and obtain root fileSystemEntry (meta information)
* */
class RomFsDecryptedConstruct {
    private Level6Header header;

    private FileSystemEntry rootEntry;
    private final BufferedInputStream fileBufferedInputStream;
    private int headerSize;
    private byte[] directoryMetadataTable;
    private byte[] fileMetadataTable;

    RomFsDecryptedConstruct(File decryptedFsImageFile, long level6offset) throws Exception{
        if (level6offset < 0)
            throw new Exception("Incorrect Level 6 Offset");

        fileBufferedInputStream = new BufferedInputStream(Files.newInputStream(decryptedFsImageFile.toPath()));
        fastForwardBySkippingBytes(level6offset);

        detectHeaderSize();
        constructHeader();

        fastForwardBySkippingBytes(header.getDirectoryMetadataTableOffset() - headerSize);

        directoryMetadataTableLengthCheck();
        directoryMetadataTableConstruct();

        fastForwardBySkippingBytes(header.getFileMetadataTableOffset() - header.getFileHashTableOffset());

        fileMetadataTableLengthCheck();
        fileMetadataTableConstruct();

        constructRootFilesystemEntry();

        fileBufferedInputStream.close();
    }
    private void detectHeaderSize() throws Exception{
        fileBufferedInputStream.mark(0x10);
        byte[] lv6HeaderSizeRaw = new byte[0x8];
        if (fileBufferedInputStream.read(lv6HeaderSizeRaw) != 0x8)
            throw new Exception("Failed to read header size");
        headerSize = Converter.getLEint(lv6HeaderSizeRaw, 0);
        fileBufferedInputStream.reset();
    }

    private void constructHeader() throws Exception{
        byte[] rawDataChunk = new byte[headerSize];

        if (fileBufferedInputStream.read(rawDataChunk) != headerSize)
            throw new Exception(String.format("Failed to read header (0x%x)", headerSize));

        this.header = new Level6Header(rawDataChunk);
    }

    private void directoryMetadataTableLengthCheck() throws Exception{
        if (header.getDirectoryMetadataTableLength() < 0)
            throw new Exception("Not supported operation.");
    }

    private void directoryMetadataTableConstruct() throws Exception{
        directoryMetadataTable = new byte[(int) header.getDirectoryMetadataTableLength()];
        if (fileBufferedInputStream.read(directoryMetadataTable) != (int) header.getDirectoryMetadataTableLength())
            throw new Exception("Failed to read "+header.getDirectoryMetadataTableLength());
    }

    private void fileMetadataTableLengthCheck() throws Exception{
        if (header.getFileMetadataTableLength() < 0)
            throw new Exception("Not supported operation.");
    }

    private void fileMetadataTableConstruct() throws Exception{
        fileMetadataTable = new byte[(int) header.getFileMetadataTableLength()];

        if (fileBufferedInputStream.read(fileMetadataTable) != (int) header.getFileMetadataTableLength())
            throw new Exception("Failed to read "+header.getFileMetadataTableLength());
    }

    private void constructRootFilesystemEntry() throws Exception{
        rootEntry = new FileSystemEntry(directoryMetadataTable, fileMetadataTable);
        //rootEntry.printTreeForDebug();
    }

    private void fastForwardBySkippingBytes(long size) throws Exception{
        long mustSkip = size;
        long skipped = 0;
        while (mustSkip > 0){
            skipped += fileBufferedInputStream.skip(mustSkip);
            mustSkip = size - skipped;
        }
    }

    Level6Header getHeader() { return header; }
    FileSystemEntry getRootEntry(){ return rootEntry; }
    byte[] getDirectoryMetadataTable() { return directoryMetadataTable; }
    byte[] getFileMetadataTable() { return fileMetadataTable;}
}
