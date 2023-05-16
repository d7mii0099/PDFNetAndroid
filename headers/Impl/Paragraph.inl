inline Paragraph::Paragraph() {}

inline Paragraph::~Paragraph()
{
	Destroy();
}

inline void Paragraph::Destroy() {}

inline void Paragraph::AddText(const UString& text)
{
	REX(TRN_ParagraphAddText(m_impl, text.mp_impl));
}

inline void Paragraph::SetTextColor(UInt8 red, UInt8 green, UInt8 blue)
{
	REX(TRN_ParagraphSetTextColor(m_impl, red, green, blue));
}

inline void Paragraph::SetItalic(bool val)
{
	REX(TRN_ParagraphSetItalic(m_impl, BToTB(val)));
}

inline bool Paragraph::IsItalic()
{
	RetBool(TRN_ParagraphIsItalic(m_impl, &result));
}

inline void Paragraph::SetBold(bool val)
{
	REX(TRN_ParagraphSetBold(m_impl, BToTB(val)));
}

inline bool Paragraph::IsBold()
{
	RetBool(TRN_ParagraphIsBold(m_impl, &result));
}


inline void Paragraph::SetFontFace(const UString& font_name)
{
	REX(TRN_ParagraphSetFontFace(m_impl, font_name.mp_impl));
}

inline UString Paragraph::GetFontFace()
{
	RetStr(TRN_ParagraphGetFontFace(m_impl, &result));	
}

inline void Paragraph::SetFontSize(double font_size)
{
	REX(TRN_ParagraphSetFontSize(m_impl, font_size));
}

inline double Paragraph::GetFontSize()
{
	RetDbl(TRN_ParagraphGetFontSize(m_impl, &result));
}



inline void Paragraph::SetSpaceBefore(double val)
{
	REX(TRN_ParagraphSetSpaceBefore(m_impl, val));
}

inline double Paragraph::GetSpaceBefore()
{
	RetDbl(TRN_ParagraphGetSpaceBefore(m_impl, &result));
}

inline void Paragraph::SetSpaceAfter(double val)
{
	REX(TRN_ParagraphSetSpaceAfter(m_impl, val));
}

inline double Paragraph::GetSpaceAfter()
{
	RetDbl(TRN_ParagraphGetSpaceAfter(m_impl, &result));
}

inline void Paragraph::SetJustificationMode(ParagraphStyle::TextJustification val)
{
	REX(TRN_ParagraphSetJustificationMode(m_impl, static_cast<TRN_ParagraphStyleTextJustification>(val)));
}

inline ParagraphStyle::TextJustification Paragraph::GetJustificationMode()
{
	TRN_ParagraphStyleTextJustification result;
	REX(TRN_ParagraphGetJustificationMode(m_impl, &result));
	return (ParagraphStyle::TextJustification)result;
}