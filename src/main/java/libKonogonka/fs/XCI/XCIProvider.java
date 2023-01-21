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
package libKonogonka.fs.XCI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class XCIProvider{
    // TODO: Since LOGO partition added, we have to handle it properly. Is it works??

    //private BufferedInputStream xciBIS;
    private final XCIGamecardHeader xciGamecardHeader;
    private final XCIGamecardInfo xciGamecardInfo;
    private final XCIGamecardCert xciGamecardCert;
    private final HFS0Provider hfs0ProviderMain;
    private HFS0Provider hfs0ProviderUpdate;
    private HFS0Provider hfs0ProviderNormal;
    private HFS0Provider hfs0ProviderSecure;
    private HFS0Provider hfs0ProviderLogo;

    public XCIProvider(File file, String XCI_HEADER_KEY) throws Exception{                                                   // TODO: ADD FILE SIZE CHECK !!! Check xciHdrKey
        RandomAccessFile raf;

        try {
            //xciBIS = new BufferedInputStream(new FileInputStream(file));
            raf = new RandomAccessFile(file, "r");
        }
        catch (FileNotFoundException fnfe){
            throw new Exception("XCI File not found: \n  "+fnfe.getMessage());
        }

        if (file.length() < 0xf010)
            throw new Exception("XCI File is too small.");

        try{
            byte[] gamecardHeaderBytes = new byte[400];
            byte[] gamecardInfoBytes = new byte[112];
            byte[] gamecardCertBytes = new byte[512];

            // Creating GC Header class
            if (raf.read(gamecardHeaderBytes) != 400) {
                raf.close();
                throw new Exception("XCI Can't read Gamecard Header bytes.");
            }
            xciGamecardHeader = new XCIGamecardHeader(gamecardHeaderBytes);     // throws exception
            // Creating GC Info class
            if (raf.read(gamecardInfoBytes) != 112) {
                raf.close();
                throw new Exception("XCI Can't read Gamecard Header bytes.");
            }
            xciGamecardInfo = new XCIGamecardInfo(gamecardInfoBytes, xciGamecardHeader.getGcInfoIV(), XCI_HEADER_KEY);
            // Creating GC Cerfificate class
            raf.seek(0x7000);
            if (raf.read(gamecardCertBytes) != 512) {
                raf.close();
                throw new Exception("XCI Can't read Gamecard certificate bytes.");
            }
            xciGamecardCert = new XCIGamecardCert(gamecardCertBytes);

            hfs0ProviderMain = new HFS0Provider(0xf000, raf, file);
            if (hfs0ProviderMain.getFilesCount() < 3){
                raf.close();
                throw new Exception("XCI Can't read Gamecard certificate bytes.");
            }
            // Get all partitions from the main HFS0 file
            String partition;
            for (HFS0File hfs0File: hfs0ProviderMain.getHfs0Files()){
                partition = hfs0File.getName();
                if (partition.equals("update")) {
                    hfs0ProviderUpdate = new HFS0Provider(hfs0ProviderMain.getRawFileDataStart() + hfs0File.getOffset(), raf, file);
                    continue;
                }
                if (partition.equals("normal")) {
                    hfs0ProviderNormal = new HFS0Provider(hfs0ProviderMain.getRawFileDataStart() + hfs0File.getOffset(), raf, file);
                    continue;
                }
                if (partition.equals("secure")) {
                    hfs0ProviderSecure = new HFS0Provider(hfs0ProviderMain.getRawFileDataStart() + hfs0File.getOffset(), raf, file);
                    continue;
                }
                if (partition.equals("logo")) {
                    hfs0ProviderLogo = new HFS0Provider(hfs0ProviderMain.getRawFileDataStart() + hfs0File.getOffset(), raf, file);
                }
            }
            raf.close();
        }
        catch (IOException ioe){
            throw new Exception("XCI Failed file analyze for ["+file.getName()+"]\n  "+ioe.getMessage());
        }
    }
    /* API */
    public XCIGamecardHeader getGCHeader(){ return this.xciGamecardHeader; }
    public XCIGamecardInfo getGCInfo(){ return this.xciGamecardInfo; }
    public XCIGamecardCert getGCCert(){ return this.xciGamecardCert; }
    public HFS0Provider getHfs0ProviderMain() { return this.hfs0ProviderMain; }
    public HFS0Provider getHfs0ProviderUpdate() { return this.hfs0ProviderUpdate; }
    public HFS0Provider getHfs0ProviderNormal() { return this.hfs0ProviderNormal; }
    public HFS0Provider getHfs0ProviderSecure() { return this.hfs0ProviderSecure; }
    public HFS0Provider getHfs0ProviderLogo() { return this.hfs0ProviderLogo; }
}