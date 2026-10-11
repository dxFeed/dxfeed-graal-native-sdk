// Reads a static library (a COFF archive) and hides its internal symbols: Hide-StaticLibrarySymbols.ps1 and
// New-StaticLibrary.ps1 load it with Add-Type. Windows PowerShell 5.1 compiles it with the C# 5 compiler of
// .NET Framework.
using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;

namespace DxFeed.StaticLibrary
{
    public sealed class ArchiveMember
    {
        public string Name;
        public byte[] Data;
    }

    public static class Archive
    {
        // The objects of the archive in its order; the linker members (the symbol index) and the long names are
        // skipped.
        public static List<ArchiveMember> Read(string path)
        {
            byte[] data = File.ReadAllBytes(path);
            if (data.Length < 8 || Encoding.ASCII.GetString(data, 0, 8) != "!<arch>\n")
            {
                throw new InvalidDataException(path + " is not an archive");
            }
            List<ArchiveMember> members = new List<ArchiveMember>();
            byte[] longNames = null;
            int position = 8;
            while (position + 60 <= data.Length)
            {
                string name = Encoding.ASCII.GetString(data, position, 16).TrimEnd(' ');
                int size = int.Parse(Encoding.ASCII.GetString(data, position + 48, 10).Trim());
                int start = position + 60;
                if (data[position + 58] != 0x60 || data[position + 59] != 0x0A || start + size > data.Length)
                {
                    throw new InvalidDataException(path + ": a broken member header at " + position);
                }
                byte[] content = new byte[size];
                Buffer.BlockCopy(data, start, content, 0, size);
                if (name == "//")
                {
                    longNames = content;
                }
                else if (name != "/" && !name.StartsWith("/<"))
                {
                    ArchiveMember member = new ArchiveMember();
                    member.Name = name.StartsWith("/")
                        ? LongName(longNames, int.Parse(name.Substring(1)))
                        : name.TrimEnd('/');
                    member.Data = content;
                    members.Add(member);
                }
                position = start + size + (size & 1);
            }
            return members;
        }

        private static string LongName(byte[] longNames, int offset)
        {
            int end = offset;
            while (end < longNames.Length && longNames[end] != 0 && longNames[end] != '\n')
            {
                end++;
            }
            return Encoding.ASCII.GetString(longNames, offset, end - offset).TrimEnd('/');
        }
    }

    // A regular COFF x64 object: its sections and symbols, read in place.
    internal sealed class CoffObject
    {
        private const int SymbolSize = 18;
        private const int SectionHeaderSize = 40;
        private const int MachineAmd64 = 0x8664;
        // Latin-1 maps each byte to one character, so a match position is a byte position in the section.
        private static readonly Encoding Latin1 = Encoding.GetEncoding(28591);

        public readonly string Name;
        public readonly byte[] Data;
        public readonly int SectionCount;
        public readonly int SectionTable;
        public readonly int SymbolTable;
        public readonly int SymbolCount;
        public readonly int StringTable;
        public readonly int StringTableSize;

        public CoffObject(ArchiveMember member)
        {
            Name = member.Name;
            Data = member.Data;
            if (Data.Length < 20 || (U16(0) == 0 && U16(2) == 0xFFFF))
            {
                throw new InvalidDataException(
                    Name + " is not a regular COFF object (an import, bigobj or LTCG object)");
            }
            if (U16(0) != MachineAmd64)
            {
                throw new InvalidDataException(
                    Name + " is not an x64 object (machine 0x" + U16(0).ToString("X4") + ")");
            }
            SectionCount = U16(2);
            SymbolTable = I32(8);
            SymbolCount = I32(12);
            SectionTable = 20 + U16(16);
            StringTable = Symbol(SymbolCount);
            StringTableSize = SymbolCount > 0 ? I32(StringTable) : 0;
            // The new names are appended to the string table, so nothing may follow it.
            if (SymbolCount > 0 && StringTable + StringTableSize != Data.Length)
            {
                throw new InvalidDataException(Name + ": data after the string table");
            }
        }

        public int U16(int offset) { return Data[offset] | (Data[offset + 1] << 8); }

        public int I32(int offset) { return BitConverter.ToInt32(Data, offset); }

        public int Section(int index) { return SectionTable + index * SectionHeaderSize; }

        public int SectionSize(int index) { return I32(Section(index) + 16); }

        public int SectionData(int index) { return I32(Section(index) + 20); }

        public int Symbol(int index) { return SymbolTable + index * SymbolSize; }

        public string SectionName(int index)
        {
            string name = ShortName(Section(index));
            int offset;
            return name.StartsWith("/") && int.TryParse(name.Substring(1), out offset) ? StringAt(offset) : name;
        }

        public string SectionText(int index)
        {
            return Latin1.GetString(Data, SectionData(index), SectionSize(index));
        }

        public string SymbolName(int index)
        {
            int symbol = Symbol(index);
            return I32(symbol) == 0 ? StringAt(I32(symbol + 4)) : ShortName(symbol);
        }

