package com.anpoz.cutitdown.Fragment;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.anpoz.cutitdown.Adapter.MyRecycleViewAdapter;
import com.anpoz.cutitdown.Beans.Url;
import com.anpoz.cutitdown.R;
import com.anpoz.cutitdown.Utils.LogUtils;
import com.anpoz.cutitdown.Utils.Provider;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class CollectPageFragment extends Fragment implements MyRecycleViewAdapter.ItemClickListener, MyRecycleViewAdapter.ItemLongClickListener {

    private View rootView;
    private RecyclerView mRecycleView;
    private List<Url> mDatas;
    private MyRecycleViewAdapter mAdapter;
    private Toast mToast = null;

    private OnItemUnstaredListener unstaredListener;

    public CollectPageFragment() {
        // Required empty public constructor
    }

    public interface OnItemUnstaredListener {
        void onItemUnstared();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        unstaredListener = (OnItemUnstaredListener) activity;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_collect_page, container, false);
        initViews();
        initData();
        return rootView;
    }

    private void initData() {
        mDatas = new ArrayList<>();
        mAdapter = new MyRecycleViewAdapter(getActivity(), mDatas);
        mRecycleView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecycleView.setItemAnimator(new DefaultItemAnimator());
        mAdapter.setItemClickListener(this);
        mAdapter.setItemLongClickListener(this);
        mRecycleView.setAdapter(mAdapter);

        updateData();
    }

    private void initViews() {
        mRecycleView = (RecyclerView) rootView.findViewById(R.id.tab2_recycler_view);
        initItemTouchHelper(mRecycleView);
    }

    /**
     * 初始化ItemTouchHelper
     *
     * @param recyclerView
     */
    private void initItemTouchHelper(RecyclerView recyclerView) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                //不处理滑动事件
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                deleteData(position);
                mDatas.remove(position);
                mAdapter.notifyItemRemoved(position);
            }
        }).attachToRecyclerView(recyclerView);
    }

    public void updateData() {
        mDatas.clear();
        Cursor cursor = getActivity().getContentResolver()
                .query(Provider.UrlColumns.CONTENT_URI, null,
                        Provider.UrlColumns.URL_STARED + "=?", new String[]{1 + ""}, null);

        int url_long = cursor.getColumnIndex(Provider.UrlColumns.URL_LONG);
        int url_short = cursor.getColumnIndex(Provider.UrlColumns.URL_SHORT);
        int url_date = cursor.getColumnIndex(Provider.UrlColumns.URL_DATE);
        int url_stared = cursor.getColumnIndex(Provider.UrlColumns.URL_STARED);
        int id = cursor.getColumnIndex(Provider.UrlColumns._ID);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Url item = new Url();
                item.setLongUrl(cursor.getString(url_long));
                item.setShortUrl(cursor.getString(url_short));
                item.setDate(cursor.getLong(url_date));
                item.setStared(cursor.getInt(url_stared));
                item.setId(cursor.getInt(id));
                LogUtils.i(item.toString());
                mDatas.add(item);
            }
        }
        cursor.close();
        mAdapter.notifyDataSetChanged();
    }

    private void updateStared(int id) {
        ContentValues cv = new ContentValues();
        cv.put(Provider.UrlColumns.URL_STARED, 0);
        int count = getActivity().getContentResolver()
                .update(Provider.UrlColumns.CONTENT_URI, cv, Provider.UrlColumns._ID + "=?", new String[]{id + ""});
        LogUtils.i("update changed count=" + count);
    }

    private void deleteData(int position) {
        int count = getActivity().getContentResolver()
                .delete(Provider.UrlColumns.CONTENT_URI, Provider.UrlColumns._ID + "=?", new String[]{mDatas.get(position).getId() + ""});
        LogUtils.i("delete changed count=" + count);
    }


    @Override
    public void onItemClick(View v, int position) {
        Url item = mDatas.get(position);
        ClipboardManager clipboardManager = (ClipboardManager) getActivity()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Shortened Url", item.getShortUrl()));

        if (mToast == null) {
            mToast = Toast.makeText(getActivity(), getResources().getString(R.string.msg_url_already_copy), Toast.LENGTH_SHORT);
        } else {
            mToast.setText(getResources().getString(R.string.msg_url_already_copy));
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();

    }

    @Override
    public void onItemLongClick(final View v, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(getResources().getStringArray(R.array.item_context_menu_tab2), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0://复制
                        onItemClick(v, position);
                        break;
                    case 1://取消收藏
                        updateStared(mDatas.get(position).getId());
                        mDatas.remove(position);
                        mAdapter.notifyItemRemoved(position);
                        unstaredListener.onItemUnstared();
                        break;
                    case 2://删除
                        deleteData(position);
                        mDatas.remove(position);
                        mAdapter.notifyItemRemoved(position);
                        break;
                    case 3://打开
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(mDatas.get(position).getShortUrl()));
                        startActivity(intent);
                        break;
                    case 4://分享
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, mDatas.get(position).getShortUrl());
                        shareIntent.putExtra(Intent.EXTRA_TEXT, mDatas.get(position).getShortUrl());
                        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(Intent.createChooser(shareIntent,
                                getActivity().getResources().getString(R.string.share_link_to)));
                        break;
                }
            }
        });
        builder.show();
    }
}
