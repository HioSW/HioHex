# HioHex

**HioHex** is a lightweight hex editor built entirely with Java. It provides a straightforward interface for viewing, editing, and analyzing binary files without requiring external dependencies.

Designed as a standalone desktop application, HioHex combines essential hex-editing tools with convenient file inspection and data conversion features.

## Features

- **Hex Editor** — View and edit binary data in hexadecimal and ASCII formats.
- **File Operations** — Create, open, save, and save files under a different name.
- **Byte Editing** — Modify, insert, and delete bytes.
- **Undo & Redo** — Revert and restore editing operations.
- **Hex Search** — Find byte sequences using hexadecimal patterns.
- **Offset Navigation** — Jump directly to a specific file offset.
- **Hash Calculator** — Calculate MD5 and SHA-256 checksums.
- **Number Converter** — Convert between decimal, hexadecimal, binary, and octal.
- **Byte Inspector** — Examine individual bytes and their representations.
- **File Statistics** — View basic binary data statistics.
- **Clipboard Support** — Copy hexadecimal and ASCII data.
- **Drag & Drop** — Open files by dropping them into the application.
- **Keyboard Shortcuts** — Access frequently used operations quickly.
- **Native Look and Feel** — Uses the system's native Java Swing appearance.

## Requirements

- **Java Development Kit (JDK): 17 or newer**
- Java Swing (included in the JDK)
- No external libraries or third-party dependencies.

### Supported Platforms

- macOS
- Windows
- Linux

HioHex is designed for Java 17+ and uses standard Java libraries.

## Project Structure

The project is distributed as a standalone Java source file:

```text
HioHex/
└── HioHex.java
```

## Build

You can compile HioHex directly using the Java compiler.

### 1. Check Java Installation

```bash
java -version
javac -version
```

Make sure both commands report Java 17 or newer.

### 2. Compile

Navigate to the directory containing `HioHex.java`:

```bash
cd HioHex
```

Compile the application:

```bash
javac HioHex.java
```

This generates the compiled class files required to run the application.

## Run

After compilation, start HioHex with:

```bash
java HioHex
```

### Open a File Directly

You can also specify a file path as a command-line argument:

```bash
java HioHex "/path/to/file.bin"
```

HioHex will attempt to open the specified file automatically.

## macOS

Example commands for macOS:

```bash
cd ~/Desktop/HioHex
javac HioHex.java
java HioHex
```

## Windows

Open Command Prompt or PowerShell:

```powershell
cd C:\HioHex
javac HioHex.java
java HioHex
```

## Linux

Open a terminal:

```bash
cd ~/HioHex
javac HioHex.java
java HioHex
```

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| Ctrl + O | Open file |
| Ctrl + S | Save file |
| Ctrl + Z | Undo |
| Ctrl + Y | Redo |
| Ctrl + F | Find HEX pattern |
| Ctrl + G | Go to offset |

On macOS, the application uses the same key bindings as defined in the source code.

## Technical Details

| Component | Technology |
|---|---|
| Language | Java |
| Minimum Java version | Java 17 |
| GUI Framework | Java Swing |
| Dependencies | None |
| Application Type | Desktop |
| Source Structure | Single Java file |

## Limitations

- Files are loaded entirely into memory.
- Very large files may require substantial RAM.
- Performance depends on the available system memory and file size.
- The current implementation is intended for general-purpose binary editing rather than extremely large disk images.

## Project Goals

HioHex is an experimental standalone utility focused on exploring what can be achieved with pure Java.

The project aims to provide practical binary-file editing functionality while keeping the implementation self-contained, lightweight, and accessible.

## License

See the repository's license file for licensing information.
