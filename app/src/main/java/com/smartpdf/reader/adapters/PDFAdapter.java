package com.smartpdf.reader.adapters;
import android.content.Context; import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup;
import android.widget.ImageButton; import android.widget.TextView;
import androidx.annotation.NonNull; import androidx.recyclerview.widget.DiffUtil; import androidx.recyclerview.widget.RecyclerView;
import com.smartpdf.reader.R; import com.smartpdf.reader.models.PDFFile;
import java.util.ArrayList; import java.util.List;
public class PDFAdapter extends RecyclerView.Adapter<PDFAdapter.VH> {
    public interface Listener { void onClick(PDFFile f,int pos); void onLongClick(PDFFile f,int pos); void onFavToggle(PDFFile f,int pos); }
    private List<PDFFile> list=new ArrayList<>(); private final Listener cb; private final Context ctx;
    public PDFAdapter(Context c,Listener l){ctx=c;cb=l;}
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int t){
        return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_pdf,p,false));}
    @Override public void onBindViewHolder(@NonNull VH h,int i){h.bind(list.get(i));}
    @Override public int getItemCount(){return list.size();}
    public void updateList(List<PDFFile> nl){
        DiffUtil.DiffResult r=DiffUtil.calculateDiff(new DiffUtil.Callback(){
            @Override public int getOldListSize(){return list.size();}
            @Override public int getNewListSize(){return nl.size();}
            @Override public boolean areItemsTheSame(int o,int n){return list.get(o).getPath().equals(nl.get(n).getPath());}
            @Override public boolean areContentsTheSame(int o,int n){PDFFile a=list.get(o),b=nl.get(n);return a.getName().equals(b.getName())&&a.isFavorite()==b.isFavorite();}});
        list=new ArrayList<>(nl); r.dispatchUpdatesTo(this);}
    public PDFFile getItem(int i){return(i>=0&&i<list.size())?list.get(i):null;}
    class VH extends RecyclerView.ViewHolder {
        TextView tvN,tvS,tvD; ImageButton ibF,ibM;
        VH(View v){super(v);tvN=v.findViewById(R.id.tv_pdf_name);tvS=v.findViewById(R.id.tv_pdf_size);tvD=v.findViewById(R.id.tv_pdf_date);ibF=v.findViewById(R.id.ib_favorite);ibM=v.findViewById(R.id.ib_more);}
        void bind(PDFFile f){
            tvN.setText(f.getName());tvS.setText(f.getFormattedSize());tvD.setText(f.getDate());
            ibF.setImageResource(f.isFavorite()?R.drawable.ic_star_filled:R.drawable.ic_star_outline);
            itemView.setOnClickListener(v->{if(cb!=null)cb.onClick(f,getAdapterPosition());});
            itemView.setOnLongClickListener(v->{if(cb!=null)cb.onLongClick(f,getAdapterPosition());return true;});
            ibF.setOnClickListener(v->{if(cb!=null)cb.onFavToggle(f,getAdapterPosition());});
            ibM.setOnClickListener(v->{if(cb!=null)cb.onLongClick(f,getAdapterPosition());});}}
}
