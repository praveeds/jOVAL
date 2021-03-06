// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

namespace jOVAL {
    using System;
    using System.Collections.Generic;
    using System.Collections.Specialized;
    using System.Diagnostics;
    using System.Runtime.CompilerServices;
    using System.Runtime.InteropServices;
    using System.Text;

    namespace Environmentvariable58 {
	[Flags]
	public enum ProcessAccessFlags : uint {
	    All			= 0x001F0FFF,
	    Terminate		= 0x00000001,
	    CreateThread	= 0x00000002,
	    VMOperation		= 0x00000008,
	    VMRead		= 0x00000010,
	    VMWrite		= 0x00000020,
	    DupHandle		= 0x00000040,
	    SetInformation	= 0x00000200,
	    QueryInformation	= 0x00000400,
	    Synchronize		= 0x00100000
	}

	[StructLayout(LayoutKind.Sequential, Pack = 1)]
	public struct PROCESS_BASIC_INFORMATION {
	    public IntPtr Reserved1;
	    public IntPtr PebBaseAddress;
	    [MarshalAs(UnmanagedType.ByValArray, SizeConst = 2)]
	    public IntPtr[] Reserved2;
	    public IntPtr UniqueProcessId;
	    public IntPtr Reserved3;
	}

	[StructLayout(LayoutKind.Sequential)]
	public struct MEMORY_BASIC_INFORMATION {
	    public IntPtr BaseAddress;
	    public IntPtr AllocationBase;
	    public int AllocationProtect;
	    public IntPtr RegionSize;
	    public int State;
	    public int Protect;
	    public int Type;
	}

	[StructLayout(LayoutKind.Sequential, Size=40)]
	public struct PROCESS_MEMORY_COUNTERS {
	    public uint cb;
	    public uint PageFaultCount;
	    public IntPtr PeakWorkingSetSize;
	    public IntPtr WorkingSetSize;
	    public IntPtr QuotaPeakPagedPoolUsage;
	    public IntPtr QuotaPagedPoolUsage;
	    public IntPtr QuotaPeakNonPagedPoolUsage;
	    public IntPtr QuotaNonPagedPoolUsage;
	    public IntPtr PagefileUsage;
	    public IntPtr PeakPagefileUsage;
	}

	public class ProcessHelper {
	    [DllImport("kernel32.dll")]
	    public static extern IntPtr OpenProcess(ProcessAccessFlags dwDesiredAccess, bool bInheritHandle, int dwProcessId);

	    [DllImport("kernel32.dll", SetLastError = true)]
	    public static extern bool IsWow64Process(IntPtr hProcess, out bool wow64Process);

	    [DllImport("kernel32.dll", SetLastError=true)][return: MarshalAs(UnmanagedType.Bool)]
	    public static extern bool CloseHandle(IntPtr hObject);

	    [DllImport("kernel32.dll")]
	    public static extern int GetLastError();

	    public static int GetProcessArchitecture(UInt32 pid) {
		IntPtr hProcess = IntPtr.Zero;
		ProcessAccessFlags flags = ProcessAccessFlags.QueryInformation | ProcessAccessFlags.VMRead;
		hProcess = OpenProcess(flags, false, (int)pid);
		if (hProcess == IntPtr.Zero) {
		    throw new System.ComponentModel.Win32Exception(GetLastError());
		}
		try {
		    bool wow64;
		    if (!IsWow64Process(hProcess, out wow64)) {
			return 32; // call failed means 32-bit
		    }
		    if (wow64) {
			return 32;
		    } else {
			return 64;
		    }
		} finally {
		    CloseHandle(hProcess);
		}
	    }
	}

	public class Probe {
	    public const int PAGE_NOACCESS = 0x01;
	    public const int PAGE_EXECUTE = 0x10;
	    public const int ProcessBasicInformation = 0;
	    public const int ProcessWow64Information = 26;

	    [DllImport("kernel32.dll")]
	    public static extern int GetLastError();

	    [DllImport("kernel32.dll", SetLastError=true)][return: MarshalAs(UnmanagedType.Bool)]
	    public static extern bool CloseHandle(IntPtr hObject);

	    [DllImport("kernel32.dll")]
	    public static extern IntPtr OpenProcess(ProcessAccessFlags dwDesiredAccess, bool bInheritHandle, int dwProcessId);

