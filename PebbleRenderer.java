package jackiequiltpatterndeterminaiton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Pebble
 */
public class PebbleRenderer extends PatternRenderer {
    private List<SvgPathCommand> renderedCommands = new ArrayList<>(),
            decoCommands;
    private SvgFileProcessor decoElemFile = null;
    private TreeNode<Point> treeRoot;

    public PebbleRenderer(TreeNode<Point> treeRoot, List<SvgPathCommand> decoCommands, SvgFileProcessor decoElemFile) {
        super(treeRoot);
        this.treeRoot = treeRoot;
        this.decoCommands = decoCommands;
        this.decoElemFile = decoElemFile;
    }

    public List<SvgPathCommand> getRenderedCommands() {
        return renderedCommands;
    }

    @Override
    public void pebbleFilling() {
        double dist = Point.getDistance(treeRoot.getData(), treeRoot.getChildren().get(0).getData());
        for (TreeNode<Point> firstChildren : treeRoot.getChildren()) {
            if (Double.compare((Point.getDistance(treeRoot.getData(), firstChildren.getData())), dist) < 0)
                dist = Point.getDistance(treeRoot.getData(), firstChildren.getData());
        }
        dist = dist * 0.66;
        System.out.println("Command distance is" + dist);

        /* Order children tobe counterclockwise*/
        TreeTraversal.treeOrdering(treeRoot, null);

        //landFillTraverse(treeRoot, null, dist);
        renderedCommands.add(new SvgPathCommand(new Point(treeRoot.getData().x + dist, treeRoot.getData().y), SvgPathCommand.CommandType.MOVE_TO));
        List<CircleBound> determinedBounds = new ArrayList<>();
        treeRoot.setBoundingCircle(new CircleBound(dist, treeRoot.getData()));

        // first determination loop, make sure each pebble touches one pebble
        pebbleRenderDetermineRadii(treeRoot, dist, determinedBounds);
//        pebbleRenderDraw(treeRoot, 0);
//        SvgFileProcessor.outputSvgCommands(renderedCommands, "beforeAdjustion");
//        renderedCommands.clear();
//        renderedCommands.add(new SvgPathCommand(new Point(treeRoot.getData().x + dist, treeRoot.getData().y), SvgPathCommand.CommandType.MOVE_TO));

        // second determination loop, make sure each pebble touches two pebbles
        pebbleAdjustTreenode(treeRoot, dist, determinedBounds);
//        pebbleRenderDraw(treeRoot, 0);
//        SvgFileProcessor.outputSvgCommands(renderedCommands, "afterfirstadjust");
//        renderedCommands.clear();
//        renderedCommands.add(new SvgPathCommand(new Point(treeRoot.getData().x + dist, treeRoot.getData().y), SvgPathCommand.CommandType.MOVE_TO));

        //
//        System.out.println("First iteration of 3 touch");
        pebbleSecondAdjustTreenode(treeRoot, dist, determinedBounds);
//        System.out.println("Second iteration of 3 touch");

        pebbleSecondAdjustTreenode(treeRoot, dist, determinedBounds);

        pebbleRenderDraw(treeRoot, 0);
    }

    public void rectanglePacking() {
        double dist = Point.getDistance(treeRoot.getData(), treeRoot.getChildren().get(0).getData());
        for (TreeNode<Point> firstChildren : treeRoot.getChildren()) {
            if (Double.compare((Point.getDistance(treeRoot.getData(), firstChildren.getData())), dist) < 0)
                dist = Point.getDistance(treeRoot.getData(), firstChildren.getData());
        }
        System.out.println("Command distance is" + dist);

        /* Order children tobe counterclockwise*/
        TreeTraversal.treeOrdering(treeRoot, null);

        //landFillTraverse(treeRoot, null, dist);
//        renderedCommands.add(new SvgPathCommand(new Point(treeRoot.getData().x + dist, treeRoot.getData().y), SvgPathCommand.CommandType.MOVE_TO));
        List<RectangleBound> determinedBounds = new ArrayList<>();
        treeRoot.setBoundingRectangle(new RectangleBound(treeRoot.getData(), dist * 0.66, dist * 0.66));

        // first determination loop, make sure each pebble touches one pebble
        rectangleRenderDetermineBound(treeRoot, dist, determinedBounds);
        rectangleRenderDraw(treeRoot, 0);
    }


