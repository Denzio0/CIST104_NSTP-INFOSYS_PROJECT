import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import dao.UserDAO;

import dao.AttendanceDAO;
import dao.EngagementDAO;
import dao.PerformanceDAO;
import dao.DemeritDAO;
import dao.AnnouncementDAO;
import dao.NotificationDAO;
import dao.StudentDAO;
import dao.PointsDAO;
import dao.FeedbackDAO;
import dao.CalendarDAO;
import dao.DisputeDAO;
import util.Util;

class NSTPInfoSys {

	static final Color WHITE = new Color(255, 255, 255);
	static final Color BG = new Color(245, 247, 250);
	static final Color BORDER = new Color(213, 219, 229);
	static final Color BORDER_LIGHT = new Color(230, 234, 241);

	static final Color NAVY = new Color(27, 42, 74);
	static final Color NAVY_LIGHT = new Color(30, 53, 101);
	static final Color NAVY_SOFT = new Color(232, 237, 248);
	static final Color TEXT_MAIN = new Color(20, 28, 40);
	static final Color TEXT_SUB = new Color(82, 96, 115);
	static final Color TEXT_MUTED = new Color(148, 162, 179);

	static final Color ACCENT = new Color(27, 42, 74);
	static final Color ACCENT_SOFT = new Color(232, 237, 248);

	static final Color GOLD = new Color(180, 140, 30);
	static final Color GOLD_SOFT = new Color(255, 249, 220);

	static final Color GREEN = new Color(20, 130, 65);
	static final Color GREEN_SOFT = new Color(220, 248, 232);
	static final Color RED = new Color(180, 35, 35);
	static final Color RED_SOFT = new Color(255, 232, 232);
	static final Color ORANGE = new Color(200, 100, 10);
	static final Color ORANGE_SOFT = new Color(255, 240, 210);

	static final Color ROW_PRESENT = new Color(198, 234, 210);
	static final Color ROW_ABSENT = new Color(255, 204, 204);
	static final Color ROW_LATE = new Color(255, 244, 196);
	static final Color ROW_EXCUSED = new Color(200, 225, 255);

	static final String[] STATUS_OPTIONS = { "Present", "Absent", "Late", "Excused" };

	static final java.util.Map<String, java.util.List<String>> SESSION_LOG = new java.util.HashMap<>();

	static final java.util.List<String[]> NOTIFICATIONS = new java.util.ArrayList<>(java.util.Arrays.asList(
			new String[] { "ALL", "May 01, 2025", "General Assembly",
					"All NSTP students are required to attend the general assembly on May 10, 2025 at the school gymnasium. Attendance is mandatory." },
			new String[] { "ROTC", "Apr 28, 2025", "Field Training Exercise",
					"FTX scheduled for May 5, 2025. Full combat gear required. Report to Alpha Platoon area by 0500H." },
			new String[] { "CWTS", "Apr 27, 2025", "Community Service",
					"Coastal cleanup activity on May 3, 2025. Please bring gloves and wear appropriate attire. Meet at main gate by 7AM." },
			new String[] { "ROTC", "Apr 20, 2025", "Uniform Inspection",
					"Uniform inspection will be conducted next training day. Ensure all gear is complete and properly maintained." },
			new String[] { "CWTS", "Apr 18, 2025", "Project Proposal Deadline",
					"Submission of community project proposals due on May 2, 2025. Submit to your respective section facilitators." }));

	static void exportFrameToPDF(JFrame frame, String filename) {
		try {

			JPanel content = (JPanel) frame.getContentPane();
			int w = content.getWidth();
			int h = content.getHeight();
			if (w <= 0 || h <= 0) {
				w = frame.getWidth();
				h = frame.getHeight();
			}

			BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = img.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			content.printAll(g2);
			g2.dispose();

			java.io.File outDir = new java.io.File(System.getProperty("user.home"), "NSTP_Exports");
			if (!outDir.exists())
				outDir.mkdirs();
			java.io.File outFile = new java.io.File(outDir, filename);

			java.io.ByteArrayOutputStream imgBytes = new java.io.ByteArrayOutputStream();
			javax.imageio.ImageIO.write(img, "jpeg", imgBytes);
			byte[] jpegData = imgBytes.toByteArray();

			float pageW, pageH;
			if (w >= h) {
				pageW = 841.89f;
				pageH = 595.28f;
			} else {
				pageW = 595.28f;
				pageH = 841.89f;
			}

			float margin = 18f;
			float availW = pageW - margin * 2;
			float availH = pageH - margin * 2;
			float scale = Math.min(availW / w, availH / h);
			float imgW = w * scale;
			float imgH = h * scale;
			float imgX = margin + (availW - imgW) / 2f;
			float imgY = margin + (availH - imgH) / 2f;

			try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
				writePDF(fos, jpegData, (int) pageW, (int) pageH, imgX, imgY, imgW, imgH);
			}

			JOptionPane.showMessageDialog(frame, "PDF saved to:\n" + outFile.getAbsolutePath(), "Export Successful",
					JOptionPane.INFORMATION_MESSAGE);

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(frame, "Failed to export PDF:\n" + ex.getMessage(), "Export Error",
					JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	static void writePDF(java.io.OutputStream out, byte[] jpeg, int pageW, int pageH, float imgX, float imgY,
			float imgW, float imgH) throws Exception {

		java.util.List<Integer> offsets = new java.util.ArrayList<>();
		java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();

		java.util.function.Consumer<String> writeStr = s -> {
			try {
				buf.write(s.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
			} catch (Exception ignored) {
			}
		};
		java.util.function.Consumer<byte[]> writeBytes = b -> {
			try {
				buf.write(b);
			} catch (Exception ignored) {
			}
		};

		writeStr.accept("%PDF-1.4\n");

		offsets.add(buf.size());
		writeStr.accept("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

		offsets.add(buf.size());
		writeStr.accept("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

		offsets.add(buf.size());
		writeStr.accept("3 0 obj\n<< /Type /Page /Parent 2 0 R\n" + "   /MediaBox [0 0 " + pageW + " " + pageH + "]\n"
				+ "   /Contents 4 0 R\n" + "   /Resources << /XObject << /Im1 5 0 R >> >> >>\nendobj\n");

		float pdfY = pageH - imgY - imgH;
		String streamContent = "q\n" + imgW + " 0 0 " + imgH + " " + imgX + " " + pdfY + " cm\n/Im1 Do\nQ\n";
		byte[] streamBytes = streamContent.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
		offsets.add(buf.size());
		writeStr.accept("4 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
		writeBytes.accept(streamBytes);
		writeStr.accept("\nendstream\nendobj\n");

		offsets.add(buf.size());
		writeStr.accept("5 0 obj\n<< /Type /XObject /Subtype /Image\n" + "   /Width " + 1 + " /Height " + 1 + "\n"
				+ "   /ColorSpace /DeviceRGB /BitsPerComponent 8\n" + "   /Filter /DCTDecode /Length " + jpeg.length
				+ " >>\nstream\n");

		buf.reset();
		buf.write(new byte[0]);
		offsets.clear();

		writeStr.accept("%PDF-1.4\n");

		offsets.add(buf.size());
		writeStr.accept("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

		offsets.add(buf.size());
		writeStr.accept("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

		offsets.add(buf.size());
		writeStr.accept("3 0 obj\n<< /Type /Page /Parent 2 0 R\n" + "   /MediaBox [0 0 " + pageW + " " + pageH + "]\n"
				+ "   /Contents 4 0 R\n" + "   /Resources << /XObject << /Im1 5 0 R >> >> >>\nendobj\n");

		offsets.add(buf.size());
		writeStr.accept("4 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
		writeBytes.accept(streamBytes);
		writeStr.accept("\nendstream\nendobj\n");

		int jpegW = 1, jpegH = 1;
		for (int i = 0; i < jpeg.length - 8; i++) {
			if ((jpeg[i] & 0xFF) == 0xFF) {
				int marker = jpeg[i + 1] & 0xFF;
				if (marker >= 0xC0 && marker <= 0xC3) {
					jpegH = ((jpeg[i + 5] & 0xFF) << 8) | (jpeg[i + 6] & 0xFF);
					jpegW = ((jpeg[i + 7] & 0xFF) << 8) | (jpeg[i + 8] & 0xFF);
					break;
				}
			}
		}

		offsets.add(buf.size());
		writeStr.accept("5 0 obj\n<< /Type /XObject /Subtype /Image\n" + "   /Width " + jpegW + " /Height " + jpegH
				+ "\n" + "   /ColorSpace /DeviceRGB /BitsPerComponent 8\n" + "   /Filter /DCTDecode /Length "
				+ jpeg.length + " >>\nstream\n");
		writeBytes.accept(jpeg);
		writeStr.accept("\nendstream\nendobj\n");

		int xrefOffset = buf.size();
		writeStr.accept("xref\n0 6\n");
		writeStr.accept("0000000000 65535 f \n");
		for (int off : offsets) {
			writeStr.accept(String.format("%010d 00000 n \n", off));
		}
		writeStr.accept("trailer\n<< /Size 6 /Root 1 0 R >>\n");
		writeStr.accept("startxref\n" + xrefOffset + "\n%%EOF\n");

		out.write(buf.toByteArray());
	}

	static void recordLogin(String studentId) {
		String ts = new SimpleDateFormat("MMM dd, yyyy  hh:mm:ss a").format(new Date());
		SESSION_LOG.computeIfAbsent(studentId, k -> new java.util.ArrayList<>()).add(0, ts);
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) {
		}

		util.DBConnection.testConnection();
		SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
	}

	public static int toInt(Object value) {
		if (value == null)
			return 0;

		try {
			return Integer.parseInt(value.toString().replaceAll("[^0-9]", ""));
		} catch (Exception e) {
			return 0;
		}
	}

	static JButton makeBtn(String text, Color bg, Color fg, int radius) {
		JButton b = new JButton(text) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(getModel().isPressed() ? bg.darker() : getModel().isRollover() ? bg.brighter() : bg);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		b.setForeground(fg);
		b.setFont(new Font("SansSerif", Font.BOLD, 13));
		b.setFocusPainted(false);
		b.setBorderPainted(false);
		b.setContentAreaFilled(false);
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return b;
	}

	static JTextField styledField() {
		JTextField f = new JTextField() {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(WHITE);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
				g2.setColor(isFocusOwner() ? ACCENT : BORDER);
				g2.setStroke(new BasicStroke(isFocusOwner() ? 1.8f : 1.2f));
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		f.setOpaque(false);
		f.setFont(new Font("SansSerif", Font.PLAIN, 13));
		f.setForeground(TEXT_MAIN);
		f.setCaretColor(ACCENT);
		f.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
		f.setPreferredSize(new Dimension(320, 44));
		return f;
	}

	static JPasswordField styledPass() {
		JPasswordField f = new JPasswordField() {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(WHITE);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
				g2.setColor(isFocusOwner() ? ACCENT : BORDER);
				g2.setStroke(new BasicStroke(isFocusOwner() ? 1.8f : 1.2f));
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		f.setOpaque(false);
		f.setFont(new Font("SansSerif", Font.PLAIN, 13));
		f.setForeground(TEXT_MAIN);
		f.setCaretColor(ACCENT);
		f.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
		f.setPreferredSize(new Dimension(320, 44));
		return f;
	}

	static JLabel fieldLabel(String text) {
		JLabel l = new JLabel(text);
		l.setFont(new Font("SansSerif", Font.BOLD, 11));
		l.setForeground(TEXT_SUB);
		return l;
	}

	static JLabel tag(String text, Color bg, Color fg) {
		JLabel l = new JLabel(text) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(bg);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		l.setOpaque(false);
		l.setFont(new Font("SansSerif", Font.BOLD, 10));
		l.setForeground(fg);
		l.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
		return l;
	}

	static Image avatarPlaceholder(int size, Color accent) {
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 25));
		g.fillOval(0, 0, size, size);
		g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 100));
		g.setStroke(new BasicStroke(2));
		g.drawOval(1, 1, size - 2, size - 2);
		g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160));
		g.setFont(new Font("SansSerif", Font.BOLD, size / 3));
		FontMetrics fm = g.getFontMetrics();
		String ic = "?";
		g.drawString(ic, (size - fm.stringWidth(ic)) / 2, (size + fm.getAscent() - fm.getDescent()) / 2);
		g.dispose();
		return img;
	}

	static JTable makeTable(DefaultTableModel model2) {

		DefaultTableModel model = model2 != null ? model2 : new DefaultTableModel();
		JTable t = new JTable(model) {
			@Override
			public Component prepareRenderer(TableCellRenderer r, int row, int col) {
				Component c = super.prepareRenderer(r, row, col);
				c.setBackground(isRowSelected(row) ? ACCENT_SOFT : row % 2 == 0 ? WHITE : BG);
				c.setForeground(TEXT_MAIN);
				((JComponent) c).setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
				return c;
			}
		};
		styleTable(t);
		return t;
	}

	static void styleTable(JTable t) {
		t.setBackground(WHITE);
		t.setForeground(TEXT_MAIN);
		t.setFont(new Font("SansSerif", Font.PLAIN, 12));
		t.setRowHeight(34);
		t.setShowGrid(false);
		t.setIntercellSpacing(new Dimension(0, 0));
		t.setSelectionBackground(ACCENT_SOFT);
		t.setSelectionForeground(TEXT_MAIN);
		JTableHeader h = t.getTableHeader();
		h.setFont(new Font("SansSerif", Font.BOLD, 11));
		h.setBackground(NAVY);
		h.setForeground(Color.WHITE);
		h.setOpaque(true);
		h.setFont(new Font("SansSerif", Font.BOLD, 11));
		h.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, GOLD));
		h.setPreferredSize(new Dimension(0, 36));
		((DefaultTableCellRenderer) h.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
	}

	static JScrollPane wrapTable(JTable t) {
		JScrollPane sp = new JScrollPane(t);
		sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
		sp.getViewport().setBackground(WHITE);
		sp.setBackground(WHITE);
		return sp;
	}

	static JSeparator sep() {
		JSeparator s = new JSeparator();
		s.setForeground(BORDER_LIGHT);
		s.setBackground(BORDER_LIGHT);
		s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		return s;
	}

	static JPanel navBar(String label, Color accent, Color accentSoft, Runnable logout) {
		return navBar(label, accent, accentSoft, logout, null);
	}

	static JPanel navBar(String label, Color accent, Color accentSoft, Runnable logout, JFrame frameRef) {
		JPanel nav = new JPanel(new BorderLayout());
		nav.setBackground(WHITE);
		nav.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
		nav.setPreferredSize(new Dimension(0, 56));

		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 12));
		left.setOpaque(false);

		JPanel logoBox = new JPanel(new GridBagLayout()) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(NAVY);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
				g2.dispose();
			}
		};
		logoBox.setOpaque(false);
		logoBox.setPreferredSize(new Dimension(32, 32));
		JLabel ll = new JLabel("N");
		ll.setFont(new Font("SansSerif", Font.BOLD, 16));
		ll.setForeground(WHITE);
		logoBox.add(ll);

		JLabel title = new JLabel("NSTP INFOSYS  |  PLM");
		title.setFont(new Font("SansSerif", Font.BOLD, 13));
		title.setForeground(TEXT_MAIN);

