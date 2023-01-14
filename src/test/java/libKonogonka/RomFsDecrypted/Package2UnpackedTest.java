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
import libKonogonka.Tools.NCA.NCAProvider;
import libKonogonka.Tools.RomFs.FileSystemEntry;
import libKonogonka.Tools.RomFs.RomFsProvider;
import libKonogonka.Tools.other.System2.System2Provider;
import libKonogonka.Tools.other.System2.ini1.Ini1Provider;
import libKonogonka.Tools.other.System2.ini1.KIP1Provider;
import libKonogonka.ctraes.InFileStreamProducer;
import libKonogonka.ctraesclassic.AesCtrDecryptClassic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Package2UnpackedTest {

    private static final String keysFileLocation = "./FilesForTests/prod.keys";
    private static final String xci_header_keyFileLocation = "./FilesForTests/xci_header_key.txt";
    private static KeyChainHolder keyChainHolder;

    private static final String fileLocation = "/home/loper/Projects/libKonogonka/FilesForTests/6b7abe7efa17ad065b18e62d1c87a5cc.nca_extracted/ROOT/nx/package2";

    final String pathToFirmware = "/home/loper/Загрузки/patchesPlayground/nintendo-switch-global-firmwares/Firmware 14.1.0";

    @DisplayName("Package2 unpacked test")
    @Test
    void discover() throws Exception{
        BufferedReader br = new BufferedReader(new FileReader(xci_header_keyFileLocation));
        String keyValue = br.readLine();
        br.close();

        Assertions.assertNotNull(keyValue);

        keyValue = keyValue.trim();
        keyChainHolder = new KeyChainHolder(keysFileLocation, keyValue);

        HashMap<String, String> rawKeys = keyChainHolder.getRawKeySet();

        HashMap<String, String> package2_keys = new HashMap<>();

        for (String key: rawKeys.keySet()){
            if (key.matches("package2_key_[0-f][0-f]"))
                package2_keys.put(key, rawKeys.get(key));
        }

        Assertions.assertNotNull(package2_keys);

        Path package2Path = Paths.get(fileLocation);
        byte[] header;

        try (BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(package2Path))){
            Assertions.assertEquals(0x100, stream.skip(0x100));
            header = new byte[0x100];
            Assertions.assertEquals(0x100, stream.read(header));
        }

        Assertions.assertNotNull(header);

        byte[] headerCTR = Arrays.copyOfRange(header, 0, 0x10);

        for (Map.Entry<String, String> entry: package2_keys.entrySet()){
            AesCtrDecryptClassic aesCtrClassic = new AesCtrDecryptClassic(entry.getValue(), headerCTR);

            byte[] decrypted = aesCtrClassic.decryptNext(header);
            //RainbowDump.hexDumpUTF8(decrypted);
            byte[] magic = Arrays.copyOfRange(decrypted, 0x50, 0x54);
            String magicString = new String(magic, StandardCharsets.US_ASCII);
            if (magicString.equals("PK21"))
                System.out.println(entry.getKey()+" "+entry.getValue()+" "+magicString);
        }
    }
    @DisplayName("Package2 written test")
    @Test
    void implement() throws Exception{
        System.out.printf("SIZE: %d 0x%x\n", Files.size(Paths.get(fileLocation)), Files.size(Paths.get(fileLocation)));
        keyChainHolder = new KeyChainHolder(keysFileLocation, null);
        System2Provider provider = new System2Provider(fileLocation, keyChainHolder);
        provider.getHeader().printDebug();
        provider.getKernelMap().printDebug();
        Ini1Provider ini1Provider = provider.getIni1Provider();
        ini1Provider.getIni1Header().printDebug();
        for (KIP1Provider kip1Provider : ini1Provider.getKip1List())
            kip1Provider.printDebug();
        boolean exported = provider.exportKernel("/home/loper/Projects/libKonogonka/FilesForTests/own/");
        System.out.println("Exported = "+exported);

        exported = ini1Provider.export("/home/loper/Projects/libKonogonka/FilesForTests/own/");
        System.out.println("Exported INI1 = "+exported);

        for (KIP1Provider kip1Provider : ini1Provider.getKip1List()) {
            exported = kip1Provider.export("/home/loper/Projects/libKonogonka/FilesForTests/own/KIP1s");
            System.out.println("Exported KIP1s "+ kip1Provider.getHeader().getName() +" = " + exported +
                    String.format(" Size 0x%x", Files.size(Paths.get("/home/loper/Projects/libKonogonka/FilesForTests/own/KIP1s/"+ kip1Provider.getHeader().getName()+".kip1"))));
        }
    }

    @DisplayName("KIP1 unpack test")
    @Test
    void unpackKip1FromNca() throws Exception{
        keyChainHolder = new KeyChainHolder(keysFileLocation, null);
        // ------------------------------------------------------------------------------------------------------------
        File firmware = new File(pathToFirmware);

        if (! firmware.exists())
            throw new Exception("Firmware directory does not exist " + pathToFirmware);

        String[] fileNamesArray = firmware.list((File directory, String file) -> ( ! file.endsWith(".cnmt.nca") && file.endsWith(".nca")));
        List<String> ncaFilesList = Arrays.asList(Objects.requireNonNull(fileNamesArray));
        if (ncaFilesList.size() == 0)
            throw new Exception("No NCA files found in firmware folder");

        List<NCAProvider> ncaProviders = new ArrayList<>();

        for (String ncaFileName : fileNamesArray){
            File nca = new File(firmware.getAbsolutePath()+File.separator+ncaFileName);
            NCAProvider provider = new NCAProvider(nca, keyChainHolder.getRawKeySet());
            ncaProviders.add(provider);
        }
        // ------------------------------------------------------------------------------------------------------------

        NCAProvider system2FatNcaProvider = null;
        NCAProvider system2ExFatNcaProvider = null;
        for (NCAProvider ncaProvider : ncaProviders) {
            String titleId = Converter.byteArrToHexStringAsLE(ncaProvider.getTitleId());
            if (titleId.equals("0100000000000819"))
                system2FatNcaProvider = ncaProvider;
            if (titleId.equals("010000000000081b"))
                system2ExFatNcaProvider = ncaProvider;
        }
        System.out.println("FAT   " + (system2FatNcaProvider == null ? "NOT FOUND": "FOUND"));
        System.out.println("ExFAT " + (system2ExFatNcaProvider == null ? "NOT FOUND": "FOUND"));


        RomFsProvider romFsExFatProvider = null;
        FileSystemEntry exFatPackage2Content = null;
        InFileStreamProducer producerExFat = null;
        if (system2ExFatNcaProvider != null){
            romFsExFatProvider = system2ExFatNcaProvider.getNCAContentProvider(0).getRomfs();
            exFatPackage2Content = romFsExFatProvider.getRootEntry().getContent()
                    .stream()
                    .filter(e -> e.getName().equals("nx"))
                    .collect(Collectors.toList())
                    .get(0)
                    .getContent()
                    .stream()
                    .filter(e -> e.getName().equals("package2"))
                    .collect(Collectors.toList())
                    .get(0);
            producerExFat = romFsExFatProvider.getStreamProducer(exFatPackage2Content);

            system2ExFatNcaProvider.getNCAContentProvider(0).getRomfs().getRootEntry().printTreeForDebug();
            romFsExFatProvider.exportContent("/tmp/exported_ExFat", exFatPackage2Content);

            System2Provider provider = new System2Provider(producerExFat, keyChainHolder);
            provider.getKernelMap().printDebug();
            Ini1Provider ini1Provider = provider.getIni1Provider();
            KIP1Provider fsProvider = null;

            for (KIP1Provider kip1Provider : ini1Provider.getKip1List())
                if (kip1Provider.getHeader().getName().startsWith("FS"))
                    fsProvider = kip1Provider;

            if (fsProvider != null) {
                fsProvider.printDebug();
                fsProvider.exportAsDecompressed("/tmp/FAT_kip1");
            }
            else
                System.out.println("FS KIP1 NOT FOUND");
        }

        RomFsProvider romFsFatProvider = null;
        FileSystemEntry fatPackage2Content = null;
        InFileStreamProducer producerFat;
        if (system2FatNcaProvider != null){
            romFsFatProvider = system2FatNcaProvider.getNCAContentProvider(0).getRomfs();

            fatPackage2Content = romFsFatProvider.getRootEntry().getContent()
                    .stream()
                    .filter(e -> e.getName().equals("nx"))
                    .collect(Collectors.toList())
                    .get(0)
                    .getContent()
                    .stream()
                    .filter(e -> e.getName().equals("package2"))
                    .collect(Collectors.toList())
                    .get(0);
            producerFat = romFsFatProvider.getStreamProducer(fatPackage2Content);
            System2Provider provider = new System2Provider(producerFat, keyChainHolder);
            provider.getKernelMap().printDebug();
            Ini1Provider ini1Provider = provider.getIni1Provider();
            KIP1Provider fsProvider = null;

            for (KIP1Provider kip1Provider : ini1Provider.getKip1List())
                if (kip1Provider.getHeader().getName().startsWith("FS"))
                    fsProvider = kip1Provider;

            if (fsProvider != null) {
                fsProvider.printDebug();
                fsProvider.exportAsDecompressed("/tmp/FAT_kip1");
            }
            else
                System.out.println("FS KIP1 NOT FOUND");
        }
    }

    @DisplayName("KIP1 unpack test")
    @Test
    void unpackKip1() throws Exception{
        System2Provider provider = new System2Provider(fileLocation, keyChainHolder);
        provider.getKernelMap().printDebug();
        Ini1Provider ini1Provider = provider.getIni1Provider();
        KIP1Provider fsProvider = null;

        for (KIP1Provider kip1Provider : ini1Provider.getKip1List())
            if (kip1Provider.getHeader().getName().startsWith("FS"))
                fsProvider = kip1Provider;

        if (fsProvider != null) {
            fsProvider.printDebug();
            fsProvider.exportAsDecompressed("/tmp");
        }
        else
            System.out.println("FS KIP1 NOT FOUND");
    }
}
