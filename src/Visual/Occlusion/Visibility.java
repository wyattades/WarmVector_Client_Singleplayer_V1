package Visual.Occlusion;

import Map.TileMap;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Directory: WarmVector_Client_Singleplayer/${PACKAGE_NAME}/
 * Created by Wyatt on 5/7/2015.
 */
public class Visibility {

/* 2d visibility algorithm, for demo
   Usage:
      new Visibility()
   Whenever map data changes:
      loadMap
   Whenever light source changes:
      setLightLocation
   To calculate the area:
      sweep
*/

    // Note: DLL is a doubly linked list but an array would be ok too

    // These represent the map and the light location:
    private ArrayList<Segment> segments;
    private ArrayList<EndPoint> endpoints;
    private Point center;

    // These are currently 'open' line segments, sorted so that the nearest
    // segment is first. It's used only during the sweep algorithm, and exposed
    // as a public field here so that the demo can display it.
    private ArrayList<Segment> open;

    // The output is a series of points that forms a visible area polygon
    public ArrayList<Point> output;

    // For the demo, keep track of wall intersections
    private ArrayList<ArrayList<Point>> demo_intersectionsDetected;

    private TileMap tileMap;

    // Construct an empty visibility set
    public Visibility(TileMap tileMap) {
        this.tileMap = tileMap;
        segments = new ArrayList<Segment>();
        endpoints = new ArrayList<EndPoint>();
        open = new ArrayList<Segment>();
        center = new Point(0, 0);
        output = new ArrayList<Point>();
        demo_intersectionsDetected = new ArrayList<ArrayList<Point>>();
        loadTileMap(tileMap);
    }

    private void loadTileMap(TileMap map) {
        segments.clear();
        endpoints.clear();
        int[][] array = map.tileArray;
        int w = map.width;
        int h = map.height;
        int size = TileMap.tileSize;

        for (int i = 0; i < map.width; i++) {
            for (int j = 0; j < map.height; j++) {
                //if a tile is solid or in the border of the map, add segments for each side of the tile
                if (array[i][j] == TileMap.SOLID || (i == 0 || i == w - 1 || j == 0 || j == h - 1) ) {
                    //add a segment if there is not an existing segment in that spac
                    addSegment( (i+.5f)* size - size/2, (j+.5f)*size + size/2,
                            (i+.5f)* size + size/2, (j+.5f)*size + size/2);
                    addSegment( (i+.5f)* size + size/2, (j+.5f)*size + size/2,
                            (i+.5f)* size + size/2, (j+.5f)*size - size/2);
                    addSegment( (i+.5f)* size + size/2, (j+.5f)*size - size/2,
                            (i+.5f)* size - size/2, (j+.5f)*size - size/2);
                    addSegment( (i+.5f)* size - size/2, (j+.5f)*size - size/2,
                            (i+.5f)* size - size/2, (j+.5f)*size + size/2);
                }
            }
        }
    }

    public void draw(Graphics2D g) {
        Polygon CUTOUT = new Polygon();
        for (Point s : output) {
            CUTOUT.addPoint((int)s.x,(int)s.y);
        }
        Polygon BORDER = new Polygon();
        BORDER.addPoint(-tileMap.width* TileMap.tileSize,-tileMap.height* TileMap.tileSize);
        BORDER.addPoint(2*tileMap.width * TileMap.tileSize, -tileMap.height* TileMap.tileSize);
        BORDER.addPoint(2*tileMap.width * TileMap.tileSize,2*tileMap.height* TileMap.tileSize);
        BORDER.addPoint(-tileMap.width * TileMap.tileSize, 2*tileMap.height* TileMap.tileSize);
        GeneralPath SHADOW = new GeneralPath(BORDER);
        SHADOW.append(CUTOUT,true);
        g.setColor(new Color(20,20,20,255));
        g.fill(SHADOW);
    }

    // Add a segment, where the first point shows up in the
    // visualization but the second one does not. (Every endpoint is
    // part of two segments, but we want to only show them once.)
    private void addSegment(float x1, float y1, float x2, float y2) {
        EndPoint p1 = new EndPoint(x1, y1);
        EndPoint p2 = new EndPoint(x2, y2);
        Segment segment = null;
        p1.segment = p2.segment = segment;
        p1.visualize = true;
        p2.visualize = false;
        segment = new Segment();
        p1.segment = p2.segment = segment;
        segment.p1 = p1;
        segment.p2 = p2;
        segment.d = 0;
        segments.add(segment);
        endpoints.add(p1);
        endpoints.add(p2);

    }


