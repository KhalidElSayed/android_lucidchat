package fi.harism.lucidchat;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

public class ChatFlipView extends FrameLayout {

	// Static values for current flip mode.
	private static final int FLIP_NEXT = 0;
	private static final int FLIP_NONE = 1;
	private static final int FLIP_PREV = 2;

	// Fragment Shader.
	private static final String SHADER_FRAGMENT = "\r\n"
			+ "precision mediump float;                             \r\n"
			+ "uniform sampler2D sLeft;                             \r\n"
			+ "uniform sampler2D sRight;                            \r\n"
			+ "uniform float uniformX;                              \r\n"
			+ "varying vec2 vPos;                                   \r\n"
			+ "void main() {                                        \r\n"
			+ "  if (vPos.x <= 0.0 && vPos.x < uniformX) {          \r\n"
			+ "    vec2 tPos = vec2(vPos.x, -vPos.y) * 0.5 + 0.5;   \r\n"
			+ "    gl_FragColor = texture2D(sLeft, tPos);           \r\n"
			+ "    float c = min(1.0, 1.0 + uniformX);              \r\n"
			+ "    gl_FragColor *= mix(0.5, 1.0, c);                \r\n"
			+ "  }                                                  \r\n"
			+ "  else if (vPos.x > 0.0 && vPos.x > uniformX) {      \r\n"
			+ "    vec2 tPos = vec2(vPos.x, -vPos.y) * 0.5 + 0.5;   \r\n"
			+ "    gl_FragColor = texture2D(sRight, tPos);          \r\n"
			+ "    float c = max(0.0, uniformX);                    \r\n"
			+ "    gl_FragColor *= mix(1.0, 0.5, c);                \r\n"
			+ "  }                                                  \r\n"
			+ "  else if (vPos.x <= 0.0) {                          \r\n"
			+ "    float vx = vPos.x / uniformX;                    \r\n"
			+ "    vec2 tPos = vec2(-vx, -vPos.y);                  \r\n"
			+ "    tPos.y += (1.0 + uniformX) * 0.5 * vx * vPos.y;  \r\n"
			+ "    tPos = tPos * 0.5 + 0.5;                         \r\n"
			+ "    gl_FragColor = texture2D(sRight, tPos);          \r\n"
			+ "    float c = min(1.0, 1.0 + uniformX);              \r\n"
			+ "    gl_FragColor *= mix(1.0, 0.5, c);                \r\n"
			+ "  }                                                  \r\n"
			+ "  else if (vPos.x > 0.0) {                           \r\n"
			+ "    float vx = vPos.x / uniformX;                    \r\n"
			+ "    vec2 tPos = vec2(vx, -vPos.y);                   \r\n"
			+ "    tPos.y += (1.0 - uniformX) * 0.5 * vx * vPos.y;  \r\n"
			+ "    tPos = tPos * 0.5 + 0.5;                         \r\n"
			+ "    gl_FragColor = texture2D(sLeft, tPos);           \r\n"
			+ "    float c = max(0.0, uniformX);                    \r\n"
			+ "    gl_FragColor *= mix(0.5, 1.0, c);                \r\n"
			+ "  }                                                  \r\n"
			+ "}                                                    \r\n";

	// Vertex Shader.
	private static final String SHADER_VERTEX = "\r\n"
			+ "attribute vec2 aPos;                                 \r\n"
			+ "varying vec2 vPos;                                   \r\n"
			+ "void main() {                                        \r\n"
			+ "  gl_Position = vec4(aPos, 0.0, 1.0);                \r\n"
			+ "  vPos = aPos;                                       \r\n"
			+ "}                                                    \r\n";

	private DataSetObserver mDataSetObserver = new DataSetObserver();
	private int mFlipMode = FLIP_NONE;
	private FlipRenderer mFlipRenderer;
	private Observer mObserver;
	private PointF mTouchPos = new PointF();
	private int mViewChildIndex = 0;
	private View[] mViewChildren = new View[0];

	public ChatFlipView(Context context) {
		super(context);
		init(context);
	}

