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
package libKonogonka.fs.NCA;

import libKonogonka.Converter;
import libKonogonka.fs.NCA.NCASectionTableBlock.NcaFsHeader;
import libKonogonka.exceptions.EmptySectionException;
import libKonogonka.xtsaes.XTSAESCipher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

import static libKonogonka.Converter.byteArrToHexStringAsLE;
import static libKonogonka.Converter.getLElong;

// TODO: check file size
public class NCAProvider {
    private final static Logger log = LogManager.getLogger(NCAProvider.class);

    private final File file;                          // File that contains NCA
    private final long offset;                        // Offset where NCA actually located
    private final HashMap<String, String> keys;       // hashmap with keys using _0x naming (where x number 0-N)
    // Header
    private byte[] rsa2048one;
    private byte[] rsa2048two;
    private String magicNumber;
    private byte systemOrGcIndicator;
    private byte contentType;
    private byte cryptoType1;                   // keyblob index. Considering as number within application/ocean/system
    private byte keyIndex;                      // application/ocean/system (kaek index?)
    private long ncaSize;                       // Size of this NCA (bytes)
    private byte[] titleId;
    private byte[] contentIndx;
    private byte[] sdkVersion;                  // version ver_revision.ver_micro.vev_minor.ver_major
    private byte cryptoType2;                   // keyblob index. Considering as number within application/ocean/system | AKA KeyGeneration
    private byte Header1SignatureKeyGeneration;
    private byte[] keyGenerationReserved;
    private byte[] rightsId;

    private byte cryptoTypeReal;

    private byte[]  sha256hash0, sha256hash1, sha256hash2, sha256hash3,
                    encryptedKey0, encryptedKey1, encryptedKey2, encryptedKey3,
                    decryptedKey0, decryptedKey1, decryptedKey2, decryptedKey3;
    private NCAHeaderTableEntry tableEntry0, tableEntry1, tableEntry2, tableEntry3;
    private NcaFsHeader sectionBlock0, sectionBlock1, sectionBlock2, sectionBlock3;
    private NCAContent ncaContent0, ncaContent1, ncaContent2, ncaContent3;

    public NCAProvider(File file, HashMap<String, String> keys) throws Exception{
        this(file, keys, 0);
    }

    public NCAProvider(File file, HashMap<String, String> keys, long offsetPosition) throws Exception{
        this.file = file;
        this.keys = keys;
        String header_key = keys.get("header_key");
        if (header_key == null )
            throw new Exception("header_key is not found within key set provided.");
        if (header_key.length() != 64)
            throw new Exception("header_key is too small or too big. Must be 64 symbols.");

        this.offset = offsetPosition;

        KeyParameter key1 = new KeyParameter(hexStrToByteArray(header_key.substring(0, 32)));
        KeyParameter key2 = new KeyParameter(hexStrToByteArray(header_key.substring(32, 64)));

        XTSAESCipher xtsaesCipher = new XTSAESCipher(false);
        xtsaesCipher.init(false, key1, key2);
        //-------------------------------------------------------------------------------------------------------------------------
        byte[] decryptedHeader = new byte[0xC00];

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] encryptedSequence = new byte[0x200];
            byte[] decryptedSequence;

            raf.seek(offsetPosition);

            for (int i = 0; i < 6; i++){
                if (raf.read(encryptedSequence) != 0x200)
                    throw new Exception("Read error "+i);
                decryptedSequence = new byte[0x200];
                xtsaesCipher.processDataUnit(encryptedSequence, 0, 0x200, decryptedSequence, 0, i);
                System.arraycopy(decryptedSequence, 0, decryptedHeader, i * 0x200, 0x200);
            }

