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

public class RomFsEncryptedProvider implements IRomFsProvider{
    private final File file;
    private final long level6Offset;
    private final Level6Header level6Header;
    private final FileSystemEntry rootEntry;

    private final byte[] key;               // Used @ createDecryptor only
    private final byte[] sectionCTR;        // Used @ createDecryptor only
    private final long mediaStartOffset;    // Used @ createDecryptor only
    private final long absoluteOffsetPosition;

    //private long mediaEndOffset;    // We know this, but actually never use

    // Used only for debug
    private final byte[] directoryMetadataTable;
    private final byte[] fileMetadataTable;

    public RomFsEncryptedProvider(long level6Offset,
                                  File encryptedFsImageFile,
                                  long romFsOffsetPosition,
                                  byte[] key,
                                  byte[] sectionCTR,
                                  long mediaStartOffset
    ) throws Exception{
        this(level6Offset, encryptedFsImageFile, romFsOffsetPosition, key, sectionCTR, mediaStartOffset, -1);
    }

    public RomFsEncryptedProvider(long level6Offset,
                                  File encryptedFsImageFile,
                                  long romFsOffsetPosition,
                                  byte[] key,
                                  byte[] sectionCTR,
                                  long mediaStartOffset,
                                  long mediaEndOffset
    ) throws Exception{
        this.key = key;
        this.sectionCTR = sectionCTR;
        this.mediaStartOffset = mediaStartOffset;

        RomFsEncryptedConstruct construct = new RomFsEncryptedConstruct(encryptedFsImageFile,
                romFsOffsetPosition,
                level6Offset,
                createDecryptor(),
                mediaStartOffset);
        this.file = encryptedFsImageFile;
        this.level6Offset = level6Offset;
        this.level6Header = construct.getHeader();
        this.rootEntry = construct.getRootEntry();

        this.absoluteOffsetPosition = romFsOffsetPosition + (mediaStartOffset * 0x200);

        this.directoryMetadataTable = construct.getDirectoryMetadataTable();
        this.fileMetadataTable = construct.getFileMetadataTable();
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
    public PipedInputStream getContent(FileSystemEntry entry) throws Exception{
        if (entry.isDirectory())
            throw new Exception("Request of the binary stream for the folder entry is not supported (and doesn't make sense).");

        PipedOutputStream streamOut = new PipedOutputStream();
        PipedInputStream streamIn = new PipedInputStream(streamOut);
        long internalFileOffset = entry.getFileOffset();
        long internalFileSize = entry.getFileSize();

        Thread contentRetrievingThread = new Thread(new RomFsEncryptedContentRetrieve(
                file,
                streamOut,
                absoluteOffsetPosition,
                createDecryptor(),
                internalFileOffset,
                internalFileSize,
                level6Offset,
                level6Header.getFileDataOffset()
        ));
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
