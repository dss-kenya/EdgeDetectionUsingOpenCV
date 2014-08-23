package com.example.edgedetectionusingopencv.customviews;

import java.io.FileOutputStream;
import java.util.List;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

public class CustomCameraView extends JavaCameraView implements PictureCallback 
{

    private static final String TAG = "myopticount";
    private String mPictureFileName;

    public CustomCameraView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public List<String> getEffectList()
    {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public boolean isEffectSupported() 
    {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    public String getEffect()
    {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect)
    {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Size> getResolutionList()
    {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Size resolution) 
    {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Size getResolution() 
    {
        return mCamera.getParameters().getPreviewSize();
    }

    public void takePicture(final String fileName)
    {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName;
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) 
    {
    	Log.i(TAG, "Saving a bitmap to file");
    	// The camera preview was automatically stopped. Start it again.
    	
    	mCamera.startPreview();
    	mCamera.setPreviewCallback(this);

    	// Write the image in a file (in jpeg format)
    	try 
    	{
    		FileOutputStream fos = new FileOutputStream(mPictureFileName);

    		fos.write(data);
    		fos.close();

    	}
    	catch (java.io.IOException e) 
    	{
    		Log.e("PictureDemo", "Exception in photoCallback", e);
    	}
    }
    
    public void setFlashMode (Context item, int type)
    {
    	Camera.Parameters params = mCamera.getParameters();
    	List<String> FlashModes = params.getSupportedFlashModes();

    	switch (type)
    	{
    	case 0:            
    		if (FlashModes.contains(Camera.Parameters.FLASH_MODE_OFF))
    			params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
    		else
    			Toast.makeText(item, "Off Mode not supported", Toast.LENGTH_SHORT).show();          
    		break;
    	case 1:
    		if (FlashModes.contains(Camera.Parameters.FLASH_MODE_ON))
    			params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
    		else
    			Toast.makeText(item, "On Mode not supported", Toast.LENGTH_SHORT).show();       
    		break;        
    	}
    	mCamera.setParameters(params);
    }
}
