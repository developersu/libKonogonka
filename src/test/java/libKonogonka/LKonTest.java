package libKonogonka;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LKonTest {
    protected static KeyChainHolder keyChainHolder;

    protected static final String KEYS_FILE_LOCATION = "FilesForTests"+File.separator+"prod.keys";
    protected static final String XCI_HEADER_KEYS_FILE_LOCATION = "FilesForTests"+File.separator+"xci_header_key.txt";
    protected static final String PATH_TO_FIRMWARE = "/home/loper/Projects/tempPatchesPlayground/nintendo-switch-global-firmwares/"+ File.separator+"Firmware 14.1.0";
    protected static final String PATH_TO_FIRMWARES = "/home/loper/Projects/tempPatchesPlayground/nintendo-switch-global-firmwares/";
    protected static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    @BeforeAll
    protected static void makeKeys() throws Exception{
        String keyValue = new String(Files.readAllBytes(Paths.get(XCI_HEADER_KEYS_FILE_LOCATION))).trim();
        Assertions.assertNotEquals(0, keyValue.length());
        keyChainHolder = new KeyChainHolder(KEYS_FILE_LOCATION, keyValue);
    }
}