	    [DllImport("kernel32.dll", SetLastError = true)]
	    public static extern bool IsWow64Process(IntPtr hProcess, out bool wow64Process);

	    [DllImport("psapi.dll", SetLastError=true)]
	    public static extern bool GetProcessMemoryInfo
		(IntPtr hProcess, out PROCESS_MEMORY_COUNTERS counters, out uint size);

	    [DllImport("ntdll.dll", SetLastError = true)]
	    public static extern int NtQueryInformationProcess
		(IntPtr hProcess, int pic, ref PROCESS_BASIC_INFORMATION pbi, int cb, ref int pSize);

	    [DllImport("ntdll.dll", SetLastError = true)]
	    public static extern int NtQueryInformationProcess
		(IntPtr hProcess, int pic, ref IntPtr pi, int cb, ref int pSize);

	    [DllImport("kernel32.dll", SetLastError = true)]
	    public static extern bool ReadProcessMemory
		(IntPtr hProcess, IntPtr lpBaseAddress, [Out] byte[] lpBuffer, IntPtr dwSize, ref IntPtr lpNumberOfBytesRead);

	    [DllImport("kernel32.dll", SetLastError = true)]
	    public static extern bool ReadProcessMemory
		(IntPtr hProcess, IntPtr lpBaseAddress, IntPtr lpBuffer, IntPtr dwSize, ref IntPtr lpNumberOfBytesRead);

	    [DllImport("kernel32", SetLastError = true)]
	    public static extern int VirtualQueryEx
		(IntPtr hProcess, IntPtr lpAddress, ref MEMORY_BASIC_INFORMATION lpBuffer, int dwLength);

	    public static StringDictionary GetEnvironmentVariables(UInt32 pid) {
		ProcessAccessFlags flags = ProcessAccessFlags.QueryInformation | ProcessAccessFlags.VMRead;
		IntPtr hProcess = IntPtr.Zero;
		hProcess = OpenProcess(flags, false, (int)pid);
		if (hProcess == IntPtr.Zero) {
		    throw new System.ComponentModel.Win32Exception(GetLastError());
		}
		IntPtr penv = GetPenv(hProcess);
		try {
		    uint size;
		    PROCESS_MEMORY_COUNTERS pmc = new PROCESS_MEMORY_COUNTERS();
		    if (!GetProcessMemoryInfo(hProcess, out pmc, out size)) {
			throw new System.ComponentModel.Win32Exception(GetLastError());
		    }
		    int dataSize = (int)pmc.WorkingSetSize;
		    const int maxEnvSize = 32767;
		    if (dataSize > maxEnvSize) {
			dataSize = maxEnvSize;
		    }
		    byte[] envData = new byte[dataSize];
		    IntPtr res_len = IntPtr.Zero;
		    bool b = ReadProcessMemory(hProcess, penv, envData, new IntPtr(dataSize), ref res_len);
		    if (!b || (int)res_len != dataSize) {
			throw new System.ComponentModel.Win32Exception(GetLastError());
		    }
		    return EnvToDictionary(envData);
		} finally {
		    CloseHandle(hProcess);
		}
	    }
		
	    static StringDictionary EnvToDictionary(byte[] env) {
		StringDictionary result = new StringDictionary();
		int len = env.Length;
		if (len < 4) {
		    return result;
		}
		int n = len - 3;
		for (int i=0; i < n; ++i) {
		    byte c1 = env[i];
		    byte c2 = env[i + 1];
		    byte c3 = env[i + 2];
		    byte c4 = env[i + 3];
		    if (c1 == 0 && c2 == 0 && c3 == 0 && c4 == 0) {
			len = i + 3;
			break;
		    }
		}
		char[] envChars = Encoding.Unicode.GetChars(env, 0, len);
		for (int i=0; i < envChars.Length; i++) {
		    int startIndex = i;
		    while ((envChars[i] != '=') && (envChars[i] != '\0') && (i < envChars.Length)) {
			i++;
		    }
		    if (envChars[i] != '\0') {
			if ((i - startIndex) == 0) {
			    while (envChars[i] != '\0' && i < envChars.Length) {
				i++;
			    }
			} else {
			    string str = new string(envChars, startIndex, i - startIndex);
			    if (i < envChars.Length) {
				i++;
				int num3 = i;
				while (envChars[i] != '\0' && i < envChars.Length) {
				    i++;
				}
				string str2 = new string(envChars, num3, i - num3);
				result[str] = str2;
			    } else {
				result[str] = "";
			    }
			}
		    }
		}
		return result;
	    }
		
