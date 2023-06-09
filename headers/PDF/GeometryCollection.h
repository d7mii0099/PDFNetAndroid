//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2023 by Apryse Software Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------
// !Warning! This file is autogenerated, modify the .codegen file, not this one
// (any changes here will be wiped out during the autogen process)

#ifndef PDFTRON_H_CPPPDFGeometryCollection
#define PDFTRON_H_CPPPDFGeometryCollection
#include <C/PDF/TRN_GeometryCollection.h>
#include <PDF/Point.h>

#include <Common/BasicTypes.h>
#include <Common/UString.h>
#include <PDF/PDFDoc.h>

namespace pdftron { namespace PDF { 


/**
 * The class GeometryCollection.
 * A Preprocessed PDF geometry collection
 */
class GeometryCollection
{
public:
	//enums:
	enum SnappingMode {
		eDefaultSnapMode = 14,
		ePointOnLine = 1,
		eLineMidpoint = 2,
		eLineIntersection = 4,
		ePathEndpoint = 8
	};
	GeometryCollection();
	GeometryCollection(const GeometryCollection& other);
	GeometryCollection(TRN_GeometryCollection impl);
	GeometryCollection& operator= (const GeometryCollection& other);
	~GeometryCollection();
	
	void Destroy();

	//methods:
	
	/**
	 * return the point within the collection which is closest to the queried point. All values are in the page coordinate space.
	 * 
	 * @param x -- the x coordinate to snap, in page coordinates.
	 * @param y -- the y coordinate to snap, in page coordinates.
	 * @param mode -- a combination of flags from the SnappingMode enumeration.
	 * @return a point within the collection, closest to the queried point. If the collection is empty, the queried point will be returned unchanged.
	 */
	Point SnapToNearest(double x, double y, UInt32 mode) const;
	
	/**
	 * return the point within the collection which is closest to the queried point. All values are in the page coordinate space.
	 * 
	 * @param x -- the x coordinate to snap.
	 * @param y -- the y coordinate to snap.
	 * @param dpi -- the resolution of the rendered page, in pixels per inch.
	 * @param mode -- a combination of flags from the SnappingMode enumeration.
	 * @return a point within the collection, closest to the queried point. If the collection is empty, the queried point will be returned unchanged.
	 */
	Point SnapToNearestPixel(double x, double y, double dpi, UInt32 mode) const;

	//for xamarin use only
	static GeometryCollection* CreateInternal(ptrdiff_t impl);
	ptrdiff_t GetHandleInternal();


#ifndef SWIGHIDDEN
	TRN_GeometryCollection m_impl;
#endif

private:

#ifndef SWIGHIDDEN
	mutable bool m_owner; 
#endif
};

#include <Impl/GeometryCollection.inl>
} //end pdftron
} //end PDF


#endif //PDFTRON_H_CPPPDFGeometryCollection