    /* We push inflate treenodes that touch two pebbles in the direciton of bisectors
*/
    public void pebbleSecondAdjustTreenode(TreeNode<Point> thisNode, double dist, final List<CircleBound> determinedBounds) {
        Point thisCenter = thisNode.getData();
        double r = thisNode.getBoundingCircle().getRadii();
        Point pebble1 = null, pebble2 = null, bestPoint = null;
        double r1 = 0, r2 = 0, best = r;
        int touchCount = 0;

        for (CircleBound bound : determinedBounds)
            if (bound.getCenter() != thisCenter) {
                if (bound.touches(thisNode.getBoundingCircle())) {
                    touchCount++;
                    if (touchCount == 1) {
                        r1 = bound.getRadii();
                        pebble1 = bound.getCenter();
                    } else if (touchCount == 2) {
                        r2 = bound.getRadii();
                        pebble2 = bound.getCenter();
                    } else
                        break;
                }
            }

        if (touchCount == 2) {
            double angle1 = Point.getAngle(thisCenter, pebble1);
            double angle2 = Point.getAngle(thisCenter, pebble2);
            double angleBTW = angle1 - angle2;
            double rotationAngle = -1 * angleBTW / 2;
            if (angleBTW < 0)
                rotationAngle += Math.PI;
            int ITERATION = 100;
            double shiftLen;
            for (int dir = 0; dir < 2; dir++) {
                if (dir == 1)
                    rotationAngle = 2 * Math.PI - rotationAngle;//second iteration, opposite direction of bisector
                //first iteration, one direction
                for (int i = 0; i < ITERATION; i++) {
                    shiftLen = dist / ITERATION * i;
                    Point newPointAngle = new Point(pebble1.x, pebble1.y).rotateAroundCenter(thisCenter, rotationAngle);
                    Point newPoint = Point.intermediatePointWithLen(thisCenter, newPointAngle, shiftLen);
                    double newRadii1 = Point.getDistance(newPoint, pebble1) - r1;
                    double newRadii2 = Point.getDistance(newPoint, pebble2) - r2;
                    double newRadii = Math.min(newRadii1, newRadii2);

                    boolean valid = (newRadii > 0) && (Math.abs(newRadii1 - newRadii2) < 0.05);
                    if (valid) {
                        for (CircleBound b : determinedBounds) {
                            Point pCenter = b.getCenter();
                            if ((pCenter != thisCenter) && (pCenter != pebble1) && (pCenter != pebble2)) {
                                //TODO: change call to circle bound
                                if (b.touches(new CircleBound(newRadii, newPoint))) {
                                    valid = false;
                                    break;
                                }
                            }
                        }
                    }

                    if (valid) {
                        if (newRadii > best) {
                            best = newRadii;
                            bestPoint = newPoint;
                        }
                    } else {
                        break;
                    }

                }
            }

            if (bestPoint != null) {
                determinedBounds.remove(thisNode.getBoundingCircle());
                thisNode.setData(bestPoint);
                thisNode.setBoundingCircle(new CircleBound(best, thisNode.getData()));
                determinedBounds.add(thisNode.getBoundingCircle());
            }
        }

        for (TreeNode<Point> child : thisNode.getChildren()) {
            pebbleSecondAdjustTreenode(child, dist, determinedBounds);
        }
    }

