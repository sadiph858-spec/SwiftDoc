package com.swiftdoc.pdf22449.adapters;
import android.content.Context; import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup;
import android.widget.ImageView; import android.widget.TextView;
import androidx.annotation.NonNull; import androidx.cardview.widget.CardView; import androidx.recyclerview.widget.RecyclerView;
import com.swiftdoc.pdf22449.R; import com.swiftdoc.pdf22449.models.DashboardItem;
import java.util.List;
public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.VH> {
    public interface OnItemClick { void onAction(int action); }
    private final List<DashboardItem> items; private final Context ctx; private final OnItemClick cb;
    public DashboardAdapter(Context c,List<DashboardItem> l,OnItemClick cb){ctx=c;items=l;this.cb=cb;}
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int t){return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_dashboard,p,false));}
    @Override public void onBindViewHolder(@NonNull VH h,int i){
        DashboardItem item=items.get(i);
        h.tvTitle.setText(item.getTitle()); h.tvSub.setText(item.getSubtitle());
        h.ivIcon.setImageResource(item.getIconRes());
        try{h.card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(ctx,item.getBgColor()));}catch(Exception e){}
        h.card.setOnClickListener(v->{if(cb!=null)cb.onAction(item.getAction());});}
    @Override public int getItemCount(){return items.size();}
    static class VH extends RecyclerView.ViewHolder{CardView card;ImageView ivIcon;TextView tvTitle,tvSub;
        VH(View v){super(v);card=v.findViewById(R.id.card_dash);ivIcon=v.findViewById(R.id.iv_dash_icon);tvTitle=v.findViewById(R.id.tv_dash_title);tvSub=v.findViewById(R.id.tv_dash_sub);}}
}