    // Set the light location. Segment and EndPoint data can't be
    // processed until the light location is known.
    public void setLightLocation(float x, float y) {
        center.x = x;
        center.y = y;

        for (Segment segment : segments) {
            float dx = 0.5f * (segment.p1.x + segment.p2.x) - x;
            float dy = 0.5f * (segment.p1.y + segment.p2.y) - y;
            // NOTE: we only use this for comparison so we can use
            // distance squared instead of distance. However in
            // practice the sqrt is plenty fast and this doesn't
            // really help in this situation.
            segment.d = dx*dx + dy*dy;

            // NOTE: future optimization: we could record the quadrant
            // and the y/x or x/y ratio, and sort by (quadrant,
            // ratio), instead of calling atan2. See
            // <https://github.com/mikolalysenko/compare-slope> for a
            // library that does this. Alternatively, calculate the
            // angles and use bucket sort to get an O(N) sort.
            segment.p1.angle = (float) Math.atan2(segment.p1.y - y, segment.p1.x - x);
            segment.p2.angle = (float) Math.atan2(segment.p2.y - y, segment.p2.x - x);

            float dAngle = segment.p2.angle - segment.p1.angle;
            if (dAngle <= -Math.PI) { dAngle += 2*Math.PI; }
            if (dAngle > Math.PI) { dAngle -= 2*Math.PI; }
            segment.p1.begin = (dAngle > 0.0);
            segment.p2.begin = !segment.p1.begin;
        }
    }

    // Helper: leftOf(segment, point) returns true if point is "left"
    // of segment treated as a vector. Note that this assumes a 2D
    // coordinate system in which the Y axis grows downwards, which
    // matches common 2D graphics libraries, but is the opposite of
    // the usual convention from mathematics and in 3D graphics
    // libraries.
    private boolean leftOf(Segment s, Point p) {
        // This is based on a 3d cross product, but we don't need to
        // use z coordinate inputs (they're 0), and we only need the
        // sign. If you're annoyed that cross product is only defined
        // in 3d, see "outer product" in Geometric Algebra.
        // <http://en.wikipedia.org/wiki/Geometric_algebra>
        float cross = (s.p2.x - s.p1.x) * (p.y - s.p1.y)
                - (s.p2.y - s.p1.y) * (p.x - s.p1.x);
        return cross < 0;
        // Also note that this is the naive version of the test and
        // isn't numerically robust. See
        // <https://github.com/mikolalysenko/robust-arithmetic> for a
        // demo of how this fails when a point is very close to the
        // line.
    }

    // Return p*(1-f) + q*f
    private Point interpolate(Point p, Point q, float f) {
        return new Point(p.x*(1-f) + q.x*f, p.y*(1-f) + q.y*f);
    }

    // Helper: do we know that segment a is in front of b?
    // Implementation not anti-symmetric (that is to say,
    // _segment_in_front_of(a, b) != (!_segment_in_front_of(b, a)).
    // Also note that it only has to work in a restricted set of cases
    // in the visibility algorithm; I don't think it handles all
    // cases. See http://www.redblobgames.com/articles/visibility/segment-sorting.html
    private boolean _segment_in_front_of(Segment a, Segment b, Point relativeTo) {
        // NOTE: we slightly shorten the segments so that
        // intersections of the endpoints (common) don't count as
        // intersections in this algorithm
        boolean A1 = leftOf(a, interpolate(b.p1, b.p2, 0.01f));
        boolean A2 = leftOf(a, interpolate(b.p2, b.p1, 0.01f));
        boolean A3 = leftOf(a, relativeTo);
        boolean B1 = leftOf(b, interpolate(a.p1, a.p2, 0.01f));
        boolean B2 = leftOf(b, interpolate(a.p2, a.p1, 0.01f));
        boolean B3 = leftOf(b, relativeTo);

        // NOTE: this algorithm is probably worthy of a short article
        // but for now, draw it on paper to see how it works. Consider
        // the line A1-A2. If both B1 and B2 are on one side and
        // relativeTo is on the other side, then A is in between the
        // viewer and B. We can do the same with B1-B2: if A1 and A2
        // are on one side, and relativeTo is on the other side, then
        // B is in between the viewer and A.
        if (B1 == B2 && B2 != B3) return true;
        if (A1 == A2 && A2 == A3) return true;
        if (A1 == A2 && A2 != A3) return false;
        if (B1 == B2 && B2 == B3) return false;

        // If A1 != A2 and B1 != B2 then we have an intersection.
        // Expose it for the GUI to show a message. A more robust
        // implementation would split segments at intersections so
        // that part of the segment is in front and part is behind.
        ArrayList<Point> points = new ArrayList<Point>();
        points.add(a.p1);
        points.add(a.p2);
        points.add(b.p1);
        points.add(b.p2);
        demo_intersectionsDetected.add(points);
        return false;

        // NOTE: previous implementation was a.d < b.d. That's simpler
        // but trouble when the segments are of dissimilar sizes. If
        // you're on a grid and the segments are similarly sized, then
        // using distance will be a simpler and faster implementation.
    }


