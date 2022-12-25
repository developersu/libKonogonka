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
import libKonogonka.Tools.NCA.NCAProvider;
import libKonogonka.Tools.NSO.NSO0Provider;
import libKonogonka.Tools.PFS0.PFS0Provider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ExportNso0FromNcaTest {
    private static final String keysFileLocation = "./FilesForTests/prod.keys";
    private static final String xci_header_keyFileLocation = "./FilesForTests/xci_header_key.txt";
    private static final String ncaFileLocation = "./FilesForTests/nso_container.nca";
    private static final String exportDecompressedNsoTo = "/tmp";

    @Disabled
    @DisplayName("Exports decompressed NSO0 example")
    @Test
    void nso0Test() throws Exception{
        BufferedReader br = new BufferedReader(new FileReader(xci_header_keyFileLocation));
        String keyValue = br.readLine();
        br.close();

        if (keyValue == null)
            throw new Exception("Unable to retrieve xci_header_key");

        keyValue = keyValue.trim();
        KeyChainHolder keyChainHolder = new KeyChainHolder(keysFileLocation, keyValue);

        NCAProvider ncaProvider = new NCAProvider(new File(ncaFileLocation), keyChainHolder.getRawKeySet());

        PFS0Provider pfs0Provider = ncaProvider.getNCAContentProvider(0).getPfs0();
        pfs0Provider.printDebug();

        NSO0Provider nso0Provider = new NSO0Provider(pfs0Provider.getStreamProducer(0));
        nso0Provider.printDebug();
        nso0Provider.exportAsDecompressedNSO0(exportDecompressedNsoTo);

        System.out.println("__--++ SDK VERSION ++--__\n"
                + ncaProvider.getSdkVersion()[3]
                +"."+ ncaProvider.getSdkVersion()[2]
                +"."+ ncaProvider.getSdkVersion()[1]
                +"."+ ncaProvider.getSdkVersion()[0]);

    }
}
