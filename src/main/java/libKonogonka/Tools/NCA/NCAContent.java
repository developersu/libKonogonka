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
package libKonogonka.Tools.NCA;

import libKonogonka.Tools.NCA.NCASectionTableBlock.NcaFsHeader;
import libKonogonka.Tools.PFS0.IPFS0Provider;
import libKonogonka.Tools.PFS0.PFS0Provider;
import libKonogonka.Tools.RomFs.IRomFsProvider;
import libKonogonka.Tools.RomFs.RomFsEncryptedProvider;
import libKonogonka.ctraes.AesCtrBufferedInputStream;
import libKonogonka.ctraes.AesCtrDecryptSimple;
import libKonogonka.exceptions.EmptySectionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;

public class NCAContent {
    private final static Logger log = LogManager.getLogger(NCAContent.class);
    
    private final File file;
    private final long ncaOffsetPosition;
    private final NcaFsHeader ncaFsHeader;
    private final NCAHeaderTableEntry ncaHeaderTableEntry;
    private final byte[] decryptedKey;

    private LinkedList<byte[]> Pfs0SHA256hashes;
    private IPFS0Provider pfs0;
    private IRomFsProvider romfs;

    // TODO: if decryptedKey is empty, throw exception?
    public NCAContent(File file,
                      long ncaOffsetPosition,
                      NcaFsHeader ncaFsHeader,
                      NCAHeaderTableEntry ncaHeaderTableEntry,
                      byte[] decryptedKey) throws Exception
    {
        this.file = file;
        this.ncaOffsetPosition = ncaOffsetPosition;
        this.ncaFsHeader = ncaFsHeader;
        this.ncaHeaderTableEntry = ncaHeaderTableEntry;
        this.decryptedKey = decryptedKey;
        System.out.println("NCAContent pfs0offsetPosition: "+ncaOffsetPosition);
        Pfs0SHA256hashes = new LinkedList<>();
        // If nothing to do
        if (ncaHeaderTableEntry.getMediaEndOffset() == 0)
            throw new EmptySectionException("Empty section");
        // If it's PFS0Provider
        if (ncaFsHeader.getSuperBlockPFS0() != null)
            this.proceedPFS0();
        else if (ncaFsHeader.getSuperBlockIVFC() != null)
            this.proceedRomFs();
        else
            throw new Exception("NCAContent(): Not supported. PFS0 or RomFS supported only.");
    }

    private void proceedPFS0() throws Exception {
        switch (ncaFsHeader.getCryptoType()){
            case 0x01:   // IF NO ENCRYPTION
                proceedPFS0NotEncrypted();
                break;
            case 0x03:
                proceedPFS0Encrypted();
                break;
            default:
                throw new Exception("'Crypto type' not supported: "+ncaFsHeader.getCryptoType());
        }
    }
    private void proceedPFS0NotEncrypted() throws Exception{
        pfs0 = new PFS0Provider(file,
                ncaOffsetPosition,
                ncaFsHeader.getSuperBlockPFS0(),
                ncaHeaderTableEntry.getMediaStartOffset(),
                ncaHeaderTableEntry.getMediaEndOffset());
        Pfs0SHA256hashes = pfs0.getPfs0SHA256hashes();
    }

    private void proceedPFS0Encrypted() throws Exception{
        AesCtrDecryptSimple decryptor = new AesCtrDecryptSimple(decryptedKey, ncaFsHeader.getSectionCTR(),
                ncaHeaderTableEntry.getMediaStartOffset() * 0x200);
        pfs0 = new PFS0Provider(file,
                ncaOffsetPosition,
                ncaFsHeader.getSuperBlockPFS0(),
                decryptor,
                ncaHeaderTableEntry.getMediaStartOffset(),
                ncaHeaderTableEntry.getMediaEndOffset());
        Pfs0SHA256hashes = pfs0.getPfs0SHA256hashes();
    }

    private void proceedRomFs() throws Exception{
        switch (ncaFsHeader.getCryptoType()){
            case 0x01:
                proceedRomFsNotEncrypted(); // IF NO ENCRYPTION
                break;
            case 0x03:
                proceedRomFsEncrypted(); // If encrypted regular [ 0x03 ]
                break;
            default:
                throw new Exception("Non-supported 'Crypto type'");
        }
    }
    private void proceedRomFsNotEncrypted(){   // TODO: Clarify, implement if needed
        log.error("proceedRomFs() -> proceedRomFsNotEncrypted() is not implemented :(");
    }
    private void proceedRomFsEncrypted() throws Exception{
        if (decryptedKey == null)
            throw new Exception("CryptoSection03: unable to proceed. No decrypted key provided.");

        this.romfs = new RomFsEncryptedProvider(
                ncaFsHeader.getSuperBlockIVFC().getLvl6Offset(),
                file,
                ncaOffsetPosition,
                decryptedKey,
                ncaFsHeader.getSectionCTR(),
                ncaHeaderTableEntry.getMediaStartOffset(),
                ncaHeaderTableEntry.getMediaEndOffset());
    }
    public IPFS0Provider getPfs0() { return pfs0; }
    public IRomFsProvider getRomfs() { return romfs; }

    /**
     * Export NCA content AS IS.
     * Not so good for PFS0 since there are SHAs list that discourages but good for 'romfs' and things like that
     * */
    public boolean exportMediaBlock(String saveToLocation){
        File location = new File(saveToLocation);
        location.mkdirs();
        BufferedInputStream stream;

        long mediaStartOffset = ncaHeaderTableEntry.getMediaStartOffset();
        long mediaEndOffset = ncaHeaderTableEntry.getMediaEndOffset();
        long mediaBlocksCount = mediaEndOffset - mediaStartOffset;

        try (BufferedOutputStream extractedFileBOS = new BufferedOutputStream(
                Files.newOutputStream(Paths.get(saveToLocation+File.separator+file.getName()+"_MediaBlock.bin")))){
            if(ncaFsHeader.getCryptoType()==0x01){
                stream = new BufferedInputStream(Files.newInputStream(file.toPath()));
            }
            else if(ncaFsHeader.getCryptoType()==0x03) {
                AesCtrDecryptSimple decryptor = new AesCtrDecryptSimple(decryptedKey,
                        ncaFsHeader.getSectionCTR(),
                        mediaStartOffset * 0x200);

                stream = new AesCtrBufferedInputStream(decryptor,
                        ncaOffsetPosition,
                        mediaStartOffset,
                        mediaEndOffset,
                        Files.newInputStream(file.toPath()));
            }
            else
                throw new Exception("Crypto type not supported");

            for (int i = 0; i < mediaBlocksCount; i++){
                byte[] block = new byte[0x200];
                if (0x200 != stream.read(block))
                    throw new Exception("Read failure");
                extractedFileBOS.write(block);
            }
            stream.close();
        } catch (Exception e) {
            log.error("Failed to export MediaBlock", e);
            return false;
        }
        return true;
    }

    public long getRawDataContentSize(){
        return (ncaHeaderTableEntry.getMediaEndOffset() - ncaHeaderTableEntry.getMediaStartOffset()) * 0x200;
    }
    public String getFileName(){
        return file.getName();
    }
}