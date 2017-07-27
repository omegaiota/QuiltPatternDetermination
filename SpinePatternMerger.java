package jackiesvgprocessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JacquelineLi on 6/14/17.
 */
public class SpinePatternMerger {
    private List<SvgPathCommand> spineCommands, patternCommands, combinedCommands = new ArrayList<>();
    private String spineName, patternName;
    private svgFileProcessor spineFileProcessed = null, patternFileProcessed = null;
    private boolean rotationOn = true;

    public SpinePatternMerger(svgFileProcessor spineFile, svgFileProcessor patternFile, boolean rotation) {
        this.spineCommands = spineFile.getCommandLists().get(0);
        this.patternCommands = patternFile.getCommandLists().get(0);
        this.spineName = spineFile.getfFileName();
        this.patternName = patternFile.getfFileName();
        this.spineFileProcessed = spineFile;
        this.patternFileProcessed = patternFile;
        this.rotationOn = rotation;
    }
    public SpinePatternMerger(String spineName, List<SvgPathCommand> spineCommands, String patternName, List<SvgPathCommand> patternCommands, boolean rotation) {
        this.spineCommands = spineCommands;
        this.patternCommands = patternCommands;
        this.spineName = spineName;
        this.patternName = patternName;
        this.rotationOn = rotation;
    }

    public SpinePatternMerger(String spineName, List<SvgPathCommand> spineCommands, svgFileProcessor patternFile, boolean rotation) {
        this.spineCommands = spineCommands;
        this.patternCommands = patternFile.getCommandLists().get(0);
        this.spineName = spineName;
        this.patternName = patternFile.getfFileName();
        this.patternFileProcessed = patternFile;
        this.rotationOn = rotation;
    }

