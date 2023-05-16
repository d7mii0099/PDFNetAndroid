package com.pdftron.pdf.utils;

import android.graphics.PointF;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorSpace;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementBuilder;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.GState;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.PathData;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.StrokeOutlineBuilder;
import com.pdftron.pdf.annots.Ink;
import com.pdftron.pdf.tools.FreehandCreate;
import com.pdftron.sdf.Obj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Helper class used to create custom Pressure Sensitive appearances for Ink annotations. Custom
 * appearance uses pressure information to determine the thickness of the strokes.
 */
public class PressureInkUtils {

    private static String KEY_THICKNESS = "PDFTron_Pressure_Thickness";

    /**
     * Clears the ink list in the ink annotation and sets a new one
     *
     * @param ink     the ink annotation to set the ink list to
     * @param inkList the ink list containing a list of stroke points
     * @throws PDFNetException
     */
    public static void setInkList(@NonNull Ink ink, @NonNull List<List<PointF>> inkList) throws PDFNetException {
        Obj annotObj = ink.getSDFObj();
        annotObj.erase(AnnotUtils.KEY_INK_LIST);

        // Insert the ink list into the ink annot, rebuild bbox, and update style
        int pathIdx = 0;
        for (List<PointF> inkListItem : inkList) {
            Point p = new Point();
            int pointIdx = 0;
            for (PointF point : inkListItem) {
                p.x = point.x;
                p.y = point.y;
                ink.setPoint(pathIdx, pointIdx, p);
                pointIdx++;
            }
            pathIdx++;
        }
    }

    /**
     * Clears the thickness list that contains varying stroke thickness
     * information for each stroke in the given Ink annotation
     *
     * @param annot the annotation with thickness list that we want to clear
     * @throws PDFNetException
     */
    public static void clearThicknessList(Annot annot) throws PDFNetException {
        Obj annotObj = annot.getSDFObj();
        annotObj.erase(KEY_THICKNESS);
    }

    /**
     * Stores the given thickness list which contains varying stroke thickness
     * information for each stroke in the given Ink annotation
     *
     * @param annot           the ink annotation with thickness list that we want to clear
     * @param thicknessesList thickness list that we want to add to the annotation
     * @throws PDFNetException
     */
    public static void setThicknessList(Ink annot, List<List<Float>> thicknessesList) throws PDFNetException {
        // Create array to store outline
        Obj thicknessArray = annot.getSDFObj().findObj(KEY_THICKNESS);
        int arrayOffset = 0;
        if (thicknessArray == null) {
            thicknessArray = annot.getSDFObj().putArray(KEY_THICKNESS);
            arrayOffset = 0;
        } else {
            if (thicknessArray.isArray()) {
                arrayOffset = (int) thicknessArray.size();
            }
        }

        for (int i = 0; i < thicknessesList.size(); i++) {
            Obj thicknessObj = thicknessArray.insertArray(i + arrayOffset);
            List<Float> thicknesses = thicknessesList.get(i);
            for (float thickness : thicknesses) {
                thicknessObj.pushBackNumber(thickness);
            }
        }
    }

    /**
     * Stores the thickness list that contains varying stroke thickness
     * information for each stroke in the given Ink annotation
     * <p>
     * Must be wrapped in a read lock
     *
     * @param ink Ink Annotation that may contain a thickness list
     * @return the thickness list of each stroke from the Ink annotation, or null if none can be fine
     * @throws PDFNetException
     */
    @Nullable
    public static List<List<Float>> getThicknessList(@NonNull Ink ink) throws PDFNetException {
        Obj thicknessesObj = ink.getSDFObj().findObj(KEY_THICKNESS);
        if (thicknessesObj == null) {
            return null;
        }
        int numStrokes = (int) thicknessesObj.size();
        List<List<Float>> thicknessesList = new ArrayList<>(numStrokes);
        for (int i = 0; i < numStrokes; i++) {
            // Get the outline path
            Obj thicknessObj = thicknessesObj.getAt(i);
            long numCoords = thicknessObj.size();
            List<Float> outlinePath = new ArrayList<>();
            for (int k = 0; k < numCoords; k++) {
                outlinePath.add((float) thicknessObj.getAt(k).getNumber());
            }
            thicknessesList.add(outlinePath);
        }
        return thicknessesList;
    }