    /* Since all internal nodes touch at least its parent and one of its children, we only adjust positions of treenodes. We
    push each treenode towards the direction of parent-self line until it touches a treeenode.
    */
    public void pebbleAdjustTreenode(TreeNode<Point> thisNode, double dist, final List<CircleBound> determinedBounds) {
        if (thisNode.getChildren().size() == 0) {
            // leafnode, adjust position
            double shiftLen;
            int ITERATION = 100;
            double thisParentDist = Point.getDistance(thisNode.getParent().getData(), thisNode.getData());
            double thisRadii = thisNode.getBoundingCircle().getRadii();
            double tempRadii;
            Point bestCenter = thisNode.getData();
            double bestRadii = thisRadii;
            boolean isValid;
            for (int i = 0; i < ITERATION; i++) {
                isValid = true;
                shiftLen = dist / 100 * i;
                Point newCenter = Point.intermediatePointWithLen(thisNode.getParent().getData(), thisNode.getData(), thisParentDist + shiftLen);
                tempRadii = thisRadii + shiftLen;
                for (CircleBound b : determinedBounds) {
                    boolean isSelfTest = thisNode.getData() == b.getCenter();
                    boolean isParentTest = thisNode.getParent().getData() == b.getCenter();
                    if ((!isSelfTest) || (!isParentTest) || Double.compare(Point.getDistance(b.getCenter(), newCenter) - (b.getRadii() + tempRadii), -0.05) < 0) {
                        isValid = false;
                        break;
                    }
                }

                if (isValid) {
                    if (tempRadii > bestRadii) {
                        bestRadii = tempRadii;
                        bestCenter = newCenter;
                    }

                }
            }
            determinedBounds.remove(thisNode.getBoundingCircle());
            thisNode.setData(bestCenter);
            thisNode.setBoundingCircle(new CircleBound(bestRadii, thisNode.getData()));
            determinedBounds.add(thisNode.getBoundingCircle());
        }
        for (TreeNode<Point> child : thisNode.getChildren()) {
            pebbleAdjustTreenode(child, dist, determinedBounds);
        }
    }

    public void pebbleRenderDraw(TreeNode<Point> thisNode, int angle) {
        boolean DEBUG = false;
        HashMap<Integer, TreeNode<Point>> degreeTreeNodeMap = new HashMap<>();
        boolean[] degreeTable = new boolean[360];
        Arrays.fill(degreeTable, false);
        /* Record the direction of the children*/
        System.out.println(decoCommands.size());


        for (TreeNode<Point> child : thisNode.getChildren()) {
            int thisAngle = (int) Math.toDegrees(Point.getAngle(thisNode.getData(), child.getData()));
            degreeTable[thisAngle] = true;
            degreeTreeNodeMap.put(thisAngle, child);
        }

        int gap = 1;
        Point zeroAnglePoint = new Point(thisNode.getData().x + thisNode.getBoundingCircle().getRadii(), thisNode.getData().y);
        Point startDrawingPoint = zeroAnglePoint.rotateAroundCenter(thisNode.getData(), Math.toRadians(angle));
        Point primitiveCentroid = thisNode.getData();
        renderedCommands.add(new SvgPathCommand(startDrawingPoint, SvgPathCommand.CommandType.LINE_TO));
        if (DEBUG) {
            SvgFileProcessor.outputSvgCommands(renderedCommands, "test");
        }
        // Insert a kissing primitive instead of a pebble
        List<SvgPathCommand> scaledCommands = new ArrayList<>();

        if (decoCommands.size() != 0) {
            PatternRenderer.insertPatternToList(SvgPathCommand.commandsScaling(decoCommands,
                    thisNode.getBoundingCircle().getRadii() * 2 /
                            decoElemFile.getMaximumExtentFromStartPoint(),
                    decoCommands.get(0).getDestinationPoint()),
                    scaledCommands, startDrawingPoint, Math.toRadians(angle));
            renderedCommands.addAll(scaledCommands);
//            primitiveCentroid = SvgFileProcessor.getCentroidOnList(scaledCommands);
            printMapping(scaledCommands, primitiveCentroid);
        }


        /** Pebbles **/

        for (int offset = 0; offset < 360; offset += gap) {
            int currentAngle = (angle + offset) % 360;
            TreeNode<Point> child;
            Point cutPoint;
            if (scaledCommands.size() != 0) {
//                cutPoint = pointOnPrimitiveWithDegreeToCenter(scaledCommands, primitiveCentroid, currentAngle); // render primitive
                cutPoint = zeroAnglePoint.rotateAroundCenter(thisNode.getData(), Math.toRadians(currentAngle));
            } else {
                cutPoint = zeroAnglePoint.rotateAroundCenter(thisNode.getData(), Math.toRadians(currentAngle));
            }
            renderedCommands.add(new SvgPathCommand(cutPoint, SvgPathCommand.CommandType.LINE_TO)); // render bubble
            if (DEBUG) {
                SvgFileProcessor.outputSvgCommands(renderedCommands, "test");
            }
            for (int j = currentAngle; j < currentAngle + gap; j++) {
                int searchAngle = j % 360;
                int newAngle = (searchAngle + 180) % 360;
                if (scaledCommands.size() != 0) {
//                    cutPoint = pointOnPrimitiveWithDegreeToCenter(scaledCommands, primitiveCentroid, searchAngle); // render primitive
                    cutPoint = zeroAnglePoint.rotateAroundCenter(thisNode.getData(), Math.toRadians(searchAngle));
                } else {
                    cutPoint = zeroAnglePoint.rotateAroundCenter(thisNode.getData(), Math.toRadians(searchAngle));
                }
                renderedCommands.add(new SvgPathCommand(cutPoint, SvgPathCommand.CommandType.LINE_TO)); // add a command to the branching point
                if (DEBUG) {
                    SvgFileProcessor.outputSvgCommands(renderedCommands, "test");
                }
                if ((child = degreeTreeNodeMap.get(searchAngle)) != null) {
                    pebbleRenderDraw(child, newAngle);
                }
            }
        }
        Point thisPoint;
        if (scaledCommands.size() != 0) {
//            thisPoint = pointOnPrimitiveWithDegreeToCenter(scaledCommands, primitiveCentroid, angle); // render primitive
            thisPoint = zeroAnglePoint.rotateAroundCenter(thisNode.getData(), Math.toRadians(angle));
        } else {
            thisPoint = zeroAnglePoint.rotateAroundCenter(thisNode.getData(), Math.toRadians(angle));
        }
        renderedCommands.add(new SvgPathCommand(thisPoint, SvgPathCommand.CommandType.LINE_TO));
        if (DEBUG) {
            SvgFileProcessor.outputSvgCommands(renderedCommands, "test");
        }

    }

