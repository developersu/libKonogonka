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

import libKonogonka.Tools.ExportAble;
import libKonogonka.Tools.RomFs.view.DirectoryMetaTablePlainView;
import libKonogonka.Tools.RomFs.view.FileMetaTablePlainView;
import libKonogonka.ctraes.InFileStreamProducer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class RomFsProvider extends ExportAble {
    private final static Logger log = LogManager.getLogger(RomFsProvider.class);

    private final InFileStreamProducer producer;
    private final long level6Offset;
    private final Level6Header level6Header;
    private final FileSystemEntry rootEntry;
    private long mediaStartOffset;
    // Used only for debug
    private final byte[] directoryMetadataTable;
    private final byte[] fileMetadataTable;

    public RomFsProvider(File decryptedFsImageFile, long level6offset) throws Exception{
        this.producer = new InFileStreamProducer(decryptedFsImageFile);
        RomFsConstruct construct = new RomFsConstruct(producer, level6offset);
        this.level6Offset = level6offset;
        this.level6Header = construct.getHeader();
        this.rootEntry = construct.getRootEntry();
        this.directoryMetadataTable = construct.getDirectoryMetadataTable();
        this.fileMetadataTable = construct.getFileMetadataTable();
    }

    public RomFsProvider(InFileStreamProducer producer,
                         long level6Offset,
                         long offsetPositionInFile,
                         long mediaStartOffset
    ) throws Exception{
        this.producer = producer;
        this.mediaStartOffset = mediaStartOffset;
        RomFsConstruct construct = new RomFsConstruct(producer, level6Offset, offsetPositionInFile);
        this.level6Offset = level6Offset;
        this.level6Header = construct.getHeader();
        this.rootEntry = construct.getRootEntry();
        this.directoryMetadataTable = construct.getDirectoryMetadataTable();
        this.fileMetadataTable = construct.getFileMetadataTable();
    }

    public long getLevel6Offset() { return level6Offset; }
    public Level6Header getHeader() {return level6Header;}
    public FileSystemEntry getRootEntry() { return rootEntry; }

    public boolean exportContent(String saveToLocation, FileSystemEntry entry){
        try{
            if (! saveToLocation.endsWith(File.separator))
                saveToLocation += File.separator;

            if (entry.isDirectory())
                exportFolderContent(entry, saveToLocation);
            else
                exportSingleFile(entry, saveToLocation);
        }
        catch (Exception e){
            log.error("File export failure", e);
            return false;
        }
        return true;
    }

    private void exportFolderContent(FileSystemEntry entry, String saveToLocation) throws Exception{
        File contentFile = new File(saveToLocation + entry.getName());
        contentFile.mkdirs();
        String currentDirPath = saveToLocation + entry.getName() + File.separator;
        for (FileSystemEntry fileEntry : entry.getContent()){
            if (fileEntry.isDirectory())
                exportFolderContent(fileEntry, currentDirPath);
            else
                exportSingleFile(fileEntry, currentDirPath);
        }
    }

    private void exportSingleFile(FileSystemEntry entry, String saveToLocation) throws Exception {
        stream = producer.produce();
        long skipBytes = entry.getOffset() + mediaStartOffset * 0x200 + level6Header.getFileDataOffset() + level6Offset;
        export(saveToLocation, entry.getName(), skipBytes, entry.getSize());
    }

    public InFileStreamProducer getStreamProducer(FileSystemEntry entry) throws Exception{
        if (entry.isDirectory())
            throw new Exception("Directory entries are not supported");
        return producer.getSuccessor(
                entry.getOffset() + mediaStartOffset * 0x200 + level6Header.getFileDataOffset() + level6Offset);
    }

    public File getFile(){
        return producer.getFile();
    }

    public void printDebug(){
        level6Header.printDebugInfo();
        new DirectoryMetaTablePlainView(level6Header.getDirectoryMetadataTableLength(), directoryMetadataTable);
        new FileMetaTablePlainView(level6Header.getFileMetadataTableLength(), fileMetadataTable);
        rootEntry.printTreeForDebug();
    }
}
