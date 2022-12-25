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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;

public class KeyChainHolderTest {
    private static final String keysFileLocation = "./FilesForTests/prod.keys";
    private static final String xci_header_keyFileLocation = "./FilesForTests/xci_header_key.txt";
    private KeyChainHolder keyChainHolder;

    @Disabled
    @DisplayName("Key Chain Holder Test")
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

    void printXciHeaderKey(){
        System.out.println("-=== xci_header ===-");
        System.out.println(keyChainHolder.getXci_header_key());
    }

    void printKAKApplication(){
        System.out.println("-=== key_area_key_application test ===-");
        for (Map.Entry entry : keyChainHolder.getKey_area_key_application().entrySet()){
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }
    void printKAKOcean(){
        System.out.println("-=== key_area_key_ocean test ===-");
        for (Map.Entry entry : keyChainHolder.getKey_area_key_ocean().entrySet()){
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }
    void printKAKSystem(){
        System.out.println("-=== key_area_key_system test ===-");
        for (Map.Entry entry : keyChainHolder.getKey_area_key_system().entrySet()){
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }
    void printKAKTitleKek(){
        System.out.println("-=== titlekek test ===-");
        for (Map.Entry entry : keyChainHolder.getTitlekek().entrySet()){
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }
    void printRawKeySet(){
        System.out.println("-=== Raw Key Set (everything) test ===-");
        for (Map.Entry entry : keyChainHolder.getRawKeySet().entrySet()){
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }
}
