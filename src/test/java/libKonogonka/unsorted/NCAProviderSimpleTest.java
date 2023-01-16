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
import libKonogonka.Tools.NCA.NCAProvider;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NCAProviderSimpleTest {
        private static final String keysFileLocation = "./FilesForTests/prod.keys";
        private static final String xci_header_keyFileLocation = "./FilesForTests/xci_header_key.txt";
        private static final String ncaFileLocation = "./FilesForTests/simple.nca";
        //private static final String ncaFileLocation = "./FilesForTests/4pfs.nca";
        private static KeyChainHolder keyChainHolder;
        private static NCAProvider ncaProvider;

        //@Disabled
        @Order(1)
        @DisplayName("KeyChain loac test")
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

        //@Disabled
        @Order(2)
        @DisplayName("NCA provider test")
        @Test
        void ncaProvider() throws Exception{
            ncaProvider = new NCAProvider(new File(ncaFileLocation), keyChainHolder.getRawKeySet());
        }
}
