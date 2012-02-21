package com.sun.pdfview.decode;

import com.sun.pdfview.PDFObject;
import org.jpedal.io.filter.ccitt.CCITT1D;
import org.jpedal.io.filter.ccitt.CCITT2D;
import org.jpedal.io.filter.ccitt.CCITTDecoder;
import org.jpedal.io.filter.ccitt.CCITTMix;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CCITTFaxDecode {



	protected static ByteBuffer decode(PDFObject dict, ByteBuffer buf,
            PDFObject params) throws IOException {

		byte[] bytes = new byte[buf.remaining()];
	    buf.get(bytes, 0, bytes.length);
		return ByteBuffer.wrap(decode(dict, bytes));
	}


	protected static byte[] decode(PDFObject dict, byte[] source) throws IOException {
		int width = 1728;
		PDFObject widthDef = dict.getDictRef("Width");
		if (widthDef == null) {
			widthDef = dict.getDictRef("W");
		}
		if (widthDef != null) {
			width = widthDef.getIntValue();
		}
		int height = 0;
		PDFObject heightDef = dict.getDictRef("Height");
		if (heightDef == null) {
			heightDef = dict.getDictRef("H");
		}
		if (heightDef != null) {
			height = heightDef.getIntValue();
		}

		//
		int columns = getOptionFieldInt(dict, "Columns", width);
		int rows = getOptionFieldInt(dict, "Rows", height);
		int k = getOptionFieldInt(dict, "K", 0);

		boolean align = getOptionFieldBoolean(dict, "EncodedByteAlign", false);
        boolean blackIsOne = getOptionFieldBoolean(dict, "BlackIs1", false);

        CCITTDecoder decoder;
        if (k == 0){
            // Pure 1D decoding, group3
            decoder = new CCITT1D(source, columns, rows, blackIsOne, align);
        } else if (k < 0) {
            // Pure 2D, group 4
            decoder = new CCITT2D(source, columns, rows, blackIsOne, align);
        } else /*if (k > 0)*/ {
            // Mixed 1/2 D encoding we can use either for maximum compression
            // A 1D line can be followed by up to K-1 2D lines
            decoder = new CCITTMix(source, columns, rows, blackIsOne, align);
        }

        byte[] result;
        try {
    		result = decoder.decode();
        } catch (RuntimeException e) {
            System.out.println("Error decoding CCITTFax image k: "+ k);
            if (k >= 0) {
                // some PDf producer don't correctly assign a k value for the deocde,
                // as  result we can try one more time using the T6.
                //first, reset buffer
                result = new CCITT2D(source, columns, rows, blackIsOne, align).decode();
            } else {
                throw e;
            }
        }
        return result;
	}

	public static int getOptionFieldInt(PDFObject dict, String name, int defaultValue) throws IOException {

		PDFObject dictParams =  dict.getDictRef("DecodeParms");

		if (dictParams == null) {
			return defaultValue;
		}
		PDFObject value = dictParams.getDictRef(name);
		if (value == null) {
			return defaultValue;
		}
		return value.getIntValue();
	}

	public static boolean getOptionFieldBoolean(PDFObject dict, String name, boolean defaultValue) throws IOException {

		PDFObject dictParams =  dict.getDictRef("DecodeParms");

		if (dictParams == null) {
			return defaultValue;
		}
		PDFObject value = dictParams.getDictRef(name);
		if (value == null) {
			return defaultValue;
		}
		return value.getBooleanValue();
	}

}
