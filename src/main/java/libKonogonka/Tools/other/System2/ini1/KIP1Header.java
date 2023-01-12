/*
    Copyright 2019-2023 Dmitry Isaenko

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
package libKonogonka.Tools.other.System2.ini1;

import libKonogonka.Converter;
import libKonogonka.RainbowDump;
import libKonogonka.Tools.NPDM.KernelAccessControlProvider;
import libKonogonka.Tools.NSO.SegmentHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class KIP1Header {
    private final static Logger log = LogManager.getLogger(KIP1Header.class);

    private final String magic;
    private final String name;
    private final byte[] programId;
    private final int version;
    private final byte mainThreadPriority;
    private final byte mainThreadCoreNumber;
    private final byte reserved1;
    private final byte flags;
    private final SegmentHeader textSegmentHeader;
    private final int threadAffinityMask;
    private final SegmentHeader roDataSegmentHeader;
    private final int mainThreadStackSize;
    private final SegmentHeader rwDataSegmentHeader;
    private final byte[] reserved2;
    private final SegmentHeader bssSegmentHeader;
    private final byte[] reserved3;
    private final KernelAccessControlProvider kernelCapabilityData;
    
    public KIP1Header(byte[] kip1HeaderBytes) throws Exception{
        this.magic = new String(kip1HeaderBytes, 0, 0x4);
        this.name = new String(kip1HeaderBytes, 0x4, 0xC).trim();
        this.programId = Arrays.copyOfRange(kip1HeaderBytes, 0x10, 0x18);
        this.version = Converter.getLEint(kip1HeaderBytes, 0x18);
        this.mainThreadPriority = kip1HeaderBytes[0x1c];
        this.mainThreadCoreNumber = kip1HeaderBytes[0x1d];
        this.reserved1 = kip1HeaderBytes[0x1e];
        this.flags = kip1HeaderBytes[0x1f];
        this.textSegmentHeader = new SegmentHeader(kip1HeaderBytes, 0x20);
        this.threadAffinityMask = Converter.getLEint(kip1HeaderBytes, 0x2c);
        this.roDataSegmentHeader = new SegmentHeader(kip1HeaderBytes, 0x30);
        this.mainThreadStackSize = Converter.getLEint(kip1HeaderBytes, 0x3c);
        this.rwDataSegmentHeader = new SegmentHeader(kip1HeaderBytes, 0x40);
        this.reserved2 = Arrays.copyOfRange(kip1HeaderBytes, 0x4c, 0x50);
        this.bssSegmentHeader = new SegmentHeader(kip1HeaderBytes, 0x50);
        this.reserved3 = Arrays.copyOfRange(kip1HeaderBytes, 0x5c, 0x80);
        this.kernelCapabilityData = new KernelAccessControlProvider(Arrays.copyOfRange(kip1HeaderBytes, 0x80, 0x100));
    }

    public String getMagic() { return magic; }
    public String getName() { return name; }
    public byte[] getProgramId() { return programId; }
    public int getVersion() { return version; }
    public byte getMainThreadPriority() { return mainThreadPriority; }
    public byte getMainThreadCoreNumber() { return mainThreadCoreNumber; }
    public byte getReserved1() { return reserved1; }
    public byte getFlags() { return flags; }
    public boolean isTextCompressFlag(){ return (flags & 1) == 1; }
    public boolean isRoDataCompressFlag(){ return (flags >> 1 & 1) == 1; }
    public boolean isRwDataCompressFlag(){ return (flags >> 2 & 1) == 1; }
    public boolean is64BitInstruction(){ return (flags >> 3 & 1) == 1; }
    public boolean isAddressSpace64Bit(){ return (flags >> 4 & 1) == 1; }
    public boolean isUseSecureMemory(){ return (flags >> 5 & 1) == 1; }
    public SegmentHeader getTextSegmentHeader() { return textSegmentHeader; }
    public int getThreadAffinityMask() { return threadAffinityMask; }
    public SegmentHeader getRoDataSegmentHeader() { return roDataSegmentHeader; }
    public int getMainThreadStackSize() { return mainThreadStackSize; }
    public SegmentHeader getRwDataSegmentHeader() { return rwDataSegmentHeader; }
    public byte[] getReserved2() { return reserved2; }
    public SegmentHeader getBssSegmentHeader() { return bssSegmentHeader; }
    public byte[] getReserved3() { return reserved3; }
    public KernelAccessControlProvider getKernelCapabilityData() { return kernelCapabilityData; }

    public void printDebug(){
        StringBuilder mapIoOrNormalRange = new StringBuilder();
        StringBuilder interruptPairs = new StringBuilder();
        StringBuilder syscallMasks = new StringBuilder();

        kernelCapabilityData.getMapIoOrNormalRange().forEach((bytes, aBoolean) -> {
            mapIoOrNormalRange.append("    ");
            mapIoOrNormalRange.append(Converter.byteArrToHexStringAsLE(bytes));
            mapIoOrNormalRange.append(" : ");
            mapIoOrNormalRange.append(aBoolean);
            mapIoOrNormalRange.append("\n");
        });
        kernelCapabilityData.getInterruptPairs().forEach((aInteger, bytes) -> {
            interruptPairs.append("  #");
            interruptPairs.append(aInteger);
            for (byte[] innerArray : bytes) {
                interruptPairs.append("\n    |- ");
                interruptPairs.append(Converter.byteArrToHexStringAsLE(innerArray));
            }
            interruptPairs.append("\n");
        });
        kernelCapabilityData.getSyscallMasks().forEach((aByte, bytes) -> {
            syscallMasks.append("    ");
            syscallMasks.append(String.format("0x%x", aByte));
            syscallMasks.append(" : ");
            syscallMasks.append(Converter.byteArrToHexStringAsLE(bytes));
            syscallMasks.append("\n");
        });

        log.debug("..:: KIP1 ::..\n" +
                "Magic                            : " + magic + "\n" +
                "Name                             : " + name + "\n" +
                "ProgramId                        : " + Converter.byteArrToHexStringAsLE(programId) + "\n" +
                "Version                          : " + RainbowDump.formatDecHexString(version) + "\n" +
                "Main thread priority             : " + String.format("0x%x", mainThreadPriority) + "\n" +
                "Main thread core number          : " + String.format("0x%x", mainThreadCoreNumber) + "\n" +
                "Reserved 1                       : " + String.format("0x%x", reserved1) + "\n" +
                "Flags                            : " + Converter.byteToBinaryString(flags) + "\n" +
                "   0| .text compress             : " + ((flags & 1) == 1 ? "YES" : "NO") + "\n" +
                "   1| .ro compress               : " + ((flags >> 1 & 1) == 1 ? "YES" : "NO") + "\n" +
                "   2| .rw compress               : " + ((flags >> 2 & 1) == 1 ? "YES" : "NO") + "\n" +
                "   3| Is 64-bit instruction      : " + ((flags >> 3 & 1) == 1 ? "YES" : "NO") + "\n" +
                "   4| Process addr. space 64-bit : " + ((flags >> 4 & 1) == 1 ? "YES" : "NO") + "\n" +
                "   5| Use secure memory          : " + ((flags >> 5 & 1) == 1 ? "YES" : "NO") + "\n" +
                ".text segment header\n" +
                "   Segment offset                : " + RainbowDump.formatDecHexString(textSegmentHeader.getSegmentOffset()) + "\n" +
                "   Memory offset                 : " + RainbowDump.formatDecHexString(textSegmentHeader.getMemoryOffset()) + "\n" +
                "   Size                          : " + RainbowDump.formatDecHexString(textSegmentHeader.getSize()) + "\n" +
                "Thread affinity mask             : " + RainbowDump.formatDecHexString(threadAffinityMask) + "\n" +
                ".ro segment header\n" +
                "   Segment offset                : " + RainbowDump.formatDecHexString(roDataSegmentHeader.getSegmentOffset()) + "\n" +
                "   Memory offset                 : " + RainbowDump.formatDecHexString(roDataSegmentHeader.getMemoryOffset()) + "\n" +
                "   Size                          : " + RainbowDump.formatDecHexString(roDataSegmentHeader.getSize()) + "\n" +
                "Main thread stack size           : " + RainbowDump.formatDecHexString(mainThreadStackSize) + "\n" +
                ".rw segment header\n" +
                "   Segment offset                : " + RainbowDump.formatDecHexString(rwDataSegmentHeader.getSegmentOffset()) + "\n" +
                "   Memory offset                 : " + RainbowDump.formatDecHexString(rwDataSegmentHeader.getMemoryOffset()) + "\n" +
                "   Size                          : " + RainbowDump.formatDecHexString(rwDataSegmentHeader.getSize()) + "\n" +
                "Reserved 2                       : " + Converter.byteArrToHexStringAsLE(reserved2) + "\n" +
                ".bss segment header\n" +
                "   Segment offset                : " + RainbowDump.formatDecHexString(bssSegmentHeader.getSegmentOffset()) + "\n" +
                "   Memory offset                 : " + RainbowDump.formatDecHexString(bssSegmentHeader.getMemoryOffset()) + "\n" +
                "   Size                          : " + RainbowDump.formatDecHexString(bssSegmentHeader.getSize()) + "\n" +
                "Reserved 3                       : " + Converter.byteArrToHexStringAsLE(reserved3) + "\n" +
                "Kernel capability data\n" +
                "  Kernel flags available?        : " + kernelCapabilityData.isKernelFlagsAvailable() + "\n" +
                "          |- CPU ID Hi           : " + kernelCapabilityData.getKernelFlagCpuIdHi() + "\n" +
                "          |- CPU ID Low          : " + kernelCapabilityData.getKernelFlagCpuIdLo() + "\n" +
                "          |- Thread priority Hi  : " + kernelCapabilityData.getKernelFlagThreadPrioHi() + "\n" +
                "          |- Thread priority Low : " + kernelCapabilityData.getKernelFlagThreadPrioLo()+ "\n" +
                "Map IO or Normal Range:\n" + mapIoOrNormalRange +
                "Interrupt pairs\n" + interruptPairs +
                "Map normal page                  : " + Converter.byteArrToHexStringAsLE(kernelCapabilityData.getMapNormalPage()) + "\n" +
                "Application type                 : " + RainbowDump.formatDecHexString(kernelCapabilityData.getApplicationType()) + "\n" +
                "  Kernel rel. version available? : " + kernelCapabilityData.isKernelRelVersionAvailable() + "\n" +
                "          |- Version Major       : " + kernelCapabilityData.getKernelRelVersionMajor() + "\n" +
                "          |- Version Minor       : " + kernelCapabilityData.getKernelRelVersionMinor() + "\n" +
                "Handle table size                : " + RainbowDump.formatDecHexString(kernelCapabilityData.getHandleTableSize()) + "\n" +
                "  Debug flags available?         : " + kernelCapabilityData.isDebugFlagsAvailable() + "\n" +
                "          |- Can be debugged     : " + kernelCapabilityData.isCanBeDebugged() + "\n" +
                "          |- Can debug others    : " + kernelCapabilityData.isCanDebugOthers() + "\n" +
                "Syscall masks\n" + syscallMasks
        );
    }
}
