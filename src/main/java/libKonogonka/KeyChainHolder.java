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
package libKonogonka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

public class KeyChainHolder {

    private final File keysFile;
    private final String xci_header_key;
    private HashMap<String, String> rawKeySet;
    private HashMap<String, String> key_area_key_application,
            key_area_key_ocean,
            key_area_key_system,
            titlekek;

    public KeyChainHolder(String pathToKeysFile, String xci_header_key) throws Exception{
        this(new File(pathToKeysFile), xci_header_key);
    }

    public KeyChainHolder(File keysFile, String xci_header_key) throws Exception{
        this.keysFile = keysFile;
        this.xci_header_key = xci_header_key;
        collectEverything();
    }

    private void collectEverything() throws Exception{
        rawKeySet = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(keysFile));

        String fileLine;
        String[] keyValue;
        while ((fileLine = br.readLine()) != null){
            keyValue = fileLine.trim().split("\\s+?=\\s+?", 2);
            if (keyValue.length == 2)
                rawKeySet.put(keyValue[0], keyValue[1]);
        }

        key_area_key_application = collectKeysByType("key_area_key_application");
        key_area_key_ocean = collectKeysByType("key_area_key_ocean");
        key_area_key_system = collectKeysByType("key_area_key_system");
        titlekek = collectKeysByType("titlekek");
    }
    private HashMap<String, String> collectKeysByType(String keyName){
        HashMap<String, String> tempKeySet = new HashMap<>();
        String keyNamePattern = keyName+"_%02x";
        String keyParsed;
        int counter = 0;
        while ((keyParsed = rawKeySet.get(String.format(keyNamePattern, counter))) != null){
            tempKeySet.put(String.format(keyNamePattern, counter), keyParsed);
            counter++;
        }
        return tempKeySet;
    }

    public String getXci_header_key() {
        return xci_header_key;
    }

    public String getHeader_key() {
        return rawKeySet.get("header_key");
    }

    public HashMap<String, String> getRawKeySet() {
        return rawKeySet;
    }

    public HashMap<String, String> getKey_area_key_application() {
        return key_area_key_application;
    }

    public HashMap<String, String> getKey_area_key_ocean() {
        return key_area_key_ocean;
    }

    public HashMap<String, String> getKey_area_key_system() {
        return key_area_key_system;
    }

    public HashMap<String, String> getTitlekek() {
        return titlekek;
    }
}
