using System;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;
using System.Windows.Forms;

[assembly: System.Reflection.AssemblyTitle("KernelN64")]
[assembly: System.Reflection.AssemblyProduct("KernelN64")]

// Small Windows entry point for tools that require an EXE. Never invokes a shell.
internal static class Launcher {
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode)] static extern IntPtr CreateJobObject(IntPtr attributes, string name);
    [DllImport("kernel32.dll")] static extern bool SetInformationJobObject(IntPtr job, int infoClass, IntPtr info, uint size);
    [DllImport("kernel32.dll")] static extern bool AssignProcessToJobObject(IntPtr job, IntPtr process);
    [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr handle);
    [StructLayout(LayoutKind.Sequential)] struct BasicLimits {
        public long ProcessTime, JobTime;
        public uint Flags;
        public UIntPtr MinimumWorkingSet, MaximumWorkingSet;
        public uint ActiveProcesses;
        public UIntPtr Affinity;
        public uint PriorityClass, SchedulingClass;
    }
    [StructLayout(LayoutKind.Sequential)] struct IoCounters { public ulong ReadOperations, WriteOperations, OtherOperations, ReadBytes, WriteBytes, OtherBytes; }
    [StructLayout(LayoutKind.Sequential)] struct ExtendedLimits {
        public BasicLimits Basic; public IoCounters Io;
        public UIntPtr ProcessMemory, JobMemory, PeakProcessMemory, PeakJobMemory;
    }
    static string Quote(string value) {
        StringBuilder text = new StringBuilder("\""); int slashes = 0;
        foreach (char c in value) {
            if (c == '\\') { slashes++; continue; }
            if (c == '"') { text.Append('\\', slashes * 2 + 1); text.Append('"'); }
            else { text.Append('\\', slashes); text.Append(c); }
            slashes = 0;
        }
        text.Append('\\', slashes * 2); text.Append('"'); return text.ToString();
    }
    static IntPtr Track(Process process) {
        IntPtr job = CreateJobObject(IntPtr.Zero, null);
        if (job == IntPtr.Zero) return IntPtr.Zero;
        ExtendedLimits limits = new ExtendedLimits(); limits.Basic.Flags = 0x2000; // Kill children when launcher closes.
        int size = Marshal.SizeOf(typeof(ExtendedLimits)); IntPtr memory = Marshal.AllocHGlobal(size);
        try {
            Marshal.StructureToPtr(limits, memory, false);
            if (!SetInformationJobObject(job, 9, memory, (uint)size) || !AssignProcessToJobObject(job, process.Handle)) {
                CloseHandle(job); return IntPtr.Zero;
            }
            return job;
        } finally { Marshal.FreeHGlobal(memory); }
    }
    [STAThread] static int Main(string[] args) {
        string home = AppDomain.CurrentDomain.BaseDirectory;
        string app = File.Exists(Path.Combine(home, "app.jar")) ? home : Path.Combine(home, "dist", "KernelN64");
        string logPath = null;
        try {
            bool check = args.Length == 1 && args[0] == "--check";
            if (args.Length == 1 && (args[0] == "--help" || args[0] == "-h")) {
                Console.WriteLine("KernelN64.exe [ROM.z64]\nKernelN64.exe --rom ROM.z64 [--state FILE.j64state]\nQuote paths containing spaces. --check verifies the bundled Java runtime."); return 0;
            }
            // Resolve file arguments before changing the child's working directory.
            bool positional = false;
            for (int i = 0; !check && i < args.Length; i++) {
                string argument = args[i];
                if (!positional && argument == "--") { positional = true; continue; }
                if (!positional && (argument == "--rom" || argument == "--state")) {
                    if (++i == args.Length) throw new ArgumentException("Missing path after " + argument);
                } else if (!positional && argument.StartsWith("-")) continue;
                args[i] = Path.GetFullPath(args[i]);
                if (!File.Exists(args[i])) throw new FileNotFoundException("File not found", args[i]);
            }
            string java = Path.Combine(app, "runtime", "bin", "javaw.exe");
            if (!File.Exists(java) || !File.Exists(Path.Combine(app, "app.jar")))
                throw new FileNotFoundException("Launcher files are missing. Run launcher\\Build.ps1 or keep the complete portable folder together.");
            Directory.CreateDirectory(Path.Combine(home, "Logs"));
            logPath = Path.Combine(home, "Logs", "launcher-" + DateTime.Now.ToString("yyyyMMdd-HHmmss-fff") + ".log");
            StringBuilder command = new StringBuilder();
            if (check) command.Append("-version");
            else {
                command.Append("-Xmx1g --enable-native-access=ALL-UNNAMED ");
                command.Append(Quote("-Djava.library.path=" + Path.Combine(app, "lib"))).Append(' ');
                command.Append("-cp ").Append(Quote(Path.Combine(app, "app.jar") + ";" + Path.Combine(app, "lib", "*"))).Append(" kerneln64.Main");
                foreach (string argument in args) command.Append(' ').Append(Quote(argument));
            }
            using (StreamWriter log = new StreamWriter(logPath, false, Encoding.UTF8)) {
                object gate = new object(); log.AutoFlush = true;
                log.WriteLine("KernelN64 launcher\nApplication: " + app + "\nWorking directory: " + home);
                ProcessStartInfo start = new ProcessStartInfo(java, command.ToString());
                start.WorkingDirectory = home; start.UseShellExecute = false; start.CreateNoWindow = true;
                start.WindowStyle = ProcessWindowStyle.Hidden;
                start.RedirectStandardOutput = true; start.RedirectStandardError = true;
                using (Process process = new Process()) {
                    process.StartInfo = start;
                    process.OutputDataReceived += delegate(object sender, DataReceivedEventArgs e) { if (e.Data != null) lock (gate) log.WriteLine(e.Data); };
                    process.ErrorDataReceived += delegate(object sender, DataReceivedEventArgs e) { if (e.Data != null) lock (gate) log.WriteLine(e.Data); };
                    process.Start(); IntPtr job = Track(process);
                    try {
                        if (job == IntPtr.Zero) log.WriteLine("Child process tracking unavailable on this host.");
                        process.BeginOutputReadLine(); process.BeginErrorReadLine(); process.WaitForExit();
                        int code = process.ExitCode; log.WriteLine("Exit code: " + code);
                        if (code != 0 && !check) MessageBox.Show("KernelN64 exited with code " + code + ".\nLog: " + logPath, "KernelN64", MessageBoxButtons.OK, MessageBoxIcon.Error);
                        return code;
                    } finally { if (job != IntPtr.Zero) CloseHandle(job); }
                }
            }
        } catch (Exception error) {
            MessageBox.Show(error.Message + (logPath == null ? "" : "\nLog: " + logPath), "KernelN64 Launcher", MessageBoxButtons.OK, MessageBoxIcon.Error); return 1;
        }
    }
}
