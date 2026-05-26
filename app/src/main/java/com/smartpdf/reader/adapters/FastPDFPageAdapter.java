package com.smartpdf.reader.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartpdf.reader.R;
import com.smartpdf.reader.engine.FastPDFRenderer;

/**
 * RecyclerView adapter for the ViewPager2-based fast PDF viewer.
 * Each page is an ImageView; bitmaps come from FastPDFRenderer's cache.
 */
public class FastPDFPageAdapter extends RecyclerView.Adapter<FastPDFPageAdapter.PageVH> {

    private final Context ctx;
    private final FastPDFRenderer renderer;
    private int pageCount = 0;

    public FastPDFPageAdapter(Context ctx, FastPDFRenderer renderer) {
        this.ctx = ctx;
        this.renderer = renderer;
    }

    public void setPageCount(int count) {
        this.pageCount = count;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public PageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_pdf_page, parent, false);
        return new PageVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageVH holder, int position) {
        holder.spinner.setVisibility(View.VISIBLE);
        holder.imageView.setImageBitmap(null);

        // Try cache-first (instant if already rendered)
        Bitmap cached = renderer.getPage(position, new FastPDFRenderer.Callback() {
            @Override public void onReady(int n) {}
            @Override public void onPageReady(int idx, Bitmap bmp) {
                if (idx == holder.getAdapterPosition()) {
                    holder.imageView.setImageBitmap(bmp);
                    holder.spinner.setVisibility(View.GONE);
                }
            }
            @Override public void onError(String msg) {
                holder.spinner.setVisibility(View.GONE);
            }
        });

        if (cached != null) {
            holder.imageView.setImageBitmap(cached);
            holder.spinner.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return pageCount; }

    static class PageVH extends RecyclerView.ViewHolder {
        ImageView imageView;
        ProgressBar spinner;
        PageVH(View v) {
            super(v);
            imageView = v.findViewById(R.id.iv_pdf_page);
            spinner   = v.findViewById(R.id.spinner_page);
        }
    }
}
