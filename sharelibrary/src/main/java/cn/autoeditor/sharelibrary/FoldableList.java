package cn.autoeditor.sharelibrary;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.Resource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoldableList<K,T> {
    public static final int TYPE_HEADER = 1 ;
    public static final int TYPE_GROUP = 2 ;
    public static final int TYPE_CHILD = 3 ;
    private Context mContext ;
    private int mDefaultImage ;
    private int mGroupLayout ;
    private int mChildLayout ;
    private int mHeaderView = -1;
    private RecyclerView mRecyclerView ;
    private ListAdapter mAdapter ;
    private List<Unit<K,T>> mData = new ArrayList<>();
    private boolean mFoldable = true ;

    private OnItemClickListener mOnItemClickListener ;
    public FoldableList(Context context, RecyclerView recyclerView, int groupLayout,int childLayout, int childRow){

        mContext = context ;
        mGroupLayout = groupLayout ;
        mChildLayout = childLayout ;
        mRecyclerView = recyclerView ;
        mAdapter =new ListAdapter();
        GridLayoutManager gridLayoutManagerProduct = new GridLayoutManager(context, childRow, GridLayoutManager.VERTICAL, false);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        gridLayoutManagerProduct.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int type = mAdapter.getItemViewType(position) ;
                if(type == TYPE_GROUP){
                    return  childRow;
                }else{
                    return 1;
                }
            }
        });
        recyclerView.setLayoutManager(gridLayoutManagerProduct);
        recyclerView.setAdapter(mAdapter);
    }
    public void update(List<Unit<K,T>> data){
        mData = data ;
        mAdapter.notifyDataSetChanged();
    }

    public void setHeaderView(int view){
        mHeaderView = view ;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        mOnItemClickListener = onItemClickListener ;
    }
    public void openItem(Unit<K,T> unit){
        unit.folded = false ;
        int position = getUnitPosition(unit) ;
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        linearLayoutManager.scrollToPositionWithOffset(position, 0);
        //mRecyclerView.scrollToPosition(position);
    }
    public void foldable(boolean foldable){
        mFoldable = foldable ;
    }
    public void onBindViewHolder(ViewHolder viewHolder, int position, int type, Unit<K,T> data, T child){

    }
    private boolean isHeaderPos(int position){
        return mHeaderView != -1 && position == 0 ;
    }
    private boolean containsHeader(){
        return mHeaderView != -1 ;
    }
    private int withoutHeaderPos(int position){
        return mHeaderView != -1 ? position-1:position ;
    }
    public int getUnitPosition(Unit<K,T> unit){
        int currentPosition = 0;
        for (Unit<K, T> u : mData) {
            if(u.equals(unit)){
                return currentPosition ;
            }
            currentPosition = currentPosition + 1;
            if (!unit.folded) {
                currentPosition = currentPosition + u.children.size();
            }
        }
        return 0 ;
    }
    public Unit<K,T> getUnit(int position) {

        position = withoutHeaderPos(position) ;
        int currentPosition = -1;
        for (Unit<K, T> unit : mData) {
            currentPosition = currentPosition + 1;
            if (currentPosition == position) {
                return unit;
            }
            if (!unit.folded) {
                currentPosition = currentPosition + unit.children.size();
                if (position <= currentPosition) {
                    return unit;
                }
            }
        }
        return null ;
    }
    private T getChild(int position){

        position = withoutHeaderPos(position) ;
        int currentPosition = 0;
        for (Unit<K, T> unit : mData) {
            currentPosition = currentPosition + 1;
            if (!unit.folded) {
                int childPos = position - currentPosition ;
                if(childPos >=0 && childPos < unit.children.size() ){
                    return unit.children.get(childPos) ;
                }
                currentPosition = currentPosition + unit.children.size();
            }
            if (currentPosition >= position) {
                LtLog.e("getchild error is not child position...."+position) ;
                return null;
            }

        }
        return null ;
    }

    class ListAdapter extends  RecyclerView.Adapter<ViewHolder>{
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = mGroupLayout;

            switch (viewType){
                case TYPE_HEADER :
                    layout = mHeaderView ;
                    break;
                case TYPE_GROUP :
                    layout = mGroupLayout ;
                    break ;
                case TYPE_CHILD :
                    layout = mChildLayout ;
                    break;

            }
            View view = LayoutInflater.from(mContext).inflate(layout, parent, false) ;
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {

            int type = getItemViewType(position) ;

            Unit<K,T> unit = getUnit(position);

            T child = getChild(position) ;
            switch (type){
                case  TYPE_HEADER :
                    viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mOnItemClickListener != null){
                            mOnItemClickListener.onHeaderClick(v);
                        }
                    }
                    });
                    return;
                case TYPE_GROUP :
                    setItemInfo(unit.group,viewHolder.views);
                    break;
                case TYPE_CHILD :
                    if(child != null){
                        setItemInfo(child, viewHolder.views);
                    }
                    break;
            }
           

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (type == TYPE_GROUP) {
                        if(mFoldable) {
                            unit.folded = !unit.folded;
                            if (unit.folded) {
                                notifyItemRangeRemoved(viewHolder.getAdapterPosition() + 1, unit.children.size());
                            } else {
                                notifyItemRangeInserted(viewHolder.getAdapterPosition() + 1, unit.children.size());
                            }
                        }
                    }else if(mOnItemClickListener  != null){

                        mOnItemClickListener.onItemClick(v,viewHolder.getAdapterPosition(), child);
                    }
                }
            });
            FoldableList.this.onBindViewHolder(viewHolder, position, type, unit,child);
        }


        private void setImageView(ImageView imageView, Object obj){
            Class fieldCls = obj.getClass() ;
            if(CharSequence.class.isAssignableFrom(fieldCls)) {
                Glide.with(mContext)
                        .load(obj.toString())
                        .centerCrop()
                        .placeholder(mDefaultImage)
                        .into(imageView);
            }else if(Bitmap.class.isAssignableFrom(fieldCls)){
                imageView.setImageBitmap((Bitmap) obj);
            }else{
                LtLog.i("setImageView error:"+obj) ;
            }
        }

        private void setTextView(TextView textView, Object obj){
            Class fieldCls = obj.getClass() ;
            if(CharSequence.class.isAssignableFrom(fieldCls)) {
                textView.setText((String)obj);
            }else{
                LtLog.i("setTextView error:"+obj) ;
            }
        }


        private void setItemInfo(Object obj, Map<Integer, View> views ){
            for(Map.Entry<Integer,View> entry:views.entrySet()){
                int id = entry.getKey() ;
                View v = entry.getValue() ;
                if(v.getId() == -1){
                    continue;
                }
                String entryName = v.getResources().getResourceEntryName(v.getId()) ;
                try {
                    Object o = null ;
                    if(obj instanceof String){
                        o = obj ;
                    }else {
                        Class cls = obj.getClass() ;
                        Field field = cls.getDeclaredField(entryName);
                        if (!field.isAccessible()) {
                            field.setAccessible(true);
                        }
                        o =field.get(obj);
                    }
                    if(v instanceof ImageView){
                        setImageView((ImageView) v, o);
                    }else if(v instanceof TextView){
                        setTextView((TextView) v, o);
                    }else {
                        LtLog.e("unsupport view:"+v) ;
                    }
                } catch (NoSuchFieldException e) {
                    //e.printStackTrace();
                } catch (IllegalAccessException e) {
                    //e.printStackTrace();
                }

            }

        }





        @Override
        public int getItemViewType(int position) {
            if(isHeaderPos(position)){
                return TYPE_HEADER ;
            }
            position = withoutHeaderPos(position) ;
            int currentPosition = -1;
            for (Unit unit : mData) {
                currentPosition = currentPosition + 1;
                if (currentPosition == position) {
                    return TYPE_GROUP;
                }
                if (!unit.folded) {
                    currentPosition = currentPosition + unit.children.size();
                    if (position <= currentPosition) {
                        return TYPE_CHILD;
                    }
                }

            }
            return TYPE_GROUP;
        }

        @Override
        public int getItemCount() {
            int count = 0 ;
            count += mData.size() ;
            for(Unit unit:mData){
                if(!unit.folded){
                    count += unit.children.size() ;
                }
            }
            if(containsHeader()){
                count++ ;
            }
            return count ;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private Map<Integer, View> views ;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            if(itemView instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) itemView;

                views = getChildList(viewGroup);
            }else{
                views = new HashMap<>(1);
                views.put(itemView.getId(), itemView) ;
            }
        }
        private Map<Integer,View> getChildList(ViewGroup viewGroup){
            Map<Integer, View> map = new HashMap<>() ;
            int count = viewGroup.getChildCount();
            for(int i = 0 ;i < count ; ++i){
                View v = viewGroup.getChildAt(i);
                if(v.getId() != -1) {
                    map.put(v.getId(), v);
                }

                if(v instanceof ViewGroup){
                    map.putAll(getChildList((ViewGroup) v));
                }
            }
            return map ;
        }
        public View getView(int id){
            return views.get(id) ;
        }
    }
    public static class Unit<K, V> {
        public K group;
        public List<V> children;
        public boolean folded = false ;

        public Unit(K group, List<V> children) {
            this.group = group;
            if (children == null) {
                this.children = new ArrayList<>();
            } else {
                this.children = children;
            }
        }

        public Unit(K group, List<V> children, boolean folded) {
            this(group, children);
            this.folded = folded;
        }
    }

    public interface OnItemClickListener<T>{
        public void onItemClick(View v, int position, T child) ;
        public void onFoolterClick(View v) ;
        public void onHeaderClick(View v) ;
    }
}
