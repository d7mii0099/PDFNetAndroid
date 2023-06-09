//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2023 by Apryse Software Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------
// !Warning! This file is autogenerated, modify the .codegen file, not this one
// (any changes here will be wiped out during the autogen process)

#ifndef PDFTRON_H_CPPPDFPDFDocGenerator
#define PDFTRON_H_CPPPDFPDFDocGenerator
#include <C/PDF/TRN_PDFDocGenerator.h>
#include <Common/BasicTypes.h>
#include <Common/UString.h>
#include <PDF/PDFDoc.h>

namespace pdftron { namespace PDF { 
class PDFDoc;

/**
 * The class PDFDocGenerator.
 * A collection of static methods to create blank documents
 *
 * No notes :(
 */
class PDFDocGenerator
{
public:
	
	//methods:
	
	/**
	 * Create a new document with one page of blank paper.
	 * 
	 * @param width -- The page width in inches.
	 * @param height -- The page height in inches.
	 * @param background_red -- The red component of the background color.
	 * @param background_green -- The green component of the background color.
	 * @param background_blue -- The blue component of the background color.
	 * @return .
	 */
	static PDFDoc GenerateBlankPaperDoc(double width, double height, double background_red, double background_green, double background_blue);
	
	/**
	 * Create a new document with one page of grid paper.
	 * 
	 * @param width -- The page width in inches.
	 * @param height -- The page height in inches.
	 * @param grid_spacing -- The grid spacing in inches.
	 * @param line_thickness -- The line thickness in points.
	 * @param red -- The red component of the line color.
	 * @param green -- The green component of the line color.
	 * @param blue -- The blue component of the line color.
	 * @param background_red -- The red component of the background color.
	 * @param background_green -- The green component of the background color.
	 * @param background_blue -- The blue component of the background color.
	 * @return .
	 */
	static PDFDoc GenerateGridPaperDoc(double width, double height, double grid_spacing, double line_thickness, double red, double green, double blue, double background_red, double background_green, double background_blue);
	
	/**
	 * Create a new document with one page of lined paper.
	 * 
	 * @param width -- The page width in inches.
	 * @param height -- The page height in inches.
	 * @param line_spacing -- The line spacing in inches.
	 * @param line_thickness -- The line thickness in points.
	 * @param red -- The red component of the line color.
	 * @param green -- The green component of the line color.
	 * @param blue -- The blue component of the line color.
	 * @param left_margin_distance -- Distance of the margin from the left side of the page.
	 * @param left_margin_red -- The red component of the left margin color.
	 * @param left_margin_green -- The green component of the left margin color.
	 * @param left_margin_blue -- The blue component of the left margin color.
	 * @param right_margin_red -- The red component of the right margin color.
	 * @param right_margin_green -- The green component of the right margin color.
	 * @param right_margin_blue -- The blue component of the right margin color.
	 * @param background_red -- The red component of the background color.
	 * @param background_green -- The green component of the background color.
	 * @param background_blue -- The blue component of the background color.
	 * @param top_margin_distance -- Distance of the margin from the top of the page.
	 * @param bottom_margin_distance -- Distance of the margin from the bottom of the page.
	 * @return .
	 */
	static PDFDoc GenerateLinedPaperDoc(double width, double height, double line_spacing, double line_thickness, double red, double green, double blue, double left_margin_distance, double left_margin_red, double left_margin_green, double left_margin_blue, double right_margin_red, double right_margin_green, double right_margin_blue, double background_red, double background_green, double background_blue, double top_margin_distance, double bottom_margin_distance);
	
	/**
	 * Create a new document with one page of graph paper.
	 * 
	 * @param width -- The page width in inches.
	 * @param height -- The page height in inches.
	 * @param grid_spacing -- The grid spacing in inches.
	 * @param line_thickness -- The line thickness in points.
	 * @param weighted_line_thickness -- The weighted line thickness in points.
	 * @param weighted_line_freq -- Ratio of weighted lines to normal lines.
	 * @param red -- The red component of the line color.
	 * @param green -- The green component of the line color.
	 * @param blue -- The blue component of the line color.
	 * @param background_red -- The red component of the background color.
	 * @param background_green -- The green component of the background color.
	 * @param background_blue -- The blue component of the background color.
	 * @return .
	 */
	static PDFDoc GenerateGraphPaperDoc(double width, double height, double grid_spacing, double line_thickness, double weighted_line_thickness, int weighted_line_freq, double red, double green, double blue, double background_red, double background_green, double background_blue);
	
	/**
	 * Create a new document with one page of music paper.
	 * 
	 * @param width -- The page width in inches.
	 * @param height -- The page height in inches.
	 * @param margin -- The page margin in inches.
	 * @param staves -- Amount of staves on the page..
	 * @param linespace_size_pts -- The space between lines in points.
	 * @param line_thickness -- The line thickness in points.
	 * @param red -- The red component of the line color.
	 * @param green -- The green component of the line color.
	 * @param blue -- The blue component of the line color.
	 * @param background_red -- The red component of the background color.
	 * @param background_green -- The green component of the background color.
	 * @param background_blue -- The blue component of the background color.
	 * @return .
	 */
	static PDFDoc GenerateMusicPaperDoc(double width, double height, double margin, int staves, double linespace_size_pts, double line_thickness, double red, double green, double blue, double background_red, double background_green, double background_blue);
	
	/**
	 * Create a new document with one page of dotted paper.
	 * 
	 * @param width -- The page width in inches.
	 * @param height -- The page height in inches.
	 * @param dot_spacing -- The dot spacing in inches.
	 * @param dot_size -- The dot size (diameter) in points.
	 * @param red -- The red component of the line color.
	 * @param green -- The green component of the line color.
	 * @param blue -- The blue component of the line color.
	 * @param background_red -- The red component of the background color.
	 * @param background_green -- The green component of the background color.
	 * @param background_blue -- The blue component of the background color.
	 * @return .
	 */
	static PDFDoc GenerateDottedPaperDoc(double width, double height, double dot_spacing, double dot_size, double red, double green, double blue, double background_red, double background_green, double background_blue);
	
	/**
	 * Create a new document with one page of dotted paper.
	 * 
	 * @param width -- The page width in inches.
	 * @param height -- The page height in inches.
	 * @param dot_spacing -- The dot spacing in inches.
	 * @param dot_size -- The dot size (diameter) in points.
	 * @param red -- The red component of the line color.
	 * @param green -- The green component of the line color.
	 * @param blue -- The blue component of the line color.
	 * @param background_red -- The red component of the background color.
	 * @param background_green -- The green component of the background color.
	 * @param background_blue -- The blue component of the background color.
	 * @return .
	 */
	static PDFDoc GenerateIsometricDottedPaperDoc(double width, double height, double dot_spacing, double dot_size, double red, double green, double blue, double background_red, double background_green, double background_blue);
};

#include <Impl/PDFDocGenerator.inl>
} //end pdftron
} //end PDF


#endif //PDFTRON_H_CPPPDFPDFDocGenerator
