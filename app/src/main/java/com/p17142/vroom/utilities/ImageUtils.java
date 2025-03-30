package com.p17142.vroom.utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

public class ImageUtils
{
    /**
     * encodes a Bitmap image into a Base64 string for uploading.
     * @param bitmap The original Bitmap image to be encoded.
     * @return A Base64-encoded string representation of the image.
     */
    public static String encodeImage(Bitmap bitmap){
        int previewWidth = 400;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap,previewWidth,previewHeight,false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG,90,byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
    }

    /**
     * decodes a Base64-encoded image string and returns the corresponding Bitmap.
     * Attempts to decode a provided Base64 string into a Bitmap image.
     * If the string corresponds to a predefined "no image" constant, the default image
     * is used instead. In case of any errors during decoding, a fallback mechanism
     * ensures the use of the default image. The method handles both valid and invalid
     * Base64-encoded strings.
     * @param encodedImage The Base64-encoded image string to be decoded.
     * @return A Bitmap representation of the decoded image, or the default image
     *         if the string is invalid or corresponds to "no image".
     */
    public static Bitmap decodeImage(String encodedImage){
        try {
            if (encodedImage.equals(Constants.NO_IMG_URI)) {
                encodedImage = Constants.DEFAULT_ENCODED_IMAGE;
                byte[] bytes = java.util.Base64.getDecoder().decode(encodedImage);
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } else {

                byte[] bytes = java.util.Base64.getDecoder().decode(encodedImage);
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
        }
        catch (Exception e)
        {
                Logger.printLogError(ImageUtils.class,"Image not Base64Format.Error: "+e+" Image Source: "+encodedImage);
                encodedImage = Constants.DEFAULT_ENCODED_IMAGE;
                byte[] bytes  = java.util.Base64.getDecoder().decode(encodedImage);
                return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        }
    }
}
