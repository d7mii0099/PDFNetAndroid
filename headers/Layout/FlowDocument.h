#ifndef PDFTRON_H_CPPFlowDocument
#define PDFTRON_H_CPPFlowDocument
#include <C/Layout/TRN_FlowDocument.h>

#include <Common/BasicTypes.h>
#include <Common/UString.h>
#include <PDF/PDFDoc.h>

#include "Paragraph.h"


namespace pdftron {
	namespace Layout {

		class FlowDocument
		{
			void Destroy();
		public:
			FlowDocument();
			~FlowDocument();

			// is this needed?
			Paragraph AddParagraph();
			Paragraph AddParagraph(const UString& text);

			void SetDefaultMargins(double left, double top, double right, double bottom);
			void SetDefaultPageSize(double width, double height);

			PDF::PDFDoc PaginateToPDF();

#ifndef SWIGHIDDEN
			TRN_FlowDocument m_impl;
#endif
		};


#include <Impl/FlowDocument.inl>
} //end pdftron
} //end PDF

#endif // PDFTRON_H_CPPFlowDocument
