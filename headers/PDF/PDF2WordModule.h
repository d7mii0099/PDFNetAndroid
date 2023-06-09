//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2023 by Apryse Software Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------
// !Warning! This file is autogenerated, modify the .codegen file, not this one
// (any changes here will be wiped out during the autogen process)

#ifndef PDFTRON_H_CPPPDFPDF2WordModule
#define PDFTRON_H_CPPPDFPDF2WordModule
#include <C/PDF/TRN_PDF2WordModule.h>

#include <Common/BasicTypes.h>
#include <Common/UString.h>
#include <PDF/PDFDoc.h>

namespace pdftron { namespace PDF { 
class PDFDoc;

/**
 * The class PDF2WordModule.
 * static interface to PDFTron SDKs PDF to Word functionality
 */
class PDF2WordModule
{
public:
	
	//methods:
	
	/**
	 * Find out whether the pdf2word module is available (and licensed).
	 * 
	 * @return returns true if pdf2word operations can be performed.
	 */
	static bool IsModuleAvailable();

};

#include <Impl/PDF2WordModule.inl>
} //end pdftron
} //end PDF


#endif //PDFTRON_H_CPPPDFPDF2WordModule