	    static IntPtr GetPenv(IntPtr hProcess) {
		if (GetProcessBitness(hProcess) == 64) {
		    if (IntPtr.Size != 8) {
			String msg = "A 64-bit process environment can only be read by a 64-bit process.";
			throw new InvalidOperationException(msg);
		    }
		    IntPtr pPeb = GetPebNative(hProcess);
		    IntPtr ptr;
		    if (!ReadIntPtr(hProcess, new IntPtr(pPeb.ToInt64() + 0x20), out ptr)) {
			throw new Exception("Unable to read PEB.");
		    }
		    IntPtr penv;
		    if (!ReadIntPtr(hProcess, new IntPtr(ptr.ToInt64() + 0x80), out penv)) {
			throw new Exception("Unable to read RTL_USER_PROCESS_PARAMETERS.");
		    }
		    return penv;
		} else {
		    IntPtr pPeb = GetPeb32(hProcess);
		    IntPtr ptr;
		    if (!ReadIntPtr32(hProcess, new IntPtr(pPeb.ToInt64() + 0x10), out ptr)) {
			throw new Exception("Unable to read PEB.");
		    }
		    IntPtr penv;
		    if (!ReadIntPtr32(hProcess, new IntPtr(ptr.ToInt64() + 0x48), out penv)) {
			throw new Exception("Unable to read RTL_USER_PROCESS_PARAMETERS.");
		    }
		    return penv;
		}
	    }
		
	    static int GetProcessBitness(IntPtr hProcess) {
		bool wow64;
		if (!IsWow64Process(hProcess, out wow64)) {
		    return 32; // call failed means 32-bit
		}
		if (wow64) {
		    return 32;
		} else {
		    return 64;
		}
	    }
		
	    static bool ReadIntPtr32(IntPtr hProcess, IntPtr ptr, out IntPtr readPtr) {
		bool result;
		RuntimeHelpers.PrepareConstrainedRegions();
		int dataSize = sizeof(Int32);
		IntPtr data = Marshal.AllocHGlobal(dataSize);
		IntPtr res_len = IntPtr.Zero;
		bool b = ReadProcessMemory(hProcess, ptr, data, new IntPtr(dataSize), ref res_len);
		readPtr = new IntPtr(Marshal.ReadInt32(data));
		Marshal.FreeHGlobal(data);
		if (!b || (int)res_len != dataSize) {
		    result = false;
		} else {
		    result = true;
		}
		return result;
	    }
		
	    static bool ReadIntPtr(IntPtr hProcess, IntPtr ptr, out IntPtr readPtr) {
		bool result;
		RuntimeHelpers.PrepareConstrainedRegions();
		int dataSize = IntPtr.Size;
		IntPtr data = Marshal.AllocHGlobal(dataSize);
		IntPtr res_len = IntPtr.Zero;
		bool b = ReadProcessMemory(hProcess, ptr, data, new IntPtr(dataSize), ref res_len);
		readPtr = Marshal.ReadIntPtr(data);
		Marshal.FreeHGlobal(data);
		if (!b || (int)res_len != dataSize) {
		    result = false;
		} else {
		    result = true;
		}
		return result;
	    }
		
	    static IntPtr GetPeb32(IntPtr hProcess) {
		if (IntPtr.Size == 8) {
		    IntPtr ptr = IntPtr.Zero;
		    int res_len = 0;
		    int pbiSize = IntPtr.Size;
		    int status = NtQueryInformationProcess(hProcess, ProcessWow64Information, ref ptr, pbiSize, ref res_len);
		    if (res_len != pbiSize) {
			throw new Exception("Unable to query process information.");
		    }
		    return ptr;
		} else {
		    return GetPebNative(hProcess);
		}
	    }
		
	    static IntPtr GetPebNative(IntPtr hProcess) {
		PROCESS_BASIC_INFORMATION pbi = new PROCESS_BASIC_INFORMATION();
		int res_len = 0;
		int pbiSize = Marshal.SizeOf(pbi);
		int status = NtQueryInformationProcess(hProcess, ProcessBasicInformation, ref pbi, pbiSize, ref res_len);
		if (res_len != pbiSize) {
		    throw new Exception("Unable to query process information.");
		}
		return pbi.PebBaseAddress;
	    }
	}
    }
}
