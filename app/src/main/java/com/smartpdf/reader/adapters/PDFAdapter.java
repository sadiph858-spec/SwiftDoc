package com.smartpdf.reader.adapters;
import android.content.Context; import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup;
import android.widget.ImageButton; import android.widget.ProgressBar; import android.widget.TextView;
import androidx.annotation.NonNull; import androidx.recyclerview.widget.DiffUtil; import androidx.recyclerview.widget.RecyclerView;
import com.smartpdf.reader.R; import com.smartpdf.reader.models.PDFFile;
import java.util.ArrayList; import java.util.List;
public class PDFAdapter extends RecyclerView.Adapter<PDFAdapter.VH> {
    public interface Listener{void onClick(PDFFile f,int pos);void onLongClick(PDFFile f,int pos);void onFavToggle(PDFFile f,int pos);}
    private List<PDFFile> list=new ArrayList<>(); private final Context ctx; private final Listener cb; private boolean gridMode;
    public PDFAdapter(Context c,Listener l,boolean grid){ctx=c;cb=l;gridMode=grid;}
    public void setGridMode(boolean g){gridMode=g;notifyDataSetChanged();}
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int t){return new VH(LayoutInflater.from(ctx).inflate(gridMode?R.layout.item_pdf_grid:R.layout.item_pdf,p,false));}
    @Override public void onBindViewHolder(@NonNull VH h,int pos){
        PDFFile f=list.get(pos); h.tvName.setText(f.getName()); h.tvSize.setText(f.getFormattedSize()); h.tvDate.setText(f.getDate());
        if(h.ibFav!=null)h.ibFav.setImageResource(f.isFavorite()?R.drawable.ic_star_filled:R.drawable.ic_star_outline);
        if(h.progressBar!=null){int pct=f.getLastPage();h.progressBar.setProgress(pct);h.progressBar.setVisibility(pct>0?View.VISIBLE:View.GONE);}
        h.itemView.setOnClickListener(v->{if(cb!=null)cb.onClick(f,h.getAdapterPosition());});
        h.itemView.setOnLongClickListener(v->{if(cb!=null)cb.onLongClick(f,h.getAdapterPosition());return true;});
        if(h.ibFav!=null)h.ibFav.setOnClickListener(v->{if(cb!=null)cb.onFavToggle(f,h.getAdapterPosition());});
        if(h.ibMore!=null)h.ibMore.setOnClickListener(v->{if(cb!=null)cb.onLongClick(f,h.getAdapterPosition());});}
    @Override public int getItemCount(){return list.size();}
    public void updateList(List<PDFFile> nl){DiffUtil.DiffResult r=DiffUtil.calculateDiff(new DiffUtil.Callback(){@Override public int getOldListSize(){return list.size();}@Override public int getNewListSize(){return nl.size();}@Override public boolean areItemsTheSame(int o,int n){return list.get(o).getPath().equals(nl.get(n).getPath());}@Override public boolean areContentsTheSame(int o,int n){PDFFile a=list.get(o),b=nl.get(n);return a.getName().equals(b.getName())&&a.isFavorite()==b.isFavorite();}});list=new ArrayList<>(nl);r.dispatchUpdatesTo(this);}
    static class VH extends RecyclerView.ViewHolder{TextView tvName,tvSize,tvDate;ImageButton ibFav,ibMore;ProgressBar progressBar;
        VH(View v){super(v);tvName=v.findViewById(R.id.tv_pdf_name);tvSize=v.findViewById(R.id.tv_pdf_size);tvDate=v.findViewById(R.id.tv_pdf_date);ibFav=v.findViewById(R.id.ib_favorite);ibMore=v.findViewById(R.id.ib_more);progressBar=v.findViewById(R.id.progress_reading);}}
}