		left.add(logoBox);
		left.add(title);
		left.add(tag(label, accentSoft, accent));

		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 13));
		right.setOpaque(false);

		if (frameRef != null) {
			JButton pdfBtn = makeBtn("⬇ Export PDF", accent, WHITE, 6);
			pdfBtn.setBorder(BorderFactory.createLineBorder(accent, 1, true));
			pdfBtn.setPreferredSize(new Dimension(110, 30));
			pdfBtn.addActionListener(e -> {
				String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
				String fname = "NSTP_" + label.replaceAll("[^a-zA-Z0-9]", "_") + "_" + ts + ".pdf";
				pdfBtn.setEnabled(false);
				pdfBtn.setText("Exporting…");
				SwingUtilities.invokeLater(() -> {
					exportFrameToPDF(frameRef, fname);
					pdfBtn.setEnabled(true);
					pdfBtn.setText("⬇ Export PDF");
				});
			});
			right.add(pdfBtn);
		}

		JButton out = makeBtn("Sign out", BG, TEXT_SUB, 6);
		out.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
		out.setPreferredSize(new Dimension(90, 30));
		out.addActionListener(e -> logout.run());
		right.add(out);

		nav.add(left, BorderLayout.WEST);
		nav.add(right, BorderLayout.EAST);
		return nav;
	}

	static class StatusRenderer extends JLabel implements TableCellRenderer {
		StatusRenderer() {
			setOpaque(true);
			setFont(new Font("SansSerif", Font.BOLD, 11));
			setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
		}

		@Override
		public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
			String s = v != null ? v.toString() : "Present";
			setText(s);
			switch (s) {
			case "Present":
				setBackground(GREEN_SOFT);
				setForeground(GREEN);
				break;
			case "Absent":
				setBackground(RED_SOFT);
				setForeground(RED);
				break;
			case "Late":
				setBackground(GOLD_SOFT);
				setForeground(GOLD);
				break;
			case "Excused":
				setBackground(ACCENT_SOFT);
				setForeground(ACCENT);
				break;
			default:
				setBackground(WHITE);
				setForeground(TEXT_MAIN);
			}
			if (sel)
				setBackground(getBackground().darker());
			return this;
		}
	}

	static class NotificationService {
		static void generateForStudent(int studentId, String program) {

			double attRate = AttendanceDAO.computeAttendanceRate(studentId);
			if (attRate > 0 && attRate < 75.0) {
				insertNotifIfNew(studentId, "AT_RISK",
						String.format("Your attendance rate is %.1f%%. Minimum required is 75%%.", attRate));
			}

			if ("ROTC".equals(program) && PerformanceDAO.checkAtRiskFlag(studentId)) {
				insertNotifIfNew(studentId, "PERFORMANCE_FLAG",
						"You have received 3 consecutive low performance ratings. Please consult your officer.");
			}

			if ("CWTS".equals(program)) {
				double hours = EngagementDAO.getTotalVerifiedHours(studentId);
				if (hours < 20.0) {
					insertNotifIfNew(studentId, "ENGAGEMENT_WARNING",
							String.format("You have %.1f of 20 required engagement hours.", hours));
				}
			}

			checkUpcomingEvents(studentId, program);
		}

		private static void checkUpcomingEvents(int studentId, String program) {

			String sql = "SELECT title FROM calendar_events " + "WHERE event_date BETWEEN CURDATE() "
					+ "AND DATE_ADD(CURDATE(), INTERVAL 3 DAY) " + "AND (program_type='All' OR program_type=?)";

			try (java.sql.Connection conn = util.DBConnection.getConnection()) {

				if (conn == null)
					return;

				try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

					ps.setString(1, program);

					java.sql.ResultSet rs = ps.executeQuery();

					while (rs.next()) {

						insertNotifIfNew(studentId, "UPCOMING_EVENT",
								"Upcoming event: " + rs.getString("title") + " in the next 3 days.");
					}

				}

			} catch (java.sql.SQLException e) {
				e.printStackTrace();
			}
		}

		private static void insertNotifIfNew(int studentId, String type, String message) {
			NotificationDAO.insertIfNew(studentId, type, message);
		}
	}

	static class StatusEditor extends DefaultCellEditor {
		StatusEditor() {
			super(new JComboBox<>(STATUS_OPTIONS));
			JComboBox<String> cb = (JComboBox<String>) getComponent();
			cb.setFont(new Font("SansSerif", Font.BOLD, 11));
			cb.setBackground(WHITE);
			cb.setForeground(TEXT_MAIN);
			cb.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
		}
	}

	public static boolean assignRank(int studentId, String abbreviation, String rank) {

		String sql = "INSERT INTO rotc_ranks (student_id, platoon, battalion, rank_name) " + "VALUES (?, '', '', ?) "
				+ "ON DUPLICATE KEY UPDATE rank_name = VALUES(rank_name)";

		try (java.sql.Connection conn = util.DBConnection.getConnection()) {
			if (conn == null)
				return false;
			try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, studentId);
				ps.setString(2, rank);
				ps.executeUpdate();
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	static JPanel notifCard(String[] notif, Color accent, Color accentSoft) {
		String target = notif[0];
		String date = notif[1];
		String subject = notif[2];
		String body = notif[3];

		JPanel card = new JPanel(new BorderLayout(12, 0)) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(WHITE);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
				g2.setColor(BORDER);
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
				g2.dispose();
			}
		};
		card.setOpaque(false);
		card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

		JPanel left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
		left.setOpaque(false);

		JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		topRow.setOpaque(false);

		Color tagBg = target.equals("ROTC") ? GOLD_SOFT : target.equals("CWTS") ? GREEN_SOFT : ACCENT_SOFT;
		Color tagFg = target.equals("ROTC") ? GOLD : target.equals("CWTS") ? GREEN : ACCENT;
		topRow.add(tag(target, tagBg, tagFg));

		JLabel dateLabel = new JLabel(date);
		dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
		dateLabel.setForeground(TEXT_MUTED);
		topRow.add(dateLabel);

		JLabel subjectLabel = new JLabel(subject);
		subjectLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
		subjectLabel.setForeground(TEXT_MAIN);
		subjectLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel bodyLabel = new JLabel("<html><div style='width:500px'>" + body + "</div></html>");
		bodyLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
		bodyLabel.setForeground(TEXT_SUB);
		bodyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		left.add(topRow);
		left.add(Box.createVerticalStrut(5));
		left.add(subjectLabel);
		left.add(Box.createVerticalStrut(4));
		left.add(bodyLabel);

		card.add(left, BorderLayout.CENTER);
		return card;
	}

	static class LoginFrame extends JFrame {
		JTextField userField;
		JPasswordField passField;
		JLabel msgLabel;

		LoginFrame() {
			setTitle("NSTP INFOSYS");
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			setSize(1200, 700);
			setMinimumSize(new Dimension(900, 580));
			setLocationRelativeTo(null);
			setBackground(WHITE);
			setContentPane(build());
		}

		JPanel build() {
			JPanel root = new JPanel(new GridBagLayout());
			root.setBackground(WHITE);

			JPanel left = new JPanel(new GridBagLayout());
			left.setBackground(BG);
			left.setPreferredSize(new Dimension(560, 0));

			GridBagConstraints lc = new GridBagConstraints();
			lc.gridx = 0;
			lc.gridy = GridBagConstraints.RELATIVE;
			lc.anchor = GridBagConstraints.WEST;
			lc.fill = GridBagConstraints.HORIZONTAL;

			JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			logoRow.setOpaque(false);

			JPanel logoBox = new JPanel(new GridBagLayout()) {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(NAVY);
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
					g2.dispose();
				}
			};
			logoBox.setOpaque(false);
			logoBox.setPreferredSize(new Dimension(40, 40));
			JLabel ll = new JLabel("N");
			ll.setFont(new Font("SansSerif", Font.BOLD, 20));
			ll.setForeground(WHITE);
			logoBox.add(ll);

			JLabel appName = new JLabel("  NSTP INFOSYS");
			appName.setFont(new Font("SansSerif", Font.BOLD, 15));
			appName.setForeground(TEXT_MAIN);
			logoRow.add(logoBox);
			logoRow.add(appName);

			lc.insets = new Insets(0, 72, 40, 72);
			left.add(logoRow, lc);

			JLabel headline = new JLabel("<html>National Service<br>Training Program</html>");
			headline.setFont(new Font("SansSerif", Font.BOLD, 38));
			headline.setForeground(TEXT_MAIN);
			lc.insets = new Insets(0, 72, 14, 72);
			left.add(headline, lc);

			JLabel sub = new JLabel("<html><div style='width:360px;color:#647080;font-size:12px;line-height:1.75'>"
					+ "Centralized records for CWTS and ROTC students</div></html>");
			lc.insets = new Insets(0, 72, 28, 72);
			left.add(sub, lc);

			JPanel tagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
			tagRow.setOpaque(false);
			tagRow.add(tag("CWTS", GREEN_SOFT, GREEN));
			tagRow.add(tag("ROTC", GOLD_SOFT, GOLD));
			tagRow.add(tag("Admin", ACCENT_SOFT, ACCENT));
			lc.insets = new Insets(0, 72, 0, 72);
			left.add(tagRow, lc);

			JLabel plmFooter = new JLabel("Pamantasan ng Lungsod ng Maynila - NSTP");
			plmFooter.setFont(new Font("SansSerif", Font.PLAIN, 10));
			plmFooter.setForeground(TEXT_MUTED);
			lc.insets = new Insets(24, 72, 0, 72);
			left.add(plmFooter, lc);

			JPanel right = new JPanel(new GridBagLayout());
			right.setBackground(WHITE);
			right.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER));
			right.setPreferredSize(new Dimension(440, 0));

			GridBagConstraints rc = new GridBagConstraints();
			rc.gridx = 0;
			rc.gridy = GridBagConstraints.RELATIVE;
			rc.fill = GridBagConstraints.HORIZONTAL;
			rc.anchor = GridBagConstraints.WEST;
			rc.weightx = 1.0;

			JLabel signIn = new JLabel("Sign in");
			signIn.setFont(new Font("SansSerif", Font.BOLD, 26));
			signIn.setForeground(TEXT_MAIN);
			rc.insets = new Insets(0, 56, 4, 56);
			right.add(signIn, rc);

			JLabel signInSub = new JLabel("Enter your credentials to continue");
			signInSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			signInSub.setForeground(TEXT_MUTED);
			rc.insets = new Insets(0, 56, 32, 56);
			right.add(signInSub, rc);

			userField = styledField();
			rc.insets = new Insets(0, 56, 6, 56);
			right.add(fieldLabel("USER ID"), rc);
			rc.insets = new Insets(0, 56, 20, 56);
			right.add(userField, rc);

			passField = styledPass();
			JPanel passWrapper = new JPanel(new BorderLayout(0, 0));
			passWrapper.setOpaque(false);
			passWrapper.setPreferredSize(new Dimension(320, 44));

			Container eyeBtn = new JButton("👁") {
				@Override
				protected void paintComponent(Graphics g) {
					g.setColor(getBackground());
					g.fillRect(0, 0, getWidth(), getHeight());
					super.paintComponent(g);
				}
			};
			eyeBtn.setFont(new Font("SansSerif", Font.PLAIN, 16));
			((AbstractButton) eyeBtn).setFocusPainted(false);
			((AbstractButton) eyeBtn).setBorderPainted(false);
			((AbstractButton) eyeBtn).setContentAreaFilled(false);
			eyeBtn.setBackground(new Color(0, 0, 0, 0));
			eyeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			eyeBtn.setPreferredSize(new Dimension(40, 44));
			((JComponent) eyeBtn).setToolTipText("Show/Hide Password");
			((AbstractButton) eyeBtn).addActionListener(e -> {
				if (passField.getEchoChar() == (char) 0) {
					passField.setEchoChar('●');
					((AbstractButton) eyeBtn).setText("👁");
				} else {
					passField.setEchoChar((char) 0);
					((AbstractButton) eyeBtn).setText("🔒");
				}
			});

			passWrapper.add(passField, BorderLayout.CENTER);
			passWrapper.add(eyeBtn, BorderLayout.EAST);

			rc.insets = new Insets(0, 56, 6, 56);
			right.add(fieldLabel("PASSWORD"), rc);
			rc.insets = new Insets(0, 56, 8, 56);
			right.add(passWrapper, rc);
			msgLabel = new JLabel(" ");
			msgLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
			msgLabel.setForeground(RED);
			rc.insets = new Insets(0, 56, 16, 56);
			right.add(msgLabel, rc);

			JButton loginBtn = makeBtn("Sign In", ACCENT, WHITE, 8);
			loginBtn.setPreferredSize(new Dimension(320, 46));
			loginBtn.addActionListener(e -> doLogin());
			passField.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER)
						doLogin();
				}
			});
			rc.insets = new Insets(0, 56, 20, 56);
			right.add(loginBtn, rc);

			JLabel hint = new JLabel("Use your credentials assigned by the administrator.");
			hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
			hint.setForeground(TEXT_MUTED);
			hint.setHorizontalAlignment(SwingConstants.CENTER);
			rc.insets = new Insets(0, 56, 0, 56);
			right.add(hint, rc);

			GridBagConstraints mc = new GridBagConstraints();
			mc.fill = GridBagConstraints.BOTH;
			mc.weightx = 1;
			mc.weighty = 1;
			root.add(left, mc);
			mc.weightx = 0;
			root.add(right, mc);

			return root;
		}

		private int failedAttempts = 0;
		private boolean lockedOut = false;
		private static final int MAX_ATTEMPTS = 5;
		private static final int LOCKOUT_SECONDS = 60;

		void doLogin() {
			if (lockedOut) {
				msgLabel.setText("Account locked. Please wait and try again.");
				msgLabel.setForeground(RED);
				return;
			}
			if (failedAttempts >= MAX_ATTEMPTS) {
				lockedOut = true;
				msgLabel.setForeground(RED);
				msgLabel.setText("Too many attempts. Locked out for " + LOCKOUT_SECONDS + " seconds.");
				javax.swing.Timer unlockTimer = new javax.swing.Timer(LOCKOUT_SECONDS * 1000, e -> {
					lockedOut = false;
					failedAttempts = 0;
					msgLabel.setText("You may try again.");
					msgLabel.setForeground(new Color(27, 42, 74));
				});
				unlockTimer.setRepeats(false);
				unlockTimer.start();
				return;
			}

			String uid = userField.getText().trim();
			char[] pwChars = passField.getPassword();
			String pw = pwChars != null ? new String(pwChars).trim() : "";
			java.util.Arrays.fill(pwChars != null ? pwChars : new char[0], '\0');
			msgLabel.setText(" ");

			if (uid.isEmpty() || pw.isEmpty()) {
				msgLabel.setForeground(RED);
				msgLabel.setText("Please enter both User ID and Password.");
				return;
			}

			String[] userInfo = UserDAO.validateLoginFull(uid, pw);
			if (userInfo != null) {
				int userId = Integer.parseInt(userInfo[0]);
				String role = userInfo[2];
				String sidStr = userInfo[3];
				String fullName = userInfo[4] != null ? userInfo[4] : "Administrator";
				String program = userInfo[5] != null ? userInfo[5] : "";

				int numericStudentId = -1;
				if (sidStr != null && !sidStr.isEmpty()) {
					try {
						numericStudentId = Integer.parseInt(sidStr);
					} catch (NumberFormatException ignored) {
						System.err.println("[Login] Warning: student_id not numeric: " + sidStr);
					}
				}

				UserDAO.recordLoginTimestamp(userId);
				recordLogin(uid);

				switch (role) {
				case "Admin":
					new AdminFrame(userId).setVisible(true);
					dispose();
					return;
				case "ROTC_Student":
					new ROTCFrame(numericStudentId, userId).setVisible(true);
					dispose();
					return;
				case "CWTS_Student":
					new CWTSFrame(numericStudentId, userId).setVisible(true);
					dispose();
					return;
				case "ROTC_Senior":
					new ROTCFrame(numericStudentId, userId).setVisible(true);
					dispose();
					return;
				}
			}

			failedAttempts++;
			int remaining = MAX_ATTEMPTS - failedAttempts;
			msgLabel.setForeground(RED);
			if (remaining > 0)
				msgLabel.setText("Invalid credentials. " + remaining + " attempt(s) remaining.");
			else
				msgLabel.setText("Last attempt failed. You will be locked out.");
		}
	}

	static abstract class BaseStudentFrame extends JFrame {
		JPanel contentArea;
		java.util.Map<String, JButton> sideButtons = new java.util.LinkedHashMap<>();
		int studentId;
		int userId;

		BaseStudentFrame(String titleSuffix, int studentId, int userId) {
			this.studentId = studentId;
			this.userId = userId;
			setTitle("NSTP INFOSYS — " + titleSuffix);
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			setSize(1200, 700);
			setMinimumSize(new Dimension(900, 580));
			setLocationRelativeTo(null);
			setContentPane(buildFrame());
		}

		abstract String getProgram();

		abstract Color getAccent();

		abstract Color getAccentSoft();

		abstract String getAvatarName();

		abstract String getRoleTagText();

		abstract String[][] getSidebarFields();

		abstract java.util.List<String[]> getSideNavItems();

		abstract JPanel buildPanelFor(String key);

		JPanel buildFrame() {
			JPanel root = new JPanel(new BorderLayout());
			root.setBackground(BG);
			root.add(navBar(getProgram(), getAccent(), getAccentSoft(), () -> {
				new LoginFrame().setVisible(true);
				dispose();
			}, this), BorderLayout.NORTH);

			JPanel body = new JPanel(new BorderLayout());
			body.setBackground(BG);
			body.add(buildProfileSidebar(), BorderLayout.WEST);

			JPanel mainArea = new JPanel(new BorderLayout());
			mainArea.setBackground(BG);
			mainArea.add(buildSideNav(), BorderLayout.WEST);

			contentArea = new JPanel(new BorderLayout());
			contentArea.setBackground(BG);

			java.util.List<String[]> navItems = getSideNavItems();
			if (!navItems.isEmpty())
				switchPanel(navItems.get(0)[0]);

			mainArea.add(contentArea, BorderLayout.CENTER);
			body.add(mainArea, BorderLayout.CENTER);
			root.add(body, BorderLayout.CENTER);
			return root;
		}

		void switchPanel(String key) {
			contentArea.removeAll();
			contentArea.add(buildPanelFor(key), BorderLayout.CENTER);
			contentArea.revalidate();
			contentArea.repaint();
			sideButtons.forEach((k, btn) -> {
				boolean active = k.equals(key);
				btn.setBackground(
						active ? new Color(getAccent().getRed(), getAccent().getGreen(), getAccent().getBlue(), 28)
								: WHITE);
				btn.setForeground(active ? getAccent() : TEXT_MAIN);
				btn.setBorder(active
						? BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, getAccent()),
								BorderFactory.createEmptyBorder(0, 13, 0, 16))
						: BorderFactory.createEmptyBorder(0, 16, 0, 16));
				btn.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 13));
			});
		}

		JPanel buildProfileSidebar() {
			JPanel p = new JPanel();
			p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
			p.setBackground(WHITE);
			p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER),
					BorderFactory.createEmptyBorder(32, 28, 32, 28)));
			p.setPreferredSize(new Dimension(240, 0));

			JLabel avatarLabel = new JLabel(new ImageIcon(avatarPlaceholder(80, getAccent())));
			avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			p.add(avatarLabel);
			p.add(Box.createVerticalStrut(16));

			JLabel name = new JLabel(getAvatarName());
			name.setFont(new Font("SansSerif", Font.BOLD, 16));
			name.setForeground(TEXT_MAIN);
			name.setAlignmentX(Component.CENTER_ALIGNMENT);
			p.add(name);
			p.add(Box.createVerticalStrut(5));

			JLabel roleTag = tag(getRoleTagText(), getAccentSoft(), getAccent());
			roleTag.setAlignmentX(Component.CENTER_ALIGNMENT);
			p.add(roleTag);
			p.add(Box.createVerticalStrut(24));
			p.add(sep());
			p.add(Box.createVerticalStrut(20));

			for (String[] f : getSidebarFields()) {
				JLabel lbl = new JLabel(f[0]);
				lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
				lbl.setForeground(TEXT_MUTED);
				lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
				p.add(lbl);
				p.add(Box.createVerticalStrut(3));
				JLabel val = new JLabel(f[1]);
				val.setFont(new Font("SansSerif", Font.PLAIN, 13));
				val.setForeground(TEXT_MAIN);
				val.setAlignmentX(Component.LEFT_ALIGNMENT);
				p.add(val);
				p.add(Box.createVerticalStrut(14));
			}
			return p;
		}

		JPanel buildSideNav() {
			JPanel nav = new JPanel();
			nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
			nav.setBackground(WHITE);
			nav.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));
			nav.setPreferredSize(new Dimension(200, 0));
			nav.add(Box.createVerticalStrut(20));

			JLabel section = new JLabel("MENU");
			section.setFont(new Font("SansSerif", Font.BOLD, 10));
			section.setForeground(TEXT_MUTED);
			section.setBorder(BorderFactory.createEmptyBorder(0, 18, 8, 18));
			section.setAlignmentX(Component.LEFT_ALIGNMENT);
			nav.add(section);

			java.util.List<String[]> items = getSideNavItems();
			for (int i = 0; i < items.size(); i++) {
				String key = items.get(i)[0];
				String label = items.get(i)[1];
				boolean first = i == 0;
				JButton btn = makeSideNavBtn(label, first ? getAccentSoft() : WHITE, first ? getAccent() : TEXT_MAIN,
						first);
				btn.addActionListener(e -> switchPanel(key));
				sideButtons.put(key, btn);
				nav.add(btn);
				nav.add(Box.createVerticalStrut(2));
			}
			return nav;
		}

		JButton makeSideNavBtn(String text, Color bg, Color fg, boolean bold) {
			JButton b = new JButton(text) {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(getBackground());
					g2.fillRoundRect(4, 2, getWidth() - 8, getHeight() - 4, 8, 8);
					g2.dispose();
					super.paintComponent(g);
				}
			};
			b.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, 13));
			b.setHorizontalAlignment(SwingConstants.LEFT);
			b.setFocusPainted(false);
			b.setBorderPainted(false);
			b.setContentAreaFilled(false);
			b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			b.setMaximumSize(new Dimension(200, 38));
			b.setPreferredSize(new Dimension(200, 38));
			b.setAlignmentX(Component.LEFT_ALIGNMENT);
			b.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
			b.setBackground(bg);
			b.setForeground(fg);
			return b;
		}
	}

	static class NotificationBoardPanel extends JPanel {
		NotificationBoardPanel(String program, Color accent, Color accentSoft) {
			setLayout(new BorderLayout());
			setBackground(BG);

			JPanel header = new JPanel();
			header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(28, 28, 16, 28));

			JLabel title = new JLabel("Notification Board");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);
			title.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel sub = new JLabel("Announcements and updates from the admin");
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);
			sub.setAlignmentX(Component.LEFT_ALIGNMENT);

			header.add(title);
			header.add(Box.createVerticalStrut(3));
			header.add(sub);

			JPanel listPanel = new JPanel();
			listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
			listPanel.setBackground(BG);
			listPanel.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

			for (String[] notif : NOTIFICATIONS) {
				if (notif[0].equals("ALL") || notif[0].equals(program)) {
					listPanel.add(notifCard(notif, accent, accentSoft));
					listPanel.add(Box.createVerticalStrut(10));
				}
			}

			JScrollPane scroll = new JScrollPane(listPanel);
			scroll.setBorder(BorderFactory.createEmptyBorder());
			scroll.getViewport().setBackground(BG);
			scroll.setBackground(BG);
			scroll.getVerticalScrollBar().setUnitIncrement(12);

			add(header, BorderLayout.NORTH);
			add(scroll, BorderLayout.CENTER);
		}
	}

	static class AttendanceRecordsPanel extends JPanel {
		String studentId;
		String program;
		DefaultTableModel model;
		JTable table;

		AttendanceRecordsPanel(String studentId, String program, String lastColHeader) {
			this.studentId = studentId;
			this.program = program;
			setLayout(new BorderLayout());
			setBackground(BG);
			setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

			JPanel tableHeader = new JPanel(new BorderLayout());
			tableHeader.setOpaque(false);
			tableHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

			JLabel tableTitle = new JLabel("Attendance Records");
			tableTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
			tableTitle.setForeground(TEXT_MAIN);

			JLabel tableSub = new JLabel(
					program.equals("ROTC") ? "All training sessions" : "All community service sessions");
			tableSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			tableSub.setForeground(TEXT_MUTED);

			tableHeader.add(tableTitle, BorderLayout.WEST);
			tableHeader.add(tableSub, BorderLayout.EAST);

			String[] cols = { "Date", "Session", "Status", "Signature", "Dispute" };
			model = new DefaultTableModel(cols, 0) {
				public boolean isCellEditable(int r, int c) {
					return false;
				}
			};
			table = new JTable(model) {
				@Override
				public Component prepareRenderer(TableCellRenderer r, int row, int col) {
					Component c = super.prepareRenderer(r, row, col);
					c.setBackground(isRowSelected(row) ? ACCENT_SOFT : row % 2 == 0 ? WHITE : BG);
					c.setForeground(TEXT_MAIN);
					((JComponent) c).setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
					return c;
				}
			};
			styleTable(table);

			table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row,
						int col) {
					super.getTableCellRendererComponent(t, v, sel, foc, row, col);
					String s = v != null ? v.toString() : "";
					setForeground(s.equals("Present") ? GREEN
							: s.equals("Absent") ? RED : s.equals("Excused") ? ACCENT : GOLD);
					setFont(new Font("SansSerif", Font.BOLD, 12));
					setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
					setBackground(sel ? ACCENT_SOFT : row % 2 == 0 ? WHITE : BG);
					setOpaque(true);
					return this;
				}
			});

			table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row,
						int col) {
					super.getTableCellRendererComponent(t, v, sel, foc, row, col);
					String s = v != null ? v.toString() : "";
					switch (s) {
					case "Pending":
						setForeground(GOLD);
						break;
					case "Approved":
						setForeground(GREEN);
						break;
					case "Rejected":
						setForeground(RED);
						break;
					default:
						setForeground(TEXT_MUTED);
					}
					setFont(new Font("SansSerif", Font.BOLD, 11));
					setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
					setBackground(sel ? ACCENT_SOFT : row % 2 == 0 ? WHITE : BG);
					setOpaque(true);
					return this;
				}
			});

			table.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						int row = table.getSelectedRow();
						if (row < 0)
							return;
						String status = model.getValueAt(row, 2).toString();
						String disputeCol = model.getValueAt(row, 4).toString();
						if (!status.equals("Absent") && !status.equals("Late")) {
							JOptionPane.showMessageDialog(AttendanceRecordsPanel.this,
									"Only Absent or Late records can be disputed.", "Not Disputable",
									JOptionPane.INFORMATION_MESSAGE);
							return;
						}
						if (disputeCol.equals("Pending") || disputeCol.equals("Approved")) {
							JOptionPane.showMessageDialog(AttendanceRecordsPanel.this,
									"A dispute for this record is already " + disputeCol.toLowerCase() + ".",
									"Already Disputed", JOptionPane.INFORMATION_MESSAGE);
							return;
						}
						showDisputeDialog(row);
					}
				}
			});

			JLabel hint = new JLabel("Double-click an Absent or Late row to file a dispute");
			hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
			hint.setForeground(TEXT_MUTED);
			hint.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 0));

			refreshTable();

			int total = 0;
			int present = 0;

			try {

				int sid = Util.toInt(studentId);

				java.util.List<String[]> attendanceList = dao.AttendanceDAO.getAttendanceByStudentId(sid);

				for (String[] rec : attendanceList) {

					total++;

					String status = rec[4];

					if ("Present".equalsIgnoreCase(status) || "Excused".equalsIgnoreCase(status)) {

						present++;
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			double rate = total == 0 ? 0.0 : (present * 100.0 / total);
			Color rateColor = rate >= 75 ? GREEN : rate >= 50 ? GOLD : RED;

			JPanel summaryBar = new JPanel(new BorderLayout(10, 0));
			summaryBar.setOpaque(false);
			summaryBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

			JLabel rateLabel = new JLabel(
					String.format("Attendance Rate: %.1f%%  (%d of %d sessions)", rate, present, total));
			rateLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
			rateLabel.setForeground(rateColor);

			JProgressBar bar = new JProgressBar(0, 100);
			bar.setValue((int) rate);
			bar.setStringPainted(false);
			bar.setForeground(rateColor);
			bar.setBackground(BG);
			bar.setPreferredSize(new Dimension(0, 8));
			bar.setBorderPainted(false);

			summaryBar.add(rateLabel, BorderLayout.NORTH);
			summaryBar.add(bar, BorderLayout.SOUTH);

			JPanel northSection = new JPanel(new BorderLayout());
			northSection.setOpaque(false);
			northSection.add(tableHeader, BorderLayout.NORTH);
			northSection.add(summaryBar, BorderLayout.SOUTH);

			add(northSection, BorderLayout.NORTH);
			add(wrapTable(table), BorderLayout.CENTER);
			add(hint, BorderLayout.SOUTH);
		}

		void refreshTable() {

			model.setRowCount(0);

			boolean hasLive = false;

			try {

				int sid = Util.toInt(studentId);

				java.util.List<String[]> records = dao.AttendanceDAO.getAttendanceWithDisputes(sid);

				for (String[] rec : records) {

					hasLive = true;

					String attendanceStatus = rec[2];

					model.addRow(new Object[] {

							rec[0], rec[1], attendanceStatus,

							attendanceStatus.equalsIgnoreCase("Absent") ? "—" : "Sgd.",

							rec[3]

					});
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			if (!hasLive) {

				model.addRow(new Object[] {

						"—", "No sessions recorded yet", "—", "—", ""

				});
			}
		}

		void showDisputeDialog(int row) {
			String date = model.getValueAt(row, 0).toString();
			String session = model.getValueAt(row, 1).toString();
			String status = model.getValueAt(row, 2).toString();

			JPanel dlg = new JPanel();
			dlg.setLayout(new BoxLayout(dlg, BoxLayout.Y_AXIS));
			dlg.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

			JLabel info = new JLabel(
					"<html>Filing dispute for: <b>" + session + "</b> on " + date + " (" + status + ")</html>");
			info.setFont(new Font("SansSerif", Font.PLAIN, 12));
			info.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel reasonLbl = new JLabel("Reason for dispute:");
			reasonLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
			reasonLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

			JTextArea reasonArea = new JTextArea(4, 30);
			reasonArea.setLineWrap(true);
			reasonArea.setWrapStyleWord(true);
			reasonArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
			reasonArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
					BorderFactory.createEmptyBorder(6, 8, 6, 8)));
			JScrollPane sp = new JScrollPane(reasonArea);
			sp.setAlignmentX(Component.LEFT_ALIGNMENT);

			dlg.add(info);
			dlg.add(Box.createVerticalStrut(12));
			dlg.add(reasonLbl);
			dlg.add(Box.createVerticalStrut(4));
			dlg.add(sp);

			int result = JOptionPane.showConfirmDialog(this, dlg, "File Dispute", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);
			if (result != JOptionPane.OK_OPTION)
				return;

			String reason = reasonArea.getText().trim();
			if (reason.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Please enter a reason.", "Required", JOptionPane.WARNING_MESSAGE);
				return;
			}

			String studentName = "";
			int parsedSid = Util.toInt(studentId);
			String[] studentInfo = dao.StudentDAO.getStudentById(parsedSid);

			if (studentInfo != null) {

				studentName = studentInfo[1];
			}

			boolean success = dao.DisputeDAO.fileDispute(parsedSid, studentName, program, date, session, status,
					reason);

			if (success) {
				refreshTable();
				JOptionPane.showMessageDialog(this, "Dispute filed successfully. Awaiting admin review.",
						"Dispute Submitted", JOptionPane.INFORMATION_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(this, "Failed to submit dispute. Check database connection.", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	static class AccountSettingsPanel extends JPanel {
		String studentId;
		Color accent;
		Color accentSoft;

		AccountSettingsPanel(String studentId, Color accent, Color accentSoft) {
			this.studentId = studentId;
			this.accent = accent;
			this.accentSoft = accentSoft;

			setLayout(new BorderLayout(0, 0));
			setBackground(BG);

			JPanel header = new JPanel();
			header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(28, 28, 20, 28));

			JLabel title = new JLabel("Account Settings");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);
			title.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel sub = new JLabel("Manage your password and view login activity");
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);
			sub.setAlignmentX(Component.LEFT_ALIGNMENT);

			header.add(title);
			header.add(Box.createVerticalStrut(3));
			header.add(sub);

			JPanel body = new JPanel(new GridLayout(1, 2, 20, 0));
			body.setBackground(BG);
			body.setBorder(BorderFactory.createEmptyBorder(0, 28, 28, 28));
			body.add(buildPasswordCard());
			body.add(buildSessionCard());

			add(header, BorderLayout.NORTH);
			add(body, BorderLayout.CENTER);
		}

		JPanel buildPasswordCard() {
			JPanel card = card();

			JLabel cardTitle = new JLabel("Change Password");
			cardTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
			cardTitle.setForeground(TEXT_MAIN);
			cardTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel cardSub = new JLabel("Your new password must be at least 6 characters");
			cardSub.setFont(new Font("SansSerif", Font.PLAIN, 11));
			cardSub.setForeground(TEXT_MUTED);
			cardSub.setAlignmentX(Component.LEFT_ALIGNMENT);

			card.add(cardTitle);
			card.add(Box.createVerticalStrut(3));
			card.add(cardSub);
			card.add(Box.createVerticalStrut(20));
			card.add(sep());
			card.add(Box.createVerticalStrut(18));

			JPasswordField currentPw = pwField();
			JPasswordField newPw = pwField();
			JPasswordField confirmPw = pwField();

			card.add(pwRow("Current Password", currentPw));
			card.add(Box.createVerticalStrut(12));
			card.add(pwRow("New Password", newPw));
			card.add(Box.createVerticalStrut(12));
			card.add(pwRow("Confirm New Password", confirmPw));
			card.add(Box.createVerticalStrut(20));

			JLabel statusMsg = new JLabel(" ");
			statusMsg.setFont(new Font("SansSerif", Font.PLAIN, 12));
			statusMsg.setAlignmentX(Component.LEFT_ALIGNMENT);

			JButton changeBtn = makeBtn("Update Password", accent, WHITE, 8);
			changeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
			changeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
			changeBtn.addActionListener(ev -> {
				String cur = new String(currentPw.getPassword()).trim();
				String nw = new String(newPw.getPassword()).trim();
				String confirm = new String(confirmPw.getPassword()).trim();
				if (nw.length() < 6) {
					statusMsg.setForeground(RED);
					statusMsg.setText("New password must be at least 6 characters.");
					return;
				}
				if (!nw.equals(confirm)) {
					statusMsg.setForeground(RED);
					statusMsg.setText("Passwords do not match.");
					return;
				}
				int uid = dao.UserDAO.getUserIdByStudentId(Util.toInt(studentId));
				if (uid < 0)
					uid = dao.UserDAO.getUserId(studentId);
				boolean ok = dao.UserDAO.updatePassword(uid, cur, nw);
				if (!ok) {
					statusMsg.setForeground(RED);
					statusMsg.setText("Current password is incorrect.");
				} else {
					statusMsg.setForeground(GREEN);
					statusMsg.setText("Password updated successfully!");
					currentPw.setText("");
					newPw.setText("");
					confirmPw.setText("");
				}
			});
			card.add(changeBtn);
			card.add(Box.createVerticalStrut(10));
			card.add(statusMsg);
			return card;
		}

		JPanel buildSessionCard() {
			JPanel card = card();

			JPanel titleRow = new JPanel(new BorderLayout());
			titleRow.setOpaque(false);
			titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
			titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

			JLabel cardTitle = new JLabel("Login Session History");
			cardTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
			cardTitle.setForeground(TEXT_MAIN);

			JLabel countTag = tag("Current session", accentSoft, accent);
			titleRow.add(cardTitle, BorderLayout.WEST);
			titleRow.add(countTag, BorderLayout.EAST);

			JLabel cardSub = new JLabel("Recent sign-in activity for your account");
			cardSub.setFont(new Font("SansSerif", Font.PLAIN, 11));
			cardSub.setForeground(TEXT_MUTED);
			cardSub.setAlignmentX(Component.LEFT_ALIGNMENT);

			card.add(titleRow);
			card.add(Box.createVerticalStrut(3));
			card.add(cardSub);
			card.add(Box.createVerticalStrut(20));
			card.add(sep());
			card.add(Box.createVerticalStrut(14));

			java.util.List<String> sessions = SESSION_LOG.getOrDefault(studentId, new java.util.ArrayList<>());
			if (sessions.isEmpty()) {
				JLabel noSessions = new JLabel("No login history available.");
				noSessions.setFont(new Font("SansSerif", Font.PLAIN, 12));
				noSessions.setForeground(TEXT_MUTED);
				noSessions.setAlignmentX(Component.LEFT_ALIGNMENT);
				card.add(noSessions);
			} else {
				int limit = Math.min(sessions.size(), 10);
				for (int i = 0; i < limit; i++) {
					card.add(sessionRow(sessions.get(i), i == 0));
					card.add(Box.createVerticalStrut(6));
				}
			}
			return card;
		}

		JPanel sessionRow(String timestamp, boolean isCurrent) {
			JPanel row = new JPanel(new BorderLayout(10, 0)) {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(isCurrent ? accentSoft : BG);
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
					g2.dispose();
				}
			};
			row.setOpaque(false);
			row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
			row.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

			JPanel dot = new JPanel() {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(isCurrent ? accent : TEXT_MUTED);
					g2.fillOval(2, 2, 10, 10);
					g2.dispose();
				}
			};
			dot.setOpaque(false);
			dot.setPreferredSize(new Dimension(14, 14));

			JPanel info = new JPanel();
			info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
			info.setOpaque(false);

			JLabel tsLabel = new JLabel(timestamp);
			tsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
			tsLabel.setForeground(isCurrent ? accent : TEXT_MAIN);

			JLabel deviceLabel = new JLabel(isCurrent ? "This session  ·  Active now" : "Previous session");
			deviceLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
			deviceLabel.setForeground(TEXT_MUTED);

			info.add(tsLabel);
			info.add(deviceLabel);

			row.add(dot, BorderLayout.WEST);
			row.add(info, BorderLayout.CENTER);
			if (isCurrent)
				row.add(tag("Active", GREEN_SOFT, GREEN), BorderLayout.EAST);
			return row;
		}

		JPasswordField pwField() {
			JPasswordField f = new JPasswordField();
			f.setFont(new Font("SansSerif", Font.PLAIN, 13));
			f.setBackground(WHITE);
			f.setForeground(TEXT_MAIN);
			f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true),
					BorderFactory.createEmptyBorder(9, 12, 9, 12)));
			f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
			f.setAlignmentX(Component.LEFT_ALIGNMENT);
			return f;
		}

		JPanel pwRow(String label, JPasswordField field) {
			JPanel row = new JPanel();
			row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
			row.setOpaque(false);
			row.setAlignmentX(Component.LEFT_ALIGNMENT);
			JLabel lbl = new JLabel(label.toUpperCase());
			lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
			lbl.setForeground(TEXT_SUB);
			lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
			field.setAlignmentX(Component.LEFT_ALIGNMENT);
			row.add(lbl);
			row.add(Box.createVerticalStrut(4));
			row.add(field);
			return row;
		}

		JPanel card() {
			JPanel p = new JPanel() {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(WHITE);
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
					g2.setColor(BORDER);
					g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
					g2.dispose();
				}
			};
			p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
			p.setOpaque(false);
			p.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
			return p;
		}
	}

	static class StudentDashboardPanel extends JPanel {
		String studentId;
		String program;
		private int total;

		StudentDashboardPanel(String studentId, String program, Color accent, Color accentSoft) {
			this.studentId = studentId;
			this.program = program;
			setLayout(new BorderLayout());
			setBackground(BG);

			JPanel header = new JPanel();
			header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(28, 28, 20, 28));

			String name = "Student";

			try {
				int sid = Integer.parseInt(studentId);

				String sql = "SELECT full_name FROM students WHERE student_id = ?";

				try (java.sql.Connection conn = util.DBConnection.getConnection();
						java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
					ps.setInt(1, sid);
					java.sql.ResultSet rs = ps.executeQuery();

					if (rs.next()) {
						name = rs.getString("full_name");
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			JLabel title = new JLabel("Welcome back, " + name.split(" ")[0] + "!");
			title.setFont(new Font("SansSerif", Font.BOLD, 22));
			title.setForeground(TEXT_MAIN);
			title.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel sub = new JLabel(program + " Program  ·  NSTP Information System");
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);
			sub.setAlignmentX(Component.LEFT_ALIGNMENT);

			header.add(title);
			header.add(Box.createVerticalStrut(4));
			header.add(sub);

			JPanel cards = new JPanel(new GridLayout(1, program.equals("ROTC") ? 3 : 3, 16, 0));
			cards.setOpaque(false);
			cards.setBorder(BorderFactory.createEmptyBorder(0, 28, 24, 28));

			int sid = Util.toInt(studentId);

			int total = dao.AttendanceDAO.getAttendanceCount(sid);
			int present = dao.AttendanceDAO.getPresentAttendanceCount(sid);
			double attRate = total == 0 ? 0.0 : (present * 100.0 / total);

			Color attColor = attRate >= 75 ? GREEN : attRate >= 50 ? GOLD : RED;

			cards.add(

					dashCard(

							"Attendance Rate",

							String.format("%.0f%%", attRate),

							attColor,

							new Color(

									attColor.getRed(), attColor.getGreen(), attColor.getBlue(), 30

							)));

			if (program.equals("CWTS")) {

				double totalHours = dao.EngagementDAO.getTotalHours(sid);

				Color engColor = totalHours >= 20 ? GREEN : totalHours >= 10 ? GOLD : RED;

				cards.add(

						dashCard(

								"Engagement Hours",

								String.format("%.1f / 20h", totalHours),

								engColor,

								new Color(

										engColor.getRed(), engColor.getGreen(), engColor.getBlue(), 30

								)));

			} else {

				int demeritPoints = dao.DemeritDAO.getTotalPoints(sid);

				int demeritCount = dao.DemeritDAO.getRecordCount(sid);

				Color demColor = demeritPoints == 0 ? GREEN : demeritPoints <= 10 ? GOLD : RED;

				cards.add(

						dashCard(

								"Demerit Points",

								demeritPoints + " pts (" + demeritCount + " records)",

								demColor,

								new Color(

										demColor.getRed(), demColor.getGreen(), demColor.getBlue(), 30

								)));
			}

			cards.add(dashCard("Sessions Logged", String.valueOf(total), NAVY, new Color(27, 42, 74, 20)));

			add(header, BorderLayout.NORTH);
			add(cards, BorderLayout.CENTER);
		}

		JPanel dashCard(String label, String value, Color accent, Color bgColor) {
			JPanel card = new JPanel() {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(WHITE);
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
					g2.setColor(BORDER);
					g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

					g2.setColor(accent);
					g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
					g2.dispose();
				}
			};
			card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
			card.setOpaque(false);
			card.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

			JLabel lbl = new JLabel(label.toUpperCase());
			lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
			lbl.setForeground(TEXT_MUTED);
			lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel val = new JLabel(value);
			val.setFont(new Font("SansSerif", Font.BOLD, 26));
			val.setForeground(accent);
			val.setAlignmentX(Component.LEFT_ALIGNMENT);

			card.add(lbl);
			card.add(Box.createVerticalStrut(8));
			card.add(val);
			return card;
		}
	}

	static class ROTCFrame extends BaseStudentFrame {
		ROTCFrame(int studentId, int userId) {
			super("ROTC", studentId, userId);
		}

		@Override
		String getProgram() {
			return "ROTC";
		}

		@Override
		Color getAccent() {
			return GOLD;
		}

		@Override
		Color getAccentSoft() {
			return GOLD_SOFT;
		}

		@Override
		String getAvatarName() {

			String name = "ROTC Cadet";

			try {

				int sid = Util.toInt(studentId);

				String sql = "SELECT full_name FROM students " + "WHERE student_id = ? AND is_deleted = 0";

				try (java.sql.Connection conn = util.DBConnection.getConnection();
						java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

					ps.setInt(1, sid);

					java.sql.ResultSet rs = ps.executeQuery();

					if (rs.next()) {
						name = rs.getString("full_name");
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return name;
		}

		@Override
		String getRoleTagText() {
			return "ROTC Cadet";
		}

		@Override
		String[][] getSidebarFields() {

			String name = "";
			String platoon = "";
			String battalion = "";
			String rank = "Cadet";

			try {

				int sid = Util.toInt(studentId);

				String sql = "SELECT s.full_name, r.rank_name, r.platoon, r.battalion " + "FROM students s "
						+ "LEFT JOIN rotc_ranks r ON s.student_id = r.student_id "
						+ "WHERE s.student_id = ? AND s.is_deleted = 0";

				try (java.sql.Connection conn = util.DBConnection.getConnection();
						java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

					ps.setInt(1, sid);

					java.sql.ResultSet rs = ps.executeQuery();

					if (rs.next()) {

						name = rs.getString("full_name");
						platoon = rs.getString("platoon");
						battalion = rs.getString("battalion");

						String dbRank = rs.getString("rank_name");
						if (dbRank != null && !dbRank.isEmpty()) {
							rank = dbRank;
						}
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return new String[][] { { "NAME", name }, { "ID", String.valueOf(studentId) }, { "RANK", rank },
					{ "PLATOON", platoon != null ? platoon : "—" },
					{ "BATTALION", battalion != null ? battalion : "—" } };
		}

		@Override
		java.util.List<String[]> getSideNavItems() {
			java.util.List<String[]> items = new java.util.ArrayList<>();
			items.add(new String[] { "dashboard", "Dashboard" });
			items.add(new String[] { "attendance", "Attendance" });
			items.add(new String[] { "demerits", "Demerits" });
			items.add(new String[] { "performance", "Performance" });
			items.add(new String[] { "calendar", "Calendar" });
			items.add(new String[] { "notifications", "Notifications" });
			items.add(new String[] { "account", "Account Settings" });
			items.add(new String[] { "points", "Points" });
			items.add(new String[] { "feedback", "Feedback" });
			return items;
		}

		@Override
		JPanel buildPanelFor(String key) {
			switch (key) {
			case "dashboard":
				return new StudentDashboardPanel(String.valueOf(studentId), "ROTC", GOLD, GOLD_SOFT);
			case "attendance":
				return new AttendanceRecordsPanel(String.valueOf(studentId), "ROTC", "Demerits");
			case "demerits":
				return new ROTCDemeritsStudentPanel(String.valueOf(studentId));
			case "performance":
				return new COCCManagementPanel.ROTCPerformanceStudentPanel(String.valueOf(studentId));
			case "calendar":
				return new CalendarPanel("ROTC", GOLD, GOLD_SOFT, false);
			case "notifications":
				return new NotificationBoardPanel("ROTC", GOLD, GOLD_SOFT);
			case "account":
				return new AccountSettingsPanel(String.valueOf(studentId), GOLD, GOLD_SOFT);
			case "points":
				return new COCCManagementPanel.PointsStudentPanel(String.valueOf(studentId));
			case "feedback":
				return new FeedbackAdminPanel();
			default:
				return new JPanel();
			}
		}

	}

	static class CWTSFrame extends BaseStudentFrame {

		CWTSFrame(int studentId, int userId) {
			super("CWTS", studentId, userId);

			int sid = Util.toInt(studentId);
			String prog = getProgram();

			final int finalSid = sid;

			javax.swing.Timer notifTimer = new javax.swing.Timer(60000, e -> {
				if (finalSid > 0) {
					NotificationService.generateForStudent(finalSid, prog);
				}
			});

			notifTimer.setInitialDelay(5000);
			notifTimer.start();
		}

		@Override
		String getProgram() {
			return "CWTS";
		}

		@Override
		Color getAccent() {
			return GREEN;
		}

		@Override
		Color getAccentSoft() {
			return GREEN_SOFT;
		}

		@Override
		String getAvatarName() {
			String[] student = dao.StudentDAO.getStudentById(studentId);
			return (student != null) ? student[1] : "CWTS Student";
		}

		@Override
		String getRoleTagText() {
			return "CWTS Student";
		}

		@Override
		String[][] getSidebarFields() {
			String[] student = dao.StudentDAO.getStudentById(studentId);
			String name = (student != null) ? student[1] : "—";
			String section = (student != null) ? student[4] : "—";
			return new String[][] { { "NAME", name }, { "ID", String.valueOf(studentId) }, { "SECTION", section } };
		}

		@Override
		java.util.List<String[]> getSideNavItems() {
			java.util.List<String[]> items = new java.util.ArrayList<>();

			items.add(new String[] { "dashboard", "Dashboard" });
			items.add(new String[] { "attendance", "Attendance" });
			items.add(new String[] { "engagement", "Engagement Status" });
			items.add(new String[] { "calendar", "Calendar" });
			items.add(new String[] { "notifications", "Notifications" });
			items.add(new String[] { "account", "Account Settings" });
			items.add(new String[] { "points", "Points" });
			items.add(new String[] { "feedback", "Feedback" });

			return items;
		}

		@Override
		JPanel buildPanelFor(String key) {
			switch (key) {
			case "dashboard":
				return new StudentDashboardPanel(String.valueOf(studentId), "CWTS", GREEN, GREEN_SOFT);

			case "attendance":
				return new AttendanceRecordsPanel(String.valueOf(studentId), "CWTS", "Contribution");

			case "engagement":
				return new CWTSEngagementStudentPanel(studentId);

			case "calendar":
				return new CalendarPanel("CWTS", GREEN, GREEN_SOFT, false);

			case "notifications":
				return new NotificationBoardPanel("CWTS", GREEN, GREEN_SOFT);

			case "account":
				return new AccountSettingsPanel(String.valueOf(studentId), GREEN, GREEN_SOFT);

			case "points":
				return new COCCManagementPanel.PointsStudentPanel(String.valueOf(studentId));

			case "feedback":
				return new FeedbackAdminPanel();

			default:
				return new JPanel();
			}
		}
	}

	static class AdminFrame extends JFrame {
		JPanel contentArea;
		java.util.Map<String, JButton> sideButtons = new java.util.LinkedHashMap<>();

		int adminUserId;

		AdminFrame(int adminUserId) {
			this.adminUserId = adminUserId;
			setTitle("NSTP INFOSYS — Admin");
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			setSize(1200, 700);
			setMinimumSize(new Dimension(900, 580));
			setLocationRelativeTo(null);
			setContentPane(build());
		}

		JPanel build() {
			JPanel root = new JPanel(new BorderLayout());
			root.setBackground(BG);
			root.add(navBar("Admin", ACCENT, ACCENT_SOFT, () -> {
				new LoginFrame().setVisible(true);
				dispose();
			}, this), BorderLayout.NORTH);

			JPanel body = new JPanel(new BorderLayout());
			body.setBackground(BG);
			body.add(buildSidebar(), BorderLayout.WEST);

			contentArea = new JPanel(new BorderLayout());
			contentArea.setBackground(BG);
			switchPanel("attendance");

			body.add(contentArea, BorderLayout.CENTER);
			root.add(body, BorderLayout.CENTER);
			return root;
		}

		JPanel buildSidebar() {
			JPanel side = new JPanel();
			side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
			side.setBackground(WHITE);
			side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));
			side.setPreferredSize(new Dimension(220, 0));
			side.add(Box.createVerticalStrut(24));

			JLabel sectionLbl = new JLabel("MANAGEMENT");
			sectionLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
			sectionLbl.setForeground(TEXT_MUTED);
			sectionLbl.setBorder(BorderFactory.createEmptyBorder(0, 20, 8, 20));
			sectionLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
			side.add(sectionLbl);

			String[][] navItems = { { "attendance", "Attendance" }, { "register", "Register Student" },
					{ "push_notif", "Push Notification" }, { "disputes", "Dispute Management" },
					{ "cwts_engage", "CWTS Engagement" }, { "rotc_perf", "ROTC Performance" },
					{ "rotc_demerits", "ROTC Demerits" }, { "points_admin", "Points Management" },
					{ "leaderboard", "Leaderboard" }, { "calendar", "Calendar Events" },
					{ "cocc_mgmt", "COCC Management" }, { "feedback_admin", "Feedback" }, { "reports", "Reports" }, };

			for (int i = 0; i < navItems.length; i++) {
				String key = navItems[i][0];
				String label = navItems[i][1];
				boolean first = i == 0;
				JButton btn = sideBtn(label, first);
				btn.addActionListener(e -> switchPanel(key));
				sideButtons.put(key, btn);
				side.add(btn);
				side.add(Box.createVerticalStrut(4));
			}

			return side;
		}

		void switchPanel(String key) {
			contentArea.removeAll();
			switch (key) {
			case "attendance":
				contentArea.add(new AttendanceAdminPanel(adminUserId), BorderLayout.CENTER);
				break;

			case "register":
				contentArea.add(new RegisterStudentPanel(), BorderLayout.CENTER);
				break;

			case "push_notif":
				contentArea.add(new PushNotificationPanel(), BorderLayout.CENTER);
				break;

			case "disputes":
				contentArea.add(new DisputePanel(), BorderLayout.CENTER);
				break;

			case "cwts_engage":
				contentArea.add(new CWTSEngagementAdminPanel(), BorderLayout.CENTER);
				break;

			case "rotc_perf":
				contentArea.add(new NSTPInfoSys.AttendanceAdminPanel.ROTCPerformanceAdminPanel(), BorderLayout.CENTER);
				break;
			case "points_admin":
				contentArea.add(new PointsAdminPanel(), BorderLayout.CENTER);
				break;
			case "leaderboard":
				contentArea.add(new LeaderboardPanel(), BorderLayout.CENTER);
				break;
			case "feedback_admin":
				contentArea.add(new FeedbackAdminPanel(), BorderLayout.CENTER);
				break;
			case "reports":
				contentArea.add(new ReportsPanel(), BorderLayout.CENTER);
				break;
			case "rotc_demerits":
				contentArea.add(new ROTCDemeritsAdminPanel(), BorderLayout.CENTER);
				break;
			case "calendar":
				contentArea.add(new CalendarPanel("ALL", ACCENT, ACCENT_SOFT, true), BorderLayout.CENTER);
				break;
			case "cocc_mgmt":
				contentArea.add(new COCCManagementPanel(), BorderLayout.CENTER);
				break;
			}
			contentArea.revalidate();
			contentArea.repaint();
			sideButtons.forEach((k, btn) -> {
				boolean active = k.equals(key);
				btn.setBackground(active ? NAVY_SOFT : WHITE);
				btn.setForeground(active ? NAVY : TEXT_MAIN);
				btn.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 13));
			});
		}

		JButton sideBtn(String text, boolean active) {
			JButton b = new JButton(text) {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(getBackground());
					g2.fillRoundRect(4, 2, getWidth() - 8, getHeight() - 4, 8, 8);
					g2.dispose();
					super.paintComponent(g);
				}
			};
			b.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 13));
			b.setHorizontalAlignment(SwingConstants.LEFT);
			b.setFocusPainted(false);
			b.setBorderPainted(false);
			b.setContentAreaFilled(false);
			b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			b.setMaximumSize(new Dimension(220, 38));
			b.setPreferredSize(new Dimension(220, 38));
			b.setAlignmentX(Component.LEFT_ALIGNMENT);
			b.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
			b.setBackground(active ? ACCENT_SOFT : WHITE);
			b.setForeground(active ? ACCENT : TEXT_MAIN);
			return b;
		}
	}

	static class AttendanceAdminPanel extends JPanel {
		JTextField sessionNameField;
		JSpinner dateSpinner;
		JComboBox<String> programFilter;
		DefaultTableModel tableModel;
		JTable table;
		java.util.List<String[]> currentStudents = new java.util.ArrayList<>();
		int loggedByUserId = 1;

		AttendanceAdminPanel() {
			this(1);
		}

		AttendanceAdminPanel(int adminUserId) {
			this.loggedByUserId = adminUserId;
			setLayout(new BorderLayout(0, 0));
			setBackground(BG);
			add(buildTop(), BorderLayout.NORTH);
			add(buildTableArea(), BorderLayout.CENTER);
			add(buildBottom(), BorderLayout.SOUTH);
			loadStudents();
		}

		JPanel buildTop() {
			JPanel top = new JPanel(new BorderLayout());
			top.setBackground(BG);
			top.setBorder(BorderFactory.createEmptyBorder(28, 28, 0, 28));

			JLabel title = new JLabel("Attendance Admin");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);

			JLabel sub = new JLabel("Log a session and mark student statuses");
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);

			JPanel titleBlock = new JPanel();
			titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
			titleBlock.setOpaque(false);
			titleBlock.add(title);
			titleBlock.add(Box.createVerticalStrut(3));
			titleBlock.add(sub);

			top.add(titleBlock, BorderLayout.NORTH);
			top.add(Box.createVerticalStrut(20), BorderLayout.CENTER);
			top.add(buildControls(), BorderLayout.SOUTH);
			return top;
		}

		JPanel buildControls() {
			JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
			row.setOpaque(false);
			row.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));

			sessionNameField = new JTextField(16);
			sessionNameField.setFont(new Font("SansSerif", Font.PLAIN, 13));
			sessionNameField.setForeground(TEXT_MAIN);
			sessionNameField.setBackground(WHITE);
			sessionNameField.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(BORDER, 1, true), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
			sessionNameField.setPreferredSize(new Dimension(200, 38));

			Date today = clearTime(new Date());
			SpinnerDateModel dateModel = new SpinnerDateModel(today, today, today, Calendar.DAY_OF_MONTH);
			dateSpinner = new JSpinner(dateModel);
			JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "MMM dd, yyyy");
			dateSpinner.setEditor(dateEditor);
			dateEditor.getTextField().setEditable(false);
			dateSpinner.setPreferredSize(new Dimension(160, 38));
			dateSpinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
			dateSpinner.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));

			programFilter = new JComboBox<>(new String[] { "All", "ROTC", "CWTS" });
			programFilter.setPreferredSize(new Dimension(120, 38));
			programFilter.setBackground(WHITE);
			programFilter.setFont(new Font("SansSerif", Font.PLAIN, 13));
			programFilter.addActionListener(e -> loadStudents());

			row.add(labeled("Session Name", sessionNameField));
			row.add(labeled("Date", dateSpinner));
			row.add(labeled("Program", programFilter));
			return row;
		}

		JPanel labeled(String label, JComponent field) {
			JPanel p = new JPanel();
			p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
			p.setOpaque(false);
			JLabel lbl = new JLabel(label);
			lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
			lbl.setForeground(TEXT_SUB);
			p.add(lbl);
			p.add(Box.createVerticalStrut(4));
			p.add(field);
			return p;
		}

		JPanel buildTableArea() {
			JPanel area = new JPanel(new BorderLayout());
			area.setBackground(BG);
			area.setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 28));

			String[] cols = { "#", "Student Name", "ID", "Program", "Platoon / Section", "Status" };
			tableModel = new DefaultTableModel(cols, 0) {
				public boolean isCellEditable(int r, int c) {
					return c == 5;
				}

				public Class<?> getColumnClass(int c) {
					return c == 5 ? String.class : Object.class;
				}
			};

			table = new JTable(tableModel) {
				@Override
				public Component prepareRenderer(TableCellRenderer r, int row, int col) {
					Component c = super.prepareRenderer(r, row, col);
					if (!isRowSelected(row)) {
						Object statusVal = tableModel.getValueAt(row, 5);
						String s = statusVal != null ? statusVal.toString() : "Present";
						switch (s) {
						case "Present":
							c.setBackground(ROW_PRESENT);
							break;
						case "Absent":
							c.setBackground(ROW_ABSENT);
							break;
						case "Late":
							c.setBackground(ROW_LATE);
							break;
						case "Excused":
							c.setBackground(ROW_EXCUSED);
							break;
						default:
							c.setBackground(row % 2 == 0 ? WHITE : BG);
						}
					}
					return c;
				}
			};
			styleTable(table);
			table.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());
			table.getColumnModel().getColumn(5).setCellEditor(new StatusEditor());
			table.getColumnModel().getColumn(0).setPreferredWidth(40);
			table.getColumnModel().getColumn(0).setMaxWidth(50);
			table.getColumnModel().getColumn(2).setPreferredWidth(110);
			table.getColumnModel().getColumn(3).setPreferredWidth(80);
			table.getColumnModel().getColumn(5).setPreferredWidth(130);

			area.add(wrapTable(table), BorderLayout.CENTER);
			return area;
		}

		JPanel buildBottom() {
			JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 28, 14));
			bottom.setBackground(BG);
			bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));

			JButton saveBtn = makeBtn("Save Session", ACCENT, WHITE, 8);
			saveBtn.setPreferredSize(new Dimension(140, 40));
			saveBtn.addActionListener(e -> saveSession());

			JButton clearBtn = makeBtn("Clear", BG, TEXT_SUB, 8);
			clearBtn.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
			clearBtn.setPreferredSize(new Dimension(90, 40));
			clearBtn.addActionListener(e -> clearStatuses());

			bottom.add(clearBtn);
			bottom.add(saveBtn);
			return bottom;
		}

		static Date clearTime(Date d) {
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			return c.getTime();
		}

		void loadStudents() {
			tableModel.setRowCount(0);
			currentStudents.clear();
			String filter = programFilter != null ? (String) programFilter.getSelectedItem() : "All";
			java.util.List<String[]> dbStudents = StudentDAO.getAllStudents(filter);
			int idx = 1;
			for (String[] s : dbStudents) {

				currentStudents.add(s);
				tableModel.addRow(new Object[] { idx++, s[1], s[0], s[2], s[4], "Present" });
			}
			if (dbStudents.isEmpty()) {
				JLabel msg = new JLabel("No students found in database.");

			}
		}

		void clearStatuses() {
			for (int i = 0; i < tableModel.getRowCount(); i++)
				tableModel.setValueAt("Present", i, 5);
		}

		void saveSession() {
			if (table.isEditing())
				table.getCellEditor().stopCellEditing();
			String name = sessionNameField.getText().trim();
			if (name.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Please enter a session name.", "Missing Info",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			Date date = (Date) dateSpinner.getValue();

			if (date.after(new Date())) {
				JOptionPane.showMessageDialog(this, "Session date cannot be in the future.", "Invalid Date",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			java.util.List<int[]> studentIds = new java.util.ArrayList<>();
			java.util.List<String> statuses = new java.util.ArrayList<>();
			StringBuilder sb = new StringBuilder();
			sb.append("Session: ").append(name).append("\nDate: ")
					.append(new SimpleDateFormat("MMM dd, yyyy").format(date)).append("\n\n");

			for (int i = 0; i < tableModel.getRowCount(); i++) {
				String sidStr = tableModel.getValueAt(i, 2).toString();
				String status = tableModel.getValueAt(i, 5).toString();
				try {
					studentIds.add(new int[] { Integer.parseInt(sidStr) });
					statuses.add(status);
					sb.append(tableModel.getValueAt(i, 1)).append(" — ").append(status).append("\n");
				} catch (NumberFormatException ignored) {
				}
			}

			int saved = AttendanceDAO.saveSession(name, date, studentIds, statuses, loggedByUserId);
			JOptionPane.showMessageDialog(this,
					saved + " attendance records saved to database.\n\n"
							+ sb.toString().substring(0, Math.min(300, sb.length())),
					"Session Saved", JOptionPane.INFORMATION_MESSAGE);
		}

		static class FeedbackStudentPanel extends JPanel {
			String studentId;

			FeedbackStudentPanel(String studentId) {
				this.studentId = studentId;
				setLayout(new BorderLayout());
				setBackground(BG);

				JPanel header = new JPanel();
				header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
				header.setOpaque(false);
				header.setBorder(BorderFactory.createEmptyBorder(28, 28, 20, 28));

				JLabel title = new JLabel("Feedback");
				title.setFont(new Font("SansSerif", Font.BOLD, 20));
				title.setForeground(TEXT_MAIN);
				title.setAlignmentX(Component.LEFT_ALIGNMENT);

				JLabel sub = new JLabel("Submit concerns or suggestions to the admin");
				sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
				sub.setForeground(TEXT_MUTED);
				sub.setAlignmentX(Component.LEFT_ALIGNMENT);

				header.add(title);
				header.add(Box.createVerticalStrut(3));
				header.add(sub);

				JPanel form = new JPanel();
				form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
				form.setBackground(WHITE);
				form.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

				JComboBox<String> typeCombo = new JComboBox<>(
						new String[] { "General", "Attendance", "Grades", "Technical", "Other" });
				typeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
				typeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

				JTextField subjectField = new JTextField();
				subjectField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
				subjectField.setAlignmentX(Component.LEFT_ALIGNMENT);
				subjectField
						.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true),
								BorderFactory.createEmptyBorder(8, 12, 8, 12)));

				JTextArea bodyArea = new JTextArea(5, 20);
				bodyArea.setLineWrap(true);
				bodyArea.setWrapStyleWord(true);
				bodyArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
				bodyArea.setForeground(TEXT_MAIN);
				bodyArea.setBackground(WHITE);
				bodyArea.setCaretColor(ACCENT);
				bodyArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true),
						BorderFactory.createEmptyBorder(8, 12, 8, 12)));
				JScrollPane bodyScroll = new JScrollPane(bodyArea);
				bodyScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
				
				JLabel statusMsg = new JLabel(" ");
				statusMsg.setFont(new Font("SansSerif", Font.PLAIN, 12));
				statusMsg.setAlignmentX(Component.LEFT_ALIGNMENT);

				JButton submitBtn = makeBtn("Submit Feedback", NAVY, WHITE, 8);
				submitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
				submitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
				submitBtn.addActionListener(e -> {
					String subject = subjectField.getText().trim();
					String body = bodyArea.getText().trim();
					if (subject.isEmpty() || body.isEmpty()) {
						statusMsg.setForeground(RED);
						statusMsg.setText("Subject and message are required.");
						return;
					}

					String type = (String) typeCombo.getSelectedItem();
					boolean saved = FeedbackDAO.submitFeedback(Util.toInt(studentId), type, subject, body);
					if (saved) {
						statusMsg.setForeground(GREEN);
						statusMsg.setText("Feedback submitted successfully!");
						subjectField.setText("");
						bodyArea.setText("");
					} else {
						statusMsg.setForeground(RED);
						statusMsg.setText("Failed to submit. Please try again.");
					}
					bodyArea.setText("");
				});

				form.add(comboRow("Type", typeCombo));
				form.add(Box.createVerticalStrut(10));
				form.add(formRow("Subject", subjectField));
				form.add(Box.createVerticalStrut(10));
				JLabel bodyLbl = new JLabel("MESSAGE");
				bodyLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
				bodyLbl.setForeground(TEXT_SUB);
				bodyLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
				form.add(bodyLbl);
				form.add(Box.createVerticalStrut(4));
				form.add(bodyScroll);
				form.add(Box.createVerticalStrut(16));
				form.add(submitBtn);
				form.add(Box.createVerticalStrut(8));
				form.add(statusMsg);

				add(header, BorderLayout.NORTH);
				add(form, BorderLayout.CENTER);
			}

			private Component comboRow(String string, JComboBox<String> typeCombo) {

				return null;
			}

		}

		static class ROTCPerformanceAdminPanel extends JPanel {
			DefaultTableModel model;
			JComboBox<String> studentCombo;
			JTextField drillNameField, remarksField;
			JComboBox<String> ratingCombo;
			JSpinner dateSpinner;

			ROTCPerformanceAdminPanel() {
				setLayout(new BorderLayout(0, 0));
				setBackground(BG);
				add(buildForm(), BorderLayout.WEST);
				add(buildLog(), BorderLayout.CENTER);
			}

			JPanel buildForm() {
				JPanel outer = new JPanel(new BorderLayout());
				outer.setBackground(WHITE);
				outer.setPreferredSize(new Dimension(360, 0));
				outer.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

				JPanel inner = new JPanel();
				inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
				inner.setBackground(WHITE);
				inner.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

				JLabel title = new JLabel("ROTC Performance");
				title.setFont(new Font("SansSerif", Font.BOLD, 20));
				title.setForeground(TEXT_MAIN);
				title.setAlignmentX(Component.LEFT_ALIGNMENT);

				JLabel sub = new JLabel("Record training performance ratings for cadets");
				sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
				sub.setForeground(TEXT_MUTED);
				sub.setAlignmentX(Component.LEFT_ALIGNMENT);

				inner.add(title);
				inner.add(Box.createVerticalStrut(4));
				inner.add(sub);
				inner.add(Box.createVerticalStrut(22));
				inner.add(sep());
				inner.add(Box.createVerticalStrut(20));

				studentCombo = buildStudentCombo("ROTC");
				inner.add(comboRow("Cadet", studentCombo));
				inner.add(Box.createVerticalStrut(13));

				drillNameField = formField();
				inner.add(formRow("Drill / Exercise Name", drillNameField));
				ratingCombo = new JComboBox<>(new String[] { "Excellent", "Proficient", "Satisfactory",
						"Needs_Improvement", "Unsatisfactory" });
				ratingCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
				ratingCombo.setBackground(java.awt.Color.WHITE);
				ratingCombo.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 40));
				inner.add(comboRow("Rating", ratingCombo));
				inner.add(Box.createVerticalStrut(13));

				remarksField = formField();
				inner.add(formRow("Remarks (optional)", remarksField));
				inner.add(Box.createVerticalStrut(13));

				Date today = new Date();
				SpinnerDateModel dm = new SpinnerDateModel(today, null, null, Calendar.DAY_OF_MONTH);
				dateSpinner = new JSpinner(dm);
				JSpinner.DateEditor de = new JSpinner.DateEditor(dateSpinner, "MMM dd, yyyy");
				dateSpinner.setEditor(de);
				de.getTextField().setEditable(false);
				dateSpinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
				dateSpinner.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
				inner.add(spinnerRow("Date", dateSpinner));
				inner.add(Box.createVerticalStrut(20));

				JButton addBtn = makeBtn("Save Performance Record", GOLD, NAVY, 8);
				addBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
				addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
				addBtn.addActionListener(e -> doAdd());
				inner.add(addBtn);

				outer.add(inner, BorderLayout.CENTER);
				return outer;
			}

			JPanel buildLog() {
				JPanel panel = new JPanel(new BorderLayout());
				panel.setBackground(BG);

				JPanel header = new JPanel(new BorderLayout());
				header.setOpaque(false);
				header.setBorder(BorderFactory.createEmptyBorder(28, 24, 14, 24));

				JLabel t = new JLabel("Performance Log");
				t.setFont(new Font("SansSerif", Font.BOLD, 16));
				t.setForeground(TEXT_MAIN);

				JButton refreshBtn = makeBtn("Refresh", BG, TEXT_SUB, 6);
				refreshBtn.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
				refreshBtn.setPreferredSize(new Dimension(80, 28));
				refreshBtn.addActionListener(e -> refreshLog());

				header.add(t, BorderLayout.WEST);
				header.add(refreshBtn, BorderLayout.EAST);

				String[] cols = { "Student ID", "Name", "Rating", "Remarks", "Date" };
				model = new DefaultTableModel(cols, 0) {
					public boolean isCellEditable(int r, int c) {
						return false;
					}
				};
				JTable tbl = makeTable(model);

				JPanel wrap = new JPanel(new BorderLayout());
				wrap.setBackground(BG);
				wrap.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
				wrap.add(wrapTable(tbl), BorderLayout.CENTER);

				panel.add(header, BorderLayout.NORTH);
				panel.add(wrap, BorderLayout.CENTER);
				refreshLog();
				return panel;
			}

			void refreshLog() {
				model.setRowCount(0);
				String sql = "SELECT p.entry_id, s.student_id, s.full_name, "
						+ "p.drill_name, p.rating, p.remarks, p.drill_date " + "FROM performance p "
						+ "JOIN students s ON p.student_id = s.student_id " + "WHERE s.is_deleted = 0 "
						+ "ORDER BY p.drill_date DESC";
				try (java.sql.Connection conn = util.DBConnection.getConnection()) {
					if (conn == null)
						return;
					try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
							java.sql.ResultSet rs = ps.executeQuery()) {
						while (rs.next()) {
							model.addRow(new Object[] { rs.getString("student_id"), rs.getString("full_name"),
									rs.getString("rating"),
									rs.getString("remarks") != null ? rs.getString("remarks") : "—",
									rs.getString("drill_date") });
						}
					}
				} catch (java.sql.SQLException ex) {
					ex.printStackTrace();
				}
			}

			void doAdd() {
				if (studentCombo.getSelectedIndex() < 0) {
					JOptionPane.showMessageDialog(this, "No ROTC cadets available.", "Error",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
				String drillName = drillNameField.getText().trim();
				String rating = (String) ratingCombo.getSelectedItem();
				String remarks = remarksField.getText().trim();
				if (drillName.isEmpty()) {
					JOptionPane.showMessageDialog(this, "Drill name is required.", "Missing",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
				String combo = (String) studentCombo.getSelectedItem();
				int sid = Util.toInt(combo.split(" — ")[0].trim());
				String dateStr = new SimpleDateFormat("yyyy-MM-dd").format((Date) dateSpinner.getValue());

				String sql = "INSERT INTO performance (student_id, drill_name, drill_date, rating, remarks, officer_name) VALUES (?, ?, ?, ?, ?, ?)";
				try (java.sql.Connection conn = util.DBConnection.getConnection();
						java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
					ps.setInt(1, sid);
					ps.setString(2, drillName);
					ps.setString(3, dateStr);
					ps.setString(4, rating);
					ps.setString(5, remarks.isEmpty() ? null : remarks);
					ps.setNull(6, java.sql.Types.VARCHAR);
					ps.executeUpdate();
				} catch (java.sql.SQLException ex) {
					JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				drillNameField.setText("");
				remarksField.setText("");
				refreshLog();
				JOptionPane.showMessageDialog(this, "Performance record saved.", "Saved",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	static class PointsAdminPanel extends JPanel {
		DefaultTableModel model;
		JComboBox<String> studentCombo;
		JTextField pointsField, reasonField;
		JComboBox<String> typeCombo;

		PointsAdminPanel() {
			setLayout(new BorderLayout(0, 0));
			setBackground(BG);
			add(buildForm(), BorderLayout.WEST);
			add(buildLog(), BorderLayout.CENTER);
		}

		JPanel buildForm() {
			JPanel outer = new JPanel(new BorderLayout());
			outer.setBackground(WHITE);
			outer.setPreferredSize(new Dimension(360, 0));
			outer.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

			JPanel inner = new JPanel();
			inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
			inner.setBackground(WHITE);
			inner.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

			JLabel title = new JLabel("Points Management");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);
			title.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel sub = new JLabel("Add or deduct points for any student");
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);
			sub.setAlignmentX(Component.LEFT_ALIGNMENT);

			inner.add(title);
			inner.add(Box.createVerticalStrut(4));
			inner.add(sub);
			inner.add(Box.createVerticalStrut(22));
			inner.add(sep());
			inner.add(Box.createVerticalStrut(20));

			studentCombo = buildStudentCombo("All");
			inner.add(comboRow("Student", studentCombo));
			inner.add(Box.createVerticalStrut(13));

			typeCombo = new JComboBox<>(new String[] { "award", "deduct" });
			typeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
			typeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
			inner.add(comboRow("Transaction Type", typeCombo));
			inner.add(Box.createVerticalStrut(13));

			pointsField = formField();
			inner.add(formRow("Points (whole number)", pointsField));
			inner.add(Box.createVerticalStrut(13));

			reasonField = formField();
			inner.add(formRow("Reason / Notes", reasonField));
			inner.add(Box.createVerticalStrut(20));

			JButton saveBtn = makeBtn("Save Transaction", ACCENT, WHITE, 8);
			saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
			saveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
			saveBtn.addActionListener(e -> doSave());
			inner.add(saveBtn);

			outer.add(inner, BorderLayout.CENTER);
			return outer;
		}

		JPanel buildLog() {
			JPanel panel = new JPanel(new BorderLayout());
			panel.setBackground(BG);

			JPanel header = new JPanel(new BorderLayout());
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(28, 24, 14, 24));

			JLabel t = new JLabel("Points Ledger");
			t.setFont(new Font("SansSerif", Font.BOLD, 16));
			t.setForeground(TEXT_MAIN);

			JButton refreshBtn = makeBtn("Refresh", BG, TEXT_SUB, 6);
			refreshBtn.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
			refreshBtn.setPreferredSize(new Dimension(80, 28));
			refreshBtn.addActionListener(e -> refreshLog());

			header.add(t, BorderLayout.WEST);
			header.add(refreshBtn, BorderLayout.EAST);

			String[] cols = { "Student ID", "Name", "Program", "Type", "Points", "Reason", "Date" };
			model = new DefaultTableModel(cols, 0) {
				public boolean isCellEditable(int r, int c) {
					return false;
				}
			};
			JTable tbl = makeTable(model);
			tbl.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row,
						int col) {
					super.getTableCellRendererComponent(t, v, sel, foc, row, col);
					String type = model.getValueAt(row, 3) != null ? model.getValueAt(row, 3).toString() : "";
					setForeground(type.equals("Earned") || type.equals("Bonus") ? GREEN : RED);
					setFont(new Font("SansSerif", Font.BOLD, 12));
					setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
					setBackground(sel ? ACCENT_SOFT : row % 2 == 0 ? WHITE : BG);
					setOpaque(true);
					return this;
				}
			});

			JPanel wrap = new JPanel(new BorderLayout());
			wrap.setBackground(BG);
			wrap.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
			wrap.add(wrapTable(tbl), BorderLayout.CENTER);

			panel.add(header, BorderLayout.NORTH);
			panel.add(wrap, BorderLayout.CENTER);
			refreshLog();
			return panel;
		}

		void refreshLog() {
			model.setRowCount(0);
			String sql = "SELECT p.transaction_id, s.student_id, s.full_name, s.program, "
					+ "p.type, p.amount, p.reason, p.transaction_date "
					+ "FROM points p JOIN students s ON p.student_id = s.student_id "
					+ "WHERE s.is_deleted = 0 ORDER BY p.transaction_date DESC";
			try (java.sql.Connection conn = util.DBConnection.getConnection()) {
				if (conn == null)
					return;
				try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
						java.sql.ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						model.addRow(new Object[] { rs.getString("student_id"), rs.getString("full_name"),
								rs.getString("program"), rs.getString("type"), rs.getString("amount"),
								rs.getString("reason") != null ? rs.getString("reason") : "—",
								rs.getString("transaction_date") });
					}
				}
			} catch (java.sql.SQLException ex) {
				ex.printStackTrace();
			}
		}

		void doSave() {
			if (studentCombo.getSelectedIndex() < 0) {
				JOptionPane.showMessageDialog(this, "No students available.", "Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			String ptsStr = pointsField.getText().trim();
			String reason = reasonField.getText().trim();
			if (ptsStr.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Points value is required.", "Missing",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			int pts;
			try {
				pts = Integer.parseInt(ptsStr);
				if (pts <= 0)
					throw new NumberFormatException();
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "Points must be a positive whole number.", "Invalid",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			String combo = (String) studentCombo.getSelectedItem();
			int sid = Util.toInt(combo.split(" — ")[0].trim());
			String type = (String) typeCombo.getSelectedItem();
			String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

			String sql = "INSERT INTO points (student_id, type, amount, reason, transaction_date) VALUES (?,?,?,?,?)";
			try (java.sql.Connection conn = util.DBConnection.getConnection();
					java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, sid);
				ps.setString(2, type);
				ps.setInt(3, pts);
				ps.setString(4, reason.isEmpty() ? null : reason);
				ps.setString(5, today);
				ps.executeUpdate();
			} catch (java.sql.SQLException ex) {
				JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			pointsField.setText("");
			reasonField.setText("");
			refreshLog();
			JOptionPane.showMessageDialog(this, "Points transaction saved.", "Saved", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	static class LeaderboardPanel extends JPanel {
		DefaultTableModel rotcModel, cwtsModel;

		LeaderboardPanel() {
			setLayout(new BorderLayout());
			setBackground(BG);

			JPanel header = new JPanel();
			header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(28, 28, 16, 28));
			JLabel title = new JLabel("Leaderboard");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);
			title.setAlignmentX(Component.LEFT_ALIGNMENT);
			JLabel sub = new JLabel("Top 10 students by total points per program");
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);
			sub.setAlignmentX(Component.LEFT_ALIGNMENT);
			header.add(title);
			header.add(Box.createVerticalStrut(3));
			header.add(sub);

			JButton refreshBtn = makeBtn("Refresh", BG, TEXT_SUB, 6);
			refreshBtn.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
			refreshBtn.setPreferredSize(new Dimension(90, 30));
			refreshBtn.addActionListener(e -> {
				loadLeaderboard(rotcModel, "ROTC");
				loadLeaderboard(cwtsModel, "CWTS");
			});
			JPanel headerRow = new JPanel(new BorderLayout());
			headerRow.setOpaque(false);
			headerRow.setBorder(BorderFactory.createEmptyBorder(28, 28, 16, 28));
			headerRow.add(header, BorderLayout.CENTER);
			headerRow.add(refreshBtn, BorderLayout.EAST);

			String[] cols = { "Rank", "Student ID", "Name", "Section", "Total Points" };
			rotcModel = new DefaultTableModel(cols, 0) {
				public boolean isCellEditable(int r, int c) {
					return false;
				}
			};
			cwtsModel = new DefaultTableModel(cols, 0) {
				public boolean isCellEditable(int r, int c) {
					return false;
				}
			};
			JTable rotcTable = makeTable(rotcModel);
			JTable cwtsTable = makeTable(cwtsModel);

			JPanel tablesRow = new JPanel(new GridLayout(1, 2, 16, 0));
			tablesRow.setBackground(BG);
			tablesRow.setBorder(BorderFactory.createEmptyBorder(0, 28, 28, 28));

			JPanel rotcPanel = new JPanel(new BorderLayout());
			rotcPanel.setBackground(BG);
			JLabel rotcLbl = new JLabel("ROTC");
			rotcLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
			rotcLbl.setForeground(GOLD);
			rotcLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
			rotcPanel.add(rotcLbl, BorderLayout.NORTH);
			rotcPanel.add(wrapTable(rotcTable), BorderLayout.CENTER);

			JPanel cwtsPanel = new JPanel(new BorderLayout());
			cwtsPanel.setBackground(BG);
			JLabel cwtsLbl = new JLabel("CWTS");
			cwtsLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
			cwtsLbl.setForeground(GREEN);
			cwtsLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
			cwtsPanel.add(cwtsLbl, BorderLayout.NORTH);
			cwtsPanel.add(wrapTable(cwtsTable), BorderLayout.CENTER);

			tablesRow.add(rotcPanel);
			tablesRow.add(cwtsPanel);

			loadLeaderboard(rotcModel, "ROTC");
			loadLeaderboard(cwtsModel, "CWTS");

			add(headerRow, BorderLayout.NORTH);
			add(tablesRow, BorderLayout.CENTER);
		}

		void loadLeaderboard(DefaultTableModel model, String program) {
			model.setRowCount(0);
			String sql = "SELECT s.student_id, s.full_name, s.section, "
					+ "COALESCE(SUM(CASE WHEN p.type IN ('award') THEN p.amount "
					+ "              WHEN p.type IN ('deduct') THEN -p.amount ELSE 0 END), 0) AS total "
					+ "FROM students s LEFT JOIN points p ON s.student_id = p.student_id "
					+ "WHERE s.is_deleted = 0 AND s.program = ? " + "GROUP BY s.student_id, s.full_name, s.section "
					+ "ORDER BY total DESC LIMIT 10";
			try (java.sql.Connection conn = util.DBConnection.getConnection()) {
				if (conn == null)
					return;
				try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
					ps.setString(1, program);
					java.sql.ResultSet rs = ps.executeQuery();
					int rank = 1;
					while (rs.next()) {
						model.addRow(new Object[] { rank++, rs.getString("student_id"), rs.getString("full_name"),
								rs.getString("section") != null ? rs.getString("section") : "—", rs.getInt("total") });
					}
				}
			} catch (java.sql.SQLException e) {
				e.printStackTrace();
			}
		}
	}

	static class FeedbackAdminPanel extends JPanel {
		DefaultTableModel model;
		JTextArea detailArea;

		FeedbackAdminPanel() {
			setLayout(new BorderLayout(0, 0));
			setBackground(BG);

			JPanel header = new JPanel(new BorderLayout());
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(28, 28, 16, 28));

			JLabel title = new JLabel("Student Feedback");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);

			JButton refreshBtn = makeBtn("Refresh", BG, TEXT_SUB, 6);
			refreshBtn.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
			refreshBtn.setPreferredSize(new Dimension(90, 30));
			refreshBtn.addActionListener(e -> refreshLog());

			header.add(title, BorderLayout.WEST);
			header.add(refreshBtn, BorderLayout.EAST);

			String[] cols = { "ID", "Student", "Type", "Subject", "Date", "Status" };
			model = new DefaultTableModel(cols, 0) {
				public boolean isCellEditable(int r, int c) {
					return false;
				}
			};
			JTable tbl = makeTable(model);
			tbl.getColumnModel().getColumn(0).setPreferredWidth(40);
			tbl.getColumnModel().getColumn(0).setMaxWidth(50);
			tbl.getSelectionModel().addListSelectionListener(e -> {
				int row = tbl.getSelectedRow();
				if (row >= 0)
					showDetail(row);
			});

			detailArea = new JTextArea(5, 30);
			detailArea.setEditable(false);
			detailArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
			detailArea.setLineWrap(true);
			detailArea.setWrapStyleWord(true);
			detailArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
					BorderFactory.createEmptyBorder(12, 16, 12, 16)));

			JPanel tableWrap = new JPanel(new BorderLayout());
			tableWrap.setBackground(BG);
			tableWrap.setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 28));
			tableWrap.add(wrapTable(tbl), BorderLayout.CENTER);
			tableWrap.add(detailArea, BorderLayout.SOUTH);

			add(header, BorderLayout.NORTH);
			add(tableWrap, BorderLayout.CENTER);
			refreshLog();
		}

		void refreshLog() {
			model.setRowCount(0);
			String sql = "SELECT f.feedback_id, s.full_name, f.type, "
					+ "f.subject, f.submitted_date, f.status, f.body "
					+ "FROM feedback f JOIN students s ON f.student_id = s.student_id "
					+ "ORDER BY f.submitted_date DESC";
			try (java.sql.Connection conn = util.DBConnection.getConnection()) {
				if (conn == null)
					return;
				try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
						java.sql.ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						model.addRow(new Object[] { rs.getInt("feedback_id"), rs.getString("full_name"),
								rs.getString("type"), rs.getString("subject"),
								rs.getString("submitted_date") != null ? rs.getString("submitted_date").substring(0, 10)
										: "—",
								rs.getString("status"), rs.getString("body") });
					}
				}
			} catch (java.sql.SQLException ex) {
				ex.printStackTrace();
			}
		}

		void showDetail(int row) {
			if (row < 0 || row >= model.getRowCount())
				return;
			Object msg = model.getValueAt(row, 6);
			String subject = model.getValueAt(row, 3).toString();
			String student = model.getValueAt(row, 1).toString();
			detailArea.setText("From: " + student + "   |   Subject: " + subject + "\n\n"
					+ (msg != null ? msg.toString() : "(no message)"));
		}
	}

	static class ReportsPanel extends JPanel {
		JTextArea outputArea;

		ReportsPanel() {
			setLayout(new BorderLayout(0, 0));
			setBackground(BG);

			JPanel header = new JPanel(new BorderLayout());
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(28, 28, 16, 28));

			JLabel title = new JLabel("Reports");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);

			JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
			btnRow.setOpaque(false);

			JButton genBtn = makeBtn("Generate Summary", ACCENT, WHITE, 8);
			genBtn.setPreferredSize(new Dimension(170, 34));
			genBtn.addActionListener(e -> generateReport());

			JButton saveBtn = makeBtn("Save to File", BG, TEXT_SUB, 8);
			saveBtn.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
			saveBtn.setPreferredSize(new Dimension(120, 34));
			saveBtn.addActionListener(e -> saveReport());

			btnRow.add(saveBtn);
			btnRow.add(genBtn);
			header.add(title, BorderLayout.WEST);
			header.add(btnRow, BorderLayout.EAST);

			outputArea = new JTextArea();
			outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
			outputArea.setEditable(false);
			outputArea.setBackground(new Color(245, 247, 250));
			outputArea.setForeground(TEXT_MAIN);
			outputArea.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
			outputArea.setText("Click 'Generate Summary' to load the report.");

			JScrollPane scroll = new JScrollPane(outputArea);
			scroll.setBorder(BorderFactory.createEmptyBorder(0, 28, 28, 28));

			add(header, BorderLayout.NORTH);
			add(scroll, BorderLayout.CENTER);
		}

		void generateReport() {
			outputArea.setText("Generating report…");
			StringBuilder sb = new StringBuilder();
			String line = "=".repeat(60) + "\n";
			sb.append(line);
			sb.append("  NSTP INTEGRATED INFORMATION SYSTEM — SUMMARY REPORT\n");
			sb.append("  Generated: ").append(new SimpleDateFormat("MMMM dd, yyyy  hh:mm a").format(new Date()))
					.append("\n");
			sb.append(line).append("\n");

			String sql;
			try (java.sql.Connection conn = util.DBConnection.getConnection()) {
				if (conn == null) {
					outputArea.setText("Cannot connect to database.");
					return;
				}

				sql = "SELECT COUNT(*) AS c FROM students WHERE is_deleted=0";
				try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
						java.sql.ResultSet rs = ps.executeQuery()) {
					if (rs.next())
						sb.append("Total Active Students   : ").append(rs.getInt("c")).append("\n");
				}
				sql = "SELECT program, COUNT(*) AS c FROM students WHERE is_deleted=0 GROUP BY program";
				try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
						java.sql.ResultSet rs = ps.executeQuery()) {
					while (rs.next())
						sb.append("  ").append(rs.getString("program")).append("\t\t: ").append(rs.getInt("c"))
								.append(" students\n");
				}
				sb.append("\n");

				sql = "SELECT COUNT(*) AS c FROM attendance";
				try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
						java.sql.ResultSet rs = ps.executeQuery()) {
					if (rs.next())
						sb.append("Total Attendance Logs   : ").append(rs.getInt("c")).append("\n");
				}
				sql = "SELECT status, COUNT(*) AS c FROM attendance GROUP BY status";
				try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
						java.sql.ResultSet rs = ps.executeQuery()) {
					while (rs.next())
						sb.append("  ").append(rs.getString("status")).append("\t\t: ").append(rs.getInt("c"))
								.append("\n");
				}
				sb.append("\n");

				sql = "SELECT status, COUNT(*) AS c FROM disputes GROUP BY status";
				sb.append("Disputes by Status:\n");
				try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
						java.sql.ResultSet rs = ps.executeQuery()) {
					while (rs.next())
						sb.append("  ").append(rs.getString("status")).append("\t\t: ").append(rs.getInt("c"))
								.append("\n");
				}
				sb.append("\n");

				sql = "SELECT COUNT(*) AS c, COALESCE(SUM(hours_rendered),0) AS h FROM engagement";
				try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
						java.sql.ResultSet rs = ps.executeQuery()) {
					if (rs.next())
						sb.append("CWTS Engagement Records : ").append(rs.getInt("c")).append(" (")
								.append(rs.getDouble("h")).append(" total hours)\n");
				}

				sql = "SELECT COUNT(*) AS c FROM demerits WHERE status='Active'";
				try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
						java.sql.ResultSet rs = ps.executeQuery()) {
					if (rs.next())
						sb.append("Active ROTC Demerits    : ").append(rs.getInt("c")).append("\n");
				}

				sql = "SELECT COALESCE(SUM(amount),0) AS t FROM points WHERE type IN ('award')";
				try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
						java.sql.ResultSet rs = ps.executeQuery()) {
					if (rs.next())
						sb.append("Total Points Awarded    : ").append(rs.getInt("t")).append(" pts\n");
				}

				sql = "SELECT COUNT(*) AS c FROM feedback WHERE status='Open'";
				try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
						java.sql.ResultSet rs = ps.executeQuery()) {
					if (rs.next())
						sb.append("Open Feedback Items     : ").append(rs.getInt("c")).append("\n");
				}

			} catch (java.sql.SQLException ex) {
				sb.append("\nERROR reading database: ").append(ex.getMessage());
			}

			sb.append("\n").append("=".repeat(60)).append("\n");
			sb.append("END OF REPORT\n");
			outputArea.setText(sb.toString());
			outputArea.setCaretPosition(0);
		}

		void saveReport() {
			String text = outputArea.getText();
			if (text.isEmpty() || text.startsWith("Click")) {
				JOptionPane.showMessageDialog(this, "Generate a report first.", "Nothing to save",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			try {
				java.io.File dir = new java.io.File(System.getProperty("user.home"), "NSTP_Exports");
				if (!dir.exists())
					dir.mkdirs();
				String fname = "NSTP_Report_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
				java.io.File f = new java.io.File(dir, fname);
				java.nio.file.Files.writeString(f.toPath(), text);
				JOptionPane.showMessageDialog(this, "Saved to:\n" + f.getAbsolutePath(), "Saved",
						JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	static class DisputePanel extends JPanel {
		DefaultTableModel pendingModel, historyModel;
		JTable pendingTable, historyTable;

		DisputePanel() {
			setLayout(new BorderLayout(0, 0));
			setBackground(BG);
			add(buildTop(), BorderLayout.NORTH);
			add(buildTables(), BorderLayout.CENTER);
		}

		JPanel buildTop() {
			JPanel top = new JPanel();
			top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
			top.setOpaque(false);
			top.setBorder(BorderFactory.createEmptyBorder(28, 28, 16, 28));

			JLabel title = new JLabel("Dispute Management");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);
			title.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel sub = new JLabel("Review and resolve student attendance disputes");
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);
			sub.setAlignmentX(Component.LEFT_ALIGNMENT);

			top.add(title);
			top.add(Box.createVerticalStrut(3));
			top.add(sub);
			return top;
		}

		JPanel buildTables() {
			JPanel body = new JPanel(new GridLayout(2, 1, 0, 1));
			body.setBackground(BG);
			body.setBorder(BorderFactory.createEmptyBorder(0, 28, 28, 28));
			body.add(buildPendingSection());
			body.add(buildHistorySection());
			return body;
		}

		JPanel buildPendingSection() {
			JPanel section = new JPanel(new BorderLayout());
			section.setBackground(BG);

			JPanel header = new JPanel(new BorderLayout());
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

			JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			titleRow.setOpaque(false);
			JLabel t = new JLabel("Pending Disputes");
			t.setFont(new Font("SansSerif", Font.BOLD, 14));
			t.setForeground(TEXT_MAIN);
			titleRow.add(t);
			titleRow.add(Box.createHorizontalStrut(10));
			titleRow.add(tag("Requires Action", GOLD_SOFT, GOLD));

			JButton refreshBtn = makeBtn("Refresh", BG, TEXT_SUB, 6);
			refreshBtn.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
			refreshBtn.setPreferredSize(new Dimension(80, 28));
			refreshBtn.addActionListener(e -> refreshAll());

			header.add(titleRow, BorderLayout.WEST);
			header.add(refreshBtn, BorderLayout.EAST);

			String[] cols = { "Student ID", "Name", "Program", "Date", "Session", "Original Status", "Reason" };
			pendingModel = new DefaultTableModel(cols, 0) {
				public boolean isCellEditable(int r, int c) {
					return false;
				}
			};
			pendingTable = makeStyledTable(pendingModel);
			pendingTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row,
						int col) {
					super.getTableCellRendererComponent(t, v, sel, foc, row, col);
					String s = v != null ? v.toString() : "";
					setForeground(s.equals("Absent") ? RED : GOLD);
					setFont(new Font("SansSerif", Font.BOLD, 11));
					setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
					setBackground(sel ? ACCENT_SOFT : row % 2 == 0 ? WHITE : BG);
					setOpaque(true);
					return this;
				}
			});

			JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
			actionBar.setOpaque(false);
			actionBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));

			JButton approveBtn = makeBtn("Approve → Excused", GREEN, WHITE, 8);
			approveBtn.setPreferredSize(new Dimension(180, 36));
			approveBtn.addActionListener(e -> resolveSelected("Approved"));

			JButton rejectBtn = makeBtn("Reject", RED, WHITE, 8);
			rejectBtn.setPreferredSize(new Dimension(90, 36));
			rejectBtn.addActionListener(e -> resolveSelected("Rejected"));

			actionBar.add(rejectBtn);
			actionBar.add(approveBtn);

			JPanel hint = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
			hint.setOpaque(false);
			JLabel hintLbl = new JLabel("Select a row then click Approve or Reject");
			hintLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
			hintLbl.setForeground(TEXT_MUTED);
			hint.add(hintLbl);

			JPanel bottom = new JPanel(new BorderLayout());
			bottom.setOpaque(false);
			bottom.add(hint, BorderLayout.WEST);
			bottom.add(actionBar, BorderLayout.EAST);

			section.add(header, BorderLayout.NORTH);
			section.add(wrapTable(pendingTable), BorderLayout.CENTER);
			section.add(bottom, BorderLayout.SOUTH);

			refreshPending();
			return section;
		}

		JPanel buildHistorySection() {
			JPanel section = new JPanel(new BorderLayout());
			section.setBackground(BG);
			section.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

			JPanel header = new JPanel(new BorderLayout());
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

			JLabel t = new JLabel("Resolved Disputes");
			t.setFont(new Font("SansSerif", Font.BOLD, 14));
			t.setForeground(TEXT_MAIN);
			header.add(t, BorderLayout.WEST);

			String[] cols = { "Student ID", "Name", "Program", "Date", "Session", "Original", "Resolution", "Reason" };
			historyModel = new DefaultTableModel(cols, 0) {
				public boolean isCellEditable(int r, int c) {
					return false;
				}
			};
			historyTable = makeStyledTable(historyModel);
			historyTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row,
						int col) {
					super.getTableCellRendererComponent(t, v, sel, foc, row, col);
					String s = v != null ? v.toString() : "";
					setForeground(s.equals("Approved") ? GREEN : RED);
					setFont(new Font("SansSerif", Font.BOLD, 11));
					setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
					setBackground(sel ? ACCENT_SOFT : row % 2 == 0 ? WHITE : BG);
					setOpaque(true);
					return this;
				}
			});

			section.add(header, BorderLayout.NORTH);
			section.add(wrapTable(historyTable), BorderLayout.CENTER);
			refreshHistory();
			return section;
		}

		JTable makeStyledTable(DefaultTableModel m) {
			JTable t = new JTable(m) {
				@Override
				public Component prepareRenderer(TableCellRenderer r, int row, int col) {
					Component c = super.prepareRenderer(r, row, col);
					c.setBackground(isRowSelected(row) ? ACCENT_SOFT : row % 2 == 0 ? WHITE : BG);
					c.setForeground(TEXT_MAIN);
					((JComponent) c).setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
					return c;
				}
			};
			styleTable(t);
			return t;
		}

		void refreshPending() {
			pendingModel.setRowCount(0);

			java.util.List<String[]> disputes = dao.DisputeDAO.getDisputesByStatus("Pending");

	        for (String[] d : disputes) {
	            pendingModel.addRow(new Object[] { d[1], d[2], d[4], d[6], d[5], d[3], d[3] });
	        }
		}

		void refreshHistory() {
			historyModel.setRowCount(0);

			java.util.List<String[]> disputes = dao.DisputeDAO.getAllDisputes();

			for (String[] d : disputes) {
				if ("Pending".equals(d[7]))
					continue;

				historyModel.addRow(new Object[] { d[0], d[1], d[2], d[3], d[4], d[5], d[7], d[6] });
			}
		}

		void refreshAll() {
			refreshPending();
			refreshHistory();
		}

		void resolveSelected(String resolution) {
			int row = pendingTable.getSelectedRow();
			if (row < 0) {
				JOptionPane.showMessageDialog(this, "Please select a dispute to resolve.", "No Selection",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			String studentId = pendingModel.getValueAt(row, 0).toString();
			String date = pendingModel.getValueAt(row, 3).toString();
			String session = pendingModel.getValueAt(row, 4).toString();
			String confirmMsg = resolution.equals("Approved") ? "Approve this dispute and change attendance to Excused?"
					: "Reject this dispute? The original status will be kept.";
			int confirm = JOptionPane.showConfirmDialog(this, confirmMsg, "Confirm " + resolution,
					JOptionPane.YES_NO_OPTION);
			if (confirm != JOptionPane.YES_OPTION)
				return;

			int sid = Integer.parseInt(String.valueOf(studentId));

			boolean updated = dao.DisputeDAO.updateDisputeStatus(sid, date, session, resolution);

			if (updated && "Approved".equals(resolution)) {
				dao.AttendanceDAO.updateAttendanceStatus(sid, date, session, "Excused");
			}
			refreshAll();
			JOptionPane.showMessageDialog(this,
					"Dispute " + resolution.toLowerCase() + "."
							+ (resolution.equals("Approved") ? "\nAttendance updated to Excused." : ""),
					"Done", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	static class PushNotificationPanel extends JPanel {
		JTextField subjectField;
		JTextArea bodyArea;
		JComboBox<String> targetCombo;
		JPanel historyPanel;

		PushNotificationPanel() {
			setLayout(new BorderLayout(0, 0));
			setBackground(BG);
			add(buildCompose(), BorderLayout.WEST);
			add(buildHistory(), BorderLayout.CENTER);
		}

		JPanel buildCompose() {
			JPanel outer = new JPanel(new BorderLayout());
			outer.setBackground(BG);
			outer.setPreferredSize(new Dimension(440, 0));
			outer.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

			JPanel inner = new JPanel();
			inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
			inner.setBackground(WHITE);
			inner.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

			JLabel title = new JLabel("Push Notification");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);
			title.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel sub = new JLabel("Send announcements to ROTC, CWTS, or all students");
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);
			sub.setAlignmentX(Component.LEFT_ALIGNMENT);

			inner.add(title);
			inner.add(Box.createVerticalStrut(4));
			inner.add(sub);
			inner.add(Box.createVerticalStrut(24));
			inner.add(sep());
			inner.add(Box.createVerticalStrut(22));

			targetCombo = new JComboBox<>(new String[] { "ALL", "ROTC", "CWTS" });
			targetCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
			targetCombo.setBackground(WHITE);
			targetCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
			targetCombo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true),
					BorderFactory.createEmptyBorder(6, 10, 6, 10)));
			inner.add(composeRow("Target Audience", targetCombo));
			inner.add(Box.createVerticalStrut(14));

			subjectField = new JTextField();
			subjectField.setFont(new Font("SansSerif", Font.PLAIN, 13));
			subjectField.setForeground(TEXT_MAIN);
			subjectField.setBackground(WHITE);
			subjectField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
			subjectField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true),
					BorderFactory.createEmptyBorder(8, 12, 8, 12)));
			inner.add(composeRow("Subject", subjectField));
			inner.add(Box.createVerticalStrut(14));

			bodyArea = new JTextArea(6, 20);
			bodyArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
			bodyArea.setForeground(TEXT_MAIN);
			bodyArea.setBackground(WHITE);
			bodyArea.setLineWrap(true);
			bodyArea.setWrapStyleWord(true);
			bodyArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

			JScrollPane bodyScroll = new JScrollPane(bodyArea);
			bodyScroll.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
			bodyScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel bodyLbl = new JLabel("MESSAGE");
			bodyLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
			bodyLbl.setForeground(TEXT_SUB);
			bodyLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
			inner.add(bodyLbl);
			inner.add(Box.createVerticalStrut(4));
			inner.add(bodyScroll);
			inner.add(Box.createVerticalStrut(20));

			JButton sendBtn = makeBtn("Send Notification", ACCENT, WHITE, 8);
			sendBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
			sendBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
			sendBtn.addActionListener(e -> doSend());
			inner.add(sendBtn);

			outer.add(inner, BorderLayout.CENTER);
			return outer;
		}

		JPanel composeRow(String label, JComponent field) {
			JPanel p = new JPanel();
			p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
			p.setOpaque(false);
			p.setAlignmentX(Component.LEFT_ALIGNMENT);
			JLabel lbl = new JLabel(label.toUpperCase());
			lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
			lbl.setForeground(TEXT_SUB);
			lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
			field.setAlignmentX(Component.LEFT_ALIGNMENT);
			p.add(lbl);
			p.add(Box.createVerticalStrut(4));
			p.add(field);
			return p;
		}

		JPanel buildHistory() {
			JPanel outer = new JPanel(new BorderLayout());
			outer.setBackground(BG);

			JPanel headerBar = new JPanel(new BorderLayout());
			headerBar.setOpaque(false);
			headerBar.setBorder(BorderFactory.createEmptyBorder(28, 24, 14, 24));

			JLabel histTitle = new JLabel("Sent Notifications");
			histTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
			histTitle.setForeground(TEXT_MAIN);

			JLabel histSub = new JLabel("Recent broadcasts");
			histSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			histSub.setForeground(TEXT_MUTED);

			headerBar.add(histTitle, BorderLayout.WEST);
			headerBar.add(histSub, BorderLayout.EAST);

			historyPanel = new JPanel();
			historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
			historyPanel.setBackground(BG);
			historyPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));

			for (String[] notif : NOTIFICATIONS) {
				historyPanel.add(notifCard(notif, ACCENT, ACCENT_SOFT));
				historyPanel.add(Box.createVerticalStrut(10));
			}

			JScrollPane scroll = new JScrollPane(historyPanel);
			scroll.setBorder(BorderFactory.createEmptyBorder());
			scroll.getViewport().setBackground(BG);
			scroll.setBackground(BG);
			scroll.getVerticalScrollBar().setUnitIncrement(12);

			outer.add(headerBar, BorderLayout.NORTH);
			outer.add(scroll, BorderLayout.CENTER);
			return outer;
		}

		void doSend() {
			String subject = subjectField.getText().trim();
			String body = bodyArea.getText().trim();
			String target = (String) targetCombo.getSelectedItem();

			if (subject.isEmpty() || body.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Subject and message are required.", "Missing Fields",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			String dateStr = new SimpleDateFormat("MMM dd, yyyy").format(new Date());
			String[] newNotif = new String[] { target, dateStr, subject, body };
			NOTIFICATIONS.add(0, newNotif);

			String dbSql = "INSERT INTO announcements (title, body, audience) VALUES (?,?,?)";
			try (java.sql.Connection conn = util.DBConnection.getConnection()) {
				if (conn != null) {
					try (java.sql.PreparedStatement ps = conn.prepareStatement(dbSql)) {
						ps.setString(1, subject);
						ps.setString(2, body);
						ps.setString(3, target);
						ps.executeUpdate();
					}
				}
			} catch (java.sql.SQLException ex) {
				ex.printStackTrace();
			}

			historyPanel.removeAll();
			for (String[] notif : NOTIFICATIONS) {
				historyPanel.add(notifCard(notif, ACCENT, ACCENT_SOFT));
				historyPanel.add(Box.createVerticalStrut(10));
			}
			historyPanel.revalidate();
			historyPanel.repaint();

			subjectField.setText("");
			bodyArea.setText("");

			JOptionPane.showMessageDialog(this, "Notification sent to: " + target + "\nSubject: " + subject,
					"Notification Sent", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	static class RegisterStudentPanel extends JPanel {
		JTextField usernameField, nameField, yearLevelField, sectionField, contactField, emailField;
		JComboBox<String> programCombo;
		DefaultTableModel rosterModel;

		RegisterStudentPanel() {
			setLayout(new BorderLayout(0, 0));
			setBackground(BG);
			add(buildForm(), BorderLayout.WEST);
			add(buildRoster(), BorderLayout.CENTER);
		}

		JPanel buildForm() {
			JPanel outer = new JPanel(new BorderLayout());
			outer.setBackground(WHITE);
			outer.setPreferredSize(new Dimension(400, 0));
			outer.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

			JPanel inner = new JPanel();
			inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
			inner.setBackground(WHITE);
			inner.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

			JLabel title = new JLabel("Register Student");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);
			title.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel sub = new JLabel("Add a new student — password will be set to their ID");
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);
			sub.setAlignmentX(Component.LEFT_ALIGNMENT);

			inner.add(title);
			inner.add(Box.createVerticalStrut(4));
			inner.add(sub);
			inner.add(Box.createVerticalStrut(22));
			inner.add(sep());
			inner.add(Box.createVerticalStrut(20));

			usernameField = formField();
			nameField = formField();
			yearLevelField = formField();
			sectionField = formField();
			contactField = formField();
			emailField = formField();

			programCombo = new JComboBox<>(new String[] { "ROTC", "CWTS" });
			programCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
			programCombo.setBackground(WHITE);
			programCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
			programCombo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true),
					BorderFactory.createEmptyBorder(6, 10, 6, 10)));
			programCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

			JPanel progRow = new JPanel();
			progRow.setLayout(new BoxLayout(progRow, BoxLayout.Y_AXIS));
			progRow.setOpaque(false);
			progRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
			JLabel progLbl = new JLabel("PROGRAM");
			progLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
			progLbl.setForeground(TEXT_SUB);
			progLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
			progRow.add(progLbl);
			progRow.add(Box.createVerticalStrut(4));
			progRow.add(programCombo);

			inner.add(formRow("Username (Login ID)", usernameField));
			inner.add(formRow("Full Name", nameField));
			inner.add(progRow);
			inner.add(formRow("Year Level (e.g. 1st Year)", yearLevelField));
			inner.add(formRow("Section (e.g. BSIT 1-3)", sectionField));
			inner.add(formRow("Contact No. (optional)", contactField));
			inner.add(formRow("Email (optional)", emailField));
			inner.add(Box.createVerticalStrut(6));

			JButton registerBtn = makeBtn("Create Account", ACCENT, WHITE, 8);
			registerBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
			registerBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
			registerBtn.addActionListener(e -> doRegister());
			inner.add(registerBtn);

			outer.add(inner, BorderLayout.CENTER);
			return outer;
		}

		JPanel buildRoster() {
			JPanel panel = new JPanel(new BorderLayout());
			panel.setBackground(BG);

			JPanel header = new JPanel(new BorderLayout());
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(28, 24, 14, 24));

			JLabel t = new JLabel("Student Roster");
			t.setFont(new Font("SansSerif", Font.BOLD, 16));
			t.setForeground(TEXT_MAIN);

			JLabel s = new JLabel("All registered students");
			s.setFont(new Font("SansSerif", Font.PLAIN, 12));
			s.setForeground(TEXT_MUTED);

			header.add(t, BorderLayout.WEST);
			header.add(s, BorderLayout.EAST);

			String[] cols = { "ID", "Name", "Program", "Year Level", "Section" };
			rosterModel = new DefaultTableModel(cols, 0) {
				public boolean isCellEditable(int r, int c) {
					return false;
				}
			};
			JTable rosterTable = new JTable(rosterModel) {
				@Override
				public Component prepareRenderer(TableCellRenderer r, int row, int col) {
					Component c = super.prepareRenderer(r, row, col);
					c.setBackground(isRowSelected(row) ? ACCENT_SOFT : row % 2 == 0 ? WHITE : BG);
					c.setForeground(TEXT_MAIN);
					((JComponent) c).setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
					return c;
				}
			};
			styleTable(rosterTable);

			JPanel tableWrap = new JPanel(new BorderLayout());
			tableWrap.setBackground(BG);
			tableWrap.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
			tableWrap.add(wrapTable(rosterTable), BorderLayout.CENTER);

			refreshRoster();
			panel.add(header, BorderLayout.NORTH);
			panel.add(tableWrap, BorderLayout.CENTER);
			return panel;
		}

		void refreshRoster() {
			if (rosterModel == null)
				return;
			rosterModel.setRowCount(0);
			java.util.List<String[]> students = StudentDAO.getAllStudents("All");
			for (String[] s : students) {

				rosterModel.addRow(new Object[] { s[0], s[1], s[2], s[3], s[4] });
			}
		}

		JTextField formField() {
			JTextField f = new JTextField();
			f.setFont(new Font("SansSerif", Font.PLAIN, 13));
			f.setForeground(TEXT_MAIN);
			f.setBackground(WHITE);
			f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true),
					BorderFactory.createEmptyBorder(9, 12, 9, 12)));
			f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
			return f;
		}

		JPanel formRow(String label, JTextField field) {
			JPanel row = new JPanel();
			row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
			row.setOpaque(false);
			row.setBorder(BorderFactory.createEmptyBorder(0, 0, 13, 0));
			JLabel lbl = new JLabel(label.toUpperCase());
			lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
			lbl.setForeground(TEXT_SUB);
			lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
			field.setAlignmentX(Component.LEFT_ALIGNMENT);
			row.add(lbl);
			row.add(Box.createVerticalStrut(4));
			row.add(field);
			return row;
		}

		void doRegister() {
			String username = usernameField.getText().trim();
			String name = nameField.getText().trim();
			String prog = (String) programCombo.getSelectedItem();
			String yearLvl = yearLevelField.getText().trim();
			String section = sectionField.getText().trim();
			String contact = contactField.getText().trim();
			String email = emailField.getText().trim();

			if (username.isEmpty() || name.isEmpty() || section.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Username, Full Name, and Section are required.", "Missing Fields",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (UserDAO.usernameExists(username)) {
				JOptionPane.showMessageDialog(this, "Username '" + username + "' is already taken.",
						"Duplicate Username", JOptionPane.ERROR_MESSAGE);
				return;
			}

			int newStudentId = StudentDAO.createStudent(name, prog, yearLvl.isEmpty() ? "1st Year" : yearLvl, section,
					contact, email);
			if (newStudentId < 0) {
				JOptionPane.showMessageDialog(this, "Failed to create student record. Check DB connection.", "DB Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			String role = prog.equals("ROTC") ? "ROTC_Student" : "CWTS_Student";

			boolean userCreated = UserDAO.createUser(username, username, role, newStudentId);
			if (!userCreated) {
				JOptionPane.showMessageDialog(this, "Student record created but user account failed. Check DB.",
						"Partial Error", JOptionPane.WARNING_MESSAGE);
				return;
			}

			JOptionPane.showMessageDialog(this,
					"Account created!\n\nName:      " + name + "\nUsername:  " + username + "\nProgram:   " + prog
							+ "\nSection:   " + section + "\nPassword:  " + username + " (default)",
					"Success", JOptionPane.INFORMATION_MESSAGE);

			usernameField.setText("");
			nameField.setText("");
			yearLevelField.setText("");
			sectionField.setText("");
			contactField.setText("");
			emailField.setText("");
			refreshRoster();
		}
	}

	static class CWTSEngagementAdminPanel extends JPanel {
		DefaultTableModel logModel;
		JTextField studentIdField, activityField, hoursField, pointsField;
		JSpinner dateSpinner;
		JComboBox<String> studentCombo;
		JLabel totalPointsLabel;

		CWTSEngagementAdminPanel() {
			setLayout(new BorderLayout(0, 0));
			setBackground(BG);
			add(buildForm(), BorderLayout.WEST);
			add(buildLog(), BorderLayout.CENTER);
		}

		JPanel buildForm() {
			JPanel outer = new JPanel(new BorderLayout());
			outer.setBackground(WHITE);
			outer.setPreferredSize(new Dimension(380, 0));
			outer.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

			JPanel inner = new JPanel();
			inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
			inner.setBackground(WHITE);
			inner.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

			JLabel title = new JLabel("CWTS Engagement");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);
			title.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel sub = new JLabel("Log community service activities and award points");
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);
			sub.setAlignmentX(Component.LEFT_ALIGNMENT);

			inner.add(title);
			inner.add(Box.createVerticalStrut(4));
			inner.add(sub);
			inner.add(Box.createVerticalStrut(22));
			inner.add(sep());
			inner.add(Box.createVerticalStrut(20));

			studentCombo = buildStudentCombo("CWTS");
			inner.add(comboRow("Student", studentCombo));
			inner.add(Box.createVerticalStrut(13));

			activityField = formField();
			inner.add(formRow("Activity / Event", activityField));

			Date today = new Date();
			SpinnerDateModel dm = new SpinnerDateModel(today, null, null, Calendar.DAY_OF_MONTH);
			dateSpinner = new JSpinner(dm);
			JSpinner.DateEditor de = new JSpinner.DateEditor(dateSpinner, "MMM dd, yyyy");
			dateSpinner.setEditor(de);
			de.getTextField().setEditable(false);
			dateSpinner.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
			dateSpinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
			dateSpinner.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
			inner.add(spinnerRow("Date", dateSpinner));
			inner.add(Box.createVerticalStrut(13));

			hoursField = formField();
			pointsField = formField();
			inner.add(formRow("Hours Rendered", hoursField));
			inner.add(formRow("Points Awarded", pointsField));

			JButton addBtn = makeBtn("Add Engagement Record", GREEN, WHITE, 8);
			addBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
			addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
			addBtn.addActionListener(e -> doAdd());
			inner.add(Box.createVerticalStrut(6));
			inner.add(addBtn);

			outer.add(inner, BorderLayout.CENTER);
			return outer;
		}

		JPanel buildLog() {
			JPanel panel = new JPanel(new BorderLayout());
			panel.setBackground(BG);

			JPanel header = new JPanel(new BorderLayout());
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(28, 24, 14, 24));

			JLabel t = new JLabel("Engagement Log");
			t.setFont(new Font("SansSerif", Font.BOLD, 16));
			t.setForeground(TEXT_MAIN);

			totalPointsLabel = new JLabel("Loading…");
			totalPointsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
			totalPointsLabel.setForeground(TEXT_MUTED);

			header.add(t, BorderLayout.WEST);
			header.add(totalPointsLabel, BorderLayout.EAST);

			String[] cols = { "Student ID", "Name", "Section", "Activity", "Date", "Hours", "Points" };
			logModel = new DefaultTableModel(cols, 0) {
				public boolean isCellEditable(int r, int c) {
					return false;
				}
			};
			JTable logTable = makeTable(logModel);

			logTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {

				@Override
				public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row,
						int col) {

					super.getTableCellRendererComponent(t, v, sel, foc, row, col);

					setText(v == null ? "" : v.toString());

					setFont(new Font("SansSerif", Font.BOLD, 12));
					setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

					if (sel) {
						setBackground(ACCENT_SOFT);
						setForeground(Color.WHITE);
					} else {
						setBackground(row % 2 == 0 ? WHITE : BG);
						setForeground(GREEN);
					}

					setOpaque(true);
					return this;
				}
			});

			JPanel tableWrap = new JPanel(new BorderLayout());
			tableWrap.setBackground(BG);
			tableWrap.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
			tableWrap.add(wrapTable(logTable), BorderLayout.CENTER);

			panel.add(header, BorderLayout.NORTH);
			panel.add(tableWrap, BorderLayout.CENTER);
			refreshLog();
			return panel;
		}

		void refreshLog() {
			logModel.setRowCount(0);
			int total = 0;
			try {
				java.util.List<String[]> logs = dao.EngagementDAO.getAllEngagement();
				for (String[] rec : logs) {

					logModel.addRow(new Object[] { rec[1], rec[2], rec[3], rec[4], rec[5], rec[7], "0" });
					try {
						total += (int) Double.parseDouble(rec[7]);
					} catch (Exception ignored) {
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (totalPointsLabel != null) {
				totalPointsLabel.setText("Total hours logged: " + total);
			}
		}

		void doAdd() {
			if (studentCombo.getSelectedIndex() < 0) {
				JOptionPane.showMessageDialog(this, "No CWTS students available.", "Error",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			String activity = activityField.getText().trim();
			String hoursStr = hoursField.getText().trim();
			String ptsStr = pointsField.getText().trim();

			if (activity.isEmpty() || hoursStr.isEmpty() || ptsStr.isEmpty()) {
				JOptionPane.showMessageDialog(this, "All fields are required.", "Missing Fields",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			try {
				Float.parseFloat(hoursStr);
				Integer.parseInt(ptsStr);
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "Hours and Points must be numeric.", "Invalid Input",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			String combo = (String) studentCombo.getSelectedItem();

			if (combo == null || combo.isEmpty())
				return;

			String sidStr = combo.replaceAll(".*\\((.*)\\)$", "$1").trim();
			int sid = Integer.parseInt(sidStr);

			String sname = "";
			String section = "";

			String sql = "SELECT full_name, section FROM students WHERE student_id = ? AND is_deleted = 0";

			try (java.sql.Connection conn = util.DBConnection.getConnection();
					java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

				ps.setInt(1, sid);

				java.sql.ResultSet rs = ps.executeQuery();

				if (rs.next()) {
					sname = rs.getString("full_name");
					section = rs.getString("section");
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			Date date = (Date) dateSpinner.getValue();
			String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(date);

			double hoursDouble;
			try {
				hoursDouble = Double.parseDouble(hoursStr);
			} catch (Exception ex) {
				hoursDouble = 0;
			}
			int actId = dao.EngagementDAO.addEngagement(sid, activity, (Date) dateSpinner.getValue(),
					"Community Service", hoursDouble);
			if (actId < 0) {
				JOptionPane.showMessageDialog(this, "Failed to save to database.", "DB Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			refreshLog();

			activityField.setText("");
			hoursField.setText("");
			pointsField.setText("");

			JOptionPane.showMessageDialog(this, "Engagement record added.", "Success", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	static class CWTSEngagementStudentPanel extends JPanel {

		int studentId;

		CWTSEngagementStudentPanel(int studentId2) {
			this.studentId = studentId2;
			setLayout(new BorderLayout());
			setBackground(BG);

			JPanel header = new JPanel();
			header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(28, 28, 20, 28));

			JLabel title = new JLabel("Engagement Status");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);
			title.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel sub = new JLabel("Your community service activities and earned points");
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);
			sub.setAlignmentX(Component.LEFT_ALIGNMENT);

			header.add(title);
			header.add(Box.createVerticalStrut(3));
			header.add(sub);

			JPanel cards = buildSummaryCards();
			cards.setBorder(BorderFactory.createEmptyBorder(0, 28, 20, 28));

			String[] cols = { "Activity", "Date", "Hours", "Verified" };
			DefaultTableModel model = new DefaultTableModel(cols, 0) {
				public boolean isCellEditable(int r, int c) {
					return false;
				}
			};

			int totalPts = 0;
			float totalHrs = 0;
			int activities = 0;
			boolean hasRecords = false;

			int sid = Integer.parseInt(String.valueOf(studentId2));

			java.util.List<String[]> records = dao.EngagementDAO.getStudentEngagement(sid);

			for (String[] rec : records) {

				hasRecords = true;
				activities++;

				String verifiedFlag = rec[5];
				String verifiedLabel = "1".equals(verifiedFlag) ? "Verified" : "Pending";

				model.addRow(new Object[] { rec[1], rec[2], rec[4], verifiedLabel });

				try {
					totalHrs += Float.parseFloat(rec[4]);
				} catch (Exception ignored) {
				}
			}
			totalPts = (int) totalHrs;

			if (!hasRecords)
				model.addRow(new Object[] { "No activities recorded yet", "—", "—", "—" });

			updateSummaryCards(cards, activities, totalHrs, totalPts);

			JTable table = new JTable(model) {
				@Override
				public Component prepareRenderer(TableCellRenderer r, int row, int col) {
					Component c = super.prepareRenderer(r, row, col);
					c.setBackground(isRowSelected(row) ? ACCENT_SOFT : row % 2 == 0 ? WHITE : BG);
					c.setForeground(TEXT_MAIN);
					((JComponent) c).setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
					return c;
				}
			};
			styleTable(table);
			table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {

				@Override
				public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row,
						int col) {

					super.getTableCellRendererComponent(t, v, sel, foc, row, col);

					setText(v == null ? "" : v.toString());

					setFont(new Font("SansSerif", Font.BOLD, 12));
					setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

					if (sel) {
						setBackground(ACCENT_SOFT);
						setForeground(TEXT_MAIN);
					} else {
						setBackground(row % 2 == 0 ? WHITE : BG);
						setForeground(GREEN);
					}

					setOpaque(true);
					return this;
				}
			});
			JPanel tableWrap = new JPanel(new BorderLayout());
			tableWrap.setBackground(BG);
			tableWrap.setBorder(BorderFactory.createEmptyBorder(0, 28, 28, 28));
			tableWrap.add(wrapTable(table), BorderLayout.CENTER);

			add(header, BorderLayout.NORTH);

			JPanel center = new JPanel(new BorderLayout());
			center.setBackground(BG);
			center.add(cards, BorderLayout.NORTH);
			center.add(tableWrap, BorderLayout.CENTER);
			add(center, BorderLayout.CENTER);
		}

		JPanel buildSummaryCards() {
			JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
			row.setOpaque(false);

			row.add(summaryCard("Activities", "0", GREEN, GREEN_SOFT));
			row.add(summaryCard("Total Hours", "0", ACCENT, ACCENT_SOFT));
			row.add(summaryCard("Total Points", "0", GOLD, GOLD_SOFT));
			return row;
		}

		void updateSummaryCards(JPanel cards, int activities, float hours, int points) {

			updateCardValue((JPanel) cards.getComponent(0), String.valueOf(activities));
			updateCardValue((JPanel) cards.getComponent(1), String.format("%.1f", hours));
			updateCardValue((JPanel) cards.getComponent(2), String.valueOf(points));
		}

		void updateCardValue(JPanel card, String value) {
			for (Component c : card.getComponents()) {
				if (c instanceof JLabel) {
					JLabel l = (JLabel) c;
					if (l.getFont().isBold() && l.getFont().getSize() >= 24) {
						l.setText(value);
						return;
					}
				}
			}
		}

		JPanel summaryCard(String label, String value, Color accent, Color accentSoft) {
			JPanel card = new JPanel() {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(WHITE);
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
					g2.setColor(BORDER);
					g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
					g2.dispose();
				}
			};
			card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
			card.setOpaque(false);
			card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

			JLabel lbl = new JLabel(label);
			lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
			lbl.setForeground(TEXT_MUTED);
			lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel val = new JLabel(value);
			val.setFont(new Font("SansSerif", Font.BOLD, 28));
			val.setForeground(accent);
			val.setAlignmentX(Component.LEFT_ALIGNMENT);

			card.add(lbl);
			card.add(Box.createVerticalStrut(6));
			card.add(val);
			return card;
		}
	}

	static class ROTCDemeritsAdminPanel extends JPanel {
		DefaultTableModel logModel;
		JComboBox<String> studentCombo;
		JTextField reasonField, pointsField;
		JSpinner dateSpinner;
		JLabel totalLabel;

		ROTCDemeritsAdminPanel() {
			setLayout(new BorderLayout(0, 0));
			setBackground(BG);
			add(buildForm(), BorderLayout.WEST);
			add(buildLog(), BorderLayout.CENTER);
		}

		JPanel buildForm() {
			JPanel outer = new JPanel(new BorderLayout());
			outer.setBackground(WHITE);
			outer.setPreferredSize(new Dimension(380, 0));
			outer.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

			JPanel inner = new JPanel();
			inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
			inner.setBackground(WHITE);
			inner.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

			JLabel title = new JLabel("ROTC Demerits");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);
			title.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel sub = new JLabel("Issue demerit points to ROTC cadets");
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);
			sub.setAlignmentX(Component.LEFT_ALIGNMENT);

			inner.add(title);
			inner.add(Box.createVerticalStrut(4));
			inner.add(sub);
			inner.add(Box.createVerticalStrut(22));
			inner.add(sep());
			inner.add(Box.createVerticalStrut(20));

			studentCombo = buildStudentCombo("ROTC");
			inner.add(comboRow("Cadet", studentCombo));
			inner.add(Box.createVerticalStrut(13));

			reasonField = formField();
			inner.add(formRow("Reason / Violation", reasonField));

			Date today = new Date();
			SpinnerDateModel dm = new SpinnerDateModel(today, null, null, Calendar.DAY_OF_MONTH);
			dateSpinner = new JSpinner(dm);
			JSpinner.DateEditor de = new JSpinner.DateEditor(dateSpinner, "MMM dd, yyyy");
			dateSpinner.setEditor(de);
			de.getTextField().setEditable(false);
			dateSpinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
			dateSpinner.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
			inner.add(spinnerRow("Date", dateSpinner));
			inner.add(Box.createVerticalStrut(13));

			pointsField = formField();
			inner.add(formRow("Demerit Points", pointsField));

			JButton issueBtn = makeBtn("Issue Demerit", RED, WHITE, 8);
			issueBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
			issueBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
			issueBtn.addActionListener(e -> doIssue());
			inner.add(Box.createVerticalStrut(6));
			inner.add(issueBtn);

			inner.add(Box.createVerticalStrut(20));
			inner.add(sep());
			inner.add(Box.createVerticalStrut(14));
			JLabel infoTitle = new JLabel("DEMERIT SCALE");
			infoTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
			infoTitle.setForeground(TEXT_MUTED);
			infoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
			inner.add(infoTitle);
			inner.add(Box.createVerticalStrut(8));
			String[][] scale = { { "1–5", "Minor infraction" }, { "6–10", "Moderate violation" },
					{ "11+", "Serious offense" } };
			for (String[] s : scale) {
				JPanel scaleRow = new JPanel(new BorderLayout(10, 0));
				scaleRow.setOpaque(false);
				scaleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
				JLabel pts = new JLabel(s[0]);
				pts.setFont(new Font("SansSerif", Font.BOLD, 12));
				pts.setForeground(RED);
				JLabel desc = new JLabel(s[1]);
				desc.setFont(new Font("SansSerif", Font.PLAIN, 12));
				desc.setForeground(TEXT_SUB);
				scaleRow.add(pts, BorderLayout.WEST);
				scaleRow.add(desc, BorderLayout.CENTER);
				inner.add(scaleRow);
				inner.add(Box.createVerticalStrut(4));
			}

			outer.add(inner, BorderLayout.CENTER);
			return outer;
		}

		JPanel buildLog() {
			JPanel panel = new JPanel(new BorderLayout());
			panel.setBackground(BG);

			JPanel header = new JPanel(new BorderLayout());
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(28, 24, 14, 24));

			JLabel t = new JLabel("Demerit Log");
			t.setFont(new Font("SansSerif", Font.BOLD, 16));
			t.setForeground(TEXT_MAIN);

			totalLabel = new JLabel("");
			totalLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
			totalLabel.setForeground(TEXT_MUTED);

			header.add(t, BorderLayout.WEST);
			header.add(totalLabel, BorderLayout.EAST);

			String[] cols = { "Student ID", "Name", "Platoon", "Battalion", "Reason", "Points", "Date" };
			logModel = new DefaultTableModel(cols, 0) {
				public boolean isCellEditable(int r, int c) {
					return false;
				}
			};
			JTable logTable = makeTable(logModel);
			logTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row,
						int col) {
					super.getTableCellRendererComponent(t, v, sel, foc, row, col);
					setForeground(RED);
					setFont(new Font("SansSerif", Font.BOLD, 12));
					setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
					setBackground(sel ? ACCENT_SOFT : row % 2 == 0 ? WHITE : BG);
					setOpaque(true);
					return this;
				}
			});

			JPanel tableWrap = new JPanel(new BorderLayout());
			tableWrap.setBackground(BG);
			tableWrap.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
			tableWrap.add(wrapTable(logTable), BorderLayout.CENTER);

			panel.add(header, BorderLayout.NORTH);
			panel.add(tableWrap, BorderLayout.CENTER);
			refreshLog();
			return panel;
		}

		void refreshLog() {
			logModel.setRowCount(0);
			int total = 0;
			String sql = "SELECT d.demerit_id, s.student_id, s.full_name, "
					+ "COALESCE(r.platoon,'—') AS platoon, COALESCE(r.battalion,'—') AS battalion, "
					+ "d.reason, d.severity, d.demerit_date "
					+ "FROM demerits d JOIN students s ON d.student_id=s.student_id "
					+ "LEFT JOIN rotc_ranks r ON s.student_id=r.student_id "
					+ "WHERE s.is_deleted=0 ORDER BY d.demerit_date DESC";
			try (java.sql.Connection conn = util.DBConnection.getConnection()) {
				if (conn == null)
					return;
				try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
						java.sql.ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						logModel.addRow(new Object[] { rs.getString("student_id"), rs.getString("full_name"),
								rs.getString("platoon"), rs.getString("battalion"), rs.getString("reason"),
								rs.getString("severity"), rs.getString("demerit_date") });
						total++;
					}
				}
			} catch (java.sql.SQLException ex) {
				ex.printStackTrace();
			}
			if (totalLabel != null)
				totalLabel.setText("Total demerits issued: " + total);
		}

		void doIssue() {
			if (studentCombo.getSelectedIndex() < 0) {
				JOptionPane.showMessageDialog(this, "No ROTC cadets available.", "Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			String reason = reasonField.getText().trim();
			String ptsStr = pointsField.getText().trim();

			if (reason.isEmpty() || ptsStr.isEmpty()) {
				JOptionPane.showMessageDialog(this, "All fields are required.", "Missing Fields",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			try {
				Integer.parseInt(ptsStr);
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "Points must be a whole number.", "Invalid",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			String combo = (String) studentCombo.getSelectedItem();
			String sid = combo.replaceAll(".*\\((.*)\\)$", "$1");
			String sname = combo.replaceAll("\\s*\\(.*\\)$", "");
			String platoon = "", battalion = "";

			Date date = (Date) dateSpinner.getValue();
			String dateStr = new SimpleDateFormat("MMM dd, yyyy").format(date);

			int sidInt = Util.toInt(sid);
			String sqlInsert = "INSERT INTO demerits (student_id, reason, severity, demerit_date, status) VALUES (?, ?, 'Minor', ?, 'Active')";
			try (java.sql.Connection conn = util.DBConnection.getConnection();
					java.sql.PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
				ps.setInt(1, sidInt);
				ps.setString(2, reason);
				ps.setString(3, new SimpleDateFormat("yyyy-MM-dd").format((Date) dateSpinner.getValue()));
				ps.executeUpdate();
			} catch (java.sql.SQLException ex) {
				JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			refreshLog();
			reasonField.setText("");
			pointsField.setText("");
			JOptionPane.showMessageDialog(this, "Demerit issued to " + sname + " (" + ptsStr + " pts).", "Issued",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	static class ROTCDemeritsStudentPanel extends JPanel {
		String studentId;

		ROTCDemeritsStudentPanel(String studentId) {
			this.studentId = studentId;
			setLayout(new BorderLayout());
			setBackground(BG);

			JPanel header = new JPanel();
			header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(28, 28, 20, 28));

			JLabel title = new JLabel("Demerit Record");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);
			title.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel sub = new JLabel("Your demerit history from the commanding officer");
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);
			sub.setAlignmentX(Component.LEFT_ALIGNMENT);

			header.add(title);
			header.add(Box.createVerticalStrut(3));
			header.add(sub);

			int totalPts = dao.DemeritDAO.getTotalPoints(Util.toInt(studentId));
			int count = dao.DemeritDAO.getRecordCount(Util.toInt(studentId));

			JPanel statusCard = buildStatusCard(totalPts, count);
			statusCard.setBorder(BorderFactory.createEmptyBorder(0, 28, 20, 28));

			String[] cols = { "Reason / Violation", "Date", "Points" };
			DefaultTableModel model = new DefaultTableModel(cols, 0) {
				public boolean isCellEditable(int r, int c) {
					return false;
				}
			};

			boolean hasRecords = false;
			String sqlDem = "SELECT reason, demerit_date, severity FROM demerits WHERE student_id=? ORDER BY demerit_date DESC";
			try (java.sql.Connection conn = util.DBConnection.getConnection()) {
				if (conn != null) {
					try (java.sql.PreparedStatement ps = conn.prepareStatement(sqlDem)) {
						ps.setInt(1, Util.toInt(studentId));
						java.sql.ResultSet rs = ps.executeQuery();
						while (rs.next()) {
							hasRecords = true;
							model.addRow(new Object[] { rs.getString("reason"), rs.getString("demerit_date"),
									rs.getString("severity") });
						}
					}
				}
			} catch (java.sql.SQLException ex) {
				ex.printStackTrace();
			}
			if (!hasRecords)
				model.addRow(new Object[] { "No demerits on record", "—", "—" });

			JTable table = makeTable(model);
			table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row,
						int col) {
					super.getTableCellRendererComponent(t, v, sel, foc, row, col);
					String s = v != null ? v.toString() : "";
					if (!s.equals("—")) {
						setForeground(RED);
						setFont(new Font("SansSerif", Font.BOLD, 12));
					} else {
						setForeground(TEXT_MUTED);
					}
					setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
					setBackground(sel ? ACCENT_SOFT : row % 2 == 0 ? WHITE : BG);
					setOpaque(true);
					return this;
				}
			});

			JPanel tableWrap = new JPanel(new BorderLayout());
			tableWrap.setBackground(BG);
			tableWrap.setBorder(BorderFactory.createEmptyBorder(0, 28, 28, 28));
			tableWrap.add(wrapTable(table), BorderLayout.CENTER);

			add(header, BorderLayout.NORTH);
			JPanel center = new JPanel(new BorderLayout());
			center.setBackground(BG);
			center.add(statusCard, BorderLayout.NORTH);
			center.add(tableWrap, BorderLayout.CENTER);
			add(center, BorderLayout.CENTER);
		}

		JPanel buildStatusCard(int totalPts, int count) {
			JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
			row.setOpaque(false);

			String standing;
			Color standColor;
			if (totalPts == 0) {
				standing = "Clean Record";
				standColor = GREEN;
			} else if (totalPts <= 10) {
				standing = "Minor";
				standColor = GOLD;
			} else if (totalPts <= 20) {
				standing = "Warning";
				standColor = new Color(200, 100, 0);
			} else {
				standing = "Critical";
				standColor = RED;
			}

			row.add(miniCard("Total Demerits", String.valueOf(totalPts), RED, RED_SOFT));
			row.add(miniCard("Incidents", String.valueOf(count), GOLD, GOLD_SOFT));
			row.add(miniCard("Standing", standing, standColor,
					new Color(standColor.getRed(), standColor.getGreen(), standColor.getBlue(), 30)));
			return row;
		}

		JPanel miniCard(String label, String value, Color accent, Color accentSoft) {
			JPanel card = new JPanel() {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(WHITE);
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
					g2.setColor(BORDER);
					g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
					g2.dispose();
				}
			};
			card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
			card.setOpaque(false);
			card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

			JLabel lbl = new JLabel(label.toUpperCase());
			lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
			lbl.setForeground(TEXT_MUTED);
			lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel val = new JLabel(value);
			val.setFont(new Font("SansSerif", Font.BOLD, 26));
			val.setForeground(accent);
			val.setAlignmentX(Component.LEFT_ALIGNMENT);

			card.add(lbl);
			card.add(Box.createVerticalStrut(6));
			card.add(val);
			return card;
		}
	}

	static JComboBox<String> buildStudentCombo(String program) {
		JComboBox<String> cb = new JComboBox<>();
		java.util.List<String[]> students = StudentDAO.getAllStudents(program);
		for (String[] s : students) {

			cb.addItem(s[1] + " (" + s[0] + ")");
		}
		if (cb.getItemCount() == 0)
			cb.addItem("— No students found —");
		cb.setFont(new Font("SansSerif", Font.PLAIN, 13));
		cb.setBackground(WHITE);
		cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
		cb.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true),
				BorderFactory.createEmptyBorder(6, 10, 6, 10)));
		cb.setAlignmentX(Component.LEFT_ALIGNMENT);
		return cb;
	}

	static int parseStudentIdFromCombo(String comboItem) {
		try {
			String idPart = comboItem.replaceAll(".*\\((.*)\\)$", "$1").trim();
			return Integer.parseInt(idPart);
		} catch (Exception e) {
			return -1;
		}
	}

	static String parseNameFromCombo(String comboItem) {
		return comboItem.replaceAll("\\s*\\(.*\\)$", "").trim();
	}

	static JPanel comboRow(String label, JComboBox<?> cb) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setOpaque(false);
		p.setBorder(BorderFactory.createEmptyBorder(0, 0, 13, 0));
		JLabel lbl = new JLabel(label.toUpperCase());
		lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
		lbl.setForeground(TEXT_SUB);
		lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		cb.setAlignmentX(Component.LEFT_ALIGNMENT);
		p.add(lbl);
		p.add(Box.createVerticalStrut(4));
		p.add(cb);
		return p;
	}

	static JPanel spinnerRow(String label, JSpinner spinner) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setOpaque(false);
		p.setBorder(BorderFactory.createEmptyBorder(0, 0, 13, 0));
		JLabel lbl = new JLabel(label.toUpperCase());
		lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
		lbl.setForeground(TEXT_SUB);
		lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		spinner.setAlignmentX(Component.LEFT_ALIGNMENT);
		spinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
		p.add(lbl);
		p.add(Box.createVerticalStrut(4));
		p.add(spinner);
		return p;
	}

	static JTextField formField() {
		JTextField f = new JTextField();
		f.setFont(new Font("SansSerif", Font.PLAIN, 13));
		f.setForeground(TEXT_MAIN);
		f.setBackground(WHITE);
		f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true),
				BorderFactory.createEmptyBorder(9, 12, 9, 12)));
		f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
		f.setAlignmentX(Component.LEFT_ALIGNMENT);
		return f;
	}

	static JPanel formRow(String label, JTextField field) {
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setOpaque(false);
		row.setBorder(BorderFactory.createEmptyBorder(0, 0, 13, 0));
		JLabel lbl = new JLabel(label.toUpperCase());
		lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
		lbl.setForeground(TEXT_SUB);
		lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		field.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.add(lbl);
		row.add(Box.createVerticalStrut(4));
		row.add(field);
		return row;
	}

	static class CalendarPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		String program;
		Color accent, accentSoft;
		boolean isAdmin;
		JPanel calGrid, eventList;
		java.util.Calendar displayCal = java.util.Calendar.getInstance();
		JLabel monthLabel;

		CalendarPanel(String program, Color accent, Color accentSoft, boolean isAdmin) {
			this.program = program;
			this.accent = accent;
			this.accentSoft = accentSoft;
			this.isAdmin = isAdmin;
			setLayout(new BorderLayout());
			setBackground(BG);

			JPanel header = new JPanel();
			header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(28, 28, 16, 28));
			JLabel title = new JLabel(isAdmin ? "Calendar Events (Admin)" : "Training Calendar");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);
			title.setAlignmentX(Component.LEFT_ALIGNMENT);
			JLabel sub = new JLabel(isAdmin ? "Manage events visible to ROTC, CWTS, or all students"
					: "Upcoming events and training schedules for " + program);
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);
			sub.setAlignmentX(Component.LEFT_ALIGNMENT);
			header.add(title);
			header.add(Box.createVerticalStrut(3));
			header.add(sub);

			JPanel body = new JPanel(new BorderLayout(16, 0));
			body.setBackground(BG);
			body.setBorder(BorderFactory.createEmptyBorder(0, 28, 28, 28));
			body.add(buildCalendarWidget(), BorderLayout.CENTER);
			body.add(buildEventList(), BorderLayout.EAST);

			add(header, BorderLayout.NORTH);
			add(body, BorderLayout.CENTER);
			if (isAdmin)
				add(buildAddEventForm(), BorderLayout.SOUTH);
			refreshAll();
		}

		JPanel buildCalendarWidget() {
			JPanel wrapper = new JPanel(new BorderLayout());
			wrapper.setOpaque(false);

			JPanel nav = new JPanel(new BorderLayout());
			nav.setOpaque(false);
			nav.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

			JButton prev = makeBtn("◀", BG, TEXT_SUB, 6);
			prev.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
			prev.setPreferredSize(new Dimension(36, 32));
			prev.addActionListener(e -> {
				displayCal.add(java.util.Calendar.MONTH, -1);
				refreshAll();
			});

			JButton next = makeBtn("▶", BG, TEXT_SUB, 6);
			next.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
			next.setPreferredSize(new Dimension(36, 32));
			next.addActionListener(e -> {
				displayCal.add(java.util.Calendar.MONTH, 1);
				refreshAll();
			});

			monthLabel = new JLabel("", SwingConstants.CENTER);
			monthLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
			monthLabel.setForeground(TEXT_MAIN);

			nav.add(prev, BorderLayout.WEST);
			nav.add(monthLabel, BorderLayout.CENTER);
			nav.add(next, BorderLayout.EAST);

			calGrid = new JPanel(new GridLayout(0, 7, 2, 2));
			calGrid.setBackground(BG);

			JPanel calCard = new JPanel(new BorderLayout()) {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(WHITE);
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
					g2.setColor(BORDER);
					g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
					g2.dispose();
				}
			};
			calCard.setOpaque(false);
			calCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
			calCard.add(nav, BorderLayout.NORTH);
			calCard.add(calGrid, BorderLayout.CENTER);
			wrapper.add(calCard, BorderLayout.CENTER);
			return wrapper;
		}

		JPanel buildEventList() {
			JPanel outer = new JPanel(new BorderLayout());
			outer.setBackground(BG);
			outer.setPreferredSize(new Dimension(280, 0));

			JLabel t = new JLabel("Events This Month");
			t.setFont(new Font("SansSerif", Font.BOLD, 13));
			t.setForeground(TEXT_MAIN);
			t.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

			eventList = new JPanel();
			eventList.setLayout(new BoxLayout(eventList, BoxLayout.Y_AXIS));
			eventList.setBackground(BG);

			JScrollPane scroll = new JScrollPane(eventList);
			scroll.setBorder(BorderFactory.createEmptyBorder());
			scroll.getViewport().setBackground(BG);
			scroll.getVerticalScrollBar().setUnitIncrement(10);

			outer.add(t, BorderLayout.NORTH);
			outer.add(scroll, BorderLayout.CENTER);
			return outer;
		}

		JPanel buildAddEventForm() {
			JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
			form.setBackground(WHITE);
			form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
					BorderFactory.createEmptyBorder(4, 20, 4, 20)));

			JComboBox<String> targetCb = new JComboBox<>(new String[] { "ALL", "ROTC", "CWTS" });
			targetCb.setPreferredSize(new Dimension(90, 34));
			targetCb.setFont(new Font("SansSerif", Font.PLAIN, 12));

			JTextField titleF = new JTextField(14);
			titleF.setFont(new Font("SansSerif", Font.PLAIN, 12));
			titleF.setPreferredSize(new Dimension(160, 34));
			titleF.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true),
					BorderFactory.createEmptyBorder(4, 8, 4, 8)));

			JTextField descF = new JTextField(20);
			descF.setFont(new Font("SansSerif", Font.PLAIN, 12));
			descF.setPreferredSize(new Dimension(220, 34));
			descF.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true),
					BorderFactory.createEmptyBorder(4, 8, 4, 8)));

			Date today = new Date();
			SpinnerDateModel dm = new SpinnerDateModel(today, null, null, java.util.Calendar.DAY_OF_MONTH);
			JSpinner dateSpin = new JSpinner(dm);
			JSpinner.DateEditor de = new JSpinner.DateEditor(dateSpin, "yyyy-MM-dd");
			dateSpin.setEditor(de);
			de.getTextField().setEditable(false);
			dateSpin.setPreferredSize(new Dimension(130, 34));
			dateSpin.setFont(new Font("SansSerif", Font.PLAIN, 12));
			dateSpin.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));

			JComboBox<String> colorCb = new JComboBox<>(new String[] { "ACCENT", "GOLD", "GREEN" });
			colorCb.setPreferredSize(new Dimension(90, 34));
			colorCb.setFont(new Font("SansSerif", Font.PLAIN, 12));

			JButton addBtn = makeBtn("+ Add Event", ACCENT, WHITE, 8);
			addBtn.setPreferredSize(new Dimension(110, 34));
			addBtn.addActionListener(e -> {
				String ttl = titleF.getText().trim();
				String desc = descF.getText().trim();
				if (ttl.isEmpty()) {
					JOptionPane.showMessageDialog(this, "Title is required.", "Missing", JOptionPane.WARNING_MESSAGE);
					return;
				}
				Date d = (Date) dateSpin.getValue();
				String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(d);
				dao.CalendarDAO.getEventsForProgram(program).add(new String[] { (String) targetCb.getSelectedItem(),
						dateStr, ttl, desc, (String) colorCb.getSelectedItem() });
				titleF.setText("");
				descF.setText("");
				refreshAll();
			});

			form.add(new JLabel("Target:"));
			form.add(targetCb);
			form.add(new JLabel("Date:"));
			form.add(dateSpin);
			form.add(new JLabel("Title:"));
			form.add(titleF);
			form.add(new JLabel("Desc:"));
			form.add(descF);
			form.add(new JLabel("Color:"));
			form.add(colorCb);
			form.add(addBtn);
			return form;
		}

		void refreshAll() {
			refreshGrid();
			refreshEventList();
		}

		void refreshGrid() {
			calGrid.removeAll();
			java.util.Calendar cal = (java.util.Calendar) displayCal.clone();
			int year = cal.get(java.util.Calendar.YEAR);
			int month = cal.get(java.util.Calendar.MONTH);
			monthLabel.setText(new SimpleDateFormat("MMMM yyyy").format(cal.getTime()));

			String[] days = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
			for (String d : days) {
				JLabel lbl = new JLabel(d, SwingConstants.CENTER);
				lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
				lbl.setForeground(TEXT_MUTED);
				lbl.setPreferredSize(new Dimension(0, 26));
				calGrid.add(lbl);
			}

			cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
			int startDow = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1;
			int daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
			for (int i = 0; i < startDow; i++)
				calGrid.add(new JLabel());

			java.util.Calendar today = java.util.Calendar.getInstance();

			java.util.Set<Integer> eventDays = new java.util.HashSet<>();
			String prefix = String.format("%04d-%02d-", year, month + 1);
			for (String[] ev : dao.CalendarDAO.getEventsForProgram(program)) {
				if (!ev[0].equals("ALL") && !ev[0].equals(program))
					continue;
				if (ev[1].startsWith(prefix)) {
					try {
						eventDays.add(Integer.parseInt(ev[1].substring(8)));
					} catch (Exception ignored) {
					}
				}
			}

			for (int day = 1; day <= daysInMonth; day++) {
				final int d = day;
				boolean isToday = today.get(java.util.Calendar.YEAR) == year
						&& today.get(java.util.Calendar.MONTH) == month
						&& today.get(java.util.Calendar.DAY_OF_MONTH) == day;
				boolean hasEvent = eventDays.contains(day);
				Color dotColor = accent;

				JPanel dayCell = new JPanel(new BorderLayout()) {
					@Override
					protected void paintComponent(Graphics g) {
						Graphics2D g2 = (Graphics2D) g.create();
						g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						if (isToday) {
							g2.setColor(accent);
							g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
						} else {
							g2.setColor(WHITE);
							g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
						}
						g2.dispose();
					}
				};
				dayCell.setOpaque(false);
				dayCell.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

				JLabel numLbl = new JLabel(String.valueOf(day), SwingConstants.CENTER);
				numLbl.setFont(new Font("SansSerif", isToday ? Font.BOLD : Font.PLAIN, 12));
				numLbl.setForeground(isToday ? WHITE : TEXT_MAIN);
				dayCell.add(numLbl, BorderLayout.CENTER);

				if (hasEvent && !isToday) {
					JPanel dot = new JPanel() {
						@Override
						protected void paintComponent(Graphics g) {
							Graphics2D g2 = (Graphics2D) g.create();
							g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
							g2.setColor(dotColor);
							g2.fillOval(getWidth() / 2 - 3, 1, 6, 6);
							g2.dispose();
						}
					};
					dot.setOpaque(false);
					dot.setPreferredSize(new Dimension(0, 10));
					dayCell.add(dot, BorderLayout.SOUTH);
				}
				calGrid.add(dayCell);
			}
			calGrid.revalidate();
			calGrid.repaint();
		}

		void refreshEventList() {
			eventList.removeAll();
			int year = displayCal.get(java.util.Calendar.YEAR);
			int month = displayCal.get(java.util.Calendar.MONTH) + 1;
			String prefix = String.format("%04d-%02d-", year, month);
			boolean any = false;

			for (String[] ev : dao.CalendarDAO.getEventsForProgram(program)) {
				if (!ev[0].equals("ALL") && !ev[0].equals(program))
					continue;
				if (!ev[1].startsWith(prefix))
					continue;
				any = true;
				eventList.add(buildEventCard(ev));
				eventList.add(Box.createVerticalStrut(8));
			}
			if (!any) {
				JLabel empty = new JLabel("No events this month.");
				empty.setFont(new Font("SansSerif", Font.PLAIN, 12));
				empty.setForeground(TEXT_MUTED);
				empty.setBorder(BorderFactory.createEmptyBorder(8, 4, 0, 0));
				eventList.add(empty);
			}
			eventList.revalidate();
			eventList.repaint();
		}

		JPanel buildEventCard(String[] ev) {
			Color bg, fg;
			switch (ev[4]) {
			case "GOLD":
				bg = GOLD_SOFT;
				fg = GOLD;
				break;
			case "GREEN":
				bg = GREEN_SOFT;
				fg = GREEN;
				break;
			default:
				bg = ACCENT_SOFT;
				fg = ACCENT;
				break;
			}
			JPanel card = new JPanel() {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(WHITE);
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
					g2.setColor(BORDER);
					g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
					g2.dispose();
				}
			};
			card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
			card.setOpaque(false);
			card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
			card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

			JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
			topRow.setOpaque(false);
			topRow.add(tag(ev[0], bg, fg));
			JLabel dateLbl = new JLabel(ev[1]);
			dateLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
			dateLbl.setForeground(TEXT_MUTED);
			topRow.add(dateLbl);
			topRow.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel titleLbl = new JLabel(ev[2]);
			titleLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
			titleLbl.setForeground(TEXT_MAIN);
			titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel descLbl = new JLabel("<html><div style='width:220px'>" + ev[3] + "</div></html>");
			descLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
			descLbl.setForeground(TEXT_SUB);
			descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

			card.add(topRow);
			card.add(Box.createVerticalStrut(4));
			card.add(titleLbl);
			card.add(Box.createVerticalStrut(3));
			card.add(descLbl);

			if (isAdmin) {
				JButton delBtn = makeBtn("✕ Remove", RED_SOFT, RED, 6);
				delBtn.setFont(new Font("SansSerif", Font.BOLD, 10));
				delBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
				delBtn.addActionListener(e -> {

					String delSql = "DELETE FROM calendar_events WHERE event_date = ? AND title = ? LIMIT 1";
					try (java.sql.Connection conn = util.DBConnection.getConnection()) {
						if (conn != null) {
							try (java.sql.PreparedStatement ps = conn.prepareStatement(delSql)) {
								ps.setString(1, ev[1]);
								ps.setString(2, ev[2]);
								ps.executeUpdate();
							}
						}
					} catch (java.sql.SQLException ex) {
						ex.printStackTrace();
					}
					refreshAll();
				});
				card.add(Box.createVerticalStrut(6));
				card.add(delBtn);
				card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
			}
			return card;
		}
	}

	static class COCCManagementPanel extends JPanel {
		DefaultTableModel model;

		COCCManagementPanel() {
			setLayout(new BorderLayout());
			setBackground(BG);

			JPanel header = new JPanel();
			header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
			header.setOpaque(false);
			header.setBorder(BorderFactory.createEmptyBorder(28, 28, 16, 28));
			JLabel title = new JLabel("COCC Management");
			title.setFont(new Font("SansSerif", Font.BOLD, 20));
			title.setForeground(TEXT_MAIN);
			title.setAlignmentX(Component.LEFT_ALIGNMENT);
			JLabel sub = new JLabel("Assign COCC officer ranks to ROTC students");
			sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sub.setForeground(TEXT_MUTED);
			sub.setAlignmentX(Component.LEFT_ALIGNMENT);
			header.add(title);
			header.add(Box.createVerticalStrut(3));
			header.add(sub);

			JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
			form.setBackground(WHITE);
			form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
					BorderFactory.createEmptyBorder(6, 24, 6, 24)));

			JComboBox<String> studentCb = buildStudentCombo("ROTC");

			String[] rankOptions = { "Cadet Colonel", "Cadet Lieutenant Colonel", "Cadet Major", "Cadet Captain",
					"Cadet First Lieutenant", "Cadet Second Lieutenant", "Cadet Master Sergeant",
					"Cadet Staff Sergeant", "Cadet Sergeant", "Cadet Corporal", "Cadet Private First Class",
					"Cadet Private" };
			JComboBox<String> rankCb = new JComboBox<>(rankOptions);
			rankCb.setPreferredSize(new Dimension(220, 34));
			rankCb.setFont(new Font("SansSerif", Font.PLAIN, 12));

			JTextField abbrevF = new JTextField(8);
			abbrevF.setFont(new Font("SansSerif", Font.PLAIN, 12));
			abbrevF.setPreferredSize(new Dimension(90, 34));
			abbrevF.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true),
					BorderFactory.createEmptyBorder(4, 8, 4, 8)));
			abbrevF.setToolTipText("Abbreviation e.g. C/Col");

			JButton assignBtn = makeBtn("Assign Rank", GOLD, WHITE, 8);
			assignBtn.setPreferredSize(new Dimension(120, 34));
			assignBtn.addActionListener(e -> {

				if (studentCb.getSelectedIndex() < 0)
					return;

				String combo = (String) studentCb.getSelectedItem();
				if (combo == null || !combo.contains("("))
					return;

				String sidStr = combo.replaceAll(".*\\((.*)\\)$", "$1").trim();
				int studentId;

				try {
					studentId = Integer.parseInt(sidStr);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(this, "Invalid student ID format.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				String rank = (String) rankCb.getSelectedItem();
				String abbrev = abbrevF.getText().trim();

				if (abbrev.isEmpty()) {
					abbrev = rank.replaceAll("Cadet ", "C/").replaceAll("\\s+.*", "");
				}

				boolean success = NSTPInfoSys.assignRank(studentId, abbrev, rank);

				if (success) {
					refreshTable();
					JOptionPane.showMessageDialog(this, "Rank assigned: " + rank + " → " + combo, "Done",
							JOptionPane.INFORMATION_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(this, "Failed to assign rank.", "Error", JOptionPane.ERROR_MESSAGE);
				}
			});

			JButton removeBtn = makeBtn("Remove Rank", RED_SOFT, RED, 8);
			removeBtn.setBorder(BorderFactory.createLineBorder(RED_SOFT, 1, true));
			removeBtn.setPreferredSize(new Dimension(120, 34));
			removeBtn.addActionListener(e -> {
				if (studentCb.getSelectedIndex() < 0)
					return;
				String combo = (String) studentCb.getSelectedItem();
				String sid = combo.replaceAll(".*\\((.*)\\)$", "$1").trim();
				int sidInt = Util.toInt(sid);
				String sqlDel = "DELETE FROM rotc_ranks WHERE student_id = ?";
				try (java.sql.Connection connDel = util.DBConnection.getConnection();
						java.sql.PreparedStatement psDel = connDel.prepareStatement(sqlDel)) {
					psDel.setInt(1, sidInt);
					psDel.executeUpdate();
				} catch (java.sql.SQLException ex) {
					ex.printStackTrace();
				}
				refreshTable();
				JOptionPane.showMessageDialog(this, "COCC rank removed for " + combo, "Done",
						JOptionPane.INFORMATION_MESSAGE);
			});

			form.add(new JLabel("Student:"));
			form.add(studentCb);
			form.add(new JLabel("Rank:"));
			form.add(rankCb);
			form.add(new JLabel("Abbrev:"));
			form.add(abbrevF);
			form.add(assignBtn);
			form.add(removeBtn);

			String[] cols = { "Student ID", "Name", "Abbreviation", "Full Rank Title", "Platoon", "Battalion" };
			model = new DefaultTableModel(cols, 0) {
				public boolean isCellEditable(int r, int c) {
					return false;
				}
			};
			JTable table = makeStdTable(model);
			JPanel tableWrap = new JPanel(new BorderLayout());
			tableWrap.setBackground(BG);
			tableWrap.setBorder(BorderFactory.createEmptyBorder(16, 28, 28, 28));
			tableWrap.add(NSTPInfoSys.wrapTable(table), BorderLayout.CENTER);

			refreshTable();
			add(header, BorderLayout.NORTH);
			JPanel center = new JPanel(new BorderLayout());
			center.setBackground(BG);
			center.add(form, BorderLayout.NORTH);
			center.add(tableWrap, BorderLayout.CENTER);
			add(center, BorderLayout.CENTER);
		}

		void refreshTable() {

			model.setRowCount(0);

			java.util.List<String[]> coccStudents = dao.StudentDAO.getCOCCStudents();

			for (String[] s : coccStudents) {

				model.addRow(new Object[] { s[0], s[1], s[2], s[3], s[4], s[5] });

			}
		}

		static class COCCFrame extends BaseStudentFrame {

			COCCFrame(int studentId, int userId) {
				super("COCC", studentId, userId);
			}

			String getProgram() {
				return "ROTC";
			}

			Color getAccent() {
				return GOLD;
			}

			Color getAccentSoft() {
				return GOLD_SOFT;
			}

			String getAvatarName() {

				String name = "COCC Officer";

				try {

					int sid = Util.toInt(studentId);

					String sql = "SELECT full_name FROM students " + "WHERE student_id = ? AND is_deleted = 0";

					try (java.sql.Connection conn = util.DBConnection.getConnection();
							java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

						ps.setInt(1, sid);

						java.sql.ResultSet rs = ps.executeQuery();

						if (rs.next()) {
							name = rs.getString("full_name");
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				return name;
			}

			@Override
			String getRoleTagText() {

				String rank = "COCC Officer";

				try {

					int sid = Util.toInt(studentId);

					String sql = "SELECT rank_name FROM rotc_ranks " + "WHERE student_id = ?";

					try (java.sql.Connection conn = util.DBConnection.getConnection();
							java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

						ps.setInt(1, sid);

						java.sql.ResultSet rs = ps.executeQuery();

						if (rs.next()) {
							rank = rs.getString("rank_name") + " · COCC";
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				return rank;
			}

			String[][] getSidebarFields() {

				String name = "";
				String platoon = "";
				String battalion = "";

				try {

					String[] studentInfo = dao.StudentDAO.getStudentById(studentId);

					if (studentInfo != null) {

						name = studentInfo[1];
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				try {
					int sid = util.Util.toInt(studentId);
					String rankSql = "SELECT rank_name, platoon, battalion FROM rotc_ranks WHERE student_id = ?";
					try (java.sql.Connection conn = util.DBConnection.getConnection()) {
						if (conn != null) {
							try (java.sql.PreparedStatement ps = conn.prepareStatement(rankSql)) {
								ps.setInt(1, sid);
								java.sql.ResultSet rs = ps.executeQuery();
								if (rs.next()) {
									String dbRank = rs.getString("rank_name");
									String dbPlatoon = rs.getString("platoon");
									String dbBattalion = rs.getString("battalion");
									String rank;
									if (dbRank != null)
										rank = dbRank;
									if (dbPlatoon != null)
										platoon = dbPlatoon;
									if (dbBattalion != null)
										battalion = dbBattalion;
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				String rank = "COCC Officer";

				try {

					int sid = Util.toInt(studentId);

					String sql = "SELECT rank_name FROM rotc_ranks WHERE student_id = ?";

					try (java.sql.Connection conn = util.DBConnection.getConnection();
							java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

						ps.setInt(1, sid);

						java.sql.ResultSet rs = ps.executeQuery();

						if (rs.next()) {
							rank = rs.getString("rank_name");
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				return new String[][] { { "NAME", name }, { "ID", String.valueOf(studentId) }, { "RANK", rank },
						{ "PLATOON", platoon != null ? platoon : "—" },
						{ "BATTALION", battalion != null ? battalion : "—" } };
			}

			@Override
			java.util.List<String[]> getSideNavItems() {
				java.util.List<String[]> items = new java.util.ArrayList<>();
				items.add(new String[] { "attendance", "Take Attendance" });
				items.add(new String[] { "demerits", "Issue Demerits" });
				items.add(new String[] { "calendar", "Calendar" });
				items.add(new String[] { "notifications", "Notifications" });
				items.add(new String[] { "account", "Account Settings" });
				return items;
			}

			@Override
			JPanel buildPanelFor(String key) {
				switch (key) {
				case "attendance":
					return new COCCAttendancePanel();
				case "demerits":
					return new COCCDemeritPanel();
				case "calendar":
					return new CalendarPanel("ROTC", GOLD, GOLD_SOFT, false);
				case "notifications":
					return new NotificationBoardPanel("ROTC", GOLD, GOLD_SOFT);
				case "account":
					return new AccountSettingsPanel(String.valueOf(studentId), GOLD, GOLD_SOFT);
				default:
					return new JPanel();
				}
			}
		}

		static class COCCAttendancePanel extends JPanel {
			JTextField sessionNameField;
			JSpinner dateSpinner;
			DefaultTableModel tableModel;
			JTable table;
			int loggedByUserId = 0;

			COCCAttendancePanel() {
				setLayout(new BorderLayout(0, 0));
				setBackground(BG);

				JPanel header = new JPanel();
				header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
				header.setOpaque(false);
				header.setBorder(BorderFactory.createEmptyBorder(28, 28, 0, 28));
				JLabel title = new JLabel("ROTC Attendance (COCC)");
				title.setFont(new Font("SansSerif", Font.BOLD, 20));
				title.setForeground(TEXT_MAIN);
				title.setAlignmentX(Component.LEFT_ALIGNMENT);
				JLabel sub = new JLabel("Log attendance for ROTC cadets under your command");
				sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
				sub.setForeground(TEXT_MUTED);
				sub.setAlignmentX(Component.LEFT_ALIGNMENT);
				header.add(title);
				header.add(Box.createVerticalStrut(3));
				header.add(sub);
				header.add(Box.createVerticalStrut(14));
				header.add(buildControls());

				add(header, BorderLayout.NORTH);
				add(buildTableArea(), BorderLayout.CENTER);
				add(buildBottom(), BorderLayout.SOUTH);
				loadStudents();
			}

			JPanel buildControls() {
				JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
				row.setOpaque(false);
				row.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

				sessionNameField = new JTextField(16);
				sessionNameField.setFont(new Font("SansSerif", Font.PLAIN, 13));
				sessionNameField.setBackground(WHITE);
				sessionNameField
						.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true),
								BorderFactory.createEmptyBorder(8, 12, 8, 12)));
				sessionNameField.setPreferredSize(new Dimension(200, 38));

				Date today = new Date();
				SpinnerDateModel dm = new SpinnerDateModel(today, null, null, java.util.Calendar.DAY_OF_MONTH);
				dateSpinner = new JSpinner(dm);
				JSpinner.DateEditor de = new JSpinner.DateEditor(dateSpinner, "MMM dd, yyyy");
				dateSpinner.setEditor(de);
				de.getTextField().setEditable(false);
				dateSpinner.setPreferredSize(new Dimension(160, 38));
				dateSpinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
				dateSpinner.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));

				JPanel sRow = new JPanel();
				sRow.setLayout(new BoxLayout(sRow, BoxLayout.Y_AXIS));
				sRow.setOpaque(false);
				JLabel sLbl = new JLabel("Session Name");
				sLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
				sLbl.setForeground(TEXT_SUB);
				sRow.add(sLbl);
				sRow.add(Box.createVerticalStrut(4));
				sRow.add(sessionNameField);

				JPanel dRow = new JPanel();
				dRow.setLayout(new BoxLayout(dRow, BoxLayout.Y_AXIS));
				dRow.setOpaque(false);
				JLabel dLbl = new JLabel("Date");
				dLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
				dLbl.setForeground(TEXT_SUB);
				dRow.add(dLbl);
				dRow.add(Box.createVerticalStrut(4));
				dRow.add(dateSpinner);

				row.add(sRow);
				row.add(dRow);
				return row;
			}

			JPanel buildTableArea() {
				JPanel area = new JPanel(new BorderLayout());
				area.setBackground(BG);
				area.setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 28));

				String[] cols = { "#", "Cadet Name", "ID", "Platoon", "Battalion", "Status" };
				tableModel = new DefaultTableModel(cols, 0) {
					public boolean isCellEditable(int r, int c) {
						return c == 5;
					}

					public Class<?> getColumnClass(int c) {
						return c == 5 ? String.class : Object.class;
					}
				};
				table = new JTable(tableModel);
				NSTPInfoSys.styleTable(table);
				table.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());
				table.getColumnModel().getColumn(5).setCellEditor(new StatusEditor());
				table.getColumnModel().getColumn(0).setMaxWidth(50);
				area.add(NSTPInfoSys.wrapTable(table), BorderLayout.CENTER);
				return area;
			}

			JPanel buildBottom() {
				JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 28, 14));
				bottom.setBackground(BG);
				bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
				JButton saveBtn = NSTPInfoSys.makeBtn("Save Session", GOLD, WHITE, 8);
				saveBtn.setPreferredSize(new Dimension(140, 40));
				saveBtn.addActionListener(e -> saveSession());
				JButton clearBtn = NSTPInfoSys.makeBtn("Clear", BG, TEXT_SUB, 8);
				clearBtn.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
				clearBtn.setPreferredSize(new Dimension(90, 40));
				clearBtn.addActionListener(e -> {
					for (int i = 0; i < tableModel.getRowCount(); i++)
						tableModel.setValueAt("Present", i, 5);
				});
				bottom.add(clearBtn);
				bottom.add(saveBtn);
				return bottom;
			}

			void loadStudents() {
				tableModel.setRowCount(0);

				java.util.List<String[]> students = dao.StudentDAO.getAllStudents("ROTC");

				int idx = 1;

				for (String[] s : students) {

					tableModel.addRow(new Object[] { idx++, s[1], s[0], s[4], "—", "Present" });
				}
			}

			void saveSession() {
				if (table.isEditing())
					table.getCellEditor().stopCellEditing();
				String name = sessionNameField.getText().trim();
				if (name.isEmpty()) {
					JOptionPane.showMessageDialog(this, "Please enter a session name.", "Missing",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
				java.util.Date spinnerDate = (java.util.Date) dateSpinner.getValue();
				java.util.List<int[]> studentIds = new java.util.ArrayList<>();
				java.util.List<String> statuses = new java.util.ArrayList<>();
				for (int i = 0; i < tableModel.getRowCount(); i++) {
					try {
						int sid = Integer.parseInt(tableModel.getValueAt(i, 2).toString());
						String status = tableModel.getValueAt(i, 5).toString();
						studentIds.add(new int[]{sid});
						statuses.add(status);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				int saved = dao.AttendanceDAO.saveSession(name, spinnerDate, studentIds, statuses, loggedByUserId);
				JOptionPane.showMessageDialog(this, saved + " attendance records saved for session: " + name, "Saved",
						JOptionPane.INFORMATION_MESSAGE);
			}

		}

		static class COCCDemeritPanel extends JPanel {

			private JComboBox<String> severityCombo = new JComboBox<>(new String[] { "Minor", "Moderate", "Major" });

			DefaultTableModel logModel;
			JComboBox<String> studentCombo;
			JTextField reasonField, pointsField;
			JSpinner dateSpinner;

			COCCDemeritPanel() {
				setLayout(new BorderLayout(0, 0));
				setBackground(BG);
				add(buildForm(), BorderLayout.WEST);
				add(buildLog(), BorderLayout.CENTER);
			}

			JPanel buildForm() {
				JPanel outer = new JPanel(new BorderLayout());
				outer.setBackground(WHITE);
				outer.setPreferredSize(new Dimension(360, 0));
				outer.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

				JPanel inner = new JPanel();
				inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
				inner.setBackground(WHITE);
				inner.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

				JLabel title = new JLabel("Issue Demerit (COCC)");
				title.setFont(new Font("SansSerif", Font.BOLD, 20));
				title.setForeground(TEXT_MAIN);
				title.setAlignmentX(Component.LEFT_ALIGNMENT);
				JLabel sub = new JLabel("Issue demerit points to cadets under your command");
				sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
				sub.setForeground(TEXT_MUTED);
				sub.setAlignmentX(Component.LEFT_ALIGNMENT);
				inner.add(title);
				inner.add(Box.createVerticalStrut(4));
				inner.add(sub);
				inner.add(Box.createVerticalStrut(22));
				inner.add(sep());
				inner.add(Box.createVerticalStrut(20));

				studentCombo = buildStudentCombo("ROTC");
				inner.add(comboRow("Cadet", studentCombo));
				inner.add(Box.createVerticalStrut(4));
				reasonField = formField();
				inner.add(formRow("Reason / Violation", reasonField));

				Date today = new Date();
				SpinnerDateModel dm = new SpinnerDateModel(today, null, null, java.util.Calendar.DAY_OF_MONTH);
				dateSpinner = new JSpinner(dm);
				JSpinner.DateEditor de = new JSpinner.DateEditor(dateSpinner, "MMM dd, yyyy");
				dateSpinner.setEditor(de);
				de.getTextField().setEditable(false);
				dateSpinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
				dateSpinner.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
				inner.add(spinnerRow("Date", dateSpinner));
				inner.add(Box.createVerticalStrut(4));
				pointsField = formField();
				inner.add(formRow("Demerit Points", pointsField));

				JButton issueBtn = NSTPInfoSys.makeBtn("Issue Demerit", RED, WHITE, 8);
				issueBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
				issueBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
				issueBtn.addActionListener(e -> doIssue());
				inner.add(Box.createVerticalStrut(6));
				inner.add(issueBtn);
				outer.add(inner, BorderLayout.CENTER);
				return outer;
			}

			JPanel buildLog() {
				JPanel panel = new JPanel(new BorderLayout());
				panel.setBackground(BG);
				JPanel header = new JPanel(new BorderLayout());
				header.setOpaque(false);
				header.setBorder(BorderFactory.createEmptyBorder(28, 24, 14, 24));
				JLabel t = new JLabel("Demerits Issued by COCC");
				t.setFont(new Font("SansSerif", Font.BOLD, 16));
				t.setForeground(TEXT_MAIN);
				header.add(t, BorderLayout.WEST);
				String[] cols = { "Student ID", "Name", "Platoon", "Reason", "Points", "Date" };
				logModel = new DefaultTableModel(cols, 0) {
					public boolean isCellEditable(int r, int c) {
						return false;
					}
				};
				JTable logTable = makeStdTable(logModel);
				logTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
					@Override
					public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc,
							int row, int col) {
						super.getTableCellRendererComponent(t, v, sel, foc, row, col);
						setForeground(RED);
						setFont(new Font("SansSerif", Font.BOLD, 12));
						setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
						setBackground(sel ? ACCENT_SOFT : row % 2 == 0 ? WHITE : BG);
						setOpaque(true);
						return this;
					}
				});
				JPanel tableWrap = new JPanel(new BorderLayout());
				tableWrap.setBackground(BG);
				tableWrap.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
				tableWrap.add(NSTPInfoSys.wrapTable(logTable), BorderLayout.CENTER);
				panel.add(header, BorderLayout.NORTH);
				panel.add(tableWrap, BorderLayout.CENTER);
				refreshLog();
				return panel;
			}

			void refreshLog() {
				logModel.setRowCount(0);
				String sql = "SELECT d.demerit_id, s.student_id, s.full_name, s.section, "
						+ "       d.reason, d.severity, d.officer_name, d.demerit_date, d.status " + "FROM demerits d "
						+ "JOIN students s ON d.student_id = s.student_id " + "WHERE s.is_deleted = 0 "
						+ "ORDER BY d.demerit_date DESC";
				try (java.sql.Connection conn = util.DBConnection.getConnection()) {
					if (conn == null)
						return;
					try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
							java.sql.ResultSet rs = ps.executeQuery()) {
						while (rs.next()) {
							logModel.addRow(new Object[] { rs.getString("student_id"), rs.getString("full_name"),
									rs.getString("section"), rs.getString("reason"), rs.getString("severity"),
									rs.getString("demerit_date") });
						}
					}
				} catch (java.sql.SQLException e) {
					System.err.println("[COCCDemeritPanel] refreshLog error: " + e.getMessage());
				}
			}

			void doIssue() {
				if (studentCombo.getSelectedIndex() < 0) {
					JOptionPane.showMessageDialog(this, "No cadets available.", "Error", JOptionPane.WARNING_MESSAGE);
					return;
				}
				String reason = reasonField.getText().trim();
				String severityStr = severityCombo != null ? (String) severityCombo.getSelectedItem() : "Minor";
				if (reason.isEmpty()) {
					JOptionPane.showMessageDialog(this, "Reason is required.", "Missing", JOptionPane.WARNING_MESSAGE);
					return;
				}
				String combo = (String) studentCombo.getSelectedItem();
				if (combo == null)
					return;
				String sidStr = combo.replaceAll(".*\\((.*)\\)$", "$1").trim();
				String sname = combo.replaceAll("\\s*\\(.*\\)$", "").trim();
				int sid;
				try {
					sid = Integer.parseInt(sidStr);
				} catch (NumberFormatException e) {
					JOptionPane.showMessageDialog(this, "Invalid student selection.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				java.util.Date spinnerDate = (java.util.Date) dateSpinner.getValue();
				String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(spinnerDate);
				String sql = "INSERT INTO demerits (student_id, reason, severity, demerit_date, status) "
						+ "VALUES (?, ?, ?, ?, 'Active')";
				try (java.sql.Connection conn = util.DBConnection.getConnection()) {
					if (conn == null) {
						JOptionPane.showMessageDialog(this, "Database connection failed.", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
						ps.setInt(1, sid);
						ps.setString(2, reason);
						ps.setString(3, severityStr);
						ps.setString(4, dateStr);
						ps.executeUpdate();
					}
				} catch (java.sql.SQLException e) {
					JOptionPane.showMessageDialog(this, "Failed to issue demerit:\n" + e.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				refreshLog();
				reasonField.setText("");
				JOptionPane.showMessageDialog(this, "Demerit issued to " + sname + ".", "Issued",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}

		static class ROTCPerformanceStudentPanel extends JPanel {
			String studentId;

			ROTCPerformanceStudentPanel(String studentId) {
				this.studentId = studentId;
				setLayout(new BorderLayout());
				setBackground(BG);

				JPanel header = new JPanel();
				header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
				header.setOpaque(false);
				header.setBorder(BorderFactory.createEmptyBorder(28, 28, 20, 28));

				JLabel title = new JLabel("Performance Record");
				title.setFont(new Font("SansSerif", Font.BOLD, 20));
				title.setForeground(TEXT_MAIN);
				title.setAlignmentX(Component.LEFT_ALIGNMENT);

				JLabel sub = new JLabel("Your drill ratings and performance history");
				sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
				sub.setForeground(TEXT_MUTED);
				sub.setAlignmentX(Component.LEFT_ALIGNMENT);

				header.add(title);
				header.add(Box.createVerticalStrut(3));
				header.add(sub);
				header.add(Box.createVerticalStrut(12));

				DefaultTableModel perfModel = new DefaultTableModel(
						new String[] { "Drill", "Date", "Rating", "Officer", "Remarks" }, 0) {
					public boolean isCellEditable(int r, int c) {
						return false;
					}
				};
				JTable perfTable = makeStdTable(perfModel);
				java.util.List<String[]> perfList = dao.PerformanceDAO.getPerformanceForStudent(studentId);
				for (String[] row : perfList) {

					perfModel.addRow(new Object[] { row[1], row[2], row[3], row[5], row[4] });
				}
				JPanel tableWrap = new JPanel(new BorderLayout());
				tableWrap.setBackground(BG);
				tableWrap.setBorder(BorderFactory.createEmptyBorder(0, 28, 28, 28));
				tableWrap.add(NSTPInfoSys.wrapTable(perfTable), BorderLayout.CENTER);

				add(header, BorderLayout.NORTH);
				add(tableWrap, BorderLayout.CENTER);
			}
		}

		static class PointsStudentPanel extends JPanel {
			String studentId;

			PointsStudentPanel(String studentId) {
				this.studentId = studentId;
				setLayout(new BorderLayout());
				setBackground(BG);

				JPanel header = new JPanel();
				header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
				header.setOpaque(false);
				header.setBorder(BorderFactory.createEmptyBorder(28, 28, 20, 28));

				JLabel title = new JLabel("Points Standing");
				title.setFont(new Font("SansSerif", Font.BOLD, 20));
				title.setForeground(TEXT_MAIN);
				title.setAlignmentX(Component.LEFT_ALIGNMENT);

				JLabel sub = new JLabel("Your point transactions and current balance");
				sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
				sub.setForeground(TEXT_MUTED);
				sub.setAlignmentX(Component.LEFT_ALIGNMENT);

				header.add(title);
				header.add(Box.createVerticalStrut(3));
				header.add(sub);

				int sid = -1;
				try { sid = Integer.parseInt(studentId); } catch (NumberFormatException ignored) {}

				int balance = sid >= 0 ? dao.PointsDAO.getBalance(sid) : 0;

				JPanel balanceCard = new JPanel();
				balanceCard.setLayout(new BoxLayout(balanceCard, BoxLayout.Y_AXIS));
				balanceCard.setOpaque(false);
				balanceCard.setBorder(BorderFactory.createEmptyBorder(0, 28, 16, 28));

				JLabel balLbl = new JLabel("Current Balance");
				balLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
				balLbl.setForeground(TEXT_MUTED);
				balLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

				JLabel balVal = new JLabel(balance + " pts");
				balVal.setFont(new Font("SansSerif", Font.BOLD, 28));
				balVal.setForeground(balance >= 0 ? GREEN : RED);
				balVal.setAlignmentX(Component.LEFT_ALIGNMENT);

				balanceCard.add(balLbl);
				balanceCard.add(Box.createVerticalStrut(4));
				balanceCard.add(balVal);
				balanceCard.add(Box.createVerticalStrut(16));

				DefaultTableModel txModel = new DefaultTableModel(
						new String[]{"Date", "Type", "Amount", "Reason"}, 0) {
					public boolean isCellEditable(int r, int c) { return false; }
				};
				JTable txTable = makeStdTable(txModel);
				if (sid >= 0) {
					for (String[] row : dao.PointsDAO.getStudentTransactions(sid)) {
						txModel.addRow(new Object[]{row[4], row[2], row[1], row[3]});
					}
				}
				JScrollPane txScroll = new JScrollPane(txTable);
				txScroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
				txScroll.getViewport().setBackground(WHITE);

				JPanel tableWrap = new JPanel(new BorderLayout());
				tableWrap.setOpaque(false);
				tableWrap.setBorder(BorderFactory.createEmptyBorder(0, 28, 28, 28));
				tableWrap.add(txScroll, BorderLayout.CENTER);

				JPanel center = new JPanel(new BorderLayout());
				center.setOpaque(false);
				center.add(balanceCard, BorderLayout.NORTH);
				center.add(tableWrap, BorderLayout.CENTER);

				add(header, BorderLayout.NORTH);
				add(center, BorderLayout.CENTER);

			}
		}

		static JTable makeStdTable(DefaultTableModel model) {
			JTable t = new JTable(model) {
				@Override
				public Component prepareRenderer(TableCellRenderer r, int row, int col) {
					Component c = super.prepareRenderer(r, row, col);
					c.setBackground(isRowSelected(row) ? ACCENT_SOFT : row % 2 == 0 ? WHITE : BG);
					c.setForeground(TEXT_MAIN);
					((JComponent) c).setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
					return c;
				}
			};
			NSTPInfoSys.styleTable(t);
			return t;
		}
	}
}