    // Run the algorithm, sweeping over all or part of the circle to find
    // the visible area, represented as a set of triangles
    public void sweep(float maxAngle) { //maxAngle:Float=999.0
        output = new ArrayList<Point>();  // output set of triangles
        demo_intersectionsDetected = new ArrayList<ArrayList<Point>>();
        Collections.sort(endpoints, new Comparator<EndPoint>() {
            // Helper: comparison function for sorting points by angle
            @Override
            public int compare(EndPoint a, EndPoint b) {
                // Traverse in angle order
                if (a.angle > b.angle) return 1;
                if (a.angle < b.angle) return -1;
                // But for ties (common), we want Begin nodes before End nodes
                if (!a.begin && b.begin) return 1;
                if (a.begin && !b.begin) return -1;
                return 0;
            }
        });

        open.clear();
        float beginAngle = 0;

        // At the beginning of the sweep we want to know which
        // segments are active. The simplest way to do this is to make
        // a pass collecting the segments, and make another pass to
        // both collect and process them. However it would be more
        // efficient to go through all the segments, figure out which
        // ones intersect the initial sweep line, and then sort them.
        for (int i = 0; i < 2; i++) { //0-3 or 0-2????
            for (EndPoint p : endpoints) {
                if (i == 1 && p.angle > maxAngle) {
                    // Early exit for the visualization to show the sweep process
                    break;
                }

                Segment current_old = open.isEmpty()? null : open.get(0);

                if (p.begin) {
                    // Insert into the right place in the list
                    Segment node = open.isEmpty()? null : open.get(0);
                    while (node != null && _segment_in_front_of(p.segment, node, center)) {
                        node = open.size()==open.indexOf(node)+1? null:open.get((open.indexOf(node) + 1));
                    }
                    if (node == null) {
                        open.add(p.segment);
                    } else {
                        open.add(open.indexOf(node), p.segment); // (index-1) or just (index)???
                    }
                }
                else {
                    open.remove(p.segment);
                }

                Segment current_new = open.isEmpty()? null : open.get(0);
                if (current_old != current_new) {
                    if (i == 1) {
                        addTriangle(beginAngle, p.angle, current_old);
                    }
                    beginAngle = p.angle;
                }
            }
        }
    }


    private Point lineIntersection(Point p1, Point p2, Point p3, Point p4) {
        // From http://paulbourke.net/geometry/lineline2d/
        float s = ((p4.x - p3.x) * (p1.y - p3.y) - (p4.y - p3.y) * (p1.x - p3.x))
                / ((p4.y - p3.y) * (p2.x - p1.x) - (p4.x - p3.x) * (p2.y - p1.y));
        return new Point(p1.x + s * (p2.x - p1.x), p1.y + s * (p2.y - p1.y));
    }


    private void addTriangle(float angle1, float angle2, Segment segment) {
        Point p1 = center;
        Point p2 = new Point(center.x + (float)Math.cos(angle1), center.y + (float)Math.sin(angle1));
        Point p3 = new Point(0, 0);
        Point p4 = new Point(0, 0);

        if (segment != null) {
            // Stop the triangle at the intersecting segment
            p3.x = segment.p1.x;
            p3.y = segment.p1.y;
            p4.x = segment.p2.x;
            p4.y = segment.p2.y;
        } else {
            // Stop the triangle at a fixed distance; this probably is
            // not what we want, but it never gets used in the demo
            p3.x = center.x + (float)Math.cos(angle1) * 500;
            p3.y = center.y + (float)Math.sin(angle1) * 500;
            p4.x = center.x + (float)Math.cos(angle2) * 500;
            p4.y = center.y + (float)Math.sin(angle2) * 500;
        }

        Point pBegin = lineIntersection(p3, p4, p1, p2);

        p2.x = center.x + (float)Math.cos(angle2);
        p2.y = center.y + (float)Math.sin(angle2);
        Point pEnd = lineIntersection(p3, p4, p1, p2);

        output.add(pBegin);
        output.add(pEnd);
    }

}
