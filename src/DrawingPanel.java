import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class DrawingPanel extends JPanel {

    private final List<PointLine> lines;

    private final Map<Integer, Color> colorMap = new HashMap<>();
    private final Map<Integer, Double> lengthMap = new HashMap<>();

    List<Map.Entry<Integer, Double>> sorted;

    private final List<Color> colors = List.of(
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA,
            Color.CYAN, Color.PINK, Color.DARK_GRAY, Color.YELLOW, Color.GRAY
    );

    private double zoom = 1.0;

    private int offsetX = 0;
    private int offsetY = 0;

    private int lastMouseX;
    private int lastMouseY;

    public DrawingPanel(List<PointLine> lines) {
        this.lines = lines;

        validateConnectionIds();
        buildColorMap();
        computeLengths();

        setBackground(Color.WHITE);

        addPanningAndZooming();
    }

    private void validateConnectionIds() {
        for (PointLine line : lines) {
            if (line.getConnectionId() == -1) {
                throw new RuntimeException("Eine Linie hat keine gültige connectionId!");
            }
        }
    }

    private void buildColorMap() {
        int index = 0;

        for (PointLine line : lines) {
            int id = line.getConnectionId();

            if (!colorMap.containsKey(id)) {
                Color color = colors.get(index % colors.size());
                colorMap.put(id, color);
                index++;
            }
        }
    }

    private void computeLengths() {
        lines.forEach(line -> {
            int id = line.getConnectionId();
            double length = line.length();
            lengthMap.put(id, lengthMap.getOrDefault(id, 0.0) + length);
        });

        sorted = new ArrayList<>(lengthMap.entrySet());

        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        System.out.println("Linienzüge nach Länge sortiert:");

        sorted.forEach(entry -> {
            int id = entry.getKey();
            double length = entry.getValue();
            String lengthInfo = "ID " + id + ": " + String.format("%.2f", length);
            System.out.println(lengthInfo);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(2));

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.translate(offsetX, offsetY);
        g2.scale(zoom, zoom);

        int width = getWidth();
        int height = getHeight();

        int paddingLeft = 40;
        int paddingBottom = 40;
        int paddingTop = 20;

        int legendWidth = 200;
        int drawWidth = width - legendWidth - paddingLeft;
        int drawHeight = height - paddingBottom - paddingTop;

        //Skalierung berechnen
        double maxX = lines.stream()
                .flatMap(l -> Stream.of(l.getP1(), l.getP2()))
                .mapToDouble(Point::getX)
                .max().orElse(1);

        double maxY = lines.stream()
                .flatMap(l -> Stream.of(l.getP1(), l.getP2()))
                .mapToDouble(Point::getY)
                .max().orElse(1);

        //auf nächsten 50er aufrunden
        maxX = Math.ceil(maxX / 50.0) * 50;
        maxY = Math.ceil(maxY / 50.0) * 50;

        double scaleX = drawWidth / maxX;
        double scaleY = drawHeight / maxY;

        //Koordinaten zeichnen
        drawGridAndAxes(g2,
                paddingLeft, paddingTop,
                drawWidth, drawHeight,
                maxX, maxY,
                scaleX, scaleY);

        //Linien zeichnen
        for (PointLine line : lines) {
            int id = line.getConnectionId();
            g2.setColor(colorMap.get(id));

            int x1 = paddingLeft + (int) (line.getP1().getX() * scaleX);
            int y1 = paddingTop + (int) (line.getP1().getY() * scaleY);

            int x2 = paddingLeft + (int) (line.getP2().getX() * scaleX);
            int y2 = paddingTop + (int) (line.getP2().getY() * scaleY);

            //y-Achse drehen
            y1 = paddingTop + drawHeight - (y1 - paddingTop);
            y2 = paddingTop + drawHeight - (y2 - paddingTop);

            g2.drawLine(x1, y1, x2, y2);

            //Punkte zeichnen (schwarz)
            g2.setColor(Color.BLACK);
            drawPoint(g2, x1, y1);
            drawPoint(g2, x2, y2);
        }

        drawLegend(g2, drawWidth + paddingLeft, height);
    }

    private void drawPoint(Graphics2D g2, int x, int y) {
        int size = 6;
        g2.fillOval(x - size / 2, y - size / 2, size, size);
    }

    private void drawLegend(Graphics2D g2, int startX, int height) {

        g2.setFont(new Font("SansSerif", Font.BOLD, 17));

        int y = 30;

        for (Map.Entry<Integer, Double> entry : sorted) {
            int id = entry.getKey();
            double length = entry.getValue();

            //Farbfeld
            g2.setColor(colorMap.get(id));
            g2.fillRect(startX + 20, y - 10, 20, 20);

            //Text
            g2.setColor(Color.BLACK);
            String lengthInfo = "ID " + id + ": " + String.format("%.2f", length);
            g2.drawString(lengthInfo,startX + 50, y + 5);

            y += 35;
        }
    }

    private void drawGridAndAxes(Graphics2D g2,
                                 int paddingLeft, int paddingTop,
                                 int drawWidth, int drawHeight,
                                 double maxX, double maxY,
                                 double scaleX, double scaleY) {

        int originX = paddingLeft;
        int originY = paddingTop + drawHeight;

        int gridSize = 50;

        //Grid (50er Schritte)
        g2.setStroke(new BasicStroke(1));
        g2.setColor(Color.LIGHT_GRAY);

        for (int x = 0; x <= maxX; x += gridSize) {
            int px = originX + (int) (x * scaleX);
            g2.drawLine(px, paddingTop, px, originY);
        }

        for (int y = 0; y <= maxY; y += gridSize) {
            int py = originY - (int) (y * scaleY);
            g2.drawLine(originX, py, originX + drawWidth, py);
        }

        //Achsen (mit Pfeilen)
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));

        //x-Achse
        g2.drawLine(originX, originY, originX + drawWidth, originY);

        //y-Achse
        g2.drawLine(originX, originY, originX, paddingTop);

        //Pfeile zeichnen
        drawArrow(g2, originX, paddingTop, originX, paddingTop + 10); // Y
        drawArrow(g2, originX + drawWidth, originY, originX + drawWidth - 10, originY); // X

        //Beschriftung (100er Schritte)
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));

        for (int x = 0; x <= maxX; x += gridSize*2) {
            int px = originX + (int) (x * scaleX);
            g2.drawString(String.valueOf(x), px - 10, originY + 18);
        }

        for (int y = gridSize*2; y <= maxY; y += gridSize*2) {
            int py = originY - (int) (y * scaleY);
            g2.drawString(String.valueOf(y), originX - 30, py + 5);
        }

        int tickSize = 6;

        //Ticks auf der x-Achse (bei 100er Schritten)
        for (int x = 0; x < maxX; x += gridSize*2) {
            int px = originX + (int) (x * scaleX);

            g2.drawLine(
                    px,
                    originY,
                    px,
                    originY - tickSize
            );
        }

        //Ticks auf der y-Achse (bei 100er Schritten)
        for (int y = 0; y < maxY; y += gridSize*2) {
            int py = originY - (int) (y * scaleY);

            g2.drawLine(
                    originX,
                    py,
                    originX + tickSize,
                    py
            );
        }
    }

    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
        g2.drawLine(x1, y1, x2, y2);

        double phi = Math.toRadians(25);
        int barb = 10;

        double dy = y1 - y2;
        double dx = x1 - x2;
        double theta = Math.atan2(dy, dx);

        double rho = theta + phi;
        for (int i = 0; i < 2; i++) {
            int x = (int) (x1 - barb * Math.cos(rho));
            int y = (int) (y1 - barb * Math.sin(rho));
            g2.drawLine(x1, y1, x, y);
            rho = theta - phi;
        }
    }

    private void addPanningAndZooming() {
        addMouseWheelListener(e -> {
            double zoomFactor = 1.1;

            double oldZoom = zoom;

            if (e.getPreciseWheelRotation() < 0) {
                zoom *= zoomFactor;
            } else {
                zoom /= zoomFactor;
            }

            double scaleChange = zoom / oldZoom;

            offsetX = (int) (e.getX() - scaleChange * (e.getX() - offsetX));
            offsetY = (int) (e.getY() - scaleChange * (e.getY() - offsetY));

            repaint();
        });

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        addMouseMotionListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                int dx = e.getX() - lastMouseX;
                int dy = e.getY() - lastMouseY;

                offsetX += dx;
                offsetY += dy;

                lastMouseX = e.getX();
                lastMouseY = e.getY();

                repaint();
            }
        });
    }
}