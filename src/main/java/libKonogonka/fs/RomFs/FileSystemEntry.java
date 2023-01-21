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

package libKonogonka.fs.RomFs;

import libKonogonka.Converter;
import libKonogonka.fs.RomFs.view.FileSystemTreeViewMaker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FileSystemEntry {
    private final static Logger log = LogManager.getLogger(FileSystemEntry.class);

    private boolean directoryFlag;
    private String name;
    private final List<FileSystemEntry> content;

    private static byte[] dirsMetadataTable;
    private static byte[] filesMetadataTable;

    private long offset;
    private long size;

    public FileSystemEntry(byte[] dirsMetadataTable, byte[] filesMetadataTable) throws Exception{
        FileSystemEntry.dirsMetadataTable = dirsMetadataTable;
        FileSystemEntry.filesMetadataTable = filesMetadataTable;
        this.content = new ArrayList<>();
        this.directoryFlag = true;
        DirectoryMetaData rootDirectoryMetaData = new DirectoryMetaData();
        if (rootDirectoryMetaData.dirName.isEmpty())
            this.name = "ROOT";
        else
            this.name = rootDirectoryMetaData.dirName;
        if (rootDirectoryMetaData.parentDirectoryOffset != 0)
            throw new Exception("Offset of Parent Directory is incorrect. Expected 0 for root, received value is "+ rootDirectoryMetaData.parentDirectoryOffset);
        if (rootDirectoryMetaData.nextSiblingDirectoryOffset != -1)
            throw new Exception("Offset of next Sibling Directory is incorrect. Expected -1 for root, received value is "+ rootDirectoryMetaData.nextSiblingDirectoryOffset);
        if (rootDirectoryMetaData.firstSubdirectoryOffset != -1)
            content.add(getDirectory(rootDirectoryMetaData.firstSubdirectoryOffset));
        if (rootDirectoryMetaData.firstFileOffset != -1)
            content.add(getFile(this, rootDirectoryMetaData.firstFileOffset));
        content.sort(Comparator.comparingLong(FileSystemEntry::getOffset));
    }

    private FileSystemEntry(){
        this.content = new ArrayList<>();
    }

    private FileSystemEntry getDirectory(int childDirMetaPosition){
        FileSystemEntry fileSystemEntry = new FileSystemEntry();
        fileSystemEntry.directoryFlag = true;

        DirectoryMetaData directoryMetaData = new DirectoryMetaData(childDirMetaPosition);
        fileSystemEntry.name = directoryMetaData.dirName;

        if (directoryMetaData.nextSiblingDirectoryOffset != -1)
            this.content.add(getDirectory(directoryMetaData.nextSiblingDirectoryOffset));

        if (directoryMetaData.firstSubdirectoryOffset != -1)
            fileSystemEntry.content.add(getDirectory(directoryMetaData.firstSubdirectoryOffset));

        if (directoryMetaData.firstFileOffset != -1)
            fileSystemEntry.content.add(getFile(fileSystemEntry, directoryMetaData.firstFileOffset));

        fileSystemEntry.content.sort(Comparator.comparingLong(FileSystemEntry::getOffset));

        return fileSystemEntry;
    }

    private FileSystemEntry getFile(FileSystemEntry directoryContainer, int childFileMetaPosition){
        FileSystemEntry fileSystemEntry = new FileSystemEntry();
        fileSystemEntry.directoryFlag = false;

        FileMetaData fileMetaData = new FileMetaData(childFileMetaPosition);
        fileSystemEntry.name = fileMetaData.fileName;
        fileSystemEntry.offset = fileMetaData.fileDataRealOffset;
        fileSystemEntry.size = fileMetaData.fileDataRealLength;
        if (fileMetaData.nextSiblingFileOffset != -1)
            directoryContainer.content.add(getFile(directoryContainer, fileMetaData.nextSiblingFileOffset) );

        return fileSystemEntry;
    }

    public boolean isDirectory() { return directoryFlag; }
    public boolean isFile() { return ! directoryFlag; }
    public long getOffset() { return offset; }
    public long getSize() { return size; }
    public List<FileSystemEntry> getContent() { return content; }
    public String getName(){ return name; }


    private static class DirectoryMetaData {
        private final int parentDirectoryOffset;
        private final int nextSiblingDirectoryOffset;
        private final int firstSubdirectoryOffset;
        private final int firstFileOffset;
        private final int nextHashTableBucketDirectoryOffset;

        private final String dirName;

        private DirectoryMetaData(){
            this(0);
        }
        private DirectoryMetaData(int childDirMetaPosition){
            int i = childDirMetaPosition;
            parentDirectoryOffset = Converter.getLEint(dirsMetadataTable, i);
            i += 4;
            nextSiblingDirectoryOffset = Converter.getLEint(dirsMetadataTable, i);
            i += 4;
            firstSubdirectoryOffset = Converter.getLEint(dirsMetadataTable, i);
            i += 4;
            firstFileOffset = Converter.getLEint(dirsMetadataTable, i);
            i += 4;
            nextHashTableBucketDirectoryOffset = Converter.getLEint(dirsMetadataTable, i);
            /*
            if (nextHashTableBucketDirectoryOffset < 0) {
                log.debug("nextHashTableBucketDirectoryOffset: "+ nextHashTableBucketDirectoryOffset);
            }
            //*/
            i += 4;
            int dirNameLength = Converter.getLEint(dirsMetadataTable, i);

            if (dirNameLength > 0) {
                i += 4;
                dirName = new String(Arrays.copyOfRange(dirsMetadataTable, i, i + dirNameLength), StandardCharsets.UTF_8);
            }
            else {
                dirName = "";
                // log.debug("Dir Name Length: "+dirNameLength);
            }
            //i += getRealNameSize(dirNameLength);
        }

        private int getRealNameSize(int value){
            if (value % 4 == 0)
                return value;
            return value + 4 - value % 4;
        }
    }
    private static class FileMetaData {
        private final int nextSiblingFileOffset;
        private final long fileDataRealOffset;
        private final long fileDataRealLength;
        private final int nextHashTableBucketFileOffset;

        private String fileName;

        private FileMetaData(){
            this(0);
        }
        
        private FileMetaData(int childFileMetaPosition){
            int i = childFileMetaPosition;
            // int containingDirectoryOffset = LoperConverter.getLEint(filesMetadataTable, i); // never used
            i += 4;
            nextSiblingFileOffset = Converter.getLEint(filesMetadataTable, i);
            i += 4;
            fileDataRealOffset = Converter.getLElong(filesMetadataTable, i);
            i += 8;
            fileDataRealLength = Converter.getLElong(filesMetadataTable, i);
            i += 8;
            nextHashTableBucketFileOffset = Converter.getLEint(filesMetadataTable, i);
            /*
            if (nextHashTableBucketFileOffset < 0) {
                log.debug("nextHashTableBucketFileOffset: "+ nextHashTableBucketFileOffset);
            }
            //*/
            i += 4;
            int fileNameLength = Converter.getLEint(filesMetadataTable, i);
            if (fileNameLength > 0) {
                i += 4;
                fileName = "";
                try {
                    fileName = new String(Arrays.copyOfRange(filesMetadataTable, i, i + fileNameLength), StandardCharsets.UTF_8);
                }
                catch (Exception e){
                    log.debug("fileName sizes are: "+filesMetadataTable.length+"\t"+i+"\t"+i + fileNameLength+"\t\t"+nextHashTableBucketFileOffset, e);
                }
            }
            else {
                fileName = "";
                //log.debug("File Name Length: "+fileNameLength);
            }
            //i += getRealNameSize(fileNameLength);
        }
    }

    public void printTreeForDebug(int spacerForSizes){
        log.debug(FileSystemTreeViewMaker.make(content, spacerForSizes));
    }
    public void printTreeForDebug(){
        log.debug(FileSystemTreeViewMaker.make(content, 100));
    }
}