        public int StorageClass(int index) { return Data[Symbol(index) + 16]; }

        public int SectionNumber(int index) { return (short) U16(Symbol(index) + 12); }

        public int Value(int index) { return I32(Symbol(index) + 8); }

        public int AuxCount(int index) { return Data[Symbol(index) + 17]; }

        private string ShortName(int offset)
        {
            int length = 0;
            while (length < 8 && Data[offset + length] != 0)
            {
                length++;
            }
            return Encoding.UTF8.GetString(Data, offset, length);
        }

        private string StringAt(int offset)
        {
            int start = StringTable + offset;
            int end = start;
            while (Data[end] != 0)
            {
                end++;
            }
            return Encoding.UTF8.GetString(Data, start, end - start);
        }
    }

    public sealed class HideResult
    {
        public List<byte[]> Objects = new List<byte[]>();
        public SortedDictionary<string, string> Renamed = new SortedDictionary<string, string>(StringComparer.Ordinal);
        public int Public;
        public int Exports;
        public int RuntimeDirectives;
        public int DebugExcluded;
    }

    public static class SymbolHider
    {
        public const string Prefix = "__dxfg_";

        // The API of the SDK, the COMDAT constants that the compiler names the same in every object, and the CRT inline
        // functions that the application's objects define too.
        private static readonly Regex Keep = new Regex(@"^(dxfg_|graal_|__real@|__xmm@|\?\?_C@|__local_stdio_)");
        private static readonly Regex AlternateName = new Regex("/alternatename:\"?([^\\s\"=]+)=([^\\s\"]+)\"?",
                RegexOptions.IgnoreCase);
        private static readonly Regex Include = new Regex("/include:\"?([^\\s\"]+)\"?", RegexOptions.IgnoreCase);
        // The application chooses the C runtime (/MT, /MTd, /MD, /MDd): the CRT shim binds to it at link time.
        private static readonly Regex Directive = new Regex(
                "(?<export>/export:\\S+)" +
                "|/defaultlib:\"?(msvcrt|msvcprt|libcmt)(\\.lib)?\"?(?=[\\s\\x00]|$)" +
                "|/failifmismatch:\"?(RuntimeLibrary|_ITERATOR_DEBUG_LEVEL)=[^\"\\s\\x00]*\"?",
                RegexOptions.IgnoreCase);

        private const int ExternalClass = 2;
        private const int WeakExternalClass = 105;
        private const int SearchAlias = 3;
        private const int StaticClass = 3;
        private const uint RelocationOverflow = 0x01000000;
        private const int CodeViewSignature = 4;
        private const int LfTypeServer = 0x0016;
        private const int LfTypeServer2 = 0x1515;

        // Prefixes every defined external symbol except the kept ones and both names of each /alternatename, in the
        // definitions and in the references of all objects; blanks the export and dynamic-CRT directives in place;
        // excludes the debug information that refers to an external PDB from the link.
        public static HideResult Hide(List<ArchiveMember> members)
        {
            List<CoffObject> objects = new List<CoffObject>();
            foreach (ArchiveMember member in members)
            {
                objects.Add(new CoffObject(member));
            }

            HashSet<string> defined = new HashSet<string>(StringComparer.Ordinal);
            HashSet<string> alternate = new HashSet<string>(StringComparer.Ordinal);
            foreach (CoffObject coff in objects)
            {
                for (int i = 0; i < coff.SymbolCount; i += 1 + coff.AuxCount(i))
                {
                    if (IsDefinedExternal(coff, i))
                    {
                        defined.Add(coff.SymbolName(i));
                    }
                }
                foreach (int section in Directives(coff))
                {
                    foreach (Match match in AlternateName.Matches(coff.SectionText(section)))
                    {
                        alternate.Add(match.Groups[1].Value);
                        alternate.Add(match.Groups[2].Value);
                    }
                }
            }

            HideResult result = new HideResult();
            result.Public = defined.Count;
            foreach (string name in defined)
            {
                if (!Keep.IsMatch(name) && !alternate.Contains(name))
                {
                    result.Renamed.Add(name, Prefix + name);
                }
            }
            foreach (CoffObject coff in objects)
            {
                result.Objects.Add(Rewrite(coff, result));
            }
            return result;
        }

        private static bool IsDefinedExternal(CoffObject coff, int index)
        {
            int storageClass = coff.StorageClass(index);
            if (storageClass == ExternalClass)
            {
                return coff.SectionNumber(index) != 0 || coff.Value(index) != 0;
            }
            // A weak external that searches only its alias defines the name, as llvm-nm counts it.
            return storageClass == WeakExternalClass && coff.AuxCount(index) > 0 &&
                   coff.I32(coff.Symbol(index + 1) + 4) == SearchAlias;
        }

        private static List<int> Directives(CoffObject coff)
        {
            List<int> sections = new List<int>();
            for (int i = 0; i < coff.SectionCount; i++)
            {
                if (coff.SectionName(i) == ".drectve")
                {
                    sections.Add(i);
                }
            }
            return sections;
        }

