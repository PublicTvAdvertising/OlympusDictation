package com.olympus.dmmobile.recorder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.olympus.dmmobile.R;

/**
 * Class for drawing Sound wav graph for both insert and overwrite process.
 * @version 1.0.1
 */
public class SoundWaveGraph {
	Bitmap mWaveBitmap;
	Canvas mCanvas;
	Context mContext;
	Paint mPaint;
	RandomAccessFile mRandomAccessFile;

	/**
	 * Constructor of this class
	 */
	SoundWaveGraph(DictateActivity con) {
		mContext = con;
		mPaint = new Paint();
		mPaint.setStyle(Style.STROKE);
		mPaint.setColor(Color.BLUE);
		mPaint.setAntiAlias(true);

	}

	/**
	 * Returns bitmap with graph drawn which can be set as background for the
	 * seekbar.
	 * 
	 * @return Bitmap with the graph drawn
	 */
	public Bitmap getSoundWavegraph() {
		// Intializing bitmap with width 490 and height 60.
		mWaveBitmap = Bitmap.createBitmap(490, 60, Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mWaveBitmap);

		float width = mWaveBitmap.getWidth();
		float height = mWaveBitmap.getHeight();

		// mCenterpoint ,Calculating the vertical center point of the bitmap.
		float mCenterpoint = height / 2;
		float mShortValue = 0;

		byte bData[] = new byte[2];

		try {
			// intializing the RandomAccessFile with the path of the
			// corresponding file.
			mRandomAccessFile = new RandomAccessFile(
					DictateActivity.getFilename(), "rw");

			// Skipping the header of the file. First 44 bytes.
			mRandomAccessFile.skipBytes(44);

			// Process of drawing the graph in bitmap starts with this loop.
			for (int iPixel = 1; iPixel < width; iPixel++) {
				// 960 is a calculated round value to read that particular
				// sample of the file to draw the graph
				double mPerwidth = (double) iPixel / 960;
				int SampletoRead;
				long totalLen = mRandomAccessFile.length() - 44;

				// After calculation we get which sample of file should be read.
				SampletoRead = (int) (totalLen * mPerwidth) * 2;

				// 2 data samples of the file should be read.
				if (SampletoRead <= totalLen) {
					mRandomAccessFile.seek(SampletoRead);
					bData[0] = mRandomAccessFile.readByte();
					mRandomAccessFile.seek(SampletoRead + 1);
					bData[1] = mRandomAccessFile.readByte();
				} else {
					break;
				}
				// A leftshift is done to get the short value from byte data.
				mShortValue = (short) ((bData[1] & 0xff) << 8 | (bData[0] & 0xff));

				// Scale factor should be calculated for placing the graph
				// correctly to the view.
				float mScaleFactor = ((float) mShortValue / Short.MAX_VALUE) * 40;

				// from centerpoint we draw that scaled value in y axis toward
				// (+)ve
				// and (-)ve directions.
				float yMax = mCenterpoint + mScaleFactor;
				float yMin = mCenterpoint - mScaleFactor;

				// Setting the width of the line to be drawn
				mPaint.setStrokeWidth(3);

				// Using shader we apply a gradient to the line drawn
				mPaint.setShader(new LinearGradient(iPixel, yMax, iPixel, yMin,
						mContext.getResources().getColor(R.color.blue_top),
						mContext.getResources().getColor(R.color.blue_bottom),
						TileMode.MIRROR));
				// In canvas we draw the lines.
				mCanvas.drawLine(iPixel, yMax, iPixel, yMin, mPaint);

				// For avoiding the unnecessary breaks we draw a line through
				// the centerpoint. So that we will get the continuity
				mPaint.setStrokeWidth(2);
				mCanvas.drawPoint(iPixel, mCenterpoint, mPaint);

				// in each pixels this process will be repeated hence we get the
				// complete graph for the .wav file.
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Then the generated bitmap is passed when the function is called.
		return mWaveBitmap;
	}

	/**
	 * The logic is same as in the getSoundWavegraph() the only difference here
	 * is that the change in the colour of the graph drawn. While drawing, the shader will
	 * be applaid according to the need.
	 * @return Bitmap with graph drawn
	 */
	public Bitmap getSoundWavegraphforOverWrite(boolean isRed) {

		mWaveBitmap = Bitmap.createBitmap(490, 60, Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mWaveBitmap);

		float width = mWaveBitmap.getWidth();
		float height = mWaveBitmap.getHeight();

		float mCenterpoint = height / 2;
		float mShortValue = 0;

		byte bData[] = new byte[2];

		try {
			mRandomAccessFile = new RandomAccessFile(
					DictateActivity.getFilename(), "rw");

			mRandomAccessFile.skipBytes(44);
			for (int iPixel = 1; iPixel < width; iPixel++) {

				double mPerwidth = (double) iPixel / 960;
				int SampletoRead;
				long totalLen = mRandomAccessFile.length() - 44;
				SampletoRead = (int) (totalLen * mPerwidth) * 2;

				if (SampletoRead <= totalLen) {
					mRandomAccessFile.seek(SampletoRead);
					bData[0] = mRandomAccessFile.readByte();
					mRandomAccessFile.seek(SampletoRead + 1);
					bData[1] = mRandomAccessFile.readByte();
				} else {
					break;
				}
				mShortValue = (short) ((bData[1] & 0xff) << 8 | (bData[0] & 0xff));

				float mScaleFactor = ((float) mShortValue / Short.MAX_VALUE) * 40;

				float yMax = mCenterpoint + mScaleFactor;
				float yMin = mCenterpoint - mScaleFactor;
				mPaint.setStrokeWidth(3);
				//Here we decide in which colour should the line be drawn.
				if (!isRed) {
					mPaint.setShader(new LinearGradient(iPixel, yMax, iPixel,
							yMin, mContext.getResources().getColor(
									R.color.blue_top), mContext.getResources()
									.getColor(R.color.blue_bottom),
							TileMode.MIRROR));
				} else {
					mPaint.setShader(new LinearGradient(iPixel, yMax, iPixel,
							yMin, mContext.getResources().getColor(
									R.color.orange_top), mContext
									.getResources().getColor(
											R.color.orange_bottom),
							TileMode.MIRROR));
				}
				mCanvas.drawLine(iPixel, yMax, iPixel, yMin, mPaint);
				mPaint.setStrokeWidth(2);
				mCanvas.drawPoint(iPixel, mCenterpoint, mPaint);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return mWaveBitmap;
	}

}
