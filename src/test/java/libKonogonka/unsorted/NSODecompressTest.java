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

import libKonogonka.Tools.NSO.NSO0Provider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

public class NSODecompressTest {
    private static final String nsoExtractedFileLocation = "./FilesForTests/NSO0/main";
    private static final String nsoExtractedFileLocationDec = "./FilesForTests/NSO0/main_d";

    @Disabled
    @DisplayName("NSO0 Decompression test")
    @Test
    void nso0DecompressionTest() throws Exception {
        NSO0Provider nso0Provider = new NSO0Provider(new File(nsoExtractedFileLocation));
        //nso0Provider.exportAsDecompressedNSO0("./FilesForTests/NSO0");
        nso0Provider.printDebug();

        NSO0Provider nso0Provider1 = new NSO0Provider(new File(nsoExtractedFileLocationDec));
        nso0Provider1.printDebug();
    }
}