    public void rectangleRenderDraw(TreeNode<Point> thisNode, double angle) {
        renderedCommands.add(new SvgPathCommand(pointOnRectangleWithDegreeToCenter(thisNode.getBoundingRectangle(), angle), SvgPathCommand.CommandType.LINE_TO));
        HashMap<Integer, TreeNode<Point>> degreeTreeNodeMap = new HashMap<>();
        boolean[] degreeTable = new boolean[360];
        Arrays.fill(degreeTable, false);
        /* Record the direction of the children*/
        for (TreeNode<Point> child : thisNode.getChildren()) {
            int thisAngle = (int) Math.toDegrees(Point.getAngle(thisNode.getData(), child.getData()));
            degreeTable[thisAngle] = true;
            degreeTreeNodeMap.put(thisAngle, child);
        }

        // first octdran
        Point zeroAnglePoint = new Point(thisNode.getData().x + thisNode.getBoundingRectangle().getHalfWidth(), thisNode.getData().y);
        double firstQuadrantAngle = Math.atan2(thisNode.getBoundingRectangle().getHalfHeight(), thisNode.getBoundingRectangle().getHalfWidth());
        TreeNode<Point> child;
        Point cutPoint;
        int gap = 15;
        for (int offset = 0; offset < 360; offset += gap) {
            int currentAngle = ((int) angle + offset) % 360;
            cutPoint = pointOnRectangleWithDegreeToCenter(thisNode.getBoundingRectangle(), currentAngle);
            renderedCommands.add(new SvgPathCommand(cutPoint, SvgPathCommand.CommandType.LINE_TO));
//            SvgFileProcessor.outputSvgCommands(renderedCommands, "this");

            for (int j = currentAngle; j < currentAngle + gap; j++) {
                int searchAngle = j % 360;
                if ((child = degreeTreeNodeMap.get(searchAngle)) != null) {
                    int newAngle = (searchAngle + 180) % 360;
                    cutPoint = pointOnRectangleWithDegreeToCenter(thisNode.getBoundingRectangle(), searchAngle);
                    renderedCommands.add(new SvgPathCommand(cutPoint, SvgPathCommand.CommandType.LINE_TO));
//                    SvgFileProcessor.outputSvgCommands(renderedCommands, "this");
                    rectangleRenderDraw(child, newAngle);
                    renderedCommands.add(new SvgPathCommand(cutPoint, SvgPathCommand.CommandType.LINE_TO));
                }
            }
            renderedCommands.add(new SvgPathCommand(cutPoint, SvgPathCommand.CommandType.LINE_TO));
//            SvgFileProcessor.outputSvgCommands(renderedCommands, "this");

        }
        renderedCommands.add(new SvgPathCommand(pointOnRectangleWithDegreeToCenter(thisNode.getBoundingRectangle(), angle), SvgPathCommand.CommandType.LINE_TO));
//        SvgFileProcessor.outputSvgCommands(renderedCommands, "this");

    }

