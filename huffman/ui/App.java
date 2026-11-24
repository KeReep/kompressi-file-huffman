package huffman.ui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import huffman.service.FileCompressor;
import huffman.service.Hasil;
import huffman.service.HufInspector;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class App extends JFrame {

    // ---------- Palet Warna ----------
    private static final Color BG_MAIN      = new Color(0xF4F4F5);
    private static final Color BG_CARD      = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(0xD4D4D8);
    private static final Color TEXT_PRIMARY = new Color(0x18181B);
    private static final Color TEXT_MUTED   = new Color(0x71717A);
    private static final Color ACCENT_DARK  = new Color(0x18181B); 
    private static final Color ACCENT_SLATE = new Color(0x3F3F46);
    private static final Color ACCENT_GREEN = new Color(0x16A34A);
    private static final Color ACCENT_AMBER = new Color(0xD97706);
    private static final Color ACCENT_RED   = new Color(0xDC2626);

    // ---------- Komponen Kiri ----------
    private final JLabel lblDropJudul   = new JLabel("Tarik & Lepas File ke Sini", SwingConstants.CENTER);
    private final JLabel lblDropSub     = new JLabel("atau klik tombol \"Pilih File\" di bawah", SwingConstants.CENTER);
    private final JLabel lblInfoNama    = new JLabel("-");
    private final JLabel lblInfoUkuran  = new JLabel("-");
    private final JLabel lblInfoTipe    = new JLabel("-");

    private final JButton btnPilih      = new JButton("Pilih File...");
    private final JButton btnKompres    = new JButton("Kompres (.huf)");
    private final JButton btnDekompres  = new JButton("Dekompres");
    private final JProgressBar bar      = new JProgressBar();
    private final JTextArea log         = new JTextArea();
    private File terpilih;

    // ---------- Komponen Kanan: Cards ----------
    private final KpiCard cardAwal      = new KpiCard("UKURAN AWAL", "-", "Menunggu file", TEXT_PRIMARY);
    private final KpiCard cardAkhir     = new KpiCard("UKURAN AKHIR", "-", "Belum diproses", TEXT_PRIMARY);
    private final KpiCard cardHemat     = new KpiCard("PENGHEMATAN / RASIO", "-", "-", ACCENT_GREEN);
    private final KpiCard cardEfisiensi = new KpiCard("EFISIENSI HUFFMAN", "-", "Entropi vs Rata-rata bit", ACCENT_DARK);

    // ---------- Komponen Kanan: Tabel & Grafik ----------
    private final DefaultTableModel ringkasanModel = new DefaultTableModel(
            new Object[]{"Parameter Teknis", "Keterangan / Nilai"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable ringkasanTable = new JTable(ringkasanModel);
    private final BreakdownBarPanel barPanel = new BreakdownBarPanel();

    private final DefaultTableModel kodeModel = new DefaultTableModel(
            new Object[]{"Byte", "Simbol", "Frekuensi", "Peluang (%)", "Kode Huffman", "Panjang Bit", "Total Bit"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) {
            switch (c) {
                case 0: case 5: return Integer.class;
                case 2: case 6: return Long.class;
                case 3: return Double.class;
                default: return String.class;
            }
        }
    };
    private final JTable kodeTable = new JTable(kodeModel);
    private final JTextArea biner = new JTextArea();
    private final JTabbedPane tabs = new JTabbedPane();

    public App() {
        super("Program Kompressi File Simple");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BG_MAIN);

        setupStyling();

        // ================= PANEL KIRI (Kontrol & Drop Zone) =================
        JPanel kiri = new JPanel(new BorderLayout(0, 12));
        kiri.setBackground(BG_MAIN);
        kiri.setBorder(new EmptyBorder(14, 14, 14, 10));

        // 1. Kartu Drop Zone
        JPanel dropCard = new JPanel(new BorderLayout(0, 6));
        dropCard.setBackground(BG_CARD);
        dropCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ACCENT_SLATE, 1, true),
                new EmptyBorder(16, 12, 16, 12)
        ));
        lblDropJudul.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblDropJudul.setForeground(ACCENT_DARK);
        lblDropSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblDropSub.setForeground(TEXT_MUTED);

        JPanel dropText = new JPanel(new GridLayout(2, 1, 0, 4));
        dropText.setOpaque(false);
        dropText.add(lblDropJudul);
        dropText.add(lblDropSub);

        btnPilih.setFocusPainted(false);
        btnPilih.setFont(new Font("SansSerif", Font.BOLD, 12));
        JPanel wrapPilih = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        wrapPilih.setOpaque(false);
        wrapPilih.add(btnPilih);

        dropCard.add(dropText, BorderLayout.CENTER);
        dropCard.add(wrapPilih, BorderLayout.SOUTH);

        // 2. Kartu Detail File Terpilih
        JPanel infoCard = new JPanel(new GridLayout(3, 2, 6, 6));
        infoCard.setBackground(BG_CARD);
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(new LineBorder(BORDER_COLOR), " File Terpilih ",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 11), TEXT_MUTED),
                new EmptyBorder(6, 10, 8, 10)
        ));
        infoCard.add(buatLabelMuted("Nama File:"));   infoCard.add(lblInfoNama);
        infoCard.add(buatLabelMuted("Ukuran:"));      infoCard.add(lblInfoUkuran);
        infoCard.add(buatLabelMuted("Status/Tipe:")); infoCard.add(lblInfoTipe);

        // 3. Tombol Aksi
        JPanel panelAksi = new JPanel(new GridLayout(1, 2, 8, 0));
        panelAksi.setOpaque(false);
        btnKompres.setBackground(ACCENT_DARK);
        btnKompres.setForeground(Color.WHITE);
        btnKompres.setOpaque(true);
        btnKompres.setBorderPainted(false);
        btnKompres.setFocusPainted(false);
        btnKompres.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnKompres.setPreferredSize(new Dimension(0, 36));

        btnDekompres.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnDekompres.setForeground(TEXT_PRIMARY);
        btnDekompres.setFocusPainted(false);
        btnDekompres.setPreferredSize(new Dimension(0, 36));

        panelAksi.add(btnKompres);
        panelAksi.add(btnDekompres);

        JPanel topKiri = new JPanel(new BorderLayout(0, 10));
        topKiri.setOpaque(false);
        topKiri.add(dropCard, BorderLayout.NORTH);
        topKiri.add(infoCard, BorderLayout.CENTER);
        topKiri.add(panelAksi, BorderLayout.SOUTH);

        // 4. Log Aktivitas
        JPanel logPanel = new JPanel(new BorderLayout(0, 4));
        logPanel.setOpaque(false);
        JLabel lblLog = new JLabel(" Riwayat Aktivitas");
        lblLog.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblLog.setForeground(TEXT_MUTED);
        logPanel.add(lblLog, BorderLayout.NORTH);
        JScrollPane scrollLog = new JScrollPane(log);
        scrollLog.setBorder(new LineBorder(BORDER_COLOR));
        logPanel.add(scrollLog, BorderLayout.CENTER);

        kiri.add(topKiri, BorderLayout.NORTH);
        kiri.add(logPanel, BorderLayout.CENTER);
        kiri.add(bar, BorderLayout.SOUTH);

        // ================= PANEL KANAN (Laporan & Analisis) =================
        JPanel kanan = new JPanel(new BorderLayout());
        kanan.setBackground(BG_MAIN);
        kanan.setBorder(new EmptyBorder(14, 6, 14, 14));

        // Tab 1: Ringkasan Eksekutif
        JPanel tabRingkasan = new JPanel(new BorderLayout(0, 12));
        tabRingkasan.setBackground(BG_MAIN);
        tabRingkasan.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Grid 4 Kartu KPI
        JPanel gridKpi = new JPanel(new GridLayout(1, 4, 10, 0));
        gridKpi.setOpaque(false);
        gridKpi.add(cardAwal);
        gridKpi.add(cardAkhir);
        gridKpi.add(cardHemat);
        gridKpi.add(cardEfisiensi);

        // Panel Tengah: Tabel Detail Teknis
        JScrollPane scrollRingkasan = new JScrollPane(ringkasanTable);
        scrollRingkasan.setBorder(new LineBorder(BORDER_COLOR));
        scrollRingkasan.getViewport().setBackground(BG_CARD);

        tabRingkasan.add(gridKpi, BorderLayout.NORTH);
        tabRingkasan.add(scrollRingkasan, BorderLayout.CENTER);
        tabRingkasan.add(barPanel, BorderLayout.SOUTH);

        // Tab 2: Tabel Kode Huffman
        JPanel tabKode = new JPanel(new BorderLayout(0, 6));
        tabKode.setBackground(BG_MAIN);
        tabKode.setBorder(new EmptyBorder(8, 8, 8, 8));
        JLabel hintKode = new JLabel(" Klik header kolom untuk mengurutkan berdasarkan frekuensi, panjang bit, atau simbol.");
        hintKode.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hintKode.setForeground(TEXT_MUTED);
        JScrollPane scrollKode = new JScrollPane(kodeTable);
        scrollKode.setBorder(new LineBorder(BORDER_COLOR));
        tabKode.add(hintKode, BorderLayout.NORTH);
        tabKode.add(scrollKode, BorderLayout.CENTER);

        // Tab 3: Inspeksi Biner
        JPanel tabBiner = new JPanel(new BorderLayout());
        tabBiner.setBorder(new EmptyBorder(8, 8, 8, 8));
        tabBiner.setBackground(BG_MAIN);
        JScrollPane scrollBiner = new JScrollPane(biner);
        scrollBiner.setBorder(new LineBorder(BORDER_COLOR));
        tabBiner.add(scrollBiner, BorderLayout.CENTER);

        tabs.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabs.addTab("  Laporan Ringkasan  ", tabRingkasan);
        tabs.addTab("  Distribusi & Tabel Kode  ", tabKode);
        tabs.addTab("  Inspeksi Header & Biner  ", tabBiner);

        kanan.add(tabs, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, kiri, kanan);
        split.setResizeWeight(0.32);
        split.setDividerLocation(360);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        // ================= EVENT LISTENERS =================
        btnPilih.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(new File("."));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                pilih(fc.getSelectedFile());
            }
        });
        btnKompres.addActionListener(e -> jalankan(true));
        btnDekompres.addActionListener(e -> jalankan(false));

        pasangDrop();
        setSize(1160, 680);
        setLocationRelativeTo(null);
        tampilkanKosong();
    }

    private void setupStyling() {
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        log.setBackground(BG_CARD);
        log.setForeground(TEXT_PRIMARY);
        log.setBorder(new EmptyBorder(6, 8, 6, 8));

        biner.setEditable(false);
        biner.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        biner.setBackground(new Color(0x18181B));
        biner.setForeground(new Color(0xE4E4E7));
        biner.setBorder(new EmptyBorder(10, 12, 10, 12));
        biner.setText("Pilih atau hasilkan file .huf untuk melihat struktur magic bytes, header frekuensi, dan aliran bit.");

        btnKompres.setEnabled(false);
        btnDekompres.setEnabled(false);

        // Styling Tabel Ringkasan
        ringkasanTable.setRowHeight(26);
        ringkasanTable.setShowVerticalLines(false);
        ringkasanTable.setGridColor(new Color(0xF4F4F5));
        ringkasanTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        ringkasanTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        ringkasanTable.getColumnModel().getColumn(0).setPreferredWidth(240);
        ringkasanTable.getColumnModel().getColumn(1).setPreferredWidth(360);

        // Styling Tabel Kode
        kodeTable.setAutoCreateRowSorter(true);
        kodeTable.setRowHeight(24);
        kodeTable.setShowVerticalLines(false);
        kodeTable.setGridColor(new Color(0xF4F4F5));
        kodeTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        kodeTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        kodeTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        kodeTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        kodeTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer persenRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                if (value instanceof Double) {
                    setText(String.format("%.2f%%", (Double) value));
                } else {
                    super.setValue(value);
                }
            }
        };
        persenRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        kodeTable.getColumnModel().getColumn(3).setCellRenderer(persenRenderer);
    }

    private JLabel buatLabelMuted(String teks) {
        JLabel l = new JLabel(teks);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(TEXT_MUTED);
        return l;
    }

    private void catatLog(String pesan) {
        String jam = new SimpleDateFormat("HH:mm:ss").format(new Date());
        log.append("[" + jam + "] " + pesan + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    // ---------- Drag and Drop ----------
    private void pasangDrop() {
        TransferHandler dropHandler = new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport s) {
                return s.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport s) {
                if (!canImport(s)) return false;
                try {
                    List<?> files = (List<?>) s.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        pilih((File) files.get(0));
                        return true;
                    }
                } catch (Exception ex) {
                    catatLog("Gagal membaca file drop: " + ex.getMessage());
                }
                return false;
            }
        };
        JComponent[] semua = {
                (JComponent) getContentPane(), log, biner,
                ringkasanTable, kodeTable, tabs
        };
        for (JComponent c : semua) c.setTransferHandler(dropHandler);
    }

    // ---------- Memilih File ----------
    private void pilih(File f) {
        if (!f.isFile()) {
            catatLog("Bukan file valid: " + f.getName());
            return;
        }
        terpilih = f;
        boolean isHuf = f.getName().toLowerCase().endsWith(".huf");

        lblDropJudul.setText("File Siap Diproses");
        lblDropSub.setText("Tarik file lain kapan saja untuk mengganti");
        lblInfoNama.setText(f.getName());
        lblInfoNama.setToolTipText(f.getAbsolutePath());
        lblInfoUkuran.setText(formatUkuranLengkap(f.length()));
        lblInfoTipe.setText(isHuf ? "Arsip Terkompresi (.huf)" : "File Mentah / Sumber");
        lblInfoTipe.setForeground(isHuf ? ACCENT_DARK : ACCENT_GREEN);
        lblInfoTipe.setFont(new Font("SansSerif", Font.BOLD, 12));

        btnKompres.setEnabled(!isHuf);
        btnDekompres.setEnabled(isHuf && f.length() > 0);
        catatLog("Memuat: " + f.getName() + " (" + formatUkuranLengkap(f.length()) + ")");

        if (isHuf) {
            refreshBiner(f);
            tabs.setSelectedIndex(2);
        } else {
            biner.setText("File ini adalah file mentah (" + f.getName() + ").\nKlik 'Kompres (.huf)' untuk membentuk bitstream Huffman dan melihat inspeksi binernya.");
            tabs.setSelectedIndex(0);
        }
    }

    private void refreshBiner(File f) {
        try {
            biner.setText(HufInspector.inspect(f, 64));
        } catch (Exception ex) {
            biner.setText("Tidak bisa membaca sebagai file .huf: " + ex.getMessage());
        }
        biner.setCaretPosition(0);
    }

    // ---------- Eksekusi Kompres / Dekompres ----------
    private void jalankan(boolean kompres) {
        File in = terpilih;
        File folderHasil = new File("hasil");
        if (!folderHasil.exists()) folderHasil.mkdirs();

        String namaSaran;
        if (kompres) {
            namaSaran = in.getName() + ".huf";
        } else if (in.getName().endsWith(".huf")) {
            String dasar = in.getName().substring(0, in.getName().length() - 4);
            if (new File(folderHasil, dasar).exists()) {
                int titik = dasar.lastIndexOf('.');
                dasar = (titik > 0)
                        ? dasar.substring(0, titik) + "_balik" + dasar.substring(titik)
                        : dasar + "_balik";
            }
            namaSaran = dasar;
        } else {
            namaSaran = in.getName() + ".out";
        }

        JFileChooser fc = new JFileChooser(folderHasil);
        fc.setSelectedFile(new File(folderHasil, namaSaran));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = fc.getSelectedFile();

        if (out.equals(in)) {
            JOptionPane.showMessageDialog(this, "File tujuan tidak boleh sama dengan file asal.");
            return;
        }
        if (out.exists() && JOptionPane.showConfirmDialog(this,
                out.getName() + " sudah ada di folder tersebut. Timpa?", "Konfirmasi Timpa File",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        setBusy(true);
        new SwingWorker<Hasil, Void>() {
            @Override
            protected Hasil doInBackground() throws Exception {
                return kompres ? FileCompressor.compress(in, out)
                               : FileCompressor.decompress(in, out);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    Hasil r = get();
                    catatLog((kompres ? "Kompresi" : "Dekompresi") + " selesai -> " + out.getName() + " (" + r.ms + " ms)");
                    tampilkan(r);
                    if (kompres) {
                        pilih(out);
                    }
                    tabs.setSelectedIndex(0);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    String msg = (cause.getMessage() != null) ? cause.getMessage() : cause.toString();
                    catatLog("ERROR: " + msg);
                    JOptionPane.showMessageDialog(App.this, msg, "Proses Gagal", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void setBusy(boolean sibuk) {
        btnPilih.setEnabled(!sibuk);
        boolean isHuf = terpilih != null && terpilih.getName().toLowerCase().endsWith(".huf");
        btnKompres.setEnabled(!sibuk && terpilih != null && !isHuf);
        btnDekompres.setEnabled(!sibuk && terpilih != null && isHuf && terpilih.length() > 0);
        bar.setIndeterminate(sibuk);
    }

    // ---------- Formatting & Laporan ----------
    private void tampilkanKosong() {
        catatLog("Aplikasi siap. Silakan pilih atau drop file uji dari folder samples/.");
        ringkasanModel.setRowCount(0);
        ringkasanModel.addRow(new Object[]{"1. Pilih Sumber", "Tarik file ke kotak di kiri atau klik 'Pilih File...'"});
        ringkasanModel.addRow(new Object[]{"2. Jalankan Operasi", "Klik 'Kompres (.huf)' untuk menguji rasio Huffman, atau 'Dekompres' pada file .huf"});
        ringkasanModel.addRow(new Object[]{"3. Analisis Hasil", "Lihat kartu statistik, komposisi header vs data, dan pohon kode tiap karakter"});
        kodeModel.setRowCount(0);
        barPanel.reset();
    }

    private static String formatUkuranLengkap(long b) {
        if (b < 1024) return String.format("%,d byte", b);
        double kb = b / 1024.0;
        if (kb < 1024) return String.format("%.2f KB (%,d byte)", kb, b);
        return String.format("%.2f MB (%,d byte)", kb / 1024.0, b);
    }

    private static String formatRingkas(long b) {
        if (b < 1024) return String.format("%,d B", b);
        double kb = b / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        return String.format("%.2f MB", kb / 1024.0);
    }

    private void baris(String item, String nilai) {
        ringkasanModel.addRow(new Object[]{item, nilai});
    }

    private static String huruf(int i) {
        if (i >= 33 && i <= 126) return "'" + (char) i + "'";
        switch (i) {
            case 32: return "[SPASI]";
            case 10: return "[LF \\n]";
            case 13: return "[CR \\r]";
            case 9:  return "[TAB \\t]";
            case 0:  return "[NULL]";
            default: return String.format("0x%02X", i);
        }
    }

    private void tampilkan(Hasil r) {
        boolean kompres = r.jenis.equals("Kompres");

        // 1. Update 4 KPI Cards
        cardAwal.update(
                kompres ? "UKURAN FILE ASLI" : "UKURAN ARSIP (.HUF)",
                formatRingkas(r.awal),
                String.format("%,d byte", r.awal),
                TEXT_PRIMARY
        );

        cardAkhir.update(
                kompres ? "UKURAN HASIL (.HUF)" : "UKURAN DEKOMPRESI",
                formatRingkas(r.akhir),
                String.format("%,d byte (%d ms)", r.akhir, r.ms),
                ACCENT_DARK
        );

        if (kompres && r.awal > 0) {
            long hemat = r.awal - r.akhir;
            double persenHemat = hemat * 100.0 / r.awal;
            double rasio = r.akhir * 100.0 / r.awal;
            if (hemat >= 0) {
                cardHemat.update("PENGHEMATAN RUANG",
                        String.format("%.1f%%", persenHemat),
                        String.format("Hemat %,d B (Rasio %.1f%%)", hemat, rasio),
                        ACCENT_GREEN);
            } else {
                cardHemat.update("PERUBAHAN UKURAN",
                        String.format("+%.1f%%", -persenHemat),
                        String.format("Membengkak %,d B (Fallback HUF0)", -hemat),
                        ACCENT_AMBER);
            }
        } else if (!kompres) {
            cardHemat.update("STATUS PEMULIHAN",
                    "100% Utuh",
                    "Integritas bit terverifikasi",
                    ACCENT_GREEN);
        } else {
            cardHemat.update("PENGHEMATAN RUANG", "0%", "File kosong (0 byte)", TEXT_MUTED);
        }

        double rata = r.rataKode();
        if (r.jumlahSimbol > 1 && rata > 0) {
            double eff = (r.entropi / rata) * 100.0;
            cardEfisiensi.update("EFISIENSI KODE",
                    String.format("%.1f%%", eff),
                    String.format("Entropi %.2f vs %.2f bit/simbol", r.entropi, rata),
                    ACCENT_DARK);
        } else {
            cardEfisiensi.update("EFISIENSI KODE",
                    r.mentah ? "Mode HUF0" : "1 Simbol",
                    r.mentah ? "Disimpan tanpa pohon Huffman" : "Tidak butuh bit payload",
                    TEXT_MUTED);
        }

        // 2. Isi Tabel Detail Teknis
        ringkasanModel.setRowCount(0);
        baris("Jenis Operasi", kompres ? "Kompresi File (Encode)" : "Dekompresi File (Decode)");
        baris("Format Kontainer", r.mode + (r.mentah ? " — Simpan Mentah (Fallback)" : " — Pohon Huffman Kanonik"));
        baris("Ukuran Awal", formatUkuranLengkap(r.awal));
        baris("Ukuran Akhir", formatUkuranLengkap(r.akhir));
        baris("Waktu Eksekusi", r.ms + " milidetik");

        if (r.freq != null && r.panjangAsli > 0) {
            baris("Variasi Simbol Unik", r.jumlahSimbol + " dari 256 kemungkinan nilai byte");
            baris("Overhead Header (.huf)", String.format("%,d byte (Magic + Panjang + Tabel Frekuensi)", r.headerByte));
            long payloadByte = (r.totalBit + 7) / 8;
            baris("Ukuran Payload Bit Terkompresi", String.format("%,d byte (%,d bit)", payloadByte, r.totalBit));
            baris("Ukuran Bit Mentah (8-bit ASCII/Byte)", String.format("%,d bit", r.panjangAsli * 8));
            baris("Entropi Shannon (Batas Teoritis)", String.format("%.4f bit/byte", r.entropi));
            baris("Rata-rata Panjang Kode Huffman", String.format("%.4f bit/byte", rata));
        }

        baris("Kesimpulan Analisis", r.mentah
                ? "Entropi file terlalu tinggi/acak sehingga kompresi Huffman tidak menghemat ruang. Sistem otomatis beralih ke mode HUF0 agar file tidak membengkak parah."
                : "Distribusi frekuensi simbol cukup bervariasi sehingga pengkodean prefix-free Huffman berhasil memadatkan representasi bit.");

        // 3. Isi Tabel Kode & Persentase
        kodeModel.setRowCount(0);
        if (r.freq != null && r.kode != null && r.panjangAsli > 0) {
            for (int i = 0; i < 256; i++) {
                if (r.freq[i] > 0 && r.kode[i] != null) {
                    double peluang = (r.freq[i] * 100.0) / r.panjangAsli;
                    kodeModel.addRow(new Object[]{
                            i,
                            huruf(i),
                            r.freq[i],
                            peluang,
                            r.kode[i],
                            r.kode[i].length(),
                            r.freq[i] * r.kode[i].length()
                    });
                }
            }
            kodeTable.getRowSorter().setSortKeys(List.of(
                    new RowSorter.SortKey(2, SortOrder.DESCENDING)));
        }

        // 4. Update Visualisasi Stacked Breakdown Bar
        long ukuranHuf = kompres ? r.akhir : r.awal;
        long ukuranAsli = kompres ? r.awal : r.akhir;
        long dataHuf = Math.max(0, ukuranHuf - r.headerByte);
        barPanel.setData(ukuranAsli, r.headerByte, dataHuf, r.mentah);
    }

    // ---------- Komponen Kartu KPI ----------
    private static class KpiCard extends JPanel {
        private final JLabel lblJudul = new JLabel();
        private final JLabel lblNilai = new JLabel();
        private final JLabel lblSub   = new JLabel();

        KpiCard(String judul, String nilai, String sub, Color warnaNilai) {
            setLayout(new BorderLayout(0, 4));
            setBackground(BG_CARD);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER_COLOR, 1, true),
                    new EmptyBorder(10, 12, 10, 12)
            ));

            lblJudul.setFont(new Font("SansSerif", Font.BOLD, 10));
            lblJudul.setForeground(TEXT_MUTED);

            lblNilai.setFont(new Font("SansSerif", Font.BOLD, 18));

            lblSub.setFont(new Font("SansSerif", Font.PLAIN, 11));
            lblSub.setForeground(TEXT_MUTED);

            add(lblJudul, BorderLayout.NORTH);
            add(lblNilai, BorderLayout.CENTER);
            add(lblSub, BorderLayout.SOUTH);

            update(judul, nilai, sub, warnaNilai);
        }

        void update(String judul, String nilai, String sub, Color warnaNilai) {
            lblJudul.setText(judul);
            lblNilai.setText(nilai);
            lblNilai.setForeground(warnaNilai);
            lblSub.setText(sub);
        }
    }

    // ---------- Komponen Grafik Komposisi (Stacked Bar) ----------
    private static class BreakdownBarPanel extends JPanel {
        private boolean aktif = false;
        private long asli, header, payload;
        private boolean isRaw;

        BreakdownBarPanel() {
            setPreferredSize(new Dimension(500, 125));
            setBackground(BG_CARD);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER_COLOR, 1, true),
                    new EmptyBorder(10, 14, 10, 14)
            ));
        }

        void reset() {
            aktif = false;
            repaint();
        }

        void setData(long asli, long header, long payload, boolean isRaw) {
            this.aktif = true;
            this.asli = asli;
            this.header = header;
            this.payload = payload;
            this.isRaw = isRaw;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!aktif) {
                g2.setColor(TEXT_MUTED);
                g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
                String msg = "Visualisasi perbandingan ukuran & struktur header akan muncul setelah operasi dijalankan.";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2 + 4);
                g2.dispose();
                return;
            }

            long totalHuf = header + payload;
            long maks = Math.max(Math.max(asli, totalHuf), 1);
            int padX = 16;
            int maxW = getWidth() - (padX * 2);

            // Bar 1: File Asli
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(TEXT_PRIMARY);
            g2.drawString(String.format("File Asli (100%%): %,d byte", asli), padX, 22);

            int wAsli = (int) (maxW * (double) asli / maks);
            g2.setColor(new Color(0x71717A));
            g2.fillRoundRect(padX, 28, Math.max(wAsli, 4), 16, 6, 6);

            // Bar 2: Struktur File .huf (Stacked: Header + Payload)
            double persenTotal = asli > 0 ? (totalHuf * 100.0 / asli) : 100.0;
            g2.setColor(TEXT_PRIMARY);
            g2.drawString(String.format("Arsip .huf (%.1f%%): Header %,d B + Data %,d B",
                    persenTotal, header, payload), padX, 66);

            int wHeader  = (int) (maxW * (double) header / maks);
            int wPayload = (int) (maxW * (double) payload / maks);

            g2.setColor(ACCENT_AMBER);
            g2.fillRect(padX, 72, Math.max(wHeader, header > 0 ? 3 : 0), 16);

            g2.setColor(isRaw ? ACCENT_RED : ACCENT_GREEN);
            g2.fillRect(padX + wHeader, 72, Math.max(wPayload, payload > 0 ? 3 : 0), 16);

            // Legenda Bawah
            int ly = 106;
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            gambarLegenda(g2, padX, ly, new Color(0x71717A), "Data Mentah Asli");
            gambarLegenda(g2, padX + 135, ly, ACCENT_AMBER, "Header Tabel (.huf)");
            gambarLegenda(g2, padX + 275, ly, isRaw ? ACCENT_RED : ACCENT_GREEN,
                    isRaw ? "Payload Tanpa Kompresi (HUF0)" : "Bitstream Terkompresi (HUF1)");

            g2.dispose();
        }

        private void gambarLegenda(Graphics2D g2, int x, int y, Color c, String teks) {
            g2.setColor(c);
            g2.fillRoundRect(x, y - 9, 10, 10, 3, 3);
            g2.setColor(TEXT_MUTED);
            g2.drawString(teks, x + 15, y);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }
}