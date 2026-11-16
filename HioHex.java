import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;

public class HioHex extends JFrame {

    private static final int BYTES_PER_ROW = 16;

    private byte[] data = new byte[0];
    private File currentFile;
    private boolean modified = false;

    private JTable table;
    private HexTableModel model;
    private JTextArea inspector;
    private JLabel statusLabel;
    private JLabel fileLabel;

    private final UndoManager undoManager = new UndoManager();

    private boolean updating = false;

    public HioHex() {
        super("HioHex");

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1150, 720);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception ignored) {
        }

        initializeUI();
        initializeKeyboard();
        initializeDragAndDrop();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        updateTitle();
        updateStatus();
    }

    // =========================================================
    // UI
    // =========================================================

    private void initializeUI() {
        setLayout(new BorderLayout());

        setJMenuBar(createMenuBar());

        JPanel top = new JPanel(new BorderLayout());
        top.add(createToolbar(), BorderLayout.NORTH);

        fileLabel = new JLabel("  No file opened");
        fileLabel.setBorder(new EmptyBorder(7, 10, 7, 10));

        top.add(fileLabel, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        model = new HexTableModel();

        table = new JTable(model);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        table.setRowHeight(23);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(
            ListSelectionModel.SINGLE_INTERVAL_SELECTION
        );
        table.setFillsViewportHeight(true);

        table.getTableHeader().setReorderingAllowed(false);

        table.setDefaultRenderer(
            Object.class,
            new HexCellRenderer()
        );

        table.getColumnModel().getColumn(0).setPreferredWidth(95);

        for (int i = 1; i <= BYTES_PER_ROW; i++) {
            table.getColumnModel()
                .getColumn(i)
                .setPreferredWidth(38);
        }

        table.getColumnModel()
            .getColumn(17)
            .setPreferredWidth(175);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateInspector();
            }
        });

        table.getColumnModel()
            .getSelectionModel()
            .addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    updateInspector();
                }
            });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(5, 8, 5, 8));

        inspector = new JTextArea();
        inspector.setEditable(false);
        inspector.setFont(
            new Font(Font.MONOSPACED, Font.PLAIN, 12)
        );
        inspector.setRows(7);
        inspector.setText("Select a byte to inspect it.");

        JPanel inspectorPanel = new JPanel(new BorderLayout());
        inspectorPanel.setBorder(
            BorderFactory.createTitledBorder("Byte Inspector")
        );
        inspectorPanel.add(
            new JScrollPane(inspector),
            BorderLayout.CENTER
        );

        JSplitPane split = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            scroll,
            inspectorPanel
        );

        split.setResizeWeight(0.78);
        split.setDividerLocation(510);

        add(split, BorderLayout.CENTER);

        statusLabel = new JLabel(" Ready");
        statusLabel.setBorder(
            new EmptyBorder(6, 10, 6, 10)
        );

        add(statusLabel, BorderLayout.SOUTH);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");

        file.add(menuItem("New", KeyStroke.getKeyStroke("control N"),
            this::newFile));

        file.add(menuItem("Open...", KeyStroke.getKeyStroke("control O"),
            this::openFile));

        file.add(menuItem("Save", KeyStroke.getKeyStroke("control S"),
            this::saveFile));

        file.add(menuItem("Save As...", null, this::saveAs));

        file.addSeparator();

        file.add(menuItem("File Information", null,
            this::showFileInfo));

        file.addSeparator();

        file.add(menuItem("Exit", null, this::exitApplication));

        JMenu edit = new JMenu("Edit");

        edit.add(menuItem("Undo", KeyStroke.getKeyStroke("control Z"),
            this::undo));

        edit.add(menuItem("Redo", KeyStroke.getKeyStroke("control Y"),
            this::redo));

        edit.addSeparator();

        edit.add(menuItem("Insert Bytes...", null,
            this::insertBytes));

        edit.add(menuItem("Delete Selected Bytes", null,
            this::deleteBytes));

        edit.addSeparator();

        edit.add(menuItem("Find HEX...", KeyStroke.getKeyStroke("control F"),
            this::findBytes));

        edit.add(menuItem("Go To Offset...", KeyStroke.getKeyStroke("control G"),
            this::goToOffset));

        edit.addSeparator();

        edit.add(menuItem("Copy HEX", null, this::copyHex));
        edit.add(menuItem("Copy ASCII", null, this::copyAscii));

        JMenu tools = new JMenu("Tools");

        tools.add(menuItem("Calculate Hashes", null,
            this::showHashes));

        tools.add(menuItem("Number Converter", null,
            this::numberConverter));

        tools.add(menuItem("File Statistics", null,
            this::showStatistics));

        bar.add(file);
        bar.add(edit);
        bar.add(tools);

        return bar;
    }

    private JMenuItem menuItem(
        String title,
        KeyStroke shortcut,
        Runnable action
    ) {
        JMenuItem item = new JMenuItem(title);

        if (shortcut != null) {
            item.setAccelerator(shortcut);
        }

        item.addActionListener(e -> action.run());

        return item;
    }

    private JPanel createToolbar() {
        JPanel panel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 5, 6)
        );

        panel.setBorder(
            new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY)
        );

        panel.add(smallButton("New", this::newFile));
        panel.add(smallButton("Open", this::openFile));
        panel.add(smallButton("Save", this::saveFile));

        panel.add(new JSeparator(SwingConstants.VERTICAL));

        panel.add(smallButton("Undo", this::undo));
        panel.add(smallButton("Redo", this::redo));

        panel.add(new JSeparator(SwingConstants.VERTICAL));

        panel.add(smallButton("Find", this::findBytes));
        panel.add(smallButton("Go To", this::goToOffset));

        panel.add(new JSeparator(SwingConstants.VERTICAL));

        panel.add(smallButton("Insert", this::insertBytes));
        panel.add(smallButton("Delete", this::deleteBytes));

        return panel;
    }

    private JButton smallButton(String title, Runnable action) {
        JButton button = new JButton(title);

        button.setFocusable(false);
        button.setMargin(new Insets(5, 11, 5, 11));

        button.addActionListener(e -> action.run());

        return button;
    }

    // =========================================================
    // TABLE
    // =========================================================

    private class HexTableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return (data.length + BYTES_PER_ROW - 1)
                / BYTES_PER_ROW;
        }

        @Override
        public int getColumnCount() {
            return 18;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return "Offset";
            }

            if (column == 17) {
                return "ASCII";
            }

            return String.format("%02X", column - 1);
        }

        @Override
        public Object getValueAt(int row, int column) {
            int offset = row * BYTES_PER_ROW;

            if (column == 0) {
                return String.format("%08X", offset);
            }

            if (column == 17) {
                StringBuilder ascii = new StringBuilder();

                for (int i = 0; i < BYTES_PER_ROW; i++) {
                    int index = offset + i;

                    if (index >= data.length) {
                        break;
                    }

                    int value = data[index] & 0xFF;

                    ascii.append(
                        value >= 32 && value <= 126
                            ? (char) value
                            : '.'
                    );
                }

                return ascii.toString();
            }

            int index = offset + column - 1;

            if (index >= data.length) {
                return "";
            }

            return String.format("%02X", data[index] & 0xFF);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            if (column < 1 || column > 17) {
                return false;
            }

            int offset = row * BYTES_PER_ROW;

            if (column == 17) {
                return offset < data.length;
            }

            return offset + column - 1 < data.length;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (updating || value == null) {
                return;
            }

            int offset = row * BYTES_PER_ROW;

            if (column == 17) {
                String text = value.toString();

                int count = Math.min(
                    text.length(),
                    Math.min(BYTES_PER_ROW, data.length - offset)
                );

                if (count <= 0) {
                    return;
                }

                byte[] replacement = Arrays.copyOfRange(
                    data,
                    offset,
                    offset + count
                );

                for (int i = 0; i < count; i++) {
                    replacement[i] = (byte) text.charAt(i);
                }

                replaceRange(offset, count, replacement);
                return;
            }

            int index = offset + column - 1;

            if (index < 0 || index >= data.length) {
                return;
            }

            String hex = value.toString()
                .trim()
                .replace("0x", "")
                .replace("0X", "");

            try {
                if (hex.isEmpty() || hex.length() > 2) {
                    return;
                }

                int parsed = Integer.parseInt(hex, 16);

                if (parsed < 0 || parsed > 255) {
                    return;
                }

                changeByte(index, (byte) parsed);

            } catch (NumberFormatException ignored) {
                JOptionPane.showMessageDialog(
                    HioHex.this,
                    "Invalid HEX byte.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private class HexCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean selected,
            boolean focused,
            int row,
            int column
        ) {
            super.getTableCellRendererComponent(
                table,
                value,
                selected,
                focused,
                row,
                column
            );

            setHorizontalAlignment(
                column == 0
                    ? SwingConstants.LEFT
                    : SwingConstants.CENTER
            );

            if (!selected && column == 0) {
                setForeground(UIManager.getColor("Label.disabledForeground"));
            }

            if (column == 17) {
                setFont(
                    new Font(Font.MONOSPACED, Font.PLAIN, 12)
                );
            }

            return this;
        }
    }

    // =========================================================
    // FILE OPERATIONS
    // =========================================================

    private void newFile() {
        if (!confirmSave()) {
            return;
        }

        data = new byte[0];
        currentFile = null;
        modified = false;

        undoManager.discardAllEdits();

        refreshTable();
        updateTitle();
        updateStatus();
    }

    private void openFile() {
        if (!confirmSave()) {
            return;
        }

        JFileChooser chooser = new JFileChooser();

        if (currentFile != null) {
            chooser.setCurrentDirectory(
                currentFile.getParentFile()
            );
        }

        if (chooser.showOpenDialog(this)
            != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selected = chooser.getSelectedFile();

        try {
            data = Files.readAllBytes(selected.toPath());

            currentFile = selected;
            modified = false;

            undoManager.discardAllEdits();

            refreshTable();
            updateTitle();
            updateStatus();

        } catch (IOException ex) {
            showError("Cannot open file:\n" + ex.getMessage());
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            saveAs();
            return;
        }

        try {
            Files.write(currentFile.toPath(), data);

            modified = false;

            updateTitle();
            updateStatus();

        } catch (IOException ex) {
            showError("Cannot save file:\n" + ex.getMessage());
        }
    }

    private void saveAs() {
        JFileChooser chooser = new JFileChooser();

        if (currentFile != null) {
            chooser.setSelectedFile(currentFile);
        }

        if (chooser.showSaveDialog(this)
            != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selected = chooser.getSelectedFile();

        if (selected.exists()) {
            int result = JOptionPane.showConfirmDialog(
                this,
                "The file already exists. Replace it?",
                "Confirm Save",
                JOptionPane.YES_NO_OPTION
            );

            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        currentFile = selected;
        saveFile();
    }

    private boolean confirmSave() {
        if (!modified) {
            return true;
        }

        int result = JOptionPane.showConfirmDialog(
            this,
            "The current file has unsaved changes.\nSave them?",
            "Unsaved Changes",
            JOptionPane.YES_NO_CANCEL_OPTION
        );

        if (result == JOptionPane.CANCEL_OPTION
            || result == JOptionPane.CLOSED_OPTION) {
            return false;
        }

        if (result == JOptionPane.YES_OPTION) {
            saveFile();
            return !modified;
        }

        return true;
    }

    private void exitApplication() {
        if (confirmSave()) {
            dispose();
            System.exit(0);
        }
    }

    // =========================================================
    // EDITING
    // =========================================================

    private void changeByte(int index, byte value) {
        if (index < 0 || index >= data.length) {
            return;
        }

        byte[] before = data.clone();

        data[index] = value;

        registerUndo(before);
        modified = true;

        refreshTable();
        selectOffset(index);
        updateTitle();
        updateStatus();
    }

    private void replaceRange(
        int offset,
        int length,
        byte[] replacement
    ) {
        if (offset < 0 || length < 0
            || offset + length > data.length) {
            return;
        }

        byte[] before = data.clone();

        byte[] result = new byte[
            data.length - length + replacement.length
        ];

        System.arraycopy(data, 0, result, 0, offset);

        System.arraycopy(
            replacement,
            0,
            result,
            offset,
            replacement.length
        );

        System.arraycopy(
            data,
            offset + length,
            result,
            offset + replacement.length,
            data.length - offset - length
        );

        data = result;

        registerUndo(before);
        modified = true;

        refreshTable();
        selectOffset(Math.min(offset, Math.max(0, data.length - 1)));
        updateTitle();
        updateStatus();
    }

    private void insertBytes() {
        JTextField input = new JTextField();

        int result = JOptionPane.showConfirmDialog(
            this,
            new Object[] {
                "HEX bytes (example: DE AD BE EF):",
                input
            },
            "Insert Bytes",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        byte[] bytes;

        try {
            bytes = parseHex(input.getText());
        } catch (Exception ex) {
            showError("Invalid HEX input.");
            return;
        }

        if (bytes.length == 0) {
            return;
        }

        int offset = getSelectedOffset();

        if (offset < 0) {
            offset = data.length;
        }

        offset = Math.min(offset, data.length);

        byte[] before = data.clone();

        byte[] resultData = new byte[
            data.length + bytes.length
        ];

        System.arraycopy(data, 0, resultData, 0, offset);
        System.arraycopy(
            bytes, 0, resultData, offset, bytes.length
        );

        System.arraycopy(
            data,
            offset,
            resultData,
            offset + bytes.length,
            data.length - offset
        );

        data = resultData;

        registerUndo(before);
        modified = true;

        refreshTable();
        selectOffset(offset);
        updateTitle();
        updateStatus();
    }

    private void deleteBytes() {
        int offset = getSelectedOffset();

        if (offset < 0 || offset >= data.length) {
            return;
        }

        int row = table.getSelectedRow();
        int column = table.getSelectedColumn();

        int length = 1;

        if (column == 17) {
            int rowStart = row * BYTES_PER_ROW;
            length = Math.min(
                BYTES_PER_ROW,
                data.length - rowStart
            );
            offset = rowStart;
        }

        int result = JOptionPane.showConfirmDialog(
            this,
            "Delete " + length + " byte(s) at offset 0x"
                + Integer.toHexString(offset).toUpperCase() + "?",
            "Delete Bytes",
            JOptionPane.YES_NO_OPTION
        );

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        byte[] before = data.clone();

        byte[] resultData = new byte[data.length - length];

        System.arraycopy(data, 0, resultData, 0, offset);

        System.arraycopy(
            data,
            offset + length,
            resultData,
            offset,
            data.length - offset - length
        );

        data = resultData;

        registerUndo(before);
        modified = true;

        refreshTable();

        if (data.length > 0) {
            selectOffset(Math.min(offset, data.length - 1));
        }

        updateTitle();
        updateStatus();
    }

    private void registerUndo(byte[] before) {
        byte[] after = data.clone();

        undoManager.addEdit(new AbstractUndoableEdit() {
            @Override
            public void undo() {
                super.undo();
                data = before.clone();
                modified = true;
                refreshTable();
                updateTitle();
                updateStatus();
            }

            @Override
            public void redo() {
                super.redo();
                data = after.clone();
                modified = true;
                refreshTable();
                updateTitle();
                updateStatus();
            }
        });
    }

    private void undo() {
        if (!undoManager.canUndo()) {
            return;
        }

        undoManager.undo();
    }

    private void redo() {
        if (!undoManager.canRedo()) {
            return;
        }

        undoManager.redo();
    }

    // =========================================================
    // SEARCH / NAVIGATION
    // =========================================================

    private void findBytes() {
        JTextField input = new JTextField();

        int result = JOptionPane.showConfirmDialog(
            this,
            new Object[] {
                "HEX pattern (example: 4D 5A):",
                input
            },
            "Find HEX",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        byte[] pattern;

        try {
            pattern = parseHex(input.getText());
        } catch (Exception ex) {
            showError("Invalid HEX pattern.");
            return;
        }

        if (pattern.length == 0) {
            return;
        }

        int start = getSelectedOffset() + 1;
        int found = findPattern(pattern, start);

        if (found < 0 && start > 0) {
            found = findPattern(pattern, 0);
        }

        if (found < 0) {
            JOptionPane.showMessageDialog(
                this,
                "Pattern not found.",
                "Find",
                JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            selectOffset(found);
            statusLabel.setText(
                " Found at offset 0x"
                    + Integer.toHexString(found).toUpperCase()
            );
        }
    }

    private int findPattern(byte[] pattern, int start) {
        if (pattern.length > data.length) {
            return -1;
        }

        for (int i = Math.max(0, start);
             i <= data.length - pattern.length;
             i++) {

            boolean match = true;

            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return i;
            }
        }

        return -1;
    }

    private void goToOffset() {
        JTextField input = new JTextField("0");

        int result = JOptionPane.showConfirmDialog(
            this,
            new Object[] {
                "Offset (decimal or 0xHEX):",
                input
            },
            "Go To Offset",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String value = input.getText().trim();

            long offset;

            if (value.startsWith("0x")
                || value.startsWith("0X")) {
                offset = Long.parseLong(value.substring(2), 16);
            } else {
                offset = Long.parseLong(value);
            }

            if (offset < 0 || offset >= data.length) {
                showError("Offset is outside the file.");
                return;
            }

            selectOffset((int) offset);

        } catch (NumberFormatException ex) {
            showError("Invalid offset.");
        }
    }

    private void selectOffset(int offset) {
        if (offset < 0 || offset >= data.length) {
            return;
        }

        int row = offset / BYTES_PER_ROW;
        int column = offset % BYTES_PER_ROW + 1;

        table.changeSelection(row, column, false, false);
        table.scrollRectToVisible(
            table.getCellRect(row, column, true)
        );

        updateInspector();
    }

    private int getSelectedOffset() {
        int row = table.getSelectedRow();
        int column = table.getSelectedColumn();

        if (row < 0) {
            return -1;
        }

        int base = row * BYTES_PER_ROW;

        if (column == 17) {
            return base;
        }

        if (column <= 0) {
            return base;
        }

        return base + column - 1;
    }

    // =========================================================
    // CLIPBOARD
    // =========================================================

    private void copyHex() {
        int offset = getSelectedOffset();

        if (offset < 0 || offset >= data.length) {
            return;
        }

        int start = offset;
        int end = offset + 1;

        if (table.getSelectedColumn() == 17) {
            start = (offset / BYTES_PER_ROW) * BYTES_PER_ROW;
            end = Math.min(start + BYTES_PER_ROW, data.length);
        }

        StringBuilder output = new StringBuilder();

        for (int i = start; i < end; i++) {
            output.append(
                String.format("%02X ", data[i] & 0xFF)
            );
        }

        copyToClipboard(output.toString().trim());
    }

    private void copyAscii() {
        int offset = getSelectedOffset();

        if (offset < 0 || offset >= data.length) {
            return;
        }

        int start = offset;
        int end = offset + 1;

        if (table.getSelectedColumn() == 17) {
            start = (offset / BYTES_PER_ROW) * BYTES_PER_ROW;
            end = Math.min(start + BYTES_PER_ROW, data.length);
        }

        StringBuilder output = new StringBuilder();

        for (int i = start; i < end; i++) {
            int value = data[i] & 0xFF;

            output.append(
                value >= 32 && value <= 126
                    ? (char) value
                    : '.'
            );
        }

        copyToClipboard(output.toString());
    }

    private void copyToClipboard(String value) {
        Toolkit.getDefaultToolkit()
            .getSystemClipboard()
            .setContents(
                new StringSelection(value),
                null
            );
    }

    // =========================================================
    // TOOLS
    // =========================================================

    private void showHashes() {
        if (data.length == 0) {
            JOptionPane.showMessageDialog(
                this,
                "No data to calculate.",
                "Hashes",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        byte[] snapshot = data.clone();

        JDialog dialog = new JDialog(
            this,
            "File Hashes",
            true
        );

        JTextArea output = new JTextArea();
        output.setEditable(false);
        output.setFont(
            new Font(Font.MONOSPACED, Font.PLAIN, 13)
        );

        output.setText("Calculating hashes...");

        dialog.add(new JScrollPane(output));
        dialog.setSize(650, 230);
        dialog.setLocationRelativeTo(this);

        SwingWorker<String, Void> worker =
            new SwingWorker<>() {

            @Override
            protected String doInBackground() throws Exception {
                StringBuilder result = new StringBuilder();

                result.append("File size: ")
                    .append(snapshot.length)
                    .append(" bytes\n\n");

                result.append("MD5:\n")
                    .append(hash("MD5", snapshot))
                    .append("\n\n");

                result.append("SHA-256:\n")
                    .append(hash("SHA-256", snapshot))
                    .append("\n");

                return result.toString();
            }

            @Override
            protected void done() {
                try {
                    output.setText(get());
                    output.setCaretPosition(0);
                } catch (Exception ex) {
                    output.setText("Hash calculation failed: "
                        + ex.getMessage());
                }
            }
        };

        worker.execute();
        dialog.setVisible(true);
    }

    private String hash(String algorithm, byte[] bytes)
        throws Exception {

        MessageDigest digest =
            MessageDigest.getInstance(algorithm);

        byte[] result = digest.digest(bytes);

        StringBuilder hex = new StringBuilder();

        for (byte b : result) {
            hex.append(String.format("%02x", b & 0xFF));
        }

        return hex.toString();
    }

    private void numberConverter() {
        JTextField input = new JTextField("255");

        JTextArea output = new JTextArea(6, 35);
        output.setEditable(false);
        output.setFont(
            new Font(Font.MONOSPACED, Font.PLAIN, 13)
        );

        JButton convert = smallButton("Convert", () -> {
            try {
                String value = input.getText().trim();

                long number;

                if (value.startsWith("0x")
                    || value.startsWith("0X")) {
                    number = Long.parseUnsignedLong(
                        value.substring(2), 16
                    );
                } else if (value.startsWith("0b")
                    || value.startsWith("0B")) {
                    number = Long.parseUnsignedLong(
                        value.substring(2), 2
                    );
                } else {
                    number = Long.parseLong(value);
                }

                output.setText(
                    "Decimal: " + number + "\n"
                    + "Hexadecimal: 0x"
                    + Long.toHexString(number).toUpperCase()
                    + "\n"
                    + "Binary: "
                    + Long.toBinaryString(number)
                    + "\n"
                    + "Octal: "
                    + Long.toOctalString(number)
                );

            } catch (NumberFormatException ex) {
                output.setText("Invalid number.");
            }
        });

        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(new JLabel("Number:"), BorderLayout.WEST);
        top.add(input, BorderLayout.CENTER);
        top.add(convert, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(output), BorderLayout.CENTER);

        JOptionPane.showMessageDialog(
            this,
            panel,
            "Number Converter",
            JOptionPane.PLAIN_MESSAGE
        );
    }

    private void showFileInfo() {
        if (currentFile == null) {
            JOptionPane.showMessageDialog(
                this,
                "No file is currently opened."
            );
            return;
        }

        try {
            String info =
                "Name: " + currentFile.getName() + "\n"
                + "Path: " + currentFile.getAbsolutePath() + "\n"
                + "Size: " + data.length + " bytes\n"
                + "Modified: "
                + new Date(currentFile.lastModified()) + "\n"
                + "Extension: " + getExtension(currentFile);

            JOptionPane.showMessageDialog(
                this,
                info,
                "File Information",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void showStatistics() {
        int zeros = 0;
        int printable = 0;
        int high = 0;

        for (byte b : data) {
            int value = b & 0xFF;

            if (value == 0) {
                zeros++;
            }

            if (value >= 32 && value <= 126) {
                printable++;
            }

            if (value >= 128) {
                high++;
            }
        }

        String info =
            "Total bytes: " + data.length + "\n"
            + "Zero bytes: " + zeros + "\n"
            + "Printable ASCII: " + printable + "\n"
            + "Bytes >= 128: " + high + "\n"
            + "Rows: "
            + ((data.length + BYTES_PER_ROW - 1)
                / BYTES_PER_ROW);

        JOptionPane.showMessageDialog(
            this,
            info,
            "File Statistics",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    // =========================================================
    // INSPECTOR
    // =========================================================

    private void updateInspector() {
        if (inspector == null) {
            return;
        }

        int offset = getSelectedOffset();

        if (offset < 0 || offset >= data.length) {
            inspector.setText("Select a byte to inspect it.");
            return;
        }

        int value = data[offset] & 0xFF;

        String binary = String.format(
            "%8s",
            Integer.toBinaryString(value)
        ).replace(' ', '0');

        String ascii =
            value >= 32 && value <= 126
                ? Character.toString((char) value)
                : ".";

        inspector.setText(
            "Offset:       0x"
                + String.format("%08X", offset) + "\n"
            + "Decimal:      " + value + "\n"
            + "Hexadecimal:  0x"
                + String.format("%02X", value) + "\n"
            + "Binary:       " + binary + "\n"
            + "ASCII:        " + ascii + "\n"
            + "Signed byte:  " + data[offset]
        );
    }

    // =========================================================
    // KEYBOARD / DRAG & DROP
    // =========================================================

    private void initializeKeyboard() {
        InputMap input = getRootPane().getInputMap(
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        ActionMap actions = getRootPane().getActionMap();

        bind(input, actions, "control O", "open", this::openFile);
        bind(input, actions, "control S", "save", this::saveFile);
        bind(input, actions, "control Z", "undo", this::undo);
        bind(input, actions, "control Y", "redo", this::redo);
        bind(input, actions, "control F", "find", this::findBytes);
        bind(input, actions, "control G", "goto", this::goToOffset);
    }

    private void bind(
        InputMap input,
        ActionMap actions,
        String key,
        String name,
        Runnable action
    ) {
        input.put(KeyStroke.getKeyStroke(key), name);

        actions.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void initializeDragAndDrop() {
        setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(
                    DataFlavor.javaFileListFlavor
                );
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                try {
                    List<File> files = (List<File>)
                        support.getTransferable().getTransferData(
                            DataFlavor.javaFileListFlavor
                        );

                    if (files.isEmpty()) {
                        return false;
                    }

                    if (!confirmSave()) {
                        return false;
                    }

                    File file = files.get(0);

                    data = Files.readAllBytes(file.toPath());
                    currentFile = file;
                    modified = false;

                    undoManager.discardAllEdits();

                    refreshTable();
                    updateTitle();
                    updateStatus();

                    return true;

                } catch (Exception ex) {
                    showError(
                        "Cannot open dropped file:\n"
                        + ex.getMessage()
                    );
                    return false;
                }
            }
        });
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private byte[] parseHex(String input) {
        String normalized = input
            .replace("0x", "")
            .replace("0X", "")
            .replaceAll("[\\s,:;_-]", "");

        if (normalized.isEmpty()) {
            return new byte[0];
        }

        if (normalized.length() % 2 != 0) {
            throw new IllegalArgumentException(
                "HEX input must contain pairs of digits."
            );
        }

        byte[] result = new byte[normalized.length() / 2];

        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(
                normalized.substring(i * 2, i * 2 + 2),
                16
            );
        }

        return result;
    }

    private String getExtension(File file) {
        String name = file.getName();

        int index = name.lastIndexOf('.');

        return index >= 0
            ? name.substring(index + 1)
            : "(none)";
    }

    private void refreshTable() {
        if (model != null) {
            model.fireTableDataChanged();
        }

        updateInspector();
    }

    private void updateTitle() {
        String name = currentFile == null
            ? "Untitled"
            : currentFile.getName();

        setTitle(
            "HioHex - " + name + (modified ? " *" : "")
        );

        if (fileLabel != null) {
            fileLabel.setText(
                "  " + (
                    currentFile == null
                        ? "Untitled"
                        : currentFile.getAbsolutePath()
                )
            );
        }
    }

    private void updateStatus() {
        if (statusLabel == null) {
            return;
        }

        statusLabel.setText(
            " " + data.length + " bytes  |  "
            + ((data.length + BYTES_PER_ROW - 1)
                / BYTES_PER_ROW)
            + " rows  |  "
            + (modified ? "Modified" : "Saved")
        );
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "HioHex Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HioHex app = new HioHex();
            app.setVisible(true);

            if (args.length > 0) {
                File file = new File(args[0]);

                if (file.isFile()) {
                    try {
                        app.data = Files.readAllBytes(file.toPath());
                        app.currentFile = file;
                        app.refreshTable();
                        app.updateTitle();
                        app.updateStatus();
                    } catch (IOException ex) {
                        app.showError(
                            "Cannot open file:\n" + ex.getMessage()
                        );
                    }
                }
            }
        });
    }
}
