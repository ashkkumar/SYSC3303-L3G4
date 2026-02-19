package FireFightingDroneSwarm.UserInterface;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.Border;

public class UserInterface extends JFrame {

    private static final int ZONES = 5;
    private JPanel[][][] zoneGrids = new JPanel[ZONES][ROWS][COLS];

    private static final int ROWS = 5;
    private static final int COLS = 5;





    public UserInterface() {
        setTitle("Fire Watch Drone Grid");
        setSize(1000, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Use a purple/dark border for the zones as seen in the image
        Border zoneBorder = BorderFactory.createLineBorder(new Color(120, 100, 180), 1);

        JPanel gridPanel = new JPanel();
        gridPanel.setLayout(new GridLayout(1, 5, 10, 10)); // Temporary simple layout

        for (int z = 0; z < ZONES; z++) {
            JPanel zonePanel = new JPanel();
            zonePanel.setLayout(new GridLayout(ROWS, COLS));
            zonePanel.setBorder(zoneBorder); // Purple outer border

            JPanel[][] grid = new JPanel[ROWS][COLS];
            zoneGrids[z] = grid;

            // Pass the zone label (e.g., "Z(1)") to the build method
            buildZone(zonePanel, grid, "Z(" + (z + 1) + ")");

            gridPanel.add(zonePanel);
        }

        JPanel legendContainer = createLegendPanel();
        add(legendContainer, BorderLayout.EAST);

        // Standard text area setup
        // JTextArea textArea = new JTextArea();
        add(gridPanel, BorderLayout.CENTER);

    }

    /**
     * Stylistic Enhancements implemented here
     */
    public void buildZone(JPanel zone, JPanel[][] grid, String labelText) {
        // Light gray border for the internal grid lines
        Border cellBorder = BorderFactory.createLineBorder(new Color(230, 230, 230));

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                JPanel cell = new JPanel(new BorderLayout());
                cell.setBackground(Color.WHITE); // Change 1: White background
                cell.setBorder(cellBorder);      // Change 2: Subtle light gray grid

                // Change 3: Add the Z(n) label to the top-left cell only
                if (r == 0 && c == 0) {
                    JLabel label = new JLabel(labelText);
                    label.setFont(new Font("SansSerif", Font.BOLD, 10));
                    label.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));
                    cell.add(label, BorderLayout.NORTH);
                }

                grid[r][c] = cell;
                zone.add(cell);
            }
        }
    }

    private JPanel createLegendPanel() {
        JPanel legend = new JPanel(new GridBagLayout());
        legend.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        legend.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        addLegendItem(legend, gbc, "Z(n)", new Color(220, 235, 220), "Zone Label", Color.BLACK);
        addLegendItem(legend, gbc, "", Color.RED, "Active fire", Color.WHITE);
        addLegendItem(legend, gbc, "", new Color(80, 150, 70), "Extinguished fire", Color.WHITE);
        addLegendItem(legend, gbc, "D(n)", new Color(255, 165, 0), "Drone outbound", Color.BLACK);
        addLegendItem(legend, gbc, "D(n)", new Color(34, 139, 34), "Drone Extinguishing fire", Color.WHITE);
        addLegendItem(legend, gbc, "D(3)", new Color(180, 130, 180), "Drone Returning", Color.BLACK);
        return legend;
    }

    private void addLegendItem(JPanel panel, GridBagConstraints gbc, String iconText, Color boxColor, String description, Color textColor) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setBackground(Color.WHITE);

        // The colored square/icon
        JPanel icon = new JPanel(new GridBagLayout());
        icon.setPreferredSize(new Dimension(40, 25));
        icon.setBackground(boxColor);
        icon.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        if (!iconText.isEmpty()) {
            JLabel text = new JLabel(iconText);
            text.setFont(new Font("SansSerif", Font.BOLD, 11));
            text.setForeground(textColor);
            icon.add(text);
        }

        // The label text
        JLabel label = new JLabel(description);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));

        row.add(icon);
        row.add(label);
        panel.add(row, gbc);
    }

    public void setZoneBackgroundColor(int zoneIndex, int x, int y, Color color) {


        // Check to ensure the index/x/y is valid
        if (zoneIndex < 0 || zoneIndex >= ZONES || x > 5 || y > 5) return;

        zoneGrids[zoneIndex][x][y].setBackground(color);
    }

    public static void main(String[] args) {
        UserInterface ui = new UserInterface();
        ui.setZoneBackgroundColor(2, 1, 1, new Color(255, 255, 200));
        ui.setVisible(true);

    }
}