    private Point pointOnRectangleWithDegreeToCenter(RectangleBound bound, double degree) {
        if (!(degree >= 0 && degree < 360)) {
            assert (false);

        }
        double radian = Math.toRadians(degree);
        double firstQuadrantAngleInRadian = Math.atan(bound.getHalfHeight() / bound.getHalfWidth());
        assert (Math.toDegrees(firstQuadrantAngleInRadian) <= 90);
        if (degree < Math.toDegrees(firstQuadrantAngleInRadian))
            return new Point(bound.getCenter().x + bound.getHalfWidth(), bound.getCenter().y + Math.tan(radian) * bound.getHalfWidth());
        if (degree >= Math.toDegrees(firstQuadrantAngleInRadian) && degree <= 90) {
            return new Point(bound.getCenter().x + Math.tan(Math.PI * 0.5 - firstQuadrantAngleInRadian) * bound.getHalfHeight(), bound.getCenter().y + bound.getHalfHeight());
        }

        if (degree > 90 && degree <= 180) {
            double complementary = 90 - (degree - 90);
            Point flipX = pointOnRectangleWithDegreeToCenter(bound, complementary);
            return new Point(bound.getCenter().x - (flipX.x - bound.getCenter().x), flipX.y);
        }


        double supplementary = 360 - degree;
        assert (supplementary < 180);
        Point opposite = pointOnRectangleWithDegreeToCenter(bound, supplementary);
        double yOffset = opposite.y - bound.getCenter().y;
//        assert (yOffset > 0);
        return new Point(opposite.x, bound.getCenter().y - yOffset);
    }

    private void printMapping(List<SvgPathCommand> decoElment, Point center) {
        for (int i = 0; i < decoElment.size(); i++) {
            Point lastCommandPoint = decoElment.get(((i - 1) + decoElment.size()) % decoElment.size()).getDestinationPoint(),
                    thisCommandPoint = decoElment.get(i).getDestinationPoint();
            double commandToThisPointAngle = (Math.toDegrees(Point.getAngle(center, thisCommandPoint))) % 360;
            double commandToLastPointAngle = (Math.toDegrees(Point.getAngle(center, lastCommandPoint))) % 360;
//            System.out.printf("command %d has angle: %s\n", i - 1, commandToLastPointAngle);
//            System.out.printf("command %d has angle: %s\n\n", i, commandToThisPointAngle);
        }
    }

