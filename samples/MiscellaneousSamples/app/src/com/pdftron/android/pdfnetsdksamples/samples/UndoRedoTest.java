package com.pdftron.android.pdfnetsdksamples.samples;//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;
import com.pdftron.common.Matrix2D;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementBuilder;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.Image;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.sdf.DocSnapshot;
import com.pdftron.sdf.ResultSnapshot;
import com.pdftron.sdf.SDFDoc;
import com.pdftron.sdf.UndoManager;

import java.util.ArrayList;

//---------------------------------------------------------------------------------------
// The following sample illustrates how to use the UndoRedo API.
//---------------------------------------------------------------------------------------
public class UndoRedoTest extends PDFNetSample {

	private static OutputListener mOutputListener;

	private static ArrayList<String> mFileList = new ArrayList<>();

    public UndoRedoTest() {
        setTitle(R.string.sample_undoredo_title);
        setDescription(R.string.sample_undoredo_description);
    }

	@Override
	public void run(OutputListener outputListener) {
		super.run(outputListener);
		mOutputListener = outputListener;
		mFileList.clear();
		printHeader(outputListener);
		try 
		{
			// The first step in every application using PDFNet is to initialize the
			// library and set the path to common PDF resources. The library is usually
			// initialized only once, but calling Initialize() multiple times is also fine.

			// Open the PDF document.
			PDFDoc doc = new PDFDoc(Utils.getAssetTempFile(INPUT_PATH + "newsletter.pdf").getAbsolutePath());

			UndoManager undo_manager = doc.getUndoManager();

			// Take a snapshot to which we can undo after making changes.
			ResultSnapshot snap0 = undo_manager.takeSnapshot();

			DocSnapshot snap0_state = snap0.currentState();
			
			Page page = doc.pageCreate();	// Start a new page

			ElementBuilder bld = new ElementBuilder();		// Used to build new Element objects
			ElementWriter writer = new ElementWriter();		// Used to write Elements to the page	
			writer.begin(page);		// Begin writing to this page

			// ----------------------------------------------------------
			// Add JPEG image to the file
			Image img = Image.create(doc, Utils.getAssetTempFile(INPUT_PATH + "peppers.jpg").getAbsolutePath());
			Element element = bld.createImage(img, new Matrix2D(200, 0, 0, 250, 50, 500));
			writer.writePlacedElement(element);

			writer.end();	// Finish writing to the page
			doc.pagePushFront(page);

			// Take a snapshot after making changes, so that we can redo later (after undoing first).
			ResultSnapshot snap1 = undo_manager.takeSnapshot();

			if (snap1.previousState().equals(snap0_state))
			{
				mOutputListener.println("snap1 previous state equals snap0_state; previous state is correct");
			}
			
			DocSnapshot snap1_state = snap1.currentState();

			doc.save(Utils.createExternalFile("addimage.pdf", mFileList).getAbsolutePath(), SDFDoc.SaveMode.INCREMENTAL, null);

			if (undo_manager.canUndo())
			{
				ResultSnapshot undo_snap;
				undo_snap = undo_manager.undo();

				doc.save(Utils.createExternalFile("addimage_undone.pdf", mFileList).getAbsolutePath(), SDFDoc.SaveMode.INCREMENTAL, null);

				DocSnapshot undo_snap_state = undo_snap.currentState();

				if (undo_snap_state.equals(snap0_state))
				{
					mOutputListener.println("undo_snap_state equals snap0_state; undo was successful");
				}
				
				if (undo_manager.canRedo())
				{
					ResultSnapshot redo_snap = undo_manager.redo();

					doc.save(Utils.createExternalFile("addimage_redone.pdf", mFileList).getAbsolutePath(), SDFDoc.SaveMode.INCREMENTAL, null);

					if (redo_snap.previousState().equals(undo_snap_state))
					{
						mOutputListener.println("redo_snap previous state equals undo_snap_state; previous state is correct");
					}
					
					DocSnapshot redo_snap_state = redo_snap.currentState();
					
					if (redo_snap_state.equals(snap1_state))
					{
						mOutputListener.println("Snap1 and redo_snap are equal; redo was successful");
					}
				}
				else
				{
					mOutputListener.println("Problem encountered - cannot redo.");
				}
			}
			else
			{
				mOutputListener.println("Problem encountered - cannot undo.");
			}

			// Calling Terminate when PDFNet is no longer in use is a good practice, but
			// is not required.
		}
		catch (Exception e) 
		{
			mOutputListener.printError(e.getStackTrace());
		}

		for (String file : mFileList) {
			addToFileList(file);
		}
		printFooter(outputListener);
	}

}