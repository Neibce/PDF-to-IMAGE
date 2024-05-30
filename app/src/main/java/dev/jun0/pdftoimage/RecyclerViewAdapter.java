package dev.jun0.pdftoimage;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    public interface OnSelectedItemsChangedListener {
        void onChanged(int items);
    }
    final OnSelectedItemsChangedListener mOnSelectedItemsChangedListener;
    final ArrayList<Bitmap> mItemList = new ArrayList<>();
    final ArrayList<Boolean> mIsSelectedList = new ArrayList<>();

    int mSelectedCount = 0;

    RecyclerViewAdapter(OnSelectedItemsChangedListener onSelectedItemsChangedListener){
        mOnSelectedItemsChangedListener = onSelectedItemsChangedListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.pdf_page_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
        holder.getTvCurrentPage().setText(String.format(Locale.getDefault(), "%d", position + 1));
        ImageView ivSelected = holder.getIvSelected();
        ImageView ivThumbnail = holder.getIvThumbnail();

        Bitmap thumbnail = mItemList.get(position);
        ivThumbnail.setImageBitmap(thumbnail);

        ivThumbnail.setOnClickListener((view)->{
            mIsSelectedList.set(position, !mIsSelectedList.get(position));
            ivSelected.setVisibility(mIsSelectedList.get(position) ? View.VISIBLE : View.INVISIBLE);
            mSelectedCount += mIsSelectedList.get(position) ? 1 : -1;

            mOnSelectedItemsChangedListener.onChanged(getSelectedCount());
        });

        ivSelected.setVisibility(mIsSelectedList.get(position) ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    public ArrayList<Boolean> getIsSelectedList(){
        return mIsSelectedList;
    }

    public int getSelectedCount(){
        return mSelectedCount;
    }

    public void initializeItems(int size){
        mSelectedCount = size;
        for (int i = 0; i < size; i++) {
            mItemList.add(null);
            mIsSelectedList.add(true);
        }

        notifyItemRangeInserted(0, size);
    }

    public void setItemBitmap(int pos, Bitmap thumbnail){
        mItemList.set(pos, thumbnail);
        notifyItemChanged(pos);
    }

    public void selectAll(boolean select){
        mSelectedCount = getItemCount() * (select ? 1 : 0);
        for (int i = 0; i < mIsSelectedList.size(); i++)
            mIsSelectedList.set(i, select);

        notifyItemRangeChanged(0, getItemCount());
        mOnSelectedItemsChangedListener.onChanged(getSelectedCount());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCurrentPage;
        final ImageView ivThumbnail;
        final ImageView ivSelected;

        public ViewHolder(View view) {
            super(view);
            tvCurrentPage = view.findViewById(R.id.tv_current_page);
            ivThumbnail = view.findViewById(R.id.iv_thumbnail);
            ivSelected = view.findViewById(R.id.iv_selected);
        }

        public TextView getTvCurrentPage() {
            return tvCurrentPage;
        }

        public ImageView getIvThumbnail() {
            return ivThumbnail;
        }

        public ImageView getIvSelected() {
            return ivSelected;
        }
    }
}