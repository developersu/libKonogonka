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
import libKonogonka.TitleKeyChainHolder;
import libKonogonka.fs.PFS0.PFS0Provider;
import libKonogonka.fs.PFS0.PFS0subFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.HashMap;

public class PFS0Test {
    private static final String keysFileLocation = "./FilesForTests/prod.keys";
    private static final String titleFileLocation = "./FilesForTests/simple_nsp.title_key";
    private static final String xci_header_keyFileLocation = "./FilesForTests/xci_header_key.txt";
    private static final String nspFileLocation = "./FilesForTests/sample.nsp";
    private static KeyChainHolder keyChainHolder;
    private static PFS0Provider pfs0Provider;

    @Disabled
    @DisplayName("NSP validation")
    @Test
    void pfs0test() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(xci_header_keyFileLocation));
        String keyValue = br.readLine();
        br.close();

        if (keyValue == null)
            throw new Exception("Unable to retrieve xci_header_key");

        keyValue = keyValue.trim();
        keyChainHolder = new KeyChainHolder(keysFileLocation, keyValue);

        TitleKeyChainHolder titleKeyChainHolder = new TitleKeyChainHolder(titleFileLocation);

        HashMap<String, String> finalKeysSet = keyChainHolder.getRawKeySet();
        finalKeysSet.putAll(titleKeyChainHolder.getKeySet());

        File nspFile = new File(nspFileLocation);

        pfs0Provider = new PFS0Provider(nspFile);
        pfs0Provider.printDebug();
        for (PFS0subFile subFile : pfs0Provider.getHeader().getPfs0subFiles()) {
            pfs0Provider.exportContent("/tmp/NSP_PFS0_NON-ENCRYPTED_TEST", subFile.getName());
        }
    }
}