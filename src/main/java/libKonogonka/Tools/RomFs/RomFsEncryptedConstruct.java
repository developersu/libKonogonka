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

import libKonogonka.ctraes.AesCtrDecryptSimple;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class RomFsEncryptedConstruct {

    private final long absoluteOffsetPosition;
    private final long level6Offset;

    private final RandomAccessFile raf;
    private final AesCtrDecryptSimple decryptor;
    private Level6Header header;
    private byte[] directoryMetadataTable;
    private byte[] fileMetadataTable;
    private FileSystemEntry rootEntry;

    RomFsEncryptedConstruct(File encryptedFsImageFile,
                            long romFsOffset,
                            long level6Offset,
                            AesCtrDecryptSimple decryptor,
                            long mediaStartOffset
                            ) throws Exception {

        if (level6Offset < 0)
            throw new Exception("Incorrect Level 6 Offset");

        this.raf = new RandomAccessFile(encryptedFsImageFile, "r");
        this.level6Offset = level6Offset;
        this.absoluteOffsetPosition = romFsOffset + (mediaStartOffset * 0x200);
        this.decryptor = decryptor;
        raf.seek(absoluteOffsetPosition + level6Offset);

        //Go to Level 6 header position
        decryptor.skipNext(level6Offset / 0x200);

        constructHeader();

        directoryMetadataTableLengthCheck();
        directoryMetadataTableConstruct();

        fileMetadataTableLengthCheck();
        fileMetadataTableConstruct();

        constructRootFilesystemEntry();

        raf.close();
    }

    private void constructHeader() throws Exception{
        // Decrypt data
        byte[] encryptedBlock = new byte[0x200];
        byte[] decryptedBlock;
        if (raf.read(encryptedBlock) == 0x200)
            decryptedBlock = decryptor.decryptNext(encryptedBlock);
        else
            throw new Exception("Failed to read header header (0x200 - block)");
        this.header = new Level6Header(decryptedBlock);
    }

    private void directoryMetadataTableLengthCheck() throws Exception{
        if (header.getDirectoryMetadataTableLength() < 0)
            throw new Exception("Not supported: DirectoryMetadataTableLength < 0");
    }
    private void directoryMetadataTableConstruct() throws Exception{
        directoryMetadataTable = readMetaTable(header.getDirectoryMetadataTableOffset(),
                header.getDirectoryMetadataTableLength());
    }

    private void fileMetadataTableLengthCheck() throws Exception{
        if (header.getFileMetadataTableLength() < 0)
            throw new Exception("Not supported: FileMetadataTableLength < 0");
    }
    private void fileMetadataTableConstruct() throws Exception{
        fileMetadataTable = readMetaTable(header.getFileMetadataTableOffset(),
                header.getFileMetadataTableLength());
    }

    private void constructRootFilesystemEntry() throws Exception{
        rootEntry = new FileSystemEntry(directoryMetadataTable, fileMetadataTable);
    }
    
    private byte[] readMetaTable(long metaOffset,
                                 long metaSize) throws Exception{
        byte[] encryptedBlock;
        byte[] decryptedBlock;
        byte[] metadataTable = new byte[(int) metaSize];
        //0
        decryptor.reset();

        long startBlock = metaOffset / 0x200;

        decryptor.skipNext(level6Offset / 0x200 + startBlock);

        raf.seek(absoluteOffsetPosition + level6Offset + startBlock * 0x200);

        //1
        long ignoreBytes = metaOffset - startBlock * 0x200;
        long currentPosition = 0;

        if (ignoreBytes > 0) {
            encryptedBlock = new byte[0x200];
            if (raf.read(encryptedBlock) == 0x200) {
                decryptedBlock = decryptor.decryptNext(encryptedBlock);
                // If we have extra-small file that is less than a block and even more
                if ((0x200 - ignoreBytes) > metaSize){
                    metadataTable = Arrays.copyOfRange(decryptedBlock, (int)ignoreBytes, 0x200);
                    return metadataTable;
                }
                else {
                    System.arraycopy(decryptedBlock, (int) ignoreBytes, metadataTable, 0, 0x200 - (int) ignoreBytes);
                    currentPosition = 0x200 - ignoreBytes;
                }
            }
            else {
                throw new Exception("Unable to get 512 bytes from 1st bock for Directory/File Metadata Table");
            }
            startBlock++;
        }
        long endBlock = (metaSize + ignoreBytes) / 0x200 + startBlock;  // <- pointing to place where any data related to this media-block ends

        //2
        int extraData = (int) ((endBlock - startBlock)*0x200 - (metaSize + ignoreBytes));

        if (extraData < 0)
            endBlock--;
        //3
        while ( startBlock < endBlock ) {
            encryptedBlock = new byte[0x200];
            if (raf.read(encryptedBlock) == 0x200) {
                decryptedBlock = decryptor.decryptNext(encryptedBlock);
                System.arraycopy(decryptedBlock, 0, metadataTable, (int) currentPosition, 0x200);
            }
            else
                throw new Exception("Unable to get 512 bytes from block for Directory/File Metadata Table");

            startBlock++;
            currentPosition += 0x200;
        }

        //4
        if (extraData != 0){                 // In case we didn't get what we want
            encryptedBlock = new byte[0x200];
            if (raf.read(encryptedBlock) == 0x200) {
                decryptedBlock = decryptor.decryptNext(encryptedBlock);
                System.arraycopy(decryptedBlock, 0, metadataTable, (int) currentPosition, Math.abs(extraData));
            }
            else
                throw new Exception("Unable to get 512 bytes from block for Directory/File Metadata Table");
        }

        return metadataTable;
    }

    Level6Header getHeader() { return header; }
    FileSystemEntry getRootEntry(){ return rootEntry; }
    byte[] getDirectoryMetadataTable() { return directoryMetadataTable; }
    byte[] getFileMetadataTable() { return fileMetadataTable;}
}
