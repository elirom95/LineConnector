import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    static String resourceName = "input.txt";

    static int frameWidth = 830;
    static int frameHeight = 600;

    public static void main(String[] args) {
        //die Listen dienen dazu, alle Punkte bzw. Linien aus dem Datensatz zu erfassen
        List<Point> pointList = new ArrayList<>();
        List<PointLine> lineList = new ArrayList<>();

        //befüllt die Listen
        fillListsFromResource(pointList, lineList);

        //erstellt ein Set mit allen Punkten, an denen die Linien zusammengefügt werden sollen
        Set<Point> matchingPoints = generateMatchingPoints(pointList);

        //definiert, welche Linien miteinander verbunden sind (selbe connectionId)
        //zunächst werden nicht-geschlossene Linienzüge verbunden
        int lineGroupNumber = generateConnections(lineList, matchingPoints,0, false);
        //daraufhin werden geschlossene Linienzüge verbunden
        generateConnections(lineList, matchingPoints, lineGroupNumber,true);

        //erzeugt die Darstellung mithilfe der Linienliste, die nun Info zur Verbindung der Linien enthält
        visualizeInFrame(lineList);
    }

    private static void fillListsFromResource(List<Point> pointList, List<PointLine> lineList) {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream(resourceName))
                )
        );

        reader.lines().forEach(s-> {
            s = s.trim();
            String[] coordinates = s.split(" ");
            if (coordinates.length != 4) throw new RuntimeException("Ungültige Koordinaten");
            Point p1 = new Point(Double.parseDouble(coordinates[0]),Double.parseDouble(coordinates[1]));
            Point p2 = new Point(Double.parseDouble(coordinates[2]),Double.parseDouble(coordinates[3]));
            pointList.add(p1);
            pointList.add(p2);
            lineList.add(new PointLine(p1,p2));
        });
    }


    //Wir weisen hiermit Linien eine connectionId zu. Zwei Linien mit derselben id gehören zum selben Linienzug.
    private static int generateConnections(List<PointLine> lineList, Set<Point> matchingPoints, int initialCounter, boolean isCyclic) {
        int counter = initialCounter;

        for (PointLine pl : lineList) {
            //eine Linie, die bereits eine nicht-triviale connectionId besitzt, wird übersprungen
            if (pl.getConnectionId() != -1) continue;

            if (!isCyclic) {
                boolean p1Matches = matchingPoints.contains(pl.getP1());
                boolean p2Matches = matchingPoints.contains(pl.getP2());

                //isolierte Linienstücke erhalten sofort eine einzigartige connectionId
                if (!p1Matches && !p2Matches) {
                    pl.setConnectionId(counter++);
                    continue;
                }
                //bei nicht-geschlossenen Linien wollen wir an einem Endpunkt beginnen, nicht an einem Mittelstück
                if (p1Matches && p2Matches) continue;
            }

            //wir weisen nun eine connectionId zu und finden im nächsten Schritt alle verbundenen Linien
            pl.setConnectionId(counter);

            PointLine current = pl;
            Optional<PointLine> next;
            //solange es noch eine weitere Linie im selben Linienzug gibt, läuft der while-loop weiter
            while ((next = findNextConnected(current, lineList, matchingPoints)).isPresent()) {
                current = next.get();
                current.setConnectionId(counter);
            }

            counter++;
        }
        //falls initialCounter == 0, wird die Gesamtzahl an Linienzügen zurückgegeben
        return counter;
    }

    //Hilfsmethode, um die nächste Linie im Linienzug zu finden
    private static Optional<PointLine> findNextConnected(PointLine pl, List<PointLine> lineList, Set<Point> matchingPoints) {
        return lineList.stream()
                .filter(temp -> temp.getConnectionId() == -1 && pl.connectsTo(temp, matchingPoints))
                .findAny();
    }

    //gibt ein Set zurück, in dem genau die Punkte zurückbleiben, an denen die Linien verbunden werden sollen
    private static Set<Point> generateMatchingPoints(List<Point> pointList) {
        return pointList.stream()
                //nur Punkte, die exakt zweimal auftauchen, sollen übrig bleiben
                .filter(p-> Collections.frequency(pointList,p) == 2)
                .collect(Collectors.toSet());
    }

    private static void visualizeInFrame(List<PointLine> lineList) {
        JFrame frame = new JFrame("Linien-Visualisierung");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(frameWidth, frameHeight);

        //im Konstruktor des DrawingPanel werden die Linien der lineList dargestellt
        //und die Längen der Linienzüge berechnet und geordnet
        frame.add(new DrawingPanel(lineList));

        frame.setVisible(true);
    }

}
