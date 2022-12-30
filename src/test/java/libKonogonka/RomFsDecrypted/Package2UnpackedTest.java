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
import libKonogonka.Tools.other.System2.System2Provider;
import libKonogonka.ctraes.AesCtrClassic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Package2UnpackedTest {

    private static final String keysFileLocation = "./FilesForTests/prod.keys";
    private static final String xci_header_keyFileLocation = "./FilesForTests/xci_header_key.txt";
    private static KeyChainHolder keyChainHolder;

    private static final String fileLocation = "/home/loper/Projects/libKonogonka/FilesForTests/6b7abe7efa17ad065b18e62d1c87a5cc.nca_extracted/ROOT/nx/package2";

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
            AesCtrClassic aesCtrClassic = new AesCtrClassic(entry.getValue(), headerCTR);

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

        boolean exported = provider.exportKernel("/home/loper/Projects/libKonogonka/FilesForTests/own/");
        System.out.println("Exported = "+exported);

        exported = provider.exportIni1("/home/loper/Projects/libKonogonka/FilesForTests/own/");
        System.out.println("Exported INI1 = "+exported);

    }
}
