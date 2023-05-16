inline FlowDocument::~FlowDocument()
{
	Destroy();
}

inline FlowDocument::FlowDocument()
{
	TRN_FlowDocumentCreate((TRN_FlowDocument*)&m_impl);
}

inline void FlowDocument::Destroy()
{
	DREX(m_impl, TRN_FlowDocumentDestroy(m_impl));
}

inline PDF::PDFDoc FlowDocument::PaginateToPDF()
{
	PDF::PDFDoc ret;
	REX(TRN_FlowDocumentPaginateToPDF((TRN_FlowDocument)m_impl, (TRN_PDFDoc *)&ret.mp_doc));
	return ret;
}

inline Paragraph FlowDocument::AddParagraph()
{
	Paragraph ret;
	TRN_FlowDocumentAddParagraph((TRN_FlowDocument)m_impl, (TRN_Paragraph*)&ret.m_impl);
	return ret;
}

inline Paragraph FlowDocument::AddParagraph(const UString& text)
{
	Paragraph ret;
	TRN_FlowDocumentAddParagraphWithText((TRN_FlowDocument)m_impl, 
		text.mp_impl, (TRN_Paragraph*)&ret.m_impl);
	return ret;
}

inline void FlowDocument::SetDefaultPageSize(double width_points, double height_points)
{
	TRN_FlowDocumentSetDefaultPageSize((TRN_FlowDocument)m_impl, width_points, height_points);
}

inline void FlowDocument::SetDefaultMargins(double left_points, double top_points, double right_points, double bottom_points)
{
	TRN_FlowDocumentSetDefaultMargins((TRN_FlowDocument)m_impl, left_points, top_points, right_points, bottom_points);
}