    private Point pointOnPrimitiveWithDegreeToCenter(List<SvgPathCommand> decoElment, Point center, int degree) {
        if (!(degree >= 0 && degree < 360)) {
            assert (false);
        }

        int decreasingCounter = 0;
        // sample to see which direction do commands go
        for (int i = 0; i < decoElment.size(); i++) {
            Point firstCommandPoint = decoElment.get(i % decoElment.size()).getDestinationPoint(),
                    secondCommandPoint = decoElment.get((i + 1) % decoElment.size()).getDestinationPoint();
            double firstAngle = (Math.toDegrees(Point.getAngle(center, firstCommandPoint))) % 360;
            double secondAngle = (Math.toDegrees(Point.getAngle(center, secondCommandPoint))) % 360;
            if (firstAngle > secondAngle)
                decreasingCounter++;
            else if (firstAngle < 5 && secondAngle > 355)
                decreasingCounter++;
        }
        boolean decreasingOrder = decreasingCounter > (decoElment.size() / 2);

        for (int i = 1; i < decoElment.size(); i++) {
            Point lastCommandPoint = decoElment.get(((i - 1) + decoElment.size()) % decoElment.size()).getDestinationPoint(),
                    thisCommandPoint = decoElment.get(i).getDestinationPoint();
            double commandToThisPointAngle = (Math.toDegrees(Point.getAngle(center, thisCommandPoint))) % 360;
            double commandToLastPointAngle = (Math.toDegrees(Point.getAngle(center, lastCommandPoint))) % 360;

            boolean isBetween = false;
            if (decreasingOrder) {
                if (degree <= commandToLastPointAngle && degree >= commandToThisPointAngle)
                    isBetween = true;
                else if ((Math.abs(commandToLastPointAngle - commandToThisPointAngle) > 200) && commandToThisPointAngle > commandToLastPointAngle) {
                    if ((degree <= commandToLastPointAngle && degree >= 0)
                            || (degree >= commandToThisPointAngle && degree <= 360))
                        isBetween = true;
                }

            } else {
                if (degree <= commandToThisPointAngle && degree >= commandToLastPointAngle)
                    isBetween = true;
                else if ((Math.abs(commandToLastPointAngle - commandToThisPointAngle) > 200) && commandToThisPointAngle < commandToLastPointAngle) {
                    if ((degree <= commandToThisPointAngle && degree >= 0)
                            || (degree >= commandToLastPointAngle && degree <= 360))
                        isBetween = true;
                }
            }
            if (isBetween) {
                double alphaInRadian = Math.toRadians(degree - commandToLastPointAngle),
                        thetaInRadian = Point.getAngle(lastCommandPoint, thisCommandPoint)
                                - Point.getAngle(lastCommandPoint, center),
                        betaInRadian = Math.PI - alphaInRadian - thetaInRadian;
                double movedLength = alphaInRadian / betaInRadian * Point.getDistance(lastCommandPoint, center);
                Point cutPoint = Point.intermediatePointWithLen(lastCommandPoint, thisCommandPoint, movedLength);
//                System.out.printf("degree: %d commandSelected:%d\n", degree, i - 1);
                if (Math.abs(commandToLastPointAngle - degree) < Math.abs(commandToThisPointAngle - degree))
                    return lastCommandPoint;
                else
                    return thisCommandPoint;
            }

        }
        return decoElment.get(0).getDestinationPoint();

    }


    public void pebbleRenderDetermineRadii(TreeNode<Point> thisNode, double dist, final List<CircleBound> determinedBounds) {
        determinedBounds.add(thisNode.getBoundingCircle());
        //determine bounding circle for each child
        for (TreeNode<Point> child : thisNode.getChildren()) {
            double adjustedRadii = Point.getDistance(thisNode.getData(), child.getData()) - dist;
            boolean shortLineSegment = false;

            //loop through pebbles that have drawn already to adjust radii
            final TreeNode<Point> thisChild = child;
            ArrayList<CircleBound> sortedBoundByDist = new ArrayList<>(determinedBounds);
            sortedBoundByDist.sort((b1, b2) -> (
                    new Double(Point.getDistance(thisChild.getData(), b1.getCenter()) - b1.getRadii()).compareTo(
                            Point.getDistance(thisChild.getData(), b2.getCenter()) - b2.getRadii())
            ));

            int adjustmentSize = (sortedBoundByDist.size() > 5) ? 5 : sortedBoundByDist.size();
            for (int i = 0; i < adjustmentSize; i++) {
                double distanceBetween = Point.getDistance(child.getData(), sortedBoundByDist.get(i).getCenter()) - sortedBoundByDist.get(i).getRadii();

                if ((distanceBetween > 0) && Double.compare(distanceBetween, adjustedRadii) < 0 && (Double.compare(adjustedRadii - distanceBetween, 0.01) > 0)) {
                    adjustedRadii = distanceBetween;
                    assert adjustedRadii > 0;
                    shortLineSegment = true;

                }
            }

            if (shortLineSegment) {
                // Strategy 1: insert a new pebble at all short line segments
                double distBtwChildParent = Point.getDistance(thisNode.getData(), child.getData());
                assert (distBtwChildParent - dist - adjustedRadii) > 0;
                double newRadii = (distBtwChildParent - dist - adjustedRadii) / 2.0;
                Point middlePoint = Point.intermediatePointWithLen(thisNode.getData(), child.getData(), newRadii + dist);
                if (!Point.onLine(thisNode.getData(), child.getData(), middlePoint)) {
                    assert newRadii + dist < distBtwChildParent;
                }
                TreeNode<Point> midTreeNode = new TreeNode<>(middlePoint, new ArrayList<>());
                midTreeNode.setParent(thisNode);
                midTreeNode.addChild(child);
                thisNode.removeChild(child);
                thisNode.addChild(midTreeNode);
                midTreeNode.setBoundingCircle(new CircleBound(newRadii, midTreeNode.getData()));
                pebbleRenderDetermineRadii(midTreeNode, newRadii, determinedBounds);
            } else {
                child.setBoundingCircle(new CircleBound(adjustedRadii, child.getData()));
                pebbleRenderDetermineRadii(child, adjustedRadii, determinedBounds);
            }
        }

    }

