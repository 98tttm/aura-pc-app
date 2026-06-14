package com.example.aura_pc_app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class GlideImageGetter implements Html.ImageGetter {
    private final TextView textView;
    private final Context context;

    public GlideImageGetter(Context context, TextView textView) {
        this.context = context;
        this.textView = textView;
    }

    @Override
    public Drawable getDrawable(String source) {
        final BitmapDrawablePlaceholder drawable = new BitmapDrawablePlaceholder();

        // Fix protocol-relative URLs
        if (source != null && source.startsWith("//")) {
            source = "https:" + source;
        }

        Glide.with(context)
                .asBitmap()
                .load(source)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        int width = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
                        if (width <= 0) width = resource.getWidth();
                        
                        float ratio = (float) resource.getHeight() / resource.getWidth();
                        int height = (int) (width * ratio);

                        BitmapDrawable bitmapDrawable = new BitmapDrawable(context.getResources(), resource);
                        bitmapDrawable.setBounds(0, 0, width, height);
                        drawable.setDrawable(bitmapDrawable);
                        drawable.setBounds(0, 0, width, height);

                        textView.setText(textView.getText()); // trigger reflow
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });

        return drawable;
    }

    private static class BitmapDrawablePlaceholder extends Drawable {
        private Drawable drawable;

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }

        public void setDrawable(Drawable drawable) {
            this.drawable = drawable;
        }

        @Override
        public void setAlpha(int alpha) {
            if (drawable != null) drawable.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable android.graphics.ColorFilter colorFilter) {
            if (drawable != null) drawable.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return drawable != null ? drawable.getOpacity() : android.graphics.PixelFormat.TRANSPARENT;
        }
    }
}
