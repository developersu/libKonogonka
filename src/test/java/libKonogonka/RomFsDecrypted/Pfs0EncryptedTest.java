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

import libKonogonka.KeyChainHolder;
import libKonogonka.RainbowDump;
import libKonogonka.Tools.NCA.NCAProvider;
import libKonogonka.Tools.PFS0.PFS0Provider;
import libKonogonka.Tools.PFS0.PFS0subFile;
import libKonogonka.ctraes.AesCtrBufferedInputStream;
import libKonogonka.ctraes.AesCtrDecryptSimple;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Pfs0EncryptedTest {
    private static final String keysFileLocation = "./FilesForTests/prod.keys";
    private static final String xci_header_keyFileLocation = "./FilesForTests/xci_header_key.txt";
    private static final String ncaFileLocation = "./FilesForTests/PFS_RomFS.nca";
    private static KeyChainHolder keyChainHolder;
    private static NCAProvider ncaProvider;

    @Disabled
    @DisplayName("PFS0 Encrypted test")
    @Test
    void pfs0test() throws Exception{
        BufferedReader br = new BufferedReader(new FileReader(xci_header_keyFileLocation));
        String keyValue = br.readLine();
        br.close();

        if (keyValue == null)
            throw new Exception("Unable to retrieve xci_header_key");

        keyValue = keyValue.trim();
        keyChainHolder = new KeyChainHolder(keysFileLocation, keyValue);

        ncaProvider = new NCAProvider(new File(ncaFileLocation), keyChainHolder.getRawKeySet());

        pfs0Validation();

        AesCtrBufferedInputStreamTest();
    }

    void pfs0Validation() throws Exception{
        for (byte i = 0; i < 4; i++){
            System.out.println("..:: TEST SECTION #"+i+" ::..");
            if (ncaProvider.getSectionBlock(i).getFsType() == 1 &&
                    ncaProvider.getSectionBlock(i).getHashType() == 2 &&
                    ncaProvider.getSectionBlock(i).getCryptoType() == 3){
                ncaProvider.getNCAContentProvider(i).getPfs0().printDebug();
                ncaProvider.getSectionBlock(i).printDebug();
                return;
            }
        }
    }

    private AesCtrDecryptSimple decryptSimple;
    long ACBISoffsetPosition;
    long ACBISmediaStartOffset;
    long ACBISmediaEndOffset;

    long offsetPosition;

    void AesCtrBufferedInputStreamTest() throws Exception {
        File nca = new File(ncaFileLocation);
        PFS0subFile[] subfiles = ncaProvider.getNCAContentProvider(0).getPfs0().getHeader().getPfs0subFiles();

        offsetPosition = ncaProvider.getTableEntry0().getMediaStartOffset()*0x200 +
                        ncaProvider.getNCAContentProvider(0).getPfs0().getRawFileDataStart();
        System.out.println("\t=============================================================");
        System.out.println("\tNCA SIZE:                         "+ RainbowDump.formatDecHexString(nca.length()));
        System.out.println("\tPFS0 Offset(get)                  "+RainbowDump.formatDecHexString(ncaProvider.getSectionBlock0().getSuperBlockPFS0().getPfs0offset()));
        System.out.println("\tPFS0 MediaStart (* 0x200)         "+RainbowDump.formatDecHexString(ncaProvider.getTableEntry0().getMediaStartOffset()*0x200));
        System.out.println("\tPFS0 MediaEnd (* 0x200)           "+RainbowDump.formatDecHexString(ncaProvider.getTableEntry0().getMediaEndOffset()*0x200));
        System.out.println("\tPFS0 Offset+MediaBlockStart:      "+RainbowDump.formatDecHexString(offsetPosition));
        System.out.println("\tRAW  Offset:                      "+RainbowDump.formatDecHexString(ncaProvider.getNCAContentProvider(0).getPfs0().getRawFileDataStart()));
        System.out.println("\tHashTableSize:                    "+RainbowDump.formatDecHexString(ncaProvider.getSectionBlock0().getSuperBlockPFS0().getHashTableSize()));
        for (PFS0subFile subFile : subfiles){
            System.out.println("\n\tEntry Name:                       "+subFile.getName());
            System.out.println("\tEntry Offset:                     "+RainbowDump.formatDecHexString(subFile.getOffset()));
            System.out.println("\tEntry Size:                       "+RainbowDump.formatDecHexString(subFile.getSize()));
        }
        System.out.println("\t=============================================================");

        ACBISoffsetPosition = 0;
        ACBISmediaStartOffset = ncaProvider.getTableEntry0().getMediaStartOffset();
        ACBISmediaEndOffset = ncaProvider.getTableEntry0().getMediaEndOffset();

        decryptSimple = new AesCtrDecryptSimple(
                ncaProvider.getDecryptedKey2(),
                ncaProvider.getSectionBlock0().getSectionCTR(),
                ncaProvider.getTableEntry0().getMediaStartOffset()*0x200);
/*
        for (PFS0subFile subFile : subfiles){
            exportContentLegacy(subFile, "/tmp/legacy_PFS0");
        }

 */
        PFS0Provider pfs0Provider = ncaProvider.getNCAContentProvider(0).getPfs0();
        //----------------------------------------------------------------------
        for (PFS0subFile subFile : subfiles) {
            System.out.println("Exporting "+subFile.getName());
            System.out.println("Result: "+pfs0Provider.exportContent("/tmp/1_brandnew_PFS0", subFile.getName()));
        }

    }

    private void exportContent(PFS0subFile entry, String saveToLocation) throws Exception{
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

        //long offsetToSkip = entry.getOffset() + ncaProvider.getNCAContentProvider(0).getPfs0().getRawFileDataStart();
        long offsetToSkip = offsetPosition+entry.getOffset();
        System.out.println("\nOffsets"+
                "\nRAW:                 "+ncaProvider.getNCAContentProvider(0).getPfs0().getRawFileDataStart()+
                "\nPfs0 offset:         "+offsetPosition+
                "\nentry.getOffset():   "+entry.getOffset()+
                "\n");

        if (offsetToSkip != aesCtrBufferedInputStream.skip(offsetToSkip))
            throw new Exception("Can't skip "+
                    ncaProvider.getSectionBlock0().getSuperBlockPFS0().getPfs0offset()+
                    "("+entry.getOffset()+
                    " + "+
                    ncaProvider.getNCAContentProvider(0).getPfs0().getRawFileDataStart()+")");


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
            if ((i + blockSize) > entry.getSize()) {
                blockSize = (int) (entry.getSize() - i);
                if (blockSize == 0)
                    break;
                block = new byte[blockSize];
            }
        }
        //---
        extractedFileBOS.close();
    }

    private void exportContentLegacy(PFS0subFile entry, String saveToLocation) throws Exception {
        File contentFile = new File(saveToLocation + entry.getName());

        BufferedOutputStream extractedFileBOS = new BufferedOutputStream(new FileOutputStream(contentFile));
        BufferedInputStream pis = ncaProvider.getNCAContentProvider(0)
                .getPfs0()
                .getStreamProducer(entry.getName())
                .produce();

        byte[] readBuf = new byte[0x200]; // 8mb NOTE: consider switching to 1mb 1048576
        int readSize;

        while ((readSize = pis.read(readBuf)) > -1) {
            extractedFileBOS.write(readBuf, 0, readSize);
            readBuf = new byte[0x200];
        }

        extractedFileBOS.close();
    }
}