    public void combinePattern() {
        System.out.println("# of spine commands: " + spineCommands.size() + "# of patternCommands: " + patternCommands.size());
        combinedCommands.add(spineCommands.get(0));
        Point prevInsertPoint = spineCommands.get(0).getDestinationPoint();
        Point insertPoint;
        double gapWidth = (patternFileProcessed == null) ? (40) : (patternFileProcessed.getWidth());
        double remainingWidth = 0;
        for (int i = 1; i < spineCommands.size(); i++) {
            double totalLength = Point.getDistance(spineCommands.get(i - 1).getDestinationPoint(),
                    spineCommands.get(i).getDestinationPoint());
            System.out.println("\nCommand:" + i + " totalLength:" + totalLength);
            /* first handle remaining width*/
            if (spineCommands.get(i).getCommandType() != SvgPathCommand.CommandType.MOVE_TO) {
            /*if current line is long enough to put the insert point*/
                if (Double.compare(totalLength, remainingWidth) > 0) {
                    //offset to handle corners
                    if (i >= 2) {
                        double angleThisLine = Point.getAngle(spineCommands.get(i - 1).getDestinationPoint(), spineCommands.get(i).getDestinationPoint());
                        double anglePrevLine = Point.getAngle(i <= 1 ? new Point(0, 0) : spineCommands.get(i - 2).getDestinationPoint(), spineCommands.get(i - 1).getDestinationPoint());
                        double betweenAngles = (angleThisLine - anglePrevLine);
                        betweenAngles += betweenAngles > 0 ? 0 : (Math.PI * 2);
                        System.out.println("prevLine angle:" + Math.toDegrees(anglePrevLine));
                        System.out.println("thisLine angle:" + Math.toDegrees(angleThisLine));
                        System.out.println("angle between lines:" + (Math.toDegrees(betweenAngles)));
                        double adjustmentThreshhold = Math.PI / 6;
                        if ((betweenAngles > adjustmentThreshhold) && (betweenAngles < (Math.PI * 2 - adjustmentThreshhold))) {
                            System.out.println("line angle meets adjustment threshhold. Adjusting corners...");
                            if (betweenAngles < Math.PI) {
                                // concave outward, need to add patterns
                                System.out.println("----Adding a pattern");
                                double angleOffset = (((remainingWidth - gapWidth / 2) / gapWidth));
                                insertPatternToList(patternCommands, combinedCommands, spineCommands.get(i - 1).getDestinationPoint(),
                                        anglePrevLine + (betweenAngles / 2)
                                                + angleOffset);
                                System.out.println("added pattern rotation angle =" + "  " + Math.toDegrees(angleOffset));
                                //old rotation angle: anglePrevLine + (Math.PI - betweenAngles + ((remainingWidth - gapWidth / 2) / gapWidth) * 2) / 2)

                            } else if (Double.compare(remainingWidth / gapWidth, 0.1) > 0) {
                                //concave inward, needs adjusting spacing
                                System.out.println("----Adding padding space");
                                betweenAngles = 2 * Math.PI - betweenAngles;
                                System.out.println("----padding:" + ((Math.toDegrees(betweenAngles)) * patternFileProcessed.getPatternHeight() / 100 + " height:" + patternFileProcessed.getPatternHeight()));
                                remainingWidth += ((Math.toDegrees(betweenAngles)) * patternFileProcessed.getPatternHeight() / 100); // this might not be on the line now
                                if (Double.compare(totalLength, remainingWidth) < 0) {
                                    System.out.println("not on the line after adjustion, skip this line");
                                    combinedCommands.add(spineCommands.get(i));
                                    remainingWidth -= totalLength;
                                    continue;
                                }

                            }
                        }
                    }
                    System.out.println("remaining width is:" + remainingWidth);
                    insertPoint = Point.intermediatePointWithLen(spineCommands.get(i - 1).getDestinationPoint(),
                            spineCommands.get(i).getDestinationPoint(), remainingWidth);
                    System.out.println("prev command dest:" + spineCommands.get(i - 1).getDestinationPoint());
                    System.out.println("this command dest:" + spineCommands.get(i).getDestinationPoint());
                    System.out.println("Next insert point is: " + insertPoint.toString());
                /* insert a lineTo command to this potential point*/
                    SvgPathCommand handleRemainingCommand = new SvgPathCommand(insertPoint, SvgPathCommand.CommandType.LINE_TO);
                    combinedCommands.add(handleRemainingCommand);
                /* insert a pattern on this potential point*/
                    double rotationAngle = Point.getAngle(spineCommands.get(i - 1).getDestinationPoint(), spineCommands.get(i).getDestinationPoint());
                    insertPatternToList(patternCommands, combinedCommands, insertPoint, rotationAngle);

                /* break this spine command to handle remaining length with each gapWid apart*/
                    totalLength -= remainingWidth;
//                System.out.println("totalLength after handle remain:" + totalLength);
                    prevInsertPoint = insertPoint;

                    while (Double.compare(totalLength, gapWidth) > 0) {
                        insertPoint = Point.intermediatePointWithLen(prevInsertPoint, spineCommands.get(i).getDestinationPoint(), gapWidth);
                    /* insert a lineTo command to this potential point*/
                        SvgPathCommand lineToGap = new SvgPathCommand(insertPoint, SvgPathCommand.CommandType.LINE_TO);
                        combinedCommands.add(lineToGap);
                    /* insert a pattern on this potential point*/
                        insertPatternToList(patternCommands, combinedCommands, insertPoint, rotationAngle); //rotation angle should remain on the same line
                    /* break this spine command to handle remaining length with each gapWid apart*/
                        totalLength -= gapWidth;
//                    System.out.println("totalLength:" + totalLength);
                        prevInsertPoint = insertPoint;
                    }

                /* reset remaining width for next spine point*/
                    remainingWidth = gapWidth - totalLength;
                    combinedCommands.add(spineCommands.get(i));
                } else {
                /* this is a super short line that can't even handle remaining width, skip this line */
                    assert(Double.compare(totalLength, remainingWidth) <= 0);
                    System.out.println("total:" + totalLength + " remaining:" + remainingWidth);
                    combinedCommands.add(spineCommands.get(i));
                    remainingWidth -= totalLength;
                    assert (remainingWidth >= 0);
                }

            } else {
                combinedCommands.add(spineCommands.get(i));
            }

        }
        svgFileProcessor.outputSvgCommands(combinedCommands, "spine-" + spineName + "-pat-" + patternName + (rotationOn ? "-on" : "-off"));
    }

    public void insertPatternToList(List<SvgPathCommand> patternCommands,
                                    List<SvgPathCommand> combinedCommands,
                                    Point insertionPoint, double rotationAngle) {
        Point patternPoint = patternCommands.get(0).getDestinationPoint();
        SvgPathCommand newCommand;
        for (int j = 0; j < patternCommands.size(); j++) {
            if (rotationOn) /** with rotation*/
                newCommand = new SvgPathCommand(patternCommands.get(j), patternPoint, insertionPoint, rotationAngle);
            else /** without rotation*/
                newCommand = new SvgPathCommand(patternCommands.get(j), patternPoint, insertionPoint);
            combinedCommands.add(newCommand);
        }

    }

}