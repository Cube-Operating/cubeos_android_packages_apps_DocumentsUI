/*
 * Copyright (C) 2026 CubeOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.documentsui.dirlist;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.android.documentsui.R;

/**
 * CubeOS: draws list-mode rows as iOS "inset grouped" sections — one rounded card per run of
 * consecutive document/directory rows, with hairline separators inset from the leading edge
 * and no separator under the last row. Purely visual: it never changes offsets, positions,
 * selection or any adapter state.
 */
final class InsetGroupDecoration extends RecyclerView.ItemDecoration {

    private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mSeparator = new Paint();
    private final float mRadius;
    private final float mSeparatorInset;
    private final float mSeparatorHeight;
    private final Path mPath = new Path();
    private final RectF mRect = new RectF();
    private final float[] mRadii = new float[8];

    InsetGroupDecoration(Context context) {
        final Resources res = context.getResources();
        mFill.setColor(context.getColor(R.color.cube_grouped_surface));
        mSeparator.setColor(context.getColor(R.color.cube_separator));
        mRadius = res.getDimension(R.dimen.cube_group_radius);
        mSeparatorInset = res.getDimension(R.dimen.cube_separator_inset);
        mSeparatorHeight = Math.max(1f, res.getDimension(R.dimen.cube_separator_height));
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final RecyclerView.Adapter<?> adapter = parent.getAdapter();
        if (adapter == null) {
            return;
        }
        final int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = parent.getChildAt(i);
            if (!(parent.getChildViewHolder(child) instanceof ListDocumentHolder)) {
                continue;
            }
            final int pos = parent.getChildAdapterPosition(child);
            final boolean first = pos != RecyclerView.NO_POSITION && !isRow(adapter, pos - 1);
            final boolean last = pos != RecyclerView.NO_POSITION && !isRow(adapter, pos + 1);

            final float top = child.getTop() + child.getTranslationY();
            final float bottom = child.getBottom() + child.getTranslationY();
            mRect.set(child.getLeft() + child.getTranslationX(), top,
                    child.getRight() + child.getTranslationX(), bottom);

            final float rt = first ? mRadius : 0f;
            final float rb = last ? mRadius : 0f;
            mRadii[0] = mRadii[1] = mRadii[2] = mRadii[3] = rt;
            mRadii[4] = mRadii[5] = mRadii[6] = mRadii[7] = rb;
            mPath.reset();
            mPath.addRoundRect(mRect, mRadii, Path.Direction.CW);

            final int alpha = Math.round(255 * child.getAlpha());
            mFill.setAlpha(Math.min(255, alpha));
            c.drawPath(mPath, mFill);
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final RecyclerView.Adapter<?> adapter = parent.getAdapter();
        if (adapter == null) {
            return;
        }
        final boolean rtl = parent.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        final int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = parent.getChildAt(i);
            if (!(parent.getChildViewHolder(child) instanceof ListDocumentHolder)) {
                continue;
            }
            final int pos = parent.getChildAdapterPosition(child);
            if (pos == RecyclerView.NO_POSITION || !isRow(adapter, pos + 1)) {
                continue;
            }
            final float left = child.getLeft() + child.getTranslationX();
            final float right = child.getRight() + child.getTranslationX();
            final float bottom = child.getBottom() + child.getTranslationY();
            c.drawRect(rtl ? left : left + mSeparatorInset, bottom - mSeparatorHeight,
                    rtl ? right - mSeparatorInset : right, bottom, mSeparator);
        }
    }

    private static boolean isRow(RecyclerView.Adapter<?> adapter, int pos) {
        if (pos < 0 || pos >= adapter.getItemCount()) {
            return false;
        }
        final int type = adapter.getItemViewType(pos);
        return type == DocumentsAdapter.ITEM_TYPE_DOCUMENT
                || type == DocumentsAdapter.ITEM_TYPE_DIRECTORY;
    }
}
