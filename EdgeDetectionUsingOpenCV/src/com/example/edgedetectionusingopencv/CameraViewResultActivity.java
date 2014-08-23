package com.example.edgedetectionusingopencv;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.example.edgedetectionusingopencv.customviews.CustomCameraView;

public class CameraViewResultActivity extends Activity implements CvCameraViewListener2
{
	private final String TAG = this.getClass().getSimpleName();
	private CustomCameraView mOpenCvCameraView;
	public String imgPath, imgName, imgPathName;
	Button btnCount;
	private Handler mHandler = new Handler();
	ProgressDialog pDialog;

	public CameraViewResultActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	static 
	{
		if(!OpenCVLoader.initDebug()) 
		{
			// not loaded
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera_view);

		mOpenCvCameraView = (CustomCameraView) findViewById(R.id.customcameraview_activity_java_surface_view);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.enableView();		
		mOpenCvCameraView.setCvCameraViewListener(this);		
		btnCount=(Button) findViewById(R.id.btnCount);
		btnCount.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {					
				saveImage(); //Capture image 				
			}
		});
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		mOpenCvCameraView.enableView();
		//OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
	}

	public void onDestroy() 
	{
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height){ 
	}

	public void onCameraViewStopped() {
	}	

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		return inputFrame.rgba();
	}

	/**
	 * Capture Image and save to sdcard
	 */
	public void saveImage() {
		File f = new File(Environment.getExternalStorageDirectory() + "/openCv");		
		if(f.exists() && f.isDirectory())
		{
			Log.v("FILES", "EXIST");
			deleteDirectory(f);
			f.mkdirs();
		}
		else
		{
			Log.v("FILES", "DONT EXIST");
			f.mkdirs();
		}

		imgName = "orignal";	
		imgPath = Environment.getExternalStorageDirectory()+"/openCv/";	
		imgPathName = imgPath + imgName + ".jpg";

		mOpenCvCameraView.takePicture(imgPathName);
		//Toast.makeText(this, imgPath + " saved", Toast.LENGTH_SHORT).show();

		pDialog = new ProgressDialog(CameraViewResultActivity.this);
		pDialog.setMessage("Please wait..");
		pDialog.setIndeterminate(true);
		pDialog.setCancelable(false);
		pDialog.show();

		mHandler.postDelayed(new Runnable()
		{
			public void run()
			{
				processMyImage(); //image processing and count object
			}
		}, 2000);
	}

	/**
	 * image processing and count object
	 */
	private void processMyImage()
	{
		String output =	getCountofEdges(); 
		pDialog.dismiss();
		displayAlert(output);
	}

	public String getCountofEdges() {
		Bitmap i = getBitmap(imgPath + "orignal.jpg");
		
		Bitmap bmpImg = i.copy(Bitmap.Config.ARGB_8888, false);
		bmpImg =SetBrightness(bmpImg,-70);
		//Log.i("after Bitmap bmpImg",""+imgPath);
		Mat srcMat = new Mat ( bmpImg.getHeight(), bmpImg.getWidth(), CvType.CV_8UC3);
		Bitmap myBitmap32 = bmpImg.copy(Bitmap.Config.ARGB_8888, true);
		Utils.bitmapToMat(bmpImg, srcMat);

		Mat dst = new Mat();
		Imgproc.cvtColor(srcMat, dst, Imgproc.COLOR_BGR2GRAY);
		Highgui.imwrite(imgPath + "gray.jpg", dst);
		
		Imgproc.GaussianBlur(dst, dst, new Size(), 2);
		Highgui.imwrite(imgPath + "gaussian.jpg", dst);
		
		Mat threshMat = new Mat(dst.rows(), dst.cols(), CvType.CV_8UC1);

		//Imgproc.threshold(dst, dst, -1, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
		Imgproc.adaptiveBilateralFilter(dst, threshMat, new Size(1, 1),1.0);
		Highgui.imwrite(imgPath + "bilateral_filter.jpg", threshMat);
		
		/*Imgproc.adaptiveThreshold(threshMat, threshMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 3, 1);
		Highgui.imwrite(imgPath + "adaptive_threshold.jpg", threshMat);*/
		
		Imgproc.threshold(threshMat, threshMat, -1, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
		Highgui.imwrite(imgPath + "threshold.jpg", threshMat);
		
		/*Core.bitwise_xor(srcMat, dst, dst);
		Highgui.imwrite(imgPath + "xored.jpg", dst);*/
		
		Imgproc.dilate(threshMat, threshMat, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(16, 16)));
		Highgui.imwrite(imgPath + "dilated.jpg", threshMat);
		
		// canny edge detection
		// here
		Mat edge = new Mat();
		Imgproc.Canny(threshMat, edge, 35,90); // 40,120 -> to try
		
		Log.e(TAG,"edge: "  + edge.cols());
		
		edge.convertTo(threshMat,CvType.CV_8U);
		
		Log.e(TAG,"dst cols " + threshMat.cols());
 
		Highgui.imwrite(imgPath + "canny.jpg", threshMat);
		
		Imgproc.dilate(threshMat, threshMat, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(4, 4)));
		Highgui.imwrite(imgPath + "dilated1.jpg", threshMat);
		
		// flood fill
		/*Mat matMask = new Mat(dst.rows() + 2, dst.cols() + 2, CvType.CV_8UC1, new Scalar(255));
		
		for(int ii=0;ii<dst.rows();ii++) {
            for(int j=0;j<dst.cols();j++) {
            	Log.e(TAG,"dst.get(ii, j) : " + dst.get(ii, j).length);
                double checker = dst.get(ii, j)[0];
                
                Log.e(TAG,"checker : " + checker);
                
                if(checker == 255) {
                    Imgproc.floodFill(dst, matMask, new Point(j,ii), new Scalar(255), null,new Scalar(255), new Scalar(255), 8);
                }
            }
        }
		
		Highgui.imwrite(imgPath + "flooded.jpg", dst);*/
		
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat(threshMat.rows(), threshMat.cols(),CvType.CV_8UC1);
		Imgproc.findContours(threshMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		Log.e(TAG,"contours : " + contours.size());
		int counter = 0;
		
		for (int idx = 0; idx < contours.size(); idx++) {
			double[] values = hierarchy.get(idx, 0);
			if(values != null) {
				Log.e(TAG,"dst : " + values[0]);
				if(values[0] != -1) {
					counter ++;
					Imgproc.drawContours(threshMat, contours, idx, new Scalar(255), -1,8,hierarchy,0,new Point());
				}
			}
			
			values = hierarchy.get(idx, 1);
			if(values != null) {
				Log.e(TAG,"dst 1 : " + values[0]);
				if(values[0] != -1) {
					counter ++;
					Imgproc.drawContours(threshMat, contours, idx, new Scalar(255), -1,8,hierarchy,0,new Point());
				}
			}
		}
		
		Highgui.imwrite(imgPath + "flooded2.jpg", threshMat);
		
		//Utils.matToBitmap(dst, bmpImg);
		
		//BlobDetection blob = new BlobDetection(bmpImg);
		//Bitmap anotherBitmap = blob.getBlob(bmpImg);
		
		//Mat newMat = new Mat();
		
		//Utils.bitmapToMat(anotherBitmap, newMat);
		//Highgui.imwrite(imgPath + "newMat.jpg", newMat);
		
		//List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		//Imgproc.findContours(dst, contours, dst, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

		List<RotatedRect> lstRotatedRects = new ArrayList<RotatedRect>();
		int ellipseCounter = 0;
		
		// fit ellipses
		for (int idx = 0; idx < contours.size(); idx++) {

			MatOfPoint  matOfPoint = new MatOfPoint(contours.get(idx));
			MatOfPoint2f matOfPoint2f = new MatOfPoint2f(matOfPoint.toArray());

			/*for (int j=0; j<dst.rows(); j++) {
				for (int k=0; k<dst.cols(); k++)
				{
					//				contourOuter, Point2f(k,j),false
					if ( Imgproc.pointPolygonTest(matOfPoint2f, 
							new Point((double)k,(double) j), false) == 0){
						//dst.get(j, i)=255;

						double[] data = dst.get(j,k);
						for(int l = 0 ;l<data.length;l++) {
							data[l] = 125;
						}

						dst.put(j, k, data);

					}else {
						double[] data = dst.get(j,k);
						for(int l = 0 ;l<data.length;l++) {
							data[l] = 80;
						}

						dst.put(j, k, data);
					}
				}
			}
			 Imgproc.drawContours(dst,contours,idx,new Scalar(255),8);*/
			if(matOfPoint.total() >= 5) {
				Log.e(TAG," : " + matOfPoint.total());
				Log.e(TAG," Imgproc.contourArea(matOfPoint) : " + Imgproc.contourArea(matOfPoint));
				//if(Imgproc.isContourConvex(contours.get(idx))) {
					Log.e(TAG,"Imgproc.contourArea(matOfPoint) : " + Imgproc.contourArea(matOfPoint) );
					if(String.valueOf(Imgproc.contourArea(matOfPoint)).length() >= 4) {
						lstRotatedRects.add(Imgproc.fitEllipse(matOfPoint2f));
						ellipseCounter ++;
						Core.ellipse( threshMat,Imgproc.fitEllipse(matOfPoint2f), new Scalar(255), -1, 8 );
					}
				//}
			}
		}
		
		//Utils.matToBitmap(dst, bmpImg);
		Highgui.imwrite(imgPath + "ellipsed.jpg", threshMat);
		
		//Toast.makeText(this, "now going to draw ellipse", 3000).show();
		
		/*BitmapDrawable bitmapDrawable = new BitmapDrawable(anotherBitmap);
		imageViewShower.setBackgroundDrawable(bitmapDrawable);
		textViewInfo.setText("Found "+blob.blobList.size()+" blobs:\n");*/
		//System.out.printf("Found %d blobs:\n", blob.blobList.size());
		//for (BlobDetection.Blob blobies : blob.blobList)  {
			//System.out.println(blobies);
			//textViewInfo.setText(textViewInfo.getText() +" " +  blobies+"\n");
		//}
		
		return String.valueOf(contours.size()) + " " + String.valueOf(ellipseCounter);//blob.blobList.size());
	}
	

	/**
	 * @param image path and get bitmap
	 * @return bitmap
	 */
	private Bitmap getBitmap(String path) 
	{
		Uri uri = Uri.parse(path);
		InputStream in = null;
		try 
		{
			final int IMAGE_MAX_SIZE = 1200000; // 1.2MP
			in = new FileInputStream(path);

			// Decode image size
			// decode image size (decode metadata only, not the whole image)
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(in, null, o);
			in.close();
			in = null;

			int scale = 1;
			while ((o.outWidth * o.outHeight) * (1 / Math.pow(scale, 2)) > 
			IMAGE_MAX_SIZE)
			{
				scale++;
			}
			Log.d(TAG, "scale = " + scale + ", orig-width: " + o.outWidth + ", orig-height: " + o.outHeight);

			Bitmap b = null;
			in = new FileInputStream(path);
			if (scale > 1) 
			{
				scale--;
				// scale to max possible inSampleSize that still yields an image
				// larger than target
				o = new BitmapFactory.Options();
				o.inSampleSize = scale;
				b = BitmapFactory.decodeStream(in, null, o);

				// resize to desired dimensions
				int height = b.getHeight();
				int width = b.getWidth();
				Log.d(TAG, "1th scale operation dimenions - width: " + width + ", height: " + height);

				double y = Math.sqrt(IMAGE_MAX_SIZE / (((double) width) / height));
				double x = (y / height) * width;

				Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, (int) x, (int) y, true);
				b.recycle();
				b = scaledBitmap;

				System.gc();
			} 
			else 
			{
				b = BitmapFactory.decodeStream(in);
			}
			in.close();

			Log.d(TAG, "bitmap size - width: " +b.getWidth() + ", height: " +b.getHeight());
			return b;
		} 
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(),e);
			return null;
		}
	}

	private void displayAlert(String alertString)
	{
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CameraViewResultActivity.this);
		alertDialogBuilder.setMessage(alertString).setCancelable(false)
		.setPositiveButton("Retry", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.cancel();
			}			
		})
		.setNegativeButton("Done",new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog,int id) {							
				dialog.cancel();
			}
		});
		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
	}

	/**
	 * @param src :- bitmap image
	 * @param value :- int value to increase or decrease brightness
	 * @return bitmap
	 */
	public Bitmap SetBrightness(Bitmap src, int value) 
	{
		// original image size
		int width = src.getWidth();
		int height = src.getHeight();
		// create output bitmap
		Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
		// color information
		int A, R, G, B;
		int pixel;

		// scan through all pixels
		for(int x = 0; x < width; ++x)
		{
			for(int y = 0; y < height; ++y)
			{
				// get pixel color
				pixel = src.getPixel(x, y);
				A = Color.alpha(pixel);
				R = Color.red(pixel);
				G = Color.green(pixel);
				B = Color.blue(pixel);

				// increase/decrease each channel
				R += value;
				if(R > 255) { R = 255; }
				else if(R < 0) { R = 0; }

				G += value;
				if(G > 255) { G = 255; }
				else if(G < 0) { G = 0; }

				B += value;
				if(B > 255) { B = 255; }
				else if(B < 0) { B = 0; }

				// apply new pixel color to output bitmap
				bmOut.setPixel(x, y, Color.argb(A, R, G, B));
			}
		}	   
		// return final image
		return bmOut;
	}	

	/**
	 * @param path :- file path which you want to delete
	 * @return boolean
	 */
	public static boolean deleteDirectory(File path) 
	{
		if( path.exists() ) 
		{
			File[] files = path.listFiles();
			if (files == null) 
			{
				return true;
			}
			for(int i=0; i<files.length; i++) 
			{
				if(files[i].isDirectory())
				{
					deleteDirectory(files[i]);
				}
				else 
				{
					files[i].delete();
				}
			}
		}
		return( path.delete() );
	}


	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	public Bitmap thresholdImage(Bitmap src, int maxValue) 
	{
		// original image size
		int width = src.getWidth();
		int height = src.getHeight();
		// create output bitmap
		Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
		// color information
		int A, R, G, B;
		int pixel;

		// scan through all pixels
		for(int x = 0; x < width; ++x)
		{
			for(int y = 0; y < height; ++y)
			{
				// get pixel color
				pixel = src.getPixel(x, y);
				A = Color.alpha(pixel);
				R = Color.red(pixel);
				G = Color.green(pixel);
				B = Color.blue(pixel);

				// increase/decrease each channel
				if(R < maxValue ){
					R = 0;
				}else {
					R = 255;
				}
				
				if(G < maxValue ){
					G = 0;
				}else {
					G = 255;
				}

				if(B < maxValue ){
					B = 0;
				}else {
					B = 255;
				}
				// apply new pixel color to output bitmap
				bmOut.setPixel(x, y, Color.argb(A, R, G, B));
			}
		}	   
		// return final image
		return bmOut;
	}	
}
