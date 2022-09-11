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

public class TitleKeyChainHolder {
    private final File keysFile;
    private HashMap<String, String> rawKeySet;

    public TitleKeyChainHolder(String pathToKeysFile) throws Exception{
        this(new File(pathToKeysFile));
    }

    public TitleKeyChainHolder(File keysFile) throws Exception{
        this.keysFile = keysFile;
        collectEverything();
    }

    private void collectEverything() throws Exception{
        rawKeySet = new HashMap<>();

        BufferedReader br = new BufferedReader(new FileReader(keysFile));

        String fileLine;
        String[] keyValue;

        while ((fileLine = br.readLine()) != null){
            keyValue = fileLine.trim().split("\\s*=\\s*", 2);
            if (keyValue.length == 2 && keyValue[0].length() > 16 && ! (keyValue[0].length() > 32) && keyValue[1].length() == 32){
                rawKeySet.put(keyValue[0], keyValue[1]);
            }
        }
    }

    public HashMap<String, String> getKeySet() {
        return rawKeySet;
    }
}
