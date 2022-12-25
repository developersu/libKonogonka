/*
    Copyright 2019-2022 Dmitry Isaenko
     
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
package libKonogonka.RomFsDecrypted;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import libKonogonka.Tools.RomFs.FileSystemEntry;
import libKonogonka.Tools.RomFs.RomFsProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// log.fatal("Configuration File Defined To Be :: "+System.getProperty("log4j.configurationFile"));

public class RomFsDecryptedTest {
    @TempDir
    Path mainLogsDir;

    private static final String decryptedFileAbsolutePath = "./FilesForTests/NCAContent_0 [lv6 147456].bin";
    private File decryptedFile;
    long lv6offset;
    RomFsProvider provider;

    @Disabled
    @DisplayName("RomFsDecryptedProvider: Overall and export")
    @Test
    void romFsValidation() throws Exception{
        makeFile();
        parseLv6offsetFromFileName();
        makeProvider();
        provider.printDebug();
        export();
    }

    void makeFile(){
        decryptedFile = new File(decryptedFileAbsolutePath);
    }
    void parseLv6offsetFromFileName(){
        lv6offset = Long.parseLong(decryptedFile.getName().replaceAll("(^.*lv6\\s)|(]\\.bin)", ""));
    }
    void makeProvider() throws Exception{
        provider = new RomFsProvider(decryptedFile, lv6offset);
    }

    void export() throws Exception {
        System.out.println("lv6offset = "+lv6offset);

        FileSystemEntry entry = provider.getRootEntry();
        System.out.print(" entry.getFileOffset(): " + entry.getOffset() +
                "\n entry.getFileSize():   " + entry.getSize() +
                "\nExport new.......");
        exportFolderContent(entry, "/tmp/decrypted_brandnew");
        System.out.println("done");
        /*----------------------------------------------------------------------
        System.out.print("Export legacy....");
        exportFolderContentLegacy(entry, "/tmp/decrypted_legacy");
        System.out.println("done"); */
    }

    private void exportFolderContent(FileSystemEntry entry, String saveToLocation) throws Exception{
        File contentFile = new File(saveToLocation + entry.getName());
        contentFile.mkdirs();
        String currentDirPath = saveToLocation + entry.getName() + File.separator;
        for (FileSystemEntry fse : entry.getContent()){
            if (fse.isDirectory())
                exportFolderContent(fse, currentDirPath);
            else
                exportSingleFile(fse, currentDirPath);
        }
    }
    private void exportSingleFile(FileSystemEntry entry, String saveToLocation) throws Exception {
        File contentFile = new File(saveToLocation + entry.getName());
        try(BufferedOutputStream extractedFileBOS = new BufferedOutputStream(Files.newOutputStream(contentFile.toPath()));
            BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(decryptedFile.toPath()))) {
            long skipBytes = entry.getOffset()+
                    //ncaProvider.getTableEntry1().getMediaStartOffset()*0x200+
                    provider.getHeader().getFileDataOffset()+
                    lv6offset;
            if (skipBytes != stream.skip(skipBytes))
                throw new Exception("Can't skip");

            int blockSize = 0x200;
            if (entry.getSize() < 0x200)
                blockSize = (int) entry.getSize();

            long i = 0;
            byte[] block = new byte[blockSize];

            int actuallyRead;

            while (true) {
                if ((actuallyRead = stream.read(block)) != blockSize)
                    throw new Exception("Read failure. Block Size: "+blockSize+", actuallyRead: "+actuallyRead);
                extractedFileBOS.write(block);
                i += blockSize;
                if ((i + blockSize) >= entry.getSize()) {
                    blockSize = (int) (entry.getSize() - i);
                    if (blockSize == 0)
                        break;
                    block = new byte[blockSize];
                }
            }
        }
    }
/*
    private void exportFolderContentLegacy(FileSystemEntry entry, String saveToLocation) throws Exception{
        File contentFile = new File(saveToLocation + entry.getName());
        contentFile.mkdirs();
        String currentDirPath = saveToLocation + entry.getName() + File.separator;
        for (FileSystemEntry fse : entry.getContent()){
            if (fse.isDirectory())
                exportFolderContentLegacy(fse, currentDirPath);
            else
                exportSingleFileLegacy(fse, currentDirPath);
        }
    }

    private void exportSingleFileLegacy(FileSystemEntry entry, String saveToLocation) throws Exception {
        File contentFile = new File(saveToLocation + entry.getName());

        BufferedOutputStream extractedFileBOS = new BufferedOutputStream(Files.newOutputStream(contentFile.toPath()));
        PipedInputStream pis = provider.getContent(entry);

        byte[] readBuf = new byte[0x200]; // 8mb NOTE: consider switching to 1mb 1048576
        int readSize;

        while ((readSize = pis.read(readBuf)) > -1) {
            extractedFileBOS.write(readBuf, 0, readSize);
            readBuf = new byte[0x200];
        }

        extractedFileBOS.close();
    }*/
}