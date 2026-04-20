import java.util.Set;

public class PointLine {
    private final Point p1;
    private final Point p2;

    private int connectionId = -1;

    public PointLine(Point p1, Point p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public double length() {
        double dx = p2.getX()-p1.getX();
        double dy = p2.getY()-p1.getY();
        return Math.sqrt(dx*dx+dy*dy);
    }

    public boolean hasLengthZero() {
        return p1.equals(p2);
    }

    public boolean isEquivalentTo(PointLine line) {
        //two ways of the lines being equivalent
        if(p1.equals(line.p1) && p2.equals(line.p2)) return true;
        if(p1.equals(line.p2) && p2.equals(line.p1)) return true;

        return false;
    }

    public boolean connectsTo(PointLine line, Set<Point> pointSet) {
        //we check if the matching point is in the list of points where exactly two lines meet

        if(this.isEquivalentTo(line) || this.hasLengthZero()) return false;

        if((p1.equals(line.p1) || p1.equals(line.p2)) && pointSet.contains(p1)) return true;
        if((p2.equals(line.p1) || p2.equals(line.p2)) && pointSet.contains(p2)) return true;

        return false;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public Point getP1() {
        return p1;
    }

    public Point getP2() {
        return p2;
    }
}