    public void rectangleRenderDetermineBound(TreeNode<Point> thisNode, double dist, final List<RectangleBound> determinedBounds) {
        determinedBounds.add(thisNode.getBoundingRectangle());
        RectangleBound parentBound = thisNode.getBoundingRectangle();
//        System.out.printf("childSize:%d\n", thisNode.getChildren().size());
        for (TreeNode<Point> child : thisNode.getChildren()) {
            RectangleBound childBound = new RectangleBound(child.getData(), dist, dist);
            thisNode.getBoundingRectangle().modifyToTightestBound(childBound);
            boolean shortLineSegment = false;

            //loop through rectangles that have drawn already to adjust radii
            for (RectangleBound b : determinedBounds) {
                b.modifyToTightestBound(childBound);
            }

            double childParentXDist = Math.abs(parentBound.getCenter().x - childBound.getCenter().x),
                    childParentYDist = Math.abs(parentBound.getCenter().y - childBound.getCenter().y),
                    halfWidthSum = Math.abs(parentBound.getHalfWidth() + childBound.getHalfWidth()),
                    halfHeightSum = Math.abs(parentBound.getHalfHeight() + childBound.getHalfHeight());
            double gapX = childParentXDist - halfWidthSum, gapY = childParentYDist - halfHeightSum;
            gapX = (gapX < 0) ? 0 : gapX;
            gapY = (gapY < 0) ? 0 : gapY;

            boolean xDetached = Double.compare(gapX, 0.01) > 0,
                    yDetached = Double.compare(gapY, 0.01) > 0;
            if (xDetached || yDetached) {
                shortLineSegment = true;
            }

            if (shortLineSegment) {
                //insert a new rectangle
                double newWidth = childParentXDist - halfWidthSum - 0.0001,
                        newHeight = childParentYDist - halfHeightSum - 0.0001;

                double newX = Double.min(parentBound.getCenter().x + parentBound.getHalfWidth(),
                        childBound.getCenter().x + childBound.getHalfWidth()) + newWidth * 0.5;
                double newY = Double.min(parentBound.getCenter().y + parentBound.getHalfHeight(),
                        childBound.getCenter().y + childBound.getHalfHeight()) + newHeight * 0.5;
                if (yDetached) {
                    newX = (childBound.getCenter().x + parentBound.getCenter().x) * 0.5;
                    newWidth = Double.min(parentBound.getWidth(), childBound.getWidth()) * 0.5;
                } else {
//                    System.out.println("x detached");
                    newY = (childBound.getCenter().y + parentBound.getCenter().y) * 0.5;
                    newHeight = Double.min(parentBound.getHeight(), childBound.getHeight()) * 0.5;
//                    System.out.printf("newWidth:%.2f newHeight:%.2f\n newX:%.2f newY:%.2f\n\n",newWidth, newHeight, newX, newY);

                }

                Point newCenter = new Point(newX, newY);
                TreeNode<Point> midTreeNode = new TreeNode<>(newCenter, new ArrayList<>());
                midTreeNode.setBoundingRectangle(new RectangleBound(newCenter, newWidth, newHeight));
                determinedBounds.add(midTreeNode.getBoundingRectangle());

                midTreeNode.setParent(thisNode);
                midTreeNode.addChild(child);
                thisNode.removeChild(child);
                thisNode.addChild(midTreeNode);

                child.setBoundingRectangle(childBound);
                rectangleRenderDetermineBound(child, dist, determinedBounds);
            } else {
                child.setBoundingRectangle(childBound);
                rectangleRenderDetermineBound(child, dist, determinedBounds);
            }

        }

    }

}