        private static byte[] Rewrite(CoffObject coff, HideResult result)
        {
            byte[] data = (byte[]) coff.Data.Clone();

            foreach (int section in Directives(coff))
            {
                string text = coff.SectionText(section);
                foreach (Match match in Include.Matches(text))
                {
                    if (result.Renamed.ContainsKey(match.Groups[1].Value))
                    {
                        throw new InvalidDataException(
                            coff.Name + ": /INCLUDE of the renamed " + match.Groups[1].Value);
                    }
                }
                int start = coff.SectionData(section);
                foreach (Match match in Directive.Matches(text))
                {
                    for (int i = 0; i < match.Length; i++)
                    {
                        data[start + match.Index + i] = (byte) ' ';
                    }
                    if (match.Groups["export"].Success)
                    {
                        result.Exports++;
                    }
                    else
                    {
                        result.RuntimeDirectives++;
                    }
                }
            }

            // The debug sections keep only the CodeView signature rather than being removed, so the numbers of the
            // sections and symbols stay the same (IMAGE_SCN_LNK_REMOVE on them warns LNK4078 for the different
            // attributes, a section without the signature warns LNK4209).
            if (HasTypeServer(coff))
            {
                for (int i = 0; i < coff.SectionCount; i++)
                {
                    string name = coff.SectionName(i);
                    int header = coff.Section(i);
                    if ((name == ".debug$S" || name == ".debug$T") && coff.SectionSize(i) >= 4 &&
                        coff.I32(coff.SectionData(i)) == CodeViewSignature)
                    {
                        PutI32(data, header + 16, 4);  // SizeOfRawData
                        Array.Clear(data, header + 24, 4);  // PointerToRelocations
                        Array.Clear(data, header + 32, 2);  // NumberOfRelocations
                        int characteristics = header + 36;
                        PutI32(data, characteristics, (int) (BitConverter.ToUInt32(data, characteristics) &
                                ~RelocationOverflow));
                        EmptySectionSymbol(coff, data, i);
                    }
                }
                result.DebugExcluded++;
            }

            // Each new name is appended once to the string table; the old offsets (long section names too) stay valid.
            MemoryStream strings = new MemoryStream();
            Dictionary<string, int> offsets = new Dictionary<string, int>(StringComparer.Ordinal);
            for (int i = 0; i < coff.SymbolCount; i += 1 + coff.AuxCount(i))
            {
                string renamed;
                if (!result.Renamed.TryGetValue(coff.SymbolName(i), out renamed))
                {
                    continue;
                }
                int offset;
                if (!offsets.TryGetValue(renamed, out offset))
                {
                    offset = coff.StringTableSize + (int) strings.Length;
                    byte[] bytes = Encoding.UTF8.GetBytes(renamed);
                    strings.Write(bytes, 0, bytes.Length);
                    strings.WriteByte(0);
                    offsets.Add(renamed, offset);
                }
                int symbol = coff.Symbol(i);
                PutI32(data, symbol, 0);
                PutI32(data, symbol + 4, offset);
            }
            if (strings.Length == 0)
            {
                return data;
            }
            byte[] rewritten = new byte[data.Length + strings.Length];
            Buffer.BlockCopy(data, 0, rewritten, 0, data.Length);
            strings.ToArray().CopyTo(rewritten, data.Length);
            PutI32(rewritten, coff.StringTable, coff.StringTableSize + (int) strings.Length);
            return rewritten;
        }

        private static void PutI32(byte[] data, int offset, int value)
        {
            Buffer.BlockCopy(BitConverter.GetBytes(value), 0, data, offset, 4);
        }

        // The section definition of the symbol of the section repeats its size, number of relocations and checksum.
        private static void EmptySectionSymbol(CoffObject coff, byte[] data, int section)
        {
            for (int i = 0; i < coff.SymbolCount; i += 1 + coff.AuxCount(i))
            {
                if (coff.SectionNumber(i) == section + 1 && coff.StorageClass(i) == StaticClass && coff.Value(i) == 0 &&
                    coff.AuxCount(i) > 0)
                {
                    int aux = coff.Symbol(i + 1);
                    PutI32(data, aux, 4);  // Length
                    Array.Clear(data, aux + 4, 2);  // NumberOfRelocations
                    Array.Clear(data, aux + 8, 4);  // CheckSum
                }
            }
        }

        // The types of the object are in a PDB of its build (LF_TYPESERVER, LF_TYPESERVER2), which is not shipped: the
        // linker warns LNK4099 for it in any link with /DEBUG.
        private static bool HasTypeServer(CoffObject coff)
        {
            for (int i = 0; i < coff.SectionCount; i++)
            {
                int start = coff.SectionData(i);
                if (coff.SectionName(i) == ".debug$T" && coff.SectionSize(i) >= 8 &&
                    coff.I32(start) == CodeViewSignature)
                {
                    int kind = coff.U16(start + 6);
                    return kind == LfTypeServer2 || kind == LfTypeServer;
                }
            }
            return false;
        }
    }
}
