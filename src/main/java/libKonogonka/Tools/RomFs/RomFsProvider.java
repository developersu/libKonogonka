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

import libKonogonka.Tools.RomFs.view.DirectoryMetaTablePlainView;
import libKonogonka.Tools.RomFs.view.FileMetaTablePlainView;
import libKonogonka.ctraes.AesCtrDecryptSimple;

import java.io.File;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class RomFsProvider implements IRomFsProvider{
    private final File file;
    private final long level6Offset;
    private final Level6Header level6Header;
    private final FileSystemEntry rootEntry;

    private long ncaOffsetPosition;
    private byte[] key;               // Used @ createDecryptor only
    private byte[] sectionCTR;        // Used @ createDecryptor only
    private long mediaStartOffset;    // Used @ createDecryptor only
    private long mediaEndOffset;
    // Used only for debug
    private final byte[] directoryMetadataTable;
    private final byte[] fileMetadataTable;

    private final boolean encryptedAesCtr;

    public RomFsProvider(File decryptedFsImageFile, long level6offset) throws Exception{
        RomFsConstruct construct = new RomFsConstruct(decryptedFsImageFile, level6offset);
        this.file = decryptedFsImageFile;
        this.level6Offset = level6offset;
        this.level6Header = construct.getHeader();
        this.rootEntry = construct.getRootEntry();

        this.directoryMetadataTable = construct.getDirectoryMetadataTable();
        this.fileMetadataTable = construct.getFileMetadataTable();

        this.encryptedAesCtr = false;
    }

    public RomFsProvider(long level6Offset,
                         File encryptedFsImageFile,
                         long ncaOffsetPosition,
                         byte[] key,
                         byte[] sectionCTR,
                         long mediaStartOffset,
                         long mediaEndOffset
    ) throws Exception{
        this.key = key;
        this.sectionCTR = sectionCTR;
        this.mediaStartOffset = mediaStartOffset;
        this.mediaEndOffset = mediaEndOffset;
        this.ncaOffsetPosition = ncaOffsetPosition;

        RomFsConstruct construct = new RomFsConstruct(encryptedFsImageFile,
                ncaOffsetPosition,
                level6Offset,
                createDecryptor(),
                mediaStartOffset,
                mediaEndOffset);
        this.file = encryptedFsImageFile;
        this.level6Offset = level6Offset;
        this.level6Header = construct.getHeader();
        this.rootEntry = construct.getRootEntry();

        this.directoryMetadataTable = construct.getDirectoryMetadataTable();
        this.fileMetadataTable = construct.getFileMetadataTable();

        this.encryptedAesCtr = true;
    }
    private AesCtrDecryptSimple createDecryptor() throws Exception{
        return new AesCtrDecryptSimple(key, sectionCTR, mediaStartOffset * 0x200);
    }

    @Override
    public File getFile() { return file; }
    @Override
    public long getLevel6Offset() { return level6Offset; }
    @Override
    public Level6Header getHeader() {return level6Header;}
    @Override
    public FileSystemEntry getRootEntry() { return rootEntry; }
    @Override
    public PipedInputStream getContent(FileSystemEntry entry) throws Exception {
        if (encryptedAesCtr)
            return getContentAesCtrEncrypted(entry);
        return getContentNonEncrypted(entry);
    }
    public PipedInputStream getContentAesCtrEncrypted(FileSystemEntry entry) throws Exception{
        if (entry.isDirectory())
            throw new Exception("Request of the binary stream for the folder entry is not supported (and doesn't make sense).");

        PipedOutputStream streamOut = new PipedOutputStream();
        PipedInputStream streamIn = new PipedInputStream(streamOut);
        long internalFileOffset = entry.getOffset();
        long internalFileSize = entry.getSize();

        Thread contentRetrievingThread = new Thread(new RomFsContentRetrieve(
                file,
                streamOut,
                createDecryptor(),
                internalFileOffset,
                internalFileSize,
                level6Header.getFileDataOffset(),
                level6Offset,
                ncaOffsetPosition,
                mediaStartOffset,
                mediaEndOffset));
        contentRetrievingThread.start();
        return streamIn;
    }
    public PipedInputStream getContentNonEncrypted(FileSystemEntry entry) throws Exception{
        if (entry.isDirectory())
            throw new Exception("Request of the binary stream for the folder entry is not supported (and doesn't make sense).");

        PipedOutputStream streamOut = new PipedOutputStream();
        PipedInputStream streamIn = new PipedInputStream(streamOut);
        long internalFileRealPosition = level6Offset + level6Header.getFileDataOffset() + entry.getOffset();
        long internalFileSize = entry.getSize();

        Thread contentRetrievingThread = new Thread(new RomFsContentRetrieve(
                file,
                streamOut,
                internalFileRealPosition,
                internalFileSize));
        contentRetrievingThread.start();
        return streamIn;
    }

    @Override
    public void printDebug(){
        level6Header.printDebugInfo();
        new DirectoryMetaTablePlainView(level6Header.getDirectoryMetadataTableLength(), directoryMetadataTable);
        new FileMetaTablePlainView(level6Header.getFileMetadataTableLength(), fileMetadataTable);
        rootEntry.printTreeForDebug();
    }
}