    /**
     * @param ink Ink Annotation that may contain a thickness list
     * @return the thickness list of each stroke in array form from Ink the annotation, or null if none can be fine
     * @throws PDFNetException
     */
    @Nullable
    public static List<double[]> getThicknessArrays(@NonNull Ink ink) throws PDFNetException {
        Obj thicknessesObj = ink.getSDFObj().findObj(KEY_THICKNESS);
        if (thicknessesObj == null) {
            return null;
        }
        int numStrokes = (int) thicknessesObj.size();
        List<double[]> thicknessesList = new ArrayList<>(numStrokes);
        for (int i = 0; i < numStrokes; i++) {
            // Get the outline path
            Obj thicknessObj = thicknessesObj.getAt(i);
            long numCoords = thicknessObj.size();
            double[] outlinePath = new double[(int) numCoords];
            for (int k = 0; k < numCoords; k++) {
                outlinePath[k] = thicknessObj.getAt(k).getNumber();
            }
            thicknessesList.add(outlinePath);
        }
        return thicknessesList;
    }

    /**
     * Returns the bounding box of the given ink strokes and the stoke thickness.
     *
     * @param strokes             the stroke points used to calculate the bounding box
     * @param thickness           the thickness of strokes
     * @param pageNumber          the page number containing these strokes, required if inScreenCoordinates is true
     * @param pdfViewCtrl         used to convert to page points if points are are screen coordinates, null if
     *                            inScreenCoordinates is false
     * @param inScreenCoordinates whether to convert points from screen to page coordinates
     * @return the bounding box of the ink strokes.
     * @hide
     */
    public static Rect getInkItemBBox(@NonNull List<List<PointF>> strokes,
            float thickness,
            int pageNumber,
            @Nullable PDFViewCtrl pdfViewCtrl,
            boolean inScreenCoordinates) {

        float min_x = Float.MAX_VALUE;
        float min_y = Float.MAX_VALUE;
        float max_x = Float.MIN_VALUE;
        float max_y = Float.MIN_VALUE;

        for (List<PointF> pageStroke : strokes) {
            for (PointF point : pageStroke) {
                min_x = Math.min(min_x, point.x);
                max_x = Math.max(max_x, point.x);
                min_y = Math.min(min_y, point.y);
                max_y = Math.max(max_y, point.y);
            }
        }

        try {
            if (min_x == Float.MAX_VALUE && min_y == Float.MAX_VALUE
                    && max_x == Float.MIN_VALUE && max_y == Float.MIN_VALUE) {
                // no stroke
                return new Rect(0.0, 0.0, 0.0, 0.0);
            } else {
                double[] min;
                double[] max;
                if (inScreenCoordinates) {
                    min = pdfViewCtrl.convScreenPtToPagePt(min_x, min_y, pageNumber);
                    max = pdfViewCtrl.convScreenPtToPagePt(max_x, max_y, pageNumber);
                } else {
                    min = new double[]{min_x, min_y};
                    max = new double[]{max_x, max_y};
                }
                Rect rect = new Rect(min[0], min[1], max[0], max[1]);
                rect.normalize();
                rect.inflate(thickness);
                return rect;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static class EraserData {
        public final boolean hasErased;
        public final List<List<PointF>> newStrokeList;
        public final List<List<Float>> newThicknessList;

        public EraserData(boolean hasErased, List<List<PointF>> newStrokeList, List<List<Float>> newThicknessList) {
            this.hasErased = hasErased;
            this.newStrokeList = newStrokeList;
            this.newThicknessList = newThicknessList;
        }
    }

    /**
     * Helper method to obtain an output stroke list and thickness after erasing strokes that
     * overlap with given eraser stroke points.
     * The input stroke and thickness list must have the same number of items.
     *
     * @param oldStrokeList    the original stroke list
     * @param oldThicknessList the origin thickness list
     * @param mPdfViewCtrl     the PDFViewCtrl that contains the annotation
     * @param eraserStrokes    points from an eraser used to erase a stroke
     * @param eraserHalfWidth  half width of the eraser
     * @param annotRect        the rect of the annotation that contains the stroke list
     * @return EraseData that contains new stroke and thickness lists
     * @throws PDFNetException
     * @hide
     */
    @NonNull
    public static EraserData erasePressureStrokesAndThickness(
            @NonNull List<List<PointF>> oldStrokeList,
            @Nullable List<List<Float>> oldThicknessList,
            @NonNull PDFViewCtrl mPdfViewCtrl,
            @NonNull List<List<PointF>> eraserStrokes,
            float eraserHalfWidth,
            @NonNull Rect annotRect) throws PDFNetException {

        PDFDoc tempDoc = new PDFDoc();
        try {
            int size = oldStrokeList.size();
            List<Obj> strokesArrays = createStrokeArrays(tempDoc, mPdfViewCtrl, oldStrokeList);

            List<List<PointF>> outputStrokeList = new ArrayList<>();
            List<List<Float>> outputThicknessList = new ArrayList<>();

            // for every eraser point, erase the points the necessary points
            // in the strokes array
            Point prevPt = null;
            Point currPt;
            boolean hasErasedAnything = false;
            for (int i = 0; i < size; i++) {
                boolean hasErasedStroke = false;
                List<PointF> tmpOldStrokeList = oldStrokeList.get(i);
                Obj strokeArr = strokesArrays.get(i);

                // Go through each eraser point and erase ink if possible
                for (List<PointF> eraserStroke : eraserStrokes) {
                    for (PointF eraserPoint : eraserStroke) {
                        if (prevPt != null) {
                            currPt = new Point(eraserPoint.x, eraserPoint.y);
                            boolean erasedPoints = Ink.erasePoints(strokeArr, annotRect, prevPt, currPt, eraserHalfWidth);
                            if (erasedPoints) {
                                hasErasedAnything = true;
                                hasErasedStroke = true;
                                break;
                            }
                            prevPt = currPt;
                        } else {
                            prevPt = new Point(eraserPoint.x, eraserPoint.y);
                        }
                    }
                }

                // We only add a stroke if we didn't erase it
                if (!hasErasedStroke) {
                    outputStrokeList.add(new ArrayList<>(tmpOldStrokeList));

                    if (oldThicknessList != null) {
                        outputThicknessList.add(oldThicknessList.get(i));
                    }
                }
            }
            return new EraserData(
                    hasErasedAnything,
                    outputStrokeList,
                    outputThicknessList
            );
        } finally {
            tempDoc.close();
        }
    }

    public static void eraseSubPath(@NonNull Ink ink, int index) throws PDFNetException {
        Obj inkList = ink.getSDFObj().findObj(AnnotUtils.KEY_INK_LIST);
        Obj thicknessArray = ink.getSDFObj().findObj(KEY_THICKNESS);
        if (inkList != null && inkList.isArray()) {
            inkList.eraseAt(index);
        }
        if (thicknessArray != null && thicknessArray.isArray()) {
            thicknessArray.eraseAt(index);
        }
    }

    /**
     * Helper method to obtain an output stroke list and thickness after applying eraser points.
     * The input stroke and thickness list must have the same number of items.
     *
     * @param oldStrokeList    the original stroke list
     * @param oldThicknessList the origin thickness list
     * @param mPdfViewCtrl     the PDFViewCtrl that contains the annotation
     * @param eraserStrokes    points from an eraser used to erase a stroke
     * @param eraserHalfWidth  half width of the eraser
     * @param annotRect        the rect of the annotation that contains the stroke list
     * @return EraseData that contains new stroke and thickness lists
     * @throws PDFNetException
     * @hide
     */
    @NonNull
    public static EraserData erasePointsAndThickness(
            @NonNull List<List<PointF>> oldStrokeList,
            @Nullable List<List<Float>> oldThicknessList,
            @NonNull PDFViewCtrl mPdfViewCtrl,
            @NonNull List<List<PointF>> eraserStrokes,
            float eraserHalfWidth,
            @NonNull Rect annotRect) throws PDFNetException {

        PDFDoc tempDoc = new PDFDoc();
        try {
            int size = oldStrokeList.size();
            List<Obj> strokesArrays = createStrokeArrays(tempDoc, mPdfViewCtrl, oldStrokeList);

            List<List<PointF>> outputStrokeList = new ArrayList<>();
            List<List<Float>> outputThicknessList = new ArrayList<>();

            // for every eraser point, erase the points the necessary points
            // in the strokes array
            Point prevPt = null;
            Point currPt;
            boolean hasErasedAnything = false;
            for (int i = 0; i < size; i++) {
                List<PointF> tmpOldStrokeList = oldStrokeList.get(i);
                Obj strokeArr = strokesArrays.get(i);

                // Go through each eraser point and erase ink if possible
                for (List<PointF> eraserStroke : eraserStrokes) {
                    for (PointF eraserPoint : eraserStroke) {
                        if (prevPt != null) {
                            currPt = new Point(eraserPoint.x, eraserPoint.y);
                            boolean erasedPoints = Ink.erasePoints(strokeArr, annotRect, prevPt, currPt, eraserHalfWidth);
                            if (erasedPoints) {
                                hasErasedAnything = true;
                            }
                            prevPt = currPt;
                        } else {
                            prevPt = new Point(eraserPoint.x, eraserPoint.y);
                        }
                    }
                }

                // Then push result to our output list
                if (hasErasedAnything) {
                    List<List<PointF>> strokesList = FreehandCreate.createStrokeListFromArrayObj(strokeArr);
                    outputStrokeList.addAll(strokesList);
                    if (oldThicknessList != null) {
                        List<List<Float>> newThickness = splitThicknessList(tmpOldStrokeList, oldThicknessList.get(i), strokesList);
                        outputThicknessList.addAll(newThickness);
                    }
                } else {
                    outputStrokeList.add(new ArrayList<>(tmpOldStrokeList));

                    if (oldThicknessList != null) {
                        outputThicknessList.add(oldThicknessList.get(i));
                    }
                }
            }
            return new EraserData(
                    hasErasedAnything,
                    outputStrokeList,
                    outputThicknessList
            );
        } finally {
            tempDoc.close();
        }
    }

    private static List<Obj> createStrokeArrays(
            @NonNull PDFDoc tempDoc,
            @NonNull PDFViewCtrl pdfviewCtrl,
            @NonNull List<List<PointF>> strokeList) throws PDFNetException {

        int size = strokeList.size();
        List<Obj> strokesArrays = new ArrayList<>();
        // for every page stroke
        for (List<PointF> pageStroke : strokeList) {
            Obj tempStrokesArray = tempDoc.createIndirectArray();
            Obj strokeArray = tempStrokesArray.pushBackArray();

            // for every point in the page stroke
            int pointIndex = 0;
            for (PointF point : pageStroke) {
                while (strokeArray.size() < (pointIndex + 1) * 2) {
                    strokeArray.pushBackNumber(0);
                    strokeArray.pushBackNumber(0);
                }

                strokeArray.getAt(pointIndex * 2).setNumber(point.x);
                strokeArray.getAt(pointIndex * 2 + 1).setNumber(point.y);
                pointIndex++;
            }
            strokesArrays.add(tempStrokesArray);
        }

        return strokesArrays;
    }

    private static List<List<Float>> splitThicknessList(
            List<PointF> oldStroke,
            List<Float> oldThickness,
            List<List<PointF>> newStrokes) {
        HashMap<PointF, Float> thicknessMap = new HashMap<>();
        for (int i = 0; i < oldStroke.size(); i++) {
            thicknessMap.put(oldStroke.get(i), oldThickness.get(i));
        }

        // Get all thicknesses available
        List<List<Float>> newThicknesses = new ArrayList<>();
        for (List<PointF> stroke : newStrokes) {
            List<Float> tempThickness = new ArrayList<>();
            for (PointF pt : stroke) {
                Float thickness = thicknessMap.get(pt);
                tempThickness.add(thickness != null ? thickness : -1);
            }
            newThicknesses.add(tempThickness);
        }

        // Fill in unknowns using neighbour points
        for (List<Float> thicknesses : newThicknesses) {
            if (thicknesses.size() == 2) {
                float first = thicknesses.get(0);
                float second = thicknesses.get(1);
                if (first == -1.0 && second == -1.0) {
                    thicknesses.set(0, 1.0f);
                    thicknesses.set(1, 1.0f);
                } else if (second == -1.0) {
                    thicknesses.set(1, thicknesses.get(0));
                } else {
                    thicknesses.set(0, thicknesses.get(1));
                }
            } else {
                for (int i = 0; i < thicknesses.size(); i++) {
                    if (thicknesses.get(i) == -1.0f) {
                        if (thicknesses.size() > 1) {
                            if (i == 0) {
                                thicknesses.set(i, thicknesses.get(i + 1));
                            } else if (i == (thicknesses.size() - 1)) {
                                thicknesses.set(i, thicknesses.get(i - 1));
                            } else {
                                throw new RuntimeException("This should never happen!");
                            }
                        } else {
                            thicknesses.set(i, 1.0f);
                        }
                    }
                }
            }
        }

        if (newStrokes.size() != newThicknesses.size()) {
            throw new RuntimeException("This should never happen!");
        }

        return newThicknesses;
    }

    /**
     * Checks whether the Ink annotation contains pressure information.
     *
     * @param ink the ink annotation to check
     * @return true if the ink annotation contains thickness information
     * @throws PDFNetException
     */
    public static boolean isPressureSensitive(@NonNull Annot ink) throws PDFNetException {
        Obj thicknessPathsObj = ink.getSDFObj().findObj(KEY_THICKNESS);
        return thicknessPathsObj != null;
    }

    // Returns null if could not generate outline due to broken invariant
    //
    // Invariant: Number of points must equals number of thickness entries,
    // otherwise we cannot compute the stroke outline
    @Nullable
    private static List<double[]> generateOutlinesFromArray(List<ArrayList<PointF>> pathList,
            List<double[]> thicknessesList, double baseThickness) {
        // Invariant: Number of points must equals number of thickness entries,
        // otherwise we cannot compute the stroke outline
        if (pathList.size() != thicknessesList.size()) {
            return null;
        }
        List<double[]> outlines = new ArrayList<>();

        for (int i = 0; i < pathList.size(); i++) {
            double[] thicknesses = thicknessesList.get(i);
            ArrayList<PointF> path = pathList.get(i);
            if (thicknesses.length != path.size()) {
                return null;
            }

            StrokeOutlineBuilder strokeOutlineBuilder = new StrokeOutlineBuilder(baseThickness);
            for (int k = 0; k < path.size(); k++) {
                strokeOutlineBuilder.addPoint(path.get(k).x, path.get(k).y, thicknesses[k]);
            }
            outlines.add(strokeOutlineBuilder.getOutline());
        }
        return outlines;
    }

    /**
     * Create a stroke outline given a stroke (i.e. list of points) and a list of thicknesses
     *
     * @param pathList        the stroke points to generate outline
     * @param thicknessesList the thickness information for each point
     * @param baseThickness   the base thickness to reference for annotation
     * @return the array containing the outline points with alternative x and y coordinates
     */
    @Nullable
    public static List<double[]> generateOutlines(List<List<PointF>> pathList,
            List<List<Float>> thicknessesList, float baseThickness) {
        // Invariant: Number of points must equals number of thickness entries,
        // otherwise we cannot compute the stroke outline
        if (pathList.size() != thicknessesList.size()) {
            return null;
        }
        List<double[]> outlines = new ArrayList<>();

        for (int i = 0; i < pathList.size(); i++) {
            List<Float> thicknesses = thicknessesList.get(i);
            List<PointF> path = pathList.get(i);
            if (thicknesses.size() != path.size()) {
                return null;
            }

            StrokeOutlineBuilder strokeOutlineBuilder = new StrokeOutlineBuilder(baseThickness);
            for (int k = 0; k < path.size(); k++) {
                strokeOutlineBuilder.addPoint(path.get(k).x, path.get(k).y, thicknesses.get(k));
            }
            outlines.add(strokeOutlineBuilder.getOutline());
        }
        return outlines;
    }

    private static Obj writeStrokeOutline(ElementBuilder eb,
            ElementWriter writer,
            List<double[]> outlines,
            Ink annot,
            boolean shouldInflate) throws PDFNetException {

        // Elements to the page
        Element element;
        GState gstate;
        for (double[] path : outlines) {

            if (path.length == 0) {
                continue;
            }

            // Create the operators
            int numOperators = (path.length / 2 - 1) / 3 + 1;
            byte[] operators = new byte[numOperators];
            operators[0] = PathData.e_moveto;
            for (int j = 1; j < numOperators; j++) {
                operators[j] = PathData.e_cubicto;
            }

            // Add them to element builder
            eb.createPath(path, operators);

            element = eb.pathEnd();            // the path is now finished
            element.setPathFill(true);        // the path should be filled
            element.setWindingFill(true);
            gstate = element.getGState();
//            gstate.setTransform(1.0f, 0, 0, 1.0d, 0, 0);
            gstate.setFillColorSpace(ColorSpace.createDeviceRGB());
            gstate.setFillColor(annot.getColorAsRGB());
            gstate.setFillOpacity(annot.getOpacity());
            writer.writePlacedElement(element);
        }

        Obj newAppearanceStream = writer.end();
        Rect bbox = annot.getRect();
        if (shouldInflate) {
            bbox.inflate(10); // todo bfung temporary workaround for now
        }
        newAppearanceStream.putRect("BBox",
                bbox.getX1(),
                bbox.getY1(),
                bbox.getX2(),
                bbox.getY2());
        annot.setRect(bbox);

        return newAppearanceStream;
    }

    /**
     * Adds a custom Ink appearance to an existing Annotation that uses Pressure sensitive
     * ink information stored in the Annot.
     * <p>
     * Must be called wrapped in a write lock
     *
     * @param annot Ink annotation with the appearance that we want to update
     * @return true if an appearance was successfully added and false otherwise
     */
    public static boolean refreshCustomInkAppearanceForExistingAnnot(@NonNull Annot annot) {
        ElementWriter writer = null;
        ElementBuilder eb = null;
        try {
            Ink ink = new Ink(annot);
            writer = new ElementWriter();
            writer.begin(annot.getAppearance());

            // Get thicknesses for each stroke
            List<double[]> thicknessesList = getThicknessArrays(ink);
            if (thicknessesList == null) {
                return false;
            }

            // Get the ink/path list from annot
            Obj inkList = annot.getSDFObj().findObj(AnnotUtils.KEY_INK_LIST);
            List<ArrayList<PointF>> pathList =
                    FreehandCreate.createPageStrokesFromArrayObj(
                            inkList
                    );

            // Compute stroke outlines from the ink/path list and thicknesses list
            double baseThickness = annot.getBorderStyle().getWidth();
            List<double[]> outlines = generateOutlinesFromArray(pathList, thicknessesList, baseThickness);
            if (outlines == null) {
                return false;
            }

            eb = new ElementBuilder();

            // set the appearance of sticky note icon to the custom icon
            ink.setAppearance(writeStrokeOutline(eb, writer, outlines, ink, false));
            return true;
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.destroy();
                } catch (Exception ignored) {

                }
            }
            if (eb != null) {
                try {
                    eb.destroy();
                } catch (Exception ignored) {

                }
            }
        }
        return false;
    }

    /**
     * Adds a custom Ink appearance to an new Annotation that uses Pressure sensitive
     * ink information stored in the Annot.
     * <p>
     * Must be called wrapped in a write lock
     *
     * @param annot Ink annotation with the appearance that we want to update
     * @return true if an appearance was successfully added and false otherwise
     */
    public static boolean refreshCustomAppearanceForNewAnnot(@NonNull PDFViewCtrl pdfViewCtrl,
            @NonNull Annot annot) {
        ElementWriter writer = null;
        ElementBuilder eb = null;
        try {
            Ink ink = new Ink(annot);
            writer = new ElementWriter();
            writer.begin(pdfViewCtrl.getDoc());

            List<double[]> allThicknessesList = getThicknessArrays(ink);
            if (allThicknessesList == null) {
                return false;
            }

            Obj inkList = annot.getSDFObj().findObj(AnnotUtils.KEY_INK_LIST);
            List<ArrayList<PointF>> pathList =
                    FreehandCreate.createPageStrokesFromArrayObj(
                            inkList
                    );

            // Compute stroke outlines from the path list and thicknesses list
            double baseThickness = annot.getBorderStyle().getWidth();
            List<double[]> outlines = generateOutlinesFromArray(pathList, allThicknessesList, baseThickness);
            if (outlines == null) {
                return false;
            }

            eb = new ElementBuilder();

            // set the appearance with stroke outline
            ink.setAppearance(writeStrokeOutline(eb, writer, outlines, ink, true));
            return true;
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.destroy();
                } catch (Exception ignored) {

                }
            }
            if (eb != null) {
                try {
                    eb.destroy();
                } catch (Exception ignored) {

                }
            }
        }
        return false;
    }
}
