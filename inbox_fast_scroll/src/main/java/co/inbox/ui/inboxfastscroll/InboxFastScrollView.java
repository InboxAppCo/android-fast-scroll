package co.inbox.ui.inboxfastscroll;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class InboxFastScrollView
        extends FrameLayout {
    private static final String TAG                   = "InboxFastScroller";
    private static final long   ANIMATION_TIME_BUBBLE = 100;
    private static final float  BOTTOM_SNAP_THRESHOLD = 10;

    private ImageView    mHandle;
    private TextView     mBubble;
    private RecyclerView mRecycler;

    private ScrollListener mScrollListener = new ScrollListener();
    private FastScrollAdapter mAdapter;

    public InboxFastScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.ifs_view, this, true);
        mHandle = (ImageView) findViewById(R.id.ifs_handle);
        mBubble = (TextView) findViewById(R.id.ifs_bubble);

        TypedArray a = context.getTheme()
                              .obtainStyledAttributes(attrs, R.styleable.InboxFastScrollView,
                                                      0, R.style.defaultInboxFastScroller);
        float bubbleRadius;
        int bubbleSize;
        int bubbleColor;
        float textSize;
        int textColor;
        Drawable handleDrawable;

        try {
            bubbleRadius = a.getDimension(R.styleable.InboxFastScrollView_ifsBubbleRadius, 60);
            bubbleSize = a.getDimensionPixelSize(R.styleable.InboxFastScrollView_ifsBubbleSize, 120);
            bubbleColor = a.getColor(R.styleable.InboxFastScrollView_ifsBubbleColor, Color.CYAN);
            textColor = a.getColor(R.styleable.InboxFastScrollView_ifsTextColor, Color.WHITE);
            textSize = a.getDimension(R.styleable.InboxFastScrollView_ifsTextSize, 40);
            handleDrawable = a.getDrawable(R.styleable.InboxFastScrollView_ifsHandleDrawable);
        } finally {
            a.recycle();
        }

        RoundRectShape rect = new RoundRectShape(
                new float[]{bubbleRadius, bubbleRadius, bubbleRadius, bubbleRadius, 0, 0, bubbleRadius, bubbleRadius},
                null, null);
        ShapeDrawable drawable = new ShapeDrawable(rect);
        drawable.setColorFilter(bubbleColor, PorterDuff.Mode.SRC_IN);
        mBubble.setBackgroundDrawable(drawable);
        mBubble.setTextSize(textSize);
        mBubble.setTextColor(textColor);

        mBubble.getLayoutParams().width = bubbleSize;
        mBubble.getLayoutParams().height = bubbleSize;

        ((MarginLayoutParams) mBubble.getLayoutParams()).rightMargin = handleDrawable.getIntrinsicWidth();

        mHandle.setImageDrawable(handleDrawable);
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        mRecycler = recyclerView;
        mRecycler.addOnScrollListener(mScrollListener);
    }

    public void setFastScrollAdapter(FastScrollAdapter adapter) {
        mAdapter = adapter;
    }

    ///////////////////////////////////////////////////////////////////////////////
    // Touch Handling
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // if the touch has outside the handle column we should ignore it
                if (event.getX() < mHandle.getLeft() ||
                        event.getX() > mHandle.getRight() ||
                        event.getY() < mHandle.getY() ||
                        event.getY() > mHandle.getY() + mHandle.getHeight()) {
                    return false;
                }

                if (mBubble.getScaleX() < 1) {
                    showBubble();
                }
                mHandle.setSelected(true);
                // fallthrough

            case MotionEvent.ACTION_MOVE:
                setBubbleAndHandlePosition(event.getY());
                setRecyclerViewPosition(event.getY());
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mHandle.setSelected(false);
                hideBubble();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void showBubble() {
        mBubble.setPivotX(mBubble.getWidth());
        mBubble.setPivotY(mBubble.getHeight());
        ViewCompat.animate(mBubble)
                  .setDuration(ANIMATION_TIME_BUBBLE)
                  .scaleX(1)
                  .scaleY(1);
    }

    private void hideBubble() {
        mBubble.setPivotX(mBubble.getWidth());
        mBubble.setPivotY(mBubble.getHeight());
        ViewCompat.animate(mBubble)
                  .setDuration(ANIMATION_TIME_BUBBLE)
                  .scaleX(0)
                  .scaleY(0);
    }

    private void setRecyclerViewPosition(float y) {
        if (mRecycler == null) {
            return;
        }

        int itemCount = mRecycler.getAdapter().getItemCount();

        float progress;
        if (mHandle.getY() == 0) {
            progress = 0;
        } else if (mHandle.getY() + mHandle.getHeight() >= getHeight() - BOTTOM_SNAP_THRESHOLD) {
            progress = 1;
        } else {
            progress = y / getHeight();
        }

        int targetPos = getValueInRange(0, itemCount - 1, (int) (progress * itemCount));
        if (targetPos >= 0) {
            ((LinearLayoutManager) mRecycler.getLayoutManager())
                    .scrollToPositionWithOffset(targetPos, 0);
            mBubble.setText(mAdapter.getTextForPosition(targetPos));
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    // Scroll Handling
    ///////////////////////////////////////////////////////////////////////////////

    private int getValueInRange(int min, int max, int value) {
        int minimum = Math.max(min, value);
        return Math.min(minimum, max);
    }

    private void setBubbleAndHandlePosition(float y) {
        int bubbleHeight = mBubble.getHeight();
        int handleHeight = mHandle.getHeight();
        mHandle.setY(getValueInRange(0, getHeight() - handleHeight, (int) (y - handleHeight / 2)));
        mBubble.setY(getValueInRange(0, getHeight() - bubbleHeight - handleHeight / 2, (int) (y - bubbleHeight)));
    }

    private class ScrollListener
            extends RecyclerView.OnScrollListener {
        private int mRVChildCount;

        @Override
        public void onScrolled(RecyclerView rv, int dx, int dy) {
            // This gets triggered on manual scrolls so we filter out the ones with no dy
            if (dy == 0) {
                return;
            }

            if (mRVChildCount == 0) {
                // Only set this once to avoid the handle jumping around as views are recycled
                mRVChildCount = rv.getChildCount();
            }

            LinearLayoutManager layoutManager = (LinearLayoutManager) rv.getLayoutManager();
            int firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition();
            int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
            int itemCount = rv.getAdapter().getItemCount();

            float progress;
            if (firstVisiblePosition == 0) {
                progress = 0;
            } else if (lastVisiblePosition == itemCount) {
                progress = 1;
            } else {
                View view = layoutManager.findViewByPosition(firstVisiblePosition);
                Log.d(TAG, view.getY() / view.getHeight() + "");
                float extra = view.getY() / view.getHeight();
                progress = (firstVisiblePosition - extra) / (itemCount - mRVChildCount);
            }

            setBubbleAndHandlePosition(getHeight() * progress);
        }
    }
}
