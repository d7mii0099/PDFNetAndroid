// This file is autogenerated: please see the codegen template "Options"
#ifndef PDFTRON_H_CPPPDFTextDiffOptions
#define PDFTRON_H_CPPPDFTextDiffOptions

#include <PDF/OptionsBase.h>
#include <PDF/ColorSpace.h>

namespace pdftron{ namespace PDF{ 

class TextDiffOptions
{
public:
	TextDiffOptions();
	~TextDiffOptions();

	
	/**
	* Gets the value ColorA from the options object
	* The difference color for deletions
	* @return a ColorPt, the current value for ColorA in the form of R, G, B
	*/
	ColorPt GetColorA();

	/**
	* Sets the value for ColorA in the options object
	* The difference color for deletions
	* @param color: the new value for ColorA in the form of R, G, B
	* @return this object, for call chaining
	*/
	TextDiffOptions& SetColorA(ColorPt color);

	/**
	* Gets the value OpacityA from the options object
	* The difference opacity for deletions
	* @return a double, the current value for OpacityA in between 0.0 (transparent) and 1.0 (opaque)
	*/
	double GetOpacityA();

	/**
	* Sets the value for OpacityA in the options object
	* The difference opacity for deletions
	* @param opacity: the new value for OpacityA in between 0.0 (transparent) and 1.0 (opaque)
	* @return this object, for call chaining
	*/
	TextDiffOptions& SetOpacityA(double opacity);


	/**
	* Gets the value ColorB from the options object
	* The difference color for insertions
	* @return a ColorPt, the current value for ColorB in the form of R, G, B
	*/
	ColorPt GetColorB();

	/**
	* Sets the value for ColorB in the options object
	* The difference color for insertions
	* @param color: the new value for ColorB in the form of R, G, B
	* @return this object, for call chaining
	*/
	TextDiffOptions& SetColorB(ColorPt color);

	/**
	* Gets the value OpacityB from the options object
	* The difference opacity for insertions
	* @return a double, the current value for OpacityB in between 0.0 (transparent) and 1.0 (opaque)
	*/
	double GetOpacityB();

	/**
	* Sets the value for OpacityB in the options object
	* The difference opacity for insertions
	* @param opacity: the new value for OpacityB in between 0.0 (transparent) and 1.0 (opaque)
	* @return this object, for call chaining
	*/
	TextDiffOptions& SetOpacityB(double opacity);


	/**
	* Gets the value ExtraMoveColor from the options object
	* The difference color for extra moves
	* @return a ColorPt, the current value for ExtraMoveColor in the form of R, G, B
	*/
	ColorPt GetExtraMoveColor();

	/**
	* Sets the value for ExtraMoveColor in the options object
	* The difference color for extra moves
	* @param color: the new value for ExtraMoveColor in the form of R, G, B
	* @return this object, for call chaining
	*/
	TextDiffOptions& SetExtraMoveColor(ColorPt color);

	/**
	* Gets the value ExtraMoveOpacity from the options object
	* The difference opacity for extra moves
	* @return a double, the current value for ExtraMoveOpacity in between 0.0 (transparent) and 1.0 (opaque)
	*/
	double GetExtraMoveOpacity();

	/**
	* Sets the value for ExtraMoveOpacity in the options object
	* The difference opacity for extra moves
	* @param opacity: the new value for ExtraMoveOpacity in between 0.0 (transparent) and 1.0 (opaque)
	* @return this object, for call chaining
	*/
	TextDiffOptions& SetExtraMoveOpacity(double opacity);


	/**
	* Gets the value CompareUsingZOrder from the options object
	* Whether to use z-order (aka paint order) when comparing text between A and B. On by default.
	* @return a bool, the current value for CompareUsingZOrder.
	*/
	bool GetCompareUsingZOrder();

	/**
	* Sets the value for CompareUsingZOrder in the options object
	* Whether to use z-order (aka paint order) when comparing text between A and B. On by default.
	* @param value: the new value for CompareUsingZOrder
	* @return this object, for call chaining
	*/
	TextDiffOptions& SetCompareUsingZOrder(bool value);


	/**
	* Gets the value ExtraMoveHighlight from the options object
	* Whether to highlight text in between short-distance moves when comparing text between A and B. Off by default.
	* @return a bool, the current value for ExtraMoveHighlight.
	*/
	bool GetExtraMoveHighlight();

	/**
	* Sets the value for ExtraMoveHighlight in the options object
	* Whether to highlight text in between short-distance moves when comparing text between A and B. Off by default.
	* @param value: the new value for ExtraMoveHighlight
	* @return this object, for call chaining
	*/
	TextDiffOptions& SetExtraMoveHighlight(bool value);


	/**
	* Gets the value ShowPlaceholders from the options object
	* Whether to show placeholder annotations. On by default.
	* Placeholders are insertion locations in document A and deletion locations in document B.
	* For example, if a word is removed from document B, we can highlight the location of the original word in document A,
	* but there is no word to highlight in B (it's removed). In this case a small "placeholder" annotation is placed in B
	* to identify the location of the removal.
	* @return a bool, the current value for ShowPlaceholders.
	*/
	bool GetShowPlaceholders();

	/**
	* Sets the value for ShowPlaceholders in the options object
	* Whether to show placeholder annotations. On by default.
	* Placeholders are insertion locations in document A and deletion locations in document B.
	* For example, if a word is removed from document B, we can highlight the location of the original word in document A,
	* but there is no word to highlight in B (it's removed). In this case a small "placeholder" annotation is placed in B
	* to identify the location of the removal.
	* @param value: the new value for ShowPlaceholders
	* @return this object, for call chaining
	*/
	TextDiffOptions& SetShowPlaceholders(bool value);


	/**
	* Adds a collection of ignorable regions for the given page,
	* an optional list of page areas not to be included in analysis
	* @param regions: the zones to be added to the ignore list
	* @param page_num: the page number the added regions belong to
	* @return this object, for call chaining
	*/
	TextDiffOptions& AddIgnoreZonesForPage(const RectCollection& regions, int page_num);

	
	// @cond PRIVATE_DOC
	#ifndef SWIGHIDDEN
	const SDF::Obj& GetInternalObj() const;
	SDF::Obj& GetInternalObj();

private:
	
	SDF::ObjSet m_obj_set;
	SDF::Obj m_dict;
	#endif
	// @endcond
};

}
}

#include "../Impl/TextDiffOptions.inl"
#endif // PDFTRON_H_CPPPDFTextDiffOptions