            setupHeader(decryptedHeader);
        }

        setupNCAContent();
        /*//---------------------------------------------------------------------
        FileInputStream fis = new FileInputStream(file);
        try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(Paths.get("/tmp/decrypted.nca")))){
            int i = 0;
            byte[] block = new byte[0x200];
            while (fis.read(block) != -1){
                decryptedSequence = new byte[0x200];
                xtsaesCipher.processDataUnit(block, 0, 0x200, decryptedSequence, 0, i++);
                bos.write(decryptedSequence);
            }
        }
        catch (Exception e){
            throw new Exception("Failed to export decrypted AES-XTS", e);
        }
        //---------------------------------------------------------------------*/
    }

    private byte[] hexStrToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    private void setupHeader(byte[] decryptedData) throws Exception{
        rsa2048one = Arrays.copyOfRange(decryptedData, 0, 0x100);
        rsa2048two = Arrays.copyOfRange(decryptedData, 0x100, 0x200);
        magicNumber = new String(decryptedData, 0x200, 0x4, StandardCharsets.US_ASCII);
        systemOrGcIndicator = decryptedData[0x204];
        contentType = decryptedData[0x205];
        cryptoType1 = decryptedData[0x206];
        keyIndex = decryptedData[0x207];
        ncaSize = getLElong(decryptedData, 0x208);
        titleId = Converter.flip(Arrays.copyOfRange(decryptedData, 0x210, 0x218));
        contentIndx = Arrays.copyOfRange(decryptedData, 0x218, 0x21C);
        sdkVersion = Arrays.copyOfRange(decryptedData, 0x21c, 0x220);
        cryptoType2 = decryptedData[0x220];
        Header1SignatureKeyGeneration = decryptedData[0x221];
        keyGenerationReserved = Arrays.copyOfRange(decryptedData, 0x222, 0x230);
        rightsId = Arrays.copyOfRange(decryptedData, 0x230, 0x240);
        byte[] tableBytes = Arrays.copyOfRange(decryptedData, 0x240, 0x280);
        byte[] sha256tableBytes = Arrays.copyOfRange(decryptedData, 0x280, 0x300);
        sha256hash0 = Arrays.copyOfRange(sha256tableBytes, 0, 0x20);
        sha256hash1 = Arrays.copyOfRange(sha256tableBytes, 0x20, 0x40);
        sha256hash2 = Arrays.copyOfRange(sha256tableBytes, 0x40, 0x60);
        sha256hash3 = Arrays.copyOfRange(sha256tableBytes, 0x60, 0x80);
        byte [] encryptedKeysArea = Arrays.copyOfRange(decryptedData, 0x300, 0x340);

        encryptedKey0 = Arrays.copyOfRange(encryptedKeysArea, 0, 0x10);
        encryptedKey1 = Arrays.copyOfRange(encryptedKeysArea, 0x10, 0x20);
        encryptedKey2 = Arrays.copyOfRange(encryptedKeysArea, 0x20, 0x30);
        encryptedKey3 = Arrays.copyOfRange(encryptedKeysArea, 0x30, 0x40);

        // Calculate real Crypto Type
        if (cryptoType1 < cryptoType2)
            cryptoTypeReal = cryptoType2;
        else
            cryptoTypeReal = cryptoType1;

        if (cryptoTypeReal > 0)             // TODO: CLARIFY WHY THE FUCK IS IT FAIR????
            cryptoTypeReal -= 1;

        //If nca3 proceed
        if (! magicNumber.equalsIgnoreCase("NCA3"))
            throw new Exception("Not supported data type: "+ magicNumber +". Only NCA3 supported");
        // Decrypt keys if encrypted
        if (Arrays.equals(rightsId, new byte[0x10])) {
            String keyAreaKey;
            switch (keyIndex){
                case 0:
                    keyAreaKey = keys.get(String.format("key_area_key_application_%02x", cryptoTypeReal));
                    break;
                case 1:
                    keyAreaKey = keys.get(String.format("key_area_key_ocean_%02x", cryptoTypeReal));
                    break;
                case 2:
                    keyAreaKey = keys.get(String.format("key_area_key_system_%02x", cryptoTypeReal));
                    break;
                default:
                    keyAreaKey = null;
            }

            if (keyAreaKey != null){
                SecretKeySpec skSpec = new SecretKeySpec(hexStrToByteArray(keyAreaKey), "AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, skSpec);
                decryptedKey0 = cipher.doFinal(encryptedKey0);
                decryptedKey1 = cipher.doFinal(encryptedKey1);
                decryptedKey2 = cipher.doFinal(encryptedKey2);
                decryptedKey3 = cipher.doFinal(encryptedKey3);
            }
            else
                keyAreaKeyNotSupportedOrFound();
        }

        tableEntry0 = new NCAHeaderTableEntry(tableBytes);
        tableEntry1 = new NCAHeaderTableEntry(Arrays.copyOfRange(tableBytes, 0x10, 0x20));
        tableEntry2 = new NCAHeaderTableEntry(Arrays.copyOfRange(tableBytes, 0x20, 0x30));
        tableEntry3 = new NCAHeaderTableEntry(Arrays.copyOfRange(tableBytes, 0x30, 0x40));

        sectionBlock0 = new NcaFsHeader(Arrays.copyOfRange(decryptedData, 0x400, 0x600));
        sectionBlock1 = new NcaFsHeader(Arrays.copyOfRange(decryptedData, 0x600, 0x800));
        sectionBlock2 = new NcaFsHeader(Arrays.copyOfRange(decryptedData, 0x800, 0xa00));
        sectionBlock3 = new NcaFsHeader(Arrays.copyOfRange(decryptedData, 0xa00, 0xc00));
    }

    private void keyAreaKeyNotSupportedOrFound() throws Exception{
        StringBuilder exceptionStringBuilder = new StringBuilder("key_area_key_");
        switch (keyIndex){
            case 0:
                exceptionStringBuilder.append("application_");
                break;
            case 1:
                exceptionStringBuilder.append("ocean_");
                break;
            case 2:
                exceptionStringBuilder.append("system_");
                break;
            default:
                exceptionStringBuilder.append(keyIndex);
                exceptionStringBuilder.append("[UNKNOWN]_");
        }
        exceptionStringBuilder.append(String.format("%02x", cryptoTypeReal));
        exceptionStringBuilder.append(" requested. Not supported or not found.");
        throw new Exception(exceptionStringBuilder.toString());
    }

    private void setupNCAContent() throws Exception{
        byte[] key = calculateKey();
        
        setupNcaContentByNumber(0, key);
        setupNcaContentByNumber(1, key);
        setupNcaContentByNumber(2, key);
        setupNcaContentByNumber(3, key);
    }
    private byte[] calculateKey() throws Exception{
        try {
            if (Arrays.equals(rightsId, new byte[0x10]))      // If empty Rights ID
                return decryptedKey2;                         // NOTE: Just remember this dumb hack
            
            byte[] rightsIdKey = hexStrToByteArray(keys.get(byteArrToHexStringAsLE(rightsId))); // throws NullPointerException

            SecretKeySpec skSpec = new SecretKeySpec(
                    hexStrToByteArray(keys.get(String.format("titlekek_%02x", cryptoTypeReal))), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skSpec);
            return cipher.doFinal(rightsIdKey);
        }
        catch (Exception e){
            throw new Exception(String.format("No title.keys loaded for 'titlekek_%02x' or '%s' (%s)",
                    cryptoTypeReal, byteArrToHexStringAsLE(rightsId), e), e);
        }
    }
    private void setupNcaContentByNumber(int number, byte[] key){
        try {
            switch (number) {
                case 0:
                    this.ncaContent0 = new NCAContent(file, offset, sectionBlock0, tableEntry0, key);
                    break;
                case 1:
                    this.ncaContent1 = new NCAContent(file, offset, sectionBlock1, tableEntry1, key);
                    break;
                case 2:
                    this.ncaContent2 = new NCAContent(file, offset, sectionBlock2, tableEntry2, key);
                    break;
                case 3:
                    this.ncaContent3 = new NCAContent(file, offset, sectionBlock3, tableEntry3, key);
                    break;
            }
        }
        catch (EmptySectionException ignored){}
        catch (Exception e){
            log.debug("Unable to get NCA Content "+number+" ("+file.getParentFile().getName()+"/"+file.getName()+")", e);
        }
    }

    // -=======================     API     =======================-

    public byte[] getRsa2048one() { return rsa2048one; }
    public byte[] getRsa2048two() { return rsa2048two; }
    public String getMagicnum() { return magicNumber; }
    public byte getSystemOrGcIndicator() { return systemOrGcIndicator; }
    public byte getContentType() { return contentType; }
    public byte getCryptoType1() { return cryptoType1; }
    public byte getKeyIndex() { return keyIndex; }
    public long getNcaSize() { return ncaSize; }
    public byte[] getTitleId() { return titleId; }
    public byte[] getContentIndx() { return contentIndx; }
    public byte[] getSdkVersion() { return sdkVersion; }
    public byte getCryptoType2() { return cryptoType2; }
    public byte getHeader1SignatureKeyGeneration() { return Header1SignatureKeyGeneration; }
    public byte[] getKeyGenerationReserved() { return keyGenerationReserved; }
    public byte[] getRightsId() { return rightsId; }

    public byte[] getSha256hash0() { return sha256hash0; }
    public byte[] getSha256hash1() { return sha256hash1; }
    public byte[] getSha256hash2() { return sha256hash2; }
    public byte[] getSha256hash3() { return sha256hash3; }

    public byte[] getEncryptedKey0() { return encryptedKey0; }
    public byte[] getEncryptedKey1() { return encryptedKey1; }
    public byte[] getEncryptedKey2() { return encryptedKey2; }
    public byte[] getEncryptedKey3() { return encryptedKey3; }

    public byte[] getDecryptedKey0() { return decryptedKey0; }
    public byte[] getDecryptedKey1() { return decryptedKey1; }
    public byte[] getDecryptedKey2() { return decryptedKey2; }
    public byte[] getDecryptedKey3() { return decryptedKey3; }
    /**
     * Get NCA Hedaer Table Entry for selected id
     * @param id must be 0-3
     * */
    public NCAHeaderTableEntry getTableEntry(int id) throws Exception{
        switch (id) {
            case 0:
                return getTableEntry0();
            case 1:
                return getTableEntry1();
            case 2:
                return getTableEntry2();
            case 3:
                return getTableEntry3();
            default:
                throw new Exception("NCA Table Entry must be defined in range 0-3 while '"+id+"' requested");
        }
    }
    public NCAHeaderTableEntry getTableEntry0() { return tableEntry0; }
    public NCAHeaderTableEntry getTableEntry1() { return tableEntry1; }
    public NCAHeaderTableEntry getTableEntry2() { return tableEntry2; }
    public NCAHeaderTableEntry getTableEntry3() { return tableEntry3; }
    /**
     * Get NCA Section Block for selected section
     * @param id must be 0-3
     * */
    public NcaFsHeader getSectionBlock(int id) throws Exception{
        switch (id) {
            case 0:
                return getSectionBlock0();
            case 1:
                return getSectionBlock1();
            case 2:
                return getSectionBlock2();
            case 3:
                return getSectionBlock3();
            default:
                throw new Exception("NCA Section Block must be defined in range 0-3 while '"+id+"' requested");
        }
    }
    public NcaFsHeader getSectionBlock0() { return sectionBlock0; }
    public NcaFsHeader getSectionBlock1() { return sectionBlock1; }
    public NcaFsHeader getSectionBlock2() { return sectionBlock2; }
    public NcaFsHeader getSectionBlock3() { return sectionBlock3; }

    public boolean isKeyAvailable(){        // NOTE: never used
        if (Arrays.equals(rightsId, new byte[0x10]))
            return false;
        else
            return keys.containsKey(byteArrToHexStringAsLE(rightsId));
    }
    /**
     * Get content for the selected section
     * @param sectionNumber must be 0-3
     * */
    public NCAContent getNCAContentProvider(int sectionNumber) throws Exception{
        switch (sectionNumber) {
            case 0:
                return ncaContent0;
            case 1:
                return ncaContent1;
            case 2:
                return ncaContent2;
            case 3:
                return ncaContent3;
            default:
                throw new Exception("NCA Content must be requested in range of 0-3, while 'Section Number "+sectionNumber+"' requested");
        }
    }

    public File getFile() {
        return file;
    }
}