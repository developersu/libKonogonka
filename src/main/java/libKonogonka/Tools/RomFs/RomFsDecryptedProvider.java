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

import java.io.*;

public class RomFsDecryptedProvider implements IRomFsProvider{
    private final File file;
    private final long level6Offset;
    private final Level6Header level6Header;
    private final FileSystemEntry rootEntry;
    // Used only for debug
    private final byte[] directoryMetadataTable;
    private final byte[] fileMetadataTable;

    public RomFsDecryptedProvider(File decryptedFsImageFile, long level6offset) throws Exception{
        RomFsDecryptedConstruct  construct = new RomFsDecryptedConstruct(decryptedFsImageFile, level6offset);
        this.file = decryptedFsImageFile;
        this.level6Offset = level6offset;
        this.level6Header = construct.getHeader();
        this.rootEntry = construct.getRootEntry();

        this.directoryMetadataTable = construct.getDirectoryMetadataTable();
        this.fileMetadataTable = construct.getFileMetadataTable();
    }

    @Override
    public File getFile() { return file; }
    @Override
    public long getLevel6Offset() { return level6Offset; }
    @Override
    public Level6Header getHeader() { return level6Header; }
    @Override
    public FileSystemEntry getRootEntry() { return rootEntry; }
    @Override
    public PipedInputStream getContent(FileSystemEntry entry) throws Exception{
        if (entry.isDirectory())
            throw new Exception("Request of the binary stream for the folder entry is not supported (and doesn't make sense).");

        PipedOutputStream streamOut = new PipedOutputStream();
        PipedInputStream streamIn = new PipedInputStream(streamOut);
        long internalFileRealPosition = level6Offset + level6Header.getFileDataOffset() + entry.getFileOffset();
        long internalFileSize = entry.getFileSize();

        Thread contentRetrievingThread = new Thread(
                new RomFsDecryptedContentRetrieve(file, streamOut, internalFileRealPosition, internalFileSize));
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
