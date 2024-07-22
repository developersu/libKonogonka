package libKonogonka.package2;

import libKonogonka.LKonTest;

import java.io.File;

public class LKonPackage2Test extends LKonTest {
    protected static final String REFERENCE_FAT = "FilesForTests"+ File.separator+"reference_for_system2"+File.separator+"FAT";
    protected static final String REFERENCE_EXFAT = "FilesForTests"+File.separator+"reference_for_system2"+File.separator+"ExFAT";

    protected String exportFat = TEMP_DIR+File.separator+"Exported_FAT"+File.separator+getClass().getSimpleName();
    protected String exportExFat = TEMP_DIR+File.separator+"Exported_ExFAT"+File.separator+getClass().getSimpleName();
}
