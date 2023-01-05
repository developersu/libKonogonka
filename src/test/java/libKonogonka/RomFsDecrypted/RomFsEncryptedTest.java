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

import libKonogonka.ctraes.AesCtrBufferedInputStream;
import libKonogonka.KeyChainHolder;
import libKonogonka.RainbowDump;
import libKonogonka.Tools.NCA.NCAProvider;
import libKonogonka.Tools.NCA.NCASectionTableBlock.NcaFsHeader;
import libKonogonka.Tools.RomFs.FileSystemEntry;
import libKonogonka.ctraes.AesCtrDecryptForMediaBlocks;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RomFsEncryptedTest {
    private static final String keysFileLocation = "./FilesForTests/prod.keys";
    private static final String xci_header_keyFileLocation = "./FilesForTests/xci_header_key.txt";
    private static final String ncaFileLocation = "./FilesForTests/PFS_RomFS.nca";
    private static KeyChainHolder keyChainHolder;
    private static NCAProvider ncaProvider;

    @Disabled
    @Order(1)
    @DisplayName("KeyChain test")
    @Test
    void keysChain() throws Exception{
        BufferedReader br = new BufferedReader(new FileReader(xci_header_keyFileLocation));
        String keyValue = br.readLine();
        br.close();

        if (keyValue == null)
            throw new Exception("Unable to retrieve xci_header_key");

        keyValue = keyValue.trim();
        keyChainHolder = new KeyChainHolder(keysFileLocation, keyValue);
    }

    @Disabled
    @Order(2)
    @DisplayName("RomFsEncryptedProvider: NCA provider quick test")
    @Test
    void ncaProvider() throws Exception{
        ncaProvider = new NCAProvider(new File(ncaFileLocation), keyChainHolder.getRawKeySet());
    }

    @Disabled
    @Order(3)
    @DisplayName("RomFsEncryptedProvider: RomFs test")
    @Test
    void romFsValidation() throws Exception{
        for (byte i = 0; i < 4; i++){
            System.out.println("..:: TEST SECTION #"+i+" ::..");
            if (ncaProvider.getSectionBlock(i).getFsType() == 0 && ncaProvider.getSectionBlock(i).getCryptoType() != 0){
                ncaProvider.getNCAContentProvider(i).getRomfs().printDebug();
                ncaProvider.getSectionBlock(i).printDebug();
                return;
            }
        }
    }

    @Disabled
    @Order(4)
    @DisplayName("RomFsEncryptedProvider: NCA Header Table Entries test")
    @Test
    void NcaHeaderTableEntryValidation() throws Exception{
        for (byte i = 0; i < 4; i++){
            NcaFsHeader header = ncaProvider.getSectionBlock(i);
            if (header != null)
                header.printDebug();
        }
    }

    private AesCtrDecryptForMediaBlocks decryptSimple;
    long ACBISoffsetPosition;
    long ACBISmediaStartOffset;
    long ACBISmediaEndOffset;
    @Disabled
    @Order(5)
    @DisplayName("AesCtrBufferedInputStream: RomFs AES-CTR dump validation")
    @Test
    void AesCtrBufferedInputStreamTest() throws Exception {
        File nca = new File(ncaFileLocation);
        System.out.println("NCA SIZE: "+RainbowDump.formatDecHexString(nca.length()));
        System.out.println(ncaProvider.getSectionBlock1().getSuperBlockIVFC().getLvl6Offset());

        FileSystemEntry entry = ncaProvider.getNCAContentProvider(1).getRomfs().getRootEntry();
        System.out.println(" entry.getFileOffset(): " + entry.getOffset());
        System.out.println(" entry.getFileSize():    " + entry.getSize());

        ACBISoffsetPosition = 0;
        ACBISmediaStartOffset = ncaProvider.getTableEntry1().getMediaStartOffset();
        ACBISmediaEndOffset = ncaProvider.getTableEntry1().getMediaEndOffset();

        decryptSimple = new AesCtrDecryptForMediaBlocks(
                ncaProvider.getDecryptedKey2(),
                ncaProvider.getSectionBlock1().getSectionCTR(),
                ncaProvider.getTableEntry1().getMediaStartOffset()*0x200);

        exportFolderContent(entry, "/tmp/brandnew");
        //----------------------------------------------------------------------
        //exportFolderContentLegacy(entry, "/tmp/legacy");
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

        BufferedOutputStream extractedFileBOS = new BufferedOutputStream(Files.newOutputStream(contentFile.toPath()));
        //---
        Path filePath = new File(ncaFileLocation).toPath();
        InputStream is = Files.newInputStream(filePath);

        AesCtrBufferedInputStream aesCtrBufferedInputStream = new AesCtrBufferedInputStream(
                decryptSimple,
                ACBISoffsetPosition,
                ACBISmediaStartOffset,
                ACBISmediaEndOffset,
                is,
                Files.size(filePath));

        long skipBytes = entry.getOffset()
                +ncaProvider.getTableEntry1().getMediaStartOffset()*0x200
                +ncaProvider.getNCAContentProvider(1).getRomfs().getHeader().getFileDataOffset()
                +ncaProvider.getSectionBlock1().getSuperBlockIVFC().getLvl6Offset();
        System.out.println("SUM: "+(entry.getOffset()
                + ncaProvider.getTableEntry1().getMediaStartOffset()*0x200
                +ncaProvider.getNCAContentProvider(1).getRomfs().getHeader().getFileDataOffset()
                +ncaProvider.getSectionBlock1().getSuperBlockIVFC().getLvl6Offset()));
        if (skipBytes != aesCtrBufferedInputStream.skip(skipBytes))
            throw new Exception("Can't skip");

        int blockSize = 0x200;
        if (entry.getSize() < 0x200)
            blockSize = (int) entry.getSize();

        long i = 0;
        byte[] block = new byte[blockSize];

        int actuallyRead;

        while (true) {
            if ((actuallyRead = aesCtrBufferedInputStream.read(block)) != blockSize)
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
        //---
        extractedFileBOS.close();
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
        PipedInputStream pis = ncaProvider.getNCAContentProvider(1).getRomfs().getContent(entry);

        byte[] readBuf = new byte[0x200]; // 8mb NOTE: consider switching to 1mb 1048576
        int readSize;

        while ((readSize = pis.read(readBuf)) > -1) {
            extractedFileBOS.write(readBuf, 0, readSize);
            readBuf = new byte[0x200];
        }

        extractedFileBOS.close();
    }*/
}
