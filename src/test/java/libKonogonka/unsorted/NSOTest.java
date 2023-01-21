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
package libKonogonka.unsorted;

import libKonogonka.KeyChainHolder;
import libKonogonka.RainbowDump;
import libKonogonka.fs.NCA.NCAProvider;
import libKonogonka.fs.NSO.NSO0Provider;
import libKonogonka.fs.PFS0.PFS0Provider;
import libKonogonka.fs.PFS0.PFS0subFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;

public class NSOTest {
    private static final String keysFileLocation = "./FilesForTests/prod.keys";
    private static final String xci_header_keyFileLocation = "./FilesForTests/xci_header_key.txt";
    private static final String ncaFileLocation = "./FilesForTests/nso_container.nca";
    private static KeyChainHolder keyChainHolder;
    private static NCAProvider ncaProvider;


    @Disabled
    @DisplayName("NSO0 test")
    @Test
    void nso0Test() throws Exception{
        BufferedReader br = new BufferedReader(new FileReader(xci_header_keyFileLocation));
        String keyValue = br.readLine();
        br.close();

        if (keyValue == null)
            throw new Exception("Unable to retrieve xci_header_key");

        keyValue = keyValue.trim();
        keyChainHolder = new KeyChainHolder(keysFileLocation, keyValue);

        ncaProvider = new NCAProvider(new File(ncaFileLocation), keyChainHolder.getRawKeySet());

        pfs0Validation();

        nso0Validation();
        // AesCtrBufferedInputStreamTest();
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

    void nso0Validation() throws Exception{
        File nca = new File(ncaFileLocation);
        PFS0subFile[] subfiles = ncaProvider.getNCAContentProvider(0).getPfs0().getHeader().getPfs0subFiles();

        long offsetPosition = ncaProvider.getTableEntry0().getMediaStartOffset()*0x200 +
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

        PFS0Provider pfs0Provider = ncaProvider.getNCAContentProvider(0).getPfs0();
        pfs0Provider.printDebug();

        NSO0Provider nso0Provider = new NSO0Provider(pfs0Provider.getStreamProducer(0));
        nso0Provider.printDebug();
        nso0Provider.exportAsDecompressedNSO0("./tmp");

        //NPDMProvider npdmProvider = new NPDMProvider(pfs0Provider.getStreamProducer(1));

        System.out.println("__--++ SDK VERSION ++--__\n"
                +ncaProvider.getSdkVersion()[3]
                +"."+ncaProvider.getSdkVersion()[2]
                +"."+ncaProvider.getSdkVersion()[1]
                +"."+ncaProvider.getSdkVersion()[0]);

    }
}
