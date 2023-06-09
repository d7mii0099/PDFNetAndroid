//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2023 by Apryse Software Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------
// !Warning! This file is autogenerated, modify the .codegen file, not this one
// (any changes here will be wiped out during the autogen process)

#ifndef PDFTRON_H_CPPPDFAdvancedImagingModule
#define PDFTRON_H_CPPPDFAdvancedImagingModule
#include <C/PDF/TRN_AdvancedImagingModule.h>

#include <Common/BasicTypes.h>
#include <Common/UString.h>
#include <PDF/PDFDoc.h>

namespace pdftron { namespace PDF { 
class PDFDoc;

/**
 * The class AdvancedImagingModule.
 * static interface to PDFTron SDKs AdvancedImaging functionality
 */
class AdvancedImagingModule
{
public:
	
	//methods:
	
	/**
	 * Find out whether the AdvancedImaging module is available (and licensed).
	 * 
	 * @return returns true if AdvancedImaging operations can be performed.
	 */
	static bool IsModuleAvailable();

};

#include <Impl/AdvancedImagingModule.inl>
} //end pdftron
} //end PDF


#endif //PDFTRON_H_CPPPDFAdvancedImagingModule