	public ChatFlipView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ChatFlipView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public void animateCurrentView(final int index) {
		if (index < 0 || index >= mViewChildren.length
				|| index == mViewChildIndex) {
			return;
		}

		mFlipMode = FLIP_NONE;
		setViewVisibility(mViewChildren[index], View.INVISIBLE);
		mViewChildren[index].requestLayout();
		post(new Runnable() {
			@Override
			public void run() {
				mFlipRenderer.bringToFront();
				if (index < mViewChildIndex) {
					updateRendererBitmaps(index, mViewChildIndex);
					mFlipRenderer.setFlipPosition(-1f);
					mFlipRenderer.moveFlipPosition(1f);
				} else if (index > mViewChildIndex) {
					updateRendererBitmaps(mViewChildIndex, index);
					mFlipRenderer.setFlipPosition(1f);
					mFlipRenderer.moveFlipPosition(-1f);
				}

				mViewChildIndex = index;
				mFlipRenderer.requestRender();
				invalidate();
			}
		});
	}

	public int getCurrentIndex() {
		return mViewChildIndex;
	}

	private void init(Context context) {
		mFlipRenderer = new FlipRenderer(context);

		LayoutParams params = new LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.MATCH_PARENT);
		addView(mFlipRenderer, params);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (mFlipMode == FLIP_NONE) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_MOVE:
				if (Math.abs(event.getX() - mTouchPos.x) > 2 * Math.abs(event
						.getY() - mTouchPos.y)) {
					onTouchEvent(event);
				}
			case MotionEvent.ACTION_DOWN:
				mTouchPos.set(event.getX(), event.getY());
				break;
			}
		} else {
			onTouchEvent(event);
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float mx = event.getX();
		switch (event.getAction()) {

		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			if (mFlipMode != FLIP_NONE) {
				float fp = (2 * mx - getWidth()) / getWidth();
				fp = Math.min(1f, Math.max(-1f, fp));
				mFlipRenderer.moveFlipPosition(fp);
				mFlipRenderer.requestRender();
			} else {
				if (mx * 2 > getWidth()
						&& mViewChildIndex < mViewChildren.length - 1) {
					mFlipMode = FLIP_NEXT;
					updateRendererBitmaps(mViewChildIndex, mViewChildIndex + 1);
					mFlipRenderer.bringToFront();
					mFlipRenderer.setFlipPosition(1f);
					mFlipRenderer.requestRender();
					invalidate();
				}
				if (mx * 2 < getWidth() && mViewChildIndex > 0) {
					mFlipMode = FLIP_PREV;
					updateRendererBitmaps(mViewChildIndex - 1, mViewChildIndex);
					mFlipRenderer.bringToFront();
					mFlipRenderer.setFlipPosition(-1f);
					mFlipRenderer.requestRender();
					invalidate();
				}
			}
			break;

		case MotionEvent.ACTION_UP:
			if (mx * 2 < getWidth() && mFlipMode == FLIP_NEXT) {
				mViewChildIndex++;
			}
			if (mx * 2 > getWidth() && mFlipMode == FLIP_PREV) {
				mViewChildIndex--;
			}
			mFlipMode = FLIP_NONE;
			mFlipRenderer.moveFlipPosition(mx * 2 > getWidth() ? 1f : -1f);
			mFlipRenderer.requestRender();
			break;
		}

		return false;
	}

	/**
	 * Setter for View adapter for this Binder View.
	 * 
	 * @param adapter
	 *            View Adapter.
	 */
	public void setAdapter(ChatFlipAdapter adapter) {
		adapter.addObserver(mDataSetObserver);

		int count = adapter.getCount();
		mViewChildren = new View[count];
		for (int i = 0; i < count; ++i) {
			mViewChildren[i] = adapter.createView(this, i);
		}
		setCurrentView(0);
	}

	/**
	 * Setter for current visible View.
	 */
	public void setCurrentView(int index) {
		if (index >= 0 && index < mViewChildren.length) {
			setViewVisibility(mViewChildren[index], View.VISIBLE);
			mViewChildren[index].bringToFront();
			mViewChildIndex = index;

			if (mObserver != null) {
				mObserver.onPageChanged(index);
			}
		}
		if (index > 0) {
			setViewVisibility(mViewChildren[index - 1], View.INVISIBLE);
		}
		if (index < mViewChildren.length - 1) {
			setViewVisibility(mViewChildren[index + 1], View.INVISIBLE);
		}

		for (int i = 0; i < index - 1; ++i) {
			removeView(mViewChildren[i]);
		}
		for (int i = index + 2; i < mViewChildren.length; ++i) {
			removeView(mViewChildren[i]);
		}

		invalidate();
	}

	public void setObserver(Observer observer) {
		mObserver = observer;
	}

	/**
	 * Changes requested view visibility.
	 */
	private void setViewVisibility(View view, int visibility) {
		view.setVisibility(visibility);
		// View is already attached.
		if (indexOfChild(view) >= 0) {
			return;
		}
		// otherwise add view to ViewGroup.
		addView(view, 0);
	}

	/**
	 * Updates renderer Bitmaps.
	 */
	private void updateRendererBitmaps(int leftIdx, int rightIdx) {

		// Generate two offscreen bitmaps.
		Bitmap left = Bitmap.createBitmap(getWidth(), getHeight(),
				Bitmap.Config.ARGB_8888);
		Bitmap right = Bitmap.createBitmap(getWidth(), getHeight(),
				Bitmap.Config.ARGB_8888);

		Canvas c = new Canvas(left);
		View v = mViewChildren[leftIdx];
		c.translate(-v.getScrollX(), -v.getScrollY());
		v.draw(c);

		c = new Canvas(right);
		v = mViewChildren[rightIdx];
		c.translate(-v.getScrollX(), -v.getScrollY());
		v.draw(c);

		mFlipRenderer.setBitmaps(left, right);
	}

	private class DataSetObserver implements ChatFlipAdapter.Observer {

		@Override
		public void onDataSetChanged(ChatFlipAdapter adapter) {
			int count = adapter.getCount();
			mViewChildren = new View[count];
			for (int i = 0; i < count; ++i) {
				mViewChildren[i] = adapter.createView(ChatFlipView.this, i);
			}
			setCurrentView(Math.max(0, Math.min(mViewChildIndex, count - 1)));
		}

	}

	private class FlipRenderer extends GLSurfaceView implements
			GLSurfaceView.Renderer {

		private Bitmap mBitmapLeft, mBitmapRight;
		private ByteBuffer mCoords;
		private float mFlipPosition;
		private float mFlipPositionTarget;
		private long mLastRenderTime;
		private int mProgram;
		private int[] mTextureIds;

		public FlipRenderer(Context context) {
			super(context);
			setEGLContextClientVersion(2);
			setRenderer(this);
			setRenderMode(RENDERMODE_WHEN_DIRTY);

			final byte[] COORDS = { -1, 1, -1, -1, 1, 1, 1, -1 };
			mCoords = ByteBuffer.allocateDirect(8);
			mCoords.put(COORDS).position(0);
		}

		/**
		 * Private shader loader.
		 */
		private final int loadProgram(String vs, String fs) throws Exception {
			int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vs);
			int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);
			int program = GLES20.glCreateProgram();
			if (program != 0) {
				GLES20.glAttachShader(program, vertexShader);
				GLES20.glAttachShader(program, fragmentShader);
				GLES20.glLinkProgram(program);
				int[] linkStatus = new int[1];
				GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS,
						linkStatus, 0);
				if (linkStatus[0] != GLES20.GL_TRUE) {
					String error = GLES20.glGetProgramInfoLog(program);
					GLES20.glDeleteProgram(program);
					throw new Exception(error);
				}
			}
			return program;
		}

		/**
		 * Private shader loader/compiler.
		 */
		private final int loadShader(int shaderType, String source)
				throws Exception {
			int shader = GLES20.glCreateShader(shaderType);
			if (shader != 0) {
				GLES20.glShaderSource(shader, source);
				GLES20.glCompileShader(shader);
				int[] compiled = new int[1];
				GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS,
						compiled, 0);
				if (compiled[0] == 0) {
					String error = GLES20.glGetShaderInfoLog(shader);
					GLES20.glDeleteShader(shader);
					throw new Exception(error);
				}
			}
			return shader;
		}

		/**
		 * Animates flip position to requested position.
		 */
		public void moveFlipPosition(float posY) {
			mFlipPositionTarget = posY;
		}

		@Override
		public void onDrawFrame(GL10 unused) {
			// Disable unneeded rendering flags.
			GLES20.glDisable(GLES20.GL_DEPTH_TEST);
			GLES20.glDisable(GLES20.GL_CULL_FACE);

			// Allocate new texture ids if needed.
			if (mTextureIds == null) {
				mTextureIds = new int[2];
				GLES20.glGenTextures(2, mTextureIds, 0);
				for (int textureId : mTextureIds) {
					// Set texture attributes.
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
							GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
							GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
							GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
							GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
				}
			}

			if (mBitmapLeft != null) {
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[0]);
				GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmapLeft, 0);
				mBitmapLeft.recycle();
				mBitmapLeft = null;
			}
			if (mBitmapRight != null) {
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[1]);
				GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmapRight, 0);
				mBitmapRight.recycle();
				mBitmapRight = null;
			}

			// Use our vertex/fragment shader program.
			GLES20.glUseProgram(mProgram);
			// Fetch variable ids.
			int uniformX = GLES20.glGetUniformLocation(mProgram, "uniformX");
			int sLeft = GLES20.glGetUniformLocation(mProgram, "sLeft");
			int sRight = GLES20.glGetUniformLocation(mProgram, "sRight");
			int aPos = GLES20.glGetAttribLocation(mProgram, "aPos");

			// If there's room for animation.
			if (Math.abs(mFlipPosition - mFlipPositionTarget) > 0.01f) {
				long currentTime = SystemClock.uptimeMillis();
				float t = Math.min(1f, (currentTime - mLastRenderTime) * .01f);
				mFlipPosition = mFlipPosition
						+ (mFlipPositionTarget - mFlipPosition) * t;
				mLastRenderTime = currentTime;
				requestRender();
			}
			// If we're done with animation plus user left us with touch up
			// event.
			else if (mFlipMode == FLIP_NONE) {
				post(new Runnable() {
					@Override
					public void run() {
						setCurrentView(mViewChildIndex);
					}
				});
			}

			// Set flip position variable.
			GLES20.glUniform1f(uniformX, mFlipPosition);
			// Set texture variables.
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[0]);
			GLES20.glUniform1i(sLeft, 0);
			GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[1]);
			GLES20.glUniform1i(sRight, 1);
			// Set vertex position variables.
			GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_BYTE, false, 0,
					mCoords);
			GLES20.glEnableVertexAttribArray(aPos);
			// Render quad.
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		}

		@Override
		public void onSurfaceChanged(GL10 unused, int width, int height) {
			// All we have to do is set viewport.
			GLES20.glViewport(0, 0, width, height);
		}

		@Override
		public void onSurfaceCreated(GL10 unused, EGLConfig config) {
			try {
				// Force instantiation for new texture ids.
				mTextureIds = null;
				// Load vertex/fragment shader program.
				mProgram = loadProgram(SHADER_VERTEX, SHADER_FRAGMENT);
			} catch (final Exception ex) {
				// On error show Toast.
				post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getContext(), ex.toString(),
								Toast.LENGTH_SHORT).show();
					}
				});
			}
		}

		/**
		 * Setter for Bitmaps.
		 */
		public void setBitmaps(Bitmap bitmapLeft, Bitmap bitmapRight) {
			mBitmapLeft = bitmapLeft;
			mBitmapRight = bitmapRight;
		}

		/**
		 * Setter for flip position, value between [-1, 1].
		 */
		public void setFlipPosition(float posY) {
			mFlipPosition = posY;
			mFlipPositionTarget = posY;
			mLastRenderTime = SystemClock.uptimeMillis();
		}

	}

	public interface Observer {
		public void onPageChanged(int index);
	}

}
