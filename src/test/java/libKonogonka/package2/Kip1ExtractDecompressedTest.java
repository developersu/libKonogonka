package libKonogonka.package2;

import libKonogonka.Converter;
import libKonogonka.fs.NCA.NCAProvider;
import libKonogonka.fs.RomFs.FileSystemEntry;
import libKonogonka.fs.RomFs.RomFsProvider;
import libKonogonka.fs.other.System2.System2Provider;
import libKonogonka.fs.other.System2.ini1.Ini1Provider;
import libKonogonka.fs.other.System2.ini1.KIP1Provider;
import libKonogonka.aesctr.InFileStreamProducer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

/* ..::::::::::::::::::::: # 5 :::::::::::::::::::::..
* This test validates decompressed KIP1 CRC32 equality and sizes match between reference values and
* 1. Decompressed KIP1 extracted from INI1.bin file
* 2. Decompressed KIP1 extracted from NCA file via streams
*  */

public class Kip1ExtractDecompressedTest extends LKonPackage2Test {
    @DisplayName("KIP1 extract test (case 'FS')")
    @Test
    void testSystem2() throws Exception{
        String[] ncaFileNames = collectNcaFileNames();
        List<NCAProvider> ncaProviders = makeNcaProviders(ncaFileNames);

        NCAProvider system2FatNcaProvider = null;
        NCAProvider system2ExFatNcaProvider = null;

        for (NCAProvider ncaProvider : ncaProviders) {
            String titleId = Converter.byteArrToHexStringAsLE(ncaProvider.getTitleId());
            if (titleId.equals("0100000000000819"))
                system2FatNcaProvider = ncaProvider;
            else if (titleId.equals("010000000000081b"))
                system2ExFatNcaProvider = ncaProvider;
        }

        Assertions.assertNotNull(system2FatNcaProvider);
        Assertions.assertNotNull(system2ExFatNcaProvider);

        System.out.println("FAT   " + system2FatNcaProvider.getFile().getName() +
                "\nExFAT " + system2ExFatNcaProvider.getFile().getName());

        Assertions.assertTrue(system2FatNcaProvider.getFile().getName().endsWith("1212c.nca"));
        Assertions.assertTrue(system2ExFatNcaProvider.getFile().getName().endsWith("cc081.nca"));

        testExportedFiles(system2FatNcaProvider, exportFat, REFERENCE_FAT);
        testExportedFiles(system2ExFatNcaProvider, exportExFat, REFERENCE_EXFAT);
    }

    String[] collectNcaFileNames(){
        File firmware = new File(PATH_TO_FIRMWARE);
        Assertions.assertTrue(firmware.exists());
        String[] ncaFileNames = firmware.list((File directory, String file) -> ( ! file.endsWith(".cnmt.nca") && file.endsWith(".nca")));
        Assertions.assertNotNull(ncaFileNames);
        return ncaFileNames;
    }
    List<NCAProvider> makeNcaProviders(String[] ncaFileNames) throws Exception{
        List<NCAProvider> ncaProviders = new ArrayList<>();
        for (String ncaFileName : ncaFileNames){
            File nca = new File(PATH_TO_FIRMWARE +File.separator+ncaFileName);
            NCAProvider provider = new NCAProvider(nca, keyChainHolder.getRawKeySet());
            ncaProviders.add(provider);
        }

        Assertions.assertNotEquals(0, ncaProviders.size());

        return ncaProviders;
    }

    void testExportedFiles(NCAProvider system2NcaProvider, String exportIntoFolder, String referenceFilesFolder) throws Exception{
        RomFsProvider romFsProvider = system2NcaProvider.getNCAContentProvider(0).getRomfs();

        FileSystemEntry package2FileSystemEntry = romFsProvider.getRootEntry().getContent()
                .stream()
                .filter(e -> e.getName().equals("nx"))
                .collect(Collectors.toList())
                .get(0)
                .getContent()
                .stream()
                .filter(e -> e.getName().equals("package2"))
                .collect(Collectors.toList())
                .get(0);

        HashMap<String, Long> referencePathCrc32 = new HashMap<>();

        Files.list(Paths.get(referenceFilesFolder))
                .filter(file -> file.toString().endsWith(".dec"))
                .forEach(path -> referencePathCrc32.put(
                        path.getFileName().toString().replaceAll("\\..*$", ""),
                        calculateReferenceCRC32(path)));
        System.out.println("Files");
        romFsProvider.exportContent(exportIntoFolder, package2FileSystemEntry);
        System2Provider kernelProviderFile = new System2Provider(exportIntoFolder+File.separator+"package2", keyChainHolder);
        kernelProviderFile.getIni1Provider().export(exportIntoFolder);
        Ini1Provider ini1Provider = new Ini1Provider(Paths.get(exportIntoFolder+File.separator+"INI1.bin"));
        for (KIP1Provider kip1Provider : ini1Provider.getKip1List()) {
            String kip1Name = kip1Provider.getHeader().getName();
            kip1Provider.exportAsDecompressed(exportIntoFolder);
            Path referenceFilePath = Paths.get(referenceFilesFolder+File.separator+kip1Name+".dec");
            Path myFilePath = Paths.get(exportIntoFolder+File.separator+kip1Name+"_decompressed.kip1");

            System.out.println(
                "\nReference : " + referenceFilePath+
                "\nOwn       : " + myFilePath);

            validateChecksums(myFilePath, referencePathCrc32.get(kip1Name));
            validateSizes(referenceFilePath, myFilePath);
        }
        System.out.println("Stream");

        InFileStreamProducer producer = romFsProvider.getStreamProducer(package2FileSystemEntry);
        System2Provider providerStream = new System2Provider(producer, keyChainHolder);
        for (KIP1Provider kip1Provider : providerStream.getIni1Provider().getKip1List()){
            String kip1Name = kip1Provider.getHeader().getName();
            kip1Provider.exportAsDecompressed(exportIntoFolder);
            Path referenceFilePath = Paths.get(referenceFilesFolder+File.separator+kip1Name+".dec");
            Path myFilePath = Paths.get(exportIntoFolder+File.separator+kip1Name+"_decompressed.kip1");

            System.out.println(
                    "\nReference : " + referenceFilePath+
                    "\nOwn       : " + myFilePath);

            validateChecksums(myFilePath, referencePathCrc32.get(kip1Name));
            validateSizes(referenceFilePath, myFilePath);
        }
        System.out.println("---");
    }
    long calculateReferenceCRC32(Path refPackage2Path){
        try {
            byte[] refPackage2Bytes = Files.readAllBytes(refPackage2Path);
            CRC32 crc32 = new CRC32();
            crc32.update(refPackage2Bytes, 0, refPackage2Bytes.length);
            return crc32.getValue();
        }
        catch (Exception e) {
            return -1;
        }
    }

    void validateChecksums(Path myPackage2Path, long refPackage2Crc32) throws Exception{
        // Check CRC32 for package2 file only
        byte[] myPackage2Bytes = Files.readAllBytes(myPackage2Path);
        CRC32 crc32 = new CRC32();
        crc32.update(myPackage2Bytes, 0, myPackage2Bytes.length);
        long myPackage2Crc32 = crc32.getValue();
        Assertions.assertEquals(myPackage2Crc32, refPackage2Crc32);
    }

    void validateSizes(Path a, Path b) throws Exception{
        Assertions.assertEquals(Files.size(a), Files.size(b));
    }
}
