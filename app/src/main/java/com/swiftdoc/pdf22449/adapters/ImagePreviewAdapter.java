package com.swiftdoc.pdf22449.adapters;
import android.content.Context; import android.net.Uri; import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup; import android.widget.ImageView;
import androidx.annotation.NonNull; import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; import com.swiftdoc.pdf22449.R;
import java.util.List;
public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.VH> {
    private List<Uri> uris; private final Context ctx;
    public ImagePreviewAdapter(Context c,List<Uri> u){ctx=c;uris=u;}
    public void updateList(List<Uri> u){uris=u;notifyDataSetChanged();}
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int t){return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_image_preview,p,false));}
    @Override public void onBindViewHolder(@NonNull VH h,int i){Glide.with(ctx).load(uris.get(i)).centerCrop().into(h.iv);}
    @Override public int getItemCount(){return uris.size();}
    static class VH extends RecyclerView.ViewHolder{ImageView iv;VH(View v){super(v);iv=v.findViewById(R.id.iv_preview);}}
}
