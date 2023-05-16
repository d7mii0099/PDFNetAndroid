//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2023 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------
// !Warning! This file is autogenerated, modify the .codegen file, not this one
// (any changes here will be wiped out during the autogen process)

#ifndef PDFTRON_H_CParagraph
#define PDFTRON_H_CParagraph


#ifdef __cplusplus
extern "C" {
#endif

#include <C/Common/TRN_Types.h>
#include <C/Common/TRN_Exception.h>
	
enum TRN_ParagraphStyleTextJustification
{
	e_ParagraphStyle_text_justification_invalid = 0,
	e_ParagraphStyle_text_justify_left,
	e_ParagraphStyle_text_justify_right,
	e_ParagraphStyle_text_justify_center
};

struct TRN_Paragraph_tag;
typedef struct TRN_Paragraph_tag* TRN_Paragraph;

TRN_API TRN_ParagraphAddText(TRN_Paragraph self, const TRN_UString text);



TRN_API TRN_ParagraphSetFontFace(TRN_Paragraph self, const TRN_UString font_name);
TRN_API TRN_ParagraphGetFontFace(TRN_Paragraph self, TRN_UString* result);

TRN_API TRN_ParagraphSetFontSize(TRN_Paragraph self, double font_size);
TRN_API TRN_ParagraphGetFontSize(TRN_Paragraph self, double* result);

TRN_API TRN_ParagraphSetItalic(TRN_Paragraph self, TRN_Bool val);
TRN_API TRN_ParagraphIsItalic(TRN_Paragraph self, TRN_Bool* result);

TRN_API TRN_ParagraphSetBold(TRN_Paragraph self, TRN_Bool val);
TRN_API TRN_ParagraphIsBold(TRN_Paragraph self, TRN_Bool* result);
	
TRN_API TRN_ParagraphSetTextColor(TRN_Paragraph self, TRN_UInt8 red,  TRN_UInt8 green, TRN_UInt8 blue);



TRN_API TRN_ParagraphSetSpaceBefore(TRN_Paragraph self, double val);
TRN_API TRN_ParagraphGetSpaceBefore(TRN_Paragraph self, double* result);

TRN_API TRN_ParagraphSetSpaceAfter(TRN_Paragraph self, double val);
TRN_API TRN_ParagraphGetSpaceAfter(TRN_Paragraph self, double* result);

TRN_API TRN_ParagraphSetJustificationMode(TRN_Paragraph self, enum TRN_ParagraphStyleTextJustification val);
TRN_API TRN_ParagraphGetJustificationMode(TRN_Paragraph self, enum TRN_ParagraphStyleTextJustification* result);

	
#ifdef __cplusplus
} // extern C
#endif

#endif /* PDFTRON_H_CParagraph */