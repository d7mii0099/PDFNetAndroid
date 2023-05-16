#ifndef PDFTRON_H_CPPParagraph
#define PDFTRON_H_CPPParagraph
#include <C/Layout/TRN_Paragraph.h>

#include <Common/BasicTypes.h>
#include <Common/UString.h>
#include <PDF/PDFDoc.h>


namespace pdftron { namespace Layout {

class ParagraphStyle
{
public:
	enum TextJustification
	{
		e_text_justification_invalid = 0,
		e_text_justify_left = 1,
		e_text_justify_right = 2,
		e_text_justify_center = 3
	};
};


class Paragraph
{
	friend class FlowDocument;

	Paragraph();
public:
	~Paragraph();
	void Destroy();

	void AddText(const UString& text);

	/* Text Properties */

	/** 
	 * Set the font name to be used for the style
	 * @param font_name the font name
	 */
	void SetFontFace(const UString& font_name);

	/** 
	 * @return the font name used for the style
	 */
	UString GetFontFace();

	/** 
	 * Set font size used for the style
	 * @param font_size the font size
	 */
	void SetFontSize(double font_size);
	
	/** 
	 * @return The font size used for the style
	 */
	double GetFontSize();

	/** 
	 * Set the style set as 'italic'
	 * @param val the new value for 'italic'
	 */
	void SetItalic(bool val);

	/** 
	 * @return true if the style set as 'italic'
	 */
	bool IsItalic();
	
	/** 
	 * Set the style set as 'Bold'
	 * @param val the new value for 'Bold'
	 */
	void SetBold(bool val);

	/** 
	 * @return true if the style set as 'Bold'
	 */
	bool IsBold();

	/** 
	 * Set text color for the style
	 * @param red the red value of the color
	 * @param green the green value of the color
	 * @param blue the blue value of the color
	 */
	void SetTextColor(UInt8 red, UInt8 green, UInt8 blue);

	/* Paragraph Properties */

	/** 
	 * Set the "space before" value for paragraph style
	 * @param val the new value for 'space before'
	 */
	void SetSpaceBefore(double val);

	/** 
	 * @return "space before" value for paragraph style
	 */
	double GetSpaceBefore();

	/** 
	 * Set the "space after" value for paragraph style
	 * @param val the new value for 'space after'
	 */
	void SetSpaceAfter(double val);

	/** 
	 * @return "space after" value for paragraph style
	 */
	double GetSpaceAfter();

	/** 
	 * Set Justification mode for paragraph style
	 * @param val
	 */
	void SetJustificationMode(ParagraphStyle::TextJustification val);

	/** 
	 * @return Justification mode for paragraph style
	 */
	ParagraphStyle::TextJustification GetJustificationMode();


#ifndef SWIGHIDDEN
	TRN_Paragraph m_impl;
#endif
};


#include <Impl/Paragraph.inl>
} //end pdftron
} //end PDF

#endif // PDFTRON_H_CPPParagraph
