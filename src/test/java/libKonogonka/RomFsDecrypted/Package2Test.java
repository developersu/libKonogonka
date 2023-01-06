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

import libKonogonka.Converter;
import libKonogonka.KeyChainHolder;
import libKonogonka.RainbowDump;
import libKonogonka.Tools.NCA.NCAContent;
import libKonogonka.Tools.NCA.NCAProvider;
import libKonogonka.Tools.RomFs.FileSystemEntry;
import libKonogonka.Tools.RomFs.RomFsProvider;
import libKonogonka.ctraes.InFileStreamProducer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Package2Test {

    private static final String keysFileLocation = "./FilesForTests/prod.keys";
    private static final String xci_header_keyFileLocation = "./FilesForTests/xci_header_key.txt";
    private static final String ncaFileLocation = "/home/loper/Загрузки/patchesPlayground/fw1100";
    private static KeyChainHolder keyChainHolder;
    private static NCAProvider ncaProvider;

    //@Disabled
    @Order(1)
    @DisplayName("Package2 Test")
    @Test
    void test() throws Exception{
        BufferedReader br = new BufferedReader(new FileReader(xci_header_keyFileLocation));
        String keyValue = br.readLine();
        br.close();

        if (keyValue == null)
            throw new Exception("Unable to retrieve xci_header_key");

        keyValue = keyValue.trim();
        keyChainHolder = new KeyChainHolder(keysFileLocation, keyValue);

        File parent = new File(ncaFileLocation);
        String[] dirWithFiles = parent.list((file, s) ->
            s.endsWith(".nca") && !s.endsWith(".cnmt.nca")); //String[] dirWithFiles = parent.list((file, s) -> s.endsWith(".cnmt.nca"));

        Assertions.assertNotNull(dirWithFiles);

        for (String fileName : dirWithFiles){
            read(new File(ncaFileLocation + File.separator + fileName));
        }
    }

    void read(File file) throws Exception{
        ncaProvider = new NCAProvider(file, keyChainHolder.getRawKeySet());

        String titleId = Converter.byteArrToHexStringAsLE(ncaProvider.getTitleId());
        if (titleId.equals("0100000000000819"))
            System.out.println(file.getName()+" "+titleId + "\tFAT");
        else if (titleId.equals("010000000000081b"))
            System.out.println(file.getName()+" "+titleId + "\tEXFAT");
        else
            return;

        if (ncaProvider.getSectionBlock0().getSuperBlockIVFC() == null)
            return;

        RomFsProvider romFsProvider = ncaProvider.getNCAContentProvider(0).getRomfs();

        FileSystemEntry package2Entry = romFsProvider.getRootEntry().getContent()
                .stream()
                .filter(fileSystemEntry -> fileSystemEntry.getName().equals("nx"))
                .collect(Collectors.toList())
                .get(0)
                .getContent()
                .stream()
                .filter(fileSystemEntry -> fileSystemEntry.getName().equals("package2"))
                .collect(Collectors.toList())
                .get(0);

        System.out.println("NAME   : "+package2Entry.getName());
        System.out.println("SIZE   : "+package2Entry.getSize());
        System.out.println("OFFSET : "+package2Entry.getOffset());

        //File tempDir = new File(System.getProperty("java.io.tmpdir"));
        //File tempFile = File.createTempFile("pFilename", ".tmp", tempDir);

        romFsProvider.exportContent(System.getProperty("java.io.tmpdir"), package2Entry);
        System.out.println(System.getProperty("java.io.tmpdir"));

        // . . .

        Files.delete(Paths.get(System.getProperty("java.io.tmpdir")+File.separator+"package2"));

        /*
        int contentSize = 0x200;
        InFileStreamProducer producer = romFsProvider.getStreamProducer(
                romFsProvider.getRootEntry()
                        .getContent().get(0)
                        .getContent().get(2)
        );

        try (BufferedInputStream stream = producer.produce()){
            byte[] everythingCNMT = new byte[contentSize];
            Assertions.assertEquals(contentSize, stream.read(everythingCNMT));

            RainbowDump.hexDumpUTF8(everythingCNMT);
        }
        */
    }
}
