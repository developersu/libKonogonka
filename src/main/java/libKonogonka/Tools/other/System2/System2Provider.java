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
package libKonogonka.Tools.other.System2;

import libKonogonka.KeyChainHolder;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class System2Provider {
    private byte[] Rsa2048signature;
    private System2Header header;
    // ...

    private final String pathToFile;
    private final KeyChainHolder keyChainHolder;

    public System2Provider(String pathToFile, KeyChainHolder keyChainHolder) throws Exception{
        this.pathToFile = pathToFile;
        this.keyChainHolder = keyChainHolder;

        readHeaderCtr();
    }

    private void readHeaderCtr() throws Exception{
        try (BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(Paths.get(pathToFile)))){
            if (0x100 != stream.skip(0x100))
                throw new Exception("Can't skip RSA-2048 signature offset (0x100)");
            byte[] headerBytes = new byte[0x100];
            if (0x100 != stream.read(headerBytes))
                throw new Exception("System2 header is too small");
            this.header = new System2Header(headerBytes, keyChainHolder.getRawKeySet());
        }
    }

    public System2Header getHeader() {
        return header;
    }
}
