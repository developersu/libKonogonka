/*
    Copyright 2018-2022 Dmitry Isaenko
     
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
import libKonogonka.Tools.XCI.HFS0File;
import libKonogonka.Tools.XCI.HFS0Provider;
import libKonogonka.Tools.XCI.XCIProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

// log.fatal("Configuration File Defined To Be :: "+System.getProperty("log4j.configurationFile"));

public class XciTest {
    private static final String keysFileLocation = "./FilesForTests/prod.keys";

    private static final String xci_header_keyFileLocation = "./FilesForTests/xci_header_key.txt";
    private static final String decryptedFileAbsolutePath = "./FilesForTests/sample.xci";
    private File xciFile;
    XCIProvider provider;
    String xci_header_key;
    HFS0Provider hfs0Provider;

    private static KeyChainHolder keyChainHolder;

    @Disabled
    @DisplayName("XciTest")
    @Test
    void romFsValidation() throws Exception{
        makeFile();
        getXciHeaderKey();
        keyChainHolder = new KeyChainHolder(keysFileLocation, xci_header_key);
        makeProvider();

        getHfsSecure();
        getFirstNca3();
    }

    void getXciHeaderKey() throws Exception{
        BufferedReader br = new BufferedReader(new FileReader(xci_header_keyFileLocation));
        xci_header_key = br.readLine();
        br.close();

        if (xci_header_key == null)
            throw new Exception("Unable to retrieve xci_header_key");

        xci_header_key = xci_header_key.trim();
    }

    void makeFile(){
        xciFile = new File(decryptedFileAbsolutePath);
    }

    void makeProvider() throws Exception{
        provider = new XCIProvider(xciFile, xci_header_key);
    }

    void getHfsSecure(){
        hfs0Provider = provider.getHfs0ProviderSecure();
    }

    void getFirstNca3() throws Exception{
        HFS0File hfs0File = hfs0Provider.getHfs0Files()[0];

        System.out.println(hfs0File.getName() +" "+ hfs0Provider.getRawFileDataStart()+" "+hfs0File.getOffset()+ " "
                +(hfs0Provider.getRawFileDataStart() + hfs0File.getOffset()));

        NCAProvider ncaProvider = new NCAProvider(xciFile, keyChainHolder.getRawKeySet(),
                        hfs0Provider.getRawFileDataStart() +
                        hfs0File.getOffset());
        //ncaProvider.
